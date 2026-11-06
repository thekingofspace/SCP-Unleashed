package net.rai.scp_unleashed.scp327.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp327.network.Scp327LurePayload;

public final class Scp327ClientLure {
    public static final float EngageRate = 0.8f;
    public static final float ReleaseRate = 1.6f;
    public static final float Follow = 3.0f;
    public static final float BaseStiffness = 0.35f;
    public static final float PullStiffness = 1.9f;
    public static final float SensitivityCut = 0.7f;
    public static final float TurnScale = 0.15f;
    public static final double DriftBase = 0.006;
    public static final double DriftPull = 0.11;
    public static final double AirDrift = 0.3;
    public static final double WaterDrift = 0.35;
    public static final double DriftStop = 0.6;
    private static boolean Active;
    private static Vec3 Target = Vec3.ZERO;
    private static float TargetStrength;
    private static float Strength;
    private static float Engage;
    private static int Clock;

    private Scp327ClientLure() {
    }

    public static void apply(Scp327LurePayload Payload) {
        Active = Payload.Active();
        if (Active) {
            Target = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        }
        TargetStrength = Payload.Strength();
    }

    public static void reset() {
        Active = false;
        TargetStrength = 0.0f;
        Strength = 0.0f;
        Engage = 0.0f;
        Clock = 0;
    }

    public static float intensity() {
        return Strength * Engage;
    }

    public static int clock() {
        return Clock;
    }

    public static void tick() {
        Clock++;
    }

    public static void frame(DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.isPaused()) {
            return;
        }
        float Seconds = Math.min(0.1f, Delta.getRealtimeDeltaTicks() / 20.0f);
        Engage = Mth.clamp(Engage + (Active ? EngageRate : -ReleaseRate) * Seconds, 0.0f, 1.0f);
        Strength += (TargetStrength - Strength) * Math.min(1.0f, Seconds * Follow);
        if (Engage <= 0.0f || !User.isAlive() || User.isSleeping() || User.isSpectator()) {
            return;
        }
        Vec3 Eye = User.getEyePosition(Delta.getGameTimeDeltaPartialTick(true));
        Vec3 Toward = Target.subtract(Eye);
        if (Toward.lengthSqr() < 0.04) {
            return;
        }
        double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        float WantYaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        float WantPitch = (float) -(Mth.atan2(Toward.y, Flat) * Mth.RAD_TO_DEG);
        float Stiffness = (BaseStiffness + PullStiffness * Strength) * Engage * Strength;
        float Factor = 1.0f - (float) Math.exp(-Stiffness * Seconds);
        float TurnYaw = Mth.wrapDegrees(WantYaw - User.getYRot()) * Factor;
        float TurnPitch = (WantPitch - User.getXRot()) * Factor * 0.6f;
        User.turn(TurnYaw / TurnScale, TurnPitch / TurnScale);
    }

    public static double sensitivity(double Original) {
        if (Engage <= 0.0f) {
            return Original;
        }
        double Keep = 1.0 - SensitivityCut * Strength * Engage;
        double Base = Original * 0.6 + 0.2;
        double Scaled = Base * Math.cbrt(Keep);
        return Math.max(0.0, (Scaled - 0.2) / 0.6);
    }

    public static void drift(LocalPlayer User) {
        if (!Active || Engage <= 0.0f || User.isPassenger() || User.getAbilities().flying || User.isSpectator()) {
            return;
        }
        double Dx = Target.x - User.getX();
        double Dz = Target.z - User.getZ();
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance < DriftStop) {
            return;
        }
        double Accel = (DriftBase + DriftPull * Strength * Strength) * Engage * (User.isInWater() ? WaterDrift : User.onGround() ? 1.0 : AirDrift);
        double Lift = User.isInWater() ? Mth.clamp(Target.y - 0.6 - User.getY(), -1.0, 1.0) * Accel * 0.5 : 0.0;
        User.setDeltaMovement(User.getDeltaMovement().add(Dx / Distance * Accel, Lift, Dz / Distance * Accel));
    }
}
