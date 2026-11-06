package net.rai.scp_unleashed.scp5001.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class Scp5001FacingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001FacingBlock> BlockCodec = simpleCodec(Scp5001FacingBlock::new);

    public Scp5001FacingBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
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
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Scp5001WallFaces.occlusion(State);
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return World.getMaxLightLevel();
    }
}
