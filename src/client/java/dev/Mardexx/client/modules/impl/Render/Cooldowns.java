package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.theme.ThemeManager;
import dev.Mardexx.mixins.MixinCooldownInstanceAccessor;
import dev.Mardexx.mixins.MixinItemCooldownsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Cooldowns extends Module {
	private static final Cooldowns INSTANCE = new Cooldowns();
	private static final float BASE_WIDTH = 52.0F;
	private static final float MAX_WIDTH = 198.0F;
	private static final float FONT_SIZE = 6.71F;
	private static final float HEADER_HEIGHT = FONT_SIZE + (16.5F - FONT_SIZE) * 0.40F;
	private static final float ROW_HEIGHT = FONT_SIZE + (15.4F - FONT_SIZE) * 0.36F;
	private static final float HEADER_FONT_SIZE = FONT_SIZE * 1.56F * 0.85F;
	private static final float ROW_START = HEADER_HEIGHT * 0.5F + ROW_HEIGHT * 0.5F;
	private static final float BOTTOM_PADDING = 3.0F * 0.65F * 0.80F;
	private static final float PADDING = 2.64F;
	private static final float NAME_GAP = 1.32F;
	private static final float TIME_GAP = 2.2F;
	private static final float CORNER_RADIUS = 5.5F;
	private static final float SHADOW_SPREAD = 6.6F;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final int ICON_SIZE = Math.round(FONT_SIZE);
	private static final int EXPIRING_TICKS = 40;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private static final ResourceLocation CUSTOM_COOLDOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_cooldown/font.png");
	private static final ResourceLocation CUSTOM_COOLDOWN_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_cooldown/font.json");
	private static MsdfFontRenderer cooldownIconFont;

	private final CooldownsElement element = new CooldownsElement();
	private MsdfFontRenderer font;

	private Cooldowns() {
		super("Cooldowns", ModuleCategory.RENDER, "Shows active item cooldowns and their remaining time.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Cooldowns") : true;
	}

	public static Cooldowns getInstance() {
		return INSTANCE;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	public static MsdfFontRenderer cooldownIconFont() {
		if (cooldownIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			cooldownIconFont = new MsdfFontRenderer(CUSTOM_COOLDOWN_TEXTURE, CUSTOM_COOLDOWN_DATA);
		}
		return cooldownIconFont;
	}

	private final class CooldownsElement extends HudElement {
		private final Map<ResourceLocation, AnimatedCooldown> rows = new LinkedHashMap<>();
		private final Map<ResourceLocation, ItemStack> representativeStacks = new HashMap<>();
		private float animatedWidth = BASE_WIDTH;
		private float animatedHeight = HEADER_HEIGHT;
		private int activeCount;
		private long lastSyncTick = Long.MAX_VALUE;
		private long lastFrameNanos = System.nanoTime();

		private CooldownsElement() {
			super("cooldowns", 174.0F, 32.0F);
		}

		@Override
		public Module module() {
			return Cooldowns.this;
		}

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			Minecraft minecraft = Minecraft.getInstance();
			MsdfFontRenderer renderer = font();
			if (renderer == null) {
				return;
			}

			float delta = frameDelta();
			updateRows(minecraft, delta);
			float targetWidth = targetWidth(renderer);
			animatedWidth = targetWidth > animatedWidth
					? targetWidth
					: animate(animatedWidth, targetWidth, 16.0F, delta);
			float contentProgress = 0.0F;
			for (AnimatedCooldown row : rows.values()) {
				contentProgress += row.progress();
			}
			float contentHeight = contentProgress * ROW_HEIGHT;
			float targetHeight = rows.isEmpty()
					? HEADER_HEIGHT
					: Math.max(HEADER_HEIGHT, ROW_START + contentHeight + BOTTOM_PADDING);
			animatedHeight = animate(animatedHeight, targetHeight, 18.0F, delta);
			int accent = ThemeManager.accent();

			Render2D.softShadow(guiGraphics, x(), y(), animatedWidth, animatedHeight, CORNER_RADIUS, SHADOW_SPREAD, HudManager.withHudAlpha(SHADOW_COLOR));
			Render2D.roundedRect(guiGraphics, x(), y(), animatedWidth, animatedHeight, CORNER_RADIUS, HudManager.withHudAlpha(0xE80A0A0D));

			// Render custom vector hourglass icon in header
			float headerIconSize = HEADER_FONT_SIZE * 1.10F;
			MsdfFontRenderer cdIconFont = cooldownIconFont();
			float iconBaseline = cdIconFont.centeredBaseline("c", headerIconSize, y() + HEADER_HEIGHT * 0.5F);
			cdIconFont.draw(guiGraphics, "c", x() + PADDING, iconBaseline, headerIconSize, accent, 0.0F, 0.05F);
			float iconWidth = cdIconFont.width("c", headerIconSize);
			float titleX = x() + PADDING + iconWidth + 4.0F;
			float titleBaseline = renderer.centeredBaseline("Cooldowns", HEADER_FONT_SIZE, y() + HEADER_HEIGHT * 0.5F);
			renderer.draw(guiGraphics, "Cooldowns", titleX, titleBaseline, HEADER_FONT_SIZE, 0xFFFFFFFF, 0.0F, 0.05F);

			guiGraphics.enableScissor(Math.round(x()), Math.round(y()), Math.round(x() + animatedWidth), Math.round(y() + animatedHeight));
			float rowY = y() + ROW_START;
			for (AnimatedCooldown row : rows.values()) {
				float progress = easeOutCubic(row.progress());
				CooldownEntry cooldown = row.cooldown();
				boolean expiring = cooldown.remainingTicks() <= EXPIRING_TICKS;
				if (expiring) {
					float pulse = 0.5F + 0.5F * (float) Math.sin(System.nanoTime() / 180_000_000.0D);
					int alpha = Math.round((35.0F + 30.0F * pulse) * progress);
					Render2D.roundedRect(guiGraphics, x() + 3.0F, rowY + 1.0F, animatedWidth - 6.0F, ROW_HEIGHT - 2.0F, 1.0F, alpha << 24 | accent & 0x00FFFFFF);
				}

				float centerY = rowY + ROW_HEIGHT * 0.5F;
				renderItem(guiGraphics, cooldown.stack(), x() + PADDING, centerY, progress);
				String duration = duration(cooldown.remainingTicks());
				float durationWidth = renderer.width(duration, FONT_SIZE);
				String name = cooldown.stack().getHoverName().getString();
				draw(renderer, guiGraphics, name, x() + PADDING + ICON_SIZE + NAME_GAP, centerY, withAlpha(0xFFD4D4D9, progress));
				int durationColor = expiring ? 0xFF000000 | accent & 0x00FFFFFF : 0xFF92929C;
				draw(renderer, guiGraphics, duration, x() + animatedWidth - PADDING - durationWidth, centerY, withAlpha(durationColor, progress));
				rowY += ROW_HEIGHT * row.progress();
			}
			MsdfFontRenderer.flushPending();
			guiGraphics.disableScissor();
		}

		private void updateRows(Minecraft minecraft, float delta) {
			long syncTick = minecraft.player == null ? Long.MIN_VALUE : minecraft.player.tickCount;
			if (syncTick != lastSyncTick) {
				lastSyncTick = syncTick;
				syncRows(activeCooldowns(minecraft, representativeStacks));
			}

			Iterator<Map.Entry<ResourceLocation, AnimatedCooldown>> iterator = rows.entrySet().iterator();
			while (iterator.hasNext()) {
				AnimatedCooldown row = iterator.next().getValue();
				row.progress = animate(row.progress, row.target ? 1.0F : 0.0F, 15.0F, delta);
				if (!row.target && row.progress <= 0.001F) {
					iterator.remove();
				}
			}
		}

		private void syncRows(List<CooldownEntry> active) {
			activeCount = active.size();
			for (AnimatedCooldown row : rows.values()) {
				row.target = false;
			}
			for (CooldownEntry cooldown : active) {
				AnimatedCooldown row = rows.computeIfAbsent(cooldown.group(), ignored -> new AnimatedCooldown(cooldown));
				row.cooldown = cooldown;
				row.target = true;
			}
		}

		private float targetWidth(MsdfFontRenderer renderer) {
			float headerIconSize = HEADER_FONT_SIZE * 1.10F;
			float required = PADDING + headerIconSize + 0.9F + renderer.width("Cooldowns", HEADER_FONT_SIZE) + PADDING + 1.0F;
			for (AnimatedCooldown row : rows.values()) {
				CooldownEntry cooldown = row.cooldown();
				float rowWidth = PADDING * 2.0F
						+ ICON_SIZE
						+ NAME_GAP
						+ renderer.width(cooldown.stack().getHoverName().getString(), FONT_SIZE)
						+ TIME_GAP
						+ renderer.width(duration(cooldown.remainingTicks()), FONT_SIZE);
				required = Math.max(required, rowWidth);
			}
			return Math.min(MAX_WIDTH, required);
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
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Cooldowns");
		}

		private float frameDelta() {
			long now = System.nanoTime();
			float delta = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
			lastFrameNanos = now;
			return delta;
		}
	}

	private static List<CooldownEntry> activeCooldowns(Minecraft minecraft, Map<ResourceLocation, ItemStack> representativeStacks) {
		if (minecraft.player == null) {
			return List.of();
		}

		ItemCooldowns cooldowns = minecraft.player.getCooldowns();
		MixinItemCooldownsAccessor accessor = (MixinItemCooldownsAccessor) cooldowns;
		int tickCount = accessor.hatred$getTickCount();
		List<CooldownEntry> active = new ArrayList<>();
		for (Map.Entry<ResourceLocation, Object> entry : accessor.hatred$getCooldowns().entrySet()) {
			if (!(entry.getValue() instanceof MixinCooldownInstanceAccessor instance)) {
				continue;
			}
			int remainingTicks = instance.hatred$getEndTime() - tickCount;
			if (remainingTicks <= 0) {
				continue;
			}
			ItemStack stack = representativeStacks.computeIfAbsent(
					entry.getKey(),
					group -> representativeStack(minecraft, cooldowns, group)
			);
			if (!stack.isEmpty()) {
				active.add(new CooldownEntry(entry.getKey(), stack, remainingTicks));
			}
		}
		active.sort(Comparator.comparingInt(CooldownEntry::remainingTicks));
		return active;
	}

	private static ItemStack representativeStack(Minecraft minecraft, ItemCooldowns cooldowns, ResourceLocation group) {
		for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = minecraft.player.getInventory().getItem(slot);
			if (!stack.isEmpty() && cooldowns.getCooldownGroup(stack).equals(group)) {
				return stack.copyWithCount(1);
			}
		}
		return BuiltInRegistries.ITEM.getOptional(group)
				.map(ItemStack::new)
				.orElse(ItemStack.EMPTY);
	}

	private static void renderItem(GuiGraphics graphics, ItemStack stack, float x, float centerY, float progress) {
		float iconScale = ICON_SIZE / 16.0F * (0.65F + 0.35F * progress);
		graphics.pose().pushPose();
		graphics.pose().translate(x + ICON_SIZE * 0.5F, centerY, 0.0F);
		graphics.pose().scale(iconScale, iconScale, 1.0F);
		graphics.pose().translate(-8.0F, -8.0F, 0.0F);
		graphics.renderItem(stack, 0, 0);
		graphics.pose().popPose();
	}

	private static String duration(int ticks) {
		float seconds = Math.max(0, ticks) / 20.0F;
		if (seconds < 60.0F) {
			return String.format(Locale.ROOT, "%.1fs", seconds);
		}
		int roundedSeconds = Math.max(0, (ticks + 19) / 20);
		return String.format(Locale.ROOT, "%d:%02d", roundedSeconds / 60, roundedSeconds % 60);
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, int color) {
		float baseline = renderer.centeredBaseline(text, FONT_SIZE, centerY);
		renderer.draw(graphics, text, x, baseline, FONT_SIZE, color);
	}

	private static void drawBold(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, float size, int color) {
		float baseline = renderer.centeredBaseline(text, size, centerY);
		renderer.draw(graphics, text, x, baseline, size, color);
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

	private record CooldownEntry(ResourceLocation group, ItemStack stack, int remainingTicks) {
	}

	private static final class AnimatedCooldown {
		private CooldownEntry cooldown;
		private float progress;
		private boolean target = true;

		private AnimatedCooldown(CooldownEntry cooldown) {
			this.cooldown = cooldown;
		}

		private CooldownEntry cooldown() {
			return cooldown;
		}

		private float progress() {
			return progress;
		}
	}
}
