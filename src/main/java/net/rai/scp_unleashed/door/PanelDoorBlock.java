package net.rai.scp_unleashed.door;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp005.KeyHoldable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PanelDoorBlock extends BaseEntityBlock implements LargeBlock, KeyHoldable {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final int MasterPart = 0;
    private static boolean Dismantling;

    protected PanelDoorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false).setValue(partProperty(), MasterPart));
    }

    public abstract DoorKind getKind();

    public abstract IntegerProperty partProperty();

    public int columnOf(BlockState State) {
        return getKind().columnOf(State.getValue(partProperty()));
    }

    public int rowOf(BlockState State) {
        return getKind().rowOf(State.getValue(partProperty()));
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Column, int Row) {
        return Origin.relative(DoorGeometry.rightOf(Front), Column).above(Row);
    }

    public BlockPos originOf(BlockPos Pos, BlockState State) {
        return Pos.relative(DoorGeometry.rightOf(State.getValue(Facing)), -columnOf(State)).below(rowOf(State));
    }

    public List<BlockPos> cells(BlockPos Origin, Direction Front) {
        DoorKind Kind = getKind();
        List<BlockPos> Cells = new ArrayList<>(Kind.getCellCount());
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            Cells.add(cellPos(Origin, Front, Kind.columnOf(Part), Kind.rowOf(Part)));
        }
        return Cells;
    }

    @Nullable
    public PanelDoorBlockEntity masterEntity(BlockGetter World, BlockPos Pos, BlockState State) {
        BlockPos Origin = originOf(Pos, State);
        if (World instanceof LevelReader Reader && !Reader.hasChunkAt(Origin)) {
            return null;
        }
        return World.getBlockEntity(Origin) instanceof PanelDoorBlockEntity Door ? Door : null;
    }

    public boolean anyCellPowered(Level World, BlockPos Origin, Direction Front) {
        for (BlockPos Cell : cells(Origin, Front)) {
            if (World.hasNeighborSignal(Cell)) {
                return true;
            }
        }
        return false;
    }

    public void setOpen(Level World, BlockPos Origin, Direction Front, boolean Opening) {
        for (BlockPos Cell : cells(Origin, Front)) {
            BlockState CellState = World.getBlockState(Cell);
            if (CellState.is(this) && CellState.getValue(Facing) == Front && CellState.getValue(Open) != Opening) {
                World.setBlock(Cell, CellState.setValue(Open, Opening), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public BlockPos keyAnchor(BlockPos Pos, BlockState State) {
        return originOf(Pos, State);
    }

    @Override
    public boolean holdOpen(ServerLevel World, BlockPos Anchor, @Nullable Player User) {
        if (World.getBlockEntity(Anchor) instanceof PanelDoorBlockEntity Door) {
            Door.hold(true);
            return true;
        }
        return false;
    }

    @Override
    public void releaseHold(ServerLevel World, BlockPos Anchor) {
        if (World.getBlockEntity(Anchor) instanceof PanelDoorBlockEntity Door) {
            Door.hold(false);
        }
    }

    @Override
    public boolean isHeldOpen(BlockGetter World, BlockPos Anchor) {
        return World.getBlockEntity(Anchor) instanceof PanelDoorBlockEntity Door && Door.isHeld();
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        DoorKind Kind = getKind();
        Direction Front = Context.getHorizontalDirection().getOpposite();
        BlockPos Clicked = Context.getClickedPos();
        BlockPos Origin = Clicked.relative(DoorGeometry.rightOf(Front), -Kind.getClickedColumn());
        List<BlockPos> Cells = new ArrayList<>(Kind.getCellCount());
        Cells.add(Clicked);
        for (BlockPos Cell : cells(Origin, Front)) {
            if (!Cell.equals(Clicked)) {
                Cells.add(Cell);
            }
        }
        return Cells;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open, partProperty());
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
        DoorKind Kind = getKind();
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(partProperty(), Kind.partOf(Kind.getClickedColumn(), 0));
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        BlockPos Origin = originOf(Pos, State);
        DoorKind Kind = getKind();
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            BlockPos Cell = cellPos(Origin, Front, Kind.columnOf(Part), Kind.rowOf(Part));
            if (!Cell.equals(Pos)) {
                World.setBlock(Cell, State.setValue(partProperty(), Part), Block.UPDATE_ALL);
            }
        }
        if (!World.isClientSide && World.getBlockEntity(Origin) instanceof PanelDoorBlockEntity Door) {
            Door.refreshPower(World);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            Dismantling = true;
            try {
                Direction Front = State.getValue(Facing);
                BlockPos Origin = originOf(Pos, State);
                DoorKind Kind = getKind();
                for (int Part = 0; Part < Kind.getCellCount(); Part++) {
                    BlockPos Cell = cellPos(Origin, Front, Kind.columnOf(Part), Kind.rowOf(Part));
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(partProperty()) == Part && CellState.getValue(Facing) == Front) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                Dismantling = false;
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!getKind().isManual()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            PanelDoorBlockEntity Door = masterEntity(World, Pos, State);
            if (Door != null) {
                Door.toggle();
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (!World.isClientSide) {
            PanelDoorBlockEntity Door = masterEntity(World, Pos, State);
            if (Door != null) {
                Door.refreshPower(World);
            }
        }
    }

    private VoxelShape shapeOf(BlockState State, BlockGetter World, BlockPos Pos) {
        PanelDoorBlockEntity Door = masterEntity(World, Pos, State);
        float Eased = Door != null ? Door.getEased() : State.getValue(Open) ? 1.0f : 0.0f;
        return DoorGeometry.cellShape(getKind(), State.getValue(Facing), columnOf(State), rowOf(State), Eased);
    }

    private VoxelShape collisionOf(BlockState State, BlockGetter World, BlockPos Pos) {
        PanelDoorBlockEntity Door = masterEntity(World, Pos, State);
        float Eased = Door != null ? Door.getEased() : State.getValue(Open) ? 1.0f : 0.0f;
        return DoorGeometry.cellCollision(getKind(), State.getValue(Facing), columnOf(State), rowOf(State), Eased);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapeOf(State, World, Pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return collisionOf(State, World, Pos);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return Type != PathComputationType.WATER && State.getValue(Open);
    }

    @Nullable
    @Override
    public PathType getBlockPathType(BlockState State, BlockGetter World, BlockPos Pos, @Nullable Mob Walker) {
        return State.getValue(Open) ? PathType.DOOR_OPEN : PathType.BLOCKED;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        Direction Right = DoorGeometry.rightOf(State.getValue(Facing));
        return Flip.mirror(Right) != Right ? State.setValue(Facing, State.getValue(Facing).getOpposite()) : State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = "block.scp_unleashed." + getKind().getSerializedName();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".travel").withStyle(ChatFormatting.GOLD));
            if (getKind().isManual()) {
                Lines.add(Component.translatable("block.scp_unleashed.door.manual").withStyle(ChatFormatting.GOLD));
            }
            Lines.add(Component.translatable("block.scp_unleashed.door.safety").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(partProperty()) == MasterPart ? new PanelDoorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(partProperty()) != MasterPart) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, DoorRegistry.PanelDoor.get(), PanelDoorBlockEntity::clientTick)
                : createTickerHelper(Type, DoorRegistry.PanelDoor.get(), PanelDoorBlockEntity::serverTick);
    }
}
