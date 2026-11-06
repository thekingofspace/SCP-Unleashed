package net.rai.scp_unleashed.powerbridge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PowerBridgeBlock extends BaseEntityBlock implements Screwdrivable {
    public static final MapCodec<PowerBridgeBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            PowerBridgeKind.KindCodec.fieldOf("kind").forGetter(PowerBridgeBlock::getKind),
            propertiesCodec()
    ).apply(Instance, PowerBridgeBlock::new));
    public static final DirectionProperty Facing = BlockStateProperties.FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final double PortDepth = 1.0;
    private static final Map<Direction, VoxelShape> SupportShapes = new EnumMap<>(Direction.class);

    static {
        for (Direction Front : Direction.values()) {
            double MinX = Front == Direction.WEST ? PortDepth : 0.0;
            double MaxX = Front == Direction.EAST ? 16.0 - PortDepth : 16.0;
            double MinY = Front == Direction.DOWN ? PortDepth : 0.0;
            double MaxY = Front == Direction.UP ? 16.0 - PortDepth : 16.0;
            double MinZ = Front == Direction.NORTH ? PortDepth : 0.0;
            double MaxZ = Front == Direction.SOUTH ? 16.0 - PortDepth : 16.0;
            SupportShapes.put(Front, box(MinX, MinY, MinZ, MaxX, MaxY, MaxZ));
        }
    }

    private final PowerBridgeKind Kind;

    public PowerBridgeBlock(PowerBridgeKind Kind, Properties Settings) {
        super(Settings);
        this.Kind = Kind;
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false));
    }

    public PowerBridgeKind getKind() {
        return Kind;
    }

    public String key() {
        return "block.scp_unleashed." + PowerBridgeRegistry.BridgeId + "." + Kind.getSerializedName() + ".";
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Clicked = Context.getClickedFace();
        BlockPos Against = Context.getClickedPos().relative(Clicked.getOpposite());
        Level World = Context.getLevel();
        boolean Energetic = !Context.replacingClickedOnBlock() && World.getCapability(Capabilities.EnergyStorage.BLOCK, Against, Clicked) != null;
        Direction Front = Energetic ? Clicked.getOpposite() : Context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(Facing, Front);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return SupportShapes.getOrDefault(State.getValue(Facing), Shapes.block());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbour, BlockPos NeighbourPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbour, NeighbourPos, MovedByPiston);
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof PowerBridgeBlockEntity Bridge) {
            Bridge.lineChanged();
        }
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        Direction Current = State.getValue(Facing);
        Direction[] Order = Direction.values();
        int Start = Current.ordinal();
        for (int Step = 1; Step < Order.length; Step++) {
            Direction Next = Order[Math.floorMod(Start + (Sneaking ? -Step : Step), Order.length)];
            if (hasConnector(World, Pos, Next)) {
                continue;
            }
            World.setBlock(Pos, State.setValue(Facing, Next), Block.UPDATE_ALL);
            World.invalidateCapabilities(Pos);
            if (World.getBlockEntity(Pos) instanceof PowerBridgeBlockEntity Bridge) {
                Bridge.lineChanged();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean hasConnector(Level World, BlockPos Pos, Direction Side) {
        BlockPos Next = Pos.relative(Side);
        BlockState Other = World.getBlockState(Next);
        return Other.getBlock() instanceof WireConnector Connector && Connector.hostPos(Other, Next).equals(Pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new PowerBridgeBlockEntity(Kind, Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, PowerBridgeRegistry.entityType(Kind), PowerBridgeBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(key() + "tooltip").withStyle(Kind.takesFe() ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(key() + "front").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(key() + "wiring").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(key() + "rate", PowerBridgeConversion.Sample, PowerBridgeConversion.sampleOut(Kind)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.power_bridge.buffer", String.format("%,d", PowerBridgeBlockEntity.BufferCapacity)).withStyle(ChatFormatting.GOLD));
        }
    }
}
