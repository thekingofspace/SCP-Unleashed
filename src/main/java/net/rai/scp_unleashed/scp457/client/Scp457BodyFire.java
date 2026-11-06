package net.rai.scp_unleashed.scp457.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Effects;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp457BodyFire {
    public static final double NearRange = 20.0;
    public static final double ThinRange = 32.0;
    public static final double FarRange = 48.0;
    public static final double FarShare = 0.25;
    public static final double DecreasedShare = 0.5;
    public static final double MinimalShare = 0.15;
    public static final double RisingBoost = 1.6;
    public static final double Inherit = 0.85;
    public static final double MaxMotion = 0.6;
    public static final double FlamesPerTick = 4.5;
    public static final double LicksPerTick = 10.5;
    public static final double EmbersPerTick = 1.2;
    public static final double SmokePerTick = 0.5;
    public static final double HeadShare = 0.7;
    public static final float PushLow = 0.02f;
    public static final float PushHigh = 0.06f;
    public static final int SteamPerTick = 2;
    public static final int Attempts = 4;
    public static final int Crown = 0;
    public static final int Head = 1;

    private record Patch(int Bone, Vector3f Corner, Vector3f EdgeU, Vector3f EdgeV, Vector3f Normal) {
    }

    private static BakedGeoModel Learned;
    private static List<Patch> Patches = List.of();
    private static double[] Running = new double[0];
    private static Vector3f[] Pivots = new Vector3f[0];

    private Scp457BodyFire() {
    }

    public static void learn(BakedGeoModel Model) {
        if (Model == Learned) {
            return;
        }
        List<Patch> Found = new ArrayList<>();
        List<Double> Weights = new ArrayList<>();
        Vector3f[] Points = new Vector3f[Scp457Effects.TrackedBones.length];
        for (int Index = 0; Index < Points.length; Index++) {
            Optional<GeoBone> Bone = Model.getBone(Scp457Effects.TrackedBones[Index]);
            if (Bone.isEmpty()) {
                return;
            }
            Vector3f Pivot = new Vector3f(Bone.get().getPivotX(), Bone.get().getPivotY(), Bone.get().getPivotZ()).div(16.0f);
            Points[Index] = Pivot;
            for (GeoCube Cube : Bone.get().getCubes()) {
                for (GeoQuad Quad : Cube.quads()) {
                    if (Quad == null || Quad.normal().y() < -0.5f) {
                        continue;
                    }
                    Vector3f Start = Quad.vertices()[0].position();
                    Vector3f EdgeU = new Vector3f(Quad.vertices()[1].position()).sub(Start);
                    Vector3f EdgeV = new Vector3f(Quad.vertices()[3].position()).sub(Start);
                    float Area = new Vector3f(EdgeU).cross(EdgeV).length();
                    if (Area <= 0.0f) {
                        continue;
                    }
                    Found.add(new Patch(Index, new Vector3f(Start).sub(Pivot), EdgeU, EdgeV, new Vector3f(Quad.normal())));
                    Weights.add(Area * (Index == Head ? HeadShare : 1.0));
                }
            }
        }
        double[] Sums = new double[Weights.size()];
        double Total = 0.0;
        for (int Index = 0; Index < Sums.length; Index++) {
            Total += Weights.get(Index);
            Sums[Index] = Total;
        }
        Patches = Found;
        Running = Sums;
        Pivots = Points;
        Learned = Model;
    }

    public static double statusShare(ParticleStatus Status) {
        return switch (Status) {
            case ALL -> 1.0;
            case DECREASED -> DecreasedShare;
            case MINIMAL -> MinimalShare;
        };
    }

    public static double distanceShare(double Distance) {
        if (Distance <= NearRange) {
            return 1.0;
        }
        if (Distance <= ThinRange) {
            return Mth.lerp((Distance - NearRange) / (ThinRange - NearRange), 1.0, FarShare);
        }
        return Distance <= FarRange ? FarShare : 0.0;
    }

    public static int roll(double Rate, RandomSource Random) {
        int Count = (int) Rate;
        return Random.nextDouble() < Rate - Count ? Count + 1 : Count;
    }

    private static Matrix4f[] poses(Scp457 Burning) {
        Matrix4f[] Tracked = Burning.bonePoses();
        if (Tracked != null && Tracked.length == Pivots.length) {
            return Tracked;
        }
        Matrix4f[] Rest = new Matrix4f[Pivots.length];
        float Turn = (180.0f - Burning.yBodyRot) * Mth.DEG_TO_RAD;
        for (int Index = 0; Index < Rest.length; Index++) {
            Rest[Index] = new Matrix4f().rotateY(Turn).translate(Pivots[Index]);
        }
        return Rest;
    }

    private static Vec3 surface(Scp457 Burning, Matrix4f[] Poses, RandomSource Random) {
        Vector3f Placed = new Vector3f();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            Placed = pick(Poses, Random);
            if (Placed.y() >= Scp457Effects.FlameFloor) {
                break;
            }
        }
        return new Vec3(Burning.getX() + Placed.x(), Burning.getY() + Math.max(Placed.y(), Scp457Effects.FlameFloor), Burning.getZ() + Placed.z());
    }

    private static Vector3f pick(Matrix4f[] Poses, RandomSource Random) {
        double Choice = Random.nextDouble() * Running[Running.length - 1];
        int Low = 0;
        int High = Running.length - 1;
        while (Low < High) {
            int Middle = (Low + High) >>> 1;
            if (Running[Middle] < Choice) {
                Low = Middle + 1;
            } else {
                High = Middle;
            }
        }
        Patch Found = Patches.get(Low);
        float Push = PushLow + Random.nextFloat() * (PushHigh - PushLow);
        Vector3f Local = new Vector3f(Found.Corner()).add(new Vector3f(Found.EdgeU()).mul(Random.nextFloat())).add(new Vector3f(Found.EdgeV()).mul(Random.nextFloat()))
                .add(new Vector3f(Found.Normal()).mul(Push));
        return Poses[Found.Bone()].transformPosition(Local);
    }

    private static Vec3 crown(Scp457 Burning, Matrix4f[] Poses) {
        Vector3f Placed = Poses[Crown].transformPosition(new Vector3f());
        return Burning.position().add(Placed.x(), Placed.y(), Placed.z());
    }

    private static void emit(ParticleEngine Engine, ParticleOptions Type, Vec3 At, Vec3 Motion, double Rise, double Wobble, RandomSource Random) {
        Engine.createParticle(Type, At.x, At.y, At.z, Motion.x * Inherit + (Random.nextDouble() - 0.5) * Wobble, Rise,
                Motion.z * Inherit + (Random.nextDouble() - 0.5) * Wobble);
    }

    public static void tick(Scp457 Burning) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || Burning.level() != Game.level) {
            return;
        }
        RandomSource Random = Burning.getRandom();
        ParticleEngine Engine = Game.particleEngine;
        double Distance = Game.gameRenderer.getMainCamera().getPosition().distanceTo(Burning.position().add(0.0, Burning.getBbHeight() * 0.5, 0.0));
        double Settings = statusShare(Game.options.particles().get());
        double Reach = distanceShare(Distance);
        if (Reach <= 0.0) {
            return;
        }
        if (Burning.isDeadOrDying()) {
            if (Burning.deathKind() == Scp457.DeathWater && Burning.tickCount % 2 == 0 && Random.nextDouble() < Settings) {
                Engine.createParticle(Scp457Registry.Steam.get(), Burning.getRandomX(0.6), Burning.getY() + Random.nextDouble() * 1.4, Burning.getRandomZ(0.6), 0.0, 0.04, 0.0);
            }
            return;
        }
        if (Burning.isSteaming()) {
            int Steam = Math.max(1, (int) Math.round(SteamPerTick * Settings));
            for (int Count = 0; Count < Steam; Count++) {
                Engine.createParticle(Scp457Registry.Steam.get(), Burning.getRandomX(0.7), Burning.getY() + 0.4 + Random.nextDouble() * Burning.getBbHeight(),
                        Burning.getRandomZ(0.7), 0.0, 0.05, 0.0);
            }
        }
        if (Running.length == 0) {
            return;
        }
        boolean Far = Distance > ThinRange;
        double Share = Settings * Reach * Burning.flameStrength(0.0f) * (Burning.isRising() ? RisingBoost : 1.0);
        Matrix4f[] Poses = poses(Burning);
        Vec3 Motion = new Vec3(Burning.getX() - Burning.xo, 0.0, Burning.getZ() - Burning.zo);
        if (Motion.length() > MaxMotion) {
            Motion = Vec3.ZERO;
        }
        int Flames = roll(FlamesPerTick * Share, Random);
        for (int Count = 0; Count < Flames; Count++) {
            emit(Engine, Scp457Registry.Flame.get(), surface(Burning, Poses, Random), Motion, 0.035 + Random.nextDouble() * 0.035, 0.012, Random);
        }
        if (Far) {
            return;
        }
        int Licks = roll(LicksPerTick * Share, Random);
        for (int Count = 0; Count < Licks; Count++) {
            emit(Engine, Scp457Registry.Lick.get(), surface(Burning, Poses, Random), Motion, 0.025 + Random.nextDouble() * 0.03, 0.01, Random);
        }
        int Embers = roll(EmbersPerTick * Share, Random);
        for (int Count = 0; Count < Embers; Count++) {
            emit(Engine, Scp457Registry.Ember.get(), surface(Burning, Poses, Random), Motion, 0.05 + Random.nextDouble() * 0.06, 0.04, Random);
        }
        int Puffs = roll(SmokePerTick * Share, Random);
        for (int Count = 0; Count < Puffs; Count++) {
            Vec3 Above = crown(Burning, Poses).add((Random.nextDouble() - 0.5) * 0.25, 0.2 + Random.nextDouble() * 0.35, (Random.nextDouble() - 0.5) * 0.25);
            emit(Engine, ParticleTypes.SMOKE, Above, Motion.scale(0.6), 0.025 + Random.nextDouble() * 0.015, 0.01, Random);
        }
    }
}
