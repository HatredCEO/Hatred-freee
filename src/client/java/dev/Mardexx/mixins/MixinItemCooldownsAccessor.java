package dev.Mardexx.mixins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public interface MixinItemCooldownsAccessor {
	@Accessor("cooldowns")
	Map<ResourceLocation, Object> hatred$getCooldowns();

	@Accessor("tickCount")
	int hatred$getTickCount();
}
