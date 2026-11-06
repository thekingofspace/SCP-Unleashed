package net.rai.scp_unleashed.scp5001.props.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp5001LabPropBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001LabPropBlock> PropCodec = simpleCodec(Props -> new Scp5001LabPropBlock(Scp5001LabProp.Bench, Props));
    private final Scp5001LabProp Entry;
    private final Map<Direction, VoxelShape> Shaped;
    private final boolean Low;

    public Scp5001LabPropBlock(Scp5001LabProp Entry, Properties Props) {
        super(Props);
        this.Entry = Entry;
        this.Shaped = Scp5001LabShapes.horizontal(Entry.lower());
        this.Low = Scp5001LabShapes.low(Entry.lower());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Scp5001LabProp entry() {
        return Entry;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PropCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shaped.get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return Low && Type == PathComputationType.LAND;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }
}
