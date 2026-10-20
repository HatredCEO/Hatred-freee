package dev.Mardexx.api.event.events.client;

import dev.Mardexx.api.event.events.Event;

public final class KeyEvent extends Event<KeyEvent.Data> {
	public static final KeyEvent INSTANCE = new KeyEvent();

	private KeyEvent() {
	}

	public static KeyEvent getInstance() {
		return INSTANCE;
	}

	public static class Data {
		private final int key;
		private final int action;
		private final boolean mouse;

		public Data(int key, int action, boolean mouse) {
			this.key = key;
			this.action = action;
			this.mouse = mouse;
		}

		public int key() {
			return key;
		}

		public int action() {
			return action;
		}

		public boolean mouse() {
			return mouse;
		}
	}

	public static final class KeyEventData extends Data {
		public KeyEventData(int key, int action, boolean mouse) {
			super(key, action, mouse);
		}
	}
}
