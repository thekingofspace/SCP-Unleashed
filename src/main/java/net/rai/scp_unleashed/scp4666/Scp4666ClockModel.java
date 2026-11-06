package net.rai.scp_unleashed.scp4666;

public final class Scp4666ClockModel {
    public static final long ReachAhead = 40L;
    public static final double Blend = 0.15;
    public static final double SnapReach = 2400.0;
    private long Base = Scp4666Clock.Arrival;
    private boolean Running;
    private int Phase = Scp4666State.PhaseDay;
    private float Rate = 1.0f;
    private long Since;
    private double Shown = Scp4666Clock.Arrival;
    private double Step;
    private boolean Fresh = true;
    private double LastError;
    private double WorstError;
    private int Snaps;

    public void accept(long Clock, boolean Moving, int Now, float Speed) {
        Base = Clock;
        Running = Moving;
        Phase = Now;
        Rate = Speed;
        Since = 0;
        LastError = target() - Shown;
        if (Fresh || Math.abs(LastError) > SnapReach) {
            snap();
            Fresh = false;
            return;
        }
        WorstError = Math.max(WorstError, Math.abs(LastError));
    }

    public void tick() {
        Since++;
        double Error = target() - Shown;
        if (Math.abs(Error) > SnapReach) {
            snap();
            return;
        }
        Step = Math.max(0.0, (Running ? Rate : 0.0) + Error * Blend);
        Shown = cap(Shown + Step);
    }

    public double target() {
        return cap(Running ? Base + Math.min(Since, ReachAhead) * (double) Rate : Base);
    }

    public double shown(float Partial) {
        return cap(Shown + Step * Partial);
    }

    public long time(float Partial) {
        return (long) Math.floor(shown(Partial));
    }

    public void reset() {
        Fresh = true;
        Since = 0;
        Step = 0.0;
        WorstError = 0.0;
        Snaps = 0;
    }

    public boolean running() {
        return Running;
    }

    public float rate() {
        return Rate;
    }

    public double lastError() {
        return LastError;
    }

    public double worstError() {
        return WorstError;
    }

    public int snaps() {
        return Snaps;
    }

    private void snap() {
        Shown = target();
        Step = 0.0;
        Snaps++;
    }

    private double cap(double Value) {
        return Phase == Scp4666State.PhaseDay && Value > Scp4666Clock.Midnight ? Scp4666Clock.Midnight : Value;
    }
}
