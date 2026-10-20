package dev.Mardexx.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.client.modules.impl.Render.AuroraSky;
import dev.Mardexx.client.theme.ThemeManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public final class AuroraSkyRenderer {
	private static final Logger LOGGER = LoggerFactory.getLogger("ShaderSkyDiagnostics");
	private static final ShaderProgram AURORA_SKY_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/aurora_sky"),
			DefaultVertexFormat.POSITION_COLOR,
			ShaderDefines.EMPTY
	);
	private static boolean resourceReloadPending;
	private static boolean diagnosticsPending = true;
	private static int resourceReloadGeneration;
	private static String diagnosticReason = "initial-render";
	private static String lastDimension = "";
	private static long animationStartNanos = -1L;

	private AuroraSkyRenderer() {
	}

	public static void render(WorldRenderContext context) {
		AuroraSky module = AuroraSky.getInstance();
		Minecraft minecraft = Minecraft.getInstance();
		if (!module.enabled()) {
			animationStartNanos = -1L;
			return;
		}
		if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
			return;
		}
		if (animationStartNanos < 0L) {
			animationStartNanos = System.nanoTime();
		}
		String dimension = minecraft.level.dimension().location().toString();
		if (!dimension.equals(lastDimension)) {
			lastDimension = dimension;
			diagnosticsPending = true;
			diagnosticReason = "dimension-change";
		}
		if (resourceReloadPending) {
			resourceReloadPending = false;
			LOGGER.info("[ShaderSky/diag] generation={} skipped first frame after resource reload", resourceReloadGeneration);
			return;
		}

		CompiledShaderProgram shader = RenderSystem.setShader(AURORA_SKY_SHADER);
		if (shader == null) {
			if (diagnosticsPending) {
				LOGGER.error("[ShaderSky/diag] generation={} reason={} shader program is unavailable", resourceReloadGeneration, diagnosticReason);
				diagnosticsPending = false;
			}
			restoreRenderState();
			return;
		}

		Matrix4f projection = context.projectionMatrix();
		Vector3f cameraForward = new Vector3f(context.camera().getLookVector()).normalize();
		Vector3f cameraRight = new Vector3f(context.camera().getLeftVector()).negate().normalize();
		Vector3f cameraUp = new Vector3f(context.camera().getUpVector()).normalize();
		Uniform timeUniform = shader.getUniform("Time");
		Uniform projectionScaleUniform = shader.getUniform("ProjectionScale");
		Uniform cameraForwardUniform = shader.getUniform("CameraForward");
		Uniform cameraRightUniform = shader.getUniform("CameraRight");
		Uniform cameraUpUniform = shader.getUniform("CameraUp");
		Uniform farDepthUniform = shader.getUniform("FarDepth");
		Uniform intensityUniform = shader.getUniform("Intensity");
		Uniform speedUniform = shader.getUniform("Speed");
		Uniform themeColorUniform = shader.getUniform("ThemeColor");
		Uniform scaleUniform = shader.getUniform("Scale");
		Uniform heightUniform = shader.getUniform("AuroraHeight");
		Uniform curtainWidthUniform = shader.getUniform("CurtainWidth");
		Uniform curtainCountUniform = shader.getUniform("CurtainCount");
		Uniform starStrengthUniform = shader.getUniform("StarStrength");
		Uniform skyTintUniform = shader.getUniform("SkyTint");
		Uniform opacityUniform = shader.getUniform("Opacity");
		Uniform styleUniform = shader.getUniform("Style");
		if (diagnosticsPending) {
			logDiagnostics(
					context,
					minecraft,
					module,
					dimension,
					cameraForward,
					cameraRight,
					cameraUp,
					missingUniforms(
							timeUniform, projectionScaleUniform, cameraForwardUniform,
							cameraRightUniform, cameraUpUniform, farDepthUniform, intensityUniform, speedUniform,
							themeColorUniform, scaleUniform, heightUniform, curtainWidthUniform,
							curtainCountUniform, starStrengthUniform, skyTintUniform, opacityUniform, styleUniform
					)
			);
			diagnosticsPending = false;
		}
		if (timeUniform != null) {
			// Never derive shader time from level time: servers replace it during
			// transfers, and large float values destroy trigonometric precision.
			float animationSeconds = (float) (((System.nanoTime() - animationStartNanos) / 1_000_000_000.0) % 4096.0);
			timeUniform.set(animationSeconds);
		}
		if (projectionScaleUniform != null) {
			projectionScaleUniform.set(projection.m00(), projection.m11());
		}
		if (cameraForwardUniform != null) {
			cameraForwardUniform.set(cameraForward.x, cameraForward.y, cameraForward.z);
		}
		if (cameraRightUniform != null) {
			cameraRightUniform.set(cameraRight.x, cameraRight.y, cameraRight.z);
		}
		if (cameraUpUniform != null) {
			cameraUpUniform.set(cameraUp.x, cameraUp.y, cameraUp.z);
		}
		if (farDepthUniform != null) {
			farDepthUniform.set(1.0F);
		}
		if (intensityUniform != null) {
			intensityUniform.set(module.intensity());
		}
		if (speedUniform != null) {
			speedUniform.set(module.speed());
		}
		if (scaleUniform != null) {
			scaleUniform.set(module.scale());
		}
		if (heightUniform != null) {
			heightUniform.set(module.height());
		}
		if (curtainWidthUniform != null) {
			curtainWidthUniform.set(module.curtainWidth());
		}
		if (curtainCountUniform != null) {
			curtainCountUniform.set(module.curtainCount());
		}
		if (starStrengthUniform != null) {
			starStrengthUniform.set(module.stars() ? 1.0F : 0.0F);
		}
		if (skyTintUniform != null) {
			skyTintUniform.set(module.skyTint());
		}
		if (opacityUniform != null) {
			opacityUniform.set(module.opacity());
		}
		if (styleUniform != null) {
			styleUniform.set((float) module.shaderStyle());
		}
		if (themeColorUniform != null) {
			int themeColor = ThemeManager.accent();
			themeColorUniform.set(
					((themeColor >>> 16) & 255) / 255.0F,
					((themeColor >>> 8) & 255) / 255.0F,
					(themeColor & 255) / 255.0F
			);
		}

		Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
		ProjectionType savedProjectionType = RenderSystem.getProjectionType();
		try {
			RenderSystem.setProjectionMatrix(new Matrix4f(), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_EQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();

			BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F).setColor(0xFFFFFFFF);
			bufferBuilder.addVertex(1.0F, -1.0F, 1.0F).setColor(0xFFFFFFFF);
			bufferBuilder.addVertex(1.0F, 1.0F, 1.0F).setColor(0xFFFFFFFF);
			bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F).setColor(0xFFFFFFFF);
			BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		} finally {
			RenderSystem.setProjectionMatrix(savedProjection, savedProjectionType);
			restoreRenderState();
		}
	}

	public static void onResourceReload() {
		resourceReloadGeneration++;
		resourceReloadPending = true;
		diagnosticsPending = true;
		diagnosticReason = "resource-reload";
		LOGGER.info("[ShaderSky/diag] generation={} resource reload callback received", resourceReloadGeneration);
	}

	private static List<String> missingUniforms(Uniform... uniforms) {
		String[] names = {
				"Time", "ProjectionScale", "CameraForward", "CameraRight", "CameraUp", "FarDepth",
				"Intensity", "Speed", "ThemeColor", "Scale", "AuroraHeight", "CurtainWidth",
				"CurtainCount", "StarStrength", "SkyTint", "Opacity", "Style"
		};
		List<String> missing = new ArrayList<>();
		for (int i = 0; i < uniforms.length; i++) {
			if (uniforms[i] == null) {
				missing.add(names[i]);
			}
		}
		return missing;
	}

	private static void logDiagnostics(
			WorldRenderContext context,
			Minecraft minecraft,
			AuroraSky module,
			String dimension,
			Vector3f cameraForward,
			Vector3f cameraRight,
			Vector3f cameraUp,
			List<String> missingUniforms
	) {
		Matrix4f projection = context.projectionMatrix();
		Matrix4f view = context.positionMatrix();
		String server = minecraft.getCurrentServer() == null ? "singleplayer" : minecraft.getCurrentServer().ip;
		LOGGER.info(
				"[ShaderSky/diag] generation={} reason={} server={} dimension={} target={}x{} window={}x{} advancedTranslucency={}",
				resourceReloadGeneration,
				diagnosticReason,
				server,
				dimension,
				minecraft.getMainRenderTarget().width,
				minecraft.getMainRenderTarget().height,
				minecraft.getWindow().getWidth(),
				minecraft.getWindow().getHeight(),
				context.advancedTranslucency()
		);
		LOGGER.info(
				"[ShaderSky/diag] settings style={} intensity={} speed={} scale={} height={} width={} count={} stars={} tint={} opacity={}",
				module.shaderStyle(), module.intensity(), module.speed(), module.scale(), module.height(),
				module.curtainWidth(), module.curtainCount(), module.stars(), module.skyTint(), module.opacity()
		);
		LOGGER.info(
				"[ShaderSky/diag] projection m00={} m11={} m22={} m23={} m32={} m33={} det={} farDepth=1.0",
				projection.m00(), projection.m11(), projection.m22(), projection.m23(), projection.m32(), projection.m33(),
				projection.determinant()
		);
		LOGGER.info(
				"[ShaderSky/diag] view m00={} m01={} m02={} m10={} m11={} m12={} m20={} m21={} m22={} det={} cameraRot=({}, {}, {}, {}) basisForward=({}, {}, {}) basisRight=({}, {}, {}) basisUp=({}, {}, {}) cameraPos=({}, {}, {})",
				view.m00(), view.m01(), view.m02(), view.m10(), view.m11(), view.m12(), view.m20(), view.m21(), view.m22(),
				view.determinant(),
				context.camera().rotation().x, context.camera().rotation().y, context.camera().rotation().z, context.camera().rotation().w,
				cameraForward.x, cameraForward.y, cameraForward.z,
				cameraRight.x, cameraRight.y, cameraRight.z,
				cameraUp.x, cameraUp.y, cameraUp.z,
				context.camera().getPosition().x, context.camera().getPosition().y, context.camera().getPosition().z
		);
		if (missingUniforms.isEmpty()) {
			LOGGER.info("[ShaderSky/diag] shader uniforms complete");
		} else {
			LOGGER.warn("[ShaderSky/diag] missing shader uniforms: {}", missingUniforms);
		}
	}

	private static void restoreRenderState() {
		RenderSystem.clearShader();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.depthMask(true);
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
	}
}
