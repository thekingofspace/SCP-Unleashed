package net.rai.scp_unleashed.scp031;

import java.util.function.DoubleSupplier;

public final class Scp031Gaze {
    public static final float YawFrequency = 0.42f;
    public static final float YawDamping = 0.55f;
    public static final float YawMaxSpeed = 15.0f;
    public static final float PitchFrequency = 0.5f;
    public static final float PitchDamping = 0.6f;
    public static final float PitchMaxSpeed = 9.0f;
    public static final float MaxPitch = 40.0f;
    public static final float PitchSlack = 4.0f;
    public static final float SaccadeYaw = 2.0f;
    public static final float SaccadePitch = 1.5f;
    public static final int SaccadeMin = 20;
    public static final int SaccadeSpread = 40;
    public static final float JumpAngle = 20.0f;
    public static final double UndershootChance = 0.5;
    public static final float UndershootShare = 0.8f;
    public static final int UndershootMin = 5;
    public static final int UndershootSpread = 4;
    public static final float Moved = 0.01f;

    private final Scp031HeadSpring YawSpring = new Scp031HeadSpring(YawFrequency, YawDamping, YawMaxSpeed);
    private final Scp031HeadSpring PitchSpring = new Scp031HeadSpring(PitchFrequency, PitchDamping, PitchMaxSpeed);
    private float OffsetYaw;
    private float OffsetPitch;
    private int SaccadeTimer;
    private int UndershootTicks;
    private float UndershootFrom;
    private float LastWanted;
    private boolean HadTarget;
    private float LastYaw;
    private float LastPitch;
    private boolean Written;

    public record Pose(float Yaw, float Pitch) {
    }

    public Pose step(float CurrentYaw, float CurrentPitch, float WantedYaw, float WantedPitch, boolean Tracking, DoubleSupplier Random) {
        if (Written && Math.abs(Scp031HeadSpring.wrap(CurrentYaw - LastYaw)) > Moved) {
            YawSpring.absorb();
        }
        if (Written && Math.abs(CurrentPitch - LastPitch) > Moved) {
            PitchSpring.absorb();
        }
        if (Tracking && HadTarget && Math.abs(Scp031HeadSpring.wrap(WantedYaw - LastWanted)) > JumpAngle) {
            if (Random.getAsDouble() < UndershootChance) {
                UndershootTicks = UndershootMin + (int) (Random.getAsDouble() * UndershootSpread);
                UndershootFrom = CurrentYaw;
            } else {
                UndershootTicks = 0;
            }
        }
        LastWanted = WantedYaw;
        HadTarget = Tracking;
        float AimYaw = WantedYaw;
        if (UndershootTicks > 0) {
            UndershootTicks--;
            AimYaw = UndershootFrom + Scp031HeadSpring.wrap(WantedYaw - UndershootFrom) * UndershootShare;
        }
        if (--SaccadeTimer <= 0) {
            SaccadeTimer = SaccadeMin + (int) (Random.getAsDouble() * (SaccadeSpread + 1));
            OffsetYaw = (float) (Random.getAsDouble() * 2.0 - 1.0) * SaccadeYaw;
            OffsetPitch = (float) (Random.getAsDouble() * 2.0 - 1.0) * SaccadePitch;
        }
        float AimPitch = Math.max(-MaxPitch, Math.min(MaxPitch, WantedPitch + (Tracking ? OffsetPitch : 0.0f)));
        float Yaw = YawSpring.step(CurrentYaw, AimYaw + (Tracking ? OffsetYaw : 0.0f));
        float Pitch = PitchSpring.step(CurrentPitch, AimPitch);
        Pitch = Math.max(-MaxPitch - PitchSlack, Math.min(MaxPitch + PitchSlack, Pitch));
        LastYaw = Yaw;
        LastPitch = Pitch;
        Written = true;
        return new Pose(Yaw, Pitch);
    }
}
