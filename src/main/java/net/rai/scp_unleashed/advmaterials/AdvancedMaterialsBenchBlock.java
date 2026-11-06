package net.rai.scp_unleashed.advmaterials;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AdvancedMaterialsBenchBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock {
    public static final MapCodec<AdvancedMaterialsBenchBlock> BlockCodec = simpleCodec(AdvancedMaterialsBenchBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, BenchLayout.CellCount - 1);
    public static final EnumProperty<BenchLight> Light = EnumProperty.create("light", BenchLight.class);
    public static final int ScreenLight = 6;
    public static final int HeadLight = 9;
    public static final int StandbyLight = 2;
    private static boolean Dismantling;

    public AdvancedMaterialsBenchBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, BenchLayout.MasterPart).setValue(Light, BenchLight.Off));
    }

    public static int lightOf(BlockState State) {
        int Index = State.getValue(Part);
        BenchLight Shown = State.getValue(Light);
        if (Index == BenchLayout.HeadPart) {
            return Shown == BenchLight.Working ? HeadLight : 0;
        }
        if (Index == BenchLayout.ScreenPart) {
            return switch (Shown) {
                case Off -> 0;
                case Idle -> StandbyLight;
                default -> ScreenLight;
            };
        }
        return 0;
    }

    public static BlockPos originOf(BlockPos Cell, BlockState State) {
        return BenchLayout.originOf(Cell, State.getValue(Facing), State.getValue(Part));
    }

    public static boolean isMaster(BlockState State) {
        return State.getValue(Part) == BenchLayout.MasterPart;
    }

    @Nullable
    public static AdvancedMaterialsBenchBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        if (!(State.getBlock() instanceof AdvancedMaterialsBenchBlock)) {
            return null;
        }
        return World.getBlockEntity(originOf(Cell, State)) instanceof AdvancedMaterialsBenchBlockEntity Machine ? Machine : null;
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        AdvancedMaterialsBenchBlockEntity Machine = masterEntity(World, Pos, State);
        return Machine == null ? null : Machine.getItemHandler(Side);
    }

    public static void applyLight(Level World, BlockPos Origin, BlockState MasterState, BenchLight Wanted) {
        Direction Front = MasterState.getValue(Facing);
        for (int Index = 0; Index < BenchLayout.CellCount; Index++) {
            BlockPos Cell = BenchLayout.cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.getBlock() instanceof AdvancedMaterialsBenchBlock && Current.getValue(Part) == Index && Current.getValue(Facing) == Front && Current.getValue(Light) != Wanted) {
                World.setBlock(Cell, Current.setValue(Light, Wanted), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static BlockPos placementOrigin(BlockPos Clicked, Direction Front) {
        return BenchLayout.originOf(Clicked, Front, BenchLayout.ClickedPart);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        return BenchLayout.cells(placementOrigin(Context.getClickedPos(), Front), Front);
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
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return BenchLayout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return BenchLayout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, BenchLayout.ClickedPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        Direction Front = State.getValue(Facing);
        BlockPos Origin = originOf(Pos, State);
        for (int Index = 0; Index < BenchLayout.CellCount; Index++) {
            if (Index != BenchLayout.ClickedPart) {
                World.setBlock(BenchLayout.cellPos(Origin, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
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
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            BlockPos Origin = originOf(Pos, State);
            Direction Front = State.getValue(Facing);
            if (World.getBlockEntity(Origin) instanceof AdvancedMaterialsBenchBlockEntity Machine) {
                Machine.dropContents(World, Pos);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    for (int Index = 0; Index < BenchLayout.CellCount; Index++) {
                        BlockPos Cell = BenchLayout.cellPos(Origin, Front, Index);
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
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            AdvancedMaterialsBenchBlockEntity Machine = masterEntity(World, Pos, State);
            if (Machine != null) {
                User.openMenu(Machine, Machine::writeMenuState);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        AdvancedMaterialsBenchBlockEntity Machine = masterEntity(World, Pos, State);
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
        Lines.add(Component.translatable("block.scp_unleashed.advanced_materials_bench.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.advanced_materials_bench.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.advanced_materials_bench.draw", AdvancedMaterialsBenchBlockEntity.WorkDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.advanced_materials_bench.slots", AdvancedMaterialsBenchBlockEntity.InputCount).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.machine.speed_upgrades", UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.advanced_materials_bench.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.advanced_materials_bench.automation").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isMaster(State) ? new AdvancedMaterialsBenchBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!isMaster(State)) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, AdvancedMaterialsRegistry.BenchEntity.get(), AdvancedMaterialsBenchBlockEntity::clientTick)
                : createTickerHelper(Type, AdvancedMaterialsRegistry.BenchEntity.get(), AdvancedMaterialsBenchBlockEntity::serverTick);
    }
}
