package net.rai.scp_unleashed.scp465;

public final class Scp465Playhead {
    public static final int Silent = Integer.MIN_VALUE;
    public static final double MaxSlew = 0.06;
    public static final double Gain = 0.012;
    public static final double SmoothTicks = 20.0;
    public static final double LateSteps = 0.5;
    public static final double WrapTicks = Scp465Music.BarTicks;

    private double Position;
    private double Drift;
    private double Rate = 1.0;
    private int Played = Silent;
    private boolean Started;

    public boolean started() {
        return Started;
    }

    public double position() {
        return Position;
    }

    public double rate() {
        return Rate;
    }

    public int played() {
        return Played;
    }

    public static double phaseError(double Target, double Position) {
        double Raw = Target - Position;
        return Raw - WrapTicks * Math.rint(Raw / WrapTicks);
    }

    public int start(double Target) {
        Started = true;
        Position = Math.max(0.0, Target);
        Drift = 0.0;
        Rate = 1.0;
        int Step = Scp465Music.stepAt(Position);
        Played = Step;
        return Position - Step * Scp465Music.StepTicks <= LateSteps * Scp465Music.StepTicks ? Step : Silent;
    }

    public int advance(double Elapsed, double Target) {
        if (!Started) {
            return start(Target);
        }
        double Spent = Math.max(0.0, Elapsed);
        double Blend = Math.min(1.0, Spent / SmoothTicks);
        Drift += (phaseError(Target, Position + Spent) - Drift) * Blend;
        Rate = 1.0 + Math.max(-MaxSlew, Math.min(MaxSlew, Drift * Gain));
        Position += Spent * Rate;
        int Step = Scp465Music.stepAt(Position);
        if (Step <= Played) {
            return Silent;
        }
        int Crossed = Step - Played;
        Played = Step;
        double Late = Position - Step * Scp465Music.StepTicks;
        if (Crossed > 1 && Late > LateSteps * Scp465Music.StepTicks) {
            return Silent;
        }
        return Step;
    }
}
