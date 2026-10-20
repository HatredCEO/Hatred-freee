package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.player.world.AttackEvent;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.render.Render3D;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.utils.render.ProjectionUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.ColorSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ESP extends Module implements QuickImports {
	private static final ESP INSTANCE = new ESP();
	private static final float OUTLINE_WIDTH = 6.0F;
	private static final float SCREEN_LINE_WIDTH = 0.5F;
	private static final int HIT_COLOR = 0xFFFF3B3B;
	private static final long HIT_FADE_IN_MS = 90L;
	private static final long HIT_HOLD_MS = 100L;
	private static final long HIT_FADE_OUT_MS = 460L;
	private static final long HIT_ANIMATION_MS = HIT_FADE_IN_MS + HIT_HOLD_MS + HIT_FADE_OUT_MS;

	private final EnumBooleanMapSetting<EntityTargetFilter> targets = EntityTargeting.createTargetsSetting("Selects which entities render in ESP.");
	private final EnumSetting<EspMode> mode = new EnumSetting<>("mode", "Mode", "Selects a 3D world box or a 2D screen box.", EspMode.THREE_D);
	private final BooleanSetting outline = new BooleanSetting("outline", "Outline", "Draws the ESP box outline.", true);
	private final BooleanSetting fill = new BooleanSetting("box_fill", "Fill", "Draws the filled ESP box.", true);
	private final ColorSetting playerColor = new ColorSetting("player_color", "Player Color", "ESP color used for players.", 0xFFFFFFFF);
	private final ColorSetting friendColor = new ColorSetting("friend_color", "Friend Color", "ESP color used for friends.", 0xFF55FF55);
	private final ColorSetting mobColor = new ColorSetting("mob_color", "Mob Color", "ESP color used for hostile mobs.", 0xFFFFFFFF);
	private final ColorSetting animalColor = new ColorSetting("animal_color", "Animal Color", "ESP color used for animals.", 0xFFFFFFFF);
	private final FloatSetting range = new FloatSetting("range", "Range", "Maximum ESP range.", 8.0F, 64.0F, 256.0F).step(1.0F).precision(0).suffix("m");
	private final BooleanSetting throughWalls = new BooleanSetting("through_walls", "Through Walls", "Draws the ESP without depth testing.", true);
	private final FloatSetting fillAlpha = (FloatSetting) new FloatSetting("fill_alpha", "Fill Alpha", "Opacity of the ESP fill.", 0.0F, 0.23F, 1.0F)
			.step(0.01F).precision(2).visibleWhen(fill::enabled);
	private final BooleanSetting changeColorOnHit = new BooleanSetting(
			"change_color_on_hit",
			"Change Color On Hit",
			"Smoothly changes a hit entity's ESP color to red.",
			true
	);
	private List<Entity> capturedEntities = List.of();
	private final Map<Integer, Long> hitAnimations = new HashMap<>();

	private ESP() {
		super("ESP", ModuleCategory.RENDER, "Renders 2D or 3D boxes around selected entities.");
		register(targets, mode, outline, fill, playerColor, friendColor, mobColor, animalColor, range, throughWalls, fillAlpha, changeColorOnHit);
		AttackEvent.getInstance().subscribe(new Listener<>(event -> {
			if (enabled() && changeColorOnHit.enabled() && event.entity() != null) {
				hitAnimations.put(event.entity().getId(), System.currentTimeMillis());
			}
		}));
		Render3D.add(this::renderWorld);
		Render2D.add(this::renderHud);
	}

	public static ESP getInstance() {
		return INSTANCE;
	}

	@Override
	public void onTick() {
		if (!enabled() || mc.level == null || mc.player == null) {
			capturedEntities = List.of();
			hitAnimations.clear();
			return;
		}
		long now = System.currentTimeMillis();
		hitAnimations.entrySet().removeIf(entry -> now - entry.getValue() >= HIT_ANIMATION_MS);

		double maxDistanceSq = range.floatValue() * range.floatValue();
		List<Entity> entities = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!EntityTargeting.matches(entity, targets, false) || entity.isRemoved()) {
				continue;
			}
			if (mc.player.distanceToSqr(entity) > maxDistanceSq) {
				continue;
			}
			entities.add(entity);
		}
		capturedEntities = entities;
	}

	private void renderWorld(WorldRenderContext context) {
		if (!enabled() || !mode.is(EspMode.THREE_D) || mc.level == null || mc.player == null || capturedEntities.isEmpty()) {
			return;
		}

		Vec3 cameraPosition = context.camera().getPosition();
		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		Matrix4f matrix = poseStack.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);
		RenderSystem.lineWidth(OUTLINE_WIDTH);

		if (throughWalls.enabled()) {
			RenderSystem.disableDepthTest();
			RenderSystem.depthMask(false);
		} else {
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
		}

		float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
		for (Entity entity : capturedEntities) {
			if (entity.isRemoved()) {
				continue;
			}
			if (mc.player.distanceToSqr(entity) > range.floatValue() * range.floatValue()) {
				continue;
			}

			double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
			double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
			double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
			AABB box = entity.getBoundingBox()
					.move(-entity.getX(), -entity.getY(), -entity.getZ())
					.move(x, y, z);
			int color = color(entity);
			if (fill.enabled() && fillAlpha.floatValue() > 0.0F) {
				drawFilledBox(matrix, box, withAlpha(color, fillAlpha.floatValue()));
			}
			if (outline.enabled()) {
				drawOutline(matrix, box, color);
			}
		}

		RenderSystem.lineWidth(1.0F);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		poseStack.popPose();
	}

	private void renderHud(GuiGraphics guiGraphics) {
		if (!enabled() || !mode.is(EspMode.TWO_D) || mc.level == null || mc.player == null || capturedEntities.isEmpty()) {
			return;
		}

		float tickDelta = Render2D.tickDelta();
		double maxDistanceSq = range.floatValue() * range.floatValue();
		try (Render2D.RectBatch batch = Render2D.beginRectBatch(guiGraphics)) {
			for (Entity entity : capturedEntities) {
				if (entity.isRemoved() || mc.player.distanceToSqr(entity) > maxDistanceSq) {
					continue;
				}
				if (!throughWalls.enabled() && !mc.player.hasLineOfSight(entity)) {
					continue;
				}

				double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
				double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
				double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
				AABB box = entity.getBoundingBox()
						.move(-entity.getX(), -entity.getY(), -entity.getZ())
						.move(x, y, z);
				ProjectionUtil.boxToScreenClipped(box).ifPresent(projection -> drawScreenBox(batch, projection, color(entity)));
			}
		}
	}

	private void drawScreenBox(Render2D.RectBatch batch, Vector4f projection, int boxColor) {
		float x = projection.x;
		float y = projection.y;
		float width = projection.z;
		float height = projection.w;
		if (width < 2.0F || height < 2.0F) {
			return;
		}

		if (fill.enabled()) {
			if (fillAlpha.floatValue() > 0.0F) {
				batch.add(x, y, width, height, withAlpha(boxColor, fillAlpha.floatValue()));
			}
		}
		if (!outline.enabled()) {
			return;
		}

		float endX = x + width;
		float endY = y + height;
		batch.add(x - 0.5F, y - 0.5F, width + 1.0F, SCREEN_LINE_WIDTH, boxColor);
		batch.add(x - 0.5F, endY, width + 1.0F, SCREEN_LINE_WIDTH, boxColor);
		batch.add(x - 0.5F, y, SCREEN_LINE_WIDTH, height, boxColor);
		batch.add(endX, y, SCREEN_LINE_WIDTH, height, boxColor);
	}

	private static void drawFilledBox(Matrix4f matrix, AABB box, int color) {
		float red = channel(color, 16);
		float green = channel(color, 8);
		float blue = channel(color, 0);
		float alpha = channel(color, 24);
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;
		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		vertex(buffer, matrix, minX, minY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, minY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, minY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, minY, maxZ, red, green, blue, alpha);

		vertex(buffer, matrix, minX, maxY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, maxY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, maxY, minZ, red, green, blue, alpha);

		vertex(buffer, matrix, minX, minY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, maxY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, maxY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, minY, minZ, red, green, blue, alpha);

		vertex(buffer, matrix, minX, minY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, minY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, maxY, maxZ, red, green, blue, alpha);

		vertex(buffer, matrix, minX, minY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, minY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, maxY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, minX, maxY, minZ, red, green, blue, alpha);

		vertex(buffer, matrix, maxX, minY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, maxY, minZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
		vertex(buffer, matrix, maxX, minY, maxZ, red, green, blue, alpha);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void drawOutline(Matrix4f matrix, AABB box, int color) {
		float red = channel(color, 16);
		float green = channel(color, 8);
		float blue = channel(color, 0);
		float alpha = channel(color, 24);
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;
		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
		line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
		line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
		line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

		line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
		line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
		line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

		line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
		line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
		line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
		line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void line(BufferBuilder buffer, Matrix4f matrix, float fromX, float fromY, float fromZ, float toX, float toY, float toZ, float red, float green, float blue, float alpha) {
		vertex(buffer, matrix, fromX, fromY, fromZ, red, green, blue, alpha);
		vertex(buffer, matrix, toX, toY, toZ, red, green, blue, alpha);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float red, float green, float blue, float alpha) {
		buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
	}

	private int color(Entity entity) {
		EntityTargetFilter category = EntityTargeting.category(entity);
		int baseColor = category == null ? playerColor.color() : switch (category) {
			case PLAYERS -> playerColor.color();
			case FRIENDS -> friendColor.color();
			case MOBS -> mobColor.color();
			case ANIMALS -> animalColor.color();
		};
		if (!changeColorOnHit.enabled()) {
			return baseColor;
		}
		Long hitAt = hitAnimations.get(entity.getId());
		return hitAt == null ? baseColor : blendColor(baseColor, HIT_COLOR, hitStrength(System.currentTimeMillis() - hitAt));
	}

	private static float hitStrength(long ageMs) {
		if (ageMs <= 0L) {
			return 0.0F;
		}
		if (ageMs < HIT_FADE_IN_MS) {
			return smoothStep(ageMs / (float) HIT_FADE_IN_MS);
		}
		if (ageMs < HIT_FADE_IN_MS + HIT_HOLD_MS) {
			return 1.0F;
		}
		float fadeOut = (ageMs - HIT_FADE_IN_MS - HIT_HOLD_MS) / (float) HIT_FADE_OUT_MS;
		return 1.0F - smoothStep(fadeOut);
	}

	private static float smoothStep(float value) {
		float clamped = Math.max(0.0F, Math.min(1.0F, value));
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static int blendColor(int from, int to, float progress) {
		float clamped = Math.max(0.0F, Math.min(1.0F, progress));
		int alpha = Math.round(Mth.lerp(clamped, from >>> 24 & 255, to >>> 24 & 255));
		int red = Math.round(Mth.lerp(clamped, from >>> 16 & 255, to >>> 16 & 255));
		int green = Math.round(Mth.lerp(clamped, from >>> 8 & 255, to >>> 8 & 255));
		int blue = Math.round(Mth.lerp(clamped, from & 255, to & 255));
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	private static int withAlpha(int color, float alpha) {
		int scaledAlpha = Math.max(0, Math.min(255, Math.round(((color >> 24) & 255) * alpha)));
		return (color & 0x00FFFFFF) | (scaledAlpha << 24);
	}

	private static float channel(int color, int shift) {
		return ((color >> shift) & 255) / 255.0F;
	}

	public enum EspMode {
		THREE_D("3D"),
		TWO_D("2D");

		private final String display;

		EspMode(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

}
