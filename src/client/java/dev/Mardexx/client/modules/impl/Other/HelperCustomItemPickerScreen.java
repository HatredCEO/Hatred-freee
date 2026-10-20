package dev.Mardexx.client.modules.impl.Other;

import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.settings.HelperCustomItemsSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class HelperCustomItemPickerScreen extends Screen {
	private static final int COLUMNS = 8;
	private static final int CELL_SIZE = 23;
	private static final int PANEL_PADDING = 10;
	private static final int TITLE_HEIGHT = 22;
	private static final int FOOTER_HEIGHT = 26;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private final Screen parent;
	private final HelperCustomItemsSetting setting;
	private final List<ItemStack> items = new ArrayList<>();
	private MsdfFontRenderer uiFont;
	private int hoveredItem = -1;

	public HelperCustomItemPickerScreen(Screen parent, HelperCustomItemsSetting setting) {
		super(Component.literal("Add Helper Custom Item"));
		this.parent = parent;
		this.setting = setting;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		refreshItems();

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
		drawCenteredText(graphics, "Add custom Helper item", width / 2.0F, panelY + 12.0F, 7.5F, 0xFFF0F0F0);

		hoveredItem = -1;
		for (int index = 0; index < items.size(); index++) {
			int cellX = gridX + index % COLUMNS * CELL_SIZE;
			int cellY = gridY + index / COLUMNS * CELL_SIZE;
			ItemStack stack = items.get(index);
			boolean hovered = inside(mouseX, mouseY, cellX, cellY, CELL_SIZE - 2, CELL_SIZE - 2);
			if (hovered) {
				hoveredItem = index;
			}

			Render2D.roundedRect(graphics, cellX, cellY, CELL_SIZE - 2, CELL_SIZE - 2, 4.0F, hovered ? 0xFF686868 : 0xFF333333);
			Render2D.roundedRect(graphics, cellX + 1, cellY + 1, CELL_SIZE - 4, CELL_SIZE - 4, 3.0F, hovered ? 0xFF303030 : 0xFF202020);
			graphics.renderItem(stack, cellX + 3, cellY + 3);
		}

		if (items.isEmpty()) {
			drawCenteredText(graphics, "Your inventory is empty", width / 2.0F, gridY + 8.0F, 6.5F, 0xFF888888);
		}

		int buttonY = panelY + panelHeight - PANEL_PADDING - 18;
		int backX = width / 2 - 29;
		renderButton(graphics, backX, buttonY, 58, 18, "Back", inside(mouseX, mouseY, backX, buttonY, 58, 18));

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
			setting.addStack(items.get(hoveredItem));
			onClose();
			return true;
		}

		int rows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
		int panelHeight = PANEL_PADDING * 2 + TITLE_HEIGHT + rows * CELL_SIZE + FOOTER_HEIGHT;
		int panelY = (height - panelHeight) / 2;
		int buttonY = panelY + panelHeight - PANEL_PADDING - 18;
		int backX = width / 2 - 29;
		if (inside(mouseX, mouseY, backX, buttonY, 58, 18)) {
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
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
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
				items.add(stack.copyWithCount(1));
			}
		}
	}

	private void renderButton(GuiGraphics graphics, int x, int y, int width, int height, String text, boolean hovered) {
		Render2D.roundedRect(graphics, x, y, width, height, 4.0F, hovered ? 0xFF444444 : 0xFF292929);
		drawCenteredText(graphics, text, x + width / 2.0F, y + height / 2.0F, 7.0F, hovered ? 0xFFFFFFFF : 0xFFB0B0B0);
	}

	private void drawCenteredText(GuiGraphics graphics, String text, float centerX, float centerY, float size, int color) {
		MsdfFontRenderer renderer = font();
		if (renderer == null) {
			graphics.drawCenteredString(this.font, text, Math.round(centerX), Math.round(centerY - 4.0F), color);
			return;
		}
		renderer.draw(graphics, text, centerX - renderer.width(text, size) / 2.0F, renderer.centeredBaseline(text, size, centerY), size, color);
	}

	private MsdfFontRenderer font() {
		if (uiFont == null && minecraft != null && minecraft.getResourceManager() != null) {
			uiFont = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return uiFont;
	}

	private static boolean inside(double mouseX, double mouseY, double x, double y, double width, double height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
}
