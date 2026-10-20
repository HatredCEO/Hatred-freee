package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatSetting;

public class AuroraSky extends Module {
	private static final AuroraSky INSTANCE = new AuroraSky();
	private final EnumSetting<ShaderStyle> shader = new EnumSetting<>(
			"shader", "Shader", "Selects the procedural sky effect.", ShaderStyle.AURORA
	);

	private final FloatSetting intensity = new FloatSetting(
			"intensity",
			"Intensity",
			"Controls aurora brightness.",
			0.1F,
			0.85F,
			1.5F
	).step(0.05F).precision(2);
	private final FloatSetting speed = new FloatSetting(
			"speed",
			"Speed",
			"Controls aurora movement speed.",
			0.0F,
			0.55F,
			2.0F
	).step(0.05F).precision(2);
	private final FloatSetting scale = new FloatSetting(
			"scale", "Scale", "Controls the size and frequency of the aurora pattern.",
			0.5F, 1.0F, 2.0F
	).step(0.05F).precision(2);
	private final FloatSetting height = new FloatSetting(
			"height", "Height", "Moves the aurora curtains vertically across the sky.",
			0.15F, 0.34F, 0.65F
	).step(0.01F).precision(2);
	private final FloatSetting curtainWidth = new FloatSetting(
			"curtain_width", "Curtain Width", "Controls how wide each aurora curtain appears.",
			0.5F, 1.0F, 2.0F
	).step(0.05F).precision(2);
	private final FloatSetting curtainCount = new FloatSetting(
			"curtain_count", "Curtain Count", "Controls the number of layered aurora curtains.",
			1.0F, 4.0F, 6.0F
	).step(1.0F).precision(0);
	private final BooleanSetting stars = new BooleanSetting(
			"stars", "Stars", "Renders procedural stars together with the sky shader.", true
	);
	private final FloatSetting skyTint = new FloatSetting(
			"sky_tint", "Sky Tint", "Controls how strongly the theme color affects the night sky.",
			0.0F, 0.62F, 1.0F
	).step(0.05F).precision(2);
	private final FloatSetting opacity = new FloatSetting(
			"opacity", "Opacity", "Controls the opacity of the complete sky shader.",
			0.2F, 0.88F, 1.0F
	).step(0.05F).precision(2);

	private AuroraSky() {
		super("Shader Sky", ModuleCategory.RENDER, "Renders a customizable procedural sky matching the client theme.");
		register(shader, intensity, speed, scale, height, curtainWidth, curtainCount, stars, skyTint, opacity);
	}

	public int shaderStyle() {
		return shader.value().ordinal();
	}

	public float intensity() {
		return intensity.value();
	}

	public float speed() {
		return speed.value();
	}

	public float scale() {
		return scale.value();
	}

	public float height() {
		return height.value();
	}

	public float curtainWidth() {
		return curtainWidth.value();
	}

	public float curtainCount() {
		return curtainCount.value();
	}

	public boolean stars() {
		return stars.enabled();
	}

	public float skyTint() {
		return skyTint.value();
	}

	public float opacity() {
		return opacity.value();
	}

	public static AuroraSky getInstance() {
		return INSTANCE;
	}

	public enum ShaderStyle {
		AURORA("Aurora"),
		NEBULA("Nebula"),
		GALAXY("Galaxy"),
		VORTEX("Vortex"),
		SMOKE_BUBBLES("Smoke Bubbles"),
		CHILL_SMOKE_ORB("Chill Smoke Orb"),
		SMOKE_NOISE("Smoke Noise"),
		WATER_CAUSTICS("Water Caustics"),
		WORLEY_WATER("Worley Water"),
		PERLIN_WATER("Perlin Water");

		private final String label;

		ShaderStyle(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		@Override
		public String toString() {
			return label;
		}
	}
}
