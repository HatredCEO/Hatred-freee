package dev.Mardexx.client.modules;

import dev.Mardexx.client.settings.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Module {
	private final String name;
	private final ModuleCategory category;
	private final String description;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	private int keyBind = -1;

	public Module(String name, ModuleCategory category) {
		this(name, category, "No description.");
	}

	public Module(String name, ModuleCategory category, String description) {
		this(name, category, description, false);
	}

	protected Module(String name, ModuleCategory category, String description, boolean defaultEnabled) {
		this.name = name;
		this.category = category;
		this.description = description;
		this.enabled = defaultEnabled;
	}

	public String name() {
		return name;
	}

	public ModuleCategory category() {
		return category;
	}

	public String description() {
		return description;
	}

	public boolean enabled() {
		return enabled;
	}

	public int keyBind() {
		return keyBind;
	}

	public void setKeyBind(int keyBind) {
		this.keyBind = keyBind;
		ModuleManager.saveConfig();
	}

	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	protected void register(Setting<?>... settings) {
		Collections.addAll(this.settings, settings);
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}

		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
		ModuleManager.moduleStateChanged(this, enabled);
		ModuleManager.saveConfig();
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	public void onPreTick() {
	}

	public void onTick() {
	}

	public void onKey(int key, int action, boolean mouse) {
	}
}
