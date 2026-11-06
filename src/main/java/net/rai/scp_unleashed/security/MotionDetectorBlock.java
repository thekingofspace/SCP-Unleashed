package net.rai.scp_unleashed.security;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.item.WireCoilItem;
import net.rai.scp_unleashed.security.client.SecurityScreens;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MotionDetectorBlock extends BaseEntityBlock implements WireConnector {
    public static final MapCodec<MotionDetectorBlock> BlockCodec = simpleCodec(MotionDetectorBlock::new);
    public static final DirectionProperty Facing = OutletBlock.Facing;
    public static final BooleanProperty Connected = OutletBlock.Connected;
    public static final BooleanProperty Tripped = BooleanProperty.create("tripped");
    public static final int Signal = 15;

    private final Map<Direction, VoxelShape> FacingShapes = new EnumMap<>(Direction.class);

    public MotionDetectorBlock(Properties Properties) {
        super(Properties);
        for (Direction Side : Direction.values()) {
            FacingShapes.put(Side, Shapes.or(
                    mountedBox(Side, 5.0, 5.0, 15.0, 11.0, 11.0, 16.0),
                    mountedBox(Side, 7.0, 7.0, 11.0, 9.0, 9.0, 15.0),
                    mountedBox(Side, 4.0, 5.0, 5.0, 12.0, 11.0, 11.0),
                    mountedBox(Side, 5.0, 6.0, 4.0, 11.0, 10.0, 5.0)));
        }
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Connected, false).setValue(Tripped, false));
    }

    private static VoxelShape mountedBox(Direction Side, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        double[] First = turned(Side, X0, Y0, Z0);
        double[] Second = turned(Side, X1, Y1, Z1);
        return box(Math.min(First[0], Second[0]), Math.min(First[1], Second[1]), Math.min(First[2], Second[2]),
                Math.max(First[0], Second[0]), Math.max(First[1], Second[1]), Math.max(First[2], Second[2]));
    }

    private static double[] turned(Direction Side, double X, double Y, double Z) {
        return switch (Side) {
            case SOUTH -> new double[]{16.0 - X, Y, 16.0 - Z};
            case EAST -> new double[]{16.0 - Z, Y, X};
            case WEST -> new double[]{Z, Y, 16.0 - X};
            case UP -> new double[]{X, 16.0 - Z, Y};
            case DOWN -> new double[]{X, Z, 16.0 - Y};
            default -> new double[]{X, Y, Z};
        };
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
        return State.getValue(Facing);
    }

    @Override
    public BlockPos hostPos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Connected, Tripped);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FacingShapes.get(State.getValue(Facing));
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Side = State.getValue(Facing);
        BlockPos SupportPos = Pos.relative(Side.getOpposite());
        return World.getBlockState(SupportPos).isFaceSturdy(World, SupportPos, Side);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Clicked = defaultBlockState().setValue(Facing, Context.getClickedFace());
        if (Clicked.canSurvive(Context.getLevel(), Context.getClickedPos())) {
            return Clicked;
        }
        for (Direction Looking : Context.getNearestLookingDirections()) {
            BlockState Candidate = defaultBlockState().setValue(Facing, Looking.getOpposite());
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return Candidate;
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == State.getValue(Facing).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
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
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        return Stack.getItem() instanceof WireCoilItem ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide && World.getBlockEntity(Pos) instanceof MotionDetectorBlockEntity) {
            SecurityScreens.openDetector(Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState State) {
        return true;
    }

    @Override
    protected int getSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return State.getValue(Tripped) ? Signal : 0;
    }

    @Override
    protected int getDirectSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return State.getValue(Tripped) && State.getValue(Facing) == Side ? Signal : 0;
    }

    public static void updateNeighbours(Level World, BlockPos Pos, BlockState State) {
        Block Owner = State.getBlock();
        World.updateNeighborsAt(Pos, Owner);
        World.updateNeighborsAt(Pos.relative(State.getValue(Facing).getOpposite()), Owner);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock())) {
            if (World instanceof ServerLevel ServerWorld && ServerWorld.getBlockEntity(Pos) instanceof MotionDetectorBlockEntity Detector) {
                Detector.silence(ServerWorld);
                Wires.detachAll(ServerWorld, Pos, Detector);
            }
            if (State.getValue(Tripped)) {
                updateNeighbours(World, Pos, State);
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.motion_detector.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.network_object.ports", String.format("%04d", WireChannels.MaxPort)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.motion_detector.detail.range", MotionDetectorBlockEntity.MinRange, MotionDetectorBlockEntity.MaxRange)
                    .withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.speaker.wires", ConnectorTier.Basic.getStrongest().getCoil().getDescription()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.motion_detector.detail.redstone").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new MotionDetectorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, SecurityRegistry.MotionDetectorEntity.get(), MotionDetectorBlockEntity::serverTick);
    }
}
