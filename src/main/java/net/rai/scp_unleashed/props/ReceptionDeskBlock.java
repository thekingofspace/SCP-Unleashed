package net.rai.scp_unleashed.props;

import com.google.common.collect.ImmutableMap;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class ReceptionDeskBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ReceptionDeskBlock> BlockCodec = simpleCodec(ReceptionDeskBlock::new);
    public static final EnumProperty<StairsShape> Outline = BlockStateProperties.STAIRS_SHAPE;
    public static final BooleanProperty Left = DeskBlock.Left;
    public static final BooleanProperty Right = DeskBlock.Right;

    private static final VoxelShape Straight = PropShapes.of(box(0, 0, 0, 16, 18, 3), box(0, 18, 0, 16, 20, 5), box(0, 14, 3, 16, 16, 16));
    private static final VoxelShape LeftCap = PropShapes.of(box(15, 0, 3, 16, 14, 16));
    private static final VoxelShape RightCap = PropShapes.of(box(0, 0, 3, 1, 14, 16));
    private static final VoxelShape InnerLeft = PropShapes.of(box(0, 0, 0, 16, 18, 3), box(0, 0, 3, 3, 18, 16), box(0, 18, 0, 16, 20, 5), box(0, 18, 5, 5, 20, 16), box(3, 14, 3, 16, 16, 16));
    private static final VoxelShape InnerRight = PropShapes.of(box(0, 0, 0, 16, 18, 3), box(13, 0, 3, 16, 18, 16), box(0, 18, 0, 16, 20, 5), box(11, 18, 5, 16, 20, 16), box(0, 14, 3, 13, 16, 16));
    private static final VoxelShape OuterLeft = PropShapes.of(box(0, 0, 0, 3, 18, 3), box(0, 18, 0, 5, 20, 5), box(3, 14, 0, 16, 16, 3), box(0, 14, 3, 16, 16, 16));
    private static final VoxelShape OuterRight = PropShapes.of(box(13, 0, 0, 16, 18, 3), box(11, 18, 0, 16, 20, 5), box(0, 14, 0, 13, 16, 3), box(0, 14, 3, 16, 16, 16));

    private final ImmutableMap<BlockState, VoxelShape> Outlines;

    public ReceptionDeskBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Outline, StairsShape.STRAIGHT)
                .setValue(Left, false).setValue(Right, false).setValue(PropWater.Waterlogged, false));
        this.Outlines = getShapeForEachState(ReceptionDeskBlock::outline);
    }

    private static VoxelShape outline(BlockState State) {
        VoxelShape North = switch (State.getValue(Outline)) {
            case INNER_LEFT -> InnerLeft;
            case INNER_RIGHT -> InnerRight;
            case OUTER_LEFT -> OuterLeft;
            case OUTER_RIGHT -> OuterRight;
            default -> {
                VoxelShape Run = Straight;
                if (!State.getValue(Left)) {
                    Run = PropShapes.of(Run, LeftCap);
                }
                if (!State.getValue(Right)) {
                    Run = PropShapes.of(Run, RightCap);
                }
                yield Run;
            }
        };
        return PropShapes.turn(North, State.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Outline, Left, Right, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        return PropWater.placed(shaped(State, Context.getLevel(), Context.getClickedPos()), Context);
    }

    private static boolean isDesk(BlockState State) {
        return State.getBlock() instanceof ReceptionDeskBlock;
    }

    private static BlockState shaped(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction Facing = State.getValue(FACING);
        return State.setValue(Outline, cornerShape(State, World, Pos)).setValue(Left, joins(World, Pos, Facing, Facing.getClockWise()))
                .setValue(Right, joins(World, Pos, Facing, Facing.getCounterClockWise()));
    }

    private static boolean joins(BlockGetter World, BlockPos Pos, Direction Facing, Direction Side) {
        BlockState Other = World.getBlockState(Pos.relative(Side));
        return isDesk(Other) && (Other.getValue(FACING) == Facing || Other.getValue(Outline) != StairsShape.STRAIGHT);
    }

    private static StairsShape cornerShape(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction Facing = State.getValue(FACING);
        BlockState Front = World.getBlockState(Pos.relative(Facing));
        if (isDesk(Front)) {
            Direction FrontFacing = Front.getValue(FACING);
            if (FrontFacing.getAxis() != Facing.getAxis() && canTakeShape(State, World, Pos, FrontFacing.getOpposite())) {
                return FrontFacing == Facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
            }
        }
        BlockState Back = World.getBlockState(Pos.relative(Facing.getOpposite()));
        if (isDesk(Back)) {
            Direction BackFacing = Back.getValue(FACING);
            if (BackFacing.getAxis() != Facing.getAxis() && canTakeShape(State, World, Pos, BackFacing)) {
                return BackFacing == Facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
            }
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        BlockState Other = World.getBlockState(Pos.relative(Side));
        return !isDesk(Other) || Other.getValue(FACING) != State.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        PropWater.update(State, World, Pos);
        if (Side.getAxis().isHorizontal()) {
            return shaped(State, World, Pos);
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "joins", "corners");
    }
}
