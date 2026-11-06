package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface WireHostBlock {
    BlockPos devicePos(BlockState State, BlockPos Pos);
}
