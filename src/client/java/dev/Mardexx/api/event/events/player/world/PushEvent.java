package dev.Mardexx.api.event.events.player.world;

import dev.Mardexx.api.event.events.Event;

public final class PushEvent extends Event<PushEvent.Data> {
	public static final PushEvent INSTANCE = new PushEvent();

	private PushEvent() {
	}

	public record Data(Source source) {
	}

	public enum Source {
		BLOCK,
		WATER,
		ENTITY
	}
}
