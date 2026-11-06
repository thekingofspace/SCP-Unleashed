package net.rai.scp_unleashed.scp3008.gear.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp3008ShelfBlock extends BaseEntityBlock {
    public static final MapCodec<Scp3008ShelfBlock> ShelfCodec = simpleCodec(Scp3008ShelfBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final double Depth = 8.0;
    public static final int Rows = 1;
    private static final Map<Direction, VoxelShape> Shapes = backHalves();

    public Scp3008ShelfBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    private static Map<Direction, VoxelShape> backHalves() {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        double Open = 16.0 - Depth;
        Made.put(Direction.NORTH, Block.box(0, 0, Open, 16, 16, 16));
        Made.put(Direction.SOUTH, Block.box(0, 0, 0, 16, 16, Depth));
        Made.put(Direction.WEST, Block.box(Open, 0, 0, 16, 16, 16));
        Made.put(Direction.EAST, Block.box(0, 0, 0, Depth, 16, 16));
        return Made;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return ShelfCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    public int rows() {
        return Rows;
    }

    public BlockPos storagePos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp3008ShelfBlockEntity(Pos, State);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(storagePos(State, Pos)) instanceof Scp3008ShelfBlockEntity Shelf) {
            User.openMenu(Shelf);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(storagePos(State, Pos)) instanceof Scp3008ShelfBlockEntity Shelf ? Shelf : null;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean Moved) {
        Containers.dropContentsOnDestroy(State, NewState, World, Pos);
        super.onRemove(State, World, Pos, NewState, Moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(World.getBlockEntity(storagePos(State, Pos)));
    }

    @Override
    public boolean isLadder(BlockState State, LevelReader World, BlockPos Pos, LivingEntity Climber) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }
}
