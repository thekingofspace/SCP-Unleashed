package net.rai.scp_unleashed.props;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class PropWater {
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;

    private PropWater() {
    }

    public static BlockState placed(BlockState State, BlockPlaceContext Context) {
        return State.setValue(Waterlogged, Context.getLevel().getFluidState(Context.getClickedPos()).getType() == Fluids.WATER);
    }

    public static void update(BlockState State, LevelAccessor World, BlockPos Pos) {
        if (State.hasProperty(Waterlogged) && State.getValue(Waterlogged)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
    }

    public static FluidState fluid(BlockState State) {
        return State.hasProperty(Waterlogged) && State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }
}
