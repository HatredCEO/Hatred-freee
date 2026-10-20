package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.utils.render.ProjectionUtil;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.FloatSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NameTags extends Module implements QuickImports {
	private static final NameTags INSTANCE = new NameTags();
	private static final ResourceLocation FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png");
	private static final ResourceLocation FONT_DATA = ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json");
	private static final float FONT_SIZE = 12.2F;
	private static final float FONT_BOLD_OFFSET = 0.35F;
	private static final float DURABILITY_TEXT_SIZE = 4.8F;
	private static final float NAMETAG_SIZE_MULTIPLIER = 0.52F;
	private static final float HEIGHT_OFFSET = 20.0F;
	private static final float BACKGROUND_ALPHA = 180.0F;
	private static final float ICON_RENDER_SCALE_MULTIPLIER = 0.8F;
	private static final float ICON_TAG_GAP = 0.7F;
	private static final float NAMETAG_CORNER_RADIUS = 6.2F;
	private static MsdfFontRenderer font;
	private static final Map<UUID, DisplayCache> CACHE = new HashMap<>();
	private List<LivingEntity> capturedTargets = List.of();

	private final EnumBooleanMapSetting<EntityTargetFilter> targets = EntityTargeting.createTargetsSetting("Selects which entities receive custom name tags.");
	private final BooleanSetting showSelf = new BooleanSetting("show_self", "Show Self", "Renders your own nametag too.", false);
	private final BooleanSetting showInvisible = new BooleanSetting("show_invisible", "Show Invisible", "Includes invisible entities.", true);
	private final BooleanSetting showPing = new BooleanSetting("show_ping", "Show Ping", "Shows ping on player name tags.", false);
	private final BooleanSetting showGamemode = new BooleanSetting("show_gamemode", "Show Gamemode", "Shows player gamemode tags.", false);
	private final BooleanSetting showDistance = new BooleanSetting("show_distance", "Show Distance", "Shows target distance.", false);
	private final BooleanSetting showHealth = new BooleanSetting("show_health", "Show Health", "Shows target health.", false);
	private final BooleanSetting showEquipment = new BooleanSetting("show_equipment", "Show Equipment", "Shows hands and armor under the tag.", false);
	private final BooleanSetting showOnlyHands = new BooleanSetting("show_only_hands", "Show Only Hands", "Only shows held items.", false);
	private final BooleanSetting showDurability = new BooleanSetting("show_durability", "Show Durability", "Shows the Minecraft-style durability bar under the icon.", false);
	private final BooleanSetting showDurabilityText = new BooleanSetting("show_durability_text", "Show Durability Text", "Shows durability percent instead of a bar.", false);
	private final FloatSetting scale = new FloatSetting("scale", "Scale", "Nametag scale.", 4.0F, 10.0F, 30.0F).step(1.0F).precision(0);
	private final FloatSetting iconScale = new FloatSetting("icon_scale", "Icon Scale", "Item icon scale.", 5.0F, 7.0F, 15.0F).step(1.0F).precision(0);

	private NameTags() {
		super("NameTags", ModuleCategory.RENDER, "Renders custom name tags for selected entities.");
		register(
				targets,
				showSelf,
				showInvisible,
				showPing,
				showGamemode,
				showDistance,
				showHealth,
				showEquipment,
				showOnlyHands,
				showDurability,
				showDurabilityText,
				scale,
				iconScale
		);
		Render2D.add(this::renderHud);
	}

	public static NameTags getInstance() {
		return INSTANCE;
	}

	public boolean shouldHideVanillaNameTag(LivingEntity entity) {
		return enabled()
				&& entity != null
				&& EntityTargeting.matches(entity, targets, showSelf.enabled())
				&& (!entity.isInvisible() || showInvisible.enabled());
	}

	@Override
	public void onTick() {
		if (!enabled() || mc.level == null || mc.player == null) {
			capturedTargets = List.of();
			return;
		}

		List<LivingEntity> entities = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity livingEntity) || livingEntity.isRemoved()) {
				continue;
			}
			if (!shouldRender(livingEntity)) {
				continue;
			}
			entities.add(livingEntity);
		}
		capturedTargets = entities;
	}

	private void renderHud(GuiGraphics guiGraphics) {
		if (!enabled() || mc.level == null || mc.player == null || capturedTargets.isEmpty()) {
			return;
		}

		MsdfFontRenderer font = font();
		if (font == null) {
			return;
		}
		float tickDelta = Render2D.tickDelta();

		for (LivingEntity livingEntity : capturedTargets) {
			if (livingEntity.isRemoved()) {
				continue;
			}
			double x = Mth.lerp(tickDelta, livingEntity.xOld, livingEntity.getX());
			double y = Mth.lerp(tickDelta, livingEntity.yOld, livingEntity.getY());
			double z = Mth.lerp(tickDelta, livingEntity.zOld, livingEntity.getZ());
			Optional<Vector2f> projection = ProjectionUtil.worldToScreen(
					new Vec3(x, y + HEIGHT_OFFSET / 10.0F, z)
			);
			if (projection.isEmpty()) {
				continue;
			}

			renderTag(guiGraphics, livingEntity, projection.get(), font);
		}
	}

	private boolean shouldRender(LivingEntity entity) {
		if (!EntityTargeting.matches(entity, targets, showSelf.enabled())) {
			return false;
		}
		if (entity.isInvisible() && !showInvisible.enabled()) {
			return false;
		}
		return true;
	}

	private void renderTag(GuiGraphics guiGraphics, LivingEntity entity, Vector2f projection, MsdfFontRenderer font) {
		DisplayCache cache = cache(entity, font);
		if (cache.parts().isEmpty()) {
			return;
		}

		float distance = mc.player != null ? mc.player.distanceTo(entity) : 0.0F;
		float distanceScale = 1.0F - (distance - 8.0F) * 0.008F;
		float tagScale = Mth.clamp(scale.floatValue() / 10.0F, 0.4F, 3.0F) * NAMETAG_SIZE_MULTIPLIER * Mth.clamp(distanceScale, 0.70F, 1.15F);
		float paddingX = 7.0F;
		float boxWidth = cache.textWidth() + paddingX * 2.0F;
		float boxHeight = 16.5F;
		float baseX = projection.x;
		float baseY = projection.y;
		float x = baseX - boxWidth / 2.0F;
		float y = baseY - 18.0F;

		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(baseX, baseY, 0.0F);
		poseStack.scale(tagScale, tagScale, 1.0F);
		poseStack.translate(-baseX, -baseY, 0.0F);

		if (BACKGROUND_ALPHA > 0.0F) {
			Render2D.roundedRect(guiGraphics, x, y, boxWidth, boxHeight, NAMETAG_CORNER_RADIUS, withAlpha(0xFF000000, BACKGROUND_ALPHA / 255.0F));
		}

		float textX = x + paddingX;
		float centerY = y + boxHeight * 0.5F;
		for (TextPart part : cache.parts()) {
			float baselineY = font.centeredBaseline(part.text(), FONT_SIZE, centerY);
			font.draw(guiGraphics, part.text(), textX, baselineY, FONT_SIZE, part.color());
			textX += font.width(part.text(), FONT_SIZE);
		}

		poseStack.popPose();

		if (showEquipment.enabled()) {
			float scaledTagY = baseY + (y - baseY) * tagScale;
			renderEquipment(guiGraphics, entity, baseX, scaledTagY, font);
		}
	}

	private List<TextPart> buildText(LivingEntity entity) {
		List<TextPart> parts = new ArrayList<>();
		if (entity instanceof Player player) {
			if (showPing.enabled()) {
				int ping = ping(player);
				parts.add(new TextPart(ping + "ms ", pingColor(ping)));
			}
			if (showGamemode.enabled()) {
				String gameMode = gamemode(player);
				if (!gameMode.isEmpty()) {
					parts.add(new TextPart(gameMode + " ", 0xFFBBBBBB));
				}
			}
		}

		int nameColor = entity instanceof Player player && EntityTargeting.isFriend(player) ? 0xFF55FF55 : 0xFFFFFFFF;
		String renderedName = entity.getDisplayName().getString();
		if (entity instanceof Player player) {
			String profileName = player.getGameProfile().getName();
			String alias = FriendManager.alias(profileName);
			if (!alias.isBlank()) {
				renderedName = alias;
			}
		}
		parts.add(new TextPart(renderedName + " ", nameColor));

		if (showHealth.enabled()) {
			float health = entity.getHealth() + entity.getAbsorptionAmount();
			parts.add(new TextPart(String.format(Locale.ROOT, "%.1f ", health), healthColor(health)));
		}
		if (showDistance.enabled()) {
			parts.add(new TextPart(String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(entity)), 0xFFDDDDDD));
		}
		return parts;
	}

	private void renderEquipment(GuiGraphics guiGraphics, LivingEntity entity, float centerX, float tagY, MsdfFontRenderer font) {
		List<ItemStack> stacks = collectEquipment(entity);
		List<ItemStack> visibleStacks = stacks.stream().filter(stack -> !stack.isEmpty()).toList();
		if (visibleStacks.isEmpty()) {
			return;
		}

		float scaleFactor = 4.0F * 0.3F * (iconScale.floatValue() / 10.0F) * ICON_RENDER_SCALE_MULTIPLIER;
		float iconSizePixels = 20.0F * scaleFactor;
		float spacing = iconSizePixels * 0.7F;
		float startX = centerX - spacing * (visibleStacks.size() - 1) / 2.0F;
		float renderedIconSize = 16.0F * scaleFactor;
		float itemY = tagY - renderedIconSize * 0.5F - ICON_TAG_GAP;

		for (int index = 0; index < visibleStacks.size(); index++) {
			ItemStack stack = visibleStacks.get(index);
			float slotX = startX + index * spacing;
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			poseStack.translate(slotX, itemY, 0.0F);
			poseStack.scale(scaleFactor, scaleFactor, 1.0F);
			poseStack.translate(-8.0F, -8.0F, 0.0F);

			guiGraphics.renderItem(stack, 0, 0);
			guiGraphics.renderItemDecorations(mc.font, stack, 0, 0);

			if (showDurability.enabled() && stack.isDamageableItem()) {
				float durability = Math.max(0.0F, 1.0F - stack.getDamageValue() / (float) stack.getMaxDamage());
				int durabilityColor = healthColor(durability * 20.0F);
				if (showDurabilityText.enabled()) {
					String text = Math.round(durability * 100.0F) + "%";
					float textX = 8.0F - font.width(text, DURABILITY_TEXT_SIZE) / 2.0F;
					drawBold(guiGraphics, text, textX, 15.0F, DURABILITY_TEXT_SIZE, durabilityColor);
				}
			}

			poseStack.popPose();
		}
	}

	private List<ItemStack> collectEquipment(LivingEntity entity) {
		List<ItemStack> stacks = new ArrayList<>();
		stacks.add(entity.getMainHandItem());
		if (!showOnlyHands.enabled()) {
			stacks.add(entity.getItemBySlot(EquipmentSlot.HEAD));
			stacks.add(entity.getItemBySlot(EquipmentSlot.CHEST));
			stacks.add(entity.getItemBySlot(EquipmentSlot.LEGS));
			stacks.add(entity.getItemBySlot(EquipmentSlot.FEET));
		}
		stacks.add(entity.getOffhandItem());
		return stacks;
	}

	private static void drawBold(GuiGraphics guiGraphics, String text, float x, float y, float size, int color) {
		MsdfFontRenderer font = font();
		if (font == null) {
			return;
		}

		font.draw(guiGraphics, text, x, y, size, color);
		font.draw(guiGraphics, text, x + FONT_BOLD_OFFSET, y, size, color);
	}

	private static MsdfFontRenderer font() {
		if (font == null && mc.getResourceManager() != null) {
			font = new MsdfFontRenderer(FONT_TEXTURE, FONT_DATA);
		}
		return font;
	}

	private static int withAlpha(int color, float alpha) {
		int scaledAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
		return (color & 0x00FFFFFF) | (scaledAlpha << 24);
	}

	private static int healthColor(float health) {
		if (health > 15.0F) {
			return 0xFF55FF55;
		}
		if (health > 7.0F) {
			return 0xFFFFFF55;
		}
		return 0xFFFF5555;
	}

	private static int pingColor(int ping) {
		if (ping <= 60) {
			return 0xFF55FF55;
		}
		if (ping <= 120) {
			return 0xFFFFFF55;
		}
		return 0xFFFF5555;
	}

	private static int ping(Player player) {
		if (mc.getConnection() == null) {
			return 0;
		}
		PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
		return playerInfo == null ? 0 : playerInfo.getLatency();
	}

	private DisplayCache cache(LivingEntity entity, MsdfFontRenderer font) {
		UUID uuid = entity.getUUID();
		float distance = mc.player.distanceTo(entity);
		float currentHealth = entity.getHealth() + entity.getAbsorptionAmount();
		int currentPing = entity instanceof Player player ? ping(player) : 0;
		long friendRevision = FriendManager.revision();
		long now = System.currentTimeMillis();

		DisplayCache cache = CACHE.get(uuid);
		if (cache == null
				|| now - cache.lastUpdate() > 500L
				|| Math.abs(cache.health() - currentHealth) > 0.5F
				|| cache.ping() != currentPing
				|| cache.friendRevision() != friendRevision) {
			List<TextPart> parts = buildText(entity);
			float textWidth = 0.0F;
			for (TextPart part : parts) {
				textWidth += font.width(part.text(), FONT_SIZE);
			}
			cache = new DisplayCache(parts, textWidth, now, currentHealth, currentPing, friendRevision);
			CACHE.put(uuid, cache);
		}

		return cache;
	}

	private static String gamemode(Player player) {
		if (mc.getConnection() == null) {
			return "";
		}
		PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
		if (playerInfo == null) {
			return "";
		}

		GameType gameType = playerInfo.getGameMode();
		if (gameType == null) {
			return "";
		}

		return switch (gameType) {
			case SURVIVAL -> "[S]";
			case CREATIVE -> "[C]";
			case ADVENTURE -> "[A]";
			case SPECTATOR -> "[SP]";
		};
	}

	private record TextPart(String text, int color) {
	}

	private record DisplayCache(List<TextPart> parts, float textWidth, long lastUpdate, float health, int ping, long friendRevision) {
	}
}
