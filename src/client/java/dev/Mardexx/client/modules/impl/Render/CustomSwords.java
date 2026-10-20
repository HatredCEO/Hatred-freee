package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.CustomSwordSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CustomSwords extends Module {
	public static final String DEFAULT_MODEL = "Katana";
	public static final String[] MODELS = {
			"Abominable Blade", "Abominable Great Saber", "Abominable Scythe", "Acidic Cleaver",
			"Amethyst Shuriken", "Ancient Royal Great Sword", "Aquatic Sacred Blade", "Arcanethyst",
			"Ashura's Blade", "Awakened Lichblade", "Blood Edge", "Bloody Death", "Bramblethorn",
			"Brimstone Claymore", "Carian Knight's Sword", "Chrono Blade", "Corrupted Mythic Blade",
			"Creation Splitter", "Crescent Rose", "Cyber Katana", "Cyber Mantis Blade", "Cyber Sword",
			"Cybernetic Chainsaw Blade", "Cybernetic Katana", "Cybernetic Knife", "Dainsleif",
			"Dark Blade", "Dark Cleaver", "Death Knight's Dagger", "Death Knight's Sword",
			"Demigod's Unholy Blade", "Demigod's Unholy Halberd", "Demon Lord's Great Axe",
			"Demon Lord's Sword", "Demonic Blade", "Demonic Cleaver", "Divine Axe Rhitta",
			"Divine Justice", "Divine Punisher", "Divine Reaper", "Dragon Slaying blade",
			"Edge Of The Astral Plane", "Emberblade", "Enigma", "Epic Sword", "Estoc",
			"Fallen God's Spear", "Fallen God's Sword", "Floral Longsword", "Floral Sabre",
			"Forest Guardian's Glaive", "Frost Axe", "Frost Blade", "Frost Scythe", "Hearthflame",
			"Hero Sword", "Holy Moonlight Sword", "Hornet's Needle", "Icewhisper", "Jade Halberd",
			"Katana", "Legendary Sword", "Longsword", "Magi Scythe", "Masamune", "Mjolnir",
			"Molten Blade", "Molten Sword", "Muramasa", "Mystical Spellblade", "Mythic Blade",
			"Ocean's Rage", "Partisan", "Pharaoh's Treasure", "Pheonix Grace", "Plague Longsword",
			"Power Fuse Hammer", "Power Fuse Sword", "Requiem of the Ninth Abyss", "Ribbon Cleaver",
			"Righteous Relic", "Rivers Of Blood", "Royal Chakram", "Royal Rapier", "Sabre",
			"Scissor Blade", "Sculk Cleaver", "Sculk Scythe", "Sculk Sword", "Sentinel's Will",
			"Silverine Blade", "Soul Claws", "Soul Collector", "Soul Devourer", "Soul Edge",
			"Soul Harvester", "Soul Stealer", "Soulrender", "Star's Edge", "Steel Sword",
			"Stop Sign", "Storm Bringer", "Storm's Edge", "Sunbreak", "Tengen's Blade",
			"Terra Blade", "Thousand Demon Daggers", "Thunder Bringer", "Thunderbrand",
			"True Excalibur", "Vampiric Needle", "Wakizashi", "Watcher Claymore",
			"Watching Warglaive", "Waxweaver", "Whisperwind", "Wickpiercer", "Wraith Scythe", "Yoru"
	};
	private static final List<String> MODEL_LIST = List.of(MODELS);
	private static final CustomSwords INSTANCE = new CustomSwords();
	private static final ThreadLocal<Boolean> PICKER_PREVIEW_RENDER = ThreadLocal.withInitial(() -> false);

	private final CustomSwordSetting models = new CustomSwordSetting(
			"models",
			"Sword models",
			"Choose custom visuals for every sword type and mace."
	);

	private CustomSwords() {
		super("Custom Swords", ModuleCategory.RENDER, "Replaces rendered sword and mace models with custom resource-pack models.");
		register(models);
	}

	public static CustomSwords getInstance() {
		return INSTANCE;
	}

	public static boolean isKnownModel(String model) {
		return MODEL_LIST.contains(model);
	}

	public static List<String> modelList() {
		return MODEL_LIST;
	}

	public CustomSwordSetting modelSetting() {
		return models;
	}

	public static void beginPickerPreviewRender() {
		PICKER_PREVIEW_RENDER.set(true);
	}

	public static void endPickerPreviewRender() {
		PICKER_PREVIEW_RENDER.set(false);
	}

	public ItemStack renderStack(ItemStack stack) {
		if (PICKER_PREVIEW_RENDER.get() || !enabled() || stack == null || stack.isEmpty()) {
			return stack;
		}

		Target target = Target.from(stack.getItem());
		if (target == null) {
			return stack;
		}
		if (!models.enabled(target)) {
			return stack;
		}

		String model = models.model(target);
		if (!isKnownModel(model)) {
			return stack;
		}

		return withModel(stack, model);
	}

	public ItemStack previewStack(Target target) {
		String model = models.model(target);
		return previewStack(target, model);
	}

	public ItemStack previewStack(Target target, String model) {
		return isKnownModel(model) ? withModel(target.createPreviewStack(), model) : target.createPreviewStack();
	}

	private static ItemStack withModel(ItemStack stack, String model) {
		ItemStack copy = stack.copy();
		copy.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
				List.of(),
				List.of(),
				List.of(model),
				List.of()
		));
		return copy;
	}

	public enum Target {
		WOODEN("Wooden", "Wood", Items.WOODEN_SWORD),
		STONE("Stone", "Stone", Items.STONE_SWORD),
		IRON("Iron", "Iron", Items.IRON_SWORD),
		GOLDEN("Golden", "Gold", Items.GOLDEN_SWORD),
		DIAMOND("Diamond", "Dia", Items.DIAMOND_SWORD),
		NETHERITE("Netherite", "Neth", Items.NETHERITE_SWORD),
		MACE("Mace", "Mace", Items.MACE);

		private final String display;
		private final String shortDisplay;
		private final Item item;

		Target(String display, String shortDisplay, Item item) {
			this.display = display;
			this.shortDisplay = shortDisplay;
			this.item = item;
		}

		public String display() {
			return display;
		}

		public String shortDisplay() {
			return shortDisplay;
		}

		public Item item() {
			return item;
		}

		public ItemStack createPreviewStack() {
			return new ItemStack(item);
		}

		public static Target from(Item item) {
			return Arrays.stream(values())
					.filter(target -> target.item == item)
					.findFirst()
					.orElse(null);
		}

		@Override
		public String toString() {
			return display.toLowerCase(Locale.ROOT);
		}
	}
}
