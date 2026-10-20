package dev.Mardexx.api.event.events.player.other;

import dev.Mardexx.api.event.events.Event;

public final class UpdateEvent extends Event<UpdateEvent> {
	private static final UpdateEvent INSTANCE = new UpdateEvent();

	private UpdateEvent() {
	}

	public static UpdateEvent getInstance() {
		return INSTANCE;
	}
}
