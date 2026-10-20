package dev.Mardexx.client.settings;

public final class FloatSetting extends Setting<Float> {
	private final float min;
	private final float max;
	private float step;
	private int precision = 1;
	private String suffix;

	public FloatSetting(String name, float min, float value, float max) {
		super(name, clamp(value, min, max));
		this.min = min;
		this.max = max;
	}

	public FloatSetting(String name, String displayName, String description, float min, float value, float max) {
		super(name, displayName, description, clamp(value, min, max));
		this.min = min;
		this.max = max;
	}

	public FloatSetting step(float step) {
		this.step = step;
		return this;
	}

	public FloatSetting precision(int precision) {
		this.precision = precision;
		return this;
	}

	public FloatSetting suffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	public float minLimit() {
		return min;
	}

	public float maxLimit() {
		return max;
	}

	public float floatValue() {
		return value();
	}

	public void setFloatValue(float value) {
		setValue(round(clamp(value, min, max)));
	}

	public float step() {
		return step;
	}

	public int precision() {
		return precision;
	}

	public String suffix() {
		return suffix;
	}

	private float round(float value) {
		if (precision < 0) {
			return value;
		}

		float scale = (float) Math.pow(10.0, precision);
		return Math.round(value * scale) / scale;
	}

	private static float clamp(float value, float min, float max) {
		if (Float.isNaN(min) || Float.isNaN(max) || min > max) {
			throw new IllegalArgumentException("Invalid slider limits");
		}

		return Math.max(min, Math.min(value, max));
	}
}
