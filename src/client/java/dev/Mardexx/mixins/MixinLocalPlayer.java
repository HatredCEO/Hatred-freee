package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Movement.NoPush;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
	@Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
	private void hatred$moveTowardsClosestSpace(double x, double z, CallbackInfo info) {
		if (NoPush.getInstance().cancelBlock()) {
			info.cancel();
		}
	}
}
