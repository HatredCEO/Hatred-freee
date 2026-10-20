package dev.Mardexx.mixins;

import dev.Mardexx.api.event.events.client.PacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import dev.Mardexx.client.modules.impl.Other.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"), cancellable = true)
	private void hatred$send(Packet<?> packet, PacketSendListener sendListener, boolean flush, CallbackInfo info) {
		if (packet instanceof ServerboundContainerClickPacket clickPacket) {
			Helper.getInstance().handleContainerClick(clickPacket);
		}
		if (!PacketEvent.isSilent()
				&& PacketEvent.getInstance().call(new PacketEvent.PacketEventData(packet, PacketEvent.PacketEventData.PacketType.SEND))) {
			info.cancel();
		}
	}
}
