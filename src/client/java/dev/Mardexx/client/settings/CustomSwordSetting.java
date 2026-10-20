package dev.Mardexx.client.settings;

import dev.Mardexx.client.modules.impl.Render.CustomSwords;

import java.util.EnumMap;
import java.util.Map;

public final class CustomSwordSetting extends Setting<EnumMap<CustomSwords.Target, String>> {
	private final EnumMap<CustomSwords.Target, Boolean> enabledTargets = defaultEnabledTargets();

	public CustomSwordSetting(String name, String displayName, String description) {
		super(name, displayName, description, defaults());
	}

	public boolean enabled(CustomSwords.Target target) {
		return target != null && Boolean.TRUE.equals(enabledTargets.getOrDefault(target, true));
	}

	public void setEnabled(CustomSwords.Target target, boolean enabled) {
		if (target == null) {
			return;
		}

		enabledTargets.put(target, enabled);
		setValue(value());
	}

	public void toggleEnabled(CustomSwords.Target target) {
		setEnabled(target, !enabled(target));
	}

	public String model(CustomSwords.Target target) {
		String model = value().get(target);
		return model == null || model.isBlank() ? CustomSwords.DEFAULT_MODEL : model;
	}

	public void setModel(CustomSwords.Target target, String model) {
		if (target == null || model == null || model.isBlank()) {
			return;
		}

		EnumMap<CustomSwords.Target, String> models = value();
		models.put(target, model);
		setValue(models);
	}

	public void loadModels(Map<String, String> savedModels) {
		if (savedModels == null || savedModels.isEmpty()) {
			return;
		}

		EnumMap<CustomSwords.Target, String> models = value();
		for (CustomSwords.Target target : CustomSwords.Target.values()) {
			String model = savedModels.get(target.name());
			if (model != null && CustomSwords.isKnownModel(model)) {
				models.put(target, model);
			}
		}
		setValue(models);
	}

	public void loadTarget(CustomSwords.Target target, String model, Boolean enabled) {
		if (target == null) {
			return;
		}

		EnumMap<CustomSwords.Target, String> models = value();
		if (model != null && CustomSwords.isKnownModel(model)) {
			models.put(target, model);
		}
		if (enabled != null) {
			enabledTargets.put(target, enabled);
		}
		setValue(models);
	}

	@Override
	protected EnumMap<CustomSwords.Target, String> copy(EnumMap<CustomSwords.Target, String> value) {
		return value == null ? defaults() : new EnumMap<>(value);
	}

	private static EnumMap<CustomSwords.Target, String> defaults() {
		EnumMap<CustomSwords.Target, String> models = new EnumMap<>(CustomSwords.Target.class);
		for (CustomSwords.Target target : CustomSwords.Target.values()) {
			models.put(target, CustomSwords.DEFAULT_MODEL);
		}
		return models;
	}

	private static EnumMap<CustomSwords.Target, Boolean> defaultEnabledTargets() {
		EnumMap<CustomSwords.Target, Boolean> enabled = new EnumMap<>(CustomSwords.Target.class);
		for (CustomSwords.Target target : CustomSwords.Target.values()) {
			enabled.put(target, true);
		}
		return enabled;
	}
}
