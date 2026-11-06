package net.rai.scp_unleashed.scp205;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
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
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.item.WireCoilItem;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Scp205LampBlock extends BaseEntityBlock implements WireConnector, LargeBlock {
    public static final MapCodec<Scp205LampBlock> BlockCodec = simpleCodec(Scp205LampBlock::new);
    public static final DirectionProperty Facing = OutletBlock.Facing;
    public static final BooleanProperty Connected = OutletBlock.Connected;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Scp205Tilt> Tilt = EnumProperty.create("tilt", Scp205Tilt.class);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;

    private final Map<Direction, VoxelShape> LowerShapes = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> UpperShapes = new EnumMap<>(Direction.class);

    public Scp205LampBlock(Properties Properties) {
        super(Properties);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            LowerShapes.put(Side, Shapes.or(
                    turned(Side, 7, 0, 10, 9, 16, 12),
                    turned(Side, 5, 6, 8, 11, 10, 14),
                    turned(Side, 6, 4, 9, 10, 6, 13),
                    turned(Side, 4, 0, 7, 12, 2, 12)));
            UpperShapes.put(Side, Shapes.or(
                    turned(Side, 7, 0, 10, 9, 8, 12),
                    turned(Side, 1, 8, 4, 15, 10, 12),
                    turned(Side, 3, 3, 0, 13, 16, 9)));
        }
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Tilt, Scp205Tilt.Level)
                .setValue(Lit, false).setValue(Connected, false));
    }

    private static VoxelShape turned(Direction Side, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        double[] First = turn(Side, X0, Z0);
        double[] Second = turn(Side, X1, Z1);
        return box(Math.min(First[0], Second[0]), Y0, Math.min(First[1], Second[1]), Math.max(First[0], Second[0]), Y1, Math.max(First[1], Second[1]));
    }

    private static double[] turn(Direction Side, double X, double Z) {
        return switch (Side) {
            case EAST -> new double[]{16.0 - Z, X};
            case SOUTH -> new double[]{16.0 - X, 16.0 - Z};
            case WEST -> new double[]{Z, 16.0 - X};
            default -> new double[]{X, Z};
        };
    }

    public static boolean isLower(BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER;
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return isLower(State) ? Pos : Pos.below();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half, Tilt, Lit, Connected);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        Direction Side = Scp205Beam.facing(State);
        return isLower(State) ? LowerShapes.get(Side) : UpperShapes.get(Side);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    public ConnectorTier getConnectorTier() {
        return ConnectorTier.Basic;
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }

    @Override
    public Direction linkFace(BlockState State, Direction Clicked) {
        return Scp205Beam.facing(State);
    }

    @Override
    public BlockPos hostPos(BlockState State, BlockPos Pos) {
        return lowerPos(State, Pos);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            Player User = Context.getPlayer();
            if (User != null && Context.getLevel().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp205_lamp.no_room").withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Half) != ThisHalf;
            if (!Partner) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && !isLower(State)) {
            BlockPos LowerPos = Pos.below();
            BlockState LowerState = World.getBlockState(LowerPos);
            if (LowerState.is(this) && isLower(LowerState)) {
                World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Stack.getItem() instanceof WireCoilItem) {
            if (isLower(State)) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            BlockHitResult Redirected = new BlockHitResult(Hit.getLocation(), Hit.getDirection(), Pos.below(), Hit.isInside());
            InteractionResult Result = Stack.useOn(new UseOnContext(User, Hand, Redirected));
            return Result.consumesAction() ? ItemInteractionResult.sidedSuccess(World.isClientSide) : ItemInteractionResult.FAIL;
        }
        return Stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel ServerWorld) {
            BlockPos LowerPos = lowerPos(State, Pos);
            BlockState Lower = World.getBlockState(LowerPos);
            if (!Lower.is(this)) {
                return InteractionResult.PASS;
            }
            BlockState Next = User.isShiftKeyDown() ? Lower.setValue(Tilt, Lower.getValue(Tilt).next()) : Lower.setValue(Facing, Scp205Beam.facing(Lower).getClockWise());
            aim(ServerWorld, LowerPos, Next);
            Scp205LampBlockEntity.play(ServerWorld, LowerPos.above(), Scp205Registry.ClickLayers, 0.6f, 1.15f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void aim(ServerLevel World, BlockPos LowerPos, BlockState Lower) {
        World.setBlock(LowerPos, Lower, Block.UPDATE_CLIENTS);
        BlockState Upper = World.getBlockState(LowerPos.above());
        if (Upper.getBlock() instanceof Scp205LampBlock) {
            World.setBlock(LowerPos.above(), Upper.setValue(Facing, Lower.getValue(Facing)).setValue(Tilt, Lower.getValue(Tilt)), Block.UPDATE_CLIENTS);
        }
        if (World.getBlockEntity(LowerPos) instanceof Scp205LampBlockEntity Lamp) {
            Lamp.aimChanged();
        }
    }

    public static void setLit(Level World, BlockPos LowerPos, boolean Value) {
        for (BlockPos Cell : List.of(LowerPos, LowerPos.above())) {
            BlockState State = World.getBlockState(Cell);
            if (State.getBlock() instanceof Scp205LampBlock && State.getValue(Lit) != Value) {
                World.setBlock(Cell, State.setValue(Lit, Value), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && isLower(State) && World instanceof ServerLevel ServerWorld && ServerWorld.getBlockEntity(Pos) instanceof Scp205LampBlockEntity Lamp) {
            Lamp.shutDown();
            Wires.detachAll(ServerWorld, Pos, Lamp);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp205_lamp.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp205_lamp.wire", ConnectorTier.Basic.getStrongest().getCoil().getDescription(), Scp205LampBlockEntity.PowerDraw)
                    .withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp205_lamp.aim").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp205_lamp.details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isLower(State) ? new Scp205LampBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!isLower(State) || World.isClientSide) {
            return null;
        }
        return createTickerHelper(Type, Scp205Registry.LampEntity.get(), Scp205LampBlockEntity::serverTick);
    }
}
