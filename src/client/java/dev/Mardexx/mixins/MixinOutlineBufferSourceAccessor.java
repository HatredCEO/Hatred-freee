package dev.Mardexx.mixins;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OutlineBufferSource.class)
public interface MixinOutlineBufferSourceAccessor {
	@Accessor("bufferSource")
	MultiBufferSource.BufferSource hatred$getBufferSource();
}
