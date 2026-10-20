package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Combat.Tap;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
	@Inject(method = "tick", at = @At("HEAD"))
	private void hatred$beforeKeyboardInputTick(CallbackInfo info) {
		Tap.getInstance().beforeMovementInputTick();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void hatred$afterKeyboardInputTick(CallbackInfo info) {
		Tap.getInstance().afterMovementInputTick();
	}
}


