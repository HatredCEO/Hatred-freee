package dev.Mardexx.client.settings;

import dev.Mardexx.client.modules.ModuleManager;

import java.util.function.BooleanSupplier;

public class Setting<T> {
	private final String name;
	private final String displayName;
	private final String description;
	private final T defaultValue;
	private T value;
	private BooleanSupplier visible = () -> true;
	private Setting<?> displayAfter;

	public Setting(String name, T defaultValue) {
		this(name, name, "", defaultValue);
	}

	public Setting(String name, String displayName, String description, T defaultValue) {
		this.name = name;
		this.displayName = displayName;
		this.description = description;
		this.defaultValue = copy(defaultValue);
		this.value = copy(defaultValue);
	}

	public String name() {
		return name;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}

	public T defaultValue() {
		return copy(defaultValue);
	}

	public T value() {
		return copy(value);
	}

	public boolean visible() {
		return visible.getAsBoolean();
	}

	public Setting<T> visibleWhen(BooleanSupplier visible) {
		this.visible = visible == null ? () -> true : visible;
		return this;
	}

	public Setting<?> displayAfter() {
		return displayAfter;
	}

	public Setting<T> displayAfter(Setting<?> setting) {
		this.displayAfter = setting;
		return this;
	}

	public void setValue(T value) {
		this.value = copy(value);
		ModuleManager.saveConfig();
	}

	protected T copy(T value) {
		return value;
	}
}
