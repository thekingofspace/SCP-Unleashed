package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp5001WallBlock extends Block {
    public Scp5001WallBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Scp5001WallFaces.occlusion(State);
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return World.getMaxLightLevel();
    }
}
