package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp407BloomBlock extends Scp407GrowthBlock {
    public static final DirectionProperty Attached = DirectionProperty.create("attached");
    private static final Map<Direction, VoxelShape> Shapes = facing(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public Scp407BloomBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Wither, 0).setValue(Attached, Direction.DOWN).setValue(Carpet, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Attached, Carpet);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Attached));
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return supported(World, Pos, State.getValue(Attached));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == State.getValue(Attached) && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(refreshCarpet(State, Side, World, Pos), Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    public Direction base(BlockState State) {
        return State.getValue(Attached);
    }

    @Override
    protected float sporeChance(BlockState State) {
        return 0.45f;
    }

    @Override
    protected float glintChance(BlockState State) {
        return 0.3f;
    }

    @Override
    protected float puffChance(BlockState State) {
        return 0.012f;
    }
}
