package dev.Mardexx.client.settings;

public final class EnumSetting<T extends Enum<T>> extends Setting<T> {
	private final T[] values;

	public EnumSetting(String name, T defaultValue) {
		super(name, defaultValue);
		this.values = defaultValue.getDeclaringClass().getEnumConstants();
	}

	public EnumSetting(String name, String displayName, String description, T defaultValue) {
		super(name, displayName, description, defaultValue);
		this.values = defaultValue.getDeclaringClass().getEnumConstants();
	}

	public T[] values() {
		return values;
	}

	public boolean is(T expected) {
		return value().equals(expected);
	}
}
