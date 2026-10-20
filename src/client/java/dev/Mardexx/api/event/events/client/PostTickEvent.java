package dev.Mardexx.api.event.events.client;

import dev.Mardexx.api.event.events.Event;

public class PostTickEvent extends Event<PostTickEvent> {
	private static final PostTickEvent INSTANCE = new PostTickEvent();

	public static PostTickEvent getInstance() {
		return INSTANCE;
	}
}
