package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp5001PillarBlock extends Block {
    public static final VoxelShape Shape = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public Scp5001PillarBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }
}
