package dev.Mardexx.client.modules.impl.Movement;

import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;

public class Sprint extends Module implements QuickImports {
	public Sprint() {
		super("Sprint", ModuleCategory.MOVEMENT, "Automatically activates sprint when the player starts moving.");
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			return;
		}

		boolean moving = mc.options.keyUp.isDown()
				|| mc.options.keyLeft.isDown()
				|| mc.options.keyRight.isDown();
		if (!moving || mc.player.isCrouching() || mc.player.isUsingItem()) {
			return;
		}

		mc.player.setSprinting(true);
	}
}
