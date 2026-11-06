package net.rai.scp_unleashed.scp001;

import software.bernie.geckolib.animation.RawAnimation;

public enum Scp001Gait {
    Idle("idle", 0, 1f, 1f, new float[]{-1f, -1f, -1f, -1f}, new float[]{0.77f}, new float[]{0.285f}, new float[]{0.08f, 0.17f, 0.28f, 0.555f, 0.66f, 0.825f, 0.93f}, new float[]{0.395f}),
    Creep("creep", 0.875, 0.8f, 0.8f, new float[]{0.27f, 0.79f, 0f, 0.52f}, new float[]{}, new float[]{}, new float[]{0.06f, 0.36f, 0.47f}, new float[]{0.7f}),
    Walk("walk", 1.25, 0.62f, 0.66f, new float[]{0.18f, 0.73f, 0f, 0.52f}, new float[]{}, new float[]{}, new float[]{0.12f, 0.38f, 0.6f}, new float[]{0.79f}),
    Gallop("gallop", 3.125, 0.32f, 0.36f, new float[]{0.44f, 0.55f, 0f, 0.09f}, new float[]{}, new float[]{}, new float[]{0.3f, 0.66f, 0.78f}, new float[]{});

    public static final double IdleSeconds = 4.0;
    public static final double TurnRadius = 0.45;
    public static final double StillPace = 0.008;
    public static final int StillTicks = 3;
    public static final double CreepEnter = 0.015;
    public static final double WalkEnter = 0.085;
    public static final double WalkExit = 0.06;
    public static final double GallopEnter = 0.24;
    public static final double GallopExit = 0.18;
    public static final int ArmLeft = 0;
    public static final int ArmRight = 1;
    public static final int LegLeft = 2;
    public static final int LegRight = 3;

    private final String Clip;
    private final double Distance;
    private final float ArmDuty;
    private final float LegDuty;
    private final float[] Touchdowns;
    private final float[] Knocks;
    private final float[] Taps;
    private final float[] Clicks;
    private final float[] Bursts;
    private final RawAnimation Loop;

    Scp001Gait(String Clip, double Distance, float ArmDuty, float LegDuty, float[] Touchdowns, float[] Knocks, float[] Taps, float[] Clicks, float[] Bursts) {
        this.Clip = Clip;
        this.Distance = Distance;
        this.ArmDuty = ArmDuty;
        this.LegDuty = LegDuty;
        this.Touchdowns = Touchdowns;
        this.Knocks = Knocks;
        this.Taps = Taps;
        this.Clicks = Clicks;
        this.Bursts = Bursts;
        this.Loop = RawAnimation.begin().thenLoop(Clip);
    }

    public String clip() {
        return Clip;
    }

    public double distance() {
        return Distance;
    }

    public RawAnimation loop() {
        return Loop;
    }

    public boolean moving() {
        return Distance > 0.0;
    }

    public float[] touchdowns() {
        return Touchdowns;
    }

    public float[] knocks() {
        return Knocks;
    }

    public float[] taps() {
        return Taps;
    }

    public float[] clicks() {
        return Clicks;
    }

    public float[] bursts() {
        return Bursts;
    }

    public float duty(int Limb) {
        return Limb == ArmLeft || Limb == ArmRight ? ArmDuty : LegDuty;
    }

    public boolean swinging(int Limb, double Phase) {
        if (!moving() || Touchdowns[Limb] < 0.0f) {
            return false;
        }
        double Local = Phase - Touchdowns[Limb];
        Local -= Math.floor(Local);
        return Local >= duty(Limb);
    }

    public static boolean crossed(double From, double To, float Mark) {
        return Mark >= 0.0f && Math.floor(To - Mark) > Math.floor(From - Mark);
    }

    public static Scp001Gait byClip(String Name) {
        for (Scp001Gait Gait : values()) {
            if (Gait.Clip.equals(Name)) {
                return Gait;
            }
        }
        return null;
    }

    public static Scp001Gait choose(Scp001Gait Current, double Pace, int Still) {
        if (Still >= StillTicks) {
            return Idle;
        }
        return switch (Current) {
            case Idle -> Pace > GallopEnter ? Gallop : Pace > WalkEnter ? Walk : Pace > CreepEnter ? Creep : Idle;
            case Creep -> Pace > GallopEnter ? Gallop : Pace > WalkEnter ? Walk : Creep;
            case Walk -> Pace > GallopEnter ? Gallop : Pace < WalkExit ? Creep : Walk;
            case Gallop -> Pace < WalkExit ? Creep : Pace < GallopExit ? Walk : Gallop;
        };
    }
}
