package net.rai.scp_unleashed.scp149;

import net.minecraft.util.Mth;

public final class Scp149Wings {
    public static final int BeatTicks = 2;
    public static final int HoverTicks = 20;
    public static final int DartTicks = 10;
    public static final double DartAbove = 0.2;
    public static final double HoverBelow = 0.16;
    public static final double Response = 0.4;
    public static final int StartTicks = 3;
    public static final int StopTicks = 5;
    public static final double SignalResponse = 0.3;
    public static final double SwingResponse = 0.18;
    public static final double HindResponse = 0.12;
    public static final float TrailGain = 50.0f;
    public static final float TrailMax = 14.0f;
    public static final float HindGain = 1.2f;
    public static final float HindTrailMax = 16.0f;
    public static final float YawGain = 0.6f;
    public static final float YawMax = 9.0f;
    public static final float RollGain = 0.4f;
    public static final float RollMax = 6.0f;
    public static final float PitchGain = 40.0f;
    public static final float PitchMax = 6.0f;
    public static final float GorgedSway = 1.5f;
    public static final float GorgedDroop = 5.0f;
    public static final float GorgedPendulum = 2.5f;
    public static final int PendulumTicks = 50;
    public static final float PumpAmount = 0.035f;
    public static final int PumpTicks = 16;
    private double Speed;
    private boolean Darting;
    private int Eager;
    private int Calm;
    private double Forward;
    private double ForwardO;
    private double Climb;
    private double ClimbO;
    private double Turn;
    private double Swing;
    private double SwingO;
    private double Hind;
    private double HindO;
    private float LastYaw;
    private boolean Primed;

    public boolean darting() {
        return Darting;
    }

    public double speed() {
        return Speed;
    }

    public void tick(Scp149Fly Owner) {
        double Dx = Owner.getX() - Owner.xo;
        double Dy = Owner.getY() - Owner.yo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) - Speed) * Response;
        signals(Owner, Dx, Dy, Dz);
        if (!Owner.isAlive()) {
            Darting = false;
            return;
        }
        if (!Darting) {
            Eager = Speed > DartAbove ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Darting = true;
                Eager = 0;
                Calm = 0;
            }
            return;
        }
        Calm = Speed < HoverBelow ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Darting = false;
            Calm = 0;
            Eager = 0;
        }
    }

    private void signals(Scp149Fly Owner, double Dx, double Dy, double Dz) {
        float Yaw = Owner.getYRot();
        double Facing = Math.toRadians(Yaw);
        double Ahead = -Dx * Math.sin(Facing) + Dz * Math.cos(Facing);
        float Delta = Primed ? Mth.wrapDegrees(Yaw - LastYaw) : 0.0f;
        LastYaw = Yaw;
        Primed = true;
        ForwardO = Forward;
        ClimbO = Climb;
        SwingO = Swing;
        HindO = Hind;
        Forward += (Math.max(0.0, Ahead) - Forward) * SignalResponse;
        Climb += (Dy - Climb) * SignalResponse;
        Turn += (Delta - Turn) * SignalResponse;
        Swing += (Turn - Swing) * SwingResponse;
        Hind += (Forward - Hind) * HindResponse;
    }

    private static double lerp(double Before, double After, float Partial) {
        return Before + (After - Before) * Partial;
    }

    public float legTrail(float Partial) {
        return Mth.clamp((float) lerp(ForwardO, Forward, Partial) * TrailGain, 0.0f, TrailMax);
    }

    public float hindTrail(float Partial) {
        return Mth.clamp((float) lerp(HindO, Hind, Partial) * TrailGain * HindGain, 0.0f, HindTrailMax);
    }

    public float abdomenYaw(float Partial, boolean Gorged) {
        float Gain = Gorged ? GorgedSway : 1.0f;
        return Mth.clamp(-(float) lerp(SwingO, Swing, Partial) * YawGain * Gain, -YawMax * Gain, YawMax * Gain);
    }

    public float abdomenRoll(float Partial, boolean Gorged) {
        float Gain = Gorged ? GorgedSway : 1.0f;
        return Mth.clamp((float) lerp(SwingO, Swing, Partial) * RollGain * Gain, -RollMax * Gain, RollMax * Gain);
    }

    public float abdomenPitch(float Partial, boolean Gorged) {
        return Mth.clamp(-(float) lerp(ClimbO, Climb, Partial) * PitchGain, -PitchMax, PitchMax) - (Gorged ? GorgedDroop : 0.0f);
    }

    public static float pendulum(float Time) {
        return GorgedPendulum * Mth.sin(Time / PendulumTicks * Mth.TWO_PI);
    }

    public static float pump(float Time) {
        return 1.0f + PumpAmount * Mth.sin(Time / PumpTicks * Mth.TWO_PI);
    }
}
