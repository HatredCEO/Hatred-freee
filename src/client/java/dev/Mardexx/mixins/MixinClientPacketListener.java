package dev.Mardexx.mixins;

import dev.Mardexx.api.event.events.client.PacketEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import dev.Mardexx.client.modules.impl.Other.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
	@Inject(method = "handleContainerContent", at = @At("HEAD"))
	private void hatred$handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo info) {
		Helper.getInstance().handleContainerContent(packet);
	}

	@Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
	private void hatred$handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo info) {
		Helper.getInstance().handleContainerSlot(packet);
	}

	@Inject(method = "handleSystemChat", at = @At("HEAD"))
	private void hatred$handleSystemChat(ClientboundSystemChatPacket packet, CallbackInfo info) {
		Helper.getInstance().handleSystemChat(packet);
	}

	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	private void hatred$handleEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo info) {
		if (PacketEvent.getInstance().call(new PacketEvent.PacketEventData(packet, PacketEvent.PacketEventData.PacketType.RECEIVE))) {
			info.cancel();
		}
	}
}
