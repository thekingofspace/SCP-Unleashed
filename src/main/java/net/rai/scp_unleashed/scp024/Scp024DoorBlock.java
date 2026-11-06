package net.rai.scp_unleashed.scp024;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

public class Scp024DoorBlock extends Scp024SolidBlock {
    public static final MapCodec<Scp024DoorBlock> DoorCodec = simpleCodec(Scp024DoorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final int Columns = 2;
    public static final int Rows = 4;
    public static final IntegerProperty Column = IntegerProperty.create("column", 0, Columns - 1);
    public static final IntegerProperty Row = IntegerProperty.create("row", 0, Rows - 1);

    public Scp024DoorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Column, 0).setValue(Row, 0));
    }

    @Override
    protected MapCodec<? extends Scp024DoorBlock> codec() {
        return DoorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Column, Row);
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        Level World = Context.getLevel();
        BlockPos Origin = Context.getClickedPos();
        for (int ColumnIndex = 0; ColumnIndex < Columns; ColumnIndex++) {
            for (int RowIndex = 0; RowIndex < Rows; RowIndex++) {
                BlockPos Part = partPos(Origin, Front, ColumnIndex, RowIndex);
                if (World.isOutsideBuildHeight(Part) || !World.getBlockState(Part).canBeReplaced(Context)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(Facing, Front);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        place(World, Pos, State.getValue(Facing), this);
    }

    public static void place(Level World, BlockPos Origin, Direction Front, Block Door) {
        for (int ColumnIndex = 0; ColumnIndex < Columns; ColumnIndex++) {
            for (int RowIndex = 0; RowIndex < Rows; RowIndex++) {
                World.setBlock(partPos(Origin, Front, ColumnIndex, RowIndex), Door.defaultBlockState().setValue(Facing, Front).setValue(Column, ColumnIndex)
                        .setValue(Row, RowIndex), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.block();
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Contestant && !(User instanceof FakePlayer)) {
            used(Contestant, origin(Pos, State), State.getValue(Facing));
        }
        return InteractionResult.CONSUME;
    }

    protected void used(ServerPlayer Contestant, BlockPos Origin, Direction Front) {
        Scp024Runs.enterByDoor(Contestant, Origin, Front);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return false;
    }
}
