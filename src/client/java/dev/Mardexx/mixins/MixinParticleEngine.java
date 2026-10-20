package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.Removals;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
	private void hatred$removeBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo info) {
		if (Removals.getInstance().removesBlockBreakParticles()) {
			info.cancel();
		}
	}

	@Inject(method = "crack", at = @At("HEAD"), cancellable = true)
	private void hatred$removeBlockHitParticles(BlockPos pos, Direction direction, CallbackInfo info) {
		if (Removals.getInstance().removesBlockBreakParticles()) {
			info.cancel();
		}
	}
}
