package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.media.MediaCoverManager;
import dev.Mardexx.client.media.MediaInfo;
import dev.Mardexx.client.media.MediaSessionProvider;
import dev.Mardexx.client.media.LyricsProvider;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;

public final class MediaPlayer extends Module {
	private static final MediaPlayer INSTANCE = new MediaPlayer();
	private static final float WIDTH = 132.0F;
	private static final float HEIGHT = 46.0F;
	private static final float PADDING = 4.0F;
	private static final float COVER_SIZE = HEIGHT - PADDING * 2.0F;
	private static final float CONTENT_GAP = 5.0F;
	private static final float TITLE_HEIGHT = 13.0F;
	private static final float CORNER_RADIUS = 5.5F;
	private static final float INNER_RADIUS = 3.6F;
	private static final float SHADOW_SPREAD = 6.0F;
	private static final float TITLE_SIZE = 7.0F;
	private static final float TEXT_SIZE = 6.2F;
	private static final int BACKGROUND = 0xE80A0A0D;
	private static final int PANEL_BACKGROUND = 0xFF111115;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final long MEDIA_TEXT_REFRESH_NANOS = 100_000_000L;
	private static final LyricsProvider.LyricsDisplay PREVIEW_LYRICS =
			new LyricsProvider.LyricsDisplay("Previous line", "Current lyric", "Next line");
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private final MediaPlayerElement element = new MediaPlayerElement();
	private MsdfFontRenderer font;

	private MediaPlayer() {
		super("Media Player", ModuleCategory.RENDER, "Shows the current Windows media session with its cover, title and synchronized lyrics.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Media Player") : true;
	}

