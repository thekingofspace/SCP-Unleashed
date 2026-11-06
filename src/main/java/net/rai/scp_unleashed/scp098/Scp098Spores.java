package net.rai.scp_unleashed.scp098;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.labware.LabExposure;

import java.util.function.Predicate;

public final class Scp098Spores {
    public static final int CloudTicks = 100;
    public static final int SwellTicks = 30;
    public static final double Radius = 3.5;
    public static final double StartRadius = 1.4;
    public static final double Drift = 1.4;
    public static final double Rise = 0.9;
    public static final double Lift = 1.1;
    public static final int ExposeInterval = 10;
    public static final float InfectChance = 0.08f;
    public static final int PuffsPerTick = 5;

    private Scp098Spores() {
    }

    public static Vec3 centre(BlockPos Pos, int Seed, int Elapsed) {
        double Angle = (Seed & 1023) / 1024.0 * Math.PI * 2.0;
        double Share = Mth.clamp(Elapsed / (double) CloudTicks, 0.0, 1.0);
        double Spread = Drift * Share;
        return new Vec3(Pos.getX() + 0.5 + Math.cos(Angle) * Spread, Pos.getY() + Lift + Rise * Share, Pos.getZ() + 0.5 + Math.sin(Angle) * Spread);
    }

    public static double radius(int Elapsed) {
        double Share = Mth.clamp(Elapsed / (double) SwellTicks, 0.0, 1.0);
        return StartRadius + (Radius - StartRadius) * Share;
    }

    public static void expose(ServerLevel World, BlockPos Pos, int Seed, int Elapsed) {
        Vec3 Centre = centre(Pos, Seed, Elapsed);
        double Reach = radius(Elapsed);
        Predicate<BlockPos> Ignored = Cell -> Cell.equals(Pos);
        RandomSource Random = World.getRandom();
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Centre, Ignored, Reach, Scp098Host::canHost)) {
            if (Random.nextFloat() < InfectChance) {
                Scp098Host.infect(Exposed, World, Pos);
            }
        }
        for (LivingEntity Exposed : ControlBodies.inSight(World, Centre, Ignored, Reach, Scp098Host::canHost)) {
            if (Random.nextFloat() < InfectChance) {
                Scp098Host.infect(Exposed, World, Pos);
            }
        }
    }

    public static void puff(Level World, BlockPos Pos, int Seed, int Elapsed, RandomSource Random) {
        Vec3 Centre = centre(Pos, Seed, Elapsed);
        double Reach = radius(Elapsed);
        double Fade = Elapsed > CloudTicks - 20 ? (CloudTicks - Elapsed) / 20.0 : 1.0;
        int Count = Math.max(1, (int) Math.round(PuffsPerTick * Fade));
        double Angle = (Seed & 1023) / 1024.0 * Math.PI * 2.0;
        double WindX = Math.cos(Angle) * Drift / CloudTicks;
        double WindZ = Math.sin(Angle) * Drift / CloudTicks;
        for (int Index = 0; Index < Count; Index++) {
            double Theta = Random.nextDouble() * Math.PI * 2.0;
            double Phi = Math.acos(2.0 * Random.nextDouble() - 1.0);
            double Distance = Reach * Math.cbrt(Random.nextDouble()) * 0.92;
            double X = Centre.x + Math.sin(Phi) * Math.cos(Theta) * Distance;
            double Y = Centre.y + Math.cos(Phi) * Distance * 0.65;
            double Z = Centre.z + Math.sin(Phi) * Math.sin(Theta) * Distance;
            World.addParticle(Scp098Registry.Spore.get(), X, Y, Z, WindX + (Random.nextDouble() - 0.5) * 0.01, 0.004 + Random.nextDouble() * 0.006,
                    WindZ + (Random.nextDouble() - 0.5) * 0.01);
        }
    }
}
