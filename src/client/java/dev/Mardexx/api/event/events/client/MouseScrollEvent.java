package dev.Mardexx.api.event.events.client;

import dev.Mardexx.api.event.events.Event;

public final class MouseScrollEvent extends Event<MouseScrollEvent.Data> {
	private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();

	private MouseScrollEvent() {
	}

	public static MouseScrollEvent getInstance() {
		return INSTANCE;
	}

	public record Data(double horizontalAmount, double verticalAmount) {
	}
}
