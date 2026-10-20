package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.hud.HudElement;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.modules.impl.Combat.AimAssist;
import dev.Mardexx.client.theme.ThemeManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TargetHud extends Module {
	private static final TargetHud INSTANCE = new TargetHud();
	private static final float UI_SCALE = 1.43F;
	private static final float MIN_WIDTH = 68.0F * UI_SCALE;
	private static final float HEIGHT = 23.0F * UI_SCALE;
	private static final float PADDING = 3.4F * UI_SCALE;
	private static final int PORTRAIT_SIZE = Math.round(16.5F * UI_SCALE);
	private static final float CONTENT_X = PADDING + PORTRAIT_SIZE + 3.2F * UI_SCALE;
	private static final float NAME_SIZE = 5.67F * UI_SCALE;
	private static final float HEALTH_SIZE = 4.86F * UI_SCALE;
	private static final float BAR_HEIGHT = 2.3F * UI_SCALE;
	private static final float BAR_RADIUS = 0.65F * UI_SCALE;
	private static final float CORNER_RADIUS = 5.5F * UI_SCALE;
	private static final float SHADOW_SPREAD = 6.0F * UI_SCALE;
	private static final long TARGET_HOLD_NANOS = 3_000_000_000L;
	private static final int BACKGROUND_COLOR = 0xE80A0A0D;
	private static final int SHADOW_COLOR = 0x800A0A0D;
	private static final int BAR_BACKGROUND_COLOR = 0xFF25252C;
	private static final int ABSORPTION_COLOR = 0xFFFFC83D;
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");

	private final TargetHudElement element = new TargetHudElement();
	private LivingEntity target;
	private long targetExpiresAt;
	private MsdfFontRenderer font;

	private TargetHud() {
		super("Target HUD", ModuleCategory.RENDER, "Shows a compact animated health panel for the current target.");
		HudManager.register(element);
		AttackEntityCallback.EVENT.register((player, level, hand, attacked, hitResult) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (enabled() && player == minecraft.player && attacked instanceof LivingEntity living) {
				setTarget(living);
			}
			return InteractionResult.PASS;
		});
	}

	@Override
	public boolean enabled() {
		Interface iface = Interface.getInstance();
		return iface != null ? iface.isWidgetEnabled("Target HUD") : true;
	}

	public static TargetHud getInstance() {
		return INSTANCE;
	}

	@Override
	public void onTick() {
		updateTargetState();
	}

	@Override
	protected void onDisable() {
		clearTarget();
	}

	private void updateTargetState() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			clearTarget();
			return;
		}

		if (minecraft.hitResult instanceof EntityHitResult hit
				&& hit.getEntity() instanceof LivingEntity living
				&& living != minecraft.player
				&& valid(living)) {
			setTarget(living);
			return;
		}

		AimAssist aimAssist = AimAssist.getInstance();
		if (aimAssist != null && aimAssist.enabled()) {
			LivingEntity aimTarget = aimAssist.currentTarget();
			if (aimTarget != null && valid(aimTarget)) {
				setTarget(aimTarget);
				return;
			} else if (target != null && aimTarget == null) {
				clearTarget();
				return;
			}
		}

		if (!valid(target) || System.nanoTime() >= targetExpiresAt) {
			clearTarget();
		}
	}

	private void setTarget(LivingEntity living) {
		target = living;
		targetExpiresAt = System.nanoTime() + TARGET_HOLD_NANOS;
	}

	private void clearTarget() {
		target = null;
		targetExpiresAt = 0L;
	}

	private static boolean valid(LivingEntity living) {
		return living != null && living.isAlive() && !living.isRemoved();
	}

	private MsdfFontRenderer font() {
		if (font == null && Minecraft.getInstance().getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private final class TargetHudElement extends HudElement {
		private float animatedHealth = Float.NaN;
		private float animatedAbsorption = Float.NaN;
		private float revealProgress;
		private float panelWidth = MIN_WIDTH;
		private int animatedEntityId = Integer.MIN_VALUE;
		private LivingEntity renderedTarget;
		private List<ItemStack> cachedItems = new ArrayList<>();
		private long lastFrameNanos = System.nanoTime();

		private TargetHudElement() {
			super("target_hud", 225.0F, 165.0F);
		}

		@Override
		public Module module() {
			return TargetHud.this;
		}

		@Override
		protected void render(GuiGraphics guiGraphics, float tickDelta) {
			updateTargetState();

			Minecraft minecraft = Minecraft.getInstance();
			LivingEntity activeTarget = valid(target)
					? target
					: minecraft.screen instanceof ChatScreen ? minecraft.player : null;
			if (activeTarget != null) {
				renderedTarget = activeTarget;
			}
			float delta = frameDelta();
			revealProgress = animate(revealProgress, activeTarget == null ? 0.0F : 1.0F, 14.0F, delta);
			LivingEntity living = activeTarget != null ? activeTarget : renderedTarget;
			MsdfFontRenderer renderer = font();
			if (living == null || renderer == null || revealProgress <= 0.001F) {
				if (revealProgress <= 0.001F) {
					renderedTarget = null;
					cachedItems.clear();
					animatedEntityId = Integer.MIN_VALUE;
				}
				return;
			}

			float health = Math.max(0.0F, living.getHealth());
			float maxHealth = Math.max(1.0F, living.getMaxHealth());
			float absorption = Math.max(0.0F, living.getAbsorptionAmount());
			if (animatedEntityId != living.getId() || Float.isNaN(animatedHealth)) {
				animatedEntityId = living.getId();
				animatedHealth = health;
				animatedAbsorption = absorption;
				cachedItems = collectEquipment(living);
			} else {
				animatedHealth = animate(animatedHealth, health, 11.0F, delta);
				animatedAbsorption = animate(animatedAbsorption, absorption, 11.0F, delta);
			}

			String name = displayName(living);
			String hpText = String.format(Locale.ROOT, "%.1f HP", health);
			float hpTextWidth = renderer.width(hpText, HEALTH_SIZE);
			float nameTextWidth = renderer.width(name, NAME_SIZE);
			List<ItemStack> items = living.isAlive() ? collectEquipment(living) : cachedItems;
			if (living.isAlive()) {
				cachedItems = items;
			}
			float itemSpacing = 9.5F;
			float equipmentWidth = items.isEmpty() ? 0.0F : items.size() * itemSpacing;
			panelWidth = Math.max(MIN_WIDTH,
					CONTENT_X + Math.max(nameTextWidth + 6.0F + hpTextWidth, equipmentWidth) + PADDING);
			float panelX = renderX();

			float fadeAlpha = clamp(revealProgress);
			float popupProgress = easeOutCubic(fadeAlpha);
			float popupScale = 0.85F + 0.15F * popupProgress;
			float centerX = panelX + panelWidth * 0.5F;
			float centerY = y() + HEIGHT * 0.5F;

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(centerX, centerY, 0.0F);
			guiGraphics.pose().scale(popupScale, popupScale, 1.0F);
			guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
			try {
				Render2D.softShadow(guiGraphics, panelX, y(), panelWidth, HEIGHT, CORNER_RADIUS, SHADOW_SPREAD,
						HudManager.withHudAlpha(withAlpha(SHADOW_COLOR, fadeAlpha)));
				Render2D.roundedRect(guiGraphics, panelX, y(), panelWidth, HEIGHT, CORNER_RADIUS,
						HudManager.withHudAlpha(withAlpha(BACKGROUND_COLOR, fadeAlpha)));
				renderPortrait(guiGraphics, living, fadeAlpha, panelX);

				float contentLeft = panelX + CONTENT_X;
				float contentRight = panelX + panelWidth - PADDING;
				float barWidth = contentRight - contentLeft;
				float itemY = y() + 2.4F * UI_SCALE;
				float textCenterY = y() + 12.8F * UI_SCALE;
				float barY = y() + 17.5F * UI_SCALE;

				float itemStartX = contentRight - equipmentWidth + 1.0F;
				renderEquipment(guiGraphics, items, itemStartX, itemY, itemSpacing, fadeAlpha);

				draw(renderer, guiGraphics, name, contentLeft, textCenterY, NAME_SIZE, withAlpha(0xFFF4F4F6, fadeAlpha), 0.04F);
				draw(renderer, guiGraphics, hpText, contentRight - hpTextWidth, textCenterY, HEALTH_SIZE, withAlpha(0xFFD6D6DC, fadeAlpha), 0.025F);

				Render2D.roundedRect(guiGraphics, contentLeft, barY, barWidth, BAR_HEIGHT, BAR_RADIUS,
						withAlpha(BAR_BACKGROUND_COLOR, fadeAlpha));
				float absorptionWidth = barWidth * clamp(animatedAbsorption / maxHealth);
				float healthWidth = barWidth * clamp(animatedHealth / maxHealth);
				if (healthWidth > 0.01F) {
					Render2D.roundedRect(guiGraphics, contentLeft, barY, healthWidth, BAR_HEIGHT, BAR_RADIUS,
							withAlpha(ThemeManager.accent(), fadeAlpha));
				}

				if (absorptionWidth > 0.01F) {
					float absorptionX = contentRight - absorptionWidth;
					Render2D.roundedRect(guiGraphics, absorptionX, barY, absorptionWidth, BAR_HEIGHT, BAR_RADIUS,
							withAlpha(ABSORPTION_COLOR, fadeAlpha));
				}
			} finally {
				guiGraphics.pose().popPose();
			}
		}

		private void renderEquipment(GuiGraphics guiGraphics, List<ItemStack> items, float startX, float itemY, float spacing, float fadeAlpha) {
			if (items.isEmpty() || fadeAlpha <= 0.001F) {
				return;
			}
			float scaleFactor = 0.52F;
			PoseStack poseStack = guiGraphics.pose();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
			try {
				for (int index = 0; index < items.size(); index++) {
					ItemStack stack = items.get(index);
					if (stack == null || stack.isEmpty()) {
						continue;
					}
					float slotX = startX + index * spacing;
					poseStack.pushPose();
					poseStack.translate(slotX, itemY, 0.0F);
					poseStack.scale(scaleFactor, scaleFactor, 1.0F);
					guiGraphics.renderItem(stack, 0, 0);
					poseStack.popPose();
				}
			} finally {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			}
		}

		private static List<ItemStack> collectEquipment(LivingEntity living) {
			List<ItemStack> items = new ArrayList<>();
			ItemStack main = living.getMainHandItem();
			if (!main.isEmpty()) items.add(main);
			ItemStack head = living.getItemBySlot(EquipmentSlot.HEAD);
			if (!head.isEmpty()) items.add(head);
			ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
			if (!chest.isEmpty()) items.add(chest);
			ItemStack legs = living.getItemBySlot(EquipmentSlot.LEGS);
			if (!legs.isEmpty()) items.add(legs);
			ItemStack feet = living.getItemBySlot(EquipmentSlot.FEET);
			if (!feet.isEmpty()) items.add(feet);
			ItemStack off = living.getOffhandItem();
			if (!off.isEmpty()) items.add(off);
			return items;
		}

		private void renderPortrait(GuiGraphics guiGraphics, LivingEntity living, float fadeAlpha, float panelX) {
			int portraitX = Math.round(panelX + PADDING);
			int portraitY = Math.round(y() + (HEIGHT - PORTRAIT_SIZE) * 0.5F);
			Render2D.roundedRect(guiGraphics, portraitX, portraitY, PORTRAIT_SIZE, PORTRAIT_SIZE, 1.5F * UI_SCALE, withAlpha(0xFF151519, fadeAlpha));
			if (living instanceof AbstractClientPlayer player) {
				PlayerFaceRenderer.draw(guiGraphics, player.getSkin(), portraitX, portraitY, PORTRAIT_SIZE, withAlpha(0xFFFFFFFF, fadeAlpha));
				return;
			}

			float centerX = portraitX + PORTRAIT_SIZE * 0.5F;
			float centerY = portraitY + PORTRAIT_SIZE * 0.42F;
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
			try {
				InventoryScreen.renderEntityInInventoryFollowsMouse(
						guiGraphics,
						portraitX,
						portraitY,
						portraitX + PORTRAIT_SIZE,
						portraitY + PORTRAIT_SIZE,
						PORTRAIT_SIZE - Math.round(3.0F * UI_SCALE),
						centerX,
						centerY,
						centerY,
						living
				);
			} finally {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			}
		}

		@Override
		public float width() {
			return panelWidth;
		}

		@Override
		protected float horizontalOffset() {
			return -(panelWidth - MIN_WIDTH) * 0.5F;
		}

		@Override
		public float height() {
			return HEIGHT;
		}

		@Override
		public boolean visible() {
			Minecraft minecraft = Minecraft.getInstance();
			return Interface.getInstance() != null && Interface.getInstance().isWidgetEnabled("Target HUD") && (minecraft.screen instanceof ChatScreen || valid(target) || revealProgress > 0.001F);
		}

		private float frameDelta() {
			long now = System.nanoTime();
			float delta = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
			lastFrameNanos = now;
			return delta;
		}
	}

	private static String displayName(LivingEntity living) {
		if (living instanceof Player player) {
			return FriendManager.displayName(player.getGameProfile().getName());
		}
		return living.getName().getString();
	}

	private static void draw(MsdfFontRenderer renderer, GuiGraphics graphics, String text, float x, float centerY, float size, int color, float thickness) {
		float baseline = renderer.centeredBaseline(text, size, centerY);
		renderer.draw(graphics, text, x, baseline, size, color, 0.0F, thickness);
	}

	private static float animate(float current, float target, float speed, float delta) {
		float result = current + (target - current) * (1.0F - (float) Math.exp(-speed * delta));
		return Math.abs(result - target) < 0.001F ? target : result;
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
