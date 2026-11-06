package net.rai.scp_unleashed.scp513;

public enum Scp513Stage {
    None,
    Dread,
    Frozen,
    Approach,
    Grab,
    Strangle;

    public static final int FreezeAt = 2400;
    public static final int ArriveAt = 2480;
    public static final int ApproachTicks = 320;
    public static final int GrabAt = ArriveAt + ApproachTicks;
    public static final int ReachTicks = 40;
    public static final int StrangleAt = GrabAt + ReachTicks;
    public static final int StrangleTicks = 120;
    public static final int DeathAt = StrangleAt + StrangleTicks;
    public static final int HastenShare = 4;

    public static Scp513Stage of(int Elapsed) {
        if (Elapsed < 0) {
            return None;
        }
        if (Elapsed < FreezeAt) {
            return Dread;
        }
        if (Elapsed < ArriveAt) {
            return Frozen;
        }
        if (Elapsed < GrabAt) {
            return Approach;
        }
        if (Elapsed < StrangleAt) {
            return Grab;
        }
        return Strangle;
    }

    public static int hastened(int Elapsed) {
        if (Elapsed < 0 || Elapsed >= FreezeAt) {
            return Elapsed;
        }
        return Elapsed + (FreezeAt - Elapsed) / HastenShare;
    }

    public static float dreadShare(int Elapsed) {
        return Math.max(0.0f, Math.min(1.0f, Elapsed / (float) FreezeAt));
    }

    public boolean frozen() {
        return ordinal() >= Frozen.ordinal();
    }

    public boolean seized() {
        return ordinal() >= Grab.ordinal();
    }
}
