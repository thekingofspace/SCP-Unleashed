package net.rai.scp_unleashed.disperser;

public final class DisperserTuning {
    public static final int PowerDraw = 30;
    public static final int PowerGrace = 10;
    public static final int PulseTicks = 40;
    public static final int WarmUpTicks = 10;
    public static final double Radius = 6.0;
    public static final double NozzleHeight = 0.94;
    public static final int ActiveLight = 6;
    public static final int SprayParticles = 32;
    public static final double SpraySpeed = 0.62;
    public static final double SprayRise = 0.035;
    public static final int WispChance = 3;
    public static final double WispSpread = 0.12;

    public static final double CloudRadius = 4.0;
    public static final int CloudTicks = 160;
    public static final int CloudPulseTicks = 10;
    public static final double CloudLift = 1.0;
    public static final double CloudReachMargin = 0.35;
    public static final int CloudParticles = 4;
    public static final int CloudFadeTicks = 40;
    public static final int CloudHissTicks = 30;
    public static final double CloudCeiling = 1.1;
    public static final double BurstLift = 0.3;
    public static final int BurstParticles = 40;
    public static final double BurstSpeed = 0.5;
    public static final double BurstRise = 0.05;

    private DisperserTuning() {
    }
}
