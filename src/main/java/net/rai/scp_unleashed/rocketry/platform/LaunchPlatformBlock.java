package net.rai.scp_unleashed.rocketry.platform;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LaunchPlatformBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock, FluidPort {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final int LampFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static boolean Dismantling;

    protected LaunchPlatformBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false).setValue(partProperty(), 0));
    }

    public abstract OilLayout layout();

    public abstract IntegerProperty partProperty();

    public abstract LaunchPlatformKind kind();

    protected abstract BlockEntityType<? extends LaunchPlatformBlockEntity> entityType();

    protected abstract LaunchPlatformBlockEntity createMaster(BlockPos Pos, BlockState State);

    public abstract int fuelPart();

    public abstract Direction fuelSide();

    public abstract double[] standPoint();

    protected abstract void appendDetails(List<Component> Lines);

    public abstract int[] lampParts();

    public int getPart(BlockState State) {
        return State.getValue(partProperty());
    }

    public BlockPos originOf(BlockPos Cell, BlockState State) {
        return layout().originOf(Cell, State.getValue(Facing), getPart(State));
    }

    public boolean isMaster(BlockState State) {
        return getPart(State) == 0;
    }

    public Vec3 standAt(BlockPos Origin, Direction Front) {
        double[] Point = standPoint();
        return OilLayout.toWorld(Origin, Front, Point[0], Point[1], Point[2]);
    }

    @Nullable
    public static LaunchPlatformBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        if (!(State.getBlock() instanceof LaunchPlatformBlock Platform)) {
            return null;
        }
        return World.getBlockEntity(Platform.originOf(Cell, State)) instanceof LaunchPlatformBlockEntity Found ? Found : null;
    }

    public static void applyActive(Level World, BlockPos Origin, BlockState MasterState, boolean Wanted) {
        if (!(MasterState.getBlock() instanceof LaunchPlatformBlock Platform)) {
            return;
        }
        Direction Front = MasterState.getValue(Facing);
        OilLayout Layout = Platform.layout();
        for (int Index : Platform.lampParts()) {
            BlockPos Cell = Layout.cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.is(Platform) && Platform.getPart(Current) == Index && Current.getValue(Facing) == Front && Current.getValue(Active) != Wanted) {
                World.setBlock(Cell, Current.setValue(Active, Wanted), LampFlags);
            }
        }
    }

    public boolean isFuelFace(BlockState State, Direction Side) {
        return getPart(State) == fuelPart() && OilLayout.localSide(State.getValue(Facing), Side) == fuelSide();
    }

    @Nullable
    public IFluidHandler fluidCapability(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        LaunchPlatformBlockEntity Platform = masterEntity(World, Pos, State);
        if (Platform == null) {
            return null;
        }
        if (Side == null) {
            return Platform.getFuelHandler();
        }
        return isFuelFace(State, Side) ? new RestrictedFluidHandler(Platform.getFuelHandler(), PortRole.Input) : null;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        return layout().cells(layout().masterFromPlaced(Context.getClickedPos(), Front), Front);
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return originOf(Pos, State);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active, partProperty());
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return layout().shape(State.getValue(Facing), getPart(State));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return layout().shape(State.getValue(Facing), getPart(State));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return layout().support(State.getValue(Facing), getPart(State));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            Player User = Context.getPlayer();
            if (User != null && Context.getLevel().isClientSide) {
                OilLayout Layout = layout();
                User.displayClientMessage(Component.translatable("message.scp_unleashed.oil_machine.no_room", Layout.getWidth(), Layout.getHeight(), Layout.getDepth())
                        .withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(partProperty(), layout().getPlacePart());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        Direction Front = State.getValue(Facing);
        BlockPos Master = layout().masterFromPlaced(Pos, Front);
        for (int Index = 0; Index < layout().cellCount(); Index++) {
            if (Index != layout().getPlacePart()) {
                World.setBlock(layout().cellPos(Master, Front, Index), State.setValue(partProperty(), Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (OldState.is(this) || !(World instanceof ServerLevel ServerWorld)) {
            return;
        }
        WireNetworkManager.get(ServerWorld).markDirty();
        if (getPart(State) == fuelPart()) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        World.invalidateCapabilities(Pos);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            BlockPos Origin = originOf(Pos, State);
            Direction Front = State.getValue(Facing);
            if (!Dismantling && World.getBlockEntity(Origin) instanceof LaunchPlatformBlockEntity Platform) {
                Platform.dropContents(World, Pos);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    for (int Index = 0; Index < layout().cellCount(); Index++) {
                        BlockPos Cell = layout().cellPos(Origin, Front, Index);
                        if (Cell.equals(Pos)) {
                            continue;
                        }
                        BlockState CellState = World.getBlockState(Cell);
                        if (CellState.is(this) && getPart(CellState) == Index && CellState.getValue(Facing) == Front) {
                            World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                        }
                    }
                } finally {
                    Dismantling = false;
                }
            }
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
                if (getPart(State) == fuelPart()) {
                    PressureNetworkManager.blockChanged(ServerWorld, Pos);
                }
            }
            World.invalidateCapabilities(Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            LaunchPlatformBlockEntity Platform = masterEntity(World, Pos, State);
            if (Platform != null) {
                User.openMenu(Platform);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        LaunchPlatformBlockEntity Platform = masterEntity(World, Pos, State);
        if (Platform != null && FluidHands.interact(User, Hand, Platform.getFuelHandler())) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
        Lines.add(Component.translatable("block.scp_unleashed." + kind().getName() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            appendDetails(Lines);
        }
    }

    protected static Component detail(String Key, Object... Arguments) {
        return Component.translatable(Key, Arguments).withStyle(ChatFormatting.GOLD);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isMaster(State) ? createMaster(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!isMaster(State) || Type != entityType()) {
            return null;
        }
        BlockEntityTicker<LaunchPlatformBlockEntity> Ticker = World.isClientSide ? LaunchPlatformBlockEntity::clientTick : LaunchPlatformBlockEntity::serverTick;
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> Cast = (BlockEntityTicker<T>) Ticker;
        return Cast;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return isFuelFace(State, Side) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        LaunchPlatformBlockEntity Platform = masterEntity(World, Pos, State);
        return Platform == null || getPart(State) != fuelPart() ? null : Platform.getFuelHandler();
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return originOf(Pos, State);
    }
}
