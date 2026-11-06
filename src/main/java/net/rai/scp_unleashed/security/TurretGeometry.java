package net.rai.scp_unleashed.security;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class TurretGeometry {
    public static final double FloorPivotY = 16.0 / 16.0;
    public static final double CeilingPivotY = 0.0;
    public static final double MuzzleReach = 14.0 / 16.0;
    public static final double BarrelSpacing = 1.5 / 16.0;
    public static final double BarrelDrop = 0.5 / 16.0;
    public static final float FloorMinPitch = -70.0f;
    public static final float FloorMaxPitch = 30.0f;
    public static final float CeilingMinPitch = -30.0f;
    public static final float CeilingMaxPitch = 75.0f;
    public static final float FloorRestPitch = 0.0f;
    public static final float CeilingRestPitch = 18.0f;
    public static final float FloorDroop = 26.0f;
    public static final float CeilingDroop = 62.0f;
    public static final float SweepAmplitude = 55.0f;
    public static final float SweepRate = 0.035f;

    private TurretGeometry() {
    }

    public static Vec3 pivot(BlockPos Pos, boolean Hanging) {
        return new Vec3(Pos.getX() + 0.5, Pos.getY() + (Hanging ? CeilingPivotY : FloorPivotY), Pos.getZ() + 0.5);
    }

    public static Vec3 forward(float Yaw, float Pitch) {
        float YawRadians = Yaw * Mth.DEG_TO_RAD;
        float PitchRadians = Pitch * Mth.DEG_TO_RAD;
        float Flat = Mth.cos(PitchRadians);
        return new Vec3(-Mth.sin(YawRadians) * Flat, -Mth.sin(PitchRadians), Mth.cos(YawRadians) * Flat);
    }

    public static float yawToward(Vec3 From, Vec3 To) {
        double DeltaX = To.x - From.x;
        double DeltaZ = To.z - From.z;
        return (float) (Mth.atan2(DeltaZ, DeltaX) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static float pitchToward(Vec3 From, Vec3 To) {
        double DeltaX = To.x - From.x;
        double DeltaY = To.y - From.y;
        double DeltaZ = To.z - From.z;
        return (float) -(Mth.atan2(DeltaY, Math.sqrt(DeltaX * DeltaX + DeltaZ * DeltaZ)) * Mth.RAD_TO_DEG);
    }

    public static float restYaw(Direction Front) {
        return Front.toYRot();
    }

    public static float minPitch(boolean Hanging) {
        return Hanging ? CeilingMinPitch : FloorMinPitch;
    }

    public static float maxPitch(boolean Hanging) {
        return Hanging ? CeilingMaxPitch : FloorMaxPitch;
    }

    public static float restPitch(boolean Hanging) {
        return Hanging ? CeilingRestPitch : FloorRestPitch;
    }

    public static float droop(boolean Hanging) {
        return Hanging ? CeilingDroop : FloorDroop;
    }

    public static float sweepYaw(Direction Front, long Time, long Seed) {
        return restYaw(Front) + SweepAmplitude * Mth.sin((Time + Seed) * SweepRate);
    }

    public static boolean sweepTurning(long Time, long Seed) {
        float Before = Mth.cos((Time - 1 + Seed) * SweepRate);
        float Now = Mth.cos((Time + Seed) * SweepRate);
        return Before > 0.0f != Now > 0.0f;
    }

    public static Vec3 muzzle(Vec3 Pivot, float Yaw, float Pitch, boolean Hanging, int Barrel) {
        Vec3 Forward = forward(Yaw, Pitch);
        Vec3 Right = forward(Yaw + 90.0f, 0.0f);
        Vec3 Up = Right.cross(Forward).normalize();
        double Side = Barrel == 0 ? -BarrelSpacing : BarrelSpacing;
        double Lift = Hanging ? BarrelDrop : -BarrelDrop;
        return Pivot.add(Forward.scale(MuzzleReach)).add(Right.scale(Side)).add(Up.scale(Lift));
    }
}
