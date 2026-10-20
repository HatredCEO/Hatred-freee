package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.WorldTweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public abstract class MixinClientLevelDataWorldTweaks {
	@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
	private void hatred$changeDayTime(CallbackInfoReturnable<Long> info) {
		WorldTweaks module = WorldTweaks.getInstance();
		if (module.changesTime()) {
			info.setReturnValue(module.dayTime());
		}
	}
}
