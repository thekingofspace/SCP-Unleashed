package net.rai.scp_unleashed.nethub;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.nethub.client.NetHubClientHooks;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NetworkHubBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock {
    public static final MapCodec<NetworkHubBlock> BlockCodec = simpleCodec(NetworkHubBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<HubStatus> Status = EnumProperty.create("status", HubStatus.class);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 80);
    public static final OilLayout Layout = new OilLayout(3, 9, 3, 1, List.of(
            new AABB(0, 0, 0, 48, 2, 48),
            new AABB(2, 2, 2, 46, 30, 46),
            new AABB(5, 30, 5, 43, 112, 43),
            new AABB(4, 112, 4, 44, 116, 44),
            new AABB(11, 116, 11, 37, 128, 37),
            new AABB(22, 128, 22, 26, 144, 26),
            new AABB(25, 129, 2, 37, 141, 10)),
            List.of(new AABB(4, 4, 0, 12, 12, 2), new AABB(0, 4, 4, 2, 12, 12)));
    public static final int BeaconLight = 9;
    public static final int StandbyBeaconLight = 6;
    public static final int RackLight = 4;
    private static boolean Dismantling;

    public NetworkHubBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Status, HubStatus.Off).setValue(Part, 0));
    }

    public static int lightOf(BlockState State) {
        HubStatus Shown = State.getValue(Status);
        if (!Shown.isPowered()) {
            return 0;
        }
        int Index = State.getValue(Part);
        int Column = Layout.column(Index);
        int Row = Layout.row(Index);
        int Depth = Layout.layer(Index);
        if (Column == 1 && Depth == 1 && Row == Layout.getHeight() - 1) {
            return Shown == HubStatus.Bridging ? BeaconLight : StandbyBeaconLight;
        }
        if (Column == 1 && Depth != 1 && Row >= 2 && Row <= 6 && Shown == HubStatus.Bridging) {
            return RackLight;
        }
        return 0;
    }

    public static BlockPos originOf(BlockPos Cell, BlockState State) {
        return Layout.originOf(Cell, State.getValue(Facing), State.getValue(Part));
    }

    @Nullable
    public static NetworkHubBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        if (!(State.getBlock() instanceof NetworkHubBlock)) {
            return null;
        }
        return World.getBlockEntity(originOf(Cell, State)) instanceof NetworkHubBlockEntity Found ? Found : null;
    }

    public static void applyStatus(Level World, BlockPos Origin, BlockState MasterState, HubStatus Wanted) {
        if (!(MasterState.getBlock() instanceof NetworkHubBlock Hub)) {
            return;
        }
        Direction Front = MasterState.getValue(Facing);
        for (int Index = 0; Index < Layout.cellCount(); Index++) {
            BlockPos Cell = Layout.cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.is(Hub) && Current.getValue(Part) == Index && Current.getValue(Facing) == Front && Current.getValue(Status) != Wanted) {
                World.setBlock(Cell, Current.setValue(Status, Wanted), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Status, Part);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        return Layout.cells(Layout.masterFromPlaced(Context.getClickedPos(), Front), Front);
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return originOf(Pos, State);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Layout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Layout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Layout.support(State.getValue(Facing), State.getValue(Part));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            Player User = Context.getPlayer();
            if (User != null && Context.getLevel().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.network_hub.no_room", Layout.getWidth(), Layout.getHeight(), Layout.getDepth())
                        .withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, Layout.getPlacePart());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        Direction Front = State.getValue(Facing);
        BlockPos Master = Layout.masterFromPlaced(Pos, Front);
        for (int Index = 0; Index < Layout.cellCount(); Index++) {
            if (Index != Layout.getPlacePart()) {
                World.setBlock(Layout.cellPos(Master, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel Server) {
            WireNetworkManager.get(Server).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            BlockPos Origin = originOf(Pos, State);
            Direction Front = State.getValue(Facing);
            if (World instanceof ServerLevel Server) {
                NetHubs.removed(Server, Origin);
            }
            Dismantling = true;
            try {
                for (int Index = 0; Index < Layout.cellCount(); Index++) {
                    BlockPos Cell = Layout.cellPos(Origin, Front, Index);
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(Part) == Index && CellState.getValue(Facing) == Front) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                Dismantling = false;
            }
            if (World instanceof ServerLevel Server) {
                WireNetworkManager.get(Server).markDirty();
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            NetHubClientHooks.open(originOf(Pos, State));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.network_hub.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.network_hub.size", Layout.getWidth(), Layout.getHeight(), Layout.getDepth()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.network_hub.port", String.format("%04d", WireChannels.MaxPort)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.network_hub.reach").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.network_hub.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.network_hub.draw", NetworkHubBlockEntity.Draw).withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == 0 ? new NetworkHubBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != 0 || Type != NetHubRegistry.NetworkHubEntity.get()) {
            return null;
        }
        BlockEntityTicker<NetworkHubBlockEntity> Ticker = World.isClientSide ? NetworkHubBlockEntity::clientTick : NetworkHubBlockEntity::serverTick;
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> Cast = (BlockEntityTicker<T>) Ticker;
        return Cast;
    }
}