	public static MediaPlayer getInstance() {
		return INSTANCE;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private final class MediaPlayerElement extends HudElement {
		private MediaInfo rendered = MediaInfo.EMPTY;
		private LyricsProvider.LyricsDisplay displayedLyrics;
		private LyricsProvider.LyricsDisplay outgoingLyrics;
		private LyricsProvider.LyricsDisplay cachedLyrics;
		private MediaInfo coverSource = MediaInfo.EMPTY;
		private MediaInfo lyricsSource = MediaInfo.EMPTY;
		private float revealProgress;
		private float lyricsTransition = 1.0F;
		private long nextLyricsRefreshNanos;
		private long lastFrameNanos = System.nanoTime();

		private MediaPlayerElement() {
			super("media_player", 225.0F, 215.0F);
		}

		@Override
		public Module module() {
			return MediaPlayer.this;
		}

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			Minecraft minecraft = Minecraft.getInstance();
			float delta = frameDelta();
			MediaInfo current = MediaSessionProvider.INSTANCE.getCurrent();
			boolean hasMedia = current != null && current.displayable();
			boolean preview = !hasMedia;
			if (hasMedia) {
				rendered = current;
			}
			if (current != coverSource) {
				coverSource = current;
				MediaCoverManager.request(hasMedia ? current.coverBase64() : "");
			}

			float target = 1.0F;
			revealProgress = animate(revealProgress, target, 14.0F, delta);
			if (revealProgress <= 0.001F) {
				rendered = MediaInfo.EMPTY;
				return;
			}

			MsdfFontRenderer renderer = font();
			if (renderer == null) {
				return;
			}

			float alpha = clamp(revealProgress);
			String title = hasMedia ? rendered.displayTitle() : "Media Player";
			LyricsProvider.LyricsDisplay lyrics = lyrics(preview);
			updateLyricsTransition(lyrics, delta);
			float contentX = x() + PADDING + COVER_SIZE + CONTENT_GAP;
			float contentWidth = WIDTH - PADDING - (contentX - x());
			float bodyY = y() + PADDING + TITLE_HEIGHT + 3.0F;
			float bodyHeight = HEIGHT - PADDING - (bodyY - y());

			Render2D.softShadow(guiGraphics, x(), y(), WIDTH, HEIGHT, CORNER_RADIUS, SHADOW_SPREAD, HudManager.withHudAlpha(withAlpha(SHADOW_COLOR, alpha)));
			Render2D.roundedRect(guiGraphics, x(), y(), WIDTH, HEIGHT, CORNER_RADIUS, HudManager.withHudAlpha(withAlpha(BACKGROUND, alpha)));
			Render2D.roundedRect(guiGraphics, contentX, y() + PADDING, contentWidth, TITLE_HEIGHT, INNER_RADIUS, HudManager.withHudAlpha(withAlpha(PANEL_BACKGROUND, alpha)));
			Render2D.roundedRect(guiGraphics, contentX, bodyY, contentWidth, bodyHeight, INNER_RADIUS, HudManager.withHudAlpha(withAlpha(PANEL_BACKGROUND, alpha)));
			renderCover(guiGraphics, preview, alpha);

			float textX = contentX + 4.0F;
			float textWidth = contentWidth - 8.0F;
			draw(renderer, guiGraphics, fit(renderer, title, TITLE_SIZE, textWidth), textX,
					y() + PADDING + TITLE_HEIGHT * 0.5F, TITLE_SIZE, withAlpha(0xFFF4F4F6, alpha), 0.035F);
			renderLyrics(renderer, guiGraphics, textX, textWidth, bodyY, bodyHeight, alpha);
		}

		private LyricsProvider.LyricsDisplay lyrics(boolean preview) {
			if (preview) {
				return PREVIEW_LYRICS;
			}

			long now = System.nanoTime();
			if (cachedLyrics == null || rendered != lyricsSource || now >= nextLyricsRefreshNanos) {
				lyricsSource = rendered;
				nextLyricsRefreshNanos = now + MEDIA_TEXT_REFRESH_NANOS;
				cachedLyrics = LyricsProvider.INSTANCE.display(
						rendered,
						MediaSessionProvider.INSTANCE.positionMillis(rendered)
				);
			}
			return cachedLyrics;
		}

		private void updateLyricsTransition(LyricsProvider.LyricsDisplay lyrics, float delta) {
			if (displayedLyrics == null) {
				displayedLyrics = lyrics;
				lyricsTransition = 1.0F;
				return;
			}
			if (!displayedLyrics.equals(lyrics)) {
				outgoingLyrics = displayedLyrics;
				displayedLyrics = lyrics;
				lyricsTransition = 0.0F;
			}
			lyricsTransition = Math.min(1.0F, lyricsTransition + delta / 0.20F);
			if (lyricsTransition >= 1.0F) {
				outgoingLyrics = null;
			}
		}

		private void renderLyrics(MsdfFontRenderer renderer, GuiGraphics guiGraphics,
				float textX, float textWidth, float bodyY, float bodyHeight, float alpha) {
			if (displayedLyrics == null) {
				return;
			}
			float progress = easeOutCubic(lyricsTransition);
			float rowDistance = bodyHeight * 0.30F;
			guiGraphics.enableScissor(
					Math.round(textX),
					Math.round(bodyY),
					Math.round(textX + textWidth),
					Math.round(bodyY + bodyHeight)
			);
			if (outgoingLyrics != null && progress < 1.0F) {
				renderLyricsRows(renderer, guiGraphics, outgoingLyrics, textX, textWidth, bodyY - rowDistance * progress,
						bodyHeight, alpha * (1.0F - progress));
			}
			renderLyricsRows(renderer, guiGraphics, displayedLyrics, textX, textWidth,
					bodyY + rowDistance * (1.0F - progress), bodyHeight, alpha * progress);
			MsdfFontRenderer.flushPending();
			guiGraphics.disableScissor();
		}

		private void renderLyricsRows(MsdfFontRenderer renderer, GuiGraphics guiGraphics, LyricsProvider.LyricsDisplay lyrics,
				float textX, float textWidth, float bodyY, float bodyHeight, float alpha) {
			String previous = lyrics.previousLine();
			String current = lyrics.currentLine();
			String next = lyrics.nextLine();
			draw(renderer, guiGraphics, fit(renderer, previous, TEXT_SIZE, textWidth), textX,
					bodyY + bodyHeight * 0.20F, TEXT_SIZE, withAlpha(0xFF777780, alpha), 0.01F);
			draw(renderer, guiGraphics, fit(renderer, current, TEXT_SIZE, textWidth), textX,
					bodyY + bodyHeight * 0.50F, TEXT_SIZE, withAlpha(ThemeManager.accent(), alpha), 0.025F);
			draw(renderer, guiGraphics, fit(renderer, next, TEXT_SIZE, textWidth), textX,
					bodyY + bodyHeight * 0.80F, TEXT_SIZE, withAlpha(0xFF888891, alpha), 0.01F);
		}

		private void renderCover(GuiGraphics guiGraphics, boolean preview, float alpha) {
			float coverX = x() + PADDING;
			float coverY = y() + PADDING;
			Render2D.roundedRect(guiGraphics, coverX, coverY, COVER_SIZE, COVER_SIZE, INNER_RADIUS,
					withAlpha(0xFF151519, alpha));
			ResourceLocation cover = preview ? null : MediaCoverManager.textureId();
			if (cover != null) {
				Render2D.texturedQuad(guiGraphics, cover, coverX, coverY, COVER_SIZE, COVER_SIZE,
						0.0F, 0.0F, 1.0F, 1.0F, withAlpha(0xFFFFFFFF, alpha));
				return;
			}

			int accent = withAlpha(ThemeManager.accent(), alpha);
			float discSize = 19.0F;
			float discX = coverX + (COVER_SIZE - discSize) * 0.5F;
			float discY = coverY + (COVER_SIZE - discSize) * 0.5F;
			Render2D.roundedRect(guiGraphics, discX, discY, discSize, discSize, discSize * 0.5F, accent);
			Render2D.roundedRect(guiGraphics, discX + 7.0F, discY + 7.0F, 5.0F, 5.0F, 2.5F,
					withAlpha(0xFF151519, alpha));
		}

		@Override
		public float width() {
			return WIDTH;
		}

		@Override
		public float height() {
			return HEIGHT;
		}

		@Override
		public boolean visible() {
			if (Interface.getInstance() == null || !Interface.getInstance().isWidgetEnabled("Media Player")) {
				return false;
			}
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.screen instanceof ChatScreen) {
				return true;
			}
			return (MediaSessionProvider.INSTANCE.getCurrent().displayable() || revealProgress > 0.001F);
		}

