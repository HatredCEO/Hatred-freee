package dev.Mardexx.api.event.events.player.world;

import dev.Mardexx.api.event.events.Event;
import net.minecraft.world.inventory.ClickType;

public class ClickSlotEvent extends Event<ClickSlotEvent.ClickSlotEventData> {
	private static final ClickSlotEvent INSTANCE = new ClickSlotEvent();

	public static ClickSlotEvent getInstance() {
		return INSTANCE;
	}

	public record ClickSlotEventData(ClickType slotActionType, int slot, int button, int id) {
	}
}
