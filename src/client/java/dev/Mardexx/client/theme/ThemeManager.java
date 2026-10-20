package dev.Mardexx.client.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.Mardexx.client.settings.ColorSetting;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ThemeManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILE = "hatred-free-theme.json";
	private static final ColorSetting ACCENT = new ColorSetting(
			"theme_accent",
			"Theme Accent",
			"Controls the accent color used by the client GUI.",
			0xFFE97AF8
	);

	private ThemeManager() {
	}

	public static ColorSetting accentSetting() {
		return ACCENT;
	}

	public static int accent() {
		return ACCENT.color();
	}

	public static int darkAccent() {
		return mix(ACCENT.color(), 0xFF090909, 0.42F);
	}

	public static int mutedAccent() {
		return mix(ACCENT.color(), 0xFF090909, 0.22F);
	}

	public static int textOnAccent() {
		int color = ACCENT.color();
		float luminance = ((color >>> 16) & 255) * 0.2126F
				+ ((color >>> 8) & 255) * 0.7152F
				+ (color & 255) * 0.0722F;
		return luminance > 150.0F ? 0xFF120F13 : 0xFFF7F7F7;
	}

	public static void load() {
		Path path = configPath();
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (root.isJsonObject()) {
				JsonElement accent = root.getAsJsonObject().get("accent");
				if (accent != null && accent.isJsonPrimitive()) {
					ACCENT.setColor(parseColor(accent.getAsString(), ACCENT.color()));
				}
			}
		} catch (IOException | RuntimeException ignored) {
		}
	}

	public static void save() {
		Path path = configPath();
		JsonObject root = new JsonObject();
		root.addProperty("accent", ACCENT.hex());
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static int parseColor(String value, int fallback) {
		String normalized = value == null ? "" : value.trim().replace("#", "");
		if (normalized.length() != 6) {
			return fallback;
		}
		return 0xFF000000 | Integer.parseInt(normalized, 16);
	}

	private static int mix(int color, int other, float amount) {
		float inverse = 1.0F - amount;
		int red = Math.round(((color >>> 16) & 255) * amount + ((other >>> 16) & 255) * inverse);
		int green = Math.round(((color >>> 8) & 255) * amount + ((other >>> 8) & 255) * inverse);
		int blue = Math.round((color & 255) * amount + (other & 255) * inverse);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private static Path configPath() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_FILE);
	}
}
