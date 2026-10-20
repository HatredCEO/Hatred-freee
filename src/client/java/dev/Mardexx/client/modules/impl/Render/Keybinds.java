package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.modules.ModuleManager;
import dev.Mardexx.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Keybinds extends Module {
	private static final Keybinds INSTANCE = new Keybinds();
	private static final float CONTENT_SCALE = 1.32F;
	private static final float BASE_WIDTH = 42.0F;
	private static final float MAX_WIDTH = 176.0F;
	private static final float FONT_SIZE = 6.3F * CONTENT_SCALE;
	private static final float BIND_FONT_SIZE = FONT_SIZE * 0.90F;
	private static final float TITLE_FONT_SIZE = FONT_SIZE * 1.3F * 0.85F;
	private static final float HEADER_HEIGHT = TITLE_FONT_SIZE + (14.3F - TITLE_FONT_SIZE) * 0.40F;
	private static final float ORIGINAL_ROW_HEIGHT = 12.0F * CONTENT_SCALE;
	private static final float ROW_HEIGHT = FONT_SIZE + (ORIGINAL_ROW_HEIGHT - FONT_SIZE) * 0.252F;
	private static final float ROW_START = HEADER_HEIGHT * 0.5F + ROW_HEIGHT * 0.5F;
	private static final float BOTTOM_PADDING = 1.2F * 0.65F * 0.75F;
	private static final float BIND_GAP = 3.1F;
	private static final float CATEGORY_ICON_SIZE = 8.03F;
	private static final float CATEGORY_ICON_GAP = 2.15F;
	private static final float CORNER_RADIUS = 5.5F;
	private static final float SHADOW_SPREAD = 6.6F;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final float PADDING = 2.4F;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");
	private static final String SWORDS_ICON = "\uE000";
	private static final String WIND_ICON = "\uE001";
	private static final String RENDER_ICON = "c";
	private static final String PLAYER_ICON = "d";
	private static final String OTHER_ICON = "e";
	private static final ResourceLocation CUSTOM_KEYBOARD_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_keyboard/font.png");
	private static final ResourceLocation CUSTOM_KEYBOARD_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_keyboard/font.json");
	private static MsdfFontRenderer sidebarIconFont;
	private static MsdfFontRenderer hatredCategoryIconFont;
	private static MsdfFontRenderer hatredMincedIconFont;
	private static MsdfFontRenderer keyboardIconFont;

	private final KeybindsElement element = new KeybindsElement();
	private MsdfFontRenderer font;

	public static MsdfFontRenderer keyboardIconFont() {
		if (keyboardIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			keyboardIconFont = new MsdfFontRenderer(CUSTOM_KEYBOARD_TEXTURE, CUSTOM_KEYBOARD_DATA);
		}
		return keyboardIconFont;
	}

	private Keybinds() {
		super("Keybinds", ModuleCategory.RENDER, "Shows enabled modules that have a keybind.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Keybinds") : true;
	}

	public static Keybinds getInstance() {
		return INSTANCE;
	}

	public static float rowHeight() {
		return ROW_HEIGHT;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private final class KeybindsElement extends HudElement {
		private final Map<Module, AnimatedBind> rows = new LinkedHashMap<>();
		private float animatedWidth = BASE_WIDTH;
		private float animatedHeight = HEADER_HEIGHT;
		private long nextModuleSyncNanos;
		private long lastFrameNanos = System.nanoTime();

		private KeybindsElement() {
			super("keybinds", 6.0F, 32.0F);
		}

		@Override
		public Module module() {
			return Keybinds.this;
		}

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			MsdfFontRenderer renderer = font();
			if (renderer == null) {
				return;
			}

			float delta = frameDelta();
			updateRows(renderer, delta);
			float targetWidth = targetWidth(renderer);
			animatedWidth = targetWidth > animatedWidth
					? targetWidth
					: animate(animatedWidth, targetWidth, 16.0F, delta);
			float contentProgress = 0.0F;
			for (AnimatedBind row : rows.values()) {
				contentProgress += row.progress();
			}
			float contentHeight = contentProgress * ROW_HEIGHT;
			float targetHeight = rows.isEmpty()
					? HEADER_HEIGHT
					: Math.max(HEADER_HEIGHT, ROW_START + contentHeight + BOTTOM_PADDING);
			animatedHeight = animate(animatedHeight, targetHeight, 18.0F, delta);

			Render2D.softShadow(guiGraphics, x(), y(), animatedWidth, animatedHeight, CORNER_RADIUS, SHADOW_SPREAD, HudManager.withHudAlpha(SHADOW_COLOR));
			Render2D.roundedRect(guiGraphics, x(), y(), animatedWidth, animatedHeight, CORNER_RADIUS, HudManager.withHudAlpha(0xE80A0A0D));

			// Render custom vector keyboard icon in header
			float headerIconSize = TITLE_FONT_SIZE * 1.15F;
			MsdfFontRenderer kbIconFont = keyboardIconFont();
			float iconBaseline = kbIconFont.centeredBaseline("k", headerIconSize, y() + HEADER_HEIGHT * 0.5F);
			kbIconFont.draw(guiGraphics, "k", x() + PADDING, iconBaseline, headerIconSize, ThemeManager.accent(), 0.0F, 0.05F);
			float iconWidth = kbIconFont.width("k", headerIconSize);
			float titleX = x() + PADDING + iconWidth + 4.0F;
			float titleBaseline = renderer.centeredBaseline("Hotkeys", TITLE_FONT_SIZE, y() + HEADER_HEIGHT * 0.5F);
			renderer.draw(guiGraphics, "Hotkeys", titleX, titleBaseline, TITLE_FONT_SIZE, 0xFFFFFFFF, 0.0F, 0.05F);


			guiGraphics.enableScissor(Math.round(x()), Math.round(y()), Math.round(x() + animatedWidth), Math.round(y() + animatedHeight));
			float rowY = y() + ROW_START;
			for (AnimatedBind row : rows.values()) {
				float progress = easeOutCubic(row.progress());
				float centerY = rowY + ROW_HEIGHT * 0.5F;
				int color = withAlpha(0xFFD3D3D8, progress);
				String key = row.keyName();
				float keyWidth = row.keyWidth();
				String name = row.module().name();
				drawCategoryIcon(guiGraphics, row.module().category(), x() + PADDING, centerY, withAlpha(ThemeManager.accent(), progress));
				draw(renderer, guiGraphics, name, x() + PADDING + CATEGORY_ICON_SIZE + CATEGORY_ICON_GAP, centerY, color);
				float keyX = x() + animatedWidth - PADDING - keyWidth;
				draw(renderer, guiGraphics, key, keyX, centerY, BIND_FONT_SIZE, withAlpha(0xFF8E8E98, progress));
				rowY += ROW_HEIGHT * row.progress();
			}
			MsdfFontRenderer.flushPending();
			guiGraphics.disableScissor();
		}

		private void updateRows(MsdfFontRenderer renderer, float delta) {
			long now = System.nanoTime();
			if (now >= nextModuleSyncNanos) {
				nextModuleSyncNanos = now + 25_000_000L;
				for (AnimatedBind row : rows.values()) {
					row.target = false;
				}
				for (Module module : ModuleManager.modules()) {
					if (module == Keybinds.this || !module.enabled() || module.keyBind() < 0) {
						continue;
					}
					AnimatedBind row = rows.get(module);
					if (row == null) {
						row = new AnimatedBind(module, renderer);
						rows.put(module, row);
					} else {
						row.updateMetrics(renderer);
					}
					row.target = true;
				}
			}

			Iterator<AnimatedBind> iterator = rows.values().iterator();
			while (iterator.hasNext()) {
				AnimatedBind row = iterator.next();
				row.progress = animate(row.progress, row.target ? 1.0F : 0.0F, 15.0F, delta);
				if (!row.target && row.progress <= 0.001F) {
					iterator.remove();
				}
			}
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
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Keybinds");
		}

		private float frameDelta() {
			long now = System.nanoTime();
			float delta = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
			lastFrameNanos = now;
			return delta;
		}

		private float targetWidth(MsdfFontRenderer renderer) {
			float headerIconSize = TITLE_FONT_SIZE * 1.15F;
			float required = PADDING + headerIconSize + 1.1F + renderer.width("Hotkeys", TITLE_FONT_SIZE) + PADDING + 1.0F;
			for (AnimatedBind row : rows.values()) {
				float rowWidth = PADDING * 2.0F
						+ CATEGORY_ICON_SIZE
						+ CATEGORY_ICON_GAP
						+ row.nameWidth()
						+ BIND_GAP
						+ row.keyWidth();
				required = Math.max(required, rowWidth);
			}
			return Math.min(MAX_WIDTH, required);
		}

	}

	private static final class AnimatedBind {
		private final Module module;
		private final float nameWidth;
		private int keyCode = Integer.MIN_VALUE;
		private String keyName = "";
		private float keyWidth;
		private float progress;
		private boolean target = true;

		private AnimatedBind(Module module, MsdfFontRenderer renderer) {
			this.module = module;
			this.nameWidth = renderer.width(module.name(), FONT_SIZE);
			updateMetrics(renderer);
		}

		private Module module() {
			return module;
		}

		private float progress() {
			return progress;
		}

		private void updateMetrics(MsdfFontRenderer renderer) {
			int currentKeyCode = module.keyBind();
			if (currentKeyCode == keyCode) {
				return;
			}
			keyCode = currentKeyCode;
			keyName = Keybinds.keyName(currentKeyCode);
			keyWidth = renderer.width(keyName, BIND_FONT_SIZE);
		}

		private float nameWidth() {
			return nameWidth;
		}

		private String keyName() {
			return keyName;
		}

		private float keyWidth() {
			return keyWidth;
		}
	}

	private static String keyName(int keyCode) {
		String name = GLFW.glfwGetKeyName(keyCode, 0);
		if (name != null && !name.isEmpty()) {
			return name.toUpperCase(Locale.ROOT);
		}
		return switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "SHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL";
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "ALT";
			case GLFW.GLFW_KEY_SPACE -> "SPACE";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			case GLFW.GLFW_KEY_ENTER -> "ENTER";
			default -> "KEY" + keyCode;
		};
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, int color) {
		draw(renderer, graphics, text, x, centerY, FONT_SIZE, color);
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, float size, int color) {
		float baseline = renderer.centeredBaseline(text, size, centerY);
		renderer.draw(graphics, text, x, baseline, size, color);
	}

	private static void drawBold(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, float size, int color) {
		float baseline = renderer.centeredBaseline(text, size, centerY);
		renderer.draw(graphics, text, x, baseline, size, color);
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
				color
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

	private static float animate(float current, float target, float speed, float delta) {
		float result = current + (target - current) * (1.0F - (float) Math.exp(-speed * delta));
		return Math.abs(result - target) < 0.001F ? target : result;
	}

	private static float easeOutCubic(float value) {
		float inverse = 1.0F - Math.max(0.0F, Math.min(1.0F, value));
		return 1.0F - inverse * inverse * inverse;
	}

	private static int withAlpha(int color, float multiplier) {
		int alpha = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, multiplier)));
		return alpha << 24 | color & 0x00FFFFFF;
	}
}
