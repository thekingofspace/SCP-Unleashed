package net.rai.scp_unleashed.decal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;

public final class DecalEffects {
    private static final double Lift = 0.53;
    private static final double Spread = 0.22;
    private static final double Flat = 0.02;

    private DecalEffects() {
    }

    public static void burst(ServerLevel World, BlockPos Pos, Direction Face, ParticleOptions Options, int Count, double Speed) {
        double X = Pos.getX() + 0.5 + Face.getStepX() * Lift;
        double Y = Pos.getY() + 0.5 + Face.getStepY() * Lift;
        double Z = Pos.getZ() + 0.5 + Face.getStepZ() * Lift;
        double SpreadX = Face.getAxis() == Direction.Axis.X ? Flat : Spread;
        double SpreadY = Face.getAxis() == Direction.Axis.Y ? Flat : Spread;
        double SpreadZ = Face.getAxis() == Direction.Axis.Z ? Flat : Spread;
        World.sendParticles(Options, X, Y, Z, Count, SpreadX, SpreadY, SpreadZ, Speed);
    }
}
