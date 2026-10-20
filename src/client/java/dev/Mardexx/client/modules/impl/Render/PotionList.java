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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PotionList extends Module {
	private static final PotionList INSTANCE = new PotionList();
	private static final float BASE_WIDTH = 52.8F;
	private static final float MAX_WIDTH = 216.0F;
	private static final float FONT_SIZE = 8.05F;
	private static final float DURATION_FONT_SIZE = FONT_SIZE * 0.80F;
	private static final float HEADER_HEIGHT = 10.6F;
	private static final float ROW_HEIGHT = 8.65F;
	private static final float HEADER_FONT_SIZE = FONT_SIZE * 1.56F * 0.85F * 0.85F;
	private static final float ROW_START = HEADER_HEIGHT + 0.5F;
	private static final float BOTTOM_PADDING = 1.2F;
	private static final float PADDING = 2.88F;
	private static final float NAME_GAP = 1.44F;
	private static final float TIME_GAP = 2.40F;
	private static final float CORNER_RADIUS = 6.6F;
	private static final float SHADOW_SPREAD = 7.2F;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final float ICON_SIZE = FONT_SIZE * 0.85F;
	private static final int EXPIRING_TICKS = 100;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private static final ResourceLocation ICON_FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_potion/font.png");
	private static final ResourceLocation ICON_FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_potion/font.json");

	private final PotionListElement element = new PotionListElement();
	private MsdfFontRenderer font;
	private static MsdfFontRenderer iconFont;

	private PotionList() {
		super("Potion List", ModuleCategory.RENDER, "Shows active effects and their remaining duration.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Potion List") : true;
	}

	public static PotionList getInstance() {
		return INSTANCE;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	public static MsdfFontRenderer potionIconFont() {
		if (iconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			iconFont = new MsdfFontRenderer(ICON_FONT_TEXTURE, ICON_FONT_DATA);
		}
		return iconFont;
	}

	private final class PotionListElement extends HudElement {
		private final Map<String, AnimatedEffect> rows = new LinkedHashMap<>();
		private final List<MobEffectInstance> activeEffects = new ArrayList<>();
		private float animatedWidth = BASE_WIDTH;
		private float animatedHeight = HEADER_HEIGHT;
		private int activeEffectCount;
		private long lastSyncTick = Long.MAX_VALUE;
		private long lastFrameNanos = System.nanoTime();

		private PotionListElement() {
			super("potion_list", 108.0F, 32.0F);
		}

		@Override
		public Module module() {
			return PotionList.this;
		}

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			MsdfFontRenderer renderer = font();
			Minecraft minecraft = Minecraft.getInstance();
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
			for (AnimatedEffect row : rows.values()) {
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

			// Render the custom MSDF vector potion icon from user SVG in the header (-15% icon scale)
			float headerIconSize = HEADER_FONT_SIZE * 1.10F * 0.85F;
			MsdfFontRenderer iconFont = potionIconFont();
			float iconBaseline = iconFont.centeredBaseline("p", headerIconSize, y() + HEADER_HEIGHT * 0.5F);
			iconFont.draw(guiGraphics, "p", x() + PADDING, iconBaseline, headerIconSize, accent, 0.0F, 0.05F);
			float iconWidth = iconFont.width("p", headerIconSize);
			float titleX = x() + PADDING + iconWidth + 4.0F;
			float titleBaseline = renderer.centeredBaseline("Effects", HEADER_FONT_SIZE, y() + HEADER_HEIGHT * 0.5F);
			renderer.draw(guiGraphics, "Effects", titleX, titleBaseline, HEADER_FONT_SIZE, 0xFFFFFFFF, 0.0F, 0.05F);

			guiGraphics.enableScissor(Math.round(x()), Math.round(y()), Math.round(x() + animatedWidth), Math.round(y() + animatedHeight));
			float rowY = y() + ROW_START;
			for (AnimatedEffect row : rows.values()) {
				float progress = easeOutCubic(row.progress());
				MobEffectInstance effect = row.effect();
				boolean expiring = !effect.isInfiniteDuration() && effect.getDuration() <= EXPIRING_TICKS;
				if (expiring) {
					float pulse = 0.5F + 0.5F * (float) Math.sin(System.nanoTime() / 180_000_000.0D);
					int alpha = Math.round((35.0F + 30.0F * pulse) * progress);
					Render2D.roundedRect(guiGraphics, x() + 3.0F, rowY + 1.0F, animatedWidth - 6.0F, ROW_HEIGHT - 2.0F, 1.0F, alpha << 24 | accent & 0x00FFFFFF);
				}

				float centerY = rowY + ROW_HEIGHT * 0.5F;
				TextureAtlasSprite sprite = minecraft.getMobEffectTextures().get(effect.getEffect());
				float scale = 0.70F + 0.30F * progress;
				float effectiveSize = ICON_SIZE * scale;
				float iconX = x() + PADDING + (ICON_SIZE - effectiveSize) * 0.5F;
				float iconY = centerY - effectiveSize * 0.5F;
				Render2D.sprite(guiGraphics, sprite, iconX, iconY, effectiveSize, effectiveSize, withAlpha(0xFFFFFFFF, progress));

				String duration = duration(effect);
				float durationWidth = renderer.width(duration, DURATION_FONT_SIZE);
				String name = effectName(effect);
				draw(renderer, guiGraphics, name, x() + PADDING + ICON_SIZE + NAME_GAP, centerY, withAlpha(0xFFD4D4D9, progress));
				int durationColor = expiring ? 0xFF000000 | accent & 0x00FFFFFF : 0xFF92929C;
				draw(renderer, guiGraphics, duration, x() + animatedWidth - PADDING - durationWidth, centerY, DURATION_FONT_SIZE, withAlpha(durationColor, progress));
				rowY += ROW_HEIGHT * row.progress();
			}
			MsdfFontRenderer.flushPending();
			guiGraphics.disableScissor();
		}

		private void updateRows(Minecraft minecraft, float delta) {
			long syncTick = minecraft.player == null ? Long.MIN_VALUE : minecraft.player.tickCount;
			if (syncTick != lastSyncTick) {
				lastSyncTick = syncTick;
				syncRows(minecraft);
			}

			Iterator<Map.Entry<String, AnimatedEffect>> iterator = rows.entrySet().iterator();
			while (iterator.hasNext()) {
				AnimatedEffect row = iterator.next().getValue();
				row.progress = animate(row.progress, row.target ? 1.0F : 0.0F, 15.0F, delta);
				if (!row.target && row.progress <= 0.001F) {
					iterator.remove();
				}
			}
		}

		private void syncRows(Minecraft minecraft) {
			activeEffects.clear();
			if (minecraft.player != null) {
				for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
					if (effect != null && effect.isVisible()) {
						activeEffects.add(effect);
					}
				}
			}
			activeEffects.sort(Comparator.comparing(MobEffectInstance::getDescriptionId));
			activeEffectCount = activeEffects.size();
			for (AnimatedEffect row : rows.values()) {
				row.target = false;
			}
			for (MobEffectInstance effect : activeEffects) {
				String key = effect.getDescriptionId();
				AnimatedEffect row = rows.computeIfAbsent(key, ignored -> new AnimatedEffect(effect));
				row.effect = effect;
				row.target = true;
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
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Potion List");
		}

		private float frameDelta() {
			long now = System.nanoTime();
			float delta = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
			lastFrameNanos = now;
			return delta;
		}

		private float targetWidth(MsdfFontRenderer renderer) {
			float headerIconSize = HEADER_FONT_SIZE * 1.10F * 0.85F;
			float required = PADDING + headerIconSize + 1.1F + renderer.width("Effects", HEADER_FONT_SIZE) + PADDING + 1.2F;
			for (AnimatedEffect row : rows.values()) {
				String name = effectName(row.effect());
				float rowWidth = PADDING * 2.0F
						+ ICON_SIZE
						+ NAME_GAP
						+ renderer.width(name, FONT_SIZE)
						+ TIME_GAP
						+ durationColumnWidth(renderer, row.effect());
				required = Math.max(required, rowWidth);
			}
			return Math.min(MAX_WIDTH, required);
		}

		private float durationColumnWidth(MsdfFontRenderer renderer, MobEffectInstance effect) {
			return renderer.width(duration(effect), DURATION_FONT_SIZE);
		}
	}

	private static final class AnimatedEffect {
		private MobEffectInstance effect;
		private float progress;
		private boolean target = true;

		private AnimatedEffect(MobEffectInstance effect) {
			this.effect = effect;
		}

		private MobEffectInstance effect() {
			return effect;
		}

		private float progress() {
			return progress;
		}
	}

	private static String effectName(MobEffectInstance effect) {
		String name = Component.translatable(effect.getDescriptionId()).getString();
		return effect.getAmplifier() <= 0 ? name : name + " " + roman(effect.getAmplifier() + 1);
	}

	private static String roman(int value) {
		return switch (value) {
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> Integer.toString(value);
		};
	}

	private static String duration(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return "INF";
		}
		int seconds = Math.max(0, (effect.getDuration() + 19) / 20);
		int hours = seconds / 3600;
		int minutes = seconds % 3600 / 60;
		int remainingSeconds = seconds % 60;
		return hours > 0
				? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds)
				: String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds);
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, int color) {
		float baseline = renderer.centeredBaseline(text, FONT_SIZE, centerY);
		renderer.draw(graphics, text, x, baseline, FONT_SIZE, color);
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, float size, int color) {
		float baseline = renderer.centeredBaseline(text, size, centerY);
		renderer.draw(graphics, text, x, baseline, size, color);
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
}
