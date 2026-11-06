package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterAssembly;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterBlockEntity;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterRegistry;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterShapes;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.props.PropTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CellShutterBlock extends CellStackBlock implements EntityBlock, Screwdrivable {
    public static final MapCodec<CellShutterBlock> ShutterCodec = simpleCodec(CellShutterBlock::new);
    public static final BooleanProperty Left = BooleanProperty.create("left");
    public static final BooleanProperty Right = BooleanProperty.create("right");
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final BooleanProperty Shut = BooleanProperty.create("shut");
    public static final BooleanProperty Lockdown = BooleanProperty.create("lockdown");
    public static final String[] Details = {"stack", "row", "power", "mode"};

    public CellShutterBlock(Properties Properties) {
        super(CellStackKind.LockdownShutter, Properties);
        registerDefaultState(defaultBlockState().setValue(Left, false).setValue(Right, false).setValue(Open, false).setValue(Shut, true).setValue(Lockdown, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return ShutterCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Left, Right, Open, Shut, Lockdown);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = withSides(super.getStateForPlacement(Context), Context.getLevel(), Context.getClickedPos());
        BlockState Joined = joinedNeighbour(Context.getLevel(), Context.getClickedPos(), Placed);
        if (Joined != null) {
            Placed = Placed.setValue(Open, Joined.getValue(Open)).setValue(Shut, Joined.getValue(Shut)).setValue(Lockdown, Joined.getValue(Lockdown));
        }
        return Placed;
    }

    @Nullable
    private BlockPos joinedPos(BlockGetter World, BlockPos Pos, BlockState State) {
        for (Direction Way : ShutterAssembly.ways(State.getValue(FACING))) {
            BlockPos Next = Pos.relative(Way);
            if (ShutterShapes.joins(State, World.getBlockState(Next))) {
                return Next;
            }
        }
        return null;
    }

    @Nullable
    private BlockState joinedNeighbour(BlockGetter World, BlockPos Pos, BlockState State) {
        BlockPos Next = joinedPos(World, Pos, State);
        return Next == null ? null : World.getBlockState(Next);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        BlockPos Next = joinedPos(World, Pos, State);
        if (World.getBlockEntity(Pos) instanceof ShutterBlockEntity Shutter) {
            if (Next != null && World.getBlockEntity(Next) instanceof ShutterBlockEntity Neighbour) {
                Shutter.adopt(Neighbour.getProgress());
            }
            Shutter.place(ShutterShapes.column(World, Pos, State));
            Shutter.request();
        }
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        BlockState Updated = super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
        if (Side.getAxis().isHorizontal() && Updated.is(this)) {
            return withSides(Updated, World, Pos);
        }
        return Updated;
    }

    private BlockState withSides(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction Facing = State.getValue(FACING);
        return State.setValue(Left, joins(State, World.getBlockState(Pos.relative(Facing.getClockWise()))))
                .setValue(Right, joins(State, World.getBlockState(Pos.relative(Facing.getCounterClockWise()))));
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide || Neighbor == this) {
            return;
        }
        if (World.getBlockEntity(Pos) instanceof ShutterBlockEntity Shutter) {
            Shutter.notice(World.hasNeighborSignal(Pos));
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!World.isClientSide && !State.is(NewState.getBlock())) {
            ShutterAssembly.touchNeighbours(World, Pos, State);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        if (!(World instanceof ServerLevel ServerWorld)) {
            return InteractionResult.PASS;
        }
        boolean Locked = ShutterAssembly.toggleMode(ServerWorld, Pos);
        if (User != null) {
            User.displayClientMessage(Locked
                    ? Component.translatable("message.scp_unleashed.lockdown_shutter.lockdown").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.scp_unleashed.lockdown_shutter.normal").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.SUCCESS;
    }

    private VoxelShape liveShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        if (State.getValue(Shut)) {
            return super.getShape(State, World, Pos, Context);
        }
        if (!(World.getBlockEntity(Pos) instanceof ShutterBlockEntity Shutter)) {
            return State.getValue(Open) ? ShutterShapes.shape(State, 0, 0) : super.getShape(State, World, Pos, Context);
        }
        ShutterShapes.Column Place = Shutter.place();
        return ShutterShapes.shape(State, ShutterShapes.localBottom(Place, Shutter.getProgress()), ShutterShapes.localTop(Place));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return liveShape(State, World, Pos, Context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return liveShape(State, World, Pos, Context);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        if (World.getBlockEntity(Pos) instanceof ShutterBlockEntity Shutter) {
            return Shutter.comparatorOutput();
        }
        return !State.getValue(Shut) && State.getValue(Open) ? 15 : 0;
    }

    private static boolean hasOpening(BlockState State) {
        PillarPart Piece = State.getValue(Part);
        return Piece == PillarPart.Bottom || Piece == PillarPart.Middle;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return Type != PathComputationType.WATER && State.getValue(Open) && hasOpening(State);
    }

    @Nullable
    @Override
    public PathType getBlockPathType(BlockState State, BlockGetter World, BlockPos Pos, @Nullable Mob Walker) {
        return State.getValue(Open) && hasOpening(State) ? PathType.DOOR_OPEN : PathType.BLOCKED;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, Details);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ShutterBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (Type != ShutterRegistry.Shutter.get()) {
            return null;
        }
        BlockEntityTicker<ShutterBlockEntity> Ticker = World.isClientSide ? ShutterBlockEntity::clientTick : ShutterBlockEntity::serverTick;
        return (BlockEntityTicker<T>) Ticker;
    }
}
