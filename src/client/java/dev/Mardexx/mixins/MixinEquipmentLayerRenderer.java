package dev.Mardexx.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.Mardexx.client.render.ChamsModelRenderHelper;
import dev.Mardexx.client.render.ChamsRenderTypes;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentLayerRenderer {
	@Redirect(
			method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
			)
	)
	private void hatred$renderHiddenPlayerArmorInStableOrder(
			Model model,
			PoseStack poseStack,
			VertexConsumer vertexConsumer,
			int packedLight,
			int overlay,
			int color,
			EquipmentClientInfo.LayerType layerType,
			ResourceKey<?> equipmentAssetId,
			Model armorModel,
			ItemStack itemStack,
			PoseStack renderPoseStack,
			MultiBufferSource bufferSource,
			int renderPackedLight,
			ResourceLocation texture
	) {
		LivingEntity entity = ChamsRenderTypes.currentRenderedEntity();
		int finalColor = color;
		VertexConsumer consumerToUse = vertexConsumer;

		if (!(entity instanceof net.minecraft.world.entity.player.Player) || !ChamsRenderTypes.armorThroughWalls() || !(model instanceof HumanoidModel<?> humanoidModel)) {
			model.renderToBuffer(poseStack, consumerToUse, packedLight, overlay, finalColor);
			return;
		}

		ChamsModelRenderHelper.renderHumanoidBodyParts(humanoidModel, poseStack, consumerToUse, packedLight, overlay, finalColor, entity);
	}
}
