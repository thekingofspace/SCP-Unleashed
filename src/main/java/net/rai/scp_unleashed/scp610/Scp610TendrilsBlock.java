package net.rai.scp_unleashed.scp610;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp610TendrilsBlock extends BushBlock {
    public static final MapCodec<Scp610TendrilsBlock> Codec0 = simpleCodec(Scp610TendrilsBlock::new);
    public static final VoxelShape Shape = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public Scp610TendrilsBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return Codec0;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState Below, BlockGetter World, BlockPos Pos) {
        return Scp610Growth.isFlesh(Below) && !(Below.getBlock() instanceof Scp610TendrilsBlock);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return mayPlaceOn(World.getBlockState(Pos.below()), World, Pos.below());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return !State.canSurvive(World, Pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }
}
