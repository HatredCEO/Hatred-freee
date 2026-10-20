package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.utils.other.TPSTracker;
import dev.Mardexx.client.discord.DiscordManager;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class Watermark extends Module {
	private static final Watermark INSTANCE = new Watermark();
	private static final float UI_SCALE = 1.3915F;
	private static final float FONT_SIZE = 6.2F * UI_SCALE;
	private static final float HEIGHT = 8.4F * UI_SCALE;
	private static final float HORIZONTAL_PADDING = 2.4F * UI_SCALE;
	private static final float SEGMENT_GAP = 2.8F * UI_SCALE;
	private static final float ICON_TEXT_GAP = 0.48F * UI_SCALE;
	private static final float CORNER_RADIUS = 4.0F;
	private static final float SHADOW_SPREAD = 4.5F;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final int BACKGROUND_COLOR = 0xE80A0A0D;

	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private static final ResourceLocation LOGO_H_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_logo_h/font.png");
	private static final ResourceLocation LOGO_H_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_logo_h/font.json");

	private static final ResourceLocation ICON_LAPTOP_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_icon_laptop/font.png");
	private static final ResourceLocation ICON_LAPTOP_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_icon_laptop/font.json");

	private static final ResourceLocation ICON_WIFI_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_icon_wifi/font.png");
	private static final ResourceLocation ICON_WIFI_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_icon_wifi/font.json");

	private static final ResourceLocation ICON_TPS_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_icon_tps/font.png");
	private static final ResourceLocation ICON_TPS_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/custom_icon_tps/font.json");

	private static MsdfFontRenderer logoHFont;
	private static MsdfFontRenderer laptopIconFont;
	private static MsdfFontRenderer wifiIconFont;
	private static MsdfFontRenderer tpsIconFont;

	private final BooleanSetting showFps = new BooleanSetting("show_fps", "FPS", "Shows current client FPS.", true);
	private final BooleanSetting showPing = new BooleanSetting("show_ping", "Ping", "Shows current server latency.", true);
	private final BooleanSetting showTps = new BooleanSetting("show_tps", "TPS", "Shows estimated server TPS.", true);
	private final WatermarkElement element = new WatermarkElement();
	private MsdfFontRenderer font;

	private Watermark() {
		super("Watermark", ModuleCategory.RENDER, "Displays the Hatred Watermark with player info and performance stats.", true);
		register(showFps, showPing, showTps);
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Watermark") : true;
	}

	public static Watermark getInstance() {
		return INSTANCE;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private static MsdfFontRenderer logoHFont() {
		if (logoHFont == null && Minecraft.getInstance().getResourceManager() != null) {
			logoHFont = new MsdfFontRenderer(LOGO_H_TEXTURE, LOGO_H_DATA);
		}
		return logoHFont;
	}

	private static MsdfFontRenderer laptopIconFont() {
		if (laptopIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			laptopIconFont = new MsdfFontRenderer(ICON_LAPTOP_TEXTURE, ICON_LAPTOP_DATA);
		}
		return laptopIconFont;
	}

	private static MsdfFontRenderer wifiIconFont() {
		if (wifiIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			wifiIconFont = new MsdfFontRenderer(ICON_WIFI_TEXTURE, ICON_WIFI_DATA);
		}
		return wifiIconFont;
	}

	private static MsdfFontRenderer tpsIconFont() {
		if (tpsIconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			tpsIconFont = new MsdfFontRenderer(ICON_TPS_TEXTURE, ICON_TPS_DATA);
		}
		return tpsIconFont;
	}

	private final class WatermarkElement extends HudElement {
		private float fpsAnimation = showFps.enabled() ? 1.0F : 0.0F;
		private float pingAnimation = showPing.enabled() ? 1.0F : 0.0F;
		private float tpsAnimation = showTps.enabled() ? 1.0F : 0.0F;
		private float animatedWidth = 85.0F * UI_SCALE;
		private String username = "Player";

		private WatermarkElement() {
			super("watermark", 6.0F, 6.0F);
		}

		@Override
		public Module module() {
			return Watermark.this;
		}

		private String fpsText = "0 FPS";
		private String pingText = "0 ms";
		private String tpsText = "20.0 TPS";
		private float userWidth;
		private float fpsWidth;
		private float pingWidth;
		private float tpsWidth;
		private long nextInformationRefreshNanos;
		private long lastAnimationNanos = System.nanoTime();

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			MsdfFontRenderer renderer = font();
			Minecraft minecraft = Minecraft.getInstance();
			if (renderer == null) {
				return;
			}

			updateAnimations();
			updateInformation(renderer);

			float iconSize = FONT_SIZE * 1.035F;
			float pingIconSize = FONT_SIZE * 1.035F;
			float logoSize = FONT_SIZE * 1.55F;
			float avatarSize = FONT_SIZE * 1.09F;

			MsdfFontRenderer logoRenderer = logoHFont();
			MsdfFontRenderer laptopRenderer = laptopIconFont();
			MsdfFontRenderer wifiRenderer = wifiIconFont();
			MsdfFontRenderer tpsRenderer = tpsIconFont();

			float logoWidth = logoRenderer != null ? logoRenderer.width("h", logoSize) : logoSize;
			float laptopWidth = laptopRenderer != null ? laptopRenderer.width("l", iconSize) : iconSize;
			float wifiWidth = wifiRenderer != null ? wifiRenderer.width("w", pingIconSize) : pingIconSize;
			float tpsIconWidth = tpsRenderer != null ? tpsRenderer.width("t", iconSize) : iconSize;

			float targetWidth = HORIZONTAL_PADDING * 2.0F + logoWidth + SEGMENT_GAP + avatarSize + ICON_TEXT_GAP + userWidth;
			if (fpsAnimation > 0.001F) {
				targetWidth += (SEGMENT_GAP + laptopWidth + ICON_TEXT_GAP + fpsWidth) * fpsAnimation;
			}
			if (pingAnimation > 0.001F) {
				targetWidth += (SEGMENT_GAP + wifiWidth + ICON_TEXT_GAP + pingWidth) * pingAnimation;
			}
			if (tpsAnimation > 0.001F) {
				targetWidth += (SEGMENT_GAP + tpsIconWidth + ICON_TEXT_GAP + tpsWidth) * tpsAnimation;
			}

			animatedWidth = animate(animatedWidth, targetWidth, 18.0F, frameDeltaSeconds());
			float width = animatedWidth;
			int accent = ThemeManager.accent();

			Render2D.softShadow(guiGraphics, x(), y(), width, HEIGHT, CORNER_RADIUS, SHADOW_SPREAD, HudManager.withHudAlpha(SHADOW_COLOR));
			Render2D.roundedRect(guiGraphics, x(), y(), width, HEIGHT, CORNER_RADIUS, HudManager.withHudAlpha(BACKGROUND_COLOR));

			float currentX = x() + HORIZONTAL_PADDING;
			float centerY = y() + HEIGHT * 0.5F;

			// 1. "H" Logo in Accent Color
			if (logoRenderer != null) {
				float baseline = logoRenderer.centeredBaseline("h", logoSize, centerY);
				logoRenderer.draw(guiGraphics, "h", currentX, baseline, logoSize, accent);
			}
			currentX += logoWidth + SEGMENT_GAP;

			// 2. User Avatar Circle & Discord Name in White
			float avatarY = centerY - avatarSize * 0.5F;
			DiscordManager discord = DiscordManager.getInstance();
			if (discord.hasAvatar()) {
				Render2D.roundedRect(guiGraphics, currentX, avatarY, avatarSize, avatarSize, avatarSize * 0.5F, 0xFF202025);
				ResourceLocation avatarLoc = discord.getAvatarLocation();
				if (avatarLoc != null) {
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					guiGraphics.blit(RenderType::guiTextured, avatarLoc, Math.round(currentX), Math.round(avatarY), 0.0F, 0.0F, Math.round(avatarSize), Math.round(avatarSize), Math.round(avatarSize), Math.round(avatarSize));
				}
			} else if (minecraft.player != null) {
				Render2D.roundedRect(guiGraphics, currentX, avatarY, avatarSize, avatarSize, avatarSize * 0.5F, 0xFF202025);
				PlayerFaceRenderer.draw(guiGraphics, minecraft.player.getSkin(), Math.round(currentX), Math.round(avatarY), Math.round(avatarSize));
			} else {
				Render2D.roundedRect(guiGraphics, currentX, avatarY, avatarSize, avatarSize, avatarSize * 0.5F, accent);
			}
			currentX += avatarSize + ICON_TEXT_GAP;

			float userBaseline = renderer.centeredBaseline(username, FONT_SIZE, centerY);
			renderer.draw(guiGraphics, username, currentX, userBaseline, FONT_SIZE, 0xFFFFFFFF);
			currentX += userWidth;

			// 3. FPS Segment
			if (fpsAnimation > 0.001F) {
				currentX += SEGMENT_GAP * fpsAnimation;
				if (laptopRenderer != null) {
					float baseline = laptopRenderer.centeredBaseline("l", iconSize, centerY);
					laptopRenderer.draw(guiGraphics, "l", currentX, baseline, iconSize, withAlpha(accent, fpsAnimation));
				}
				currentX += (laptopWidth + ICON_TEXT_GAP) * fpsAnimation;
				float textBaseline = renderer.centeredBaseline(fpsText, FONT_SIZE, centerY);
				renderer.draw(guiGraphics, fpsText, currentX, textBaseline, FONT_SIZE, withAlpha(0xFFFFFFFF, fpsAnimation));
				currentX += fpsWidth * fpsAnimation;
			}

			// 4. Ping Segment
			if (pingAnimation > 0.001F) {
				currentX += SEGMENT_GAP * pingAnimation;
				if (wifiRenderer != null) {
					float baseline = wifiRenderer.centeredBaseline("w", pingIconSize, centerY);
					wifiRenderer.draw(guiGraphics, "w", currentX, baseline, pingIconSize, withAlpha(accent, pingAnimation));
				}
				currentX += (wifiWidth + ICON_TEXT_GAP) * pingAnimation;
				float textBaseline = renderer.centeredBaseline(pingText, FONT_SIZE, centerY);
				renderer.draw(guiGraphics, pingText, currentX, textBaseline, FONT_SIZE, withAlpha(0xFFFFFFFF, pingAnimation));
				currentX += pingWidth * pingAnimation;
			}

			// 5. TPS Segment
			if (tpsAnimation > 0.001F) {
				currentX += SEGMENT_GAP * tpsAnimation;
				if (tpsRenderer != null) {
					float baseline = tpsRenderer.centeredBaseline("t", iconSize, centerY);
					tpsRenderer.draw(guiGraphics, "t", currentX, baseline, iconSize, withAlpha(accent, tpsAnimation));
				}
				currentX += (tpsIconWidth + ICON_TEXT_GAP) * tpsAnimation;
				float textBaseline = renderer.centeredBaseline(tpsText, FONT_SIZE, centerY);
				renderer.draw(guiGraphics, tpsText, currentX, textBaseline, FONT_SIZE, withAlpha(0xFFFFFFFF, tpsAnimation));
			}

			MsdfFontRenderer.flushPending();
		}

		private void updateInformation(MsdfFontRenderer renderer) {
			long now = System.nanoTime();
			if (now < nextInformationRefreshNanos) {
				return;
			}
			nextInformationRefreshNanos = now + 100_000_000L;
			Minecraft minecraft = Minecraft.getInstance();
			TPSTracker tracker = TPSTracker.getInstance();
			username = DiscordManager.getInstance().getUsername();
			fpsText = minecraft.getFps() + " FPS";
			pingText = tracker.getPing() + " ms";
			float roundedTps = Math.round(tracker.getTps() * 10.0F) / 10.0F;
			tpsText = roundedTps + " TPS";

			userWidth = renderer.width(username, FONT_SIZE);
			fpsWidth = renderer.width(fpsText, FONT_SIZE);
			pingWidth = renderer.width(pingText, FONT_SIZE);
			tpsWidth = renderer.width(tpsText, FONT_SIZE);
		}

		@Override
		public float width() {
			return animatedWidth;
		}

		@Override
		public float height() {
			return HEIGHT;
		}

		@Override
		public boolean visible() {
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Watermark");
		}

		private void updateAnimations() {
			float deltaSeconds = animationDeltaSeconds();
			fpsAnimation = animate(fpsAnimation, showFps.enabled() ? 1.0F : 0.0F, 15.0F, deltaSeconds);
			pingAnimation = animate(pingAnimation, showPing.enabled() ? 1.0F : 0.0F, 15.0F, deltaSeconds);
			tpsAnimation = animate(tpsAnimation, showTps.enabled() ? 1.0F : 0.0F, 15.0F, deltaSeconds);
		}

		private float animationDeltaSeconds() {
			long now = System.nanoTime();
			float deltaSeconds = Math.min(0.05F, Math.max(0.0F, (now - lastAnimationNanos) / 1_000_000_000.0F));
			lastAnimationNanos = now;
			return deltaSeconds;
		}

		private float frameDeltaSeconds() {
			return Math.min(0.05F, 1.0F / Math.max(20.0F, Minecraft.getInstance().getFps()));
		}
	}

	private static float animate(float current, float target, float speed, float deltaSeconds) {
		float value = current + (target - current) * (1.0F - (float) Math.exp(-speed * deltaSeconds));
		return Math.abs(value - target) < 0.001F ? target : value;
	}

	private static int withAlpha(int color, float multiplier) {
		int alpha = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, multiplier)));
		return alpha << 24 | color & 0x00FFFFFF;
	}
}
