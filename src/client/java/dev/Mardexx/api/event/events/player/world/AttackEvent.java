package dev.Mardexx.api.event.events.player.world;

import dev.Mardexx.api.event.events.Event;
import net.minecraft.world.entity.Entity;

public class AttackEvent extends Event<AttackEvent.AttackEventData> {
	private static final AttackEvent INSTANCE = new AttackEvent();

	public static AttackEvent getInstance() {
		return INSTANCE;
	}

	public record AttackEventData(Entity entity) {
	}
}
