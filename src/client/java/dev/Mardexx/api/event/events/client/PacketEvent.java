package dev.Mardexx.api.event.events.client;

import dev.Mardexx.api.event.events.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public class PacketEvent extends Event<PacketEvent.PacketEventData> {
	private static final PacketEvent INSTANCE = new PacketEvent();
	private static final ThreadLocal<Boolean> SILENT = ThreadLocal.withInitial(() -> false);

	public static PacketEvent getInstance() {
		return INSTANCE;
	}

	public static boolean isSilent() {
		return SILENT.get();
	}

	public static void sendSilently(Packet<?> packet) {
		if (Minecraft.getInstance().getConnection() == null) {
			return;
		}

		SILENT.set(true);
		try {
			Minecraft.getInstance().getConnection().send(packet);
		} finally {
			SILENT.set(false);
		}
	}

	public record PacketEventData(Packet<?> packet, PacketType packetType) {
		public boolean isReceive() {
			return packetType == PacketType.RECEIVE;
		}

		public boolean isSend() {
			return packetType == PacketType.SEND;
		}

		public enum PacketType {
			SEND,
			RECEIVE
		}
	}
}
