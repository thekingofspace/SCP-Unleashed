package net.rai.scp_unleashed.props;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class DeskBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<DeskBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            DeskKind.KindCodec.fieldOf("kind").forGetter(DeskBlock::getKind), propertiesCodec()).apply(Instance, DeskBlock::new));
    public static final BooleanProperty Left = BooleanProperty.create("left");
    public static final BooleanProperty Right = BooleanProperty.create("right");

    private final DeskKind Kind;
    private final ImmutableMap<BlockState, VoxelShape> Outlines;

    public DeskBlock(DeskKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Left, false).setValue(Right, false).setValue(PropWater.Waterlogged, false));
        this.Outlines = getShapeForEachState(this::outline);
    }

    public DeskKind getKind() {
        return Kind;
    }

    private VoxelShape outline(BlockState State) {
        VoxelShape North = Kind.getTop();
        if (!State.getValue(Left)) {
            North = PropShapes.of(North, Kind.getLeftEnd());
        }
        if (!State.getValue(Right)) {
            North = PropShapes.of(North, Kind.getRightEnd());
        }
        return PropShapes.turn(North, State.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Left, Right, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        return PropWater.placed(connected(State, Context.getLevel(), Context.getClickedPos()), Context);
    }

    private BlockState connected(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction Facing = State.getValue(FACING);
        return State.setValue(Left, joins(World, Pos, Facing, true)).setValue(Right, joins(World, Pos, Facing, false));
    }

    private boolean joins(BlockGetter World, BlockPos Pos, Direction Facing, boolean LeftSide) {
        Direction Side = LeftSide ? Facing.getClockWise() : Facing.getCounterClockWise();
        BlockState Other = World.getBlockState(Pos.relative(Side));
        if (Other.is(this)) {
            return Other.getValue(FACING) == Facing;
        }
        if (Kind.joinsCorners() && Other.getBlock() instanceof CornerDeskBlock) {
            Direction Corner = Other.getValue(CornerDeskBlock.FACING);
            return LeftSide ? Corner == Facing : Corner == Facing.getClockWise();
        }
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        PropWater.update(State, World, Pos);
        if (Side.getAxis().isHorizontal()) {
            return connected(State, World, Pos);
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "joins");
    }
}
