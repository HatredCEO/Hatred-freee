package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatSetting;

public final class AspectRatio extends Module {
	private static final AspectRatio INSTANCE = new AspectRatio();

	private final EnumSetting<Mode> mode = new EnumSetting<>(
			"mode",
			"Mode",
			"Selects the camera aspect ratio.",
			Mode.CUSTOM
	);
	private final FloatSetting customRatio = (FloatSetting) new FloatSetting(
			"custom_ratio",
			"Custom Ratio",
			"Custom camera width/height ratio.",
			0.1F,
			1.9F,
			5.0F
	).step(0.1F).precision(1);

	private AspectRatio() {
		super("Aspect Ratio", ModuleCategory.RENDER, "Changes the camera aspect ratio.");
		customRatio.visibleWhen(() -> mode.is(Mode.CUSTOM));
		register(mode, customRatio);
	}

	public static AspectRatio getInstance() {
		return INSTANCE;
	}

	public float ratio() {
		return switch (mode.value()) {
			case FOUR_THREE -> 4.0F / 3.0F;
			case SIXTEEN_NINE -> 16.0F / 9.0F;
			case ONE_ONE -> 1.0F;
			case SIXTEEN_TEN -> 16.0F / 10.0F;
			case CUSTOM -> customRatio.floatValue();
		};
	}

	public enum Mode {
		FOUR_THREE("4:3"),
		SIXTEEN_NINE("16:9"),
		ONE_ONE("1:1"),
		SIXTEEN_TEN("16:10"),
		CUSTOM("Custom");

		private final String display;

		Mode(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}
}
