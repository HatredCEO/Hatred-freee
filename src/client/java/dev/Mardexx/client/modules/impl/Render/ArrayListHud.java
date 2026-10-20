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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArrayListHud extends Module {
	private static final ArrayListHud INSTANCE = new ArrayListHud();
	private static final float BASE_WIDTH = 25.0F;
	private static final float MAX_WIDTH = 198.0F;
	private static final float FONT_SIZE = 7.7F;
	private static final float ROW_HEIGHT = 8.8F;
	private static final float ROW_GAP = 0.0F;
	private static final float HORIZONTAL_PADDING = 2.2F;
	private static final float CORNER_RADIUS = 0.0F;
	private static final float SHADOW_SPREAD = 3.5F;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private final ArrayListElement element = new ArrayListElement();
	private MsdfFontRenderer font;

	private ArrayListHud() {
		super("Array List", ModuleCategory.RENDER, "Shows enabled modules in a compact animated list.");
		HudManager.register(element);
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Active Modules") : true;
	}

	public static ArrayListHud getInstance() {
		return INSTANCE;
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private final class ArrayListElement extends HudElement {
		private final Map<Module, AnimatedModule> rows = new LinkedHashMap<>();
		private final List<AnimatedModule> orderedRows = new ArrayList<>();
		private float animatedWidth = BASE_WIDTH;
		private float animatedHeight = ROW_HEIGHT;
		private long nextModuleSyncNanos;
		private long lastFrameNanos = System.nanoTime();

		private ArrayListElement() {
			super("array_list", 720.0F, 6.0F);
		}

		@Override
		public Module module() {
			return ArrayListHud.this;
		}

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			MsdfFontRenderer renderer = font();
			if (renderer == null) {
				return;
			}

			float delta = frameDelta();
			updateRows(renderer, delta);
			float targetWidth = targetWidth();
			animatedWidth = targetWidth > animatedWidth
					? targetWidth
					: animate(animatedWidth, targetWidth, 16.0F, delta);
			float contentProgress = 0.0F;
			for (AnimatedModule row : orderedRows) {
				contentProgress += row.progress();
			}
			float contentHeight = contentProgress * (ROW_HEIGHT + ROW_GAP);
			float targetHeight = Math.max(ROW_HEIGHT, contentHeight - ROW_GAP);
			animatedHeight = animate(animatedHeight, targetHeight, 18.0F, delta);

			float rowY = y();
			int accent = ThemeManager.accent();
			for (int index = 0; index < orderedRows.size(); index++) {
				AnimatedModule row = orderedRows.get(index);
				row.drawProgress = easeOutCubic(row.progress());
				float textWidth = row.textWidth();
				float slide = (1.0F - row.drawProgress) * 7.0F;
				row.drawRight = x() + animatedWidth + slide;
				row.drawWidth = Math.min(animatedWidth, textWidth + HORIZONTAL_PADDING * 2.0F);
				row.drawX = row.drawRight - row.drawWidth;
				row.drawY = rowY;
				row.topRadius = 0.0F;
				row.bottomRadius = 0.0F;
				rowY += (ROW_HEIGHT + ROW_GAP) * row.progress();
			}

			// Keep all GPU state changes together so module count does not multiply text uploads.
			if (!orderedRows.isEmpty()) {
				try (Render2D.SoftShadowBatch shadows = Render2D.beginSoftShadowBatch(guiGraphics, 0.0F, SHADOW_SPREAD)) {
					for (AnimatedModule row : orderedRows) {
						shadows.add(row.drawX, row.drawY, row.drawWidth, ROW_HEIGHT,
								HudManager.withHudAlpha(withAlpha(SHADOW_COLOR, row.drawProgress)));
					}
				}
				try (Render2D.RectBatch backgrounds = Render2D.beginRectBatch(guiGraphics)) {
					for (AnimatedModule row : orderedRows) {
						backgrounds.add(row.drawX, row.drawY, row.drawWidth, ROW_HEIGHT,
								HudManager.withHudAlpha(withAlpha(0xE80A0A0D, row.drawProgress)));
					}
				}
			}
			for (AnimatedModule row : orderedRows) {
				float contentX = row.drawRight - HORIZONTAL_PADDING - row.textWidth();
				float centerY = row.drawY + ROW_HEIGHT * 0.5F;
				draw(renderer, guiGraphics, row.module().name(), contentX, centerY,
						withAlpha(accent, row.drawProgress));
			}
		}

		private void updateRows(MsdfFontRenderer renderer, float delta) {
			boolean orderChanged = false;
			long now = System.nanoTime();
			if (now >= nextModuleSyncNanos) {
				nextModuleSyncNanos = now + 25_000_000L;
				for (AnimatedModule row : rows.values()) {
					row.target = false;
				}

				boolean hasActive = false;
				for (Module module : ModuleManager.modules()) {
					if (!module.enabled()) {
						continue;
					}
					hasActive = true;
					AnimatedModule row = rows.get(module);
					if (row == null) {
						row = new AnimatedModule(module, moduleWidth(renderer, module));
						rows.put(module, row);
						orderedRows.add(row);
						orderChanged = true;
					}
					row.target = true;
				}

				if (!hasActive) {
					AnimatedModule row = rows.get(ArrayListHud.this);
					if (row == null) {
						row = new AnimatedModule(ArrayListHud.this, moduleWidth(renderer, ArrayListHud.this));
						rows.put(ArrayListHud.this, row);
						orderedRows.add(row);
						orderChanged = true;
					}
					row.target = true;
				}
			}

			Iterator<AnimatedModule> iterator = rows.values().iterator();
			while (iterator.hasNext()) {
				AnimatedModule row = iterator.next();
				row.progress = animate(row.progress, row.target ? 1.0F : 0.0F, 15.0F, delta);
				if (!row.target && row.progress <= 0.001F) {
					iterator.remove();
					orderedRows.remove(row);
					orderChanged = true;
				}
			}
			if (orderChanged) {
				orderedRows.sort(Comparator.comparingDouble(AnimatedModule::textWidth).reversed());
			}
		}

		private float targetWidth() {
			float required = BASE_WIDTH;
			for (AnimatedModule row : orderedRows) {
				required = Math.max(required, row.textWidth() + HORIZONTAL_PADDING * 2.0F);
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
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Active Modules");
		}

		private float frameDelta() {
			long now = System.nanoTime();
			float delta = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
			lastFrameNanos = now;
			return delta;
		}
	}

	private static final class AnimatedModule {
		private final Module module;
		private final float textWidth;
		private float drawProgress;
		private float drawX;
		private float drawY;
		private float drawWidth;
		private float drawRight;
		private float topRadius;
		private float bottomRadius;
		private float progress;
		private boolean target = true;

		private AnimatedModule(Module module, float textWidth) {
			this.module = module;
			this.textWidth = textWidth;
		}

		private Module module() {
			return module;
		}

		private float progress() {
			return progress;
		}

		private float textWidth() {
			return textWidth;
		}
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, int color) {
		float baseline = renderer.centeredBaseline(text, FONT_SIZE, centerY);
		renderer.draw(graphics, text, x, baseline, FONT_SIZE, color, 0.0F, 0.025F);
	}

	private static float moduleWidth(MsdfFontRenderer renderer, Module module) {
		return renderer.width(module.name(), FONT_SIZE);
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
		int sourceAlpha = color >>> 24;
		if (sourceAlpha == 0) {
			sourceAlpha = 255;
		}
		int alpha = Math.round(sourceAlpha * Math.max(0.0F, Math.min(1.0F, multiplier)));
		return alpha << 24 | color & 0x00FFFFFF;
	}
}
