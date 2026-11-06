package net.rai.scp_unleashed.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class WallDeviceBlock extends HorizontalDirectionalBlock {
    protected WallDeviceBlock(Properties Properties) {
        super(Properties);
    }

    protected abstract boolean isEmitting(BlockState State);

    public static BlockPos supportOf(BlockPos Pos, BlockState State) {
        return Pos.relative(State.getValue(FACING).getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Support = supportOf(Pos, State);
        return World.getBlockState(Support).isFaceSturdy(World, Support, State.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (Side.getAxis().isHorizontal()) {
                BlockState State = defaultBlockState().setValue(FACING, Side.getOpposite());
                if (State.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                    return State;
                }
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected boolean isSignalSource(BlockState State) {
        return true;
    }

    @Override
    protected int getSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return isEmitting(State) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return isEmitting(State) && State.getValue(FACING) == Side ? 15 : 0;
    }

    protected void updateNeighbours(BlockState State, Level World, BlockPos Pos) {
        World.updateNeighborsAt(Pos, this);
        World.updateNeighborsAt(supportOf(Pos, State), this);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!MovedByPiston && !State.is(NewState.getBlock()) && isEmitting(State)) {
            updateNeighbours(State, World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(FACING)));
    }
}
