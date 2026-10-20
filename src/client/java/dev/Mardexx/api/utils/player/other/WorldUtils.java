package dev.Mardexx.api.utils.player.other;

import dev.Mardexx.api.system.interfaces.QuickImports;
import net.minecraft.world.level.block.Blocks;

public final class WorldUtils implements QuickImports {
	private WorldUtils() {
	}

	public static boolean isInWeb() {
		return mc.player != null
				&& mc.level != null
				&& mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB);
	}
}
