package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.api.render.Render3D;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Trajectories extends Module implements QuickImports {
	private static final Trajectories INSTANCE = new Trajectories();
	private static final int MAX_SIMULATION_TICKS = 300;
	private static final double AIR_DRAG = 0.99D;
	private static final double ARROW_GRAVITY = 0.05D;
	private static final double CROSSBOW_ARROW_SPEED = 3.15D;
	private static final double MULTISHOT_ANGLE = Math.toRadians(10.0D);
	private static final double MARKER_OFFSET = 0.004D;
	private static final double MARKER_RADIUS = 0.22D;
	private static final int MARKER_SEGMENTS = 40;

	private Trajectories() {
		super("Trajectories", ModuleCategory.RENDER, "Shows where a bow or loaded crossbow arrow will hit.");
		Render3D.add(this::renderWorld);
	}

	public static Trajectories getInstance() {
		return INSTANCE;
	}

	private void renderWorld(WorldRenderContext context) {
		if (!enabled() || mc.level == null || mc.player == null) {
			return;
		}

		float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
		List<Launch> launches = getLaunches(tickDelta);
		if (launches.isEmpty()) {
			return;
		}

		Set<Integer> highlightedEntities = new HashSet<>();
		for (Launch launch : launches) {
			Impact impact = simulate(launch);
			if (impact != null) {
				drawMarker(context, impact);
				if (impact.target() != null && highlightedEntities.add(impact.target().getId())) {
					drawTargetBox(context, impact.target(), tickDelta);
				}
			}
		}
	}

	private List<Launch> getLaunches(float tickDelta) {
		ItemStack usedStack = mc.player.getUseItem();
		if (mc.player.isUsingItem() && usedStack.getItem() instanceof BowItem) {
			float power = BowItem.getPowerForTime(mc.player.getTicksUsingItem());
			if (power < 0.1F) {
				return List.of();
			}
			return List.of(launch(tickDelta, mc.player.getViewVector(tickDelta), power * 3.0D));
		}

		ItemStack crossbow = findLoadedCrossbow();
		if (crossbow.isEmpty()) {
			return List.of();
		}

		ChargedProjectiles projectiles = crossbow.get(DataComponents.CHARGED_PROJECTILES);
		if (projectiles == null || projectiles.isEmpty() || projectiles.contains(Items.FIREWORK_ROCKET)) {
			return List.of();
		}

		Vec3 direction = mc.player.getViewVector(tickDelta).normalize();
		Vec3 up = mc.player.getUpVector(tickDelta).normalize();
		List<ItemStack> loadedProjectiles = projectiles.getItems();
		List<Launch> launches = new ArrayList<>(loadedProjectiles.size());
		for (int index = 0; index < loadedProjectiles.size(); index++) {
			double angle = switch (index) {
				case 1 -> -MULTISHOT_ANGLE;
				case 2 -> MULTISHOT_ANGLE;
				default -> 0.0D;
			};
			launches.add(launch(tickDelta, rotateAroundAxis(direction, up, angle), CROSSBOW_ARROW_SPEED));
		}
		return launches;
	}

	private Launch launch(float tickDelta, Vec3 shotDirection, double speed) {
		Vec3 direction = shotDirection.normalize();
		Vec3 position = mc.player.getEyePosition(tickDelta).add(direction.scale(0.15D));
		return new Launch(position, direction.scale(speed));
	}

	private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, double angle) {
		double cosine = Math.cos(angle);
		double sine = Math.sin(angle);
		return vector.scale(cosine)
				.add(axis.cross(vector).scale(sine))
				.add(axis.scale(axis.dot(vector) * (1.0D - cosine)))
				.normalize();
	}

	private ItemStack findLoadedCrossbow() {
		ItemStack mainHand = mc.player.getMainHandItem();
		if (mainHand.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(mainHand)) {
			return mainHand;
		}

		ItemStack offHand = mc.player.getOffhandItem();
		if (offHand.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(offHand)) {
			return offHand;
		}
		return ItemStack.EMPTY;
	}

	private Impact simulate(Launch launch) {
		Vec3 position = launch.position();
		Vec3 velocity = launch.velocity();

		for (int tick = 0; tick < MAX_SIMULATION_TICKS; tick++) {
			Vec3 nextPosition = position.add(velocity);
			BlockHitResult blockHit = mc.level.clip(new ClipContext(
					position,
					nextPosition,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					mc.player
			));

			Vec3 collisionEnd = blockHit.getType() == HitResult.Type.MISS ? nextPosition : blockHit.getLocation();
			EntityHit entityHit = findEntityHit(position, collisionEnd);
			if (entityHit != null) {
				Vec3 normal = velocity.lengthSqr() > 1.0E-8D
						? velocity.normalize().scale(-1.0D)
						: new Vec3(0.0D, 1.0D, 0.0D);
				return new Impact(entityHit.position(), normal, entityHit.entity());
			}

			if (blockHit.getType() != HitResult.Type.MISS) {
				Direction face = blockHit.getDirection();
				Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
				return new Impact(blockHit.getLocation(), normal, null);
			}

			position = nextPosition;
			velocity = velocity.scale(AIR_DRAG).add(0.0D, -ARROW_GRAVITY, 0.0D);
		}
		return null;
	}

	private EntityHit findEntityHit(Vec3 start, Vec3 end) {
		AABB searchBox = new AABB(start, end).inflate(1.0D);
		Entity closestEntity = null;
		Vec3 closestPosition = null;
		double closestDistanceSq = start.distanceToSqr(end);

		for (Entity entity : mc.level.getEntities(mc.player, searchBox, candidate ->
				candidate != mc.player && !candidate.isSpectator() && candidate.isPickable())) {
			AABB hitBox = entity.getBoundingBox().inflate(0.3D);
			Optional<Vec3> intersection = hitBox.clip(start, end);
			if (intersection.isEmpty() && !hitBox.contains(start)) {
				continue;
			}

			Vec3 hitPosition = intersection.orElse(start);
			double distanceSq = start.distanceToSqr(hitPosition);
			if (distanceSq <= closestDistanceSq) {
				closestDistanceSq = distanceSq;
				closestEntity = entity;
				closestPosition = hitPosition;
			}
		}

		return closestEntity == null ? null : new EntityHit(closestEntity, closestPosition);
	}

	private void drawMarker(WorldRenderContext context, Impact impact) {
		Vec3 normal = impact.normal().normalize();
		Vec3 reference = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 horizontal = normal.cross(reference).normalize();
		Vec3 vertical = normal.cross(horizontal).normalize();
		Vec3 center = impact.position().add(normal.scale(MARKER_OFFSET));
		double distance = context.camera().getPosition().distanceTo(center);
		float distanceProgress = Mth.clamp((float) ((distance - 12.0D) / 88.0D), 0.0F, 1.0F);
		double radius = MARKER_RADIUS * Mth.lerp(distanceProgress, 1.0F, 0.38F);
		int alpha = Math.round(Mth.lerp(distanceProgress, 255.0F, 45.0F));
		int markerColor = alpha << 24 | 0x00FFFFFF;

		PoseStack poseStack = context.matrixStack();
		Vec3 camera = context.camera().getPosition();
		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);
		Matrix4f matrix = poseStack.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);
		RenderSystem.lineWidth(Mth.lerp(distanceProgress, 4.8F, 2.0F));

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		for (int segment = 0; segment < MARKER_SEGMENTS; segment++) {
			double firstAngle = Math.PI * 2.0D * segment / MARKER_SEGMENTS;
			double secondAngle = Math.PI * 2.0D * (segment + 1) / MARKER_SEGMENTS;
			line(matrix, buffer,
					point(center, horizontal, vertical, firstAngle, radius),
					point(center, horizontal, vertical, secondAngle, radius),
					markerColor);
		}

		line(matrix, buffer, center.add(horizontal.scale(-radius)), center.add(horizontal.scale(radius)), markerColor);
		line(matrix, buffer, center.add(vertical.scale(-radius)), center.add(vertical.scale(radius)), markerColor);
		BufferUploader.drawWithShader(buffer.buildOrThrow());

		RenderSystem.lineWidth(1.0F);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private void drawTargetBox(WorldRenderContext context, Entity entity, float tickDelta) {
		double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
		double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
		double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
		AABB box = entity.getBoundingBox()
				.move(-entity.getX(), -entity.getY(), -entity.getZ())
				.move(x, y, z);
		PoseStack poseStack = context.matrixStack();
		Vec3 camera = context.camera().getPosition();
		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);
		Matrix4f matrix = poseStack.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);
		drawFilledBox(matrix, box, 0x30FF2020);
		RenderSystem.lineWidth(4.0F);
		drawBoxOutline(matrix, box, 0xFFFF3030);

		RenderSystem.lineWidth(1.0F);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private static void drawFilledBox(Matrix4f matrix, AABB box, int color) {
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;
		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		quad(matrix, buffer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
		quad(matrix, buffer, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
		quad(matrix, buffer, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
		quad(matrix, buffer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		quad(matrix, buffer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
		quad(matrix, buffer, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void drawBoxOutline(Matrix4f matrix, AABB box, int color) {
		Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
		Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);
		Vec3[] corners = {
				min,
				new Vec3(max.x, min.y, min.z),
				new Vec3(max.x, min.y, max.z),
				new Vec3(min.x, min.y, max.z),
				new Vec3(min.x, max.y, min.z),
				new Vec3(max.x, max.y, min.z),
				max,
				new Vec3(min.x, max.y, max.z)
		};
		int[][] edges = {
				{0, 1}, {1, 2}, {2, 3}, {3, 0},
				{4, 5}, {5, 6}, {6, 7}, {7, 4},
				{0, 4}, {1, 5}, {2, 6}, {3, 7}
		};
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		for (int[] edge : edges) {
			line(matrix, buffer, corners[edge[0]], corners[edge[1]], color);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void quad(Matrix4f matrix, BufferBuilder buffer,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4, int color) {
		buffer.addVertex(matrix, x1, y1, z1).setColor(color);
		buffer.addVertex(matrix, x2, y2, z2).setColor(color);
		buffer.addVertex(matrix, x3, y3, z3).setColor(color);
		buffer.addVertex(matrix, x4, y4, z4).setColor(color);
	}

	private static Vec3 point(Vec3 center, Vec3 horizontal, Vec3 vertical, double angle, double radius) {
		return center
				.add(horizontal.scale(Math.cos(angle) * radius))
				.add(vertical.scale(Math.sin(angle) * radius));
	}

	private static void line(Matrix4f matrix, BufferBuilder buffer, Vec3 start, Vec3 end, int color) {
		buffer.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z).setColor(color);
		buffer.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z).setColor(color);
	}

	private record Launch(Vec3 position, Vec3 velocity) {
	}

	private record Impact(Vec3 position, Vec3 normal, Entity target) {
	}

	private record EntityHit(Entity entity, Vec3 position) {
	}
}
