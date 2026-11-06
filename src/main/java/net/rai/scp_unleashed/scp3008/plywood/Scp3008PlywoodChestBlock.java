package net.rai.scp_unleashed.scp3008.plywood;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp3008PlywoodChestBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<Scp3008PlywoodChestBlock> Codec = simpleCodec(Scp3008PlywoodChestBlock::new);
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final VoxelShape AlongX = Block.box(0.5, 0.0, 1.5, 15.5, 13.0, 14.5);
    public static final VoxelShape AlongZ = Block.box(1.5, 0.0, 0.5, 14.5, 13.0, 15.5);

    public Scp3008PlywoodChestBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Open);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(FACING).getAxis() == Direction.Axis.Z ? AlongX : AlongZ;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp3008PlywoodChestEntity(Pos, State);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(Pos) instanceof Scp3008PlywoodChestEntity Storage) {
            User.openMenu(Storage);
            User.awardStat(Stats.OPEN_CHEST);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (World.getBlockEntity(Pos) instanceof Scp3008PlywoodChestEntity Storage) {
            Storage.recheckOpen();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        Containers.dropContentsOnDestroy(State, NewState, World, Pos);
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(World.getBlockEntity(Pos));
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(FACING, Turn.rotate(State.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }
}
