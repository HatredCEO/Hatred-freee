package dev.Mardexx.mixins;

import dev.Mardexx.api.event.events.client.KeyEvent;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void hatred$keyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
		KeyEvent.getInstance().call(new KeyEvent.KeyEventData(key, action, false));
	}
}
