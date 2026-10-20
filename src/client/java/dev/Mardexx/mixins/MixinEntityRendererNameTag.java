package dev.Mardexx.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.Mardexx.api.system.interfaces.ILivingEntityRenderStateAccess;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.modules.impl.Render.NameTags;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererNameTag {
	@Inject(method = "render", at = @At("HEAD"))
	private void hatred$removeVanillaNameTagAtRender(
			EntityRenderState renderState,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo info
	) {
		if (!(renderState instanceof ILivingEntityRenderStateAccess access)) {
			return;
		}

		LivingEntity entity = access.hatred$getEntity();
		if (NameTags.getInstance().shouldHideVanillaNameTag(entity)) {
			renderState.nameTag = null;
			return;
		}
		if (entity instanceof Player player) {
			String alias = FriendManager.alias(player.getGameProfile().getName());
			if (!alias.isBlank()) {
				renderState.nameTag = Component.literal(alias);
			}
		}
	}
}
