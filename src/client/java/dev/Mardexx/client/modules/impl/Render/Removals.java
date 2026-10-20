package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;

public final class Removals extends Module {
	private static final Removals INSTANCE = new Removals();

	private final BooleanSetting wardenEffect = setting("warden_effect", "Warden Effect", "Removes the darkness effect caused by wardens.");
	private final BooleanSetting blockBreakParticles = setting("block_break_particles", "Block Break Particles", "Removes block breaking and block hit particles.");
	private final BooleanSetting waterFog = setting("water_fog", "Water Fog", "Removes underwater distance fog.");
	private final BooleanSetting lavaFog = setting("lava_fog", "Lava Fog", "Removes lava distance fog.");
	private final BooleanSetting fog = setting("fog", "Fog", "Removes normal world fog.");
	private final BooleanSetting hurtCamera = setting("hurt_camera", "Hurt Camera", "Removes camera movement after taking damage.");
	private final BooleanSetting badEffects = setting("bad_effects", "Bad Effects", "Removes harmful effect icons and screen distortions.");
	private final BooleanSetting fishingRod = setting("fishing_rod", "Fishing Rod", "Hides fishing bobbers and their lines.");
	private final BooleanSetting inWallOverlay = setting("in_wall_overlay", "In Wall Overlay", "Removes the block overlay shown inside walls.");
	private final BooleanSetting fireOverlay = setting("fire_overlay", "Fire Overlay", "Removes flames covering the screen while burning.");

	private Removals() {
		super("Removals", ModuleCategory.RENDER, "Removes selected visual effects and render elements.");
		register(
				wardenEffect,
				blockBreakParticles,
				waterFog,
				lavaFog,
				fog,
				hurtCamera,
				badEffects,
				fishingRod,
				inWallOverlay,
				fireOverlay
		);
	}

	private static BooleanSetting setting(String name, String displayName, String description) {
		return new BooleanSetting(name, displayName, description, true);
	}

	public boolean removesWardenEffect() {
		return enabled() && wardenEffect.enabled();
	}

	public boolean removesBlockBreakParticles() {
		return enabled() && blockBreakParticles.enabled();
	}

	public boolean removesWaterFog() {
		return enabled() && waterFog.enabled();
	}

	public boolean removesLavaFog() {
		return enabled() && lavaFog.enabled();
	}

	public boolean removesFog() {
		return enabled() && fog.enabled();
	}

	public boolean removesHurtCamera() {
		return enabled() && hurtCamera.enabled();
	}

	public boolean removesBadEffects() {
		return enabled() && badEffects.enabled();
	}

	public boolean removesFishingRod() {
		return enabled() && fishingRod.enabled();
	}

	public boolean removesInWallOverlay() {
		return enabled() && inWallOverlay.enabled();
	}

	public boolean removesFireOverlay() {
		return enabled() && fireOverlay.enabled();
	}

	public static Removals getInstance() {
		return INSTANCE;
	}
}
