package net.rai.scp_unleashed.scp420j;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp420jHempBlock extends CropBlock {
    public static final MapCodec<Scp420jHempBlock> HempCodec = simpleCodec(Scp420jHempBlock::new);
    public static final int[] StageHeights = {3, 5, 7, 9, 11, 13, 15, 16};
    private static final VoxelShape[] Shapes = shapes();

    public Scp420jHempBlock(Properties Settings) {
        super(Settings);
    }

    private static VoxelShape[] shapes() {
        VoxelShape[] Made = new VoxelShape[StageHeights.length];
        for (int Stage = 0; Stage < StageHeights.length; Stage++) {
            Made[Stage] = Block.box(2.0, 0.0, 2.0, 14.0, StageHeights[Stage], 14.0);
        }
        return Made;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return HempCodec;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Scp420jRegistry.Seeds.get();
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes[Math.min(getAge(State), Shapes.length - 1)];
    }
}
