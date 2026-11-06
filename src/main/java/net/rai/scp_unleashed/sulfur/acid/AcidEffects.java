package net.rai.scp_unleashed.sulfur.acid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.rai.scp_unleashed.liquidsound.LiquidSounds;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;

public final class AcidEffects {
    public static final int FizzChance = 2;
    public static final int WispChance = 6;
    public static final int WispColour = 0xFFC4D03A;

    private AcidEffects() {
    }

    public static void ambience(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
        LiquidSounds.ambience(World, Pos, State, Random, SulfurRegistry.AcidSounds);
        BlockPos Above = Pos.above();
        if (!World.getFluidState(Above).isEmpty() || World.getBlockState(Above).isSolidRender(World, Above)) {
            return;
        }
        double Surface = Pos.getY() + State.getHeight(World, Pos);
        if (Random.nextInt(FizzChance) == 0) {
            World.addParticle(ParticleTypes.BUBBLE_POP, Pos.getX() + 0.1 + Random.nextDouble() * 0.8, Surface, Pos.getZ() + 0.1 + Random.nextDouble() * 0.8,
                    0.0, 0.02, 0.0);
        }
        if (Random.nextInt(WispChance) == 0) {
            World.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, WispColour), Pos.getX() + Random.nextDouble(), Surface + 0.05,
                    Pos.getZ() + Random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
