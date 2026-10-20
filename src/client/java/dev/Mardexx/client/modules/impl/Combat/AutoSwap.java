package dev.Mardexx.client.modules.impl.Combat;

import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.utils.other.SlownessManager;
import dev.Mardexx.api.utils.player.MoveUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.modules.impl.Render.Notifications;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.ItemStackSetting;
import dev.Mardexx.client.settings.KeySetting;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class AutoSwap extends Module implements QuickImports {
	private static final AutoSwap INSTANCE = new AutoSwap();
	private static final long INPUT_DELAY_MS = 200L;
	private static final long SWAP_RELEASE_DELAY_MS = 1L;
	private static final long DOUBLE_SWAP_SLOWNESS_TOTAL_MS = 95L;
	private static final long DOUBLE_SWAP_DELAY_MS = 35L;

	private final EnumSetting<SwapMode> mode = new EnumSetting<>(
			"mode",
			"Mode",
			"Double alternates two item types. Triple opens a three-item selection wheel.",
			SwapMode.DOUBLE
	);
	private final ItemStackSetting firstItem = new ItemStackSetting(
			"first_item",
			"First Item",
			"Click the gear and choose the first stack from your current inventory."
	);
	private final ItemStackSetting secondItem = new ItemStackSetting(
			"second_item",
			"Second Item",
			"Click the gear and choose the second stack from your current inventory."
	);
	private final KeySetting swapKey = new KeySetting(
			"swap_key",
			"Swap Key",
			"Swaps between the selected items in your offhand."
	);
	private final BooleanSetting notifications = new BooleanSetting(
			"swap_notifications",
			"Swap Notifications",
			"Shows the selected item in chat after a successful swap.",
			true
	);
	private final ItemStackSetting tripleSlotOne = new ItemStackSetting(
			"triple_slot_1",
			"Triple Slot 1",
			"Click the gear and choose an item currently in your inventory."
	);
	private final ItemStackSetting tripleSlotTwo = new ItemStackSetting(
			"triple_slot_2",
			"Triple Slot 2",
			"Click the gear and choose an item currently in your inventory."
	);
	private final ItemStackSetting tripleSlotThree = new ItemStackSetting(
			"triple_slot_3",
			"Triple Slot 3",
			"Click the gear and choose an item currently in your inventory."
	);

	private boolean swap;
	private boolean hand;
	private boolean bypassActive;
	private boolean bypassSwapped;
	private boolean bypassSlowed;
	private boolean movementLocked;
	private int bypassSlot = -1;
	private long swapStartedAtMs;
	private long lastInputAtMs;

	private AutoSwap() {
		super("Auto Swap", ModuleCategory.COMBAT, "Swaps selected inventory items into your offhand.");
		firstItem.visibleWhen(() -> mode.is(SwapMode.DOUBLE));
		secondItem.visibleWhen(() -> mode.is(SwapMode.DOUBLE));
		tripleSlotOne.visibleWhen(() -> mode.is(SwapMode.TRIPLE));
		tripleSlotTwo.visibleWhen(() -> mode.is(SwapMode.TRIPLE));
		tripleSlotThree.visibleWhen(() -> mode.is(SwapMode.TRIPLE));
		register(
				mode,
				firstItem,
				secondItem,
				tripleSlotOne,
				tripleSlotTwo,
				tripleSlotThree,
				swapKey,
				notifications
		);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null) {
			resetSwapState();
			return;
		}

		SlownessManager.tick();

		if (bypassActive) {
			MoveUtil.releaseMovementKeys();
			if (!bypassSlowed && !bypassSwapped && bypassSlot != -1) {
				performBypassSwap();
			}

			if (bypassSlowed) {
				if (bypassSwapped && !SlownessManager.slowed()) {
					finishBypass();
				}
				return;
			}

			if (bypassSwapped && System.currentTimeMillis() - swapStartedAtMs >= SWAP_RELEASE_DELAY_MS) {
				finishBypass();
			}
			return;
		}

		if (mode.is(SwapMode.DOUBLE) && swap) {
			ItemStackSetting target = nextDoubleItem();
			startSwap(target);
			hand = target == secondItem;
		}
	}

	@Override
	public void onKey(int key, int action, boolean mouse) {
		if (mouse || action != GLFW.GLFW_PRESS || mc.screen != null || swapKey.key() < 0) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastInputAtMs < INPUT_DELAY_MS) {
			return;
		}

		if (key == swapKey.key()) {
			if (mode.is(SwapMode.TRIPLE)) {
				mc.setScreen(new TripleWheelScreen(this));
			} else {
				swap = true;
			}
		} else {
			swap = false;
		}
		lastInputAtMs = now;
	}

	@Override
	protected void onDisable() {
		if (mc.screen instanceof TripleWheelScreen || mc.screen instanceof TripleItemPickerScreen) {
			mc.setScreen(null);
		}
		resetSwapState();
	}

	private void startSwap(ItemStackSetting setting) {
		int slot = findSelectedSlot(setting);
		beginSwap(slot, true);
		swap = false;
	}

	private ItemStackSetting nextDoubleItem() {
		if (matchesOffhand(firstItem) && secondItem.hasValue()) {
			return secondItem;
		}
		if (matchesOffhand(secondItem) && firstItem.hasValue()) {
			return firstItem;
		}
		if (hand && firstItem.hasValue()) {
			return firstItem;
		}
		if (!hand && secondItem.hasValue()) {
			return secondItem;
		}
		return firstItem.hasValue() ? firstItem : secondItem;
	}

	private boolean matchesOffhand(ItemStackSetting setting) {
		if (setting == null || !setting.hasValue() || mc.player == null) {
			return false;
		}
		ItemStack offhand = mc.player.getOffhandItem();
		return !offhand.isEmpty() && (setting.matches(offhand) || offhand.is(setting.item()));
	}

	void executeTripleSwap(int slotIndex) {
		ItemStackSetting setting = tripleSlot(slotIndex);
		if (setting == null || !setting.hasValue() || mc.player == null) {
			return;
		}

		int slot = findSelectedSlot(setting);
		beginSwap(slot, false);
	}

	private int findSelectedSlot(ItemStackSetting setting) {
		if (setting == null || !setting.hasValue() || mc.player == null) {
			return -1;
		}

		for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
			if (setting.matches(mc.player.getInventory().getItem(inventorySlot))) {
				return inventorySlot;
			}
		}
		for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
			if (mc.player.getInventory().getItem(inventorySlot).is(setting.item())) {
				return inventorySlot;
			}
		}
		return -1;
	}

	private void beginSwap(int slot, boolean slowed) {
		if (slot == -1) {
			return;
		}
		MoveUtil.releaseMovementKeys();
		movementLocked = true;
		bypassActive = true;
		bypassSwapped = false;
		bypassSlowed = slowed;
		bypassSlot = slot;
		swapStartedAtMs = System.currentTimeMillis();
		if (slowed) {
			SlownessManager.applySlowness(DOUBLE_SWAP_SLOWNESS_TOTAL_MS, DOUBLE_SWAP_DELAY_MS, this::performBypassSwap);
		}
	}

	private void performBypassSwap() {
		if (!bypassActive || bypassSwapped || bypassSlot == -1 || mc.player == null || mc.gameMode == null) {
			return;
		}

		AbstractContainerMenu menu = mc.player.containerMenu;
		ItemStack swappedItem = mc.player.getInventory().getItem(bypassSlot).copyWithCount(1);
		mc.gameMode.handleInventoryMouseClick(
				menu.containerId,
				toMenuSlot(bypassSlot),
				Inventory.SLOT_OFFHAND,
				ClickType.SWAP,
				mc.player
		);
		mc.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
		if (notifications.enabled() && !swappedItem.isEmpty()) {
			Notifications.getInstance().pushSwap(swappedItem);
		}
		bypassSwapped = true;
	}

	private static int toMenuSlot(int inventorySlot) {
		return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
	}

	private void finishBypass() {
		bypassActive = false;
		bypassSwapped = false;
		bypassSlowed = false;
		bypassSlot = -1;
		if (movementLocked) {
			if (SlownessManager.slowed()) {
				MoveUtil.releaseMovementKeys();
			} else {
				MoveUtil.updateMovementKeys();
			}
			movementLocked = false;
		}
	}

	private void resetSwapState() {
		swap = false;
		finishBypass();
	}

	public static AutoSwap getInstance() {
		return INSTANCE;
	}

	int swapKey() {
		return swapKey.key();
	}

	ItemStackSetting tripleSlot(int index) {
		return switch (index) {
			case 0 -> tripleSlotOne;
			case 1 -> tripleSlotTwo;
			case 2 -> tripleSlotThree;
			default -> null;
		};
	}

	public enum SwapMode {
		DOUBLE("Double"),
		TRIPLE("Triple");

		private final String display;

		SwapMode(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

}
