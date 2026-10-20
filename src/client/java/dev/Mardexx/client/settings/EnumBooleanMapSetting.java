package dev.Mardexx.client.settings;

import java.util.EnumMap;
import java.util.function.Consumer;

public final class EnumBooleanMapSetting<T extends Enum<T>> extends Setting<EnumMap<T, Boolean>> {
	private final Class<T> enumType;

	private EnumBooleanMapSetting(String name, Class<T> enumType, EnumMap<T, Boolean> defaultValue) {
		super(name, defaultValue);
		this.enumType = enumType;
	}

	private EnumBooleanMapSetting(String name, String displayName, String description, Class<T> enumType, EnumMap<T, Boolean> defaultValue) {
		super(name, displayName, description, defaultValue);
		this.enumType = enumType;
	}

	public static <T extends Enum<T>> EnumBooleanMapSetting<T> create(
			String name,
			Class<T> enumType,
			Consumer<EnumMap<T, Boolean>> defaults
	) {
		EnumMap<T, Boolean> map = new EnumMap<>(enumType);
		defaults.accept(map);
		for (T constant : enumType.getEnumConstants()) {
			map.put(constant, map.getOrDefault(constant, false));
		}

		return new EnumBooleanMapSetting<>(name, enumType, map);
	}

	public static <T extends Enum<T>> EnumBooleanMapSetting<T> create(
			String name,
			String displayName,
			String description,
			Class<T> enumType,
			Consumer<EnumMap<T, Boolean>> defaults
	) {
		EnumMap<T, Boolean> map = new EnumMap<>(enumType);
		defaults.accept(map);
		for (T constant : enumType.getEnumConstants()) {
			map.put(constant, map.getOrDefault(constant, false));
		}

		return new EnumBooleanMapSetting<>(name, displayName, description, enumType, map);
	}

	public boolean enabled(T key) {
		return value().getOrDefault(key, false);
	}

	public EnumBooleanMapSetting<T> setEnabled(T key, boolean enabled) {
		EnumMap<T, Boolean> copy = value();
		if (!copy.containsKey(key)) {
			throw new IllegalArgumentException("Enum key is not present in this setting");
		}

		copy.replace(key, enabled);
		setValue(copy);
		return this;
	}

	public Class<T> enumType() {
		return enumType;
	}

	@Override
	protected EnumMap<T, Boolean> copy(EnumMap<T, Boolean> value) {
		return value == null ? null : new EnumMap<>(value);
	}
}
