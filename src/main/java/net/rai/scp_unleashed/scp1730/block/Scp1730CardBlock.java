package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp1730.Scp1730Sparkle;

public class Scp1730CardBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp1730CardBlock> CardCodec = simpleCodec(Scp1730CardBlock::new);
    private static final VoxelShape Flat = Block.box(4, 0, 4, 12, 1, 12);

    public Scp1730CardBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CardCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    protected ItemStack card() {
        return new ItemStack(Scp1730Registry.AccessCard.get());
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        Scp1730Sparkle.twinkle(World, Pos, Random, 0.05);
    }

    public static class Clearance extends Scp1730CardBlock {
        public static final MapCodec<Clearance> ClearanceCodec = simpleCodec(Clearance::new);

        public Clearance(Properties Props) {
            super(Props);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return ClearanceCodec;
        }

        @Override
        protected ItemStack card() {
            return new ItemStack(Scp1730Registry.ClearanceCard.get());
        }
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return !World.getBlockState(Pos.below()).getCollisionShape(World, Pos.below()).isEmpty();
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Flat;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            ItemStack Card = card();
            if (!User.getInventory().add(Card)) {
                User.drop(Card, false);
            }
            World.removeBlock(Pos, false);
            World.playSound(null, Pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.4f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
