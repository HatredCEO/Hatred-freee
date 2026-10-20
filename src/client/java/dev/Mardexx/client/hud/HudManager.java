package dev.Mardexx.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.modules.ModuleManager;
import dev.Mardexx.client.theme.ThemeManager;
import dev.Mardexx.client.config.ClientConfig;
import dev.Mardexx.client.modules.impl.Render.Interface;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudManager {
	private static final ResourceLocation INTER_FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation INTER_FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");
	private static final ResourceLocation BOTTOM_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/bottom_icons/font.png");
	private static final ResourceLocation BOTTOM_ICON_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/bottom_icons/font.json");
	private static final String SETTINGS_ICON = "\uE103";

	private static MsdfFontRenderer interFont;
	private static MsdfFontRenderer iconFont;

	private static final List<HudElement> ELEMENTS = new ArrayList<>();
	private static final Map<String, Position> SAVED_POSITIONS = new LinkedHashMap<>();
	private static HudElement activeElement;
	private static boolean initialized;

	private static boolean panelOpen;
	private static float panelAnimation = 0.0F;
	private static long lastAnimationNanos = System.nanoTime();

	private enum SliderDrag { NONE, SCALE, TRANSPARENCY }
	private static SliderDrag draggingSlider = SliderDrag.NONE;

	private static float transparency = 1.0F;
	private static float scale = 1.0F;
	private static boolean renderingHud;

	private static final String[] WIDGET_NAMES = {
			"Watermark",
			"Active Modules",
			"Keybinds",
			"Potion List",
			"Cooldowns",
			"Target HUD",
			"Notifications",
			"Media Player",
			"Helper HUD"
	};

	private HudManager() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		dev.Mardexx.client.modules.impl.Render.Watermark.getInstance();
		dev.Mardexx.client.modules.impl.Render.ArrayListHud.getInstance();
		dev.Mardexx.client.modules.impl.Render.Keybinds.getInstance();
		dev.Mardexx.client.modules.impl.Render.PotionList.getInstance();
		dev.Mardexx.client.modules.impl.Render.Cooldowns.getInstance();
		dev.Mardexx.client.modules.impl.Render.TargetHud.getInstance();
		dev.Mardexx.client.modules.impl.Render.Notifications.getInstance();
		dev.Mardexx.client.modules.impl.Render.MediaPlayer.getInstance();
		dev.Mardexx.client.modules.impl.Render.HelperHud.getInstance();
		Render2D.add(HudManager::render);
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof ChatScreen)) {
				return;
			}

			ScreenMouseEvents.allowMouseClick(screen).register((ignored, mouseX, mouseY, button) ->
					!mouseClicked(mouseX, mouseY, button));
			ScreenMouseEvents.allowMouseRelease(screen).register((ignored, mouseX, mouseY, button) ->
					!mouseReleased(button));
			ScreenEvents.remove(screen).register(ignored -> {
				stopDragging(true);
				draggingSlider = SliderDrag.NONE;
			});
		});
	}

	public static <T extends HudElement> T register(T element) {
		for (HudElement existing : ELEMENTS) {
			if (existing.id().equals(element.id())) {
				throw new IllegalArgumentException("Duplicate HUD element id: " + element.id());
			}
		}

		Position saved = SAVED_POSITIONS.get(element.id());
		if (saved != null) {
			element.setPosition(saved.x(), saved.y());
		}
		ELEMENTS.add(element);
		return element;
	}

	public static void unregister(HudElement element) {
		if (activeElement == element) {
			stopDragging(false);
		}
		ELEMENTS.remove(element);
	}

	public static List<HudElement> elements() {
		return Collections.unmodifiableList(ELEMENTS);
	}

	public static boolean isRenderingHud() {
		return renderingHud;
	}

	public static void setRenderingHud(boolean renderingHud) {
		HudManager.renderingHud = renderingHud;
	}

	public static float transparency() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.transparency().floatValue() : transparency;
	}

	public static void setTransparency(float transparency) {
		HudManager.transparency = Math.max(0.0F, Math.min(1.0F, transparency));
		Interface iface = Interface.getInstance();
		if (iface != null) {
			iface.transparency().setValue(HudManager.transparency);
		}
	}

	public static float scale() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.scale().floatValue() : scale;
	}

	public static void setScale(float scale) {
		HudManager.scale = Math.max(0.5F, Math.min(1.5F, scale));
		Interface iface = Interface.getInstance();
		if (iface != null) {
			iface.scale().setValue(HudManager.scale);
		}
	}

	public static int withHudAlpha(int color) {
		float currentTrans = transparency();
		if (currentTrans >= 0.999F) {
			return color;
		}
		int alpha = (color >>> 24) & 0xFF;
		int modifiedAlpha = Math.round(alpha * currentTrans);
		return (modifiedAlpha << 24) | (color & 0x00FFFFFF);
	}

	public static JsonObject save() {
		JsonObject hud = new JsonObject();
		hud.addProperty("transparency", transparency());
		hud.addProperty("scale", scale());
		for (Map.Entry<String, Position> entry : SAVED_POSITIONS.entrySet()) {
			hud.add(entry.getKey(), positionObject(entry.getValue()));
		}
		for (HudElement element : ELEMENTS) {
			hud.add(element.id(), positionObject(new Position(element.x(), element.y())));
		}
		return hud;
	}

	public static void load(JsonObject hud) {
		SAVED_POSITIONS.clear();
		if (hud == null) {
			return;
		}

		if (number(hud, "transparency")) {
			setTransparency(hud.get("transparency").getAsFloat());
		}
		if (number(hud, "scale")) {
			setScale(hud.get("scale").getAsFloat());
		}

		for (Map.Entry<String, JsonElement> entry : hud.entrySet()) {
			if (entry.getKey().equals("headerLayouts")) {
				continue;
			}
			JsonElement value = entry.getValue();
			if (!value.isJsonObject()) {
				continue;
			}

			JsonObject object = value.getAsJsonObject();
			if (!number(object, "x") || !number(object, "y")) {
				continue;
			}

			Position position = new Position(object.get("x").getAsFloat(), object.get("y").getAsFloat());
			SAVED_POSITIONS.put(entry.getKey(), position);
			for (HudElement element : ELEMENTS) {
				if (element.id().equals(entry.getKey())) {
					element.setPosition(position.x(), position.y());
					break;
				}
			}
		}
	}

	private static void render(GuiGraphics guiGraphics) {
		Minecraft minecraft = Minecraft.getInstance();
		boolean editing = minecraft.screen instanceof ChatScreen;
		updateAnimations();

		if (!editing && activeElement != null) {
			stopDragging(true);
		}

		double mouseX = 0.0;
		double mouseY = 0.0;
		if (editing) {
			double guiScale = Math.max(0.01, minecraft.getWindow().getGuiScale());
			mouseX = minecraft.mouseHandler.xpos() / guiScale;
			mouseY = minecraft.mouseHandler.ypos() / guiScale;

			if (draggingSlider == SliderDrag.SCALE) {
				updateScaleFromMouse(mouseX, 32.0F + 8.0F, 142.0F - 16.0F);
			} else if (draggingSlider == SliderDrag.TRANSPARENCY) {
				updateTransparencyFromMouse(mouseX, 32.0F + 8.0F, 142.0F - 16.0F);
			}

			if (activeElement != null) {
				activeElement.updateDrag(
						mouseX,
						mouseY,
						minecraft.getWindow().getGuiScaledWidth(),
						minecraft.getWindow().getGuiScaledHeight()
				);
			}
		}

		renderingHud = true;
		MsdfFontRenderer.beginBatching();
		try {
			for (HudElement element : ELEMENTS) {
				element.updateEditorFocus(editing && element.dragging());
				element.draw(guiGraphics, Render2D.tickDelta());
			}
			if (editing) {
				renderingHud = false;
				renderGearButton(guiGraphics, mouseX, mouseY);
				if (panelOpen || panelAnimation > 0.001F) {
					renderSettingsPanel(guiGraphics, mouseX, mouseY);
				}
				renderingHud = true;
			}
		} finally {
			MsdfFontRenderer.endBatching();
			renderingHud = false;
		}
	}

	private static void renderGearButton(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		float btnX = 8.0F;
		float btnY = 8.0F;
		float btnSize = 20.0F;
		boolean hovered = inside(mouseX, mouseY, btnX, btnY, btnSize, btnSize);

		Render2D.softShadow(guiGraphics, btnX, btnY, btnSize, btnSize, 5.0F, 4.0F, 0x60000000);
		Render2D.roundedRect(guiGraphics, btnX, btnY, btnSize, btnSize, 5.0F, hovered || panelOpen ? 0x44FFFFFF : 0x22FFFFFF);
		Render2D.roundedRect(guiGraphics, btnX + 1.0F, btnY + 1.0F, btnSize - 2.0F, btnSize - 2.0F, 4.0F, 0xE80E0E12);

		MsdfFontRenderer iconRenderer = iconFont();
		if (iconRenderer != null) {
			int iconColor = panelOpen || hovered ? ThemeManager.accent() : 0xFFCCCCCC;
			float iconSize = 8.0F;
			float iconCenterX = btnX + btnSize / 2.0F;
			float iconCenterY = btnY + btnSize / 2.0F;
			float iconX = iconRenderer.centeredX(SETTINGS_ICON, iconSize, iconCenterX);
			float iconBaseline = iconRenderer.centeredBaseline(SETTINGS_ICON, iconSize, iconCenterY);
			iconRenderer.draw(guiGraphics, SETTINGS_ICON, iconX, iconBaseline, iconSize, iconColor);
		}
	}

	private static void renderSettingsPanel(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		MsdfFontRenderer font = interFont();
		if (font == null) {
			return;
		}

		float progress = easeOutCubic(panelAnimation);
		float panelX = 32.0F;
		float panelY = 8.0F;
		float width = 142.0F;
		float height = 232.0F;

		float visibleWidth = width * progress;
		int accent = ThemeManager.accent();

		Render2D.softShadow(guiGraphics, panelX, panelY, visibleWidth, height, 6.0F, 6.0F, 0x80000000);
		Render2D.roundedRect(guiGraphics, panelX, panelY, visibleWidth, height, 6.0F, 0x33FFFFFF);
		Render2D.roundedRect(guiGraphics, panelX + 1.0F, panelY + 1.0F, Math.max(0.0F, visibleWidth - 2.0F), height - 2.0F, 5.0F, 0xF20E0E14);

		if (visibleWidth < width * 0.4F) {
			return;
		}

		guiGraphics.enableScissor(Math.round(panelX), Math.round(panelY), Math.round(panelX + visibleWidth), Math.round(panelY + height));
		try {
			// Title
			drawText(font, guiGraphics, "HUD SETTINGS", panelX + 8.0F, panelY + 11.0F, 6.5F, accent, 0.04F);

			// Scale Slider
			float scaleRowY = panelY + 22.0F;
			drawText(font, guiGraphics, "Scale", panelX + 8.0F, scaleRowY + 5.0F, 5.8F, 0xFFCCCCCC, 0.02F);
			String scaleStr = Math.round(scale * 100.0F) + "%";
			float scaleStrW = font.width(scaleStr, 5.8F);
			drawText(font, guiGraphics, scaleStr, panelX + width - 8.0F - scaleStrW, scaleRowY + 5.0F, 5.8F, accent, 0.02F);

			float sliderTrackX = panelX + 8.0F;
			float sliderTrackW = width - 16.0F;
			float sliderTrackY = scaleRowY + 13.0F;
			float sliderTrackH = 3.5F;
			float scaleProgress = Math.max(0.0F, Math.min(1.0F, (scale - 0.5F) / 1.0F));

			Render2D.roundedRect(guiGraphics, sliderTrackX, sliderTrackY, sliderTrackW, sliderTrackH, 1.75F, 0xFF2A2A34);
			Render2D.roundedRect(guiGraphics, sliderTrackX, sliderTrackY, sliderTrackW * scaleProgress, sliderTrackH, 1.75F, accent);
			float thumbRadius = 3.5F;
			Render2D.roundedRect(guiGraphics, sliderTrackX + sliderTrackW * scaleProgress - thumbRadius, sliderTrackY + sliderTrackH / 2.0F - thumbRadius, thumbRadius * 2.0F, thumbRadius * 2.0F, thumbRadius, 0xFFFFFFFF);

			// Transparency Slider
			float transRowY = panelY + 44.0F;
			drawText(font, guiGraphics, "Transparency", panelX + 8.0F, transRowY + 5.0F, 5.8F, 0xFFCCCCCC, 0.02F);
			String transStr = Math.round(transparency * 100.0F) + "%";
			float transStrW = font.width(transStr, 5.8F);
			drawText(font, guiGraphics, transStr, panelX + width - 8.0F - transStrW, transRowY + 5.0F, 5.8F, accent, 0.02F);

			float transTrackY = transRowY + 13.0F;
			float transProgress = Math.max(0.0F, Math.min(1.0F, transparency));

			Render2D.roundedRect(guiGraphics, sliderTrackX, transTrackY, sliderTrackW, sliderTrackH, 1.75F, 0xFF2A2A34);
			Render2D.roundedRect(guiGraphics, sliderTrackX, transTrackY, sliderTrackW * transProgress, sliderTrackH, 1.75F, accent);
			Render2D.roundedRect(guiGraphics, sliderTrackX + sliderTrackW * transProgress - thumbRadius, transTrackY + sliderTrackH / 2.0F - thumbRadius, thumbRadius * 2.0F, thumbRadius * 2.0F, thumbRadius, 0xFFFFFFFF);

			// Separator
			float sepY = panelY + 68.0F;
			Render2D.roundedRect(guiGraphics, panelX + 8.0F, sepY, width - 16.0F, 1.0F, 0.5F, 0x22FFFFFF);
			drawText(font, guiGraphics, "WIDGETS", panelX + 8.0F, sepY + 8.0F, 5.0F, 0xFF777782, 0.03F);

			// Widgets list
			float rowY = sepY + 16.0F;
			float rowH = 15.0F;
			Interface iface = Interface.getInstance();

			for (String widgetName : WIDGET_NAMES) {
				boolean widgetEnabled = iface != null && iface.isWidgetEnabled(widgetName);
				boolean hovered = inside(mouseX, mouseY, panelX + 6.0F, rowY, width - 12.0F, rowH);

				if (hovered) {
					Render2D.roundedRect(guiGraphics, panelX + 6.0F, rowY, width - 12.0F, rowH, 3.0F, 0x1AFFFFFF);
				}

				drawText(font, guiGraphics, widgetName, panelX + 9.0F, rowY + rowH / 2.0F, 5.7F, widgetEnabled ? 0xFFEEEEEE : 0xFF888894, 0.02F);

				// Toggle Switch
				float toggleW = 16.0F;
				float toggleH = 8.5F;
				float toggleX = panelX + width - 8.0F - toggleW;
				float toggleY = rowY + (rowH - toggleH) / 2.0F;
				int trackBg = widgetEnabled ? accent : 0xFF282832;
				Render2D.roundedRect(guiGraphics, toggleX, toggleY, toggleW, toggleH, toggleH / 2.0F, trackBg);
				float knobSize = toggleH - 2.0F;
				float knobX = widgetEnabled ? toggleX + toggleW - knobSize - 1.0F : toggleX + 1.0F;
				Render2D.roundedRect(guiGraphics, knobX, toggleY + 1.0F, knobSize, knobSize, knobSize / 2.0F, 0xFFFFFFFF);

				rowY += rowH;
			}
		} finally {
			guiGraphics.disableScissor();
		}
	}

	private static void updateScaleFromMouse(double mouseX, float sliderX, float sliderWidth) {
		float progress = (float) Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / sliderWidth));
		float newScale = 0.5F + progress * 1.0F;
		newScale = Math.round(newScale * 20.0F) / 20.0F;
		setScale(newScale);
		Interface iface = Interface.getInstance();
		if (iface != null) {
			iface.scale().setValue(newScale);
		}
	}

	private static void updateTransparencyFromMouse(double mouseX, float sliderX, float sliderWidth) {
		float progress = (float) Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / sliderWidth));
		float newTrans = Math.round(progress * 20.0F) / 20.0F;
		setTransparency(newTrans);
		Interface iface = Interface.getInstance();
		if (iface != null) {
			iface.transparency().setValue(newTrans);
		}
	}

	private static void toggleWidget(String widgetName) {
		Interface iface = Interface.getInstance();
		if (iface == null) {
			return;
		}
		switch (widgetName) {
			case "Watermark" -> iface.watermark().setValue(!iface.watermark().enabled());
			case "Active Modules" -> iface.activeModules().setValue(!iface.activeModules().enabled());
			case "Keybinds" -> iface.keybinds().setValue(!iface.keybinds().enabled());
			case "Potion List" -> iface.potionList().setValue(!iface.potionList().enabled());
			case "Cooldowns" -> iface.cooldowns().setValue(!iface.cooldowns().enabled());
			case "Target HUD" -> iface.targetHud().setValue(!iface.targetHud().enabled());
			case "Notifications" -> iface.notifications().setValue(!iface.notifications().enabled());
			case "Media Player" -> iface.mediaPlayer().setValue(!iface.mediaPlayer().enabled());
			case "Helper HUD" -> iface.helperHud().setValue(!iface.helperHud().enabled());
		}
	}

	private static boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}

		// Gear button (8, 8, 20, 20)
		if (inside(mouseX, mouseY, 8.0F, 8.0F, 20.0F, 20.0F)) {
			panelOpen = !panelOpen;
			return true;
		}

		if (panelOpen) {
			float panelX = 32.0F;
			float panelY = 8.0F;
			float width = 142.0F;
			float height = 232.0F;

			if (inside(mouseX, mouseY, panelX, panelY, width, height)) {
				// Scale Slider
				float sliderTrackX = panelX + 8.0F;
				float sliderTrackW = width - 16.0F;
				if (inside(mouseX, mouseY, panelX + 6.0F, panelY + 22.0F, width - 12.0F, 20.0F)) {
					draggingSlider = SliderDrag.SCALE;
					updateScaleFromMouse(mouseX, sliderTrackX, sliderTrackW);
					return true;
				}

				// Transparency Slider
				if (inside(mouseX, mouseY, panelX + 6.0F, panelY + 44.0F, width - 12.0F, 20.0F)) {
					draggingSlider = SliderDrag.TRANSPARENCY;
					updateTransparencyFromMouse(mouseX, sliderTrackX, sliderTrackW);
					return true;
				}

				// Widgets list
				float sepY = panelY + 68.0F;
				float rowY = sepY + 16.0F;
				float rowH = 15.0F;
				for (String widgetName : WIDGET_NAMES) {
					if (inside(mouseX, mouseY, panelX + 6.0F, rowY, width - 12.0F, rowH)) {
						toggleWidget(widgetName);
						ClientConfig.save();
						return true;
					}
					rowY += rowH;
				}

				return true;
			}
			panelOpen = false;
		}

		HudElement hovered = hoveredElement(mouseX, mouseY);
		if (hovered == null) {
			return false;
		}

		activeElement = hovered;
		bringToFront(hovered);
		hovered.beginDrag(mouseX, mouseY);
		return true;
	}

	private static boolean mouseReleased(int button) {
		if (button != 0) {
			return false;
		}

		if (draggingSlider != SliderDrag.NONE) {
			draggingSlider = SliderDrag.NONE;
			ClientConfig.save();
			return true;
		}

		if (activeElement == null || !activeElement.dragging()) {
			return false;
		}

		stopDragging(true);
		return true;
	}

	private static void drawText(MsdfFontRenderer renderer, GuiGraphics guiGraphics, String text, float x, float centerY, float size, int color, float thickness) {
		float baseline = renderer.centeredBaseline(text, size, centerY);
		renderer.draw(guiGraphics, text, x, baseline, size, color, 0.0F, thickness);
	}

	private static void updateAnimations() {
		long now = System.nanoTime();
		float deltaSeconds = Math.min(0.05F, Math.max(0.0F, (now - lastAnimationNanos) / 1_000_000_000.0F));
		lastAnimationNanos = now;
		float target = panelOpen ? 1.0F : 0.0F;
		panelAnimation = panelAnimation + (target - panelAnimation) * (1.0F - (float) Math.exp(-18.0F * deltaSeconds));
		if (Math.abs(panelAnimation - target) < 0.001F) {
			panelAnimation = target;
		}
	}

	private static float easeOutCubic(float value) {
		float inverse = 1.0F - Math.max(0.0F, Math.min(1.0F, value));
		return 1.0F - inverse * inverse * inverse;
	}

	private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	private static MsdfFontRenderer interFont() {
		if (interFont == null && Minecraft.getInstance().getResourceManager() != null) {
			interFont = new MsdfFontRenderer(INTER_FONT_TEXTURE, INTER_FONT_DATA);
		}
		return interFont;
	}

	private static MsdfFontRenderer iconFont() {
		if (iconFont == null && Minecraft.getInstance().getResourceManager() != null) {
			iconFont = new MsdfFontRenderer(BOTTOM_ICON_TEXTURE, BOTTOM_ICON_DATA);
		}
		return iconFont;
	}

	private static HudElement hoveredElement(double mouseX, double mouseY) {
		for (int index = ELEMENTS.size() - 1; index >= 0; index--) {
			HudElement element = ELEMENTS.get(index);
			if (element.contains(mouseX, mouseY)) {
				return element;
			}
		}
		return null;
	}

	private static void bringToFront(HudElement element) {
		if (ELEMENTS.isEmpty() || ELEMENTS.getLast() == element) {
			return;
		}
		ELEMENTS.remove(element);
		ELEMENTS.add(element);
	}

	private static void stopDragging(boolean save) {
		if (activeElement == null) {
			return;
		}

		boolean wasDragging = activeElement.dragging();
		activeElement.endDrag();
		activeElement = null;
		if (save && wasDragging) {
			ModuleManager.saveConfig();
		}
	}

	private static JsonObject positionObject(Position position) {
		JsonObject object = new JsonObject();
		object.addProperty("x", position.x());
		object.addProperty("y", position.y());
		return object;
	}

	private static boolean number(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
	}

	private record Position(float x, float y) {
	}
}
