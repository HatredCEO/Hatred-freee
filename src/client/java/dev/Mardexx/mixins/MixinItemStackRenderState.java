package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.WallHack;
import dev.Mardexx.client.render.ChamsRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStackRenderState.class)
public class MixinItemStackRenderState {
	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int hatred$fullBrightEntityItem(int packedLight) {
		boolean fullBright = WallHack.getInstance().enabled() && WallHack.getInstance().fullBright();
		return ChamsRenderTypes.entityChamsActive() && ChamsRenderTypes.currentRenderedEntity() != null && fullBright
				? LightTexture.FULL_BRIGHT
				: packedLight;
	}
}
