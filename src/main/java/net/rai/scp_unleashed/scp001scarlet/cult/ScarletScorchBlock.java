package net.rai.scp_unleashed.scp001scarlet.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScarletScorchBlock extends Block {
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 8);
    public static final VoxelShape Shape = Block.box(0.0, 0.0, 0.0, 16.0, 0.5, 16.0);

    public ScarletScorchBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Part, 4));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Part);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return Side == Direction.DOWN && !canSurvive(State, World, Pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    public static BlockState part(Block Owner, int Dx, int Dz) {
        return Owner.defaultBlockState().setValue(Part, (Dz + 1) * 3 + (Dx + 1));
    }
}
