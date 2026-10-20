package dev.Mardexx.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.Mardexx.api.system.interfaces.ILivingEntityRenderStateAccess;
import dev.Mardexx.client.modules.impl.Render.NameTags;
import dev.Mardexx.client.modules.impl.Render.WallHack;
import dev.Mardexx.client.render.ChamsModelRenderHelper;
import dev.Mardexx.client.render.ChamsRenderTypes;
import dev.Mardexx.client.render.ChamsDepthCapture;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
	private static final ThreadLocal<RenderType> HATRED$CURRENT_BODY_RENDER_TYPE = new ThreadLocal<>();

	@Shadow
	protected EntityModel<?> model;

	@Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
	private void hatred$extractRenderState(LivingEntity livingEntity, LivingEntityRenderState renderState, float partialTick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo info) {
		((ILivingEntityRenderStateAccess) renderState).hatred$setEntity(livingEntity);
	}

	@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
	private void hatred$hideVanillaNameTag(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> info) {
		if (NameTags.getInstance().shouldHideVanillaNameTag(entity)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void hatred$renderHead(LivingEntityRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo info) {
		LivingEntity entity = ((ILivingEntityRenderStateAccess) renderState).hatred$getEntity();

		WallHack wallHack = WallHack.getInstance();
		boolean wallHackEnabled = wallHack.shouldRender(entity);
		boolean enabled = wallHackEnabled;

		boolean dynamicOcclusion = enabled && ChamsDepthCapture.available();
		boolean throughWalls = dynamicOcclusion || (wallHackEnabled && wallHack.isBehindWall(entity));

		int visibleColor = 0xFFFFFFFF;
		int hiddenColor = 0xFFFFFFFF;
		float visibleIntensity = 0.0F;
		float hiddenIntensity = 0.0F;
		boolean fullBright = wallHackEnabled && wallHack.fullBright();
		boolean armorThroughWalls = wallHackEnabled;

		ChamsRenderTypes.setEntityChams(
				enabled,
				throughWalls,
				armorThroughWalls,
				entity instanceof Player,
				visibleColor,
				hiddenColor,
				visibleIntensity,
				hiddenIntensity,
				fullBright,
				entity
		);
		ChamsRenderTypes.setDynamicOcclusion(dynamicOcclusion);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void hatred$renderReturn(LivingEntityRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo info) {
		flushCurrentBodyRenderType(bufferSource);
		HATRED$CURRENT_BODY_RENDER_TYPE.remove();
		ChamsRenderTypes.clearEntityChams();
	}

	@Redirect(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
			)
	)
	private void hatred$renderHiddenPlayerBodyInStableOrder(
			EntityModel<?> entityModel,
			PoseStack poseStack,
			VertexConsumer vertexConsumer,
			int packedLight,
			int overlay,
			int color,
			LivingEntityRenderState renderState,
			PoseStack renderPoseStack,
			MultiBufferSource bufferSource,
			int renderPackedLight
	) {
		LivingEntity entity = ((ILivingEntityRenderStateAccess) renderState).hatred$getEntity();
		if (ChamsRenderTypes.dynamicOcclusionActive()) {
			net.minecraft.resources.ResourceLocation texture = ((LivingEntityRenderer) (Object) this).getTextureLocation(renderState);
			RenderType dynamicRenderType = ChamsRenderTypes.entityChams(texture, true, ChamsRenderTypes.entityHiddenColor());
			VertexConsumer dynamicConsumer = bufferSource.getBuffer(dynamicRenderType);
			entityModel.renderToBuffer(poseStack, dynamicConsumer, packedLight, overlay, color);
			flushRenderType(bufferSource, dynamicRenderType);
			return;
		}

		entityModel.renderToBuffer(poseStack, vertexConsumer, packedLight, overlay, color);
	}

	@Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
	private void hatred$getRenderType(LivingEntityRenderState renderState, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> info) {
		RenderType renderType = info.getReturnValue();
		if (renderType != null && ChamsRenderTypes.entityChamsActive()) {
			RenderType chamsRenderType = ChamsRenderTypes.entityChams(
					((LivingEntityRenderer) (Object) this).getTextureLocation(renderState),
					ChamsRenderTypes.entityThroughWalls(),
					ChamsRenderTypes.entityHiddenColor()
			);
			HATRED$CURRENT_BODY_RENDER_TYPE.set(chamsRenderType);
			info.setReturnValue(chamsRenderType);
			return;
		}

		HATRED$CURRENT_BODY_RENDER_TYPE.remove();
	}

	private static void flushCurrentBodyRenderType(MultiBufferSource bufferSource) {
		RenderType renderType = HATRED$CURRENT_BODY_RENDER_TYPE.get();
		if (renderType == null) {
			return;
		}

		if (bufferSource instanceof MultiBufferSource.BufferSource bufferSourceImpl) {
			bufferSourceImpl.endBatch(renderType);
			return;
		}

		if (bufferSource instanceof OutlineBufferSource outlineBufferSource) {
			((MixinOutlineBufferSourceAccessor) outlineBufferSource).hatred$getBufferSource().endBatch(renderType);
			outlineBufferSource.endOutlineBatch();
		}
	}

	private static void flushRenderType(MultiBufferSource bufferSource, RenderType renderType) {
		if (bufferSource instanceof MultiBufferSource.BufferSource bufferSourceImpl) {
			bufferSourceImpl.endBatch(renderType);
			return;
		}

		if (bufferSource instanceof OutlineBufferSource outlineBufferSource) {
			((MixinOutlineBufferSourceAccessor) outlineBufferSource).hatred$getBufferSource().endBatch(renderType);
			outlineBufferSource.endOutlineBatch();
		}
	}
}
