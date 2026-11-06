package net.rai.scp_unleashed.scp3008.furniture.living;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class Scp3008FurnitureAStoveBlock extends Scp3008FurnitureABlock {
    public static final MapCodec<Scp3008FurnitureAStoveBlock> StoveCodec = simpleCodec(Properties -> new Scp3008FurnitureAStoveBlock(Properties, Scp3008FurnitureAPiece.Stove));
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int Glow = 7;

    public Scp3008FurnitureAStoveBlock(Properties Properties, Scp3008FurnitureAPiece Piece) {
        super(Properties, Piece);
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Lit) ? Glow : 0;
    }

    @Override
    protected BlockState startState(BlockState State) {
        return State.setValue(Lit, false);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return StoveCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Lit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean Lighting = !State.getValue(Lit);
        World.setBlock(Pos, State.setValue(Lit, Lighting), Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp3008FurnitureARegistry.HobClick.get(), SoundSource.BLOCKS, 1.0f, Lighting ? 1.0f : 0.8f);
        if (Lighting) {
            World.playSound(null, Pos, Scp3008FurnitureARegistry.HobIgnite.get(), SoundSource.BLOCKS, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Lit) || Random.nextInt(4) != 0) {
            return;
        }
        Direction Facing = State.getValue(FACING);
        double Side = 12.0 / 16.0 - 0.5;
        double Back = 12.0 / 16.0 - 0.5;
        double X = Pos.getX() + 0.5 - Facing.getStepX() * Back + Facing.getClockWise().getStepX() * Side;
        double Z = Pos.getZ() + 0.5 - Facing.getStepZ() * Back + Facing.getClockWise().getStepZ() * Side;
        World.addParticle(ParticleTypes.SMOKE, X + (Random.nextDouble() - 0.5) * 0.15, Pos.getY() + 1.25, Z + (Random.nextDouble() - 0.5) * 0.15, 0.0, 0.015, 0.0);
    }
}
