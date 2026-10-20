package dev.Mardexx.api.event.events.player.move;

import dev.Mardexx.api.event.events.Event;

public final class JumpEvent extends Event<JumpEvent> {
	public static final JumpEvent INSTANCE = new JumpEvent();

	private JumpEvent() {
	}

	public static JumpEvent getInstance() {
		return INSTANCE;
	}
}
