package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.FloatSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class WallHack extends Module implements QuickImports {
	private static final WallHack INSTANCE = new WallHack();

	private final FloatSetting range = new FloatSetting(
			"range",
			"Range",
			"Maximum range to render players.",
			10.0F,
			55.0F,
			160.0F
	).step(5.0F).precision(0).suffix("m");

	private final FloatSetting playerLimit = new FloatSetting(
			"player_limit",
			"Player limit",
			"Maximum number of players to render.",
			1.0F,
			18.0F,
			60.0F
	).step(1.0F).precision(0);

	private final BooleanSetting showFriends = new BooleanSetting(
			"show_friends",
			"Show friends",
			"Renders friendly players through blocks.",
			true
	);

	private final BooleanSetting fullBright = new BooleanSetting(
			"full_bright",
			"Full Bright",
			"Renders players with full brightness without world shadows.",
			false
	);

	private WallHack() {
		super("WallHack", ModuleCategory.RENDER, "Renders player skins through blocks.");
		register(range, playerLimit, showFriends, fullBright);
	}

	public static WallHack getInstance() {
		return INSTANCE;
	}

	public boolean fullBright() {
		return fullBright.enabled();
	}

	public boolean shouldRender(Entity entity) {
		if (!enabled() || !(entity instanceof Player player) || player == mc.player || !player.isAlive() || player.isSpectator()) {
			return false;
		}

		if (mc.player != null && mc.player.distanceToSqr(player) > (double) range.value() * range.value()) {
			return false;
		}

		if (!showFriends.enabled() && FriendManager.isFriend(player.getGameProfile().getName())) {
			return false;
		}

		return true;
	}

	public boolean isBehindWall(Entity entity) {
		if (mc.level == null || mc.player == null || entity == null) {
			return false;
		}
		return !mc.player.hasLineOfSight(entity);
	}
}
