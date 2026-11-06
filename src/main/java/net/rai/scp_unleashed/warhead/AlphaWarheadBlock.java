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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AlphaWarheadBlock extends BaseEntityBlock implements LargeBlock {
    public static final MapCodec<AlphaWarheadBlock> BlockCodec = simpleCodec(AlphaWarheadBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, WarheadGeometry.WarheadCells - 1);
    public static final EnumProperty<WarheadStatus> Status = EnumProperty.create("status", WarheadStatus.class);
    public static final int ControlPart = WarheadGeometry.WarheadOrigin;
    public static final int[] BeaconParts = {3, 5, 9, 11};
    private static final VoxelShape[] CellShapes = new VoxelShape[4 * WarheadGeometry.WarheadCells];
    private static boolean Dismantling;

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Index = 0; Index < WarheadGeometry.WarheadCells; Index++) {
                CellShapes[slot(Front, Index)] = WarheadGeometry.cellShape(WarheadGeometry.WarheadBoxes, Front,
                        WarheadGeometry.column(Index, WarheadGeometry.WarheadWidth), WarheadGeometry.row(Index, WarheadGeometry.WarheadWidth, WarheadGeometry.WarheadHeight),
                        WarheadGeometry.layer(Index, WarheadGeometry.WarheadWidth, WarheadGeometry.WarheadHeight));
            }
        }
    }

    public AlphaWarheadBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, WarheadGeometry.WarheadOrigin).setValue(Status, WarheadStatus.Idle));
    }

    private static int slot(Direction Front, int Index) {
        return Front.get2DDataValue() * WarheadGeometry.WarheadCells + Index;
    }

    public static int lightOf(BlockState State) {
        WarheadStatus Current = State.getValue(Status);
        int Index = State.getValue(Part);
        if (Index == ControlPart) {
            return switch (Current) {
                case Idle -> 3;
                case Armed -> 4;
                case Counting -> 6;
                case Locked -> 8;
            };
        }
        for (int Beacon : BeaconParts) {
            if (Beacon == Index) {
                return Current == WarheadStatus.Locked ? 8 : Current == WarheadStatus.Counting ? 6 : 0;
            }
        }
        return 0;
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Index) {
        return WarheadGeometry.cellPos(Origin, Front, Index, WarheadGeometry.WarheadWidth, WarheadGeometry.WarheadHeight);
    }

    public static BlockPos masterOf(BlockPos Cell, BlockState State) {
        return WarheadGeometry.originOf(Cell, State.getValue(Facing), State.getValue(Part), WarheadGeometry.WarheadWidth, WarheadGeometry.WarheadHeight);
    }

    public static boolean isMaster(BlockState State) {
        return State.getBlock() instanceof AlphaWarheadBlock && State.getValue(Part) == WarheadGeometry.WarheadOrigin;
    }

    @Nullable
    public static AlphaWarheadBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        return World.getBlockEntity(masterOf(Cell, State)) instanceof AlphaWarheadBlockEntity Warhead ? Warhead : null;
    }

    public static Vec3 centre(BlockPos Origin, BlockState State) {
        return WarheadGeometry.toWorld(WarheadGeometry.WarheadCentre, Origin, State.getValue(Facing));
    }

    public static void applyStatus(Level World, BlockPos Origin, BlockState State, WarheadStatus Wanted) {
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < WarheadGeometry.WarheadCells; Index++) {
            BlockPos Cell = cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.getBlock() instanceof AlphaWarheadBlock && Current.getValue(Part) == Index && Current.getValue(Status) != Wanted) {
                World.setBlock(Cell, Current.setValue(Status, Wanted), Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void consume(ServerLevel World, BlockPos Origin) {
        World.setBlock(Origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        List<BlockPos> Cells = new ArrayList<>(WarheadGeometry.WarheadCells);
        Cells.add(Context.getClickedPos());
        for (int Index = 0; Index < WarheadGeometry.WarheadCells; Index++) {
            if (Index != WarheadGeometry.WarheadOrigin) {
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
        Builder.add(Facing, Part, Status);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return CellShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return CellShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, WarheadGeometry.WarheadOrigin);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < WarheadGeometry.WarheadCells; Index++) {
            if (Index != WarheadGeometry.WarheadOrigin) {
                World.setBlock(cellPos(Pos, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && isMaster(State) && World instanceof ServerLevel ServerWorld) {
            WarheadNetwork.get(ServerWorld).addWarhead(Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            BlockPos Origin = masterOf(Pos, State);
            if (World instanceof ServerLevel ServerWorld) {
                WarheadNetwork.get(ServerWorld).removeWarhead(Origin);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    Direction Front = State.getValue(Facing);
                    for (int Index = 0; Index < WarheadGeometry.WarheadCells; Index++) {
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
        if (State.getValue(Status).isActive()) {
            return 0.0f;
        }
        return super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel ServerWorld) {
            BlockPos Origin = masterOf(Pos, State);
            WarheadNetwork Network = WarheadNetwork.get(ServerWorld);
            WarheadStatus Current = Network.statusFor(ServerWorld, Origin);
            Component Message = switch (Current) {
                case Idle -> Component.translatable("message.scp_unleashed.warhead.status_idle", Network.panelsNear(ServerWorld, Origin, WarheadNetwork.LinkRange)).withStyle(ChatFormatting.GRAY);
                case Armed -> Component.translatable("message.scp_unleashed.warhead.status_armed").withStyle(ChatFormatting.GOLD);
                case Counting -> Component.translatable("message.scp_unleashed.warhead.status_counting", Network.secondsLeftFor(ServerWorld, Origin)).withStyle(ChatFormatting.RED);
                case Locked -> Component.translatable("message.scp_unleashed.warhead.status_locked", Network.secondsLeftFor(ServerWorld, Origin)).withStyle(ChatFormatting.DARK_RED);
            };
            User.displayClientMessage(Message, true);
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
        Lines.add(Component.translatable("block.scp_unleashed.warhead_alpha.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.warhead_alpha.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_alpha.linking", (int) WarheadNetwork.LinkRange).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_alpha.blast", (int) WarheadNetwork.KillRange).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_alpha.crater").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.warhead_alpha.safety").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == WarheadGeometry.WarheadOrigin ? new AlphaWarheadBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || State.getValue(Part) != WarheadGeometry.WarheadOrigin) {
            return null;
        }
        return createTickerHelper(Type, WarheadRegistry.AlphaWarheadEntity.get(), AlphaWarheadBlockEntity::serverTick);
    }
}
