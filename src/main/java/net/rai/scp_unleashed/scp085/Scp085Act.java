package net.rai.scp_unleashed.scp085;

public enum Scp085Act {
    Idle(true),
    Walk(true),
    Spot(true),
    Watch(true),
    Wave(false),
    Note(false),
    Sign(false),
    LeapOut(false),
    LeapIn(false),
    ShiftOut(false),
    ShiftBack(false),
    AnimalIdle(false),
    AnimalMove(false);

    public static final int LeapTicks = 14;
    public static final int ShiftTicks = 12;
    public static final int WaveTicks = 40;
    public static final int NoteWriteTicks = 40;
    public static final int NoteTicks = 100;
    public static final int SignTicks = 70;

    private final boolean Interruptible;

    Scp085Act(boolean Interruptible) {
        this.Interruptible = Interruptible;
    }

    public boolean interruptible() {
        return Interruptible;
    }

    public static Scp085Act byIndex(int Index) {
        Scp085Act[] Values = values();
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }
}
