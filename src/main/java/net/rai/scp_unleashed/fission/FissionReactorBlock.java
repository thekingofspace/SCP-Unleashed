package net.rai.scp_unleashed.fission;

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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.DuctConnectable;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FissionReactorBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock, FluidPort, DuctConnectable {
    public static final MapCodec<FissionReactorBlock> BlockCodec = simpleCodec(FissionReactorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty CellX = IntegerProperty.create("x", 0, ReactorLayout.Last);
    public static final IntegerProperty CellY = IntegerProperty.create("y", 0, ReactorLayout.Top);
    public static final IntegerProperty CellZ = IntegerProperty.create("z", 0, ReactorLayout.Last);
    private static boolean Dismantling;
    private static boolean Melting;

    public FissionReactorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(CellX, 0).setValue(CellY, 0).setValue(CellZ, 0));
    }

    public static BlockPos originOf(BlockPos Pos, BlockState State) {
        return Pos.offset(-State.getValue(CellX), -State.getValue(CellY), -State.getValue(CellZ));
    }

    public static boolean isMaster(BlockState State) {
        return State.getValue(CellX) == 0 && State.getValue(CellY) == 0 && State.getValue(CellZ) == 0;
    }

    public static boolean isWindow(BlockState State) {
        return ReactorLayout.isWindow(State.getValue(Facing), State.getValue(CellX), State.getValue(CellY), State.getValue(CellZ));
    }

    public static boolean isPowerPort(BlockState State) {
        return ReactorLayout.isPowerPort(State.getValue(Facing), State.getValue(CellX), State.getValue(CellY), State.getValue(CellZ));
    }

    public static boolean isWastePort(BlockState State) {
        return ReactorLayout.isWastePort(State.getValue(Facing), State.getValue(CellX), State.getValue(CellY), State.getValue(CellZ));
    }

    public static boolean isGasPort(BlockState State) {
        return ReactorLayout.isGasPort(State.getValue(Facing), State.getValue(CellX), State.getValue(CellY), State.getValue(CellZ));
    }

    public static boolean isEnclosed(BlockState State) {
        return ReactorLayout.isEnclosed(State.getValue(CellX), State.getValue(CellY), State.getValue(CellZ));
    }

    @Nullable
    public static Direction portOutward(BlockState State) {
        return ReactorLayout.portOutward(State.getValue(Facing), State.getValue(CellX), State.getValue(CellY), State.getValue(CellZ));
    }

    @Nullable
    public static FissionReactorBlockEntity masterEntity(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof FissionReactorBlock)) {
            return null;
        }
        BlockPos Origin = originOf(Pos, State);
        return World.isLoaded(Origin) && World.getBlockEntity(Origin) instanceof FissionReactorBlockEntity Reactor ? Reactor : null;
    }

    @Nullable
    public static IFluidHandler fluidHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!isWastePort(State) || (Side != null && Side != portOutward(State))) {
            return null;
        }
        FissionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
        return Reactor == null ? null : new RestrictedFluidHandler(Reactor.getWasteTank(), PortRole.Output);
    }

    public static void meltdown(ServerLevel World, BlockPos Origin, BlockState MasterState) {
        Melting = true;
        try {
            if (World.getBlockState(Origin).is(MasterState.getBlock())) {
                World.setBlock(Origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        } finally {
            Melting = false;
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        BlockPos Clicked = Context.getClickedPos();
        List<BlockPos> Cells = ReactorLayout.cells(ReactorLayout.originFor(Clicked, Front));
        Cells.remove(Clicked);
        Cells.addFirst(Clicked);
        return Cells;
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        if (isPowerPort(State)) {
            return originOf(Pos, State);
        }
        return isMaster(State) ? Pos.above() : Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, CellX, CellY, CellZ);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return isEnclosed(State) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return isWindow(State) ? Shapes.empty() : Shapes.block();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (Context.getLevel().isClientSide && Context.getPlayer() != null) {
            Context.getPlayer().displayClientMessage(Component.translatable("message.scp_unleashed.reactor.decommissioned").withStyle(ChatFormatting.GOLD), true);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        BlockPos Origin = originOf(Pos, State);
        for (int Row = 0; Row < ReactorLayout.Height; Row++) {
            for (int Z = 0; Z < ReactorLayout.Width; Z++) {
                for (int X = 0; X < ReactorLayout.Width; X++) {
                    BlockPos Cell = Origin.offset(X, Row, Z);
                    if (!Cell.equals(Pos)) {
                        World.setBlock(Cell, State.setValue(CellX, X).setValue(CellY, Row).setValue(CellZ, Z), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (OldState.is(this) || !(World instanceof ServerLevel ServerWorld)) {
            return;
        }
        if (isPowerPort(State)) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        if (isWastePort(State)) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            BlockPos Origin = originOf(Pos, State);
            Direction Front = State.getValue(Facing);
            if (!Melting && World.getBlockEntity(Origin) instanceof FissionReactorBlockEntity Reactor) {
                Reactor.dropContents(World, Pos);
            }
            Dismantling = true;
            try {
                for (int Row = 0; Row < ReactorLayout.Height; Row++) {
                    for (int Z = 0; Z < ReactorLayout.Width; Z++) {
                        for (int X = 0; X < ReactorLayout.Width; X++) {
                            BlockPos Cell = Origin.offset(X, Row, Z);
                            if (!Cell.equals(Pos) && ReactorLayout.matches(World.getBlockState(Cell), Front, X, Row, Z)) {
                                World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                            }
                        }
                    }
                }
            } finally {
                Dismantling = false;
            }
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbour, BlockPos NeighbourPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbour, NeighbourPos, MovedByPiston);
        if (!World.isClientSide && !isEnclosed(State)) {
            FissionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
            if (Reactor != null) {
                Reactor.markRedstoneDirty();
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.reactor.decommissioned").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!isWastePort(State) || FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            FissionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
            if (Reactor != null) {
                FluidHands.interact(User, Hand, new RestrictedFluidHandler(Reactor.getWasteTank(), PortRole.Output));
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        FissionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
        return Reactor == null ? 0 : Reactor.comparatorLevel();
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        int X = State.getValue(CellX);
        int Z = State.getValue(CellZ);
        int Last = ReactorLayout.Last;
        int[] Turned = switch (Turn) {
            case CLOCKWISE_90 -> new int[]{Last - Z, X};
            case CLOCKWISE_180 -> new int[]{Last - X, Last - Z};
            case COUNTERCLOCKWISE_90 -> new int[]{Z, Last - X};
            default -> new int[]{X, Z};
        };
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing))).setValue(CellX, Turned[0]).setValue(CellZ, Turned[1]);
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.reactor.decommissioned").withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("block.scp_unleashed.reactor.decommissioned.hint").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isMaster(State) ? new FissionReactorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return null;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return isWastePort(State) && Side == portOutward(State) ? PortRole.Output : null;
    }

    @Override
    public boolean acceptsDuct(BlockState State, Direction Side, DuctKind Kind) {
        return Kind == DuctKind.Vent && isGasPort(State) && Side == portOutward(State);
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        FissionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
        return Reactor == null ? null : new RestrictedFluidHandler(Reactor.getWasteTank(), PortRole.Output);
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return originOf(Pos, State);
    }
}
