package net.rai.scp_unleashed.scp610;

import software.bernie.geckolib.animation.RawAnimation;

public final class Scp610Stride {
    private final String Clip;
    private final double Distance;
    private final float[] Touchdowns;
    private final RawAnimation Loop;

    public Scp610Stride(String Clip, double Distance, float[] Touchdowns) {
        this.Clip = Clip;
        this.Distance = Distance;
        this.Touchdowns = Touchdowns;
        this.Loop = RawAnimation.begin().thenLoop(Clip);
    }

    public String clip() {
        return Clip;
    }

    public double distance() {
        return Distance;
    }

    public float[] touchdowns() {
        return Touchdowns;
    }

    public RawAnimation loop() {
        return Loop;
    }

    public static boolean crossed(double From, double To, float Mark) {
        return Mark >= 0.0f && Math.floor(To - Mark) > Math.floor(From - Mark);
    }
}
