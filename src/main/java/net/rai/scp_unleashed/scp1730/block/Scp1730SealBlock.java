package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp1730SealBlock extends Block {
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;

    public Scp1730SealBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Axis, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Axis);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Axis, Context.getHorizontalDirection().getClockWise().getAxis());
    }
}
