package dev.Mardexx.api.utils.other;

import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.client.PacketEvent;
import dev.Mardexx.api.system.interfaces.QuickImports;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;

public final class TPSTracker implements QuickImports {
	private static final TPSTracker INSTANCE = new TPSTracker();
	private final ArrayDeque<Float> tpsSamples = new ArrayDeque<>(20);
	private long lastPacketTime;
	private long tickInterval = 1000L;
	private float currentTps = 20.0F;
	private boolean loaded;

	public void load() {
		if (loaded) {
			return;
		}

		EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
			if (event.isReceive() && event.packet() instanceof ClientboundSetTimePacket) {
				long now = System.currentTimeMillis();
				if (lastPacketTime != 0L) {
					long delta = now - lastPacketTime;
					tickInterval = Math.max(250L, Math.min(10000L, delta));
					if (tpsSamples.size() >= 20) {
						tpsSamples.poll();
					}
					tpsSamples.add(20.0F * (1000.0F / (float) tickInterval));

					float sum = 0.0F;
					for (Float sample : tpsSamples) {
						sum += Mth.clamp(sample, 0.0F, 20.0F);
					}
					if (!tpsSamples.isEmpty()) {
						currentTps = sum / tpsSamples.size();
					}
				}
				lastPacketTime = now;
			}
		}));
		loaded = true;
	}

	public float getTps() {
		return round(Mth.clamp(currentTps, 0.0F, 20.0F));
	}

	public float getTickMultiplier() {
		if (tickInterval <= 0L) {
			return 1.0F;
		}
		return Mth.clamp((float) tickInterval / 1000.0F, 1.0F, 3.0F);
	}

	public long adjustForTps(long delay) {
		if (delay <= 0L) {
			return 0L;
		}
		return Math.max(1L, Math.round(delay * getTickMultiplier()));
	}

	public int getPing() {
		if (mc.getConnection() == null || mc.player == null) {
			return 0;
		}

		PlayerInfo entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return entry == null ? 0 : entry.getLatency();
	}

	private static float round(double value) {
		return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).floatValue();
	}

	public static TPSTracker getInstance() {
		return INSTANCE;
	}

	private TPSTracker() {
	}
}
