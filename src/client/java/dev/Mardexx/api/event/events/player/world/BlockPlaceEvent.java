package dev.Mardexx.api.event.events.player.world;

import dev.Mardexx.api.event.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockPlaceEvent extends Event<BlockPlaceEvent.Data> {
	public static final BlockPlaceEvent INSTANCE = new BlockPlaceEvent();

	private BlockPlaceEvent() {
	}

	public record Data(Block block, BlockState state, BlockPos pos, LivingEntity placer) {
	}
}
