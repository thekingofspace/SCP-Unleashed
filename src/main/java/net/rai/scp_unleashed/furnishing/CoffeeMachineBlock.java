package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CoffeeMachineBlock extends SwitchBlock {
    public static final MapCodec<CoffeeMachineBlock> CoffeeCodec = simpleCodec(CoffeeMachineBlock::new);

    public CoffeeMachineBlock(Properties Properties) {
        super(FurnishingKind.CoffeeMachine, Properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CoffeeCodec;
    }

    @Override
    protected void playSwitch(Level World, BlockPos Pos, boolean On) {
        super.playSwitch(World, Pos, On);
        if (On) {
            World.playSound(null, Pos, FurnishingSounds.CoffeeBrew.get(), SoundSource.BLOCKS, 0.8f, 0.95f + World.random.nextFloat() * 0.1f);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Lit) || Random.nextInt(3) != 0) {
            return;
        }
        Direction Facing = State.getValue(FACING);
        double Forward = 2.0 / 16.0;
        double X = Pos.getX() + 0.5 + Facing.getStepX() * Forward + (Random.nextDouble() - 0.5) * 0.12;
        double Z = Pos.getZ() + 0.5 + Facing.getStepZ() * Forward + (Random.nextDouble() - 0.5) * 0.12;
        World.addParticle(ParticleTypes.WHITE_SMOKE, X, Pos.getY() + 0.85, Z, 0.0, 0.02 + Random.nextDouble() * 0.02, 0.0);
    }
}
