package net.rai.scp_unleashed.scp352;

import software.bernie.geckolib.animation.RawAnimation;

public enum Scp352Gait {
    Idle("idle", 0, 1, new float[]{}, new float[]{}),
    Shuffle("shuffle", 1.25, 1, new float[]{0.25f, 0.75f}, new float[]{0f, 0.5f}),
    Tall("idle_tall", 0, 2, new float[]{}, new float[]{}),
    Stalk("stalk", 2.25, 2, new float[]{0f, 0.5f}, new float[]{}),
    Run("run", 4, 2, new float[]{0f, 0.5f}, new float[]{});

    public static final double StillPace = 0.006;
    public static final int StillTicks = 3;
    public static final double ShuffleEnter = 0.008;
    public static final double StalkEnter = 0.012;
    public static final double RunEnter = 0.2;
    public static final double RunExit = 0.15;
    public static final double TurnRadius = 0.35;
    public static final double MaxStep = 0.4;

    private final String Clip;
    private final double Distance;
    private final int Stage;
    private final float[] Steps;
    private final float[] Taps;
    private final RawAnimation Loop;

    Scp352Gait(String Clip, double Distance, int Stage, float[] Steps, float[] Taps) {
        this.Clip = Clip;
        this.Distance = Distance;
        this.Stage = Stage;
        this.Steps = Steps;
        this.Taps = Taps;
        this.Loop = RawAnimation.begin().thenLoop(Clip);
    }

    public String clip() {
        return Clip;
    }

    public double distance() {
        return Distance;
    }

    public int stage() {
        return Stage;
    }

    public boolean moving() {
        return Distance > 0.0;
    }

    public float[] steps() {
        return Steps;
    }

    public float[] taps() {
        return Taps;
    }

    public RawAnimation loop() {
        return Loop;
    }

    public static boolean crossed(double From, double To, float Mark) {
        return Math.floor(To - Mark) > Math.floor(From - Mark);
    }

    public static Scp352Gait byClip(String Name) {
        for (Scp352Gait Gait : values()) {
            if (Gait.Clip.equals(Name)) {
                return Gait;
            }
        }
        return null;
    }

    public static Scp352Gait choose(boolean TallStage, Scp352Gait Current, double Pace, int Still) {
        if (!TallStage) {
            if (Still >= StillTicks) {
                return Idle;
            }
            return Current == Shuffle || Pace > ShuffleEnter ? Shuffle : Idle;
        }
        if (Still >= StillTicks) {
            return Tall;
        }
        return switch (Current) {
            case Run -> Pace < RunExit ? Stalk : Run;
            case Stalk -> Pace > RunEnter ? Run : Stalk;
            default -> Pace > RunEnter ? Run : Pace > StalkEnter ? Stalk : Tall;
        };
    }
}
