package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.utils.render.ProjectionUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.ColorSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Tracers extends Module implements QuickImports {
	private static final Tracers INSTANCE = new Tracers();
	private static final float LINE_WIDTH = 4.8F;

	private final EnumBooleanMapSetting<EntityTargetFilter> targets = EntityTargeting.createTargetsSetting("Selects which entities receive tracers.");
	private final BooleanSetting drawSpine = new BooleanSetting("draw_line", "Draw Line", "Draws a head-to-feet spine on the target.", true);
	private final EnumSetting<AimTarget> aimTarget = new EnumSetting<>("aim_target", "Aim Target", "Target point used by the tracer.", AimTarget.TORSO);
	private final ColorSetting playerColor = new ColorSetting("player_color", "Player Color", "Tracer color used for players.", 0xFFFFFFFF);
	private final ColorSetting friendColor = new ColorSetting("friend_color", "Friend Color", "Tracer color used for friends.", 0xFF55FF55);
	private final ColorSetting mobColor = new ColorSetting("mob_color", "Mob Color", "Tracer color used for hostile mobs.", 0xFFFFFFFF);
	private final ColorSetting animalColor = new ColorSetting("animal_color", "Animal Color", "Tracer color used for animals.", 0xFFFFFFFF);
	private List<Entity> capturedTargets = List.of();

	private Tracers() {
		super("Tracers", ModuleCategory.RENDER, "Draws screen-space tracers to selected entities.");
		register(targets, drawSpine, aimTarget, playerColor, friendColor, mobColor, animalColor);
		Render2D.add(this::renderHud);
	}

	public static Tracers getInstance() {
		return INSTANCE;
	}

	@Override
	public void onTick() {
		if (!enabled() || mc.level == null || mc.player == null) {
			capturedTargets = List.of();
			return;
		}

		List<Entity> entities = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!EntityTargeting.matches(entity, targets, false) || entity.isRemoved()) {
				continue;
			}
			entities.add(entity);
		}
		capturedTargets = entities;
	}

	private void renderHud(GuiGraphics guiGraphics) {
		if (!enabled() || mc.level == null || mc.player == null || capturedTargets.isEmpty()) {
			return;
		}

		float centerX = mc.getWindow().getGuiScaledWidth() / 2.0F;
		float centerY = mc.getWindow().getGuiScaledHeight() / 2.0F;
		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		boolean hasVertices = false;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);
		RenderSystem.lineWidth(LINE_WIDTH);
		float tickDelta = Render2D.tickDelta();

		for (Entity entity : capturedTargets) {
			if (entity.isRemoved()) {
				continue;
			}

			double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
			double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
			double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
			double headY = y + entity.getEyeHeight();
			double targetY = switch (aimTarget.value()) {
				case HEAD -> headY;
				case TORSO -> y + entity.getEyeHeight() * 0.5D;
				case LEGS -> y;
			};

			Optional<Vector2f> targetProjection = ProjectionUtil.worldToScreen(new Vec3(x, targetY, z), true);
			if (targetProjection.isEmpty()) {
				continue;
			}

			int color = color(entity);
			Vector2f point = targetProjection.get();
			buffer.addVertex(matrix, centerX, centerY, 0.0F).setColor(color);
			buffer.addVertex(matrix, point.x, point.y, 0.0F).setColor(color);
			hasVertices = true;

			if (!drawSpine.enabled()) {
				continue;
			}

			Optional<Vector2f> headProjection = ProjectionUtil.worldToScreen(new Vec3(x, headY, z), false);
			Optional<Vector2f> feetProjection = ProjectionUtil.worldToScreen(new Vec3(x, y, z), false);
			if (headProjection.isEmpty() || feetProjection.isEmpty()) {
				continue;
			}

			Vector2f head = headProjection.get();
			Vector2f feet = feetProjection.get();
			buffer.addVertex(matrix, head.x, head.y, 0.0F).setColor(color);
			buffer.addVertex(matrix, feet.x, feet.y, 0.0F).setColor(color);
		}

		if (hasVertices) {
			BufferUploader.drawWithShader(buffer.buildOrThrow());
		}

		RenderSystem.lineWidth(1.0F);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	private int color(Entity entity) {
		EntityTargetFilter category = EntityTargeting.category(entity);
		if (category == null) {
			return playerColor.color();
		}

		return switch (category) {
			case PLAYERS -> playerColor.color();
			case FRIENDS -> friendColor.color();
			case MOBS -> mobColor.color();
			case ANIMALS -> animalColor.color();
		};
	}

	public enum AimTarget {
		HEAD("Head"),
		TORSO("Torso"),
		LEGS("Legs");

		private final String display;

		AimTarget(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

}
