package dev.Mardexx.client.modules.impl.Movement;

import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.client.PacketEvent;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;

public class NoPush extends Module implements QuickImports {
	private static final NoPush INSTANCE = new NoPush();

	private final BooleanSetting block = new BooleanSetting("Block", "Block", "Prevents blocks from pushing you out.", true);
	private final BooleanSetting liquids = new BooleanSetting("Liquids", "Liquids", "Prevents water and other fluids from pushing you.", true);
	private final BooleanSetting entity = new BooleanSetting("Entity", "Entity", "Prevents entity collision push.", true);
	private final BooleanSetting fishingRod = new BooleanSetting("Fishing rod", "Fishing Rod", "Prevents fishing rods from pulling you.", true);
	private EventListener packetEvent;

	private NoPush() {
		super("No Push", ModuleCategory.MOVEMENT, "Prevents blocks, fluids, entities and fishing rods from pushing you.");
		register(block, liquids, entity, fishingRod);
	}

	@Override
	protected void onEnable() {
		packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
			if (!event.isReceive() || mc.level == null || mc.player == null || !fishingRod.enabled()) {
				return;
			}

			Packet<?> packet = event.packet();
			if (packet instanceof ClientboundEntityEventPacket statusPacket && statusPacket.getEventId() == 31) {
				Entity entity = statusPacket.getEntity(mc.level);
				if (entity instanceof FishingHook hook && hook.getHookedIn() == mc.player) {
					PacketEvent.getInstance().setCancel(true);
				}
			}
		}));
	}

	@Override
	protected void onDisable() {
		if (packetEvent != null) {
			packetEvent.unsubscribe();
			packetEvent = null;
		}
	}

	public boolean cancelBlock() {
		return enabled() && block.enabled();
	}

	public boolean cancelLiquids() {
		return enabled() && liquids.enabled();
	}

	public boolean cancelEntity() {
		return enabled() && entity.enabled();
	}

	public boolean cancelFishingRod() {
		return enabled() && fishingRod.enabled();
	}

	public static NoPush getInstance() {
		return INSTANCE;
	}
}
