package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.Removals;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class MixinLightTextureRemovals {
	@Inject(method = "getDarknessGamma", at = @At("HEAD"), cancellable = true)
	private void hatred$removeWardenDarkness(float tickDelta, CallbackInfoReturnable<Float> info) {
		if (Removals.getInstance().removesWardenEffect()) {
			info.setReturnValue(0.0F);
		}
	}
}
