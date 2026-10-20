package dev.Mardexx.mixins;

import dev.Mardexx.api.system.interfaces.IJumpingCooldown;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface MixinLivingEntityAccessor extends IJumpingCooldown {
	@Override
	@Accessor("noJumpDelay")
	void hatred$setJumpingCooldown(int cooldown);
}
