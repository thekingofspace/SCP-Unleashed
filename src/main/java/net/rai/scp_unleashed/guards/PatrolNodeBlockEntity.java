package net.rai.scp_unleashed.guards;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PatrolNodeBlockEntity extends BlockEntity {
    public PatrolNodeBlockEntity(BlockPos Pos, BlockState State) {
        super(GuardsRegistry.PatrolNodeEntity.get(), Pos, State);
    }

    @Override
    public boolean isValidBlockState(BlockState State) {
        return super.isValidBlockState(State) && State.getValue(PatrolNodeBlock.Hidden);
    }
}
