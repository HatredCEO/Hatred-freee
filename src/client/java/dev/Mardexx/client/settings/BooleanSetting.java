package dev.Mardexx.client.settings;

public final class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public BooleanSetting(String name, String displayName, String description, boolean defaultValue) {
		super(name, displayName, description, defaultValue);
	}

	public boolean enabled() {
		return Boolean.TRUE.equals(value());
	}
}
