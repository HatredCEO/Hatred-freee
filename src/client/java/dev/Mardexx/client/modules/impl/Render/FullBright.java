package dev.Mardexx.client.modules.impl.Render;

import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FullBright extends Module implements QuickImports {
	private static final FullBright INSTANCE = new FullBright();
	private static final int NIGHT_VISION_DURATION = 20 * 60 * 60;

	private FullBright() {
		super("Full Bright", ModuleCategory.RENDER, "Makes dark areas fully visible.");
	}

	@Override
	public void onTick() {
		applyNightVision();
	}

	@Override
	protected void onDisable() {
		if (mc.player != null) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}

	private void applyNightVision() {
		if (mc.player == null || mc.level == null) {
			return;
		}

		mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false, false));
	}

	public static FullBright getInstance() {
		return INSTANCE;
	}
}
