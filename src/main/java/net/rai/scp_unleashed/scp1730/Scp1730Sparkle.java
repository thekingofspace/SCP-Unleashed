package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class Scp1730Sparkle {
    public static final double Spread = 0.6;
    public static final double Rise = 0.35;
    public static final double LiftMin = 1.5;
    public static final double LiftExtra = 1.5;

    private Scp1730Sparkle() {
    }

    public static void twinkle(Level World, BlockPos Pos, RandomSource Random, double Height) {
        int Count = 2 + Random.nextInt(2);
        for (int Index = 0; Index < Count; Index++) {
            double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * Spread;
            double Y = Pos.getY() + Height + Random.nextDouble() * Rise;
            double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * Spread;
            World.addParticle(ParticleTypes.WAX_OFF, X, Y, Z, 0.0, LiftMin + Random.nextDouble() * LiftExtra, 0.0);
        }
    }
}
