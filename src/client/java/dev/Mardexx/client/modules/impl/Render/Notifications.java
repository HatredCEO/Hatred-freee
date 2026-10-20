package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Notifications extends Module {
	private static final Notifications INSTANCE = new Notifications();
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");
	private static final float ROW_HEIGHT = Keybinds.rowHeight();
	private static final float ROW_GAP = 2.75F;
	private static final float PADDING = 2.64F;
	private static final float STATUS_GAP = 2.2F;
	private static final float SWAP_GAP = 1.32F;
	private static final float SWAP_RIGHT_PADDING = 1.32F;
	private static final float ITEM_ICON_SIZE = 9.9F;
	private static final float ITEM_ICON_Y_OFFSET = 0.0F;
	private static final float CATEGORY_ICON_SIZE = 8.03F;
	private static final float CATEGORY_ICON_GAP = 2.2F;
	private static final float FONT_SIZE = ROW_HEIGHT * 0.68F;
	private static final float CORNER_RADIUS = 5.5F;
	private static final float SHADOW_SPREAD = 6.6F;
	private static final long DISPLAY_TIME_MS = 2600L;
	private static final long FADE_IN_MS = 230L;
	private static final long FADE_OUT_MS = 240L;
	private static final int MAX_NOTIFICATIONS = 5;
	private static final int ENABLED_COLOR = 0xFF58D878;
	private static final int DISABLED_COLOR = 0xFFF05E64;
	private static final String SWORDS_ICON = "\uE000";
	private static final String WIND_ICON = "\uE001";
	private static final String RENDER_ICON = "c";
	private static final String PLAYER_ICON = "d";
	private static final String OTHER_ICON = "e";
	private static MsdfFontRenderer sidebarIconFont;
	private static MsdfFontRenderer hatredCategoryIconFont;
	private static MsdfFontRenderer hatredMincedIconFont;

	private final NotificationsElement element = new NotificationsElement();
	private final List<Entry> entries = new ArrayList<>();
	private MsdfFontRenderer font;

	private Notifications() {
		super("Notifications", ModuleCategory.RENDER, "Shows HUD messages when modules are enabled or disabled.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Notifications") : true;
	}

	public static Notifications getInstance() {
		return INSTANCE;
	}

	public void push(Module module, boolean enabled) {
		if (!enabled() || module == this) {
			return;
		}
		enqueue(Entry.module(module.name(), module.category(), enabled));
	}

	public void pushSwap(ItemStack itemStack) {
		if (!enabled() || itemStack == null || itemStack.isEmpty()) {
			return;
		}
		enqueue(Entry.swap(itemStack.copyWithCount(1)));
	}

	private void enqueue(Entry entry) {
		entries.add(entry);
		while (entries.size() > MAX_NOTIFICATIONS) {
			entries.removeFirst();
		}
	}

	@Override
	protected void onDisable() {
		entries.clear();
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private final class NotificationsElement extends HudElement {
		private float animatedWidth = 72.0F;
		private float animatedHeight = ROW_HEIGHT;

		private NotificationsElement() {
			super("notifications", 6.0F, 52.0F);
		}

		@Override
		public Module module() {
			return Notifications.this;
		}

		@Override
		protected void render(GuiGraphics graphics, float tickDelta) {
			MsdfFontRenderer renderer = font();
			if (renderer == null) {
				return;
			}

			long now = System.currentTimeMillis();
			entries.removeIf(entry -> now - entry.createdAtMs() >= DISPLAY_TIME_MS);
			boolean preview = entries.isEmpty() && Minecraft.getInstance().screen instanceof ChatScreen;
			animatedWidth = Math.max(1.0F, preview ? anchorWidth(renderer) : targetWidth(renderer));
			animatedHeight = Math.max(1.0F, preview ? ROW_HEIGHT : targetHeight());

			if (preview) {
				drawRow(graphics, renderer, Entry.module("Notifications", ModuleCategory.RENDER, true), y(), 1.0F);
				return;
			}

			float rowY = y();
			for (Entry entry : entries) {
				float progress = progress(entry, now);
				if (progress <= 0.001F) {
					continue;
				}
				drawRow(graphics, renderer, entry, rowY, progress);
				rowY += ROW_HEIGHT + ROW_GAP;
			}
		}

		private void drawRow(GuiGraphics graphics, MsdfFontRenderer renderer, Entry entry, float rowY, float progress) {
			float rowWidth = widthFor(renderer, entry);
			float rowX = x() + (animatedWidth - rowWidth) * 0.5F;
			float centerX = rowX + rowWidth * 0.5F;
			float centerY = rowY + ROW_HEIGHT * 0.5F;
			float popupProgress = easeOutCubic(progress);
			float popupScale = 0.84F + 0.16F * popupProgress;
			float alpha = clamp(progress * 1.35F);

			graphics.pose().pushPose();
			graphics.pose().translate(centerX, centerY, 0.0F);
			graphics.pose().scale(popupScale, popupScale, 1.0F);
			graphics.pose().translate(-centerX, -centerY, 0.0F);
			try {
				Render2D.softShadow(graphics, rowX, rowY, rowWidth, ROW_HEIGHT, CORNER_RADIUS, SHADOW_SPREAD, HudManager.withHudAlpha(withAlpha(0x800A0A0D, alpha)));
				Render2D.roundedRect(graphics, rowX, rowY, rowWidth, ROW_HEIGHT, CORNER_RADIUS, HudManager.withHudAlpha(withAlpha(0xE80A0A0D, alpha)));
				if (entry.swap()) {
					drawSwapContent(graphics, renderer, entry, centerX, centerY, alpha);
				} else {
					drawModuleContent(graphics, renderer, entry, centerX, centerY, alpha);
				}
			} finally {
				MsdfFontRenderer.flushPending();
				graphics.pose().popPose();
			}
		}

		private void drawModuleContent(GuiGraphics graphics, MsdfFontRenderer renderer, Entry entry, float centerX, float centerY, float progress) {
			String status = entry.enabled() ? "Enabled" : "Disabled";
			int stateColor = entry.enabled() ? ENABLED_COLOR : DISABLED_COLOR;
			float nameWidth = renderer.width(entry.moduleName(), FONT_SIZE);
			float contentWidth = CATEGORY_ICON_SIZE
					+ CATEGORY_ICON_GAP
					+ nameWidth
					+ STATUS_GAP
					+ renderer.width(status, FONT_SIZE);
			float contentX = centerX - contentWidth * 0.5F;
			drawCategoryIcon(graphics, entry.category(), contentX, centerY, withAlpha(ThemeManager.accent(), progress));
			float nameX = contentX + CATEGORY_ICON_SIZE + CATEGORY_ICON_GAP;
			draw(renderer, graphics, entry.moduleName(), nameX, centerY, withAlpha(0xFFE8E8EC, progress));
			float statusX = nameX + nameWidth + STATUS_GAP;
			draw(renderer, graphics, status, statusX, centerY, withAlpha(stateColor, progress));
		}

		private void drawSwapContent(GuiGraphics graphics, MsdfFontRenderer renderer, Entry entry, float centerX, float centerY, float progress) {
			String text = "Swapped to";
			float textWidth = renderer.width(text, FONT_SIZE);
			float contentWidth = textWidth + SWAP_GAP + ITEM_ICON_SIZE;
			float contentX = centerX - contentWidth * 0.5F;
			draw(renderer, graphics, text, contentX, centerY, withAlpha(0xFFE8E8EC, progress));
			if (progress < 0.08F) {
				return;
			}
			float iconX = contentX + textWidth + SWAP_GAP;
			float iconScale = ITEM_ICON_SIZE / 16.0F;
			graphics.pose().pushPose();
			graphics.pose().translate(iconX + ITEM_ICON_SIZE * 0.5F, centerY + ITEM_ICON_Y_OFFSET, 0.0F);
			graphics.pose().scale(iconScale, iconScale, 1.0F);
			graphics.pose().translate(-8.0F, -8.0F, 0.0F);
			graphics.renderItem(entry.itemStack(), 0, 0);
			graphics.pose().popPose();
		}

		private float targetWidth(MsdfFontRenderer renderer) {
			float width = anchorWidth(renderer);
			for (Entry entry : entries) {
				width = Math.max(width, widthFor(renderer, entry));
			}
			return width;
		}

		private float targetHeight() {
			return entries.isEmpty() ? 0.0F : entries.size() * ROW_HEIGHT + (entries.size() - 1) * ROW_GAP;
		}

		@Override
		public float width() {
			return animatedWidth;
		}

		@Override
		public float height() {
			return animatedHeight;
		}

		@Override
		public boolean visible() {
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Notifications");
		}

	}

	private static float anchorWidth(MsdfFontRenderer renderer) {
		return widthFor(renderer, Entry.module("Notifications", ModuleCategory.RENDER, true));
	}

	private static float widthFor(MsdfFontRenderer renderer, Entry entry) {
		if (entry.swap()) {
			return PADDING
					+ renderer.width("Swapped to", FONT_SIZE)
					+ SWAP_GAP
					+ ITEM_ICON_SIZE
					+ SWAP_RIGHT_PADDING;
		}
		String status = entry.enabled() ? "Enabled" : "Disabled";
		return PADDING * 2.0F
				+ CATEGORY_ICON_SIZE
				+ CATEGORY_ICON_GAP
				+ renderer.width(entry.moduleName(), FONT_SIZE)
				+ STATUS_GAP
				+ renderer.width(status, FONT_SIZE);
	}

	private static float progress(Entry entry, long now) {
		long age = Math.max(0L, now - entry.createdAtMs());
		if (age < FADE_IN_MS) {
			return age / (float) FADE_IN_MS;
		}
		long fadeOutStart = DISPLAY_TIME_MS - FADE_OUT_MS;
		if (age > fadeOutStart) {
			return Math.max(0.0F, (DISPLAY_TIME_MS - age) / (float) FADE_OUT_MS);
		}
		return 1.0F;
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, int color) {
		float baseline = renderer.centeredBaseline(text, FONT_SIZE, centerY);
		renderer.draw(graphics, text, x, baseline, FONT_SIZE, color, 0.0F, 0.025F);
	}

	private static void drawCategoryIcon(GuiGraphics graphics, ModuleCategory category, float x, float centerY, int color) {
		String icon = categoryIcon(category);
		MsdfFontRenderer renderer = categoryIconFont(category);
		if (renderer == null) {
			return;
		}
		float size = categoryIconSize(category);
		float centerX = x + CATEGORY_ICON_SIZE * 0.5F;
		renderer.draw(
				graphics,
				icon,
				renderer.centeredX(icon, size, centerX),
				renderer.centeredBaseline(icon, size, centerY),
				size,
				color,
				0.0F,
				0.025F
		);
	}

	private static String categoryIcon(ModuleCategory category) {
		return switch (category) {
			case COMBAT -> SWORDS_ICON;
			case MOVEMENT -> WIND_ICON;
			case RENDER -> RENDER_ICON;
			case PLAYER -> PLAYER_ICON;
			case OTHER -> OTHER_ICON;
		};
	}

	private static MsdfFontRenderer categoryIconFont(ModuleCategory category) {
		return switch (category) {
			case RENDER, PLAYER -> hatredCategoryIconFont();
			case OTHER -> hatredMincedIconFont();
			default -> sidebarIconFont();
		};
	}

	private static float categoryIconSize(ModuleCategory category) {
		if (category == ModuleCategory.COMBAT || category == ModuleCategory.MOVEMENT) {
			return CATEGORY_ICON_SIZE;
		}

		MsdfFontRenderer baseFont = sidebarIconFont();
		MsdfFontRenderer iconFont = categoryIconFont(category);
		if (baseFont == null || iconFont == null) {
			return CATEGORY_ICON_SIZE;
		}

		float targetSize = baseFont.visualSize(SWORDS_ICON, CATEGORY_ICON_SIZE);
		float currentSize = iconFont.visualSize(categoryIcon(category), CATEGORY_ICON_SIZE);
		if (currentSize <= 0.0F) {
			return CATEGORY_ICON_SIZE;
		}
		return CATEGORY_ICON_SIZE * targetSize / currentSize;
	}

	private static MsdfFontRenderer sidebarIconFont() {
		if (sidebarIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			sidebarIconFont = new MsdfFontRenderer(
					ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/icons/font.png"),
					ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/icons/font.json")
			);
		}
		return sidebarIconFont;
	}

	private static MsdfFontRenderer hatredCategoryIconFont() {
		if (hatredCategoryIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			hatredCategoryIconFont = new MsdfFontRenderer(
					ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/category_icons/font.png"),
					ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/category_icons/font.json")
			);
		}
		return hatredCategoryIconFont;
	}

	private static MsdfFontRenderer hatredMincedIconFont() {
		if (hatredMincedIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			hatredMincedIconFont = new MsdfFontRenderer(
					ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/iconsminced/font.png"),
					ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/iconsminced/font.json")
			);
		}
		return hatredMincedIconFont;
	}

	private static float easeOutCubic(float value) {
		float inverse = 1.0F - Math.max(0.0F, Math.min(1.0F, value));
		return 1.0F - inverse * inverse * inverse;
	}

	private static int withAlpha(int color, float multiplier) {
		int alpha = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, multiplier)));
		return alpha << 24 | color & 0x00FFFFFF;
	}

	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private record Entry(String moduleName, ModuleCategory category, boolean enabled, ItemStack itemStack, boolean swap, long createdAtMs) {
		private static Entry module(String moduleName, ModuleCategory category, boolean enabled) {
			return new Entry(moduleName, category, enabled, ItemStack.EMPTY, false, System.currentTimeMillis());
		}

		private static Entry swap(ItemStack itemStack) {
			return new Entry("", ModuleCategory.OTHER, true, itemStack, true, System.currentTimeMillis());
		}
	}
}
