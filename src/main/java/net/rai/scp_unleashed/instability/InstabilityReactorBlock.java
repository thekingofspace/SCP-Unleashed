package net.rai.scp_unleashed.instability;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InstabilityReactorBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock {
    public static final MapCodec<InstabilityReactorBlock> BlockCodec = simpleCodec(InstabilityReactorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 7);
    public static final EnumProperty<ReactorStatus> Status = EnumProperty.create("status", ReactorStatus.class);
    public static final int Width = 2;
    public static final int Height = 2;
    public static final int Depth = 2;
    public static final int CellCount = Width * Height * Depth;
    public static final int OriginPart = index(1, 0, 0);
    public static final int MasterPart = OriginPart;
    private static final VoxelShape CapSupport = box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static boolean Dismantling;

    public InstabilityReactorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, OriginPart).setValue(Status, ReactorStatus.Off));
    }

    public static int index(int X, int Y, int Z) {
        return X + Y * Width + Z * Width * Height;
    }

    public static int column(int Index) {
        return Index % Width;
    }

    public static int row(int Index) {
        return Index / Width % Height;
    }

    public static int layer(int Index) {
        return Index / (Width * Height);
    }

    public static int lightOf(BlockState State) {
        boolean Upper = row(State.getValue(Part)) == 1;
        return switch (State.getValue(Status)) {
            case Running -> Upper ? 14 : 6;
            case Idle -> Upper ? 8 : 3;
            default -> 0;
        };
    }

    private static BlockPos turn(int X, int Y, int Z, Direction Front) {
        return switch (Front) {
            case EAST -> new BlockPos(-Z, Y, X);
            case SOUTH -> new BlockPos(-X, Y, -Z);
            case WEST -> new BlockPos(Z, Y, -X);
            default -> new BlockPos(X, Y, Z);
        };
    }

    private static BlockPos offsetOf(int Index, Direction Front) {
        return turn(column(Index) - column(OriginPart), row(Index), layer(Index), Front);
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Index) {
        return Origin.offset(offsetOf(Index, Front));
    }

    public static BlockPos originOf(BlockPos CellPos, BlockState State) {
        return CellPos.subtract(offsetOf(State.getValue(Part), State.getValue(Facing)));
    }

    public static BlockPos masterOf(BlockPos CellPos, BlockState State) {
        return cellPos(originOf(CellPos, State), State.getValue(Facing), MasterPart);
    }

    public static List<BlockPos> cells(BlockPos Origin, Direction Front) {
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        for (int Index = 0; Index < CellCount; Index++) {
            Cells.add(cellPos(Origin, Front, Index));
        }
        return Cells;
    }

    @Nullable
    public static InstabilityReactorBlockEntity masterEntity(Level World, BlockPos CellPos, BlockState State) {
        if (!(State.getBlock() instanceof InstabilityReactorBlock)) {
            return null;
        }
        return World.getBlockEntity(masterOf(CellPos, State)) instanceof InstabilityReactorBlockEntity Reactor ? Reactor : null;
    }

    public static void setStatus(Level World, BlockPos Origin, BlockState OriginState, ReactorStatus Next) {
        Direction Front = OriginState.getValue(Facing);
        for (int Index = 0; Index < CellCount; Index++) {
            BlockPos Cell = cellPos(Origin, Front, Index);
            BlockState CellState = World.getBlockState(Cell);
            if (CellState.is(OriginState.getBlock()) && CellState.getValue(Part) == Index && CellState.getValue(Status) != Next) {
                World.setBlock(Cell, CellState.setValue(Status, Next), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        Cells.add(Context.getClickedPos());
        for (int Index = 0; Index < CellCount; Index++) {
            if (Index != OriginPart) {
                Cells.add(cellPos(Context.getClickedPos(), Front, Index));
            }
        }
        return Cells;
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
        Builder.add(Facing, Part, Status);
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
        return row(State.getValue(Part)) == 0 ? Shapes.block() : CapSupport;
    }

    @Override
    protected boolean skipRendering(BlockState State, BlockState Adjacent, Direction Side) {
        return Adjacent.is(this) || super.skipRendering(State, Adjacent, Side);
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
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < CellCount; Index++) {
            if (Index != OriginPart) {
                World.setBlock(cellPos(Pos, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
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
            InstabilityReactorBlockEntity Reactor = masterEntity(World, Pos, State);
            if (Reactor != null) {
                Reactor.dropContents(World, Reactor.getBlockPos());
            }
            Dismantling = true;
            try {
                BlockPos Origin = originOf(Pos, State);
                Direction Front = State.getValue(Facing);
                for (int Index = 0; Index < CellCount; Index++) {
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
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.reactor.decommissioned").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        InstabilityReactorBlockEntity Reactor = masterEntity(World, Pos, State);
        return Reactor == null ? 0 : ItemHandlerHelper.calcRedstoneFromInventory(Reactor.getFuel());
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
        Lines.add(Component.translatable("block.scp_unleashed.reactor.decommissioned").withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("block.scp_unleashed.reactor.decommissioned.hint").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == MasterPart ? new InstabilityReactorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != MasterPart) {
            return null;
        }
        return World.isClientSide ? null : createTickerHelper(Type, InstabilityRegistry.ReactorEntity.get(), InstabilityReactorBlockEntity::serverTick);
    }
}
