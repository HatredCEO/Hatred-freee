package dev.Mardexx.api.event.events.player.move;

import dev.Mardexx.api.event.events.Event;

public final class CollisionEvent extends Event<CollisionEvent> {
	public static final CollisionEvent INSTANCE = new CollisionEvent();

	private CollisionEvent() {
	}
}
