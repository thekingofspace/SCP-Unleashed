package net.rai.scp_unleashed.solar;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarArrayBlock extends BaseEntityBlock implements WireHostBlock, LargeBlock {
    public static final MapCodec<SolarArrayBlock> BlockCodec = simpleCodec(SolarArrayBlock::new);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 3);
    public static final BooleanProperty Generating = BooleanProperty.create("generating");
    public static final int MasterPart = 0;
    public static final int CellCount = 4;
    private static final VoxelShape NorthSupport = box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape SouthSupport = box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static boolean Dismantling;

    public SolarArrayBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Part, MasterPart).setValue(Generating, false));
    }

    public static int column(int Index) {
        return Index % 2;
    }

    public static int row(int Index) {
        return Index / 2;
    }

    public static int partAt(int Column, int Row) {
        return Column + Row * 2;
    }

    public static BlockPos masterOf(BlockPos Pos, BlockState State) {
        int Index = State.getValue(Part);
        return Pos.offset(-column(Index), 0, -row(Index));
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return masterOf(Pos, State);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Part, Generating);
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
        return row(State.getValue(Part)) == 0 ? NorthSupport : SouthSupport;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Forward = Context.getHorizontalDirection();
        Direction Right = Forward.getClockWise();
        BlockPos Clicked = Context.getClickedPos();
        return List.of(Clicked, Clicked.relative(Forward), Clicked.relative(Right), Clicked.relative(Forward).relative(Right));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        List<BlockPos> Footprint = getFootprint(Context);
        if (!LargeBlock.canPlace(Context, Footprint)) {
            return null;
        }
        int MinX = Integer.MAX_VALUE;
        int MinZ = Integer.MAX_VALUE;
        for (BlockPos Cell : Footprint) {
            MinX = Math.min(MinX, Cell.getX());
            MinZ = Math.min(MinZ, Cell.getZ());
        }
        BlockPos Clicked = Context.getClickedPos();
        return defaultBlockState().setValue(Part, partAt(Clicked.getX() - MinX, Clicked.getZ() - MinZ));
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        BlockPos Master = masterOf(Pos, State);
        int Placed = State.getValue(Part);
        for (int Index = 0; Index < CellCount; Index++) {
            if (Index != Placed) {
                World.setBlock(Master.offset(column(Index), 0, row(Index)), State.setValue(Part, Index), Block.UPDATE_ALL);
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
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            Dismantling = true;
            try {
                BlockPos Master = masterOf(Pos, State);
                for (int Index = 0; Index < CellCount; Index++) {
                    BlockPos Cell = Master.offset(column(Index), 0, row(Index));
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(Part) == Index) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
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
    protected BlockState rotate(BlockState State, Rotation Turn) {
        int Column = column(State.getValue(Part));
        int Row = row(State.getValue(Part));
        return switch (Turn) {
            case CLOCKWISE_90 -> State.setValue(Part, partAt(1 - Row, Column));
            case CLOCKWISE_180 -> State.setValue(Part, partAt(1 - Column, 1 - Row));
            case COUNTERCLOCKWISE_90 -> State.setValue(Part, partAt(Row, 1 - Column));
            default -> State;
        };
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        int Column = column(State.getValue(Part));
        int Row = row(State.getValue(Part));
        return switch (Flip) {
            case LEFT_RIGHT -> State.setValue(Part, partAt(Column, 1 - Row));
            case FRONT_BACK -> State.setValue(Part, partAt(1 - Column, Row));
            default -> State;
        };
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.solar_array.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.solar_array.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.peak", SolarTier.Array.getPeakOutput()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.conditions").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar_array.sky").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.buffer", SolarTier.Array.getCapacity()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.wiring").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == MasterPart ? new SolarBlockEntity(SolarTier.Array, Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != MasterPart) {
            return null;
        }
        return createTickerHelper(Type, SolarRegistry.SolarArrayEntity.get(), World.isClientSide ? SolarBlockEntity::clientTick : SolarBlockEntity::serverTick);
    }
}
