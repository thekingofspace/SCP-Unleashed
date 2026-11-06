package net.rai.scp_unleashed.terminal;

public final class TerminalEta {
    public static final long MinSpanMs = 1500L;
    public static final float MinGain = 0.02f;
    public static final long StallMs = 3000L;
    public static final long WindowMs = 4000L;
    public static final long SampleMs = 200L;
    public static final double Blend = 0.3;
    public static final double MaxRisePerSecond = 2.0;
    public static final double Unknown = -1.0;
    public static final int MaxShownSeconds = 3600;

    private static final int Slots = 32;

    private final long[] Times = new long[Slots];
    private final float[] Values = new float[Slots];
    private int Count;
    private int Head;
    private long FirstAt = -1L;
    private float FirstValue;
    private float LastValue;
    private long LastChangeAt;
    private long LastUpdateAt;
    private double Shown = Unknown;

    public void reset() {
        Count = 0;
        Head = 0;
        FirstAt = -1L;
        Shown = Unknown;
    }

    public void sample(long Now, float Value) {
        float Clean = Float.isNaN(Value) ? 0.0f : Math.max(0.0f, Math.min(1.0f, Value));
        if (FirstAt < 0L || Clean + 0.001f < LastValue) {
            reset();
            FirstAt = Now;
            FirstValue = Clean;
            LastValue = Clean;
            LastChangeAt = Now;
            LastUpdateAt = Now;
            push(Now, Clean);
            return;
        }
        if (Clean > LastValue + 1.0e-5f) {
            LastValue = Clean;
            LastChangeAt = Now;
        }
        if (Now - Times[(Head + Slots - 1) % Slots] >= SampleMs) {
            push(Now, Clean);
        }
        update(Now);
    }

    public double seconds() {
        return Shown;
    }

    public boolean known() {
        return Shown >= 0.0;
    }

    public String text() {
        return format(Shown);
    }

    public double target(long Now) {
        if (FirstAt < 0L || Now - FirstAt < MinSpanMs) {
            return Unknown;
        }
        if (LastValue >= 1.0f) {
            return 0.0;
        }
        if (Now - LastChangeAt > StallMs) {
            return Unknown;
        }
        if (LastValue - FirstValue < MinGain) {
            return Unknown;
        }
        long OldestTime = Now;
        float OldestValue = LastValue;
        for (int Step = 0; Step < Count; Step++) {
            int Slot = (Head + Slots - 1 - Step) % Slots;
            if (Now - Times[Slot] > WindowMs) {
                break;
            }
            OldestTime = Times[Slot];
            OldestValue = Values[Slot];
        }
        double Rate = Now - OldestTime >= SampleMs && LastValue > OldestValue ? (LastValue - OldestValue) / (double) (Now - OldestTime) : 0.0;
        if (Rate <= 0.0) {
            Rate = (LastValue - FirstValue) / (double) Math.max(1L, Now - FirstAt);
        }
        if (Rate <= 0.0) {
            return Unknown;
        }
        return Math.min(MaxShownSeconds, (1.0 - LastValue) / Rate / 1000.0);
    }

    private void update(long Now) {
        double Target = target(Now);
        double Elapsed = Math.max(0.0, (Now - LastUpdateAt) / 1000.0);
        LastUpdateAt = Now;
        if (Target < 0.0) {
            Shown = Unknown;
            return;
        }
        if (Shown < 0.0) {
            Shown = Target;
            return;
        }
        double Ticked = Math.max(0.0, Shown - Elapsed);
        double Next = Ticked + (Target - Ticked) * Blend;
        double Ceiling = Ticked + MaxRisePerSecond * Math.max(Elapsed, 0.05);
        Shown = Math.max(0.0, Math.min(Next, Ceiling));
    }

    private void push(long Now, float Value) {
        Times[Head] = Now;
        Values[Head] = Value;
        Head = (Head + 1) % Slots;
        Count = Math.min(Slots, Count + 1);
    }

    public static String format(double Seconds) {
        if (Seconds < 0.0 || Double.isNaN(Seconds)) {
            return "--";
        }
        int Whole = (int) Math.ceil(Seconds);
        if (Whole >= MaxShownSeconds) {
            return "~60m+";
        }
        if (Whole < 60) {
            return "~" + Whole + "s";
        }
        int Rest = Whole % 60;
        return "~" + Whole / 60 + "m " + (Rest < 10 ? "0" : "") + Rest + "s";
    }
}
