package net.rai.scp_unleashed.gas;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fission.ToxicGasCloud;

public final class GasReleases {
    public static final int SteamPerPuff = 25;
    public static final int MaxPuffs = 12;
    public static final int CloudChunk = (int) ToxicGasCloud.MaxGas;
    public static final int MaxClouds = 8;
    public static final double CloudScatter = 3.0;

    private GasReleases() {
    }

    public static void steam(ServerLevel World, Vec3 Point, int Amount) {
        int Count = Mth.clamp(Amount / SteamPerPuff + 1, 1, MaxPuffs);
        World.sendParticles(ParticleTypes.CLOUD, Point.x, Point.y, Point.z, Count, 0.15, 0.1, 0.15, 0.02);
        if (Amount >= SteamPerPuff * 4) {
            World.sendParticles(ParticleTypes.WHITE_SMOKE, Point.x, Point.y + 0.2, Point.z, Count / 2 + 1, 0.2, 0.15, 0.2, 0.01);
        }
    }

    public static void toxic(ServerLevel World, Vec3 Point, int Amount) {
        if (Amount <= CloudChunk) {
            ToxicGasCloud.feed(World, Point, Amount);
            return;
        }
        RandomSource Random = World.getRandom();
        int Left = Math.min(Amount, CloudChunk * MaxClouds);
        boolean First = true;
        while (Left > 0) {
            int Part = Math.min(CloudChunk, Left);
            Left -= Part;
            Vec3 Spot = First ? Point : Point.add((Random.nextDouble() - 0.5) * 2.0 * CloudScatter, Random.nextDouble() * 1.5, (Random.nextDouble() - 0.5) * 2.0 * CloudScatter);
            First = false;
            ToxicGasCloud.feed(World, Spot, Part);
        }
    }
}
