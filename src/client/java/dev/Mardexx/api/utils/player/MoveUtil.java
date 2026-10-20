package dev.Mardexx.api.utils.player;

import dev.Mardexx.api.system.interfaces.QuickImports;
import net.minecraft.client.KeyMapping;

import java.util.List;

public final class MoveUtil implements QuickImports {
	private MoveUtil() {
	}

	public static List<KeyMapping> getMovementKeys() {
		return List.of(
				mc.options.keyUp,
				mc.options.keyLeft,
				mc.options.keyDown,
				mc.options.keyRight,
				mc.options.keyJump,
				mc.options.keySprint
		);
	}

	public static void updateMovementKeys() {
		KeyMapping.setAll();
	}

	public static void releaseMovementKeys() {
		for (KeyMapping keyMapping : getMovementKeys()) {
			keyMapping.setDown(false);
		}
		mc.options.keyShift.setDown(false);
	}

	public static boolean isMoving() {
		return mc.options.keyUp.isDown()
				|| mc.options.keyLeft.isDown()
				|| mc.options.keyDown.isDown()
				|| mc.options.keyRight.isDown();
	}
}
