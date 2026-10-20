package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.render.Render3D;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.utils.render.ProjectionUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Indicator extends Module implements QuickImports {
	private static final Indicator INSTANCE = new Indicator();
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");
	private static final int MAX_PREDICTION_TICKS = 240;
	private static final double MAX_SAFE_COORDINATE = 29_999_984.0D;
	private static final double MAX_SAFE_STEP_DISTANCE_SQ = 1024.0D;
	private static final double AIR_DRAG = 0.99D;
	private static final double WATER_DRAG = 0.60D;
	private static final float FONT_SIZE = 8.5F;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int BACKGROUND_COLOR = 0xFF000000;
	private static final float DISTANT_SCALE = 0.65F;
	private static MsdfFontRenderer font;

	private List<Projectile> capturedProjectiles = List.of();
	private List<Prediction> framePredictions = List.of();
	private final Map<Integer, SmoothedImpact> smoothedImpacts = new HashMap<>();

	private Indicator() {
		super("Indicator", ModuleCategory.RENDER, "Shows a glowing projectile path, item icon and time to impact.");
		Render3D.add(this::renderWorld);
		Render2D.add(this::renderHud);
	}

	public static Indicator getInstance() {
		return INSTANCE;
	}

	@Override
	public void onTick() {
		if (!enabled() || mc.level == null || mc.player == null) {
			capturedProjectiles = List.of();
			framePredictions = List.of();
			smoothedImpacts.clear();
			return;
		}

		List<Projectile> projectiles = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof Projectile projectile && projectileIcon(projectile) != null && isFlying(projectile)) {
				projectiles.add(projectile);
			}
		}
		capturedProjectiles = projectiles;
	}

	private void renderWorld(WorldRenderContext context) {
		if (!enabled() || mc.level == null || mc.player == null || capturedProjectiles.isEmpty()) {
			framePredictions = List.of();
			return;
		}

		float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
		List<Prediction> predictions = new ArrayList<>();
		Set<Integer> activeIds = new HashSet<>();
		long now = System.nanoTime();
		for (Projectile projectile : capturedProjectiles) {
			if (!isFlying(projectile)) {
				continue;
			}

			Prediction prediction = predict(projectile, tickDelta);
			if (prediction != null) {
				activeIds.add(projectile.getId());
				SmoothedImpact smoothed = smoothedImpacts.computeIfAbsent(
						projectile.getId(),
						ignored -> new SmoothedImpact(prediction.impact(), now)
				);
				predictions.add(prediction.withDisplayImpact(smoothed.update(prediction.impact(), now)));
			}
		}
		smoothedImpacts.keySet().removeIf(id -> !activeIds.contains(id));
		framePredictions = predictions;

		if (predictions.isEmpty()) {
			return;
		}

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

		drawPaths(matrix, predictions, 7.0F, 0x2839BFFF);
		drawPaths(matrix, predictions, 4.0F, 0x7052D5FF);
		drawPaths(matrix, predictions, 1.8F, 0xFFE8FAFF);

		RenderSystem.lineWidth(1.0F);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private void renderHud(GuiGraphics guiGraphics) {
		if (!enabled() || framePredictions.isEmpty()) {
			return;
		}

		MsdfFontRenderer font = font();
		if (font == null) {
			return;
		}

		for (Prediction prediction : framePredictions) {
			Optional<Vector2f> projection = ProjectionUtil.worldToScreen(prediction.displayImpact().add(0.0D, 0.18D, 0.0D));
			if (projection.isEmpty()) {
				continue;
			}

			double seconds = Math.max(0.0D, prediction.ticksToImpact() / 20.0D);
			String text = String.format(Locale.ROOT, "%.2fs", seconds);
			float textWidth = font.width(text, FONT_SIZE);
			float iconScale = 0.7F;
			float iconSize = 16.0F * iconScale;
			float boxWidth = 5.0F + iconSize + 3.0F + textWidth + 5.0F;
			Vector2f point = projection.get();
			float boxX = point.x - boxWidth / 2.0F;
			float boxY = point.y - 15.0F;
			float scale = indicatorScale(prediction.displayImpact());
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			poseStack.translate(point.x, point.y, 0.0F);
			poseStack.scale(scale, scale, 1.0F);
			poseStack.translate(-point.x, -point.y, 0.0F);

			Render2D.roundedRect(guiGraphics, boxX, boxY, boxWidth, 15.0F, 3.0F, BACKGROUND_COLOR);
			poseStack.pushPose();
			poseStack.translate(boxX + 4.0F, boxY + 1.9F, 0.0F);
			poseStack.scale(iconScale, iconScale, 1.0F);
			guiGraphics.renderItem(prediction.icon(), 0, 0);
			poseStack.popPose();
			font.draw(guiGraphics, text, boxX + 5.0F + iconSize + 3.0F, boxY + 10.4F, FONT_SIZE, TEXT_COLOR);
			poseStack.popPose();
		}
	}

	private static float indicatorScale(Vec3 position) {
		if (mc.player == null) {
			return 1.0F;
		}
		float distance = (float) mc.player.position().distanceTo(position);
		float progress = Mth.clamp((distance - 6.0F) / 42.0F, 0.0F, 1.0F);
		return Mth.lerp(progress, 1.0F, DISTANT_SCALE);
	}

	private Prediction predict(Projectile projectile, float tickDelta) {
		ItemStack icon = projectileIcon(projectile);
		if (icon == null) {
			return null;
		}
		ProjectilePhysics physics = physics(projectile);
		Vec3 position = new Vec3(
				Mth.lerp(tickDelta, projectile.xOld, projectile.getX()),
				Mth.lerp(tickDelta, projectile.yOld, projectile.getY()),
				Mth.lerp(tickDelta, projectile.zOld, projectile.getZ())
		);
		Vec3 velocity = projectile.getDeltaMovement();
		if (!isSafeVector(position) || !isSafeVector(velocity)) {
			return null;
		}
		List<Vec3> path = new ArrayList<>();
		path.add(position);

		for (int tick = 0; tick < MAX_PREDICTION_TICKS; tick++) {
			Vec3 nextPosition = position.add(velocity);
			if (!isSafeVector(nextPosition) || position.distanceToSqr(nextPosition) > MAX_SAFE_STEP_DISTANCE_SQ) {
				return null;
			}
			BlockHitResult blockHit = mc.level.clip(new ClipContext(
					position,
					nextPosition,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					projectile
			));

			double collisionDistanceSq = blockHit.getType() == HitResult.Type.MISS
					? position.distanceToSqr(nextPosition)
					: position.distanceToSqr(blockHit.getLocation());
			EntityHitResult entityHit = findEntityHit(projectile, position, nextPosition, collisionDistanceSq);

			HitResult hit = entityHit != null ? entityHit : blockHit.getType() != HitResult.Type.MISS ? blockHit : null;
			if (hit != null) {
				Vec3 impact = hit.getLocation();
				if (!isSafeVector(impact)) {
					return null;
				}
				path.add(impact);
				double stepLength = position.distanceTo(nextPosition);
				double stepProgress = stepLength <= 1.0E-6D ? 0.0D : position.distanceTo(impact) / stepLength;
				return new Prediction(projectile.getId(), List.copyOf(path), impact, impact, tick + Mth.clamp(stepProgress, 0.0D, 1.0D), icon.copy());
			}

			path.add(nextPosition);
			position = nextPosition;
			double drag = mc.level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER)
					? physics.waterDrag()
					: physics.airDrag();
			velocity = velocity.scale(drag);
			if (!projectile.isNoGravity()) {
				velocity = velocity.add(0.0D, -physics.gravity(), 0.0D);
			}
			if (!isSafeVector(velocity)) {
				return null;
			}
		}
		return null;
	}

	private static EntityHitResult findEntityHit(Projectile projectile, Vec3 position, Vec3 nextPosition, double collisionDistanceSq) {
		AABB searchBox = new AABB(position, nextPosition).inflate(1.0D);
		if (!isSafeBox(searchBox)) {
			return null;
		}

		try {
			return ProjectileUtil.getEntityHitResult(
					projectile,
					position,
					nextPosition,
					searchBox,
					entity -> entity != projectile.getOwner() && !entity.isSpectator() && entity.isPickable(),
					collisionDistanceSq
			);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static boolean isSafeVector(Vec3 vector) {
		return Double.isFinite(vector.x)
				&& Double.isFinite(vector.y)
				&& Double.isFinite(vector.z)
				&& Math.abs(vector.x) <= MAX_SAFE_COORDINATE
				&& Math.abs(vector.y) <= MAX_SAFE_COORDINATE
				&& Math.abs(vector.z) <= MAX_SAFE_COORDINATE;
	}

	private static boolean isSafeBox(AABB box) {
		return Double.isFinite(box.minX)
				&& Double.isFinite(box.minY)
				&& Double.isFinite(box.minZ)
				&& Double.isFinite(box.maxX)
				&& Double.isFinite(box.maxY)
				&& Double.isFinite(box.maxZ)
				&& box.minX <= box.maxX
				&& box.minY <= box.maxY
				&& box.minZ <= box.maxZ
				&& Math.abs(box.minX) <= MAX_SAFE_COORDINATE
				&& Math.abs(box.minY) <= MAX_SAFE_COORDINATE
				&& Math.abs(box.minZ) <= MAX_SAFE_COORDINATE
				&& Math.abs(box.maxX) <= MAX_SAFE_COORDINATE
				&& Math.abs(box.maxY) <= MAX_SAFE_COORDINATE
				&& Math.abs(box.maxZ) <= MAX_SAFE_COORDINATE;
	}

	private static boolean isFlying(Projectile projectile) {
		return !projectile.isRemoved()
				&& (!(projectile instanceof AbstractArrow arrow) || !arrow.isNoPhysics())
				&& projectile.getDeltaMovement().lengthSqr() > 1.0E-5D;
	}

	private static ItemStack projectileIcon(Projectile projectile) {
		if (projectile instanceof ThrownTrident) {
			return new ItemStack(Items.TRIDENT);
		}
		if (projectile instanceof SpectralArrow) {
			return new ItemStack(Items.SPECTRAL_ARROW);
		}
		if (projectile instanceof AbstractArrow) {
			return new ItemStack(Items.ARROW);
		}
		if (projectile instanceof ThrowableItemProjectile itemProjectile) {
			ItemStack stack = itemProjectile.getItem();
			return stack.isEmpty() ? null : stack;
		}
		if (projectile instanceof FireworkRocketEntity) {
			return new ItemStack(Items.FIREWORK_ROCKET);
		}
		if (projectile instanceof AbstractWindCharge) {
			return new ItemStack(Items.WIND_CHARGE);
		}
		return null;
	}

	private static ProjectilePhysics physics(Projectile projectile) {
		if (projectile instanceof FireworkRocketEntity) {
			return new ProjectilePhysics(1.15D, 1.15D, 0.0D);
		}
		if (projectile instanceof AbstractWindCharge) {
			return new ProjectilePhysics(1.0D, 1.0D, 0.0D);
		}
		if (projectile instanceof ThrownExperienceBottle) {
			return new ProjectilePhysics(AIR_DRAG, WATER_DRAG, 0.07D);
		}
		if (projectile instanceof ThrownPotion) {
			return new ProjectilePhysics(AIR_DRAG, WATER_DRAG, 0.05D);
		}
		if (projectile instanceof ThrowableItemProjectile) {
			return new ProjectilePhysics(AIR_DRAG, WATER_DRAG, 0.03D);
		}
		return new ProjectilePhysics(AIR_DRAG, WATER_DRAG, 0.05D);
	}

	private static void drawPaths(Matrix4f matrix, List<Prediction> predictions, float width, int color) {
		RenderSystem.lineWidth(width);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		for (Prediction prediction : predictions) {
			List<Vec3> path = prediction.path();
			for (int index = 1; index < path.size(); index++) {
				Vec3 start = path.get(index - 1);
				Vec3 end = path.get(index);
				buffer.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z).setColor(color);
				buffer.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z).setColor(color);
			}
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static MsdfFontRenderer font() {
		if (font == null && mc.getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private record Prediction(int projectileId, List<Vec3> path, Vec3 impact, Vec3 displayImpact, double ticksToImpact, ItemStack icon) {
		private Prediction withDisplayImpact(Vec3 position) {
			return new Prediction(projectileId, path, impact, position, ticksToImpact, icon);
		}
	}

	private record ProjectilePhysics(double airDrag, double waterDrag, double gravity) {
	}

	private static final class SmoothedImpact {
		private Vec3 position;
		private long lastUpdateNanos;

		private SmoothedImpact(Vec3 position, long lastUpdateNanos) {
			this.position = position;
			this.lastUpdateNanos = lastUpdateNanos;
		}

		private Vec3 update(Vec3 target, long now) {
			if (position.distanceToSqr(target) > 4.0D) {
				position = target;
				lastUpdateNanos = now;
				return position;
			}

			double elapsedSeconds = Mth.clamp((now - lastUpdateNanos) / 1_000_000_000.0D, 0.0D, 0.1D);
			double smoothing = 1.0D - Math.exp(-18.0D * elapsedSeconds);
			position = position.lerp(target, smoothing);
			lastUpdateNanos = now;
			return position;
		}
	}
}
