package net.rai.scp_unleashed.facilitysign;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class FacilityHeaderBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FacilityHeaderBlock> BlockCodec = simpleCodec(FacilityHeaderBlock::new);
    public static final EnumProperty<FacilityHeaderPart> Part = EnumProperty.create("part", FacilityHeaderPart.class);
    public static final EnumProperty<FacilitySignMount> Mount = FacilitySignBlock.Mount;
    private static final Map<Direction, VoxelShape> WallLeft = PropShapes.horizontal(PropShapes.box(-32, 1, 14.5, 16, 15, 16));
    private static final Map<Direction, VoxelShape> WallCentre = PropShapes.horizontal(PropShapes.box(-16, 1, 14.5, 32, 15, 16));
    private static final Map<Direction, VoxelShape> WallRight = PropShapes.horizontal(PropShapes.box(0, 1, 14.5, 48, 15, 16));
    private static final Map<Direction, VoxelShape> WallCell = PropShapes.horizontal(PropShapes.box(0, 1, 14.5, 16, 15, 16));
    private static final Map<Direction, VoxelShape> HungLeft = PropShapes.horizontal(PropShapes.of(PropShapes.box(-32, 0, 7, 16, 14, 9), PropShapes.box(9.5, 14, 7.5, 10.5, 16, 8.5)));
    private static final Map<Direction, VoxelShape> HungCentre = PropShapes.horizontal(PropShapes.box(-16, 0, 7, 32, 14, 9));
    private static final Map<Direction, VoxelShape> HungRight = PropShapes.horizontal(PropShapes.of(PropShapes.box(0, 0, 7, 48, 14, 9), PropShapes.box(5.5, 14, 7.5, 6.5, 16, 8.5)));
    private static final Map<Direction, VoxelShape> HungCell = PropShapes.horizontal(PropShapes.box(0, 0, 7, 16, 14, 9));

    public FacilityHeaderBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, FacilityHeaderPart.Centre).setValue(Mount, FacilitySignMount.Wall));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Part, Mount);
    }

    public static BlockPos centrePos(BlockState State, BlockPos Pos) {
        Direction Right = FacilitySignSupport.rightOf(State.getValue(FACING));
        return switch (State.getValue(Part)) {
            case Left -> Pos.relative(Right);
            case Right -> Pos.relative(Right.getOpposite());
            case Centre -> Pos;
        };
    }

    public static List<BlockPos> parts(BlockState State, BlockPos Pos) {
        BlockPos Centre = centrePos(State, Pos);
        Direction Right = FacilitySignSupport.rightOf(State.getValue(FACING));
        return List.of(Centre.relative(Right.getOpposite()), Centre, Centre.relative(Right));
    }

    private static boolean supported(LevelReader World, BlockPos Pos, Direction Facing, FacilitySignMount Hung) {
        return Hung == FacilitySignMount.Ceiling ? FacilitySignSupport.underCeiling(World, Pos) : FacilitySignSupport.onWall(World, Pos, Facing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Clicked = Context.getClickedPos();
        boolean AnySupport = false;
        boolean Overhead = Context.getClickedFace() == Direction.DOWN;
        Direction Toward = Context.getHorizontalDirection().getOpposite();
        if (Overhead && FacilitySignSupport.underCeiling(World, Clicked)) {
            AnySupport = true;
            if (fitsAround(Context, Toward, FacilitySignMount.Ceiling)) {
                return centre(Toward, FacilitySignMount.Ceiling);
            }
        }
        for (Direction Facing : FacilitySignSupport.facingChoices(Context)) {
            if (!FacilitySignSupport.onWall(World, Clicked, Facing)) {
                continue;
            }
            AnySupport = true;
            if (fitsAround(Context, Facing, FacilitySignMount.Wall)) {
                return centre(Facing, FacilitySignMount.Wall);
            }
        }
        if (!Overhead && FacilitySignSupport.underCeiling(World, Clicked)) {
            AnySupport = true;
            if (fitsAround(Context, Toward, FacilitySignMount.Ceiling)) {
                return centre(Toward, FacilitySignMount.Ceiling);
            }
        }
        FacilitySignSupport.refuse(Context, AnySupport ? "no_room_header" : "no_support");
        return null;
    }

    private BlockState centre(Direction Facing, FacilitySignMount Hung) {
        return defaultBlockState().setValue(FACING, Facing).setValue(Part, FacilityHeaderPart.Centre).setValue(Mount, Hung);
    }

    private static boolean fitsAround(BlockPlaceContext Context, Direction Facing, FacilitySignMount Hung) {
        Level World = Context.getLevel();
        Direction Right = FacilitySignSupport.rightOf(Facing);
        for (BlockPos Other : List.of(Context.getClickedPos().relative(Right), Context.getClickedPos().relative(Right.getOpposite()))) {
            if (World.isOutsideBuildHeight(Other) || !World.getWorldBorder().isWithinBounds(Other)) {
                return false;
            }
            if (!World.getBlockState(Other).canBeReplaced(Context) || !supported(World, Other, Facing, Hung)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        Direction Right = FacilitySignSupport.rightOf(State.getValue(FACING));
        World.setBlock(Pos.relative(Right.getOpposite()), State.setValue(Part, FacilityHeaderPart.Left), Block.UPDATE_ALL);
        World.setBlock(Pos.relative(Right), State.setValue(Part, FacilityHeaderPart.Right), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return supported(World, Pos, State.getValue(FACING), State.getValue(Mount));
    }

    @Nullable
    private static FacilityHeaderPart expected(BlockState State, Direction Side) {
        Direction Right = FacilitySignSupport.rightOf(State.getValue(FACING));
        FacilityHeaderPart Own = State.getValue(Part);
        if (Side == Right) {
            return Own == FacilityHeaderPart.Left ? FacilityHeaderPart.Centre : Own == FacilityHeaderPart.Centre ? FacilityHeaderPart.Right : null;
        }
        if (Side == Right.getOpposite()) {
            return Own == FacilityHeaderPart.Right ? FacilityHeaderPart.Centre : Own == FacilityHeaderPart.Centre ? FacilityHeaderPart.Left : null;
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        FacilityHeaderPart Wanted = expected(State, Side);
        if (Wanted != null) {
            boolean Joined = NeighborState.is(this) && NeighborState.getValue(FACING) == State.getValue(FACING)
                    && NeighborState.getValue(Mount) == State.getValue(Mount) && NeighborState.getValue(Part) == Wanted;
            if (!Joined) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        Direction Support = State.getValue(Mount) == FacilitySignMount.Ceiling ? Direction.UP : State.getValue(FACING).getOpposite();
        if (Side == Support && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) != FacilityHeaderPart.Centre) {
            BlockPos Main = centrePos(State, Pos);
            BlockState MainState = World.getBlockState(Main);
            if (MainState.is(this) && MainState.getValue(Part) == FacilityHeaderPart.Centre) {
                World.setBlock(Main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, Main, Block.getId(MainState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        boolean Hung = State.getValue(Mount) == FacilitySignMount.Ceiling;
        Map<Direction, VoxelShape> Outline = switch (State.getValue(Part)) {
            case Left -> Hung ? HungLeft : WallLeft;
            case Centre -> Hung ? HungCentre : WallCentre;
            case Right -> Hung ? HungRight : WallRight;
        };
        return Outline.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Mount) == FacilitySignMount.Ceiling ? HungCell : WallCell).get(State.getValue(FACING));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Flip == Mirror.NONE) {
            return State;
        }
        return State.rotate(Flip.getRotation(State.getValue(FACING))).setValue(Part, State.getValue(Part).mirrored());
    }
}
