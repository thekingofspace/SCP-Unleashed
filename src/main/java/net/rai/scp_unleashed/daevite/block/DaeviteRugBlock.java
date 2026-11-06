package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DaeviteRugBlock extends DaeviteDecorBlock {
    public static final MapCodec<DaeviteRugBlock> RugCodec = simpleCodec(DaeviteRugBlock::new);

    public DaeviteRugBlock(Properties Settings) {
        super(Settings, DaeviteDecor.Rug);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return RugCodec;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return !World.isEmptyBlock(Pos.below());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        if (Side == Direction.DOWN && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Other, World, Pos, OtherPos);
    }
}
