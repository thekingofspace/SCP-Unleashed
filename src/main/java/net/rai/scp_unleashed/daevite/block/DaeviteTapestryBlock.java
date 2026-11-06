package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DaeviteTapestryBlock extends DaeviteDecorBlock {
    public static final MapCodec<DaeviteTapestryBlock> TapestryCodec = simpleCodec(DaeviteTapestryBlock::new);

    public DaeviteTapestryBlock(Properties Settings) {
        super(Settings, DaeviteDecor.Tapestry);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return TapestryCodec;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (!Side.getAxis().isHorizontal()) {
                continue;
            }
            BlockState Candidate = defaultBlockState().setValue(FACING, Side.getOpposite());
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return Candidate;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Facing = State.getValue(FACING);
        BlockPos Wall = Pos.relative(Facing.getOpposite());
        return World.getBlockState(Wall).isFaceSturdy(World, Wall, Facing);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        if (Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Other, World, Pos, OtherPos);
    }
}
