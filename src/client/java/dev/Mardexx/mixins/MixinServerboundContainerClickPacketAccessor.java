package dev.Mardexx.mixins;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundContainerClickPacket.class)
public interface MixinServerboundContainerClickPacketAccessor {
	@Mutable
	@Accessor("slotNum")
	void hatred$setSlotNum(int slot);
}
