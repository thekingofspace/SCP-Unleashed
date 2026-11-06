package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class Scp3008ArrowBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008ArrowBlock> ArrowCodec = simpleCodec(Scp3008ArrowBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;

    public Scp3008ArrowBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Scp3008ArrowBlock> codec() {
        return ArrowCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
