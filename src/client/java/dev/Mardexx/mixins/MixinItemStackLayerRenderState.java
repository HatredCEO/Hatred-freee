package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.WallHack;
import dev.Mardexx.client.render.ChamsRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class MixinItemStackLayerRenderState {
	@ModifyArg(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
			),
			index = 7
	)
	private RenderType hatred$entityItemChamsRenderType(RenderType originalRenderType) {
		if (!ChamsRenderTypes.entityChamsActive() || ChamsRenderTypes.currentRenderedEntity() == null) {
			return originalRenderType;
		}

		boolean ownerBehindWall = WallHack.getInstance().enabled() && WallHack.getInstance().isBehindWall(ChamsRenderTypes.currentRenderedEntity());
		int hiddenColor = 0xFFFFFFFF;
		return ChamsRenderTypes.itemChams(
				TextureAtlas.LOCATION_BLOCKS,
				ChamsRenderTypes.entityThroughWalls(),
				ownerBehindWall,
				hiddenColor
		);
	}
}
