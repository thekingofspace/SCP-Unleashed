package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SecurityCameraBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<SecurityCameraBlock> BlockCodec = simpleCodec(SecurityCameraBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Hanging = BlockStateProperties.HANGING;
    private static final Map<Direction, VoxelShape> WallOutlines = PropShapes.horizontal(PropShapes.of(box(5, 5, 3, 11, 14, 16)));
    private static final Map<Direction, VoxelShape> HangingOutlines = PropShapes.horizontal(PropShapes.of(box(5, 5, 3, 11, 16, 13)));

    public SecurityCameraBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Hanging, false).setValue(PropWater.Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Hanging, PropWater.Waterlogged);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            BlockState Candidate;
            if (Side == Direction.UP) {
                Candidate = defaultBlockState().setValue(Hanging, true).setValue(Facing, Context.getHorizontalDirection().getOpposite());
            } else if (Side.getAxis().isHorizontal()) {
                Candidate = defaultBlockState().setValue(Hanging, false).setValue(Facing, Side.getOpposite());
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
        return State.getValue(Hanging) ? PropMounts.underCeiling(World, Pos) : PropMounts.onWall(World, Pos, State.getValue(Facing));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        Direction Support = State.getValue(Hanging) ? Direction.UP : State.getValue(Facing).getOpposite();
        if (Side == Support && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Hanging) ? HangingOutlines : WallOutlines).get(State.getValue(Facing));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new SecurityCameraBlockEntity(Pos, State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "mount");
    }
}
