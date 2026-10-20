package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Render.CustomSwords;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public abstract class MixinItemModelResolverCustomSwords {
	@ModifyVariable(
			method = "updateForTopItem",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 0
	)
	private ItemStack hatred$customSwordsModel(ItemStack stack) {
		return CustomSwords.getInstance().renderStack(stack);
	}
}
