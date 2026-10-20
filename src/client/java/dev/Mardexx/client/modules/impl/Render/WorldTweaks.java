package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.ColorSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatSetting;
import dev.Mardexx.client.theme.ThemeManager;

public final class WorldTweaks extends Module {
	private static final WorldTweaks INSTANCE = new WorldTweaks();
	private final BooleanSetting changeTime = new BooleanSetting("change_time", "Change Time", "Overrides the visual time of day.", true);
	private final FloatSetting time = (FloatSetting) new FloatSetting("time", "Time", "Visual world time in hours (12 = Noon, 0 = Midnight).", 0.0F, 12.0F, 24.0F)
			.step(1.0F).precision(0).visibleWhen(changeTime::enabled);
	private final BooleanSetting changeFog = new BooleanSetting("change_fog", "Change Fog", "Overrides fog distance and optional color.", false);
	private final FloatSetting fogDistance = (FloatSetting) new FloatSetting("fog_distance", "Fog Distance", "Fog end distance in blocks.", 10.0F, 120.0F, 300.0F)
			.step(5.0F).precision(0).visibleWhen(changeFog::enabled);
	private final EnumSetting<ColorMode> colorMode = (EnumSetting<ColorMode>) new EnumSetting<>("fog_color_mode", "Fog Color Mode", "Fog color palette.", ColorMode.VANILLA)
			.visibleWhen(changeFog::enabled);
	private final ColorSetting fogColor = (ColorSetting) new ColorSetting("fog_color", "Fog Color", "Custom fog color.", 0xFF88AAFF)
			.visibleWhen(() -> changeFog.enabled() && colorMode.is(ColorMode.CUSTOM));

	private WorldTweaks() {
		super("World Tweaks", ModuleCategory.RENDER, "Small visual controls for world time and fog.");
		register(changeTime, time, changeFog, fogDistance, colorMode, fogColor);
	}

	public boolean changesTime() { return enabled() && changeTime.enabled(); }
	public boolean changesFog() { return enabled() && changeFog.enabled(); }
	public long dayTime() {
		float hours = time.floatValue();
		return (long) (((hours - 6.0F + 24.0F) % 24.0F) / 24.0F * 24000.0F);
	}
	public float fogDistance() { return fogDistance.floatValue(); }
	public ColorMode colorMode() { return colorMode.value(); }
	public int fogColor() { return colorMode.is(ColorMode.THEME) ? ThemeManager.accent() : fogColor.color(); }
	public static WorldTweaks getInstance() { return INSTANCE; }
	public enum ColorMode { VANILLA, THEME, CUSTOM }
}
