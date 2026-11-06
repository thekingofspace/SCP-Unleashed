package net.rai.scp_unleashed.scp428;

import net.minecraft.util.Mth;

public final class Scp428Size {
    public static final int Capacity = 23;
    public static final float EmptyDiameter = 0.32f;
    public static final float SingleDiameter = 0.42f;
    public static final float FullDiameter = 2.8f;
    public static final float Curve = 0.8f;
    public static final float HeightShare = 0.81f;
    public static final float EyeShare = 0.62f;
    public static final float NarrowWidth = 0.7f;
    public static final float NarrowHeight = 1.7f;
    public static final double EmptyHealth = 8.0;
    public static final double HealthPerInstance = 6.0;
    public static final double BaseArmour = 1.0;
    public static final double ArmourPerInstance = 0.35;
    public static final double EmptySpeed = 0.13;
    public static final double SingleSpeed = 0.14;
    public static final double FullSpeed = 0.3;
    public static final double BaseKnockback = 0.2;
    public static final double KnockbackPerInstance = 0.035;
    public static final double BaseSense = 14.0;
    public static final double SensePerInstance = 0.5;
    public static final double LowClearance = 0.3;
    public static final double HighClearance = 0.9;
    public static final double SqueezeClearance = 0.08;

    private Scp428Size() {
    }

    public static float diameter(float Girth) {
        float Clamped = Mth.clamp(Girth, 0.0f, Capacity);
        if (Clamped <= 1.0f) {
            return Mth.lerp(Clamped, EmptyDiameter, SingleDiameter);
        }
        float Share = (Clamped - 1.0f) / (Capacity - 1.0f);
        return SingleDiameter + (FullDiameter - SingleDiameter) * (float) Math.pow(Share, Curve);
    }

    public static float height(float Girth) {
        return diameter(Girth) * HeightShare;
    }

    public static float width(float Girth, float Squeeze) {
        float Full = diameter(Girth);
        return Mth.lerp(Mth.clamp(Squeeze, 0.0f, 1.0f), Full, Math.min(Full, NarrowWidth));
    }

    public static float tall(float Girth, float Squeeze) {
        float Full = height(Girth);
        return Mth.lerp(Mth.clamp(Squeeze, 0.0f, 1.0f), Full, Math.min(Full, NarrowHeight));
    }

    public static float share(float Girth) {
        return Mth.clamp((diameter(Girth) - EmptyDiameter) / (FullDiameter - EmptyDiameter), 0.0f, 1.0f);
    }

    public static double clearance(float Girth, float Squeeze) {
        double Open = LowClearance + (HighClearance - LowClearance) * share(Girth);
        return Mth.lerp(Mth.clamp(Squeeze, 0.0f, 1.0f), Open, SqueezeClearance);
    }

    public static double maxHealth(int Count) {
        return EmptyHealth + HealthPerInstance * clamp(Count);
    }

    public static double speed(int Count) {
        int Held = clamp(Count);
        if (Held <= 0) {
            return EmptySpeed;
        }
        return SingleSpeed + (FullSpeed - SingleSpeed) * (Held - 1) / (Capacity - 1.0);
    }

    public static double armour(int Count) {
        return BaseArmour + ArmourPerInstance * clamp(Count);
    }

    public static double knockback(int Count) {
        return Math.min(1.0, BaseKnockback + KnockbackPerInstance * clamp(Count));
    }

    public static double sense(int Count) {
        return BaseSense + SensePerInstance * clamp(Count);
    }

    public static int clamp(int Count) {
        return Mth.clamp(Count, 0, Capacity);
    }
}
