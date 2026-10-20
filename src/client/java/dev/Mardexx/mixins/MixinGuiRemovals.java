package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.Removals;
import dev.Mardexx.client.modules.impl.Render.PotionList;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(Gui.class)
public abstract class MixinGuiRemovals {
	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void hatred$hideVanillaEffectIcons(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo info) {
		if (PotionList.getInstance().enabled()) {
			info.cancel();
		}
	}

	@Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
	private void hatred$removeConfusionOverlay(GuiGraphics graphics, float strength, CallbackInfo info) {
		if (Removals.getInstance().removesBadEffects()) {
			info.cancel();
		}
	}

	@Redirect(
			method = "renderEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"
			)
	)
	private Collection<MobEffectInstance> hatred$filterBadEffectIcons(LocalPlayer player) {
		Collection<MobEffectInstance> effects = player.getActiveEffects();
		if (!Removals.getInstance().removesBadEffects()) {
			return effects;
		}
		return effects.stream()
				.filter(effect -> effect.getEffect().value().isBeneficial())
				.toList();
	}
}
