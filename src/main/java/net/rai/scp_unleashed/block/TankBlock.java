package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.entity.TankBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TankBlock extends BaseEntityBlock implements LargeBlock, FluidPort {
    public static final MapCodec<TankBlock> BlockCodec = simpleCodec(TankBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 26);
    public static final int Capacity = 128 * FluidType.BUCKET_VOLUME;
    public static final int MasterPart = index(1, 1, 1);
    public static final int InputPart = index(1, 2, 0);
    public static final int OutputPart = index(1, 0, 0);
    public static final int OriginPart = OutputPart;
    private static boolean Dismantling;

    public TankBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, OriginPart));
    }

    public static int index(int X, int Y, int Z) {
        return X + Y * 3 + Z * 9;
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
        int X = Index % 3;
        int Y = Index / 3 % 3;
        int Z = Index / 9;
        return turn(X - 1, Y, Z, Front);
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

    @Nullable
    public static TankBlockEntity masterEntity(Level World, BlockPos CellPos, BlockState State) {
        return World.getBlockEntity(masterOf(CellPos, State)) instanceof TankBlockEntity Tank ? Tank : null;
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        TankBlockEntity Tank = masterEntity(World, Pos, State);
        if (Tank == null) {
            return null;
        }
        if (Side == null) {
            return Tank.getTank();
        }
        PortRole Role = ((TankBlock) State.getBlock()).portRole(State, Side);
        return Role == null ? null : new RestrictedFluidHandler(Tank.getTank(), Role);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        List<BlockPos> Cells = new ArrayList<>(27);
        for (int Index = 0; Index < 27; Index++) {
            Cells.add(cellPos(Context.getClickedPos(), Front, Index));
        }
        Cells.remove(cellPos(Context.getClickedPos(), Front, OriginPart));
        Cells.addFirst(Context.getClickedPos());
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
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, OriginPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < 27; Index++) {
            if (Index != OriginPart) {
                World.setBlock(cellPos(Pos, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            Dismantling = true;
            try {
                BlockPos Origin = originOf(Pos, State);
                Direction Front = State.getValue(Facing);
                for (int Index = 0; Index < 27; Index++) {
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
                PressureNetworkManager.blockChanged(ServerWorld, Pos);
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            TankBlockEntity Tank = masterEntity(World, Pos, State);
            if (Tank != null) {
                FluidHands.interact(User, Hand, Tank.getTank());
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        if (Side != State.getValue(Facing)) {
            return null;
        }
        int Index = State.getValue(Part);
        return Index == InputPart ? PortRole.Input : Index == OutputPart ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        TankBlockEntity Tank = masterEntity(World, Pos, State);
        return Tank == null ? null : Tank.getTank();
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return masterOf(Pos, State);
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
        Lines.add(Component.translatable("block.scp_unleashed.tank.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.tank.capacity", Capacity / FluidType.BUCKET_VOLUME).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.tank.valves").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == MasterPart ? new TankBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != MasterPart) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, ModBlockEntityTypes.Tank.get(), TankBlockEntity::clientTick)
                : createTickerHelper(Type, ModBlockEntityTypes.Tank.get(), TankBlockEntity::serverTick);
    }
}
