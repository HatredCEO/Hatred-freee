package dev.Mardexx.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ChamsModelRenderHelper {
	private ChamsModelRenderHelper() {
	}

	public static void renderHumanoidBodyParts(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int overlay, int color, LivingEntity entity) {
		boolean cameraOnRight = isCameraOnEntityRight(entity);
		ModelPart farArm = cameraOnRight ? model.leftArm : model.rightArm;
		ModelPart nearArm = cameraOnRight ? model.rightArm : model.leftArm;
		ModelPart farLeg = cameraOnRight ? model.leftLeg : model.rightLeg;
		ModelPart nearLeg = cameraOnRight ? model.rightLeg : model.leftLeg;

		farArm.render(poseStack, vertexConsumer, packedLight, overlay, color);
		farLeg.render(poseStack, vertexConsumer, packedLight, overlay, color);
		model.body.render(poseStack, vertexConsumer, packedLight, overlay, color);
		model.head.render(poseStack, vertexConsumer, packedLight, overlay, color);
		nearLeg.render(poseStack, vertexConsumer, packedLight, overlay, color);
		nearArm.render(poseStack, vertexConsumer, packedLight, overlay, color);
	}

	private static boolean isCameraOnEntityRight(LivingEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
			return false;
		}

		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		Vec3 toCamera = cameraPosition.subtract(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
		double yawRadians = Math.toRadians(entity.getYRot());
		double rightX = Math.cos(yawRadians);
		double rightZ = -Math.sin(yawRadians);
		return toCamera.x * rightX + toCamera.z * rightZ < 0.0D;
	}
}
