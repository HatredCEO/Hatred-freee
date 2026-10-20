package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.modules.impl.Other.Helper;
import dev.Mardexx.client.theme.ThemeManager;
import dev.Mardexx.mixins.MixinCooldownInstanceAccessor;
import dev.Mardexx.mixins.MixinItemCooldownsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HelperHud extends Module {
	private static final HelperHud INSTANCE = new HelperHud();
	private static final float HUD_SCALE = 1.1F;
	private static final float HEIGHT = 13.0F * HUD_SCALE;
	private static final float FONT_SIZE = 6.3F * HUD_SCALE;
	private static final float ICON_SIZE = 9.0F * HUD_SCALE;
	private static final float PADDING = 3.0F * HUD_SCALE;
	private static final float ICON_GAP = 2.5F * HUD_SCALE;
	private static final float ITEM_GAP = 6.0F * HUD_SCALE;
	private static final float CORNER_RADIUS = 5.5F * HUD_SCALE;
	private static final float SHADOW_SPREAD = 6.0F * HUD_SCALE;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final int BACKGROUND_COLOR = 0xE80A0A0D;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private final HelperElement element = new HelperElement();
	private MsdfFontRenderer font;

	private HelperHud() {
		super("Helper HUD", ModuleCategory.RENDER, "Shows Helper item binds and active cooldowns.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Helper HUD") : true;
	}

	public static HelperHud getInstance() {
		return INSTANCE;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private final class HelperElement extends HudElement {
		private final Map<String, AnimatedItem> items = new LinkedHashMap<>();
		private float currentWidth;
		private float currentHeight;
		private long nextSyncNanos;

		private HelperElement() {
			super("helper_hud", 6.0F, 48.0F);
		}

		@Override
		public Module module() {
			return HelperHud.this;
		}

		@Override
		protected void render(GuiGraphics graphics, float tickDelta) {
			MsdfFontRenderer renderer = font();
			if (renderer == null) {
				return;
			}
			updateItems(renderer);
			float targetWidth = targetWidth();
			currentWidth = targetWidth;
			currentHeight = targetWidth > 0.01F ? HEIGHT : 0.0F;
			if (currentWidth < 0.1F || currentHeight < 0.1F) {
				return;
			}

			float centerY = y() + currentHeight * 0.5F;
			Render2D.softShadow(graphics, x(), y(), currentWidth, currentHeight, CORNER_RADIUS, SHADOW_SPREAD, HudManager.withHudAlpha(SHADOW_COLOR));
			Render2D.roundedRect(graphics, x(), y(), currentWidth, currentHeight, CORNER_RADIUS, HudManager.withHudAlpha(BACKGROUND_COLOR));
			float itemX = x() + PADDING;
			Helper helper = Helper.getInstance();
			for (AnimatedItem animated : items.values()) {
				boolean inInventory = helper.hasInventoryItem(animated.item.item());
				int textColor = animated.cooldown ? ThemeManager.accent() : (inInventory ? 0xFFD3D3D8 : 0xFF7A7A82);
				renderItem(graphics, new ItemStack(animated.item.item()), itemX, centerY);
				draw(renderer, graphics, animated.label, itemX + ICON_SIZE + ICON_GAP, centerY, textColor);
				MsdfFontRenderer.flushPending();
				itemX += animated.width + ITEM_GAP;
			}
		}

		private void updateItems(MsdfFontRenderer renderer) {
			long now = System.nanoTime();
			if (now >= nextSyncNanos) {
				nextSyncNanos = now + 25_000_000L;
				items.clear();
				Helper helper = Helper.getInstance();
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null) {
					return;
				}
				List<Helper.HudItem> available = helper.hudItems();
				boolean inChat = minecraft.screen instanceof ChatScreen;

				List<Helper.HudItem> boundItems = new ArrayList<>();
				for (Helper.HudItem item : available) {
					if (item.key() >= 0) {
						boundItems.add(item);
					}
				}

				if (boundItems.isEmpty()) {
					if (inChat) {
						int count = 0;
						for (Helper.HudItem item : available) {
							if (count >= 3) break;
							count++;
							String label = item.key() >= 0 ? keyName(item.key()) : "KEY";
							AnimatedItem animated = new AnimatedItem(item);
							animated.label = label;
							animated.width = PADDING * 2.0F + ICON_SIZE + ICON_GAP + renderer.width(label, FONT_SIZE);
							animated.cooldown = false;
							items.put(item.name(), animated);
						}
					}
					return;
				}

				for (Helper.HudItem item : boundItems) {
					String label = cooldownLabel(minecraft, item);
					AnimatedItem animated = new AnimatedItem(item);
					animated.label = label;
					animated.width = PADDING * 2.0F + ICON_SIZE + ICON_GAP + renderer.width(label, FONT_SIZE);
					animated.cooldown = isOnCooldown(minecraft, item);
					items.put(item.name(), animated);
				}
			}
		}

		private float targetWidth() {
			float width = 0.0F;
			for (AnimatedItem item : items.values()) {
				if (width > 0.0F) {
					width += ITEM_GAP;
				}
				width += item.width;
			}
			return width;
		}

		@Override
		public float width() {
			return currentWidth;
		}

		@Override
		public float height() {
			return currentHeight;
		}

		@Override
		public boolean visible() {
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Helper HUD");
		}

	}

	private static ItemStack findInventoryStack(Minecraft minecraft, net.minecraft.world.item.Item item) {
		if (minecraft.player != null) {
			for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = minecraft.player.getInventory().getItem(slot);
				if (!stack.isEmpty() && stack.is(item)) {
					return stack;
				}
			}
		}
		return new ItemStack(item);
	}

	private static String cooldownLabel(Minecraft minecraft, Helper.HudItem item) {
		int remaining = remainingCooldownTicks(minecraft, item);
		if (remaining > 0) {
			return String.format(Locale.ROOT, "%.1fs", remaining / 20.0F);
		}
		return item.key() < 0 ? "None" : keyName(item.key());
	}

	private static boolean isOnCooldown(Minecraft minecraft, Helper.HudItem item) {
		return remainingCooldownTicks(minecraft, item) > 0;
	}

	private static int remainingCooldownTicks(Minecraft minecraft, Helper.HudItem item) {
		if (minecraft.player != null) {
			ItemCooldowns cooldowns = minecraft.player.getCooldowns();
			ItemStack stack = findInventoryStack(minecraft, item.item());
			if (!cooldowns.isOnCooldown(stack)) {
				return 0;
			}
			ResourceLocation group = cooldowns.getCooldownGroup(stack);
			MixinItemCooldownsAccessor accessor = (MixinItemCooldownsAccessor) cooldowns;
			int tickCount = accessor.hatred$getTickCount();
			Object value = accessor.hatred$getCooldowns().get(group);
			if (value instanceof MixinCooldownInstanceAccessor instance) {
				int remaining = instance.hatred$getEndTime() - tickCount;
				if (remaining > 0) {
					return remaining;
				}
			}
			for (Map.Entry<ResourceLocation, Object> entry : accessor.hatred$getCooldowns().entrySet()) {
				if (entry.getValue() instanceof MixinCooldownInstanceAccessor instance) {
					if (entry.getKey().equals(group) || entry.getKey().getPath().equalsIgnoreCase(item.name())) {
						int remaining = instance.hatred$getEndTime() - tickCount;
						if (remaining > 0) {
							return remaining;
						}
					}
				}
			}
		}
		return 0;
	}

	private static void renderItem(GuiGraphics graphics, ItemStack stack, float x, float centerY) {
		float scale = ICON_SIZE / 16.0F;
		graphics.pose().pushPose();
		graphics.pose().translate(x + ICON_SIZE * 0.5F, centerY, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-8.0F, -8.0F, 0.0F);
		graphics.renderItem(stack, 0, 0);
		graphics.pose().popPose();
	}

	private static String keyName(int key) {
		String name = GLFW.glfwGetKeyName(key, 0);
		if (name != null && !name.isEmpty()) {
			return name.toUpperCase(Locale.ROOT);
		}
		return switch (key) {
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "SHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL";
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "ALT";
			case GLFW.GLFW_KEY_SPACE -> "SPACE";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			case GLFW.GLFW_KEY_ENTER -> "ENTER";
			default -> "KEY" + key;
		};
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, int color) {
		float baseline = renderer.centeredBaseline(text, FONT_SIZE, centerY);
		renderer.draw(graphics, text, x, baseline, FONT_SIZE, color, 0.0F, 0.025F);
	}

	private static final class AnimatedItem {
		private Helper.HudItem item;
		private String label = "None";
		private float width;
		private boolean cooldown;

		private AnimatedItem(Helper.HudItem item) {
			this.item = item;
		}
	}
}
