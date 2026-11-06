package net.rai.scp_unleashed.furnishing;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public class CouchBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, SeatHost {
    public static final MapCodec<CouchBlock> BlockCodec = simpleCodec(CouchBlock::new);
    public static final EnumProperty<CouchShape> Shape = EnumProperty.create("shape", CouchShape.class);
    public static final BooleanProperty Left = BooleanProperty.create("left");
    public static final BooleanProperty Right = BooleanProperty.create("right");
    public static final double SeatHeight = 8.0 / 16.0;
    private static final VoxelShape StraightBody = FurnishingShapes.of(box(0, 0, 0, 16, 8, 12), box(0, 0, 12, 16, 15, 16));
    private static final VoxelShape CornerBody = FurnishingShapes.of(box(0, 0, 0, 12, 8, 12), box(0, 0, 12, 16, 15, 16), box(12, 0, 0, 16, 15, 12));
    private static final VoxelShape WestArm = box(0, 8, 0, 3, 11, 12);
    private static final VoxelShape EastArm = box(13, 8, 0, 16, 11, 12);

    private final ImmutableMap<BlockState, VoxelShape> Outlines;

    public CouchBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Shape, CouchShape.Straight).setValue(Left, false).setValue(Right, false)
                .setValue(FurnishingWater.Waterlogged, false));
        this.Outlines = getShapeForEachState(CouchBlock::outline);
    }

    private static VoxelShape outline(BlockState State) {
        boolean Left0 = State.getValue(Left);
        boolean Right0 = State.getValue(Right);
        VoxelShape North = switch (State.getValue(Shape)) {
            case CornerRight -> Left0 ? CornerBody : FurnishingShapes.of(CornerBody, WestArm);
            case CornerLeft -> FurnishingShapes.mirror(Right0 ? CornerBody : FurnishingShapes.of(CornerBody, WestArm));
            default -> {
                VoxelShape Body = StraightBody;
                if (!Left0) {
                    Body = FurnishingShapes.of(Body, WestArm);
                }
                if (!Right0) {
                    Body = FurnishingShapes.of(Body, EastArm);
                }
                yield Body;
            }
        };
        return FurnishingShapes.turn(North, State.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Shape, Left, Right, FurnishingWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        return FurnishingWater.placed(shaped(State, Context.getLevel(), Context.getClickedPos()), Context);
    }

    private static boolean isCouch(BlockState State, BlockState Other) {
        return Other.is(State.getBlock());
    }

    private static List<Direction> backrests(BlockState State) {
        Direction Facing = State.getValue(FACING);
        return switch (State.getValue(Shape)) {
            case CornerLeft -> List.of(Facing.getOpposite(), Facing.getCounterClockWise());
            case CornerRight -> List.of(Facing.getOpposite(), Facing.getClockWise());
            default -> List.of(Facing.getOpposite());
        };
    }

    private static boolean opens(BlockState State, Direction Side) {
        Direction Facing = State.getValue(FACING);
        return switch (State.getValue(Shape)) {
            case CornerLeft -> Side == Facing || Side == Facing.getClockWise();
            case CornerRight -> Side == Facing || Side == Facing.getCounterClockWise();
            default -> Side == Facing.getClockWise() || Side == Facing.getCounterClockWise();
        };
    }

    private static boolean joins(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        BlockState Other = World.getBlockState(Pos.relative(Side));
        if (!isCouch(State, Other) || !opens(State, Side) || !opens(Other, Side.getOpposite())) {
            return false;
        }
        List<Direction> Theirs = backrests(Other);
        for (Direction Back : backrests(State)) {
            if (Back.getAxis() != Side.getAxis() && Theirs.contains(Back)) {
                return true;
            }
        }
        return false;
    }

    private static CouchShape cornerShape(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction Facing = State.getValue(FACING);
        BlockState Front = World.getBlockState(Pos.relative(Facing));
        if (isCouch(State, Front)) {
            Direction FrontFacing = Front.getValue(FACING);
            if (FrontFacing.getAxis() != Facing.getAxis()) {
                Direction Back = FrontFacing.getOpposite();
                BlockState Behind = World.getBlockState(Pos.relative(Back));
                if (!isCouch(State, Behind) || Behind.getValue(FACING) != Facing) {
                    return Back == Facing.getClockWise() ? CouchShape.CornerRight : CouchShape.CornerLeft;
                }
            }
        }
        return CouchShape.Straight;
    }

    private static BlockState shaped(BlockState State, BlockGetter World, BlockPos Pos) {
        BlockState Shaped = State.setValue(Shape, cornerShape(State, World, Pos));
        Direction Facing = Shaped.getValue(FACING);
        return Shaped.setValue(Left, joins(Shaped, World, Pos, Facing.getCounterClockWise())).setValue(Right, joins(Shaped, World, Pos, Facing.getClockWise()));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        FurnishingWater.update(State, World, Pos);
        if (Side.getAxis().isHorizontal()) {
            return shaped(State, World, Pos);
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return FurnishingWater.fluid(State);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        return Seating.sit(World, Pos, State, User);
    }

    @Override
    public double seatHeight(BlockState State) {
        return SeatHeight;
    }

    @Override
    public float seatYaw(BlockState State, BlockPos Pos, Player User) {
        float Front = State.getValue(FACING).toYRot();
        return switch (State.getValue(Shape)) {
            case CornerLeft -> Front + 45.0f;
            case CornerRight -> Front - 45.0f;
            default -> Front;
        };
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        FurnishingTooltips.add(this, Lines, "joins", "corners", "sit");
    }
}
