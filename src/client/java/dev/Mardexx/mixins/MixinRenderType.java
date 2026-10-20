package dev.Mardexx.mixins;

import dev.Mardexx.client.render.ChamsRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderType.class)
public class MixinRenderType {
	@Inject(method = "armorCutoutNoCull", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$armorCutoutNoCull(ResourceLocation texture, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.entityChamsActive()) {
			ChamsRenderTypes.setCurrentArmorTexture(texture);
		}
		if (ChamsRenderTypes.armorChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.armorChams(texture, ChamsRenderTypes.armorThroughWalls(), ChamsRenderTypes.armorHiddenColor()));
		}
	}

	@Inject(method = "entityCutout(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityCutout(ResourceLocation texture, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}

	@Inject(method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityCutoutNoCull(ResourceLocation texture, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}

	@Inject(method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityCutoutNoCull(ResourceLocation texture, boolean affectsOutline, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}

	@Inject(method = "entityCutoutNoCullZOffset(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityCutoutNoCullZOffset(ResourceLocation texture, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}

	@Inject(method = "entityCutoutNoCullZOffset(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityCutoutNoCullZOffset(ResourceLocation texture, boolean affectsOutline, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}

	@Inject(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityTranslucent(ResourceLocation texture, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}

	@Inject(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true, require = 0)
	private static void hatred$entityTranslucent(ResourceLocation texture, boolean affectsOutline, CallbackInfoReturnable<RenderType> info) {
		if (ChamsRenderTypes.layerChamsActive()) {
			info.setReturnValue(ChamsRenderTypes.layerChams(texture, ChamsRenderTypes.layerThroughWalls(), ChamsRenderTypes.layerHiddenColor()));
		}
	}
}
