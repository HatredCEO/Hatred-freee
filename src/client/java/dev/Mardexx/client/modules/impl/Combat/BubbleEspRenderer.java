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
import org.joml.Vector3f;

public final class BubbleEspRenderer {
	private static final int LATITUDE_SEGMENTS = 36;
	private static final int LONGITUDE_SEGMENTS = 48;
	private static final float PI = (float) Math.PI;
	private static final float TWO_PI = (float) (Math.PI * 2.0);

	// Light direction for specular highlight
	private static final Vector3f LIGHT_DIR = new Vector3f(0.577F, 0.707F, 0.408F).normalize();

	private BubbleEspRenderer() {
	}

	public static void renderBubble(WorldRenderContext context, LivingEntity target, float scaleMultiplier, float progress) {
		if (target == null || progress <= 0.001F) {
			return;
		}

		float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
		double interpX = Mth.lerp(tickDelta, target.xOld, target.getX());
		double interpY = Mth.lerp(tickDelta, target.yOld, target.getY());
		double interpZ = Mth.lerp(tickDelta, target.zOld, target.getZ());

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

		float time = (float) (System.currentTimeMillis() % 1_000_000L) * 0.0025F;
		double centerX = interpX;
		double centerY = interpY + target.getBbHeight() * 0.5D;
		double centerZ = interpZ;

		float baseRadiusXZ = (target.getBbWidth() * 0.85F + 0.3F) * scaleMultiplier;
		float baseRadiusY = (target.getBbHeight() * 0.55F + 0.25F) * scaleMultiplier;

		drawBubbleSphere(matrix, camera, centerX, centerY, centerZ, baseRadiusXZ, baseRadiusY, time, progress);
		drawOrbitingBubbles(matrix, camera, centerX, centerY, centerZ, baseRadiusXZ, baseRadiusY, time, progress);

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private static void drawBubbleSphere(Matrix4f matrix, Vec3 camera, double cx, double cy, double cz,
			float rxz, float ry, float time, float progress) {
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		// Generate sphere vertices grid with thin-film interference
		int totalVerts = (LATITUDE_SEGMENTS + 1) * (LONGITUDE_SEGMENTS + 1);
		float[] vx = new float[totalVerts];
		float[] vy = new float[totalVerts];
		float[] vz = new float[totalVerts];
		int[] colors = new int[totalVerts];

		int index = 0;
		for (int lat = 0; lat <= LATITUDE_SEGMENTS; lat++) {
			float theta = (float) lat / LATITUDE_SEGMENTS * PI;
			float sinTheta = Mth.sin(theta);
			float cosTheta = Mth.cos(theta);

			for (int lon = 0; lon <= LONGITUDE_SEGMENTS; lon++) {
				float phi = (float) lon / LONGITUDE_SEGMENTS * TWO_PI;
				float sinPhi = Mth.sin(phi);
				float cosPhi = Mth.cos(phi);

				// Fluid dynamic wobble (surface tension vibration)
				float wobble = 1.0F
						+ 0.035F * Mth.sin(3.0F * theta + time * 2.5F) * Mth.cos(4.0F * phi - time * 1.8F)
						+ 0.020F * Mth.sin(5.0F * phi + time * 3.2F) * sinTheta
						+ 0.015F * Mth.cos(2.0F * theta - time * 2.0F);

				float normX = sinTheta * cosPhi;
				float normY = cosTheta;
				float normZ = sinTheta * sinPhi;

				float px = (float) cx + normX * rxz * wobble;
				float py = (float) cy + normY * ry * wobble;
				float pz = (float) cz + normZ * rxz * wobble;

				vx[index] = px;
				vy[index] = py;
				vz[index] = pz;

				// View vector from vertex to camera
				float viewX = (float) camera.x - px;
				float viewY = (float) camera.y - py;
				float viewZ = (float) camera.z - pz;
				float viewLen = (float) Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
				if (viewLen > 1.0E-5F) {
					viewX /= viewLen;
					viewY /= viewLen;
					viewZ /= viewLen;
				}

				// Incident cosine angle
				float cosIncidence = Math.abs(normX * viewX + normY * viewY + normZ * viewZ);

				// Fast Thin-Film Interference (cornusammonis ld3SDl)
				colors[index] = computeThinFilmColor(theta, phi, cosIncidence, normX, normY, normZ, viewX, viewY, viewZ, time, progress);
				index++;
			}
		}

		// Build triangle mesh
		for (int lat = 0; lat < LATITUDE_SEGMENTS; lat++) {
			for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
				int first = (lat * (LONGITUDE_SEGMENTS + 1)) + lon;
				int second = first + LONGITUDE_SEGMENTS + 1;

				// Triangle 1
				buffer.addVertex(matrix, vx[first], vy[first], vz[first]).setColor(colors[first]);
				buffer.addVertex(matrix, vx[second], vy[second], vz[second]).setColor(colors[second]);
				buffer.addVertex(matrix, vx[first + 1], vy[first + 1], vz[first + 1]).setColor(colors[first + 1]);

				// Triangle 2
				buffer.addVertex(matrix, vx[first + 1], vy[first + 1], vz[first + 1]).setColor(colors[first + 1]);
				buffer.addVertex(matrix, vx[second], vy[second], vz[second]).setColor(colors[second]);
				buffer.addVertex(matrix, vx[second + 1], vy[second + 1], vz[second + 1]).setColor(colors[second + 1]);
			}
		}

		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static int computeThinFilmColor(float theta, float phi, float cosTheta,
			float nx, float ny, float nz, float vx, float vy, float vz, float time, float progress) {
		// Soap bubble film thickness variation across surface (in nanometers: 350nm - 750nm)
		float thickness = 420.0F + 260.0F * (
				Mth.sin(theta * 3.5F + time * 1.3F) * Mth.cos(phi * 2.8F - time * 0.9F)
				+ Mth.sin(phi * 4.0F + time * 1.8F) * 0.4F
				+ Mth.cos(theta * 5.0F - time * 1.1F) * 0.3F
		);

		float IOR = 1.333F; // Soap film refractive index
		// Optical Path Difference: 2 * n * d * cos(theta_t)
		float sinThetaSq = Math.max(0.0F, 1.0F - cosTheta * cosTheta);
		float cosThetaT = (float) Math.sqrt(Math.max(0.0F, 1.0F - sinThetaSq / (IOR * IOR)));
		float opd = 2.0F * IOR * thickness * cosThetaT;

		// Airy spectral wave interference for Red (650nm), Green (532nm), Blue (440nm)
		float phaseR = (TWO_PI * opd) / 650.0F;
		float phaseG = (TWO_PI * opd) / 532.0F;
		float phaseB = (TWO_PI * opd) / 440.0F;

		float r = 0.5F + 0.5F * Mth.cos(phaseR);
		float g = 0.5F + 0.5F * Mth.cos(phaseG);
		float b = 0.5F + 0.5F * Mth.cos(phaseB);

		// Fresnel rim reflection (Schlick approximation)
		float fresnel = (float) Math.pow(1.0F - cosTheta, 2.6F);

		// Blinn-Phong specular highlight for glassy bubble reflection
		float hx = LIGHT_DIR.x + vx;
		float hy = LIGHT_DIR.y + vy;
		float hz = LIGHT_DIR.z + vz;
		float hLen = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
		float spec = 0.0F;
		if (hLen > 1.0E-5F) {
			hx /= hLen;
			hy /= hLen;
			hz /= hLen;
			float ndoth = Math.max(0.0F, nx * hx + ny * hy + nz * hz);
			spec = (float) Math.pow(ndoth, 48.0F);
		}

		// Composite final iridescent color
		float finalR = Mth.clamp(r * (0.35F + 0.65F * fresnel) + spec * 0.85F + fresnel * 0.35F, 0.0F, 1.0F);
		float finalG = Mth.clamp(g * (0.35F + 0.65F * fresnel) + spec * 0.85F + fresnel * 0.35F, 0.0F, 1.0F);
		float finalB = Mth.clamp(b * (0.35F + 0.65F * fresnel) + spec * 0.85F + fresnel * 0.35F, 0.0F, 1.0F);

		// Alpha with rim glow
		float alpha = Mth.clamp((0.14F + 0.72F * fresnel + spec * 0.9F) * progress * 0.82F, 0.0F, 1.0F);

		int a = (int) (alpha * 255.0F);
		int red = (int) (finalR * 255.0F);
		int green = (int) (finalG * 255.0F);
		int blue = (int) (finalB * 255.0F);

		return (a << 24) | (red << 16) | (green << 8) | blue;
	}

	private static void drawOrbitingBubbles(Matrix4f matrix, Vec3 camera, double cx, double cy, double cz,
			float rxz, float ry, float time, float progress) {
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		int miniCount = 8;
		for (int i = 0; i < miniCount; i++) {
			float offsetAngle = (float) i / miniCount * TWO_PI + time * (0.8F + i * 0.15F);
			float orbitRadiusXZ = rxz * (1.18F + 0.22F * Mth.sin(time * 1.5F + i));
			float orbitY = ry * (0.4F * Mth.sin(time * 2.0F + i * 1.3F));

			float miniX = (float) cx + Mth.cos(offsetAngle) * orbitRadiusXZ;
			float miniY = (float) cy + orbitY;
			float miniZ = (float) cz + Mth.sin(offsetAngle) * orbitRadiusXZ;

			float miniSize = 0.08F + 0.04F * Mth.sin(time * 3.0F + i * 2.0F);
			drawMiniBubble(buffer, matrix, camera, miniX, miniY, miniZ, miniSize, time + i, progress);
		}

		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void drawMiniBubble(BufferBuilder buffer, Matrix4f matrix, Vec3 camera,
			float bx, float by, float bz, float size, float time, float progress) {
		int segments = 12;
		for (int lat = 0; lat < segments; lat++) {
			float theta1 = (float) lat / segments * PI;
			float theta2 = (float) (lat + 1) / segments * PI;

			for (int lon = 0; lon < segments; lon++) {
				float phi1 = (float) lon / segments * TWO_PI;
				float phi2 = (float) (lon + 1) / segments * TWO_PI;

				addMiniVertex(buffer, matrix, camera, bx, by, bz, theta1, phi1, size, time, progress);
				addMiniVertex(buffer, matrix, camera, bx, by, bz, theta2, phi1, size, time, progress);
				addMiniVertex(buffer, matrix, camera, bx, by, bz, theta1, phi2, size, time, progress);

				addMiniVertex(buffer, matrix, camera, bx, by, bz, theta1, phi2, size, time, progress);
				addMiniVertex(buffer, matrix, camera, bx, by, bz, theta2, phi1, size, time, progress);
				addMiniVertex(buffer, matrix, camera, bx, by, bz, theta2, phi2, size, time, progress);
			}
		}
	}

	private static void addMiniVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 camera,
			float bx, float by, float bz, float theta, float phi, float size, float time, float progress) {
		float nx = Mth.sin(theta) * Mth.cos(phi);
		float ny = Mth.cos(theta);
		float nz = Mth.sin(theta) * Mth.sin(phi);

		float px = bx + nx * size;
		float py = by + ny * size;
		float pz = bz + nz * size;

		float viewX = (float) camera.x - px;
		float viewY = (float) camera.y - py;
		float viewZ = (float) camera.z - pz;
		float viewLen = (float) Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
		if (viewLen > 1.0E-5F) {
			viewX /= viewLen;
			viewY /= viewLen;
			viewZ /= viewLen;
		}

		float cosTheta = Math.abs(nx * viewX + ny * viewY + nz * viewZ);
		int color = computeThinFilmColor(theta, phi, cosTheta, nx, ny, nz, viewX, viewY, viewZ, time, progress);

		buffer.addVertex(matrix, px, py, pz).setColor(color);
	}
}
