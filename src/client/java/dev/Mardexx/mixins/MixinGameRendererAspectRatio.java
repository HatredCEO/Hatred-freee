package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.AspectRatio;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererAspectRatio {
	@Inject(method = "getProjectionMatrix", at = @At("RETURN"), cancellable = true)
	private void hatred$aspectRatio(float fov, CallbackInfoReturnable<Matrix4f> info) {
		AspectRatio aspectRatio = AspectRatio.getInstance();
		if (!aspectRatio.enabled()) {
			return;
		}
		Matrix4f matrix = new Matrix4f(info.getReturnValue());
		matrix.m00(matrix.m11() / aspectRatio.ratio());
		info.setReturnValue(matrix);
	}
}
