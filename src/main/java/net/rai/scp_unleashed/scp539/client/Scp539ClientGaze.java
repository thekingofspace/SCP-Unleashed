package net.rai.scp_unleashed.scp539.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp539.network.Scp539GazePayload;

public final class Scp539ClientGaze {
    public static final float EngageRate = 5.0f;
    public static final float ReleaseRate = 2.5f;
    public static final float PullStiffness = 7.0f;
    public static final float PitchShare = 0.85f;
    public static final float TurnScale = 0.15f;
    public static final float SluggishShare = 0.3f;
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;
    private static int TargetId = -1;
    private static Vec3 Spot = Vec3.ZERO;
    private static int Left;
    private static float Engage;

    private Scp539ClientGaze() {
    }

    public static void apply(Scp539GazePayload Payload) {
        if (Payload.Ticks() <= 0) {
            Left = 0;
            return;
        }
        TargetId = Payload.TargetId();
        Spot = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        Left = Payload.Ticks();
    }

    public static boolean active() {
        return Left > 0;
    }

    public static float engage() {
        return Engage;
    }

    public static void tick() {
        if (Left > 0) {
            Left--;
        }
    }

    public static void reset() {
        TargetId = -1;
        Spot = Vec3.ZERO;
        Left = 0;
        Engage = 0.0f;
    }

    private static Vec3 target(Minecraft Client, float Partial) {
        Entity Found = Client.level == null || TargetId < 0 ? null : Client.level.getEntity(TargetId);
        if (Found != null && !Found.isRemoved()) {
            Spot = Found.getPosition(Partial).add(0.0, Found.getBbHeight() * 0.5, 0.0);
        }
        return Spot;
    }

    public static void frame(DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.isPaused()) {
            return;
        }
        float Seconds = Math.min(0.1f, Delta.getRealtimeDeltaTicks() / 20.0f);
        Engage = Mth.clamp(Engage + (active() ? EngageRate : -ReleaseRate) * Seconds, 0.0f, 1.0f);
        if (Engage <= 0.0f || !User.isAlive() || User.isSpectator() || User.isSleeping()) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(true);
        Vec3 Toward = target(Client, Partial).subtract(User.getEyePosition(Partial));
        if (Toward.lengthSqr() < 0.04) {
            return;
        }
        double Across = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        float WantYaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        float WantPitch = (float) -(Mth.atan2(Toward.y, Across) * Mth.RAD_TO_DEG);
        float Factor = 1.0f - (float) Math.exp(-PullStiffness * Engage * Seconds);
        float TurnYaw = Mth.wrapDegrees(WantYaw - User.getYRot()) * Factor;
        float TurnPitch = (WantPitch - User.getXRot()) * Factor * PitchShare;
        User.turn(TurnYaw / TurnScale, TurnPitch / TurnScale);
    }

    public static double sensitivity(double Original) {
        if (Engage <= 0.0f) {
            return Original;
        }
        float Share = 1.0f - (1.0f - SluggishShare) * Engage;
        double Base = Original * SensitivityScale + SensitivityFloor;
        return (Base * Math.cbrt(Share) - SensitivityFloor) / SensitivityScale;
    }
}
