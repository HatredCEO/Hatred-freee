package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Movement.NoPush;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
	@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void hatred$pushEntity(Entity entity, CallbackInfo info) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && NoPush.getInstance().cancelEntity() && ((Object) this == mc.player || entity == mc.player)) {
			info.cancel();
		}
	}

	@Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
	private void hatred$isPushedByFluid(CallbackInfoReturnable<Boolean> info) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && (Object) this == mc.player && NoPush.getInstance().cancelLiquids()) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
	private void hatred$shouldRenderWallhack(double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
		Entity entity = (Entity) (Object) this;
		if (dev.Mardexx.client.modules.impl.Render.WallHack.getInstance().shouldRender(entity)) {
			info.setReturnValue(true);
		}
	}
}


