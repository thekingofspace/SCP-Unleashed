package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface WireConnector {
    ConnectorTier getConnectorTier();

    boolean isFullBlock();

    Direction linkFace(BlockState State, Direction Clicked);

    BlockPos hostPos(BlockState State, BlockPos Pos);
}
