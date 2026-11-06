package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class Scp2317RitualGuide {
    public static final double TrailRadius = Scp2317Plan.WalkRadius + 1.5;
    public static final double TrailHeight = 1.25;
    public static final int CometPeriod = 28;
    public static final double CometLead = 0.2;
    public static final double CometReach = Math.PI * 0.55;
    public static final int TrailPoints = 6;
    public static final double TrailSpacing = 0.05;
    public static final int SparkEvery = 3;
    public static final int PillarEvery = 4;
    public static final int SmokeEvery = 4;
    public static final int BurstPoints = 36;
    public static final double BackwardLimit = 0.3;
    public static final Vector3f Clockwise = new Vector3f(0.86f, 0.1f, 0.07f);
    public static final Vector3f Counter = new Vector3f(0.94f, 0.9f, 0.8f);

    private Scp2317RitualGuide() {
    }

    public static int way(int Stage) {
        return Stage == 0 ? 1 : -1;
    }

    public static Vector3f colour(int Stage) {
        return Stage == 0 ? Clockwise : Counter;
    }

    public static ParticleOptions spark(int Stage) {
        return Stage == 0 ? ParticleTypes.FLAME : ParticleTypes.END_ROD;
    }

    public static double pillarAngle(int Index) {
        int[] Centre = Scp2317Plan.pillarCentre(Index, Scp2317Plan.RingRadius);
        return Math.atan2(Centre[1], Centre[0]);
    }

    public static double ahead(double From, double To, int Way) {
        double Arc = ((To - From) * Way) % Scp2317Ritual.FullTurn;
        return Arc < 0.0 ? Arc + Scp2317Ritual.FullTurn : Arc;
    }

    public static boolean lit(Scp2317Ritual.Attempt Current, int Index) {
        if (Double.isNaN(Current.StartAngle)) {
            return false;
        }
        double Progress = Current.Sweep * way(Current.Stage);
        if (Progress <= 0.0) {
            return false;
        }
        if (Progress >= Scp2317Ritual.FullTurn) {
            return true;
        }
        return ahead(Current.StartAngle, pillarAngle(Index), way(Current.Stage)) <= Progress;
    }

    public static int litCount(Scp2317Ritual.Attempt Current) {
        int Count = 0;
        for (int Index = 0; Index < Scp2317Plan.Pillars; Index++) {
            if (lit(Current, Index)) {
                Count++;
            }
        }
        return Count;
    }

    public static Vec3 pillarTop(int Index) {
        int[] Centre = Scp2317Plan.pillarCentre(Index, Scp2317Plan.RingRadius);
        return new Vec3(Scp2317World.baseX() + Centre[0] + 0.5, Scp2317Plan.Ground + Scp2317Plan.pillarHeight(Index) + 1.1, Scp2317World.baseZ() + Centre[1] + 0.5);
    }

    public static void show(ServerPlayer User, Scp2317Ritual.Attempt Current) {
        ServerLevel World = User.serverLevel();
        if (!Scp2317World.inside(World)) {
            return;
        }
        long Time = World.getGameTime();
        Vec3 Centre = Scp2317World.centre();
        double Here = Math.atan2(User.getZ() - Centre.z, User.getX() - Centre.x);
        int Way = way(Current.Stage);
        Vector3f Colour = colour(Current.Stage);
        double Y = Scp2317Plan.Ground + TrailHeight;
        double Share = (Time % CometPeriod) / (double) CometPeriod;
        double Head = Here + Way * (CometLead + Share * CometReach);
        for (int Point = 0; Point < TrailPoints; Point++) {
            double Angle = Head - Way * Point * TrailSpacing;
            send(User, new DustParticleOptions(Colour, 1.9f - Point * 0.25f), Centre.x + Math.cos(Angle) * TrailRadius, Y, Centre.z + Math.sin(Angle) * TrailRadius, 1, 0.0, 0.0);
        }
        if (Time % SparkEvery == 0) {
            send(User, spark(Current.Stage), Centre.x + Math.cos(Head) * TrailRadius, Y + 0.1, Centre.z + Math.sin(Head) * TrailRadius, 1, 0.04, 0.005);
        }
        if (Time % PillarEvery == 0) {
            for (int Index = 0; Index < Scp2317Plan.Pillars; Index++) {
                if (lit(Current, Index)) {
                    Vec3 Top = pillarTop(Index);
                    send(User, new DustParticleOptions(Colour, 1.5f), Top.x, Top.y, Top.z, 3, 0.35, 0.01);
                    send(User, spark(Current.Stage), Top.x, Top.y, Top.z, 1, 0.2, 0.005);
                }
            }
        }
        if (Current.Sweep * Way < -BackwardLimit && Time % SmokeEvery == 0) {
            send(User, ParticleTypes.LARGE_SMOKE, User.getX(), User.getY() + 0.1, User.getZ(), 3, 0.3, 0.01);
        }
    }

    public static void turned(ServerPlayer User) {
        Vector3f Colour = colour(1);
        for (int Index = 0; Index < Scp2317Plan.Pillars; Index++) {
            Vec3 Top = pillarTop(Index);
            send(User, new DustParticleOptions(Colour, 2.0f), Top.x, Top.y, Top.z, 20, 0.5, 0.02);
            send(User, ParticleTypes.SMOKE, Top.x, Top.y, Top.z, 8, 0.4, 0.02);
        }
        Vec3 Centre = Scp2317World.centre();
        double Y = Scp2317Plan.Ground + TrailHeight;
        for (int Point = 0; Point < BurstPoints; Point++) {
            double Angle = Point * Scp2317Ritual.FullTurn / BurstPoints;
            send(User, new DustParticleOptions(Colour, 1.4f), Centre.x + Math.cos(Angle) * TrailRadius, Y, Centre.z + Math.sin(Angle) * TrailRadius, 2, 0.1, 0.0);
        }
    }

    private static void send(ServerPlayer User, ParticleOptions Options, double X, double Y, double Z, int Count, double Spread, double Speed) {
        User.serverLevel().sendParticles(User, Options, true, X, Y, Z, Count, Spread, Spread * 0.6, Spread, Speed);
    }
}
