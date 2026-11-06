package net.rai.scp_unleashed.scp076;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

public class Scp076DoorBlock extends Scp076SealedBlock implements EntityBlock {
    public static final MapCodec<Scp076DoorBlock> DoorCodec = simpleCodec(Scp076DoorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final int Columns = 3;
    public static final int Rows = 4;
    public static final IntegerProperty Column = IntegerProperty.create("column", 0, Columns - 1);
    public static final IntegerProperty Row = IntegerProperty.create("row", 0, Rows - 1);
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    private static boolean Clearing;

    public Scp076DoorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Column, 0).setValue(Row, 0).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends Scp076DoorBlock> codec() {
        return DoorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Column, Row, Open);
    }

    public static Direction across(Direction Front) {
        return Front.getCounterClockWise();
    }

    public static BlockPos partPos(BlockPos Origin, Direction Front, int ColumnIndex, int RowIndex) {
        return Origin.relative(across(Front), ColumnIndex).above(RowIndex);
    }

    public static BlockPos origin(BlockPos Pos, BlockState State) {
        return Pos.relative(across(State.getValue(Facing)), -State.getValue(Column)).below(State.getValue(Row));
    }

    public static boolean isOrigin(BlockState State) {
        return State.getValue(Column) == 0 && State.getValue(Row) == 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        Level World = Context.getLevel();
        BlockPos Origin = Context.getClickedPos().relative(across(Front), -1);
        for (int ColumnIndex = 0; ColumnIndex < Columns; ColumnIndex++) {
            for (int RowIndex = 0; RowIndex < Rows; RowIndex++) {
                BlockPos Part = partPos(Origin, Front, ColumnIndex, RowIndex);
                if (World.isOutsideBuildHeight(Part) || !World.getBlockState(Part).canBeReplaced(Context)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(Facing, Front).setValue(Column, 1);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (!World.isClientSide) {
            place(World, origin(Pos, State), State.getValue(Facing));
        }
    }

    public static void place(Level World, BlockPos Origin, Direction Front) {
        Block Door = Scp076Registry.VaultDoor.get();
        for (int RowIndex = 0; RowIndex < Rows; RowIndex++) {
            for (int ColumnIndex = 0; ColumnIndex < Columns; ColumnIndex++) {
                World.setBlock(partPos(Origin, Front, ColumnIndex, RowIndex), Door.defaultBlockState().setValue(Facing, Front).setValue(Column, ColumnIndex)
                        .setValue(Row, RowIndex), Block.UPDATE_ALL);
            }
        }
    }

    public static void setOpen(Level World, BlockPos Origin, Direction Front, boolean Value) {
        for (int RowIndex = 0; RowIndex < Rows; RowIndex++) {
            for (int ColumnIndex = 0; ColumnIndex < Columns; ColumnIndex++) {
                BlockPos Part = partPos(Origin, Front, ColumnIndex, RowIndex);
                BlockState State = World.getBlockState(Part);
                if (State.getBlock() instanceof Scp076DoorBlock && State.getValue(Open) != Value) {
                    World.setBlock(Part, State.setValue(Open, Value), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replacement, boolean Moved) {
        if (!State.is(Replacement.getBlock()) && !World.isClientSide && !Clearing) {
            Clearing = true;
            try {
                BlockPos Origin = origin(Pos, State);
                Direction Front = State.getValue(Facing);
                for (int RowIndex = 0; RowIndex < Rows; RowIndex++) {
                    for (int ColumnIndex = 0; ColumnIndex < Columns; ColumnIndex++) {
                        BlockPos Part = partPos(Origin, Front, ColumnIndex, RowIndex);
                        BlockState There = World.getBlockState(Part);
                        if (!Part.equals(Pos) && There.getBlock() instanceof Scp076DoorBlock && origin(Part, There).equals(Origin)) {
                            World.setBlock(Part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
                if (World instanceof ServerLevel Server) {
                    Scp076Vaults.get(Server.getServer()).forget(Server.dimension(), Origin);
                }
            } finally {
                Clearing = false;
            }
        }
        super.onRemove(State, World, Pos, Replacement, Moved);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Open) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Open) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Shapes.empty();
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.getValue(Open) ? 0 : 15;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.getValue(Open);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return State.getValue(Open) && Kind == PathComputationType.LAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (State.getValue(Open)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Visitor && !(User instanceof FakePlayer) && World.getBlockEntity(origin(Pos, State)) instanceof Scp076DoorBlockEntity Door) {
            Door.use(Visitor);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isOrigin(State) ? new Scp076DoorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || Type != Scp076Registry.DoorEntity.get()) {
            return null;
        }
        return (Level Where, BlockPos Pos, BlockState Current, T Ticking) -> {
            if (Ticking instanceof Scp076DoorBlockEntity Door) {
                Door.serverTick();
            }
        };
    }
}
