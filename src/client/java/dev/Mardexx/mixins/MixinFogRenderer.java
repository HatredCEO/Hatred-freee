package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.Removals;
import dev.Mardexx.client.modules.impl.Render.WorldTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {
	@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private static void hatred$removeFog(
			Camera camera,
			FogRenderer.FogMode fogMode,
			Vector4f fogColor,
			float viewDistance,
			boolean thickFog,
			float tickDelta,
			CallbackInfoReturnable<FogParameters> info
	) {
		Removals removals = Removals.getInstance();
		FogType fluid = camera.getFluidInCamera();
		boolean removeFluidFog = fluid == FogType.WATER && removals.removesWaterFog()
				|| fluid == FogType.LAVA && removals.removesLavaFog()
				|| fluid != FogType.WATER && fluid != FogType.LAVA && removals.removesFog();

		boolean removeEffectFog = camera.getEntity() instanceof LivingEntity entity
				&& (removals.removesWardenEffect() && entity.hasEffect(MobEffects.DARKNESS)
				|| removals.removesBadEffects() && entity.hasEffect(MobEffects.BLINDNESS));
		if (removeFluidFog || removeEffectFog) {
			info.setReturnValue(FogParameters.NO_FOG);
			return;
		}

		WorldTweaks worldTweaks = WorldTweaks.getInstance();
		if (worldTweaks.changesFog() && fluid != FogType.WATER && fluid != FogType.LAVA) {
			float end = worldTweaks.fogDistance();
			float start = end * 0.75F;
			if (worldTweaks.colorMode() == WorldTweaks.ColorMode.VANILLA) {
				info.setReturnValue(new FogParameters(
						start,
						end,
						FogShape.SPHERE,
						fogColor.x,
						fogColor.y,
						fogColor.z,
						fogColor.w
				));
			} else {
				int color = worldTweaks.fogColor();
				info.setReturnValue(new FogParameters(
						start,
						end,
						FogShape.SPHERE,
						((color >>> 16) & 255) / 255.0F,
						((color >>> 8) & 255) / 255.0F,
						(color & 255) / 255.0F,
						1.0F
				));
			}
		}
	}
}
