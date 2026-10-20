package dev.Mardexx.api.utils.render;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4d;
import org.joml.Vector2f;
import org.joml.Vector4d;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class ProjectionUtil {
	private static final double NEAR_PLANE = 0.05D;
	private static final int[][] BOX_EDGES = {
			{0, 1}, {0, 2}, {1, 3}, {2, 3},
			{4, 5}, {4, 6}, {5, 7}, {6, 7},
			{0, 4}, {1, 5}, {2, 6}, {3, 7}
	};
	private static final AtomicReference<Matrix4f> LAST_PROJECTION = new AtomicReference<>();
	private static final AtomicReference<Matrix4f> LAST_MODEL_VIEW = new AtomicReference<>();
	private static final AtomicReference<Matrix4f> LAST_VIEW_PROJECTION = new AtomicReference<>();
	private static final AtomicReference<Vec3> LAST_CAMERA_POSITION = new AtomicReference<>();

	private ProjectionUtil() {
	}

	public static void capture(Matrix4f projectionMatrix, Matrix4f modelViewMatrix) {
		Minecraft minecraft = Minecraft.getInstance();
		if (projectionMatrix != null) {
			LAST_PROJECTION.set(new Matrix4f(projectionMatrix));
		}
		if (modelViewMatrix != null) {
			LAST_MODEL_VIEW.set(new Matrix4f(modelViewMatrix));
		}
		if (projectionMatrix != null && modelViewMatrix != null) {
			LAST_VIEW_PROJECTION.set(new Matrix4f(projectionMatrix).mul(modelViewMatrix));
		}
		if (minecraft.gameRenderer != null && minecraft.gameRenderer.getMainCamera() != null) {
			LAST_CAMERA_POSITION.set(minecraft.gameRenderer.getMainCamera().getPosition());
		}
	}

	public static Optional<Vector2f> worldToScreen(Vec3 position) {
		return worldToScreen(position, false);
	}

	public static Optional<Vector2f> worldToScreen(Vec3 position, boolean clampBehind) {
		Matrix4f viewProjection = LAST_VIEW_PROJECTION.get();
		Vec3 cameraPosition = LAST_CAMERA_POSITION.get();
		Minecraft minecraft = Minecraft.getInstance();
		if (viewProjection == null || cameraPosition == null || minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
			return Optional.empty();
		}

		Vec3 relative = position.subtract(cameraPosition);
		Vector4f projected = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);
		viewProjection.transform(projected);
		if (!Float.isFinite(projected.w)) {
			return Optional.empty();
		}

		boolean behind = projected.w <= 0.0F;
		if (behind && !clampBehind) {
			return Optional.empty();
		}

		float normalizedX = projected.x / projected.w;
		float normalizedY = projected.y / projected.w;
		if (!Float.isFinite(normalizedX) || !Float.isFinite(normalizedY)) {
			return Optional.empty();
		}

		if (behind) {
			normalizedX = -normalizedX;
			normalizedY = -normalizedY;
			float max = Math.max(Math.abs(normalizedX), Math.abs(normalizedY));
			if (max < 1.0E-4F) {
				normalizedX = 0.0F;
				normalizedY = 1.0F;
				max = 1.0F;
			}
			if (max < 1.0F) {
				float scale = 1.0F / max;
				normalizedX *= scale;
				normalizedY *= scale;
			}
		}

		Window window = minecraft.getWindow();
		float guiScale = (float) window.getGuiScale();
		float screenX = (normalizedX + 1.0F) * 0.5F * window.getWidth() / guiScale;
		float screenY = (1.0F - normalizedY) * 0.5F * window.getHeight() / guiScale;
		if (!Float.isFinite(screenX) || !Float.isFinite(screenY)) {
			return Optional.empty();
		}

		return Optional.of(new Vector2f(screenX, screenY));
	}

	public static Optional<Vector4f> boxToScreen(AABB box) {
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		int projectedCorners = 0;

		for (Vec3 corner : corners(box)) {
			Optional<Vector2f> projected = worldToScreen(corner);
			if (projected.isEmpty()) {
				continue;
			}

			Vector2f point = projected.get();
			projectedCorners++;
			minX = Math.min(minX, point.x);
			minY = Math.min(minY, point.y);
			maxX = Math.max(maxX, point.x);
			maxY = Math.max(maxY, point.y);
		}

		if (projectedCorners == 0) {
			return Optional.empty();
		}

		Window window = Minecraft.getInstance().getWindow();
		float screenWidth = (float) window.getGuiScaledWidth();
		float screenHeight = (float) window.getGuiScaledHeight();
		if (maxX < 0.0F || minX > screenWidth || maxY < 0.0F || minY > screenHeight) {
			return Optional.empty();
		}

		float width = Math.max(0.0F, maxX - minX);
		float height = Math.max(0.0F, maxY - minY);
		if (width <= 0.0F && height <= 0.0F) {
			return Optional.empty();
		}

		return Optional.of(new Vector4f(minX, minY, width, height));
	}

	/** Projects an AABB like Nightix ESP, clipping box edges against the camera near plane. */
	public static Optional<Vector4f> boxToScreenClipped(AABB box) {
		Matrix4f viewProjection = LAST_VIEW_PROJECTION.get();
		Vec3 cameraPosition = LAST_CAMERA_POSITION.get();
		Minecraft minecraft = Minecraft.getInstance();
		if (viewProjection == null || cameraPosition == null) {
			return Optional.empty();
		}

		Matrix4d matrix = new Matrix4d(viewProjection);
		Vec3[] boxCorners = corners(box);
		Vector4d[] clips = new Vector4d[boxCorners.length];
		for (int i = 0; i < boxCorners.length; i++) {
			Vec3 relative = boxCorners[i].subtract(cameraPosition);
			clips[i] = matrix.transform(new Vector4d(relative.x, relative.y, relative.z, 1.0D));
		}

		Window window = minecraft.getWindow();
		double screenWidth = window.getGuiScaledWidth();
		double screenHeight = window.getGuiScaledHeight();
		double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
		boolean hasPoint = false;

		for (Vector4d clip : clips) {
			if (clip.w > NEAR_PLANE) {
				includeClipPoint(clip, screenWidth, screenHeight, bounds);
				hasPoint = true;
			}
		}

		for (int[] edge : BOX_EDGES) {
			Vector4d from = clips[edge[0]];
			Vector4d to = clips[edge[1]];
			boolean fromVisible = from.w > NEAR_PLANE;
			boolean toVisible = to.w > NEAR_PLANE;
			if (fromVisible == toVisible) {
				continue;
			}

			double denominator = to.w - from.w;
			if (Math.abs(denominator) < 1.0E-12D) {
				continue;
			}
			double progress = (NEAR_PLANE - from.w) / denominator;
			Vector4d clipped = new Vector4d(from).lerp(to, Math.max(0.0D, Math.min(1.0D, progress)));
			includeClipPoint(clipped, screenWidth, screenHeight, bounds);
			hasPoint = true;
		}

		if (!hasPoint || !finite(bounds)) {
			return Optional.empty();
		}

		bounds[0] = Math.max(-screenWidth, bounds[0]);
		bounds[1] = Math.max(-screenHeight, bounds[1]);
		bounds[2] = Math.min(screenWidth * 2.0D, bounds[2]);
		bounds[3] = Math.min(screenHeight * 2.0D, bounds[3]);
		if (bounds[2] < 0.0D || bounds[0] > screenWidth || bounds[3] < 0.0D || bounds[1] > screenHeight) {
			return Optional.empty();
		}

		float width = (float) (bounds[2] - bounds[0]);
		float height = (float) (bounds[3] - bounds[1]);
		if (width <= 0.0F || height <= 0.0F) {
			return Optional.empty();
		}
		return Optional.of(new Vector4f((float) bounds[0], (float) bounds[1], width, height));
	}

	private static void includeClipPoint(Vector4d clip, double screenWidth, double screenHeight, double[] bounds) {
		double inverseW = 1.0D / clip.w;
		double screenX = (clip.x * inverseW * 0.5D + 0.5D) * screenWidth;
		double screenY = (0.5D - clip.y * inverseW * 0.5D) * screenHeight;
		screenX = Math.max(-screenWidth * 2.0D, Math.min(screenWidth * 3.0D, screenX));
		screenY = Math.max(-screenHeight * 2.0D, Math.min(screenHeight * 3.0D, screenY));
		bounds[0] = Math.min(bounds[0], screenX);
		bounds[1] = Math.min(bounds[1], screenY);
		bounds[2] = Math.max(bounds[2], screenX);
		bounds[3] = Math.max(bounds[3], screenY);
	}

	private static boolean finite(double[] values) {
		for (double value : values) {
			if (!Double.isFinite(value)) {
				return false;
			}
		}
		return true;
	}

	public static float centerX(Vector4f projection) {
		return projection.x + projection.z / 2.0F;
	}

	private static Vec3[] corners(AABB box) {
		return new Vec3[] {
				new Vec3(box.minX, box.minY, box.minZ),
				new Vec3(box.minX, box.minY, box.maxZ),
				new Vec3(box.minX, box.maxY, box.minZ),
				new Vec3(box.minX, box.maxY, box.maxZ),
				new Vec3(box.maxX, box.minY, box.minZ),
				new Vec3(box.maxX, box.minY, box.maxZ),
				new Vec3(box.maxX, box.maxY, box.minZ),
				new Vec3(box.maxX, box.maxY, box.maxZ)
		};
	}
}
