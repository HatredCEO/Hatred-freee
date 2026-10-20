package dev.Mardexx.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.opengl.GL11;

public final class ChamsRenderTypes extends RenderType {
	private static final ThreadLocal<Boolean> ENTITY_CHAMS = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Boolean> ENTITY_THROUGH_WALLS = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Boolean> ENTITY_ARMOR_THROUGH_WALLS = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Boolean> ENTITY_PLAYER = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Boolean> DYNAMIC_OCCLUSION = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Integer> ENTITY_VISIBLE_COLOR = ThreadLocal.withInitial(() -> 0xFFFFFFFF);
	private static final ThreadLocal<Integer> ENTITY_HIDDEN_COLOR = ThreadLocal.withInitial(() -> 0xFFFFFFFF);
	private static final ThreadLocal<Float> ENTITY_VISIBLE_INTENSITY = ThreadLocal.withInitial(() -> 0.0F);
	private static final ThreadLocal<Float> ENTITY_HIDDEN_INTENSITY = ThreadLocal.withInitial(() -> 0.0F);
	private static final ThreadLocal<Boolean> ENTITY_FULL_BRIGHT = ThreadLocal.withInitial(() -> true);
	private static final ThreadLocal<LivingEntity> CURRENT_RENDERED_ENTITY = new ThreadLocal<>();
	private static final ThreadLocal<ResourceLocation> CURRENT_ARMOR_TEXTURE = new ThreadLocal<>();
	private static final LayeringStateShard HIDDEN_DEPTH_RANGE = new LayeringStateShard(
			"hatred_hidden_depth_range",
			() -> GL11.glDepthRange(0.0D, 0.01D),
			() -> GL11.glDepthRange(0.0D, 1.0D)
	);
	private static final ShaderProgram CHAMS_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/entity_chams"),
			DefaultVertexFormat.NEW_ENTITY,
			ShaderDefines.EMPTY
	);
	private static final ShaderProgram CHAMS_DYNAMIC_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/entity_chams_dynamic"),
			DefaultVertexFormat.NEW_ENTITY,
			ShaderDefines.EMPTY
	);

	private ChamsRenderTypes() {
		super(null, null, null, 0, false, false, null, null);
	}

	public static RenderType entityChams(ResourceLocation texture) {
		return entityChams(texture, true, 0xFFFFFFFF);
	}

	public static RenderType entityChams(ResourceLocation texture, boolean throughWalls, int hiddenColor) {
		return chams(texture, throughWalls, hiddenColor, false, false);
	}

	public static RenderType armorChams(ResourceLocation texture, boolean throughWalls, int hiddenColor) {
		return chams(texture, throughWalls, hiddenColor, true, false);
	}

	public static RenderType layerChams(ResourceLocation texture, boolean throughWalls, int hiddenColor) {
		return chams(texture, throughWalls, hiddenColor, false, false);
	}

	public static RenderType entityGlowChams(ResourceLocation texture, boolean throughWalls, int hiddenColor) {
		return entityChams(texture, throughWalls, hiddenColor);
	}

	public static RenderType armorGlowChams(ResourceLocation texture, boolean throughWalls, int hiddenColor) {
		return armorChams(texture, throughWalls, hiddenColor);
	}

	public static RenderType itemChams(ResourceLocation texture, boolean throughWalls, boolean ownerBehindWall, int hiddenColor) {
		if (throughWalls && DYNAMIC_OCCLUSION.get()) {
			return chams(texture, true, hiddenColor, false, false, true);
		}

		return create(
				"hatred_entity_chams_item_" + (ownerBehindWall ? "hidden" : "visible"),
				DefaultVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS,
				1536,
				true,
				true,
				CompositeState.builder()
						.setShaderState(new ShaderStateShard(CHAMS_SHADER))
						.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
						.setTransparencyState(NO_TRANSPARENCY)
						.setDepthTestState(LEQUAL_DEPTH_TEST)
						.setCullState(NO_CULL)
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.setTexturingState(chamsUniforms(false, ownerBehindWall, hiddenColor, false))
						.setLayeringState(throughWalls ? HIDDEN_DEPTH_RANGE : NO_LAYERING)
						.setWriteMaskState(COLOR_DEPTH_WRITE)
						.setOutputState(MAIN_TARGET)
						.createCompositeState(false)
		);
	}

	private static RenderType chams(ResourceLocation texture, boolean throughWalls, int hiddenColor, boolean armorLayer, boolean glowTarget) {
		return chams(texture, throughWalls, hiddenColor, armorLayer, glowTarget, false);
	}

	private static RenderType chams(ResourceLocation texture, boolean throughWalls, int hiddenColor, boolean armorLayer, boolean glowTarget, boolean itemLayer) {
		boolean dynamic = throughWalls && DYNAMIC_OCCLUSION.get();
		ShaderProgram shader = dynamic ? CHAMS_DYNAMIC_SHADER : CHAMS_SHADER;
		return create(
				renderTypeName(throughWalls, hiddenColor, armorLayer) + (glowTarget ? "_glow" : "") + (itemLayer ? "_item" : ""),
				DefaultVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS,
				1536,
				true,
				true,
				CompositeState.builder()
						.setShaderState(new ShaderStateShard(shader))
						.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
						.setTransparencyState(NO_TRANSPARENCY)
						.setDepthTestState(LEQUAL_DEPTH_TEST)
						.setCullState(NO_CULL)
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.setTexturingState(chamsUniforms(dynamic, throughWalls, hiddenColor, glowTarget))
						.setLayeringState(throughWalls ? HIDDEN_DEPTH_RANGE : NO_LAYERING)
						.setWriteMaskState(COLOR_DEPTH_WRITE)
						.setOutputState(MAIN_TARGET)
						.createCompositeState(false)
		);
	}

	private static boolean shouldCullHiddenFaces(boolean armorLayer) {
		return false;
	}

	private static String renderTypeName(boolean throughWalls, int hiddenColor, boolean armorLayer) {
		if (!throughWalls) {
			return armorLayer ? "hatred_armor_chams" : "hatred_entity_chams";
		}

		return armorLayer ? "hatred_armor_chams_hidden" : "hatred_entity_chams_hidden";
	}

	private static TexturingStateShard chamsUniforms(boolean dynamic, boolean hidden, int hiddenColor, boolean glowTarget) {
		return new TexturingStateShard(
				"hatred_chams_uniforms",
				() -> {
					if (dynamic) {
						RenderSystem.setShaderTexture(3, ChamsDepthCapture.textureId());
					}
					CompiledShaderProgram shader = RenderSystem.getShader();
					if (shader == null) {
						return;
					}

					dev.Mardexx.client.modules.impl.Render.WallHack wallHack = dev.Mardexx.client.modules.impl.Render.WallHack.getInstance();
					boolean fullBright = wallHack != null && wallHack.enabled() && wallHack.fullBright();
					int visibleColor = 0xFFFFFFFF;
					int hColor = 0xFFFFFFFF;
					float visibleIntensity = 0.0F;
					float hiddenIntensity = 0.0F;

					shader.safeGetUniform("FullBright").set(fullBright ? 1.0F : 0.0F);
					if (dynamic) {
						setColor(shader, "VisibleChamsColor", visibleColor);
						setColor(shader, "HiddenChamsColor", hColor);
						shader.safeGetUniform("VisibleOverlayStrength").set(visibleIntensity);
						shader.safeGetUniform("HiddenOverlayStrength").set(hiddenIntensity);
						return;
					}

					setColor(shader, "ChamsColor", hidden ? hColor : visibleColor);
					shader.safeGetUniform("OverlayStrength").set(hidden ? hiddenIntensity : visibleIntensity);
				},
				() -> {
					if (dynamic) {
						RenderSystem.setShaderTexture(3, 0);
					}
				}
		);
	}

	private static void setColor(CompiledShaderProgram shader, String uniformName, int color) {
		shader.safeGetUniform(uniformName).set(
				((color >>> 16) & 255) / 255.0F,
				((color >>> 8) & 255) / 255.0F,
				(color & 255) / 255.0F,
				1.0F
		);
	}

	public static boolean entityChamsActive() {
		return ENTITY_CHAMS.get();
	}

	public static boolean entityThroughWalls() {
		return ENTITY_THROUGH_WALLS.get();
	}

	public static int entityHiddenColor() {
		return ENTITY_HIDDEN_COLOR.get();
	}

	public static boolean armorChamsActive() {
		return entityChamsActive() && (!entityThroughWalls() || ENTITY_ARMOR_THROUGH_WALLS.get());
	}

	public static boolean layerChamsActive() {
		return entityChamsActive() && !ENTITY_PLAYER.get();
	}

	public static boolean layerThroughWalls() {
		return entityThroughWalls();
	}

	public static int layerHiddenColor() {
		return entityHiddenColor();
	}

	public static boolean armorThroughWalls() {
		return entityThroughWalls() && ENTITY_ARMOR_THROUGH_WALLS.get();
	}

	public static int armorHiddenColor() {
		return entityHiddenColor();
	}

	public static LivingEntity currentRenderedEntity() {
		return CURRENT_RENDERED_ENTITY.get();
	}

	public static void setCurrentArmorTexture(ResourceLocation texture) {
		CURRENT_ARMOR_TEXTURE.set(texture);
	}

	public static ResourceLocation currentArmorTexture() {
		return CURRENT_ARMOR_TEXTURE.get();
	}

	public static boolean dynamicOcclusionActive() {
		return DYNAMIC_OCCLUSION.get();
	}

	public static void setDynamicOcclusion(boolean enabled) {
		DYNAMIC_OCCLUSION.set(enabled);
	}

	public static void setEntityChams(boolean enabled, boolean throughWalls, boolean armorThroughWalls, boolean playerEntity, int visibleColor, int hiddenColor, float visibleIntensity, float hiddenIntensity, boolean fullBright, LivingEntity currentEntity) {
		ENTITY_CHAMS.set(enabled);
		ENTITY_THROUGH_WALLS.set(throughWalls);
		ENTITY_ARMOR_THROUGH_WALLS.set(armorThroughWalls);
		ENTITY_PLAYER.set(playerEntity);
		ENTITY_VISIBLE_COLOR.set(visibleColor);
		ENTITY_HIDDEN_COLOR.set(hiddenColor);
		ENTITY_VISIBLE_INTENSITY.set(visibleIntensity);
		ENTITY_HIDDEN_INTENSITY.set(hiddenIntensity);
		ENTITY_FULL_BRIGHT.set(fullBright);
		CURRENT_RENDERED_ENTITY.set(currentEntity);
	}

	public static void clearEntityChams() {
		ENTITY_CHAMS.set(false);
		ENTITY_THROUGH_WALLS.set(false);
		ENTITY_ARMOR_THROUGH_WALLS.set(false);
		ENTITY_PLAYER.set(false);
		DYNAMIC_OCCLUSION.set(false);
		ENTITY_VISIBLE_COLOR.set(0xFF00FF40);
		ENTITY_HIDDEN_COLOR.set(0xFFFF0D05);
		ENTITY_VISIBLE_INTENSITY.set(0.45F);
		ENTITY_HIDDEN_INTENSITY.set(0.55F);
		ENTITY_FULL_BRIGHT.set(true);
		CURRENT_RENDERED_ENTITY.remove();
		CURRENT_ARMOR_TEXTURE.remove();
	}
}
