package dev.Mardexx.client.modules.impl.Movement;

import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.client.PacketEvent;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.utils.other.SlownessManager;
import dev.Mardexx.api.utils.player.MoveUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;

import java.util.ArrayDeque;
import java.util.Queue;

public class InventoryMove extends Module implements QuickImports {
	private static final InventoryMove INSTANCE = new InventoryMove();
	private static final long LEGIT2_TOTAL_MS = 95L;
	private static final long LEGIT2_DELAY_MS = 35L;

	private final Queue<Packet<?>> queuedPackets = new ArrayDeque<>();
	private EventListener packetEvent;
	private volatile ServerboundContainerClosePacket pendingClose;
	private boolean preparingClose;

	private InventoryMove() {
		super("Inventory Move", ModuleCategory.MOVEMENT, "Allows movement while inventory screens are open.");
	}

	@Override
	protected void onEnable() {
		packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::packetEvent));
	}

	@Override
	protected void onDisable() {
		if (packetEvent != null) {
			packetEvent.unsubscribe();
			packetEvent = null;
		}
		pendingClose = null;
		preparingClose = false;
		queuedPackets.clear();
		SlownessManager.clear();
	}

	@Override
	public void onTick() {
		SlownessManager.tick();
		postTickEvent();
		updateMovement();
	}

	private void updateMovement() {
		if (mc.screen instanceof ChatScreen
				|| mc.screen instanceof SignEditScreen
				|| mc.screen instanceof AnvilScreen
				|| mc.screen instanceof AbstractCommandBlockEditScreen
				|| mc.screen instanceof StructureBlockEditScreen) {
			return;
		}

		if (SlownessManager.slowed()) {
			MoveUtil.releaseMovementKeys();
			return;
		}

		MoveUtil.updateMovementKeys();
		if (mc.screen != null) {
			mc.options.keyShift.setDown(false);
		}
	}

	private void packetEvent(PacketEvent.PacketEventData event) {
		if (!event.isSend() || mc.player == null) {
			return;
		}

		Packet<?> packet = event.packet();
		if (packet instanceof ServerboundContainerClosePacket closePacket) {
			if (!queuedPackets.isEmpty()) {
				sendCloseSafely(closePacket);
				PacketEvent.getInstance().setCancel(true);
			}
			return;
		}

		if ((!MoveUtil.isMoving() && !mc.options.keyJump.isDown())
				|| !(mc.screen instanceof InventoryScreen)
				|| !isInventoryActionPacket(packet)) {
			return;
		}

		PacketEvent.getInstance().setCancel(true);
		queuedPackets.add(packet);
	}

	private void sendCloseSafely(ServerboundContainerClosePacket closePacket) {
		preparingClose = false;
		pendingClose = closePacket;
	}

	private void postTickEvent() {
		ServerboundContainerClosePacket closePacket = pendingClose;
		if (closePacket == null) {
			preparingClose = false;
			return;
		}

		if (!preparingClose) {
			SlownessManager.applySlowness(LEGIT2_TOTAL_MS, LEGIT2_DELAY_MS, () -> {
				sendQueuedPackets();
			});
			preparingClose = true;
			return;
		}

		pendingClose = null;
		preparingClose = false;
		PacketEvent.sendSilently(closePacket);
	}

	private void sendQueuedPackets() {
		while (!queuedPackets.isEmpty()) {
			PacketEvent.sendSilently(queuedPackets.poll());
		}
	}

	private boolean isInventoryActionPacket(Packet<?> packet) {
		return packet instanceof ServerboundContainerClickPacket
				|| packet instanceof ServerboundContainerButtonClickPacket
				|| packet instanceof ServerboundSetCreativeModeSlotPacket
				|| packet instanceof ServerboundContainerSlotStateChangedPacket;
	}

	public static InventoryMove getInstance() {
		return INSTANCE;
	}
}
