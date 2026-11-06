package net.rai.scp_unleashed.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.gas.GasHandler;
import org.jetbrains.annotations.Nullable;

public interface GasPort {
    @Nullable
    PortRole gasRole(BlockState State, Direction Side);

    @Nullable
    GasHandler gasHandler(Level World, BlockPos Pos, BlockState State);

    default BlockPos gasOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    default boolean exhausts(BlockState State) {
        return false;
    }
}
