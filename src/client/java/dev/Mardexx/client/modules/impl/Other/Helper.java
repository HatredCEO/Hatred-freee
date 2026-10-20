package dev.Mardexx.client.modules.impl.Other;

import dev.Mardexx.api.utils.player.MoveUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.HelperCustomItemsSetting;
import dev.Mardexx.client.settings.KeySetting;
import dev.Mardexx.mixins.MixinClientboundContainerSetSlotPacketAccessor;
import dev.Mardexx.mixins.MixinScreenTitleAccessor;
import dev.Mardexx.mixins.MixinServerboundContainerClickPacketAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.component.DataComponents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Helper extends Module {
	private static final Helper INSTANCE = new Helper();
	private static final long ITEM_USE_SLOWNESS_TOTAL_MS = 95L;
	private static final long ITEM_USE_DELAY_MS = 35L;
	private static final int REQUIRED_STOPPED_END_TICKS = 3;
	private static final String PRICE_RU = "$ \u0426\u0435\u043d\u0430: ";
	private static final String PRICE_RU_ALT = "$ \u0426\u0435\u043da: ";
	private static final String STORAGE_RU = "\u0425\u0440\u0430\u043d\u0438\u043b\u0438\u0449\u0435";

	private final EnumSetting<Server> server = new EnumSetting<>(
			"server", "Target Server", "Selects the server-specific helper behavior.", Server.FUN_TIME);
	private final EnumBooleanMapSetting<Feature> features = EnumBooleanMapSetting.create(
			"features", "Helper Features", "Auction tools available on FunTime and SpookyTime.", Feature.class,
			defaults -> {
				defaults.put(Feature.AUCTION_ASSISTANT, true);
				defaults.put(Feature.SORT_BY_PRICE, true);
			});
	private final EnumBooleanMapSetting<ArmorFilter> armorFilters = EnumBooleanMapSetting.create(
			"armor_filters", "Armor Filter", "Required armor properties for auction filtering.", ArmorFilter.class,
			defaults -> {
				defaults.put(ArmorFilter.NO_THORNS, true);
				defaults.put(ArmorFilter.MENDING, true);
				defaults.put(ArmorFilter.DEPTH_STRIDER, true);
			});
	private final EnumBooleanMapSetting<SwordFilter> swordFilters = EnumBooleanMapSetting.create(
			"sword_filters", "Sword Filter", "Required custom sword properties for auction filtering.", SwordFilter.class,
			defaults -> {
				defaults.put(SwordFilter.DETECTION, true);
				defaults.put(SwordFilter.VAMPIRISM, true);
				defaults.put(SwordFilter.OXIDATION, true);
				defaults.put(SwordFilter.POISON, true);
			});
	private final EnumBooleanMapSetting<PickaxeFilter> pickaxeFilters = EnumBooleanMapSetting.create(
			"pickaxe_filters", "Pickaxe Filter", "Required custom pickaxe properties for auction filtering.", PickaxeFilter.class,
			defaults -> {
				defaults.put(PickaxeFilter.FORTUNE, true);
				defaults.put(PickaxeFilter.MAGNET, true);
				defaults.put(PickaxeFilter.MENDING, true);
			});

	private final List<ItemBind> itemBinds = List.of(
			bind("trap", "Trap", Items.NETHERITE_SCRAP, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("freezing_snowball", "Freezing Snowball", Items.SNOWBALL, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("plast", "Plast", Items.DRIED_KELP, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("disorientation", "Disorientation", Items.ENDER_EYE, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("revealing_dust", "Revealing Dust", Items.SUGAR, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("wind_charge", "Wind Charge", Items.WIND_CHARGE, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("fire_charge", "Fire Charge", Items.FIRE_CHARGE, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("gods_aura", "God's Aura", Items.PHANTOM_MEMBRANE, Server.FUN_TIME, Server.SPOOKY_TIME),
			bind("explosive_thing", "Explosive Thing", Items.FIRE_CHARGE, Server.HOLY_WORLD),
			bind("explosive_stick", "Explosive Stick", Items.BLAZE_ROD, Server.HOLY_WORLD),
			bind("explosive_trap", "Explosive Trap", Items.PRISMARINE_SHARD, Server.HOLY_WORLD),
			bind("stun", "Stun", Items.NETHER_STAR, Server.HOLY_WORLD),
			bind("snowball", "Snowball", Items.SNOWBALL, Server.HOLY_WORLD)
	);
	private final BooleanSetting autoGodsAura = new BooleanSetting(
			"auto_gods_aura", "Auto God's Aura", "Uses God's Aura when Weakness II lasts at least 15 seconds.", false);
	private final BooleanSetting physicalItemSwap = new BooleanSetting(
			"physical_item_swap", "Physical Item Swap", "On FunTime, item binds physically move the item to your current slot and press again to swap it back.", false);
	private final HelperCustomItemsSetting customItems = new HelperCustomItemsSetting(
			"custom_items", "Custom Items", "Add your own Helper items and bind them."
	);

	private int[] sortedSlots;
	private long lastAuraUse;
	private String originalStorageTitle;
	private boolean pendingItemUse;
	private boolean pendingItemUsed;
	private boolean movementLocked;
	private boolean pendingReleasedAtPreTick;
	private int pendingItemSlot = -1;
	private int pendingSelectedSlot = -1;
	private int pendingStoppedEndTicks;
	private long pendingUseAtMs;
	private long pendingReleaseAtMs;
	private Item pendingItem;
	private String pendingItemName;
	private boolean pendingNotify;
	private final List<DeltaUseTask> deltaUseTasks = new ArrayList<>();

	private Helper() {
		super("Helper", ModuleCategory.OTHER, "Server helper with item hotkeys, auction tools and automatic utility actions.", true);
		features.visibleWhen(this::supportsAuction);
		armorFilters.visibleWhen(() -> supportsAuction() && features.enabled(Feature.AUCTION_ASSISTANT));
		swordFilters.visibleWhen(() -> supportsAuction() && features.enabled(Feature.AUCTION_ASSISTANT));
		pickaxeFilters.visibleWhen(() -> supportsAuction() && features.enabled(Feature.AUCTION_ASSISTANT));
		autoGodsAura.visibleWhen(this::supportsAuction);
		physicalItemSwap.visibleWhen(() -> server.is(Server.FUN_TIME));
		customItems.visibleWhen(() -> server.is(Server.CUSTOM));
		register(server, physicalItemSwap, features, armorFilters, swordFilters, pickaxeFilters, customItems);
		register(itemBinds.stream().map(ItemBind::setting).toArray(KeySetting[]::new));
		register(autoGodsAura);
	}

	public static Helper getInstance() {
		return INSTANCE;
	}

	public List<HudItem> hudItems() {
		List<HudItem> items = new ArrayList<>();
		if (server.is(Server.CUSTOM)) {
			for (HelperCustomItemsSetting.Entry entry : customItems.entries()) {
				if (!entry.stack().isEmpty()) {
					items.add(new HudItem(entry.name(), entry.item(), entry.key()));
				}
			}
			return items;
		}
		items.addAll(itemBinds.stream()
				.filter(bind -> bind.supports(server.value()))
				.map(bind -> new HudItem(bind.name(), bind.item(), bind.setting().key()))
				.toList());
		return items;
	}

	public boolean hasInventoryItem(Item item) {
		return findInventoryItem(item) >= 0;
	}

	@Override
	public void onKey(int key, int action, boolean mouse) {
		if (mouse || action != GLFW.GLFW_PRESS) {
			return;
		}
		for (ItemBind bind : itemBinds) {
			if (bind.setting().key() == key && bind.supports(server.value())) {
				if (usesDeltaItemUse()) {
					queueDeltaItemUse(bind.item(), bind.name(), true);
				} else if (usesPhysicalItemSwap()) {
					togglePhysicalItemSwap(bind, true);
				} else {
					useInventoryItem(bind.item(), bind.name(), true);
				}
				return;
			}
		}
		if (server.is(Server.CUSTOM)) {
			for (HelperCustomItemsSetting.Entry entry : customItems.entries()) {
				if (entry.key() == key && entry.key() >= 0) {
					useInventoryItem(entry.item(), entry.name(), true);
					return;
				}
			}
		}
	}

	@Override
	public void onPreTick() {
		tickDeltaItemUse(true);
		tickPendingItemUse(true);
	}

	@Override
	public void onTick() {
		Minecraft minecraft = Minecraft.getInstance();
		tickDeltaItemUse(false);
		tickPendingItemUse(false);
		if (!usesPhysicalItemSwap()) {
			restorePhysicalItemSwaps();
		}
		if (!autoGodsAura.enabled() || !supportsAuction() || minecraft.player == null) {
			return;
		}
		var weakness = minecraft.player.getEffect(MobEffects.WEAKNESS);
		if (weakness == null || weakness.getAmplifier() < 1 || weakness.getDuration() < 300
				|| minecraft.player.getAbsorptionAmount() > 3.0F || System.currentTimeMillis() - lastAuraUse < 5000L
				|| minecraft.player.getCooldowns().isOnCooldown(new ItemStack(Items.PHANTOM_MEMBRANE))) {
			return;
		}
		if (useInventoryItem(Items.PHANTOM_MEMBRANE, "God's Aura", false)) {
			lastAuraUse = System.currentTimeMillis();
		}
	}

	@Override
	protected void onDisable() {
		sortedSlots = null;
		originalStorageTitle = null;
		deltaUseTasks.clear();
		MoveUtil.updateMovementKeys();
		restorePhysicalItemSwaps();
		resetPendingItemUse();
	}

	public void handleContainerContent(ClientboundContainerSetContentPacket packet) {
		if (!enabled() || !supportsAuction() || !features.enabled(Feature.SORT_BY_PRICE)) {
			sortedSlots = null;
			return;
		}
		List<ItemStack> contents = packet.getItems();
		int containerSlots = contents.size() > 36 ? contents.size() - 36 : contents.size();
		sortedSlots = null;
		if (packet.getContainerId() == 0 || containerSlots < 44) {
			return;
		}
		boolean useFilter = contents.subList(0, containerSlots).stream()
				.anyMatch(stack -> price(stack) >= 0 && matchesAuctionFilter(stack));
		Integer[] order = new Integer[containerSlots];
		for (int index = 0; index < containerSlots; index++) {
			order[index] = index;
		}
		Arrays.sort(order, Comparator.comparingInt(index -> {
			ItemStack stack = contents.get(index);
			int value = stack.isEmpty() || useFilter && !matchesAuctionFilter(stack) ? -1 : price(stack);
			return value < 0 ? Integer.MAX_VALUE : value;
		}));
		List<ItemStack> original = new ArrayList<>(contents.subList(0, containerSlots));
		sortedSlots = new int[containerSlots];
		for (int display = 0; display < containerSlots; display++) {
			sortedSlots[display] = order[display];
			contents.set(display, original.get(order[display]));
		}
	}

	public void handleContainerSlot(ClientboundContainerSetSlotPacket packet) {
		if (!enabled() || sortedSlots == null || packet.getContainerId() == 0) {
			return;
		}
		int original = packet.getSlot();
		for (int display = 0; display < sortedSlots.length; display++) {
			if (sortedSlots[display] == original) {
				((MixinClientboundContainerSetSlotPacketAccessor) packet).hatred$setSlot(display);
				return;
			}
		}
	}

	public void handleContainerClick(ServerboundContainerClickPacket packet) {
		if (!enabled() || sortedSlots == null || packet.getContainerId() == 0) {
			return;
		}
		int display = packet.getSlotNum();
		if (display >= 0 && display < sortedSlots.length) {
			((MixinServerboundContainerClickPacketAccessor) packet).hatred$setSlotNum(sortedSlots[display]);
		}
	}

	public void handleSystemChat(ClientboundSystemChatPacket packet) {
		if (!enabled()) {
			return;
		}
		// Kept as a server-side recognition hook; the visible module UI remains English.
		if (packet.content().getString().equals("\u041d\u0430 \u044d\u0442\u043e\u0439 \u0430\u043d\u0430\u0440\u0445\u0438\u0438 \u044d\u0442\u043e\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043d\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442")) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(Component.literal("Helper: this item is disabled on the current realm.").withStyle(ChatFormatting.RED), false);
			}
		}
	}

	public List<Component> augmentTooltip(ItemStack stack, List<Component> original) {
		if (!enabled() || stack.getCount() <= 1 || price(stack) < 0
				|| original.stream().anyMatch(line -> line.getString().contains("Price per item"))) {
			return original;
		}
		List<Component> result = new ArrayList<>(original);
		for (int index = 0; index < result.size(); index++) {
			String line = result.get(index).getString();
			if (line.contains(PRICE_RU) || line.contains(PRICE_RU_ALT) || line.contains("$ Price: ")) {
				result.add(index + 1, Component.literal("$ ").withStyle(ChatFormatting.GREEN)
						.append(Component.literal("Price per item: ").withStyle(ChatFormatting.WHITE))
						.append(Component.literal(String.format(Locale.US, "%,d", price(stack))).withStyle(ChatFormatting.GREEN)));
				break;
			}
		}
		return result;
	}

	public void renderContainerOverlay(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
		if (!enabled() || !supportsAuction()) {
			return;
		}
		updateStorageTitle(screen);
		if (!features.enabled(Feature.AUCTION_ASSISTANT) || screen.getMenu().slots.size() < 90) {
			return;
		}
		int containerSlots = screen.getMenu().slots.size() - 36;
		List<Slot> slots = screen.getMenu().slots.subList(0, containerSlots);
		boolean useFilter = slots.stream().anyMatch(slot -> price(slot.getItem()) >= 0 && matchesAuctionFilter(slot.getItem()));
		Slot cheapest = slots.stream()
				.filter(slot -> !slot.getItem().isEmpty())
				.filter(slot -> !useFilter || matchesAuctionFilter(slot.getItem()))
				.filter(slot -> price(slot.getItem()) >= 0)
				.min(Comparator.comparingInt(slot -> price(slot.getItem())))
				.orElse(null);
		if (cheapest == null) {
			return;
		}
		long time = System.currentTimeMillis();
		int alpha = 40 + Math.round(120.0F * (0.5F + 0.5F * (float) Math.sin(time / 100.0D)));
		graphics.fill(cheapest.x, cheapest.y, cheapest.x + 16, cheapest.y + 16, alpha << 24 | 0x0000FF00);
	}

	private void updateStorageTitle(AbstractContainerScreen<?> screen) {
		String title = screen.getTitle().getString();
		if (originalStorageTitle != null && !title.contains(STORAGE_RU) && !title.startsWith(originalStorageTitle + " - ")) {
			originalStorageTitle = null;
		}
		if (originalStorageTitle == null && title.contains(STORAGE_RU)) {
			originalStorageTitle = title;
		}
		if (originalStorageTitle == null || !title.contains(STORAGE_RU) && !title.startsWith(originalStorageTitle + " - ")) {
			return;
		}
		long total = screen.getMenu().slots.stream()
				.mapToLong(slot -> (long) Math.max(price(slot.getItem()), 0) * Math.max(slot.getItem().getCount(), 1))
				.sum();
		String value = total >= 1_000_000_000L ? String.format(Locale.US, "%.0fB", total / 1_000_000_000.0D)
				: total >= 1_000_000L ? String.format(Locale.US, "%.0fM", total / 1_000_000.0D)
				: total >= 1_000L ? String.format(Locale.US, "%.0fK", total / 1_000.0D) : Long.toString(total);
		((MixinScreenTitleAccessor) screen).hatred$setTitle(Component.literal(originalStorageTitle + " - " + value));
	}

	private boolean useInventoryItem(Item item, String name, boolean notify) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			return false;
		}
		restorePhysicalItemSwaps();
		if (minecraft.player.getCooldowns().isOnCooldown(new ItemStack(item))) {
			if (notify) {
				message(name + " is on cooldown.", ChatFormatting.RED);
			}
			return false;
		}
		int slot = findInventoryItem(item);
		if (slot < 0) {
			if (notify) {
				message(name + " is not in your inventory.", ChatFormatting.RED);
			}
			return false;
		}
		startPendingItemUse(item, name, slot, minecraft.player.getInventory().selected, notify);
		return true;
	}

	private boolean queueDeltaItemUse(Item item, String name, boolean notify) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			return false;
		}
		restorePhysicalItemSwaps();
		resetPendingItemUse();
		if (minecraft.player.getCooldowns().isOnCooldown(new ItemStack(item))) {
			if (notify) {
				message(name + " is on cooldown.", ChatFormatting.RED);
			}
			return false;
		}
		if (findHotbarItem(item) < 0 && findInventoryItem(item) < 0) {
			if (notify) {
				message(name + " is not in your inventory.", ChatFormatting.RED);
			}
			return false;
		}
		deltaUseTasks.add(new DeltaUseTask(item));
		MoveUtil.releaseMovementKeys();
		return true;
	}

	private void tickDeltaItemUse(boolean allowUse) {
		if (!usesDeltaItemUse()) {
			if (!deltaUseTasks.isEmpty()) {
				deltaUseTasks.clear();
				MoveUtil.updateMovementKeys();
			}
			return;
		}
		if (deltaUseTasks.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			deltaUseTasks.clear();
			MoveUtil.updateMovementKeys();
			return;
		}

		DeltaUseTask task = deltaUseTasks.getFirst();
		MoveUtil.releaseMovementKeys();
		if (!allowUse && task.releasedAtPreTick()) {
			task.stoppedEndTicks(task.stoppedEndTicks() + 1);
			task.releasedAtPreTick(false);
			return;
		}
		if (!allowUse) {
			return;
		}

		task.releasedAtPreTick(true);
		if (task.tick() < 0 && (task.stoppedEndTicks() < REQUIRED_STOPPED_END_TICKS || Util.getMillis() < task.useAtMs())) {
			return;
		}

		int hotbarSlot = findHotbarItem(task.item());
		int inventorySlot = findInventoryItem(task.item());
		if (task.tick() == -1 && hotbarSlot == -1 && inventorySlot == -1) {
			deltaUseTasks.removeFirst();
			MoveUtil.updateMovementKeys();
			return;
		}

		task.tick(task.tick() + 1);
		if (task.tick() == 0) {
			task.originalSelectedSlot(minecraft.player.getInventory().selected);
			if (hotbarSlot != -1) {
				task.itemSlot(hotbarSlot);
				if (hotbarSlot != minecraft.player.getInventory().selected) {
					minecraft.player.getInventory().setSelectedHotbarSlot(hotbarSlot);
				}
				return;
			}
			if (inventorySlot != -1) {
				task.itemSlot(inventorySlot);
				minecraft.gameMode.handleInventoryMouseClick(
						minecraft.player.inventoryMenu.containerId,
						toMenuSlot(inventorySlot),
						minecraft.player.getInventory().selected,
						ClickType.SWAP,
						minecraft.player
				);
			}
			return;
		}

		if (task.tick() == 1) {
			minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
			return;
		}

		if (task.tick() == 2) {
			if (task.itemSlot() > 8) {
				minecraft.gameMode.handleInventoryMouseClick(
						minecraft.player.inventoryMenu.containerId,
						toMenuSlot(task.itemSlot()),
						task.originalSelectedSlot(),
						ClickType.SWAP,
						minecraft.player
				);
			} else if (task.originalSelectedSlot() != minecraft.player.getInventory().selected) {
				minecraft.player.getInventory().setSelectedHotbarSlot(task.originalSelectedSlot());
			}
			deltaUseTasks.removeFirst();
			if (deltaUseTasks.isEmpty()) {
				MoveUtil.updateMovementKeys();
			}
		}
	}

	private boolean togglePhysicalItemSwap(ItemBind bind, boolean notify) {
		if (bind.swapped()) {
			restorePhysicalItemSwap(bind);
			return true;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			return false;
		}
		int sourceSlot = findInventoryItem(bind.item());
		int hotbarSlot = minecraft.player.getInventory().selected;
		if (sourceSlot < 0) {
			if (notify) {
				message(bind.name() + " is not in your inventory.", ChatFormatting.RED);
			}
			return false;
		}
		if (sourceSlot == hotbarSlot) {
			return true;
		}

		bind.sourceSlot(sourceSlot);
		bind.hotbarSlot(hotbarSlot);
		minecraft.gameMode.handleInventoryMouseClick(
				minecraft.player.inventoryMenu.containerId,
				toMenuSlot(sourceSlot),
				hotbarSlot,
				ClickType.SWAP,
				minecraft.player
		);
		return true;
	}

	private void restorePhysicalItemSwaps() {
		for (ItemBind bind : itemBinds) {
			restorePhysicalItemSwap(bind);
		}
	}

	private void restorePhysicalItemSwap(ItemBind bind) {
		if (!bind.swapped()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		int sourceSlot = bind.sourceSlot();
		int hotbarSlot = bind.hotbarSlot();
		bind.clearSwap();
		if (minecraft.player == null || minecraft.gameMode == null || sourceSlot < 0 || hotbarSlot < 0) {
			return;
		}
		minecraft.gameMode.handleInventoryMouseClick(
				minecraft.player.inventoryMenu.containerId,
				toMenuSlot(sourceSlot),
				hotbarSlot,
				ClickType.SWAP,
				minecraft.player
		);
	}

	private boolean usesPhysicalItemSwap() {
		return server.is(Server.FUN_TIME) && physicalItemSwap.enabled();
	}

	private boolean usesDeltaItemUse() {
		return server.is(Server.FUN_TIME) && !physicalItemSwap.enabled();
	}

	private void startPendingItemUse(Item item, String name, int slot, int selected, boolean notify) {
		resetPendingItemUse();
		pendingItemUse = true;
		pendingItemUsed = false;
		movementLocked = true;
		pendingReleasedAtPreTick = false;
		pendingItemSlot = slot;
		pendingSelectedSlot = selected;
		pendingStoppedEndTicks = 0;
		pendingItem = item;
		pendingItemName = name;
		pendingNotify = notify;
		long now = Util.getMillis();
		pendingUseAtMs = now + ITEM_USE_DELAY_MS;
		pendingReleaseAtMs = now + ITEM_USE_SLOWNESS_TOTAL_MS;
		MoveUtil.releaseMovementKeys();
	}

	private void tickPendingItemUse(boolean allowUse) {
		if (!pendingItemUse) {
			return;
		}

		stopMovementForPendingUse(allowUse);
		long now = Util.getMillis();
		if (!allowUse && pendingReleasedAtPreTick && !pendingItemUsed) {
			pendingStoppedEndTicks++;
			pendingReleasedAtPreTick = false;
		}
		if (allowUse && !pendingItemUsed && pendingStoppedEndTicks >= REQUIRED_STOPPED_END_TICKS && now >= pendingUseAtMs) {
			performPendingItemUse();
		}
		if (pendingItemUsed && now >= pendingReleaseAtMs) {
			finishPendingItemUse();
		}
	}

	private void stopMovementForPendingUse(boolean preTick) {
		Minecraft minecraft = Minecraft.getInstance();
		MoveUtil.releaseMovementKeys();
		if (preTick) {
			pendingReleasedAtPreTick = true;
		}
	}

	private void performPendingItemUse() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!pendingItemUse || pendingItemUsed || pendingItemSlot < 0 || pendingSelectedSlot < 0 || minecraft.player == null || minecraft.gameMode == null) {
			return;
		}
		if (pendingItem != null && !minecraft.player.getInventory().getItem(pendingItemSlot).is(pendingItem)) {
			int updatedSlot = findInventoryItem(pendingItem);
			if (updatedSlot < 0) {
				if (pendingNotify && pendingItemName != null) {
					message(pendingItemName + " is not in your inventory.", ChatFormatting.RED);
				}
				pendingItemUsed = true;
				return;
			}
			pendingItemSlot = updatedSlot;
		}

		int selected = Math.max(0, Math.min(8, pendingSelectedSlot));
		minecraft.player.getInventory().setSelectedHotbarSlot(selected);
		if (pendingItemSlot == selected) {
			minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
			pendingItemUsed = true;
			return;
		}

		int menuSlot = toMenuSlot(pendingItemSlot);
		minecraft.gameMode.handleInventoryMouseClick(minecraft.player.inventoryMenu.containerId, menuSlot, selected, ClickType.SWAP, minecraft.player);
		minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
		minecraft.gameMode.handleInventoryMouseClick(minecraft.player.inventoryMenu.containerId, menuSlot, selected, ClickType.SWAP, minecraft.player);
		pendingItemUsed = true;
	}

	private void finishPendingItemUse() {
		pendingItemUse = false;
		pendingItemUsed = false;
		pendingReleasedAtPreTick = false;
		pendingItemSlot = -1;
		pendingSelectedSlot = -1;
		pendingStoppedEndTicks = 0;
		pendingUseAtMs = 0L;
		pendingReleaseAtMs = 0L;
		pendingItem = null;
		pendingItemName = null;
		pendingNotify = false;
		if (movementLocked) {
			MoveUtil.updateMovementKeys();
			movementLocked = false;
		}
	}

	private void resetPendingItemUse() {
		pendingItemUse = false;
		pendingItemUsed = false;
		pendingReleasedAtPreTick = false;
		pendingItemSlot = -1;
		pendingSelectedSlot = -1;
		pendingStoppedEndTicks = 0;
		pendingUseAtMs = 0L;
		pendingReleaseAtMs = 0L;
		pendingItem = null;
		pendingItemName = null;
		pendingNotify = false;
		if (movementLocked) {
			MoveUtil.updateMovementKeys();
			movementLocked = false;
		}
	}

	private static int toMenuSlot(int inventorySlot) {
		return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
	}

	private int findInventoryItem(Item item) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return -1;
		}
		for (int slot = 0; slot < 36; slot++) {
			if (minecraft.player.getInventory().getItem(slot).is(item)) {
				return slot;
			}
		}
		return -1;
	}

	private int findHotbarItem(Item item) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return -1;
		}
		for (int slot = 0; slot < 9; slot++) {
			if (minecraft.player.getInventory().getItem(slot).is(item)) {
				return slot;
			}
		}
		return -1;
	}

	private void message(String text, ChatFormatting color) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(Component.literal("Helper: " + text).withStyle(color), false);
		}
	}

	private int price(ItemStack stack) {
		if (stack.isEmpty() || stack.is(Items.GRAY_DYE)) {
			return -1;
		}
		for (Component component : Screen.getTooltipFromItem(Minecraft.getInstance(), stack)) {
			String line = component.getString();
			String marker = line.contains(PRICE_RU) ? PRICE_RU : line.contains(PRICE_RU_ALT) ? PRICE_RU_ALT
					: line.contains("$ Price: ") ? "$ Price: " : null;
			if (marker == null) {
				continue;
			}
			String digits = line.substring(line.indexOf(marker) + marker.length()).replaceAll("[^0-9]", "");
			try {
				return digits.isEmpty() ? -1 : Integer.parseInt(digits) / Math.max(stack.getCount(), 1);
			} catch (NumberFormatException ignored) {
				return -1;
			}
		}
		return -1;
	}

	private boolean matchesAuctionFilter(ItemStack stack) {
		if (stack.getItem() instanceof ArmorItem) {
			if (armorFilters.enabled(ArmorFilter.PROTECTION)
					&& (enchantmentLevel(stack, Enchantments.UNBREAKING) < 4 || enchantmentLevel(stack, Enchantments.PROTECTION) < 5)) {
				return false;
			}
			if (armorFilters.enabled(ArmorFilter.NO_THORNS) && enchantmentLevel(stack, Enchantments.THORNS) > 0) {
				return false;
			}
			if (armorFilters.enabled(ArmorFilter.MENDING) && enchantmentLevel(stack, Enchantments.MENDING) < 1) {
				return false;
			}
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			return !armorFilters.enabled(ArmorFilter.DEPTH_STRIDER) || equippable == null || equippable.slot() != EquipmentSlot.FEET
					|| enchantmentLevel(stack, Enchantments.DEPTH_STRIDER) >= 1;
		}
		if (stack.getItem() instanceof SwordItem) {
			int knockback = enchantmentLevel(stack, Enchantments.KNOCKBACK);
			if (knockback > 1 || swordFilters.enabled(SwordFilter.SHARPNESS) && enchantmentLevel(stack, Enchantments.SHARPNESS) < 6
					|| tooltipContains(stack, "\u041d\u0435\u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u043e\u0441\u0442\u044c ") || tooltipContains(stack, "\u041d\u0435\u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u044b\u0439 ")) {
				return false;
			}
			return (!swordFilters.enabled(SwordFilter.DETECTION) || tooltipLevel(stack, "\u0414\u0435\u0442\u0435\u043a\u0446\u0438\u044f") >= 2)
					&& (!swordFilters.enabled(SwordFilter.VAMPIRISM) || tooltipLevel(stack, "\u0412\u0430\u043c\u043f\u0438\u0440\u0438\u0437\u043c") >= 2)
					&& (!swordFilters.enabled(SwordFilter.OXIDATION) || tooltipLevel(stack, "\u041e\u043a\u0438\u0441\u043b\u0435\u043d\u0438\u0435") >= 2)
					&& (!swordFilters.enabled(SwordFilter.POISON) || tooltipLevel(stack, "\u042f\u0434") >= 3);
		}
		if (stack.getItem() instanceof PickaxeItem) {
			return (!pickaxeFilters.enabled(PickaxeFilter.MENDING) || enchantmentLevel(stack, Enchantments.MENDING) >= 1)
					&& (!pickaxeFilters.enabled(PickaxeFilter.FORTUNE) || tooltipLevel(stack, "\u0423\u0434\u0430\u0447\u0430") >= 5)
					&& (!pickaxeFilters.enabled(PickaxeFilter.EFFICIENCY) || tooltipLevel(stack, "\u042d\u0444\u0444\u0435\u043a\u0442\u0438\u0432\u043d\u043e\u0441\u0442\u044c") >= 4)
					&& (!pickaxeFilters.enabled(PickaxeFilter.MAGNET) || tooltipContains(stack, "\u041c\u0430\u0433\u043d\u0438\u0442"));
		}
		return true;
	}

	private int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return 0;
		}
		Holder<Enchantment> holder = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
		return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}

	private boolean tooltipContains(ItemStack stack, String text) {
		return Screen.getTooltipFromItem(Minecraft.getInstance(), stack).stream().anyMatch(line -> line.getString().contains(text));
	}

	private int tooltipLevel(ItemStack stack, String name) {
		for (Component line : Screen.getTooltipFromItem(Minecraft.getInstance(), stack)) {
			String text = line.getString();
			int index = text.indexOf(name);
			if (index < 0) {
				continue;
			}
			String tail = text.substring(index + name.length()).trim();
			if (tail.isEmpty()) {
				return 1;
			}
			String token = tail.split("\\s+")[0];
			int roman = romanValue(token);
			if (roman > 0) {
				return roman;
			}
			try {
				return Integer.parseInt(token.replaceAll("[^0-9]", ""));
			} catch (NumberFormatException ignored) {
				return 1;
			}
		}
		return 0;
	}

	private static int romanValue(String value) {
		return switch (value.toUpperCase(Locale.ROOT)) {
			case "I" -> 1;
			case "II" -> 2;
			case "III" -> 3;
			case "IV" -> 4;
			case "V" -> 5;
			case "VI" -> 6;
			case "VII" -> 7;
			case "VIII" -> 8;
			case "IX" -> 9;
			case "X" -> 10;
			default -> 0;
		};
	}

	private boolean supportsAuction() {
		return server.is(Server.FUN_TIME) || server.is(Server.SPOOKY_TIME);
	}

	private ItemBind bind(String id, String name, Item item, Server... servers) {
		KeySetting setting = new KeySetting(id + "_key", name, "Uses " + name + " from anywhere in your inventory.");
		setting.visibleWhen(() -> Arrays.asList(servers).contains(server.value()));
		return new ItemBind(name, item, setting, List.of(servers));
	}

	public enum Server {
		FUN_TIME("FunTime"), SPOOKY_TIME("SpookyTime"), HOLY_WORLD("HolyWorld"), CUSTOM("Custom");
		private final String display;
		Server(String display) { this.display = display; }
		public String display() { return display; }
	}

	public enum Feature {
		AUCTION_ASSISTANT("Auction Assistant"), SORT_BY_PRICE("Sort by Price");
		private final String display;
		Feature(String display) { this.display = display; }
		public String display() { return display; }
	}

	public enum ArmorFilter {
		PROTECTION("Protection"), NO_THORNS("No Thorns"), MENDING("Mending"), DEPTH_STRIDER("Depth Strider");
		private final String display;
		ArmorFilter(String display) { this.display = display; }
		public String display() { return display; }
	}

	public enum SwordFilter {
		SHARPNESS("Sharpness"), DETECTION("Detection"), VAMPIRISM("Vampirism"), OXIDATION("Oxidation"), POISON("Poison");
		private final String display;
		SwordFilter(String display) { this.display = display; }
		public String display() { return display; }
	}

	public enum PickaxeFilter {
		EFFICIENCY("Efficiency"), FORTUNE("Fortune"), MAGNET("Magnet"), MENDING("Mending");
		private final String display;
		PickaxeFilter(String display) { this.display = display; }
		public String display() { return display; }
	}

	public record HudItem(String name, Item item, int key) {
	}

	private static final class DeltaUseTask {
		private final Item item;
		private int originalSelectedSlot;
		private int itemSlot;
		private int tick = -1;
		private int stoppedEndTicks;
		private long useAtMs = Util.getMillis() + ITEM_USE_DELAY_MS;
		private boolean releasedAtPreTick;

		private DeltaUseTask(Item item) {
			this.item = item;
		}

		private Item item() {
			return item;
		}

		private int originalSelectedSlot() {
			return originalSelectedSlot;
		}

		private void originalSelectedSlot(int originalSelectedSlot) {
			this.originalSelectedSlot = originalSelectedSlot;
		}

		private int itemSlot() {
			return itemSlot;
		}

		private void itemSlot(int itemSlot) {
			this.itemSlot = itemSlot;
		}

		private int tick() {
			return tick;
		}

		private void tick(int tick) {
			this.tick = tick;
		}

		private int stoppedEndTicks() {
			return stoppedEndTicks;
		}

		private void stoppedEndTicks(int stoppedEndTicks) {
			this.stoppedEndTicks = stoppedEndTicks;
		}

		private long useAtMs() {
			return useAtMs;
		}

		private boolean releasedAtPreTick() {
			return releasedAtPreTick;
		}

		private void releasedAtPreTick(boolean releasedAtPreTick) {
			this.releasedAtPreTick = releasedAtPreTick;
		}
	}

	private static final class ItemBind {
		private final String name;
		private final Item item;
		private final KeySetting setting;
		private final List<Server> servers;
		private int sourceSlot = -1;
		private int hotbarSlot = -1;

		private ItemBind(String name, Item item, KeySetting setting, List<Server> servers) {
			this.name = name;
			this.item = item;
			this.setting = setting;
			this.servers = servers;
		}

		private String name() {
			return name;
		}

		private Item item() {
			return item;
		}

		private KeySetting setting() {
			return setting;
		}

		private boolean supports(Server server) {
			return servers.contains(server);
		}

		private int sourceSlot() {
			return sourceSlot;
		}

		private void sourceSlot(int sourceSlot) {
			this.sourceSlot = sourceSlot;
		}

		private int hotbarSlot() {
			return hotbarSlot;
		}

		private void hotbarSlot(int hotbarSlot) {
			this.hotbarSlot = hotbarSlot;
		}

		private boolean swapped() {
			return sourceSlot != -1;
		}

		private void clearSwap() {
			sourceSlot = -1;
			hotbarSlot = -1;
		}
	}
}
