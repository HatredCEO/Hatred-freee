package dev.Mardexx.client.modules.impl.Combat;

import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.settings.ItemStackSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class TripleItemPickerScreen extends Screen {
	private static final int COLUMNS = 9;
	private static final int CELL_SIZE = 24;
	private static final int PANEL_PADDING = 12;
	private static final int TITLE_HEIGHT = 24;
	private static final int FOOTER_HEIGHT = 28;

	private final Screen parent;
	private final ItemStackSetting setting;
	private final List<ItemStack> items = new ArrayList<>();
	private int hoveredItem = -1;

	public TripleItemPickerScreen(Screen parent, ItemStackSetting setting) {
		super(Component.literal("Select AutoSwap Item"));
		this.parent = parent;
		this.setting = setting;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		refreshItems();
		graphics.fill(0, 0, width, height, 0xA6000000);

		int rows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
		int panelWidth = PANEL_PADDING * 2 + COLUMNS * CELL_SIZE;
		int panelHeight = PANEL_PADDING * 2 + TITLE_HEIGHT + rows * CELL_SIZE + FOOTER_HEIGHT;
		int panelX = (width - panelWidth) / 2;
		int panelY = (height - panelHeight) / 2;
		int gridX = panelX + PANEL_PADDING;
		int gridY = panelY + PANEL_PADDING + TITLE_HEIGHT;

		Render2D.softShadow(graphics, panelX, panelY, panelWidth, panelHeight, 9.0F, 16.0F, 0x99000000);
		Render2D.roundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 9.0F, 0xFF101010);
		Render2D.roundedRect(graphics, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 8.0F, 0xFF151515);
		graphics.drawCenteredString(font, "Choose item for " + setting.displayName(), width / 2, panelY + 10, 0xFFF0F0F0);

		hoveredItem = -1;
		for (int index = 0; index < items.size(); index++) {
			int cellX = gridX + index % COLUMNS * CELL_SIZE;
			int cellY = gridY + index / COLUMNS * CELL_SIZE;
			ItemStack stack = items.get(index);
			boolean hovered = inside(mouseX, mouseY, cellX, cellY, CELL_SIZE - 2, CELL_SIZE - 2);
			boolean selected = setting.matches(stack);
			if (hovered) {
				hoveredItem = index;
			}

			int background = selected ? 0xFF4D4330 : hovered ? 0xFF303030 : 0xFF202020;
			int outline = selected ? 0xFFE2BD68 : hovered ? 0xFF686868 : 0xFF333333;
			Render2D.roundedRect(graphics, cellX, cellY, CELL_SIZE - 2, CELL_SIZE - 2, 4.0F, outline);
			Render2D.roundedRect(graphics, cellX + 1, cellY + 1, CELL_SIZE - 4, CELL_SIZE - 4, 3.0F, background);
			graphics.renderItem(stack, cellX + 3, cellY + 3);
		}

		if (items.isEmpty()) {
			graphics.drawCenteredString(font, "Your inventory is empty", width / 2, gridY + 7, 0xFF888888);
		}

		int buttonY = panelY + panelHeight - PANEL_PADDING - 18;
		int clearX = width / 2 - 72;
		int backX = width / 2 + 4;
		renderButton(graphics, clearX, buttonY, 68, 18, "Clear", inside(mouseX, mouseY, clearX, buttonY, 68, 18));
		renderButton(graphics, backX, buttonY, 68, 18, "Back", inside(mouseX, mouseY, backX, buttonY, 68, 18));

		if (hoveredItem >= 0) {
			graphics.renderTooltip(font, items.get(hoveredItem), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		if (hoveredItem >= 0 && hoveredItem < items.size()) {
			setting.setStack(items.get(hoveredItem));
			onClose();
			return true;
		}

		int rows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
		int panelHeight = PANEL_PADDING * 2 + TITLE_HEIGHT + rows * CELL_SIZE + FOOTER_HEIGHT;
		int panelY = (height - panelHeight) / 2;
		int buttonY = panelY + panelHeight - PANEL_PADDING - 18;
		int clearX = width / 2 - 72;
		int backX = width / 2 + 4;
		if (inside(mouseX, mouseY, clearX, buttonY, 68, 18)) {
			setting.clear();
			onClose();
			return true;
		}
		if (inside(mouseX, mouseY, backX, buttonY, 68, 18)) {
			onClose();
			return true;
		}
		return true;
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void refreshItems() {
		items.clear();
		if (minecraft == null || minecraft.player == null) {
			return;
		}
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = minecraft.player.getInventory().getItem(slot);
			if (!stack.isEmpty()) {
				items.add(stack);
			}
		}
	}

	private void renderButton(GuiGraphics graphics, int x, int y, int width, int height, String text, boolean hovered) {
		Render2D.roundedRect(graphics, x, y, width, height, 4.0F, hovered ? 0xFF444444 : 0xFF292929);
		graphics.drawCenteredString(font, text, x + width / 2, y + 5, hovered ? 0xFFFFFFFF : 0xFFB0B0B0);
	}

	private static boolean inside(double mouseX, double mouseY, double x, double y, double width, double height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
}
