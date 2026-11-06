package net.rai.scp_unleashed.scp018;

public final class BallTuning {
    public static final double Gravity = 0.04;
    public static final double Radius = 0.125;
    public static final double ThrowSpeed = 1.0;
    public static final double TossSpeed = 0.4;
    public static final double BounceGain = 1.01;
    public static final double GrazeSpeed = 0.06;
    public static final double MinimumHop = 0.12;
    public static final double DamageSpeed = 1.8;
    public static final double ExplodeSpeed = 5.0;
    public static final double TrailSpeed = 1.4;
    public static final double WhooshSpeed = 1.6;
    public static final float BaseDamage = 2.0f;
    public static final float ExplosionPower = 3.0f;
    public static final double MaxSegmentTicks = 8.0;
    public static final double Lookahead = 4.0;
    public static final double ChordLength = 0.5;
    public static final double MaxChordTicks = 0.5;
    public static final int OwnerGraceTicks = 6;
    public static final double RehitTicks = 4.0;
    public static final int ThrowCooldown = 10;
    public static final double TerminalDelay = 1.0;
    public static final double TicksPerSecond = 20.0;
    public static final double WaterDrag = 0.8;
    public static final double WaterQuadraticDrag = 1.0;
    public static final double FullDragDepth = 0.25;
    public static final double Buoyancy = 0.1;
    public static final double FlowPush = 0.02;
    public static final double WaterStepTicks = 1.0;
    public static final double WaterChord = 0.5;
    public static final double RestSpeed = 0.015;
    public static final double RestBand = 0.12;
    public static final int RestCheckTicks = 10;
    public static final double BobAmplitude = 0.012;
    public static final double BobPeriod = 50.0;

    private BallTuning() {
    }

    public static float damageFor(double Speed) {
        double Ratio = Speed / DamageSpeed;
        return (float) (BaseDamage * Ratio * Ratio);
    }

    public static double floatDepth() {
        return Gravity / Buoyancy;
    }

    public static double danger(double Speed) {
        return Math.max(0.0, Math.min(1.0, (Speed - DamageSpeed) / (ExplodeSpeed - DamageSpeed)));
    }
}
