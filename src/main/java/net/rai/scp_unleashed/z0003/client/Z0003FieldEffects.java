package net.rai.scp_unleashed.z0003.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.ParticleStatus;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.z0003.Z0003Device;
import org.joml.Vector3f;

public final class Z0003FieldEffects {
    public static final Vector3f Green = new Vector3f(0.36f, 1.0f, 0.48f);
    public static final Vector3f PaleGreen = new Vector3f(0.7f, 1.0f, 0.74f);
    public static final int MoteBudget = 6;
    public static final int FrontBudget = 8;
    public static final int BurstCount = 24;
    public static final double NearDistance = 24.0;
    public static final double FarDistance = 96.0;
    public static final float FarShare = 0.25f;
    public static final float SparkleChance = 0.18f;

    private Z0003FieldEffects() {
    }

    public static void tick(Z0003Device Device, byte Current) {
        Z0003FieldTint.track(Device);
        Level World = Device.level();
        RandomSource Random = Device.getRandom();
        Vec3 Centre = Device.fieldCentre();
        if (Current == Z0003Device.Armed) {
            if (Device.tickCount % 10 == 0) {
                World.addParticle(new DustParticleOptions(Green, 0.5f), Centre.x, Centre.y + 0.15, Centre.z, 0.0, 0.01, 0.0);
            }
            return;
        }
        if (Current != Z0003Device.Active) {
            return;
        }
        float Budget = budget(Centre, Device.fieldReach(1.0f));
        if (Budget <= 0.0f) {
            return;
        }
        if (Random.nextInt(2) == 0) {
            World.addParticle(new DustParticleOptions(Green, 0.7f), Centre.x + (Random.nextDouble() - 0.5) * 0.6, Centre.y + (Random.nextDouble() - 0.5) * 0.6,
                    Centre.z + (Random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
        float Reach = Device.fieldReach(1.0f);
        float Fade = Device.fieldFade(1.0f);
        if (Reach < 0.5f || Fade < 0.3f) {
            return;
        }
        long Elapsed = World.getGameTime() - Device.fieldStart();
        if (Elapsed == Z0003Device.growTicks(Device.radius())) {
            burst(World, Random, Centre, Reach, Budget);
        }
        int Front = Math.round(Math.min(FrontBudget, 1 + Reach * 0.4f) * Budget);
        for (int Index = 0; Index < Front; Index++) {
            Vec3 Direction = direction(Random);
            World.addParticle(new DustParticleOptions(Green, 1.0f), Centre.x + Direction.x * Reach, Centre.y + Direction.y * Reach, Centre.z + Direction.z * Reach,
                    0.0, 0.0, 0.0);
        }
        int Motes = Math.round(Math.min(MoteBudget, 1 + Reach * 0.3f) * Budget * Fade);
        for (int Index = 0; Index < Motes; Index++) {
            Vec3 Direction = direction(Random);
            double Depth = Reach * Math.cbrt(Random.nextDouble()) * 0.95;
            double X = Centre.x + Direction.x * Depth;
            double Y = Centre.y + Direction.y * Depth;
            double Z = Centre.z + Direction.z * Depth;
            if (Random.nextFloat() < SparkleChance) {
                World.addParticle(ParticleTypes.HAPPY_VILLAGER, X, Y, Z, 0.0, 0.0, 0.0);
            } else {
                World.addParticle(new DustParticleOptions(PaleGreen, 0.45f + Random.nextFloat() * 0.35f), X, Y, Z, 0.0, 0.015, 0.0);
            }
        }
    }

    private static void burst(Level World, RandomSource Random, Vec3 Centre, float Reach, float Budget) {
        int Count = Math.round(BurstCount * Budget);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Direction = direction(Random);
            double Depth = Reach * (0.85 + Random.nextDouble() * 0.15);
            World.addParticle(new DustParticleOptions(Green, 1.4f), Centre.x + Direction.x * Depth, Centre.y + Direction.y * Depth, Centre.z + Direction.z * Depth,
                    0.0, 0.0, 0.0);
        }
        World.addParticle(ParticleTypes.FLASH, Centre.x, Centre.y, Centre.z, 0.0, 0.0, 0.0);
    }

    private static Vec3 direction(RandomSource Random) {
        double Theta = Random.nextDouble() * Math.PI * 2.0;
        double Cosine = Random.nextDouble() * 2.0 - 1.0;
        double Sine = Math.sqrt(1.0 - Cosine * Cosine);
        return new Vec3(Math.cos(Theta) * Sine, Cosine, Math.sin(Theta) * Sine);
    }

    private static float budget(Vec3 Centre, float Reach) {
        Minecraft Game = Minecraft.getInstance();
        ParticleStatus Setting = Game.options.particles().get();
        float Share = switch (Setting) {
            case ALL -> 1.0f;
            case DECREASED -> 0.5f;
            case MINIMAL -> 0.15f;
        };
        Camera View = Game.gameRenderer.getMainCamera();
        double Distance = Math.max(0.0, View.getPosition().distanceTo(Centre) - Reach);
        float Near = (float) Mth.clamp(1.0 - (Distance - NearDistance) / (FarDistance - NearDistance) * (1.0 - FarShare), FarShare, 1.0);
        if (Distance > FarDistance * 2.0) {
            return 0.0f;
        }
        return Share * Near;
    }
}
