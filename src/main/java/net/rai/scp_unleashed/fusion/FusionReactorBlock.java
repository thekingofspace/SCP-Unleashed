package net.rai.scp_unleashed.fusion;

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
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FusionReactorBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock, FluidPort {
    public static final MapCodec<FusionReactorBlock> BlockCodec = simpleCodec(FusionReactorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, FusionLayout.CellCount - 1);
    public static final EnumProperty<FusionGlow> Glow = EnumProperty.create("glow", FusionGlow.class);
    private static boolean Dismantling;

    public FusionReactorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, FusionLayout.OriginPart).setValue(Glow, FusionGlow.Off));
    }

    public static int lightOf(BlockState State) {
        int Index = State.getValue(Part);
        FusionGlow Shown = State.getValue(Glow);
        if (FusionLayout.isViewport(Index)) {
            return switch (Shown) {
                case Running -> 13;
                case Igniting -> 15;
                case Charging -> 6;
                case Fading -> 8;
                default -> 0;
            };
        }
        if (FusionLayout.isSeam(Index)) {
            return switch (Shown) {
                case Running -> 9;
                case Igniting -> 11;
                case Charging, Fading -> 4;
                default -> 0;
            };
        }
        if (Index == FusionLayout.CrownPart) {
            return switch (Shown) {
                case Running, Igniting -> 10;
                case Charging, Fading -> 5;
                default -> 0;
            };
        }
        if (Index == FusionLayout.ConsolePart) {
            return Shown == FusionGlow.Off ? 0 : 5;
        }
        if (Index == FusionLayout.InputPart) {
            return Shown == FusionGlow.Charging ? 7 : 0;
        }
        if (Index == FusionLayout.OutputPart) {
            return Shown == FusionGlow.Running ? 7 : 0;
        }
        return 0;
    }

    @Nullable
    public static FusionReactorBlockEntity masterEntity(Level World, BlockPos CellPos, BlockState State) {
        if (!(State.getBlock() instanceof FusionReactorBlock)) {
            return null;
        }
        BlockPos MasterPos = FusionLayout.masterOf(CellPos, State);
        return World.isLoaded(MasterPos) && World.getBlockEntity(MasterPos) instanceof FusionReactorBlockEntity Reactor ? Reactor : null;
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (Side == null || State.getValue(Part) != FusionLayout.InletPart || Side != FusionLayout.portFace(FusionLayout.InletPart, State.getValue(Facing))) {
            return null;
        }
        FusionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
        return Reactor == null ? null : Reactor.getInlet();
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return FusionLayout.isPort(State.getValue(Part)) ? Pos : FusionLayout.masterOf(Pos, State);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        List<BlockPos> Cells = new ArrayList<>(FusionLayout.CellCount);
        Cells.add(Context.getClickedPos());
        for (int Index = 0; Index < FusionLayout.CellCount; Index++) {
            if (Index != FusionLayout.OriginPart) {
                Cells.add(FusionLayout.cellPos(Context.getClickedPos(), Front, Index));
            }
        }
        return Cells;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Glow);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return FusionLayout.isPort(State.getValue(Part)) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, FusionLayout.OriginPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < FusionLayout.CellCount; Index++) {
            if (Index != FusionLayout.OriginPart) {
                World.setBlock(FusionLayout.cellPos(Pos, Front, Index), State.setValue(Part, Index).setValue(Glow, FusionGlow.Off), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
            if (State.getValue(Part) == FusionLayout.InletPart) {
                PressureNetworkManager.blockChanged(ServerWorld, Pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            if (State.getValue(Part) == FusionLayout.MasterPart && World.getBlockEntity(Pos) instanceof FusionReactorBlockEntity Reactor) {
                Reactor.dropContents(World, Pos);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    BlockPos Origin = FusionLayout.originOf(Pos, State);
                    Direction Front = State.getValue(Facing);
                    for (int Index = 0; Index < FusionLayout.CellCount; Index++) {
                        BlockPos Cell = FusionLayout.cellPos(Origin, Front, Index);
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
            }
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
                if (State.getValue(Part) == FusionLayout.InletPart) {
                    PressureNetworkManager.blockChanged(ServerWorld, Pos);
                }
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbour, BlockPos NeighbourPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbour, NeighbourPos, MovedByPiston);
        if (!World.isClientSide && FusionLayout.isPort(State.getValue(Part))) {
            FusionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
            if (Reactor != null) {
                Reactor.markBusesDirty();
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        int Index = State.getValue(Part);
        boolean Port = FusionLayout.isPort(Index) || Index == FusionLayout.InletPart;
        if (Port && Stack.getItem() instanceof BlockItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (Index != FusionLayout.InletPart || FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            FusionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
            if (Reactor != null) {
                FluidHands.interact(User, Hand, Reactor.getInlet());
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            FusionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
            if (Reactor != null) {
                User.openMenu(Reactor);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return State.getValue(Part) == FusionLayout.InletPart && Side == FusionLayout.portFace(FusionLayout.InletPart, State.getValue(Facing)) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        FusionReactorBlockEntity Reactor = masterEntity(World, Pos, State);
        return Reactor == null ? null : Reactor.getWater();
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return FusionLayout.masterOf(Pos, State);
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
        Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.output", grouped(FusionReactorBlockEntity.MaxOutput)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.startup", grouped(FusionReactorBlockEntity.StartupEnergy)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.fuel", FusionReactorBlockEntity.FuelTicks / 1200).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.water", FusionReactorBlockEntity.WaterPerTick, FusionReactorBlockEntity.WaterCapacity / FluidType.BUCKET_VOLUME).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fusion_reactor.clean").withStyle(ChatFormatting.GREEN));
        }
    }

    public static String grouped(long Value) {
        return String.format(Locale.ROOT, "%,d", Value);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        int Index = State.getValue(Part);
        if (Index == FusionLayout.MasterPart) {
            return new FusionReactorBlockEntity(Pos, State);
        }
        return FusionLayout.isPort(Index) ? new FusionPortBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != FusionLayout.MasterPart) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, FusionRegistry.ReactorEntity.get(), FusionReactorBlockEntity::clientTick)
                : createTickerHelper(Type, FusionRegistry.ReactorEntity.get(), FusionReactorBlockEntity::serverTick);
    }
}
