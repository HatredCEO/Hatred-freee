package dev.Mardexx.client.settings;

public final class ColorSetting extends Setting<Integer> {
	public ColorSetting(String name, int defaultColor) {
		super(name, opaque(defaultColor));
	}

	public ColorSetting(String name, String displayName, String description, int defaultColor) {
		super(name, displayName, description, opaque(defaultColor));
	}

	public int color() {
		return value();
	}

	public void setColor(int color) {
		setValue(opaque(color));
	}

	public String hex() {
		return String.format("#%06X", color() & 0x00FFFFFF);
	}

	private static int opaque(int color) {
		return 0xFF000000 | color & 0x00FFFFFF;
	}
}
