package dev.Mardexx.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleManager;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.ColorSetting;
import dev.Mardexx.client.settings.CustomSwordSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatRangeSetting;
import dev.Mardexx.client.settings.FloatSetting;
import dev.Mardexx.client.settings.HelperCustomItemsSetting;
import dev.Mardexx.client.settings.KeySetting;
import dev.Mardexx.client.settings.ItemStackSetting;
import dev.Mardexx.client.settings.Setting;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

public final class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILE = "hatred-free.json";

	private static String activeConfigName = "default";
	private static float guiScale = 1.0F;
	private static boolean guiAnimations = true;

	public static float getGuiScale() {
		return guiScale;
	}

	public static void setGuiScale(float scale) {
		guiScale = Math.max(0.70F, Math.min(1.30F, scale));
	}

	public static boolean isGuiAnimations() {
		return guiAnimations;
	}

	public static void setGuiAnimations(boolean animations) {
		guiAnimations = animations;
	}

	private ClientConfig() {
	}

	public static java.nio.file.Path configsDir() {
		java.nio.file.Path dir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("hatred-configs");
		try {
			java.nio.file.Files.createDirectories(dir);
		} catch (java.io.IOException ignored) {
		}
		return dir;
	}

	public static java.util.List<String> listConfigs() {
		java.util.List<String> list = new java.util.ArrayList<>();
		java.nio.file.Path dir = configsDir();
		if (java.nio.file.Files.isDirectory(dir)) {
			try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(dir)) {
				stream.filter(p -> p.getFileName().toString().endsWith(".json"))
						.map(p -> {
							String fn = p.getFileName().toString();
							return fn.substring(0, fn.length() - 5);
						})
						.sorted(String.CASE_INSENSITIVE_ORDER)
						.forEach(list::add);
			} catch (java.io.IOException ignored) {
			}
		}
		if (!list.contains("default")) {
			list.add(0, "default");
		}
		return list;
	}

	public static String getActiveConfig() {
		return activeConfigName;
	}

	public static void setActiveConfig(String name) {
		if (name != null && !name.isBlank()) {
			activeConfigName = name.trim();
		}
	}

	public static boolean loadNamed(String name) {
		if (name == null || name.isBlank()) return false;
		String cleanName = name.trim();
		java.nio.file.Path file = configsDir().resolve(cleanName + ".json");
		if (!java.nio.file.Files.isRegularFile(file)) {
			if (cleanName.equalsIgnoreCase("default")) {
				load();
				activeConfigName = "default";
				return true;
			}
			return false;
		}

		try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
			com.google.gson.JsonElement rootElement = com.google.gson.JsonParser.parseReader(reader);
			if (rootElement.isJsonObject()) {
				loadFromObject(rootElement.getAsJsonObject());
				activeConfigName = cleanName;
				save();
				return true;
			}
		} catch (java.io.IOException | RuntimeException ignored) {
		}
		return false;
	}

	public static boolean saveNamed(String name) {
		if (name == null || name.isBlank()) return false;
		String cleanName = name.trim();
		java.nio.file.Path dir = configsDir();
		java.nio.file.Path file = dir.resolve(cleanName + ".json");
		try {
			java.nio.file.Files.createDirectories(dir);
			try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
				GSON.toJson(root(), writer);
			}
			activeConfigName = cleanName;
			save();
			return true;
		} catch (java.io.IOException ignored) {
			return false;
		}
	}

	public static boolean deleteNamed(String name) {
		if (name == null || name.isBlank()) return false;
		String cleanName = name.trim();
		if (cleanName.equalsIgnoreCase("default")) return false;
		java.nio.file.Path file = configsDir().resolve(cleanName + ".json");
		try {
			boolean deleted = java.nio.file.Files.deleteIfExists(file);
			if (activeConfigName.equalsIgnoreCase(cleanName)) {
				activeConfigName = "default";
			}
			return deleted;
		} catch (java.io.IOException ignored) {
			return false;
		}
	}

	public static void load() {
		Path path = configPath();
		if (!Files.isRegularFile(path)) {
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement rootElement = JsonParser.parseReader(reader);
			if (!rootElement.isJsonObject()) {
				return;
			}
			loadFromObject(rootElement.getAsJsonObject());
		} catch (IOException | RuntimeException ignored) {
		}
	}

	public static void loadFromObject(JsonObject root) {
		if (root == null) {
			return;
		}
		HudManager.load(object(root, "hud"));
		if (root.has("guiScale")) {
			try {
				setGuiScale(root.get("guiScale").getAsFloat());
			} catch (Exception ignored) {
			}
		}
		if (root.has("guiAnimations")) {
			try {
				setGuiAnimations(root.get("guiAnimations").getAsBoolean());
			} catch (Exception ignored) {
			}
		}
		JsonObject modules = object(root, "modules");
		if (modules == null) {
			return;
		}

		for (Module module : ModuleManager.modules()) {
			JsonObject moduleObject = object(modules, module.name());
			if (moduleObject == null && module.name().equals("Shader Sky")) {
				moduleObject = object(modules, "Aurora Sky");
			}
			if (moduleObject == null) {
				continue;
			}

			loadModule(module, moduleObject);
		}
	}

	public static void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(root(), writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static JsonObject root() {
		JsonObject root = new JsonObject();
		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.modules()) {
			modules.add(module.name(), saveModule(module));
		}
		root.add("modules", modules);
		root.add("hud", HudManager.save());
		root.addProperty("guiScale", guiScale);
		root.addProperty("guiAnimations", guiAnimations);
		return root;
	}

	private static JsonObject saveModule(Module module) {
		JsonObject object = new JsonObject();
		object.addProperty("enabled", module.enabled());
		object.addProperty("keyBind", module.keyBind());

		JsonObject settings = new JsonObject();
		for (Setting<?> setting : module.settings()) {
			settings.add(setting.name(), saveSetting(setting));
		}
		object.add("settings", settings);
		return object;
	}

	private static JsonElement saveSetting(Setting<?> setting) {
		if (setting instanceof ColorSetting colorSetting) {
			return GSON.toJsonTree(colorSetting.hex());
		}
		if (setting instanceof BooleanSetting booleanSetting) {
			return GSON.toJsonTree(booleanSetting.enabled());
		}
		if (setting instanceof FloatSetting floatSetting) {
			return GSON.toJsonTree(floatSetting.floatValue());
		}
		if (setting instanceof KeySetting keySetting) {
			return GSON.toJsonTree(keySetting.key());
		}
		if (setting instanceof ItemStackSetting itemStackSetting) {
			JsonObject object = new JsonObject();
			if (itemStackSetting.hasValue()) {
				object.addProperty("item", itemStackSetting.itemId());
				object.addProperty("name", itemStackSetting.selectedName());
				object.addProperty("components", itemStackSetting.componentSignature());
			}
			return object;
		}
		if (setting instanceof CustomSwordSetting customSwordSetting) {
			JsonObject object = new JsonObject();
			customSwordSetting.value().forEach((target, model) -> {
				JsonObject targetObject = new JsonObject();
				targetObject.addProperty("model", model);
				targetObject.addProperty("enabled", customSwordSetting.enabled(target));
				object.add(target.name(), targetObject);
			});
			return object;
		}
		if (setting instanceof HelperCustomItemsSetting customItemsSetting) {
			JsonObject root = new JsonObject();
			root.addProperty("active", customItemsSetting.activeIndex());
			com.google.gson.JsonArray profiles = new com.google.gson.JsonArray();
			for (HelperCustomItemsSetting.Profile profile : customItemsSetting.profiles()) {
				JsonObject profileObject = new JsonObject();
				profileObject.addProperty("name", profile.name());
				com.google.gson.JsonArray items = new com.google.gson.JsonArray();
				for (HelperCustomItemsSetting.Entry entry : profile.entries()) {
					JsonObject object = new JsonObject();
					object.addProperty("item", entry.itemId());
					object.addProperty("name", entry.name());
					object.addProperty("components", entry.componentSignature());
					object.addProperty("key", entry.key());
					items.add(object);
				}
				profileObject.add("items", items);
				profiles.add(profileObject);
			}
			root.add("profiles", profiles);
			return root;
		}
		if (setting instanceof FloatRangeSetting rangeSetting) {
			JsonObject object = new JsonObject();
			object.addProperty("from", rangeSetting.from());
			object.addProperty("to", rangeSetting.to());
			return object;
		}
		if (setting instanceof EnumSetting<?> enumSetting) {
			return GSON.toJsonTree(enumSetting.value().name());
		}
		if (setting instanceof EnumBooleanMapSetting<?> mapSetting) {
			JsonObject object = new JsonObject();
			for (Enum<?> value : mapSetting.value().keySet()) {
				object.addProperty(value.name(), Boolean.TRUE.equals(mapSetting.value().get(value)));
			}
			return object;
		}
		return GSON.toJsonTree(setting.value());
	}

	private static void loadModule(Module module, JsonObject object) {
		if (object.has("keyBind") && object.get("keyBind").isJsonPrimitive()) {
			module.setKeyBind(object.get("keyBind").getAsInt());
		}

		JsonObject settings = object(object, "settings");
		if (settings != null) {
			for (Setting<?> setting : module.settings()) {
				JsonElement value = settings.get(setting.name());
				if (value != null && !value.isJsonNull()) {
					loadSetting(setting, value);
				}
			}
		}

		if (object.has("enabled") && object.get("enabled").isJsonPrimitive()) {
			module.setEnabled(object.get("enabled").getAsBoolean());
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void loadSetting(Setting<?> setting, JsonElement value) {
		if (setting instanceof ColorSetting colorSetting && value.isJsonPrimitive()) {
			loadColor(colorSetting, value);
			return;
		}
		if (setting instanceof BooleanSetting booleanSetting && value.isJsonPrimitive()) {
			booleanSetting.setValue(value.getAsBoolean());
			return;
		}
		if (setting instanceof FloatSetting floatSetting && value.isJsonPrimitive()) {
			floatSetting.setFloatValue(value.getAsFloat());
			return;
		}
		if (setting instanceof KeySetting keySetting && value.isJsonPrimitive()) {
			keySetting.setKey(value.getAsInt());
			return;
		}
		if (setting instanceof ItemStackSetting itemStackSetting && value.isJsonObject()) {
			JsonObject object = value.getAsJsonObject();
			if (!object.has("item")) {
				itemStackSetting.clear();
				return;
			}
			itemStackSetting.restore(
					object.get("item").getAsString(),
					object.has("name") ? object.get("name").getAsString() : "",
					object.has("components") ? object.get("components").getAsString() : ""
			);
			return;
		}
		if (setting instanceof CustomSwordSetting customSwordSetting && value.isJsonObject()) {
			JsonObject object = value.getAsJsonObject();
			for (String key : object.keySet()) {
				dev.Mardexx.client.modules.impl.Render.CustomSwords.Target target;
				try {
					target = dev.Mardexx.client.modules.impl.Render.CustomSwords.Target.valueOf(key);
				} catch (IllegalArgumentException ignored) {
					continue;
				}

				JsonElement element = object.get(key);
				if (element != null && element.isJsonPrimitive()) {
					customSwordSetting.loadTarget(target, element.getAsString(), null);
				} else if (element != null && element.isJsonObject()) {
					JsonObject targetObject = element.getAsJsonObject();
					String model = targetObject.has("model") && targetObject.get("model").isJsonPrimitive()
							? targetObject.get("model").getAsString()
							: null;
					Boolean enabled = targetObject.has("enabled") && targetObject.get("enabled").isJsonPrimitive()
							? targetObject.get("enabled").getAsBoolean()
							: null;
					customSwordSetting.loadTarget(target, model, enabled);
				}
			}
			return;
		}
		if (setting instanceof HelperCustomItemsSetting customItemsSetting) {
			if (value.isJsonArray()) {
				customItemsSetting.load(loadHelperCustomEntries(value.getAsJsonArray()));
				return;
			}
			if (!value.isJsonObject()) {
				return;
			}
			JsonObject object = value.getAsJsonObject();
			java.util.ArrayList<HelperCustomItemsSetting.Profile> profiles = new java.util.ArrayList<>();
			com.google.gson.JsonArray profileArray = object.has("profiles") && object.get("profiles").isJsonArray()
					? object.getAsJsonArray("profiles")
					: new com.google.gson.JsonArray();
			for (JsonElement profileElement : profileArray) {
				if (!profileElement.isJsonObject()) {
					continue;
				}
				JsonObject profileObject = profileElement.getAsJsonObject();
				String name = profileObject.has("name") ? profileObject.get("name").getAsString() : "Custom";
				com.google.gson.JsonArray items = profileObject.has("items") && profileObject.get("items").isJsonArray()
						? profileObject.getAsJsonArray("items")
						: new com.google.gson.JsonArray();
				profiles.add(new HelperCustomItemsSetting.Profile(name, loadHelperCustomEntries(items)));
			}
			int active = object.has("active") && object.get("active").isJsonPrimitive() ? object.get("active").getAsInt() : 0;
			customItemsSetting.loadProfiles(profiles, active);
			return;
		}
		if (setting instanceof FloatRangeSetting rangeSetting && value.isJsonObject()) {
			JsonObject object = value.getAsJsonObject();
			if (object.has("from") && object.has("to")) {
				rangeSetting.setRange(object.get("from").getAsFloat(), object.get("to").getAsFloat());
			}
			return;
		}
		if (setting instanceof EnumSetting enumSetting && value.isJsonPrimitive()) {
			setEnumValue(enumSetting, value.getAsString());
			return;
		}
		if (setting instanceof EnumBooleanMapSetting mapSetting && value.isJsonObject()) {
			setEnumBooleanValues(mapSetting, value.getAsJsonObject());
		}
	}

	private static void loadColor(ColorSetting setting, JsonElement value) {
		try {
			if (value.getAsJsonPrimitive().isNumber()) {
				setting.setColor(value.getAsInt());
				return;
			}

			String hex = value.getAsString().trim();
			if (hex.startsWith("#")) {
				hex = hex.substring(1);
			}
			setting.setColor(Integer.parseUnsignedInt(hex, 16));
		} catch (IllegalArgumentException ignored) {
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void setEnumValue(EnumSetting setting, String name) {
		for (Enum value : (Enum[]) setting.values()) {
			if (value.name().equals(name) || ("LIGHTNING".equals(name) && value.name().equals("THUNDER"))) {
				setting.setValue(value);
				return;
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void setEnumBooleanValues(EnumBooleanMapSetting setting, JsonObject object) {
		EnumMap values = (EnumMap) setting.value();
		for (Object key : values.keySet()) {
			Enum value = (Enum) key;
			JsonElement enabled = object.get(value.name());
			if (enabled != null && enabled.isJsonPrimitive()) {
				values.put(value, enabled.getAsBoolean());
			}
		}
		setting.setValue(values);
	}

	private static java.util.ArrayList<HelperCustomItemsSetting.Entry> loadHelperCustomEntries(com.google.gson.JsonArray array) {
		java.util.ArrayList<HelperCustomItemsSetting.Entry> entries = new java.util.ArrayList<>();
		for (JsonElement element : array) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject object = element.getAsJsonObject();
			if (!object.has("item")) {
				continue;
			}
			entries.add(new HelperCustomItemsSetting.Entry(
					object.get("item").getAsString(),
					object.has("name") ? object.get("name").getAsString() : "",
					object.has("components") ? object.get("components").getAsString() : "",
					object.has("key") ? object.get("key").getAsInt() : -1
			));
		}
		return entries;
	}

	private static JsonObject object(JsonObject parent, String name) {
		JsonElement element = parent.get(name);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static Path configPath() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_FILE);
	}
}
