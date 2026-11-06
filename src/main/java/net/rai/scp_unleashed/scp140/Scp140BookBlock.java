package net.rai.scp_unleashed.scp140;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp140BookBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<Scp140BookBlock> BlockCodec = simpleCodec(Scp140BookBlock::new);
    private static final VoxelShape Lengthwise = box(3.5, 0.0, 2.5, 12.5, 4.0, 13.5);
    private static final VoxelShape Crosswise = box(2.5, 0.0, 3.5, 13.5, 4.0, 12.5);

    public Scp140BookBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(FACING).getAxis() == Direction.Axis.Z ? Lengthwise : Crosswise;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return !World.isEmptyBlock(Pos.below());
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
        if (User.isSecondaryUseActive() && User.getMainHandItem().isEmpty()) {
            if (!World.isClientSide) {
                World.removeBlock(Pos, false);
                User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp140Registry.BookItem.get()));
                if (World instanceof ServerLevel Server) {
                    Scp140Effects.play(Server, Pos.getCenter(), Scp140Registry.CloseLayers, 0.7f, 1.05f);
                }
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!World.isClientSide && User instanceof ServerPlayer Reader) {
            World.playSound(null, Pos, Scp140Registry.PageRustle.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            Scp140Reading.open(Reader);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Scp140BookItem.describe(Lines);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp140BookBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (Type != Scp140Registry.BookEntity.get() || !World.isClientSide) {
            return null;
        }
        return (Where, At, Current, Holder) -> Scp140BookBlockEntity.clientTick(Where, At, Current, (Scp140BookBlockEntity) Holder);
    }
}
