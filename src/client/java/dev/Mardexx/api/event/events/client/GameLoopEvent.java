package dev.Mardexx.api.event.events.client;

import dev.Mardexx.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
	private static final GameLoopEvent INSTANCE = new GameLoopEvent();

	public static GameLoopEvent getInstance() {
		return INSTANCE;
	}
}
