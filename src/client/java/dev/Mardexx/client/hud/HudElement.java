package dev.Mardexx.client.hud;

import dev.Mardexx.client.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

public abstract class HudElement {
	private final String id;
	private final float defaultX;
	private final float defaultY;
	private float x;
	private float y;
	private float dragOffsetX;
	private float dragOffsetY;
	private float editorLift;
	private long lastEditorAnimationNanos = System.nanoTime();
	private boolean dragging;

	protected HudElement(String id, float defaultX, float defaultY) {
		this.id = Objects.requireNonNull(id, "id");
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.x = defaultX;
		this.y = defaultY;
	}

	protected abstract void render(GuiGraphics guiGraphics, float tickDelta);

	public abstract float width();

	public abstract float height();

	public boolean visible() {
		dev.Mardexx.client.modules.impl.Render.Interface iface = dev.Mardexx.client.modules.impl.Render.Interface.getInstance();
		if (iface == null) {
			return true;
		}
		if (module() != null) {
			return iface.isWidgetEnabled(module().name());
		}
		return true;
	}

	public Module module() {
		return null;
	}

	public final String id() {
		return id;
	}

	public final float x() {
		return x;
	}

	public final float y() {
		return y;
	}

	protected float horizontalOffset() {
		return 0.0F;
	}

	protected final float renderX() {
		return x + horizontalOffset();
	}

	public final boolean dragging() {
		return dragging;
	}

	public final void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	final void draw(GuiGraphics guiGraphics, float tickDelta) {
		if (!visible()) {
			return;
		}
		boolean previousRenderingHud = HudManager.isRenderingHud();
		HudManager.setRenderingHud(true);
		try {
			float hudScale = HudManager.scale();
			float scale = (1.0F + editorLift * 0.025F) * hudScale;
			float centerX = renderX() + Math.max(0.0F, width()) * 0.5F;
			float centerY = y + Math.max(0.0F, height()) * 0.5F;
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(centerX, centerY - editorLift * 1.25F, 0.0F);
			guiGraphics.pose().scale(scale, scale, 1.0F);
			guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
			render(guiGraphics, tickDelta);
			guiGraphics.pose().popPose();
		} finally {
			HudManager.setRenderingHud(previousRenderingHud);
		}
	}

	final void updateEditorFocus(boolean focused) {
		long now = System.nanoTime();
		float delta = Math.min(0.05F, Math.max(0.0F, (now - lastEditorAnimationNanos) / 1_000_000_000.0F));
		lastEditorAnimationNanos = now;
		float target = focused ? 1.0F : 0.0F;
		editorLift += (target - editorLift) * (1.0F - (float) Math.exp(-16.0F * delta));
		if (Math.abs(editorLift - target) < 0.001F) {
			editorLift = target;
		}
	}

	final boolean contains(double mouseX, double mouseY) {
		return visible()
				&& mouseX >= renderX()
				&& mouseX <= renderX() + Math.max(0.0F, width())
				&& mouseY >= y
				&& mouseY <= y + Math.max(0.0F, height());
	}

	final void beginDrag(double mouseX, double mouseY) {
		dragOffsetX = (float) mouseX - renderX();
		dragOffsetY = (float) mouseY - y;
		dragging = true;
	}

	final void updateDrag(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		if (!dragging) {
			return;
		}

		x = (float) mouseX - dragOffsetX - horizontalOffset();
		y = (float) mouseY - dragOffsetY;
		clampToScreen(screenWidth, screenHeight);
	}

	final void endDrag() {
		dragging = false;
	}

	final void clampToScreen(int screenWidth, int screenHeight) {
		float scale = HudManager.scale();
		float w = Math.max(0.0F, width());
		float h = Math.max(0.0F, height());
		float offsetX = horizontalOffset();

		float halfExtraW = Math.max(0.0F, (w * scale - w) * 0.5F);
		float halfExtraH = Math.max(0.0F, (h * scale - h) * 0.5F);

		float minRenderedX = halfExtraW;
		float maxRenderedX = Math.max(minRenderedX, screenWidth - w - halfExtraW);

		float minRenderedY = halfExtraH;
		float maxRenderedY = Math.max(minRenderedY, screenHeight - h - halfExtraH);

		float renderedX = x + offsetX;
		renderedX = Math.max(minRenderedX, Math.min(renderedX, maxRenderedX));
		x = renderedX - offsetX;
		y = Math.max(minRenderedY, Math.min(y, maxRenderedY));
	}

	final void repairInvalidPosition() {
		if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 100_000.0F || Math.abs(y) > 100_000.0F) {
			x = defaultX;
			y = defaultY;
		}
	}
}
