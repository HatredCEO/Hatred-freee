package dev.Mardexx.client.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FriendManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILE = "hatred-free-friends.json";
	private static final Map<String, FriendEntry> FRIENDS = new LinkedHashMap<>();
	private static long revision;

	private FriendManager() {
	}

	public static synchronized void load() {
		FRIENDS.clear();
		Path path = configPath();
		if (!Files.isRegularFile(path)) {
			revision++;
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (!root.isJsonArray()) {
				return;
			}
			for (JsonElement element : root.getAsJsonArray()) {
				if (!element.isJsonObject()) {
					continue;
				}
				JsonObject object = element.getAsJsonObject();
				String name = string(object, "name");
				String alias = string(object, "alias");
				if (!name.isBlank()) {
					FRIENDS.put(normalize(name), new FriendEntry(clean(name, 32), clean(alias, 32)));
				}
			}
		} catch (IOException | RuntimeException ignored) {
		} finally {
			revision++;
		}
	}

	public static synchronized boolean addOrUpdate(String name, String alias) {
		String cleanedName = clean(name, 32);
		if (cleanedName.isBlank()) {
			return false;
		}
		FRIENDS.put(normalize(cleanedName), new FriendEntry(cleanedName, clean(alias, 32)));
		revision++;
		save();
		return true;
	}

	public static synchronized void remove(String name) {
		if (FRIENDS.remove(normalize(name)) != null) {
			revision++;
			save();
		}
	}

	public static synchronized boolean isFriend(String name) {
		return FRIENDS.containsKey(normalize(name));
	}

	public static synchronized String alias(String name) {
		FriendEntry entry = FRIENDS.get(normalize(name));
		return entry == null ? "" : entry.alias();
	}

	public static synchronized String displayName(String name) {
		FriendEntry entry = FRIENDS.get(normalize(name));
		if (entry == null || entry.alias().isBlank()) {
			return name;
		}
		return entry.alias();
	}

	public static synchronized List<FriendEntry> friends() {
		return List.copyOf(new ArrayList<>(FRIENDS.values()));
	}

	public static synchronized long revision() {
		return revision;
	}

	public static synchronized void save() {
		Path path = configPath();
		JsonArray array = new JsonArray();
		for (FriendEntry friend : FRIENDS.values()) {
			JsonObject object = new JsonObject();
			object.addProperty("name", friend.name());
			object.addProperty("alias", friend.alias());
			array.add(object);
		}

		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(array, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static Path configPath() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_FILE);
	}

	private static String string(JsonObject object, String key) {
		JsonElement element = object.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static String clean(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		String cleaned = value.trim();
		return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
	}

	public record FriendEntry(String name, String alias) {
		public String shownName() {
			return alias.isBlank() ? name : alias;
		}
	}
}
