package net.rai.scp_unleashed.warhead;

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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WarheadPanelBlock extends BaseEntityBlock implements LargeBlock {
    public static final MapCodec<WarheadPanelBlock> BlockCodec = simpleCodec(WarheadPanelBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, WarheadGeometry.PanelCells - 1);
    private static boolean Dismantling;

    public WarheadPanelBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, WarheadGeometry.PanelOrigin));
    }

    public static int lightOf(BlockState State) {
        return WarheadGeometry.row(State.getValue(Part), WarheadGeometry.PanelWidth, WarheadGeometry.PanelHeight) == 1 ? 4 : 2;
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Index) {
        return WarheadGeometry.cellPos(Origin, Front, Index, WarheadGeometry.PanelWidth, WarheadGeometry.PanelHeight);
    }

    public static BlockPos masterOf(BlockPos Cell, BlockState State) {
        return WarheadGeometry.originOf(Cell, State.getValue(Facing), State.getValue(Part), WarheadGeometry.PanelWidth, WarheadGeometry.PanelHeight);
    }

    public static boolean isMaster(BlockState State) {
        return State.getBlock() instanceof WarheadPanelBlock && State.getValue(Part) == WarheadGeometry.PanelOrigin;
    }

    @Nullable
    public static WarheadPanelBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        return World.getBlockEntity(masterOf(Cell, State)) instanceof WarheadPanelBlockEntity Panel ? Panel : null;
    }

    public static void updateAll(Level World, BlockPos Origin, BlockState State) {
        if (!(State.getBlock() instanceof WarheadPanelBlock Console)) {
            return;
        }
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < WarheadGeometry.PanelCells; Index++) {
            BlockPos Cell = cellPos(Origin, Front, Index);
            World.updateNeighborsAt(Cell, Console);
            World.updateNeighbourForOutputSignal(Cell, Console);
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        List<BlockPos> Cells = new ArrayList<>(WarheadGeometry.PanelCells);
        Cells.add(Context.getClickedPos());
        for (int Index = 0; Index < WarheadGeometry.PanelCells; Index++) {
            if (Index != WarheadGeometry.PanelOrigin) {
                Cells.add(cellPos(Context.getClickedPos(), Front, Index));
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
        Builder.add(Facing, Part);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, WarheadGeometry.PanelOrigin);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < WarheadGeometry.PanelCells; Index++) {
            if (Index != WarheadGeometry.PanelOrigin) {
                World.setBlock(cellPos(Pos, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            BlockPos Origin = masterOf(Pos, State);
            if (World instanceof ServerLevel ServerWorld) {
                WarheadNetwork Network = WarheadNetwork.get(ServerWorld);
                Network.removePanel(Origin);
                Network.refreshNear(ServerWorld, Origin);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    Direction Front = State.getValue(Facing);
                    for (int Index = 0; Index < WarheadGeometry.PanelCells; Index++) {
                        BlockPos Cell = cellPos(Origin, Front, Index);
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
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        WarheadPanelBlockEntity Panel = masterEntity(World, Pos, State);
        if (Panel != null && Panel.isEmitting()) {
            return 0.0f;
        }
        return super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel ServerWorld) {
            WarheadPanelBlockEntity Panel = masterEntity(World, Pos, State);
            if (Panel != null) {
                Panel.interact(ServerWorld, User, Hit);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState State) {
        return true;
    }

    @Override
    protected int getSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        WarheadPanelBlockEntity Panel = masterEntity(World, Pos, State);
        return Panel != null && Panel.isEmitting() ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        WarheadPanelBlockEntity Panel = masterEntity(World, Pos, State);
        return Panel == null ? 0 : Panel.comparatorLevel(World.getGameTime());
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
        Lines.add(Component.translatable("block.scp_unleashed.warhead_panel.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.warhead_panel.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_panel.sequence").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_panel.countdown", WarheadNetwork.CountdownTicks / 20, WarheadNetwork.LockTicks / 20).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_panel.linking", (int) WarheadNetwork.LinkRange).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_panel.redstone").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == WarheadGeometry.PanelOrigin ? new WarheadPanelBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != WarheadGeometry.PanelOrigin) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, WarheadRegistry.PanelEntity.get(), WarheadPanelBlockEntity::clientTick)
                : createTickerHelper(Type, WarheadRegistry.PanelEntity.get(), WarheadPanelBlockEntity::serverTick);
    }
}
