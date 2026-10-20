package dev.Mardexx.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface MixinCooldownInstanceAccessor {
	@Accessor("endTime")
	int hatred$getEndTime();
}
