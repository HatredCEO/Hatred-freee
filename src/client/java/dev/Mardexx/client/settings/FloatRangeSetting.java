package dev.Mardexx.client.settings;

import java.util.concurrent.ThreadLocalRandom;

public final class FloatRangeSetting extends Setting<FloatRangeSetting.Range> {
	public record Range(float from, float to) {
	}

	private final float min;
	private final float max;
	private float step;
	private int precision = 1;
	private String suffix;

	public FloatRangeSetting(String name, float min, float from, float to, float max) {
		super(name, normalize(min, from, to, max));
		this.min = min;
		this.max = max;
	}

	public FloatRangeSetting(String name, String displayName, String description, float min, float from, float to, float max) {
		super(name, displayName, description, normalize(min, from, to, max));
		this.min = min;
		this.max = max;
	}

	public FloatRangeSetting step(float step) {
		this.step = step;
		return this;
	}

	public FloatRangeSetting precision(int precision) {
		this.precision = precision;
		return this;
	}

	public FloatRangeSetting suffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	public float minLimit() {
		return min;
	}

	public float maxLimit() {
		return max;
	}

	public float from() {
		return value().from();
	}

	public float to() {
		return value().to();
	}

	public float randomValue() {
		Range range = value();
		if (range.from() == range.to()) {
			return range.from();
		}

		float raw = range.from() + ThreadLocalRandom.current().nextFloat() * (range.to() - range.from());
		if (step > 0.0F) {
			int slot = Math.round((raw - min) / step);
			raw = min + slot * step;
		}

		return clamp(round(raw), range.from(), range.to());
	}

	public void setRange(float from, float to) {
		setValue(normalize(min, from, to, max));
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

	@Override
	protected Range copy(Range value) {
		return value == null ? null : new Range(value.from(), value.to());
	}

	private float round(float value) {
		if (precision < 0) {
			return value;
		}

		float scale = (float) Math.pow(10.0, precision);
		return Math.round(value * scale) / scale;
	}

	private static Range normalize(float min, float from, float to, float max) {
		if (Float.isNaN(min) || Float.isNaN(max) || min > max) {
			throw new IllegalArgumentException("Invalid range limits");
		}

		float normalizedFrom = clamp(from, min, max);
		float normalizedTo = clamp(to, min, max);
		return normalizedFrom <= normalizedTo
				? new Range(normalizedFrom, normalizedTo)
				: new Range(normalizedTo, normalizedFrom);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(value, max));
	}
}