		private float frameDelta() {
			long now = System.nanoTime();
			float delta = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
			lastFrameNanos = now;
			return delta;
		}
	}

	private static String fit(MsdfFontRenderer renderer, String text, float size, float maxWidth) {
		String value = text == null ? "" : text.strip();
		if (value.isBlank()) {
			return "";
		}
		if (renderer.width(value, size) <= maxWidth) {
			return value;
		}
		String suffix = "...";
		for (int length = value.length() - 1; length > 0; length--) {
			String shortened = value.substring(0, length).stripTrailing() + suffix;
			if (renderer.width(shortened, size) <= maxWidth) {
				return shortened;
			}
		}
		return suffix;
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY,
			float size, int color, float thickness) {
		renderer.draw(graphics, text, x, renderer.centeredBaseline(text, size, centerY), size, color, 0.0F, thickness);
	}

	private static float animate(float current, float target, float speed, float deltaSeconds) {
		float value = current + (target - current) * (1.0F - (float) Math.exp(-speed * deltaSeconds));
		return Math.abs(value - target) < 0.001F ? target : value;
	}

	private static float easeOutCubic(float value) {
		float inverse = 1.0F - clamp(value);
		return 1.0F - inverse * inverse * inverse;
	}

	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static int withAlpha(int color, float multiplier) {
		int alpha = Math.round((color >>> 24) * clamp(multiplier));
		return alpha << 24 | color & 0x00FFFFFF;
	}
}
