package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Combat.TriggerBot;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftKeybinds {
	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void hatred$beforeHandleKeybinds(CallbackInfo info) {
		TriggerBot.getInstance().beforeHandleKeybinds();
	}

	@Inject(method = "handleKeybinds", at = @At("RETURN"))
	private void hatred$afterHandleKeybinds(CallbackInfo info) {
		TriggerBot.getInstance().afterHandleKeybinds();
	}
}
