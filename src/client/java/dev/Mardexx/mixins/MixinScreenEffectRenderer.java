package dev.Mardexx.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.Mardexx.client.modules.impl.Render.Removals;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class MixinScreenEffectRenderer {
	@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
	private static void hatred$removeFireOverlay(
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			CallbackInfo info
	) {
		if (Removals.getInstance().removesFireOverlay()) {
			info.cancel();
		}
	}

	@Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
	private static void hatred$removeInWallOverlay(
			TextureAtlasSprite sprite,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			CallbackInfo info
	) {
		if (Removals.getInstance().removesInWallOverlay()) {
			info.cancel();
		}
	}
}
