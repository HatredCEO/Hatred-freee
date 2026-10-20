package dev.Mardexx.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class Render3D {
	private static final List<Renderer> RENDERERS = new ArrayList<>();

	private Render3D() {
	}

	public static void add(Renderer renderer) {
		RENDERERS.add(renderer);
	}

	public static void render(WorldRenderContext context) {
		for (Renderer renderer : RENDERERS) {
			renderer.render(context);
		}
	}

	public static void box(WorldRenderContext context, AABB box, int color) {
		PoseStack poseStack = context.matrixStack();
		Vec3 cameraPosition = context.camera().getPosition();
		VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderType.lines());

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		drawBoxLines(poseStack.last().pose(), vertexConsumer, box, color);
		poseStack.popPose();
	}

	private static void drawBoxLines(Matrix4f matrix, VertexConsumer vertexConsumer, AABB box, int color) {
		line(matrix, vertexConsumer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
		line(matrix, vertexConsumer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
		line(matrix, vertexConsumer, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
		line(matrix, vertexConsumer, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);

		line(matrix, vertexConsumer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
		line(matrix, vertexConsumer, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
		line(matrix, vertexConsumer, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
		line(matrix, vertexConsumer, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);

		line(matrix, vertexConsumer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
		line(matrix, vertexConsumer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
		line(matrix, vertexConsumer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
		line(matrix, vertexConsumer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
	}

	private static void line(Matrix4f matrix, VertexConsumer vertexConsumer, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
		double deltaX = x2 - x1;
		double deltaY = y2 - y1;
		double deltaZ = z2 - z1;
		double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		float normalX = length <= 1.0E-7D ? 0.0F : (float) (deltaX / length);
		float normalY = length <= 1.0E-7D ? 1.0F : (float) (deltaY / length);
		float normalZ = length <= 1.0E-7D ? 0.0F : (float) (deltaZ / length);
		vertex(matrix, vertexConsumer, x1, y1, z1, color, normalX, normalY, normalZ);
		vertex(matrix, vertexConsumer, x2, y2, z2, color, normalX, normalY, normalZ);
	}

	private static void vertex(Matrix4f matrix, VertexConsumer vertexConsumer, double x, double y, double z, int color, float normalX, float normalY, float normalZ) {
		vertexConsumer.addVertex(matrix, (float) x, (float) y, (float) z).setColor(color).setNormal(normalX, normalY, normalZ);
	}

	public static AABB box(double x, double y, double z, double width, double height, double depth) {
		return new AABB(x, y, z, x + width, y + height, z + depth);
	}

	public static float alpha(int color) {
		return ((color >> 24) & 255) / 255.0F;
	}

	public static float red(int color) {
		return ((color >> 16) & 255) / 255.0F;
	}

	public static float green(int color) {
		return ((color >> 8) & 255) / 255.0F;
	}

	public static float blue(int color) {
		return (color & 255) / 255.0F;
	}

	@FunctionalInterface
	public interface Renderer {
		void render(WorldRenderContext context);
	}
}
