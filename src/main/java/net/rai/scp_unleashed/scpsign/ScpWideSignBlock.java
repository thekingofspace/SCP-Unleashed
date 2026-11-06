package net.rai.scp_unleashed.scpsign;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ScpWideSignBlock extends ScpSignBlock {
    public static final MapCodec<ScpWideSignBlock> BlockCodec = simpleCodec(ScpWideSignBlock::new);
    public static final EnumProperty<ScpSignPart> Part = EnumProperty.create("part", ScpSignPart.class);
    private static final Map<Direction, VoxelShape> LeftShapes = PropShapes.horizontal(PropShapes.box(-15.5, 0.5, 13.5, 15.5, 15.5, 16));
    private static final Map<Direction, VoxelShape> RightShapes = PropShapes.horizontal(PropShapes.box(0.5, 0.5, 13.5, 31.5, 15.5, 16));

    public ScpWideSignBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Lit, false).setValue(Part, ScpSignPart.Left));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Part);
    }

    @Override
    public ScpSignKind getKind() {
        return ScpSignKind.Wide;
    }

    public static Direction towardPartner(BlockState State) {
        Direction Right = rightOf(State.getValue(FACING));
        return State.getValue(Part) == ScpSignPart.Left ? Right : Right.getOpposite();
    }

    public static BlockPos partnerPos(BlockState State, BlockPos Pos) {
        return Pos.relative(towardPartner(State));
    }

    @Override
    public BlockPos mainPos(BlockState State, BlockPos Pos) {
        return State.getValue(Part) == ScpSignPart.Left ? Pos : partnerPos(State, Pos);
    }

    @Override
    public List<BlockPos> parts(BlockState State, BlockPos Pos) {
        return List.of(Pos, partnerPos(State, Pos));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Clicked = Context.getClickedPos();
        boolean AnyWall = false;
        for (Direction Facing : facingChoices(Context)) {
            if (!onWall(World, Clicked, Facing)) {
                continue;
            }
            AnyWall = true;
            Direction Right = rightOf(Facing);
            Vec3 Offset = Context.getClickLocation().subtract(Vec3.atCenterOf(Clicked));
            boolean Rightward = Offset.x * Right.getStepX() + Offset.z * Right.getStepZ() >= 0.0;
            for (ScpSignPart Placed : Rightward ? new ScpSignPart[]{ScpSignPart.Left, ScpSignPart.Right} : new ScpSignPart[]{ScpSignPart.Right, ScpSignPart.Left}) {
                BlockPos Other = Clicked.relative(Placed == ScpSignPart.Left ? Right : Right.getOpposite());
                if (fits(Context, Other, Facing)) {
                    return defaultBlockState().setValue(FACING, Facing).setValue(Part, Placed)
                            .setValue(Lit, World.hasNeighborSignal(Clicked) || World.hasNeighborSignal(Other));
                }
            }
        }
        refuse(Context, AnyWall ? "no_room" : "no_wall");
        return null;
    }

    private static boolean fits(BlockPlaceContext Context, BlockPos Other, Direction Facing) {
        Level World = Context.getLevel();
        if (World.isOutsideBuildHeight(Other) || !World.getWorldBorder().isWithinBounds(Other)) {
            return false;
        }
        return World.getBlockState(Other).canBeReplaced(Context) && onWall(World, Other, Facing);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        World.setBlock(partnerPos(State, Pos), State.setValue(Part, State.getValue(Part).other()), Block.UPDATE_ALL);
        if (!World.isClientSide) {
            applyStackDocument(World, State, Pos, Stack);
            refreshPower(World, State, Pos);
        }
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == towardPartner(State)) {
            boolean Paired = NeighborState.is(this) && NeighborState.getValue(FACING) == State.getValue(FACING) && NeighborState.getValue(Part) != State.getValue(Part);
            if (!Paired) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) == ScpSignPart.Right) {
            BlockPos Main = partnerPos(State, Pos);
            BlockState MainState = World.getBlockState(Main);
            if (MainState.is(this) && MainState.getValue(Part) == ScpSignPart.Left) {
                World.setBlock(Main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, Main, Block.getId(MainState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Part) == ScpSignPart.Left ? LeftShapes : RightShapes).get(State.getValue(FACING));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Flip == Mirror.NONE) {
            return State;
        }
        return State.rotate(Flip.getRotation(State.getValue(FACING))).setValue(Part, State.getValue(Part).other());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == ScpSignPart.Left ? new ScpSignBlockEntity(Pos, State) : null;
    }
}
