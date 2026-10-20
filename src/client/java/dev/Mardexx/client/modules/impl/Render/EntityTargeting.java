package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.friends.FriendManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class EntityTargeting implements QuickImports {
	private EntityTargeting() {
	}

	public static EnumBooleanMapSetting<EntityTargetFilter> createTargetsSetting(String description) {
		return EnumBooleanMapSetting.create(
				"targets",
				"Targets",
				description,
				EntityTargetFilter.class,
				defaults -> {
					defaults.put(EntityTargetFilter.PLAYERS, true);
					defaults.put(EntityTargetFilter.FRIENDS, true);
					defaults.put(EntityTargetFilter.MOBS, true);
					defaults.put(EntityTargetFilter.ANIMALS, true);
				}
		);
	}

	public static boolean matches(Entity entity, EnumBooleanMapSetting<EntityTargetFilter> targets, boolean includeSelf) {
		if (entity == null || mc.player == null || !entity.isAlive()) {
			return false;
		}

		if (entity == mc.player) {
			return includeSelf && targets.enabled(EntityTargetFilter.PLAYERS);
		}

		EntityTargetFilter category = category(entity);
		return category != null && targets.enabled(category);
	}

	public static EntityTargetFilter category(Entity entity) {
		if (entity instanceof Player player) {
			return isFriend(player) ? EntityTargetFilter.FRIENDS : EntityTargetFilter.PLAYERS;
		}

		if (entity instanceof Animal
				|| entity.getType().getCategory() == MobCategory.CREATURE
				|| entity.getType().getCategory() == MobCategory.WATER_CREATURE
				|| entity.getType().getCategory() == MobCategory.AXOLOTLS
				|| entity.getType().getCategory() == MobCategory.UNDERGROUND_WATER_CREATURE) {
			return EntityTargetFilter.ANIMALS;
		}

		if (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER) {
			return EntityTargetFilter.MOBS;
		}

		return null;
	}

	public static boolean isFriend(Player player) {
		return player != null && FriendManager.isFriend(player.getGameProfile().getName());
	}
}
