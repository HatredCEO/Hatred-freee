package dev.Mardexx.client.render;

import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.render.Render3D;
import dev.Mardexx.api.utils.render.ProjectionUtil;
import dev.Mardexx.client.modules.impl.Combat.AimAssist;
import dev.Mardexx.client.modules.impl.Combat.Tap;
import dev.Mardexx.client.modules.impl.Combat.TriggerBot;
import dev.Mardexx.client.modules.impl.Render.WallHack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class ClientRenderManager {
	private ClientRenderManager() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> Render2D.render(guiGraphics, tickCounter.getGameTimeDeltaPartialTick(false)));
		WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
			if (WallHack.getInstance().enabled()) {
				ChamsDepthCapture.capture();
			} else {
				ChamsDepthCapture.invalidate();
			}
		});
		WorldRenderEvents.LAST.register(AuroraSkyRenderer::render);
		WorldRenderEvents.LAST.register(context -> ProjectionUtil.capture(context.projectionMatrix(), context.positionMatrix()));
		WorldRenderEvents.LAST.register(AimAssist.getInstance()::onRenderFrame);
		WorldRenderEvents.LAST.register(TriggerBot.getInstance()::onRenderFrame);
		WorldRenderEvents.LAST.register(Tap.getInstance()::onRenderFrame);
		WorldRenderEvents.LAST.register(Render3D::render);
	}
}
