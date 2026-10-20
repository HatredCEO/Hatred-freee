package dev.Mardexx.mixins;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundContainerSetSlotPacket.class)
public interface MixinClientboundContainerSetSlotPacketAccessor {
	@Mutable
	@Accessor("slot")
	void hatred$setSlot(int slot);
}
