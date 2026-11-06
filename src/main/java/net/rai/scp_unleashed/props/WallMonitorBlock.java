package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WallMonitorBlock extends ScreenBlock {
    public static final MapCodec<WallMonitorBlock> MonitorCodec = simpleCodec(WallMonitorBlock::new);
    public static final BooleanProperty Hanging = BlockStateProperties.HANGING;
    private static final Map<Direction, VoxelShape> HangingOutlines = PropShapes.horizontal(PropShapes.of(box(0, 2, 7, 16, 12, 9), box(2, 12, 7, 3, 16, 8), box(13, 12, 7, 14, 16, 8)));

    public WallMonitorBlock(Properties Properties) {
        super(ScreenKind.WallMonitor, Properties);
        registerDefaultState(defaultBlockState().setValue(Hanging, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return MonitorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Hanging);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            BlockState Candidate;
            if (Side == Direction.UP) {
                Candidate = defaultBlockState().setValue(Hanging, true).setValue(FACING, Context.getHorizontalDirection().getOpposite());
            } else if (Side.getAxis().isHorizontal()) {
                Candidate = defaultBlockState().setValue(Hanging, false).setValue(FACING, Side.getOpposite());
            } else {
                continue;
            }
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return PropWater.placed(Candidate, Context);
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return State.getValue(Hanging) ? PropMounts.underCeiling(World, Pos) : PropMounts.onWall(World, Pos, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        Direction Support = State.getValue(Hanging) ? Direction.UP : State.getValue(FACING).getOpposite();
        if (Side == Support && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Hanging) ? HangingOutlines.get(State.getValue(FACING)) : super.getShape(State, World, Pos, Context);
    }
}
