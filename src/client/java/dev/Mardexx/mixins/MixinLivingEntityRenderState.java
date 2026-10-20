package dev.Mardexx.mixins;

import dev.Mardexx.api.system.interfaces.ILivingEntityRenderStateAccess;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class MixinLivingEntityRenderState implements ILivingEntityRenderStateAccess {
	@Unique
	private LivingEntity hatred$entity;

	@Override
	public LivingEntity hatred$getEntity() {
		return hatred$entity;
	}

	@Override
	public void hatred$setEntity(LivingEntity entity) {
		hatred$entity = entity;
	}
}
