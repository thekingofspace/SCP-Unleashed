package net.rai.scp_unleashed.facilitysign;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FacilitySignBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FacilitySignBlock> BlockCodec = simpleCodec(FacilitySignBlock::new);
    public static final EnumProperty<FacilitySignPart> Part = EnumProperty.create("part", FacilitySignPart.class);
    public static final EnumProperty<FacilitySignArrow> Arrow = EnumProperty.create("arrow", FacilitySignArrow.class);
    public static final EnumProperty<FacilitySignMount> Mount = EnumProperty.create("mount", FacilitySignMount.class);
    private static final Map<Direction, VoxelShape> WallLeft = PropShapes.horizontal(PropShapes.box(-16, 1, 14.5, 16, 15, 16));
    private static final Map<Direction, VoxelShape> WallRight = PropShapes.horizontal(PropShapes.box(0, 1, 14.5, 32, 15, 16));
    private static final Map<Direction, VoxelShape> WallCell = PropShapes.horizontal(PropShapes.box(0, 1, 14.5, 16, 15, 16));
    private static final Map<Direction, VoxelShape> HungLeft = PropShapes.horizontal(PropShapes.of(PropShapes.box(-16, 0, 7, 16, 14, 9), PropShapes.box(11.5, 14, 7.5, 12.5, 16, 8.5)));
    private static final Map<Direction, VoxelShape> HungRight = PropShapes.horizontal(PropShapes.of(PropShapes.box(0, 0, 7, 32, 14, 9), PropShapes.box(3.5, 14, 7.5, 4.5, 16, 8.5)));
    private static final Map<Direction, VoxelShape> HungCell = PropShapes.horizontal(PropShapes.box(0, 0, 7, 16, 14, 9));

    public FacilitySignBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, FacilitySignPart.Left)
                .setValue(Arrow, FacilitySignArrow.Right).setValue(Mount, FacilitySignMount.Wall));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Part, Arrow, Mount);
    }

    public static Direction towardPartner(BlockState State) {
        Direction Right = FacilitySignSupport.rightOf(State.getValue(FACING));
        return State.getValue(Part) == FacilitySignPart.Left ? Right : Right.getOpposite();
    }

    public static BlockPos partnerPos(BlockState State, BlockPos Pos) {
        return Pos.relative(towardPartner(State));
    }

    public static Direction supportSide(BlockState State) {
        return State.getValue(Mount) == FacilitySignMount.Ceiling ? Direction.UP : State.getValue(FACING).getOpposite();
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
            BlockState Hung = attempt(Context, Toward, FacilitySignMount.Ceiling);
            if (Hung != null) {
                return Hung;
            }
        }
        for (Direction Facing : FacilitySignSupport.facingChoices(Context)) {
            if (!FacilitySignSupport.onWall(World, Clicked, Facing)) {
                continue;
            }
            AnySupport = true;
            BlockState Placed = attempt(Context, Facing, FacilitySignMount.Wall);
            if (Placed != null) {
                return Placed;
            }
        }
        if (!Overhead && FacilitySignSupport.underCeiling(World, Clicked)) {
            AnySupport = true;
            BlockState Hung = attempt(Context, Toward, FacilitySignMount.Ceiling);
            if (Hung != null) {
                return Hung;
            }
        }
        FacilitySignSupport.refuse(Context, AnySupport ? "no_room" : "no_support");
        return null;
    }

    @Nullable
    private BlockState attempt(BlockPlaceContext Context, Direction Facing, FacilitySignMount Hung) {
        BlockPos Clicked = Context.getClickedPos();
        Direction Right = FacilitySignSupport.rightOf(Facing);
        Vec3 Offset = Context.getClickLocation().subtract(Vec3.atCenterOf(Clicked));
        boolean Rightward = Offset.x * Right.getStepX() + Offset.z * Right.getStepZ() >= 0.0;
        FacilitySignPart[] Order = Rightward ? new FacilitySignPart[]{FacilitySignPart.Left, FacilitySignPart.Right} : new FacilitySignPart[]{FacilitySignPart.Right, FacilitySignPart.Left};
        for (FacilitySignPart Placed : Order) {
            BlockPos Other = Clicked.relative(Placed == FacilitySignPart.Left ? Right : Right.getOpposite());
            if (fits(Context, Other, Facing, Hung)) {
                return defaultBlockState().setValue(FACING, Facing).setValue(Part, Placed).setValue(Mount, Hung).setValue(Arrow, FacilitySignArrow.Right);
            }
        }
        return null;
    }

    private static boolean fits(BlockPlaceContext Context, BlockPos Other, Direction Facing, FacilitySignMount Hung) {
        Level World = Context.getLevel();
        if (World.isOutsideBuildHeight(Other) || !World.getWorldBorder().isWithinBounds(Other)) {
            return false;
        }
        return World.getBlockState(Other).canBeReplaced(Context) && supported(World, Other, Facing, Hung);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        World.setBlock(partnerPos(State, Pos), State.setValue(Part, State.getValue(Part).other()), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return supported(World, Pos, State.getValue(FACING), State.getValue(Mount));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == towardPartner(State)) {
            boolean Paired = NeighborState.is(this) && NeighborState.getValue(FACING) == State.getValue(FACING)
                    && NeighborState.getValue(Mount) == State.getValue(Mount) && NeighborState.getValue(Part) != State.getValue(Part);
            if (!Paired) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        if (Side == supportSide(State) && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) == FacilitySignPart.Right) {
            BlockPos Main = partnerPos(State, Pos);
            BlockState MainState = World.getBlockState(Main);
            if (MainState.is(this) && MainState.getValue(Part) == FacilitySignPart.Left) {
                World.setBlock(Main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, Main, Block.getId(MainState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            FacilitySignArrow Current = State.getValue(Arrow);
            FacilitySignArrow Turned = User.isSecondaryUseActive() ? Current.previous() : Current.next();
            turn(World, Pos, State, Turned);
            World.playSound(null, Pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 0.8f);
            World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public void turn(Level World, BlockPos Pos, BlockState State, FacilitySignArrow Turned) {
        BlockPos Other = partnerPos(State, Pos);
        BlockState OtherState = World.getBlockState(Other);
        World.setBlock(Pos, State.setValue(Arrow, Turned), Block.UPDATE_ALL);
        if (OtherState.is(this)) {
            World.setBlock(Other, OtherState.setValue(Arrow, Turned), Block.UPDATE_ALL);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        boolean Left = State.getValue(Part) == FacilitySignPart.Left;
        if (State.getValue(Mount) == FacilitySignMount.Ceiling) {
            return (Left ? HungLeft : HungRight).get(State.getValue(FACING));
        }
        return (Left ? WallLeft : WallRight).get(State.getValue(FACING));
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
        return State.rotate(Flip.getRotation(State.getValue(FACING))).setValue(Part, State.getValue(Part).other()).setValue(Arrow, State.getValue(Arrow).mirrored());
    }
}
