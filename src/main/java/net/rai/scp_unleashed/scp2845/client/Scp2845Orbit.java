package net.rai.scp_unleashed.scp2845.client;

import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;

public final class Scp2845Orbit {
    public static final float[] ShardAngles = {0.0f, 22.5f, 67.5f, 90.0f, 112.5f, 157.5f, 180.0f, 202.5f, 247.5f, 270.0f, 292.5f, 337.5f};
    public static final float HaloRadius = 8.5f;
    public static final float OrbitSpeed = 9.0f;
    public static final float BobAmount = 0.35f;
    public static final float DriftAmount = 0.3f;
    public static final float BobSpeed = 0.45f;
    public static final float[] TumbleAmount = {10.0f, 14.0f, 8.0f};
    public static final float[] TumbleSpeed = {0.31f, 0.23f, 0.37f};
    public static final float Golden = 1.618f;

    private Scp2845Orbit() {
    }

    public static void apply(AnimationProcessor<?> Processor, float Time) {
        float Turn = OrbitSpeed * Time * Mth.DEG_TO_RAD;
        for (int Index = 0; Index < ShardAngles.length; Index++) {
            GeoBone Shard = Processor.getBone("shard" + Index);
            if (Shard == null) {
                continue;
            }
            float Angle = ShardAngles[Index] * Mth.DEG_TO_RAD;
            float Seed = Index * Golden;
            float Reach = HaloRadius + BobAmount * Mth.sin(Time * BobSpeed * Mth.TWO_PI + Seed);
            float Dx = Reach * Mth.cos(Angle + Turn) - HaloRadius * Mth.cos(Angle);
            float Dz = Reach * Mth.sin(Angle + Turn) - HaloRadius * Mth.sin(Angle);
            float Dy = DriftAmount * Mth.sin(Time * BobSpeed * 0.7f * Mth.TWO_PI + Seed * 1.3f);
            Shard.setPosX(Shard.getInitialSnapshot().getOffsetX() + Dx);
            Shard.setPosY(Shard.getInitialSnapshot().getOffsetY() + Dy);
            Shard.setPosZ(Shard.getInitialSnapshot().getOffsetZ() + Dz);
            Shard.setRotX(Shard.getInitialSnapshot().getRotX() - tumble(Time, Seed, 0) * Mth.DEG_TO_RAD);
            Shard.setRotY(Shard.getInitialSnapshot().getRotY() - tumble(Time, Seed, 1) * Mth.DEG_TO_RAD);
            Shard.setRotZ(Shard.getInitialSnapshot().getRotZ() + tumble(Time, Seed, 2) * Mth.DEG_TO_RAD);
        }
    }

    private static float tumble(float Time, float Seed, int Axis) {
        return TumbleAmount[Axis] * Mth.sin(Time * TumbleSpeed[Axis] * Mth.TWO_PI + Seed * (Axis + 1));
    }
}
