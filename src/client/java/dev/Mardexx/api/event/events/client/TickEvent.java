package dev.Mardexx.api.event.events.client;

import dev.Mardexx.api.event.events.Event;

public class TickEvent extends Event<TickEvent> {
	private static final TickEvent INSTANCE = new TickEvent();

	public static TickEvent getInstance() {
		return INSTANCE;
	}
}
