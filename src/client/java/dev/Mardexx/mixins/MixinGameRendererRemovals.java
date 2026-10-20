package dev.Mardexx.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.Mardexx.client.modules.impl.Render.Removals;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererRemovals {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void hatred$removeHurtCamera(PoseStack poseStack, float tickDelta, CallbackInfo info) {
		if (Removals.getInstance().removesHurtCamera()) {
			info.cancel();
		}
	}
}
