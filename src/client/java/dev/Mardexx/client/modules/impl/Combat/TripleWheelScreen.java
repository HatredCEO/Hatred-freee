package dev.Mardexx.client.modules.impl.Combat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public final class TripleWheelScreen extends Screen {
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");
	private static final float OUTER_RADIUS = 100.0F;
	private static final float INNER_RADIUS = 60.0F;
	private static final float MID_RADIUS = (OUTER_RADIUS + INNER_RADIUS) * 0.5F;
	private static final double GAP = Math.toRadians(1.0D);
	private static final double STEP = Math.toRadians(0.5D);
	private static final long OPEN_ALPHA_MS = 90L;
	private static final long OPEN_SECTOR_MS = 130L;
	private static final long OPEN_STAGGER_MS = 18L;
	private static final long SELECTION_MS = 85L;
	private static final long CLOSE_MS = 80L;
	private static final double[] CENTERS = {
			-Math.PI / 2.0D,
			Math.PI / 6.0D,
			5.0D * Math.PI / 6.0D
	};

	private final AutoSwap module;
	private final float[] hoverProgress = new float[3];
	private final float[] pressProgress = new float[3];
	private MsdfFontRenderer uiFont;
	private boolean exit;
	private boolean selectionAnimating;
	private boolean closingAfterSelection;
	private int hoveredSlot = -1;
	private int selectionSlot = -1;
	private long openTimeMs;
	private long selectionStartedAtMs;
	private long closeStartedAtMs;
	private Runnable closeAction;

	public TripleWheelScreen(AutoSwap module) {
		super(Component.literal("Auto Swap"));
		this.module = module;
	}

	@Override
	protected void init() {
		exit = false;
		selectionAnimating = false;
		closingAfterSelection = false;
		hoveredSlot = -1;
		selectionSlot = -1;
		closeAction = null;
		openTimeMs = System.currentTimeMillis();
		selectionStartedAtMs = 0L;
		closeStartedAtMs = 0L;
		for (int slot = 0; slot < 3; slot++) {
			hoverProgress[slot] = 0.0F;
			pressProgress[slot] = 0.0F;
		}
	}

	int getHoveredSlot() {
		return hoveredSlot;
	}

	boolean isSelectionAnimating() {
		return selectionAnimating || closingAfterSelection || exit;
	}

	void startSelectionAnimation(int slot, Runnable action) {
		if (selectionAnimating || closingAfterSelection || exit || slot < 0 || slot >= 3) {
			return;
		}
		selectionAnimating = true;
		selectionSlot = slot;
		selectionStartedAtMs = System.currentTimeMillis();
		closeAction = action;
	}

	void startCloseAnimation(Runnable action) {
		if (closingAfterSelection || exit) {
			return;
		}
		closeAction = action;
		closingAfterSelection = true;
		closeStartedAtMs = System.currentTimeMillis();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		long now = System.currentTimeMillis();
		hoveredSlot = computeHoveredSlot(mouseX, mouseY);

		if (selectionAnimating && now - selectionStartedAtMs >= SELECTION_MS) {
			Runnable action = closeAction;
			closeAction = null;
			if (action != null) {
				action.run();
			}
			selectionAnimating = false;
			closingAfterSelection = true;
			closeStartedAtMs = now;
		}

		if ((exit || closingAfterSelection) && now - closeStartedAtMs >= CLOSE_MS) {
			Runnable action = closeAction;
			closeAction = null;
			if (minecraft != null) {
				minecraft.setScreen(null);
			}
			if (action != null) {
				action.run();
			}
			return;
		}

		float alpha = Math.min(1.0F, (now - openTimeMs) / (float) OPEN_ALPHA_MS);
		if (exit || closingAfterSelection) {
			alpha *= 1.0F - Mth.clamp((now - closeStartedAtMs) / (float) CLOSE_MS, 0.0F, 1.0F);
		}
		float centerX = width * 0.5F;
		float centerY = height * 0.5F;
		float selectionProgress = selectionAnimating || closingAfterSelection
				? smoothOut((now - selectionStartedAtMs) / (float) SELECTION_MS)
				: 0.0F;

		for (int slot = 0; slot < 3; slot++) {
			boolean hovered = hoveredSlot == slot && !closingAfterSelection && !exit;
			boolean pressed = (selectionAnimating || closingAfterSelection) && selectionSlot == slot;
			hoverProgress[slot] = hovered ? 1.0F : approach(hoverProgress[slot], 0.0F, 0.35F);
			pressProgress[slot] = pressed ? 1.0F : approach(pressProgress[slot], 0.0F, 0.35F);
		}

		Render2D.softShadow(graphics, centerX - OUTER_RADIUS, centerY - OUTER_RADIUS, OUTER_RADIUS * 2.0F, OUTER_RADIUS * 2.0F, OUTER_RADIUS, 18.0F, argb(0, 0, 0, 0.55F * alpha));
		for (int order = 0; order < 3; order++) {
			int slot = drawOrderSlot(order);
			drawSlot(graphics, slot, centerX, centerY, alpha, selectionProgress, now);
		}

		Render2D.roundedRect(graphics, centerX - INNER_RADIUS + 2.0F, centerY - INNER_RADIUS + 2.0F, (INNER_RADIUS - 2.0F) * 2.0F, (INNER_RADIUS - 2.0F) * 2.0F, INNER_RADIUS - 2.0F, argb(10, 10, 13, 0.82F * alpha));
		drawHint(graphics, centerX, centerY, alpha);
	}

	private void drawSlot(GuiGraphics graphics, int slot, float centerX, float centerY, float alpha,
			float selectionProgress, long now) {
		double half = Math.PI / 3.0D - GAP;
		boolean inHand = isInHand(slot);
		boolean dimmed = (selectionAnimating || closingAfterSelection) && selectionSlot != slot;
		float open = openProgress(slot, now);
		float hover = hoverProgress[slot];
		float pressed = pressProgress[slot];
		float visible = alpha * open * (dimmed ? 1.0F - 0.82F * selectionProgress : 1.0F);
		float openScale = 0.42F + open * 0.58F;
		float innerRadius = INNER_RADIUS * openScale - hover * 3.0F - pressed * 5.0F;
		float outerRadius = OUTER_RADIUS * openScale + hover * 5.0F + pressed * 8.0F;
		float middleRadius = (innerRadius + outerRadius) * 0.5F;
		float shift = (1.0F - open) * -16.0F + hover * 3.0F + pressed * 5.0F;
		float sectorCenterX = centerX + (float) Math.cos(CENTERS[slot]) * shift;
		float sectorCenterY = centerY + (float) Math.sin(CENTERS[slot]) * shift;

		float red = inHand ? 210.0F : 128.0F;
		float green = inHand ? 50.0F : 128.0F;
		float blue = inHand ? 40.0F : 128.0F;
		float colorAlpha = inHand ? 0.72F : 0.46F;
		red = lerp(red, 230.0F, hover);
		green = lerp(green, 205.0F, hover);
		blue = lerp(blue, 115.0F, hover);
		colorAlpha = lerp(colorAlpha, 0.82F, hover);
		red = lerp(red, 238.0F, pressed);
		green = lerp(green, 210.0F, pressed);
		blue = lerp(blue, 85.0F, pressed);
		colorAlpha = lerp(colorAlpha, 0.90F, pressed);

		drawSector(
				graphics,
				sectorCenterX,
				sectorCenterY,
				Math.max(1.0F, innerRadius - 1.2F),
				outerRadius + 1.2F,
				CENTERS[slot] - half,
				CENTERS[slot] + half,
				argb(255, 255, 255, 0.16F * visible)
		);

		drawSector(
				graphics,
				sectorCenterX,
				sectorCenterY,
				innerRadius,
				outerRadius,
				CENTERS[slot] - half,
				CENTERS[slot] + half,
				argb(Math.round(red), Math.round(green), Math.round(blue), colorAlpha * visible)
		);

		ItemStack stack = stack(slot);
		if (!stack.isEmpty()) {
			float iconX = sectorCenterX + middleRadius * (float) Math.cos(CENTERS[slot]);
			float iconY = sectorCenterY + middleRadius * (float) Math.sin(CENTERS[slot]);
			float iconScale = (0.92F + hover * 0.15F + pressed * 0.12F) * open;
			drawItem(graphics, stack, iconX, iconY, iconScale);
		}
	}

	private void drawHint(GuiGraphics graphics, float centerX, float centerY, float alpha) {
		MsdfFontRenderer renderer = font();
		if (renderer == null) {
			return;
		}
		String hint;
		if (hoveredSlot >= 0) {
			ItemStack item = stack(hoveredSlot);
			hint = item.isEmpty()
					? "Empty  —  RMB to assign an item"
					: item.getHoverName().getString() + "  —  release to swap  |  RMB to change";
		} else {
			hint = "Move the mouse toward a slot  |  RMB to assign";
		}
		float size = 8.5F;
		float x = centerX - renderer.width(hint, size) * 0.5F;
		float baseline = renderer.centeredBaseline(hint, size, centerY + OUTER_RADIUS + 16.0F);
		renderer.draw(graphics, hint, x, baseline, size, argb(255, 255, 255, 0.55F * alpha), 0.0F, 0.025F);
		MsdfFontRenderer.flushPending();
	}

	private int drawOrderSlot(int order) {
		if (hoveredSlot < 0 || order == 2) {
			return hoveredSlot >= 0 && order == 2 ? hoveredSlot : order;
		}
		int index = 0;
		for (int slot = 0; slot < 3; slot++) {
			if (slot == hoveredSlot) {
				continue;
			}
			if (index++ == order) {
				return slot;
			}
		}
		return order;
	}

	private float openProgress(int slot, long now) {
		float progress = (now - openTimeMs - slot * OPEN_STAGGER_MS) / (float) OPEN_SECTOR_MS;
		return smoothOut(progress);
	}

	private boolean isInHand(int slot) {
		if (minecraft == null || minecraft.player == null) {
			return false;
		}
		ItemStack configured = stack(slot);
		ItemStack offhand = minecraft.player.getOffhandItem();
		return !configured.isEmpty() && !offhand.isEmpty() && offhand.is(configured.getItem());
	}

	private ItemStack stack(int slot) {
		var setting = module.tripleSlot(slot);
		return setting == null || !setting.hasValue() ? ItemStack.EMPTY : setting.stack();
	}

	private int computeHoveredSlot(double mouseX, double mouseY) {
		double dx = mouseX - width * 0.5D;
		double dy = mouseY - height * 0.5D;
		if (Math.hypot(dx, dy) < 12.0D) {
			return -1;
		}
		double angle = Math.atan2(dy, dx);
		int closestSlot = 0;
		double closestDifference = Double.MAX_VALUE;
		for (int slot = 0; slot < 3; slot++) {
			double difference = Math.abs(wrapAngle(angle - CENTERS[slot]));
			if (difference < closestDifference) {
				closestDifference = difference;
				closestSlot = slot;
			}
		}
		return closestSlot;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isSelectionAnimating()) {
			return true;
		}
		int slot = computeHoveredSlot(mouseX, mouseY);
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			selectAndClose(slot);
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			if (slot >= 0 && minecraft != null) {
				minecraft.setScreen(new TripleItemPickerScreen(this, module.tripleSlot(slot)));
				return true;
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (keyCode == module.swapKey()) {
			selectAndClose(currentHoveredSlot());
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			if (!exit && !closingAfterSelection) {
				exit = true;
				closeStartedAtMs = System.currentTimeMillis();
				closeAction = null;
			}
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private MsdfFontRenderer font() {
		if (uiFont == null && minecraft != null && minecraft.getResourceManager() != null) {
			uiFont = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return uiFont;
	}

	private void selectAndClose(int slot) {
		if (slot < 0) {
			startCloseAnimation(null);
			return;
		}
		startSelectionAnimation(slot, () -> module.executeTripleSwap(slot));
	}

	private int currentHoveredSlot() {
		if (minecraft == null || minecraft.getWindow() == null) {
			return hoveredSlot;
		}
		double guiScale = minecraft.getWindow().getGuiScale();
		return computeHoveredSlot(minecraft.mouseHandler.xpos() / guiScale, minecraft.mouseHandler.ypos() / guiScale);
	}

	private static void drawItem(GuiGraphics graphics, ItemStack stack, float centerX, float centerY, float scale) {
		if (scale <= 0.001F) {
			return;
		}
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-8.0F, -8.0F, 0.0F);
		graphics.renderItem(stack, 0, 0);
		graphics.pose().popPose();
	}

	private static void drawSector(GuiGraphics graphics, float centerX, float centerY, float innerRadius,
			float outerRadius, double startAngle, double endAngle, int color) {
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);

		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int steps = Math.max(1, (int) Math.ceil((endAngle - startAngle) / STEP));
		double angleStep = (endAngle - startAngle) / steps;
		for (int index = 0; index < steps; index++) {
			double first = startAngle + index * angleStep;
			double second = first + angleStep;
			buffer.addVertex(matrix, centerX + (float) Math.cos(first) * innerRadius, centerY + (float) Math.sin(first) * innerRadius, 0.0F).setColor(color);
			buffer.addVertex(matrix, centerX + (float) Math.cos(first) * outerRadius, centerY + (float) Math.sin(first) * outerRadius, 0.0F).setColor(color);
			buffer.addVertex(matrix, centerX + (float) Math.cos(second) * outerRadius, centerY + (float) Math.sin(second) * outerRadius, 0.0F).setColor(color);
			buffer.addVertex(matrix, centerX + (float) Math.cos(second) * innerRadius, centerY + (float) Math.sin(second) * innerRadius, 0.0F).setColor(color);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.disableBlend();
	}

	private static float approach(float value, float target, float speed) {
		return Math.abs(value - target) < 0.001F ? target : value + (target - value) * speed;
	}

	private static float smoothOut(float value) {
		float clamped = Mth.clamp(value, 0.0F, 1.0F);
		return 1.0F - (float) Math.pow(1.0F - clamped, 3.0D);
	}

	private static float lerp(float from, float to, float progress) {
		return from + (to - from) * progress;
	}

	private static double wrapAngle(double angle) {
		while (angle > Math.PI) angle -= Math.PI * 2.0D;
		while (angle < -Math.PI) angle += Math.PI * 2.0D;
		return angle;
	}

	private static int argb(int red, int green, int blue, float alpha) {
		return Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24
				| red << 16
				| green << 8
				| blue;
	}
}
