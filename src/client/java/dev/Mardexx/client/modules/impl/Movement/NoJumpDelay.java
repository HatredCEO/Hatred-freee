package dev.Mardexx.client.modules.impl.Movement;

import dev.Mardexx.api.system.interfaces.IJumpingCooldown;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;

public class NoJumpDelay extends Module implements QuickImports {
	public NoJumpDelay() {
		super("No Jump Delay", ModuleCategory.MOVEMENT, "Removes the delay between jumps.");
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) {
			return;
		}

		((IJumpingCooldown) mc.player).hatred$setJumpingCooldown(0);
	}
}
