package net.rai.scp_unleashed.scp055;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp055CaseBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp055CaseBlock> BlockCodec = simpleCodec(Scp055CaseBlock::new);
    private static final VoxelShape AlongX = Shapes.or(Block.box(1.5, 1.0, 5.5, 14.5, 11.0, 10.5), Block.box(2.5, 0.0, 7.0, 13.5, 1.0, 9.0),
            Block.box(4.5, 11.0, 7.5, 11.5, 13.0, 8.5));
    private static final VoxelShape AlongZ = Shapes.or(Block.box(5.5, 1.0, 1.5, 10.5, 11.0, 14.5), Block.box(7.0, 0.0, 2.5, 9.0, 1.0, 13.5),
            Block.box(7.5, 11.0, 4.5, 8.5, 13.0, 11.5));

    public Scp055CaseBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = defaultBlockState().setValue(FACING, Context.getHorizontalDirection());
        return State.canSurvive(Context.getLevel(), Context.getClickedPos()) ? State : null;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(FACING).getAxis() == Direction.Axis.Z ? AlongX : AlongZ;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.DOWN && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User.isSecondaryUseActive()) {
            if (!World.isClientSide) {
                pickUp(World, Pos, State, User);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!Scp055Episodes.canBegin(User)) {
            return InteractionResult.CONSUME;
        }
        if (User instanceof ServerPlayer Viewer) {
            Scp055Episodes.begin(Viewer);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    private static void pickUp(Level World, BlockPos Pos, BlockState State, Player User) {
        World.removeBlock(Pos, false);
        World.gameEvent(User, GameEvent.BLOCK_DESTROY, Pos);
        World.playSound(null, Pos, Scp055Registry.CaseBreak.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
        ItemStack Stack = new ItemStack(Scp055Registry.CaseItem.get());
        if (User.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        } else if (!User.getInventory().add(Stack)) {
            User.drop(Stack, false);
        }
    }
}
