package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.FloatSetting;

public class Interface extends Module {
	private static final Interface INSTANCE = new Interface();

	private final BooleanSetting watermark = new BooleanSetting("Watermark", "Watermark", "Displays client info, FPS, Ping, and TPS.", true);
	private final BooleanSetting activeModules = new BooleanSetting("Active Modules", "Active Modules", "Displays active module list (ArrayList).", true);
	private final BooleanSetting keybinds = new BooleanSetting("Keybinds", "Keybinds", "Displays active hotkeys on screen.", true);
	private final BooleanSetting potionList = new BooleanSetting("Potion List", "Potion List", "Displays active potion effects and durations.", true);
	private final BooleanSetting cooldowns = new BooleanSetting("Cooldowns", "Cooldowns", "Displays active item and pearl cooldowns.", true);
	private final BooleanSetting targetHud = new BooleanSetting("Target HUD", "Target HUD", "Displays target health, gear and status.", true);
	private final BooleanSetting notifications = new BooleanSetting("Notifications", "Notifications", "Displays event and toggle notifications.", true);
	private final BooleanSetting mediaPlayer = new BooleanSetting("Media Player", "Media Player", "Displays currently playing media.", true);
	private final BooleanSetting helperHud = new BooleanSetting("Helper HUD", "Helper HUD", "Displays auto-totem and helper widget.", true);
	private final FloatSetting scale = new FloatSetting("Scale", "Scale", "Global scale factor for all HUD widgets.", 0.5F, 1.0F, 1.5F).step(0.05F);
	private final FloatSetting transparency = new FloatSetting("Transparency", "Transparency", "Background transparency for HUD widgets.", 0.0F, 1.0F, 1.0F).step(0.05F);

	private Interface() {
		super("Interface", ModuleCategory.RENDER, "Customizable HUD interface and widgets.", true);
		setEnabled(true);
		register(
				watermark,
				activeModules,
				keybinds,
				potionList,
				cooldowns,
				targetHud,
				notifications,
				mediaPlayer,
				helperHud,
				scale,
				transparency
		);
	}

	public static Interface getInstance() {
		return INSTANCE;
	}

	public BooleanSetting watermark() {
		return watermark;
	}

	public BooleanSetting activeModules() {
		return activeModules;
	}

	public BooleanSetting keybinds() {
		return keybinds;
	}

	public BooleanSetting potionList() {
		return potionList;
	}

	public BooleanSetting cooldowns() {
		return cooldowns;
	}

	public BooleanSetting targetHud() {
		return targetHud;
	}

	public BooleanSetting notifications() {
		return notifications;
	}

	public BooleanSetting mediaPlayer() {
		return mediaPlayer;
	}

	public BooleanSetting helperHud() {
		return helperHud;
	}

	public FloatSetting scale() {
		return scale;
	}

	public FloatSetting transparency() {
		return transparency;
	}

	public boolean isWidgetEnabled(String widgetName) {
		return switch (widgetName) {
			case "Watermark" -> watermark.enabled();
			case "Active Modules" -> activeModules.enabled();
			case "Keybinds" -> keybinds.enabled();
			case "Potion List" -> potionList.enabled();
			case "Cooldowns" -> cooldowns.enabled();
			case "Target HUD" -> targetHud.enabled();
			case "Notifications" -> notifications.enabled();
			case "Media Player" -> mediaPlayer.enabled();
			case "Helper HUD" -> helperHud.enabled();
			default -> true;
		};
	}
}
