package dev.Mardexx.client.settings;

public final class KeySetting extends Setting<Integer> {
	public KeySetting(String name, String displayName, String description) {
		super(name, displayName, description, -1);
	}

	public int key() {
		return value();
	}

	public void setKey(int key) {
		setValue(key);
	}
}
