package net.rai.scp_unleashed.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FacilityWindowBlock extends FacilityWallPieceBlock {
    public static final MapCodec<FacilityWindowBlock> WindowCodec = simpleCodec(FacilityWindowBlock::new);
    public static final int SlotBottom = 7;
    public static final int SlotTop = 11;
    private static final VoxelShape Frame = Shapes.or(Block.box(0, 0, 0, 16, SlotBottom, 16), Block.box(0, SlotTop, 0, 16, 16, 16));

    public FacilityWindowBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return WindowCodec;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Frame;
    }
}
