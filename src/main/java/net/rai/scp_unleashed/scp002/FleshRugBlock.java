package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FleshRugBlock extends FleshFixtureBlock {
    public static final MapCodec<FleshRugBlock> RugCodec = simpleCodec(FleshRugBlock::new);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 5);
    private static final VoxelShape Flat = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public FleshRugBlock(Properties Properties) {
        super(Properties, Fixture.Rug);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, 0));
    }

    @Override
    protected MapCodec<? extends FleshRugBlock> codec() {
        return RugCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Flat;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Mode) {
        return Mode == PathComputationType.LAND;
    }
}
