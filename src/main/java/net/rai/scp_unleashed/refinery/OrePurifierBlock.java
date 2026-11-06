package net.rai.scp_unleashed.refinery;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OrePurifierBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock, FluidPort {
    public static final MapCodec<OrePurifierBlock> BlockCodec = simpleCodec(OrePurifierBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, PurifierLayout.CellCount - 1);
    public static final EnumProperty<PurifierLight> Light = EnumProperty.create("light", PurifierLight.class);
    public static final int DrumLight = 8;
    public static final int PanelLight = 5;
    public static final int StandbyLight = 2;
    private static boolean Dismantling;

    public OrePurifierBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, PurifierLayout.MasterPart).setValue(Light, PurifierLight.Off));
    }

    public static int lightOf(BlockState State) {
        int Index = State.getValue(Part);
        PurifierLight Shown = State.getValue(Light);
        if (Index == PurifierLayout.DrumPart) {
            return Shown == PurifierLight.Working ? DrumLight : 0;
        }
        if (Index == PurifierLayout.PanelPart) {
            return switch (Shown) {
                case Off -> 0;
                case Idle -> StandbyLight;
                default -> PanelLight;
            };
        }
        return 0;
    }

    public static BlockPos originOf(BlockPos Cell, BlockState State) {
        return PurifierLayout.originOf(Cell, State.getValue(Facing), State.getValue(Part));
    }

    public static boolean isMaster(BlockState State) {
        return State.getValue(Part) == PurifierLayout.MasterPart;
    }

    @Nullable
    public static OrePurifierBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        if (!(State.getBlock() instanceof OrePurifierBlock)) {
            return null;
        }
        return World.getBlockEntity(originOf(Cell, State)) instanceof OrePurifierBlockEntity Machine ? Machine : null;
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        OrePurifierBlockEntity Machine = masterEntity(World, Pos, State);
        return Machine == null ? null : Machine.getItemHandler(Side);
    }

    @Nullable
    public static IFluidHandler fluidHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        OrePurifierBlockEntity Machine = masterEntity(World, Pos, State);
        if (Machine == null) {
            return null;
        }
        if (Side == null) {
            return Machine.getCombined();
        }
        int Index = State.getValue(Part);
        if (Side != PurifierLayout.portSide(State.getValue(Facing), Index)) {
            return null;
        }
        return Index == PurifierLayout.WaterPart ? Machine.getWaterInlet() : Machine.getWasteOutlet();
    }

    public static void applyLight(Level World, BlockPos Origin, BlockState MasterState, PurifierLight Wanted) {
        Direction Front = MasterState.getValue(Facing);
        for (int Index = 0; Index < PurifierLayout.CellCount; Index++) {
            BlockPos Cell = PurifierLayout.cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.getBlock() instanceof OrePurifierBlock && Current.getValue(Part) == Index && Current.getValue(Facing) == Front && Current.getValue(Light) != Wanted) {
                World.setBlock(Cell, Current.setValue(Light, Wanted), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        List<BlockPos> Cells = new ArrayList<>(PurifierLayout.cells(Context.getClickedPos(), Front));
        return Cells;
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return originOf(Pos, State);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Light);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return PurifierLayout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return PurifierLayout.hull(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, PurifierLayout.MasterPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < PurifierLayout.CellCount; Index++) {
            if (Index != PurifierLayout.MasterPart) {
                World.setBlock(PurifierLayout.cellPos(Pos, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
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
        if (PurifierLayout.portSide(State.getValue(Facing), State.getValue(Part)) != null) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            BlockPos Origin = originOf(Pos, State);
            Direction Front = State.getValue(Facing);
            if (World.getBlockEntity(Origin) instanceof OrePurifierBlockEntity Machine) {
                Machine.dropContents(World, Pos);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    for (int Index = 0; Index < PurifierLayout.CellCount; Index++) {
                        BlockPos Cell = PurifierLayout.cellPos(Origin, Front, Index);
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
                if (PurifierLayout.portSide(Front, State.getValue(Part)) != null) {
                    PressureNetworkManager.blockChanged(ServerWorld, Pos);
                }
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            OrePurifierBlockEntity Machine = masterEntity(World, Pos, State);
            if (Machine != null) {
                User.openMenu(Machine, Machine::writeMenuState);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            OrePurifierBlockEntity Machine = masterEntity(World, Pos, State);
            if (Machine != null) {
                FluidHands.interact(User, Hand, Machine.getCombined());
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
        OrePurifierBlockEntity Machine = masterEntity(World, Pos, State);
        return Machine == null ? 0 : Machine.comparatorLevel();
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
        Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.draw", OrePurifierBlockEntity.WorkDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.water", PurifyingRecipe.DefaultWater).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.machine.speed_upgrades", UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.ore_purifier.automation").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isMaster(State) ? new OrePurifierBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!isMaster(State)) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, RefineryRegistry.OrePurifierEntity.get(), OrePurifierBlockEntity::clientTick)
                : createTickerHelper(Type, RefineryRegistry.OrePurifierEntity.get(), OrePurifierBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        int Index = State.getValue(Part);
        if (Side != PurifierLayout.portSide(State.getValue(Facing), Index)) {
            return null;
        }
        return Index == PurifierLayout.WaterPart ? PortRole.Input : PortRole.Output;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        OrePurifierBlockEntity Machine = masterEntity(World, Pos, State);
        if (Machine == null) {
            return null;
        }
        int Index = State.getValue(Part);
        if (Index == PurifierLayout.WaterPart) {
            return Machine.getWaterInlet();
        }
        return Index == PurifierLayout.WastePart ? Machine.getWasteOutlet() : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return originOf(Pos, State);
    }
}
