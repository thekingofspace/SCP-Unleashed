package net.rai.scp_unleashed.props;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WallClockBlockEntity extends BlockEntity {
    public WallClockBlockEntity(BlockPos Pos, BlockState State) {
        super(PropsRegistry.WallClockEntity.get(), Pos, State);
    }
}
