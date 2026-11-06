package net.rai.scp_unleashed.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.pressure.DuctKind;

public interface DuctConnectable {
    boolean acceptsDuct(BlockState State, Direction Side, DuctKind Kind);
}
