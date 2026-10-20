package dev.Mardexx.mixins;

import dev.Mardexx.client.modules.impl.Other.Helper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {
	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	@Inject(method = "render", at = @At("TAIL"))
	private void hatred$renderHelperOverlay(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta, CallbackInfo info) {
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos, topPos, 0.0F);
		try {
			Helper.getInstance().renderContainerOverlay((AbstractContainerScreen<?>) (Object) this, graphics);
		} finally {
			graphics.pose().popPose();
		}
	}

	@Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
	private void hatred$augmentHelperTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> info) {
		info.setReturnValue(Helper.getInstance().augmentTooltip(stack, info.getReturnValue()));
	}
}
