package dev.Mardexx.client.modules.impl.Combat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class TargetEspRenderer {
	private static final float[][] OCTAHEDRON_VERTICES = {
			{0.0F, 1.5F, 0.0F},
			{0.0F, -1.5F, 0.0F},
			{1.0F, 0.0F, 0.0F},
			{-1.0F, 0.0F, 0.0F},
			{0.0F, 0.0F, 1.0F},
			{0.0F, 0.0F, -1.0F}
	};
	private static final int[][] OCTAHEDRON_FACES = {
			{0, 4, 2}, {0, 3, 4}, {0, 5, 3}, {0, 2, 5},
			{1, 2, 4}, {1, 4, 3}, {1, 3, 5}, {1, 5, 2}
	};
	private static final float[] FACE_BRIGHTNESS = {1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F};

	private TargetEspRenderer() {
	}

	static void render(WorldRenderContext context, LivingEntity target, TriggerBot.TargetEspMode mode, int color, float progress) {
		float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
		double x = Mth.lerp(tickDelta, target.xOld, target.getX());
		double y = Mth.lerp(tickDelta, target.yOld, target.getY());
		double z = Mth.lerp(tickDelta, target.zOld, target.getZ());
		Vec3 camera = context.camera().getPosition();
		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);

		Matrix4f matrix = poseStack.last().pose();
		if (mode == TriggerBot.TargetEspMode.BUBBLE) {
			BubbleEspRenderer.renderBubble(context, target, 1.0F, progress);
		} else if (mode == TriggerBot.TargetEspMode.CIRCLE) {
			drawCircle(matrix, target, x, y, z, color, progress);
		} else {
			drawSpheres(matrix, target, x, y, z, color, progress);
		}

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private static void drawSpheres(Matrix4f matrix, LivingEntity target, double x, double y, double z, int color, float progress) {
		float moving = (System.currentTimeMillis() % 360_000L) / 2.5F + progress;
		float ringWidth = target.getBbWidth() * 1.5F;
		float ringScale = 1.25F - 0.5F * progress;
		drawSpherePass(matrix, target, x, y, z, moving, ringWidth, ringScale, 0.16F, withAlpha(color, progress * 0.12F));
		drawSpherePass(matrix, target, x, y, z, moving, ringWidth, ringScale, 0.10F, withAlpha(color, progress));
	}

	private static void drawSpherePass(Matrix4f matrix, LivingEntity target, double x, double y, double z,
			float moving, float ringWidth, float ringScale, float size, int color) {
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (int degrees = 0; degrees < 360; degrees += 20) {
			float angle = (float) Math.toRadians(degrees + moving * 0.3F);
			float offsetX = (float) Math.sin(angle) * ringWidth * ringScale;
			float offsetZ = (float) Math.cos(angle) * ringWidth * ringScale;
			float offsetY = 0.1F + target.getBbHeight() * Math.abs((float) Math.sin(degrees));
			float crystalX = (float) x + offsetX;
			float crystalY = (float) y + offsetY;
			float crystalZ = (float) z + offsetZ;
			drawOctahedron(buffer, matrix, crystalX, crystalY, crystalZ, size, color);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void drawOctahedron(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float scale, int color) {
		for (int faceIndex = 0; faceIndex < OCTAHEDRON_FACES.length; faceIndex++) {
			int shaded = shade(color, FACE_BRIGHTNESS[faceIndex]);
			for (int vertexIndex : OCTAHEDRON_FACES[faceIndex]) {
				float[] vertex = OCTAHEDRON_VERTICES[vertexIndex];
				buffer.addVertex(matrix, x + vertex[0] * scale, y + vertex[1] * scale, z + vertex[2] * scale).setColor(shaded);
			}
		}
	}

	private static void drawCircle(Matrix4f matrix, LivingEntity target, double x, double y, double z, int color, float progress) {
		float height = target.getBbHeight() + 0.15F;
		float radius = target.getBbWidth() * 0.8F;
		double time = System.currentTimeMillis() % 1750.0D;
		boolean inverted = time > 875.0D;
		double cycle = time / 875.0D;
		double raw = inverted ? cycle - 1.0D : 1.0D - cycle;
		double eased = raw < 0.5D ? 2.0D * raw * raw : 1.0D - Math.pow(-2.0D * raw + 2.0D, 2.0D) / 2.0D;
		float ringY = (float) y + height * (float) eased;
		float skirtOffset = height * 0.8F * (float) Math.min(eased, 1.0D - eased) * (inverted ? -1.0F : 1.0F);
		int ringColor = withAlpha(color, progress);
		int skirtColor = withAlpha(color, progress * 0.55F);

		BufferBuilder skirt = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int degrees = 0; degrees <= 360; degrees++) {
			double angle = Math.toRadians(degrees);
			float pointX = (float) (x + Math.cos(angle) * radius);
			float pointZ = (float) (z + Math.sin(angle) * radius);
			skirt.addVertex(matrix, pointX, ringY, pointZ).setColor(skirtColor);
			skirt.addVertex(matrix, pointX, ringY + skirtOffset, pointZ).setColor(withAlpha(color, 0.0F));
		}
		BufferUploader.drawWithShader(skirt.buildOrThrow());

		BufferBuilder outline = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		for (int degrees = 0; degrees < 360; degrees++) {
			double first = Math.toRadians(degrees);
			double second = Math.toRadians(degrees + 1);
			outline.addVertex(matrix, (float) (x + Math.cos(first) * radius), ringY, (float) (z + Math.sin(first) * radius)).setColor(ringColor);
			outline.addVertex(matrix, (float) (x + Math.cos(second) * radius), ringY, (float) (z + Math.sin(second) * radius)).setColor(ringColor);
		}
		BufferUploader.drawWithShader(outline.buildOrThrow());
	}

	private static int shade(int color, float brightness) {
		int alpha = color >>> 24;
		int red = Math.min(255, Math.round(((color >>> 16) & 255) * brightness));
		int green = Math.min(255, Math.round(((color >>> 8) & 255) * brightness));
		int blue = Math.min(255, Math.round((color & 255) * brightness));
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	private static int withAlpha(int color, float multiplier) {
		int alpha = Math.round(((color >>> 24) & 255) * Mth.clamp(multiplier, 0.0F, 1.0F));
		return alpha << 24 | color & 0x00FFFFFF;
	}
}
