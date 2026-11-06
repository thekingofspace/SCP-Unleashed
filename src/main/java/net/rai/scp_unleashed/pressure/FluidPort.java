package net.rai.scp_unleashed.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public interface FluidPort {
    @Nullable
    PortRole portRole(BlockState State, Direction Side);

    @Nullable
    IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State);

    BlockPos portOwner(Level World, BlockPos Pos, BlockState State);
}
