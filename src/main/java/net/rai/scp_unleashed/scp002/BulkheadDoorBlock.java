package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp005.KeyHoldable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class BulkheadDoorBlock extends FleshBlock implements EntityBlock, KeyHoldable {
    public static final MapCodec<BulkheadDoorBlock> DoorCodec = simpleCodec(BulkheadDoorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    private static final Map<Direction, VoxelShape> SillFloor = new EnumMap<>(Direction.class);

    static {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            SillFloor.put(Side, FleshFixtureBlock.rotated(Side, 1.0, 0.0, 0.0, 15.0, 2.0, 5.0));
        }
    }

    public BulkheadDoorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends BulkheadDoorBlock> codec() {
        return DoorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half, Open);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return BulkheadShapes.outline(State.getValue(Facing), State.getValue(Half), State.getValue(Open));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        if (!State.getValue(Open)) {
            return Shapes.block();
        }
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? SillFloor.get(State.getValue(Facing)) : Shapes.empty();
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.getValue(Open) ? 1 : 15;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return Kind == PathComputationType.LAND && State.getValue(Open);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        DoubleBlockHalf Part = State.getValue(Half);
        Direction Partner = Part == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (Side == Partner) {
            if (!Neighbour.is(this) || Neighbour.getValue(Half) == Part) {
                return Blocks.AIR.defaultBlockState();
            }
            return State.setValue(Open, Neighbour.getValue(Open)).setValue(Facing, Neighbour.getValue(Facing));
        }
        return State;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        BlockPos Lower = lowerOf(Pos, State);
        if (!(World.getBlockEntity(Lower) instanceof BulkheadDoorBlockEntity Door) || !Door.isIdleClosed()) {
            return InteractionResult.PASS;
        }
        if (!(World instanceof ServerLevel Server)) {
            return InteractionResult.SUCCESS;
        }
        if (!isOutside(Lower, State.getValue(Facing), User.getX(), User.getZ())) {
            Scp002Sounds.doorDeny(Server, Lower);
            User.displayClientMessage(Component.translatable("block.scp_unleashed.scp002_door.sealed").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        Door.open(Server);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockPos keyAnchor(BlockPos Pos, BlockState State) {
        return lowerOf(Pos, State);
    }

    @Override
    public boolean holdOpen(ServerLevel World, BlockPos Anchor, @Nullable Player User) {
        if (!(World.getBlockEntity(Anchor) instanceof BulkheadDoorBlockEntity Door)) {
            return false;
        }
        if (User != null && !isOutside(Anchor, Door.getFront(), User.getX(), User.getZ())) {
            Scp002Sounds.doorDeny(World, Anchor);
            User.displayClientMessage(Component.translatable("block.scp_unleashed.scp002_door.sealed").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            return false;
        }
        Door.hold(World, true);
        return true;
    }

    @Override
    public void releaseHold(ServerLevel World, BlockPos Anchor) {
        if (World.getBlockEntity(Anchor) instanceof BulkheadDoorBlockEntity Door) {
            Door.hold(World, false);
        }
    }

    @Override
    public boolean isHeldOpen(BlockGetter World, BlockPos Anchor) {
        return World.getBlockEntity(Anchor) instanceof BulkheadDoorBlockEntity Door && Door.isHeld();
    }

    public static BlockPos lowerOf(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? Pos : Pos.below();
    }

    public static boolean isOutside(BlockPos Lower, Direction Front, double X, double Z) {
        double Dx = X - (Lower.getX() + 0.5);
        double Dz = Z - (Lower.getZ() + 0.5);
        return Dx * Front.getStepX() + Dz * Front.getStepZ() > 0.0;
    }

    public static void setOpen(Level World, BlockPos Lower, boolean Opened) {
        BlockState State = World.getBlockState(Lower);
        BlockState Above = World.getBlockState(Lower.above());
        if (State.getBlock() instanceof BulkheadDoorBlock) {
            World.setBlock(Lower, State.setValue(Open, Opened), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
        if (Above.getBlock() instanceof BulkheadDoorBlock) {
            World.setBlock(Lower.above(), Above.setValue(Open, Opened), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
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
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new BulkheadDoorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || State.getValue(Half) != DoubleBlockHalf.LOWER || Type != Scp002Registry.DoorEntity.get()) {
            return null;
        }
        return (TickWorld, TickPos, TickState, Ticking) -> BulkheadDoorBlockEntity.serverTick(TickWorld, TickPos, TickState, (BulkheadDoorBlockEntity) Ticking);
    }
}
