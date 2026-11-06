package net.rai.scp_unleashed.motion;

import org.jetbrains.annotations.Nullable;

public final class ActionClock {
    public static final double Unknown = -1.0;
    public static final byte None = 0;
    private byte Kind = None;
    @Nullable
    private String Clip;
    private double Begun;
    private double Length = Unknown;
    private boolean Live;
    private boolean Fresh = true;

    public interface Owner {
        ActionClock actionClock();
    }

    public void begin(byte Action, @Nullable String Name, double Now, double Already) {
        Kind = Action;
        Clip = Name;
        Begun = Now - Math.max(0.0, Already);
        Length = Unknown;
        Live = Action != None;
    }

    public void finish() {
        Live = false;
    }

    public void clear() {
        Kind = None;
        Clip = null;
        Live = false;
        Length = Unknown;
    }

    public void settle() {
        Fresh = false;
    }

    public boolean fresh() {
        return Fresh;
    }

    public boolean live() {
        return Live;
    }

    public void learn(String Name, double Ticks) {
        if (Name.equals(Clip) && Ticks > 0.0) {
            Length = Ticks;
        }
    }

    public double elapsed(double Now) {
        return Math.max(0.0, Now - Begun);
    }

    public boolean latched(double Now) {
        return !Live && Clip != null && Length > 0.0 && elapsed(Now) < Length;
    }

    @Nullable
    public String clip(double Now) {
        return Live || latched(Now) ? Clip : null;
    }

    public byte kind(double Now) {
        return Live || latched(Now) ? Kind : None;
    }

    public static double already(boolean Fresh, double Now, double Origin, double Start) {
        return Fresh ? Math.max(0.0, Now + Origin - Start) : 0.0;
    }

    public static double catchUp(double Spent, double Blend) {
        if (Blend <= 0.0 || Spent >= Blend * 3.0) {
            return Spent;
        }
        return Spent <= Blend ? 0.0 : (Spent - Blend) * 1.5;
    }
}
