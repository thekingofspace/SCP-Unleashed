package net.rai.scp_unleashed.scp323.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp323.Scp323Registry;
import net.rai.scp_unleashed.scp323.network.Scp323LurePayload;

public final class Scp323ClientLure {
    public static final float EngageRate = 1.6f;
    public static final float ReleaseRate = 2.4f;
    public static final float Follow = 4.0f;
    public static final float BaseStiffness = 5.0f;
    public static final float CloseStiffness = 13.0f;
    public static final float SensitivityCut = 0.65f;
    public static final float TurnScale = 0.15f;
    public static final double DriftBase = 0.010;
    public static final double DriftClose = 0.024;
    public static final double AirDrift = 0.35;
    public static final double WaterDrift = 0.6;
    public static final double DriftStop = 0.3;
    public static final double WhisperReach = 3.0;
    public static final int SlowBeat = 24;
    public static final int FastBeat = 11;
    public static final float PulseDecay = 0.86f;
    private static boolean Active;
    private static Vec3 Target = Vec3.ZERO;
    private static float TargetStrength;
    private static float TargetCompel;
    private static float Strength;
    private static float Compel;
    private static float Engage;
    private static float Pulse;
    private static float PulseO;
    private static int HeartClock;
    private static int WhisperClock = 40;

    private Scp323ClientLure() {
    }

    public static void apply(Scp323LurePayload Payload) {
        Active = Payload.Active();
        if (Active) {
            Target = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        }
        TargetStrength = Payload.Strength();
        TargetCompel = Payload.Compel();
    }

    public static void reset() {
        Active = false;
        TargetStrength = 0.0f;
        TargetCompel = 0.0f;
        Strength = 0.0f;
        Compel = 0.0f;
        Engage = 0.0f;
        Pulse = 0.0f;
        PulseO = 0.0f;
        HeartClock = 0;
        WhisperClock = 40;
    }

    public static float intensity() {
        return Math.max(Strength * Engage, Compel);
    }

    public static float pulse(float PartialTick) {
        return Mth.lerp(PartialTick, PulseO, Pulse);
    }

    public static boolean isEngaged() {
        return Engage > 0.0f;
    }

    public static void frame(DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.isPaused()) {
            return;
        }
        float Seconds = Math.min(0.1f, Delta.getRealtimeDeltaTicks() / 20.0f);
        Engage = Mth.clamp(Engage + (Active ? EngageRate : -ReleaseRate) * Seconds, 0.0f, 1.0f);
        float Blend = Math.min(1.0f, Seconds * Follow);
        Strength += (TargetStrength - Strength) * Blend;
        Compel += (TargetCompel - Compel) * Blend;
        if (Engage <= 0.0f || !User.isAlive() || User.isSleeping()) {
            return;
        }
        Vec3 Eye = User.getEyePosition(Delta.getGameTimeDeltaPartialTick(true));
        Vec3 Toward = Target.subtract(Eye);
        double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        if (Toward.lengthSqr() < 0.0025) {
            return;
        }
        float WantYaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        float WantPitch = (float) -(Mth.atan2(Toward.y, Flat) * Mth.RAD_TO_DEG);
        float Stiffness = (BaseStiffness + CloseStiffness * Strength) * Engage;
        float Factor = 1.0f - (float) Math.exp(-Stiffness * Seconds);
        float TurnYaw = Mth.wrapDegrees(WantYaw - User.getYRot()) * Factor;
        float TurnPitch = (WantPitch - User.getXRot()) * Factor;
        User.turn(TurnYaw / TurnScale, TurnPitch / TurnScale);
    }

    public static double sensitivity(double Original) {
        if (Engage <= 0.0f) {
            return Original;
        }
        double Keep = 1.0 - SensitivityCut * Math.max(Strength, 0.35f) * Engage;
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
        double Accel = (DriftBase + DriftClose * Strength * Strength) * Engage * (User.onGround() ? 1.0 : AirDrift) * (User.isInWater() ? WaterDrift : 1.0);
        User.setDeltaMovement(User.getDeltaMovement().add(Dx / Distance * Accel, 0.0, Dz / Distance * Accel));
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        PulseO = Pulse;
        Pulse *= PulseDecay;
        if (User == null || Client.isPaused()) {
            return;
        }
        float Amount = intensity();
        if (Amount < 0.02f || !User.isAlive()) {
            HeartClock = 0;
            WhisperClock = Math.max(WhisperClock, 30);
            return;
        }
        RandomSource Random = User.getRandom();
        if (--HeartClock <= 0) {
            HeartClock = Math.round(Mth.lerp(Amount, SlowBeat, FastBeat));
            Pulse = 1.0f;
            Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp323Registry.Heartbeat.get(), 0.92f + 0.12f * Amount, 0.25f + 0.55f * Amount));
        }
        if (Active && --WhisperClock <= 0) {
            WhisperClock = 90 + Random.nextInt(90) - Math.round(50.0f * Amount);
            Vec3 Eye = User.getEyePosition();
            Vec3 Toward = Target.subtract(Eye);
            Vec3 Where = Toward.lengthSqr() > WhisperReach * WhisperReach ? Eye.add(Toward.normalize().scale(WhisperReach)) : Target;
            User.level().playLocalSound(Where.x, Where.y, Where.z, Scp323Registry.Whisper.get(), SoundSource.HOSTILE, 0.3f + 0.4f * Amount, 0.85f + Random.nextFloat() * 0.2f, false);
        }
    }
}
