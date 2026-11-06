package net.rai.scp_unleashed.scp407;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class Scp407HangingBlock extends Scp407PlantBlock {
    public Scp407HangingBlock(Properties Settings, Traits Kind) {
        super(Settings, Kind);
    }

    @Override
    public Direction base(BlockState State) {
        return Direction.UP;
    }
}
