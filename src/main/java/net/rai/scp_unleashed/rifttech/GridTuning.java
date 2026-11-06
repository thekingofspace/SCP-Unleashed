package net.rai.scp_unleashed.rifttech;

import net.rai.scp_unleashed.anomaly.FragmentRarity;

public final class GridTuning {
    public static final int MinSide = 3;
    public static final int MaxSide = 24;
    public static final int MinRise = 3;
    public static final int MaxRise = 16;
    public static final int PylonHeight = 3;
    public static final int StandbyDraw = 40;
    public static final int ScanTicks = 20;
    public static final int WindowTicks = 20;
    public static final int AssignHold = 3;
    public static final float PoweredRatio = 0.8f;
    public static final float CornerMinRatio = 0.5f;
    public static final float FullRatio = 0.95f;
    public static final int RectangleCap = 4;
    public static final int BoxCap = 6;
    public static final float BoxFactor = 1.5f;
    public static final int ApocalypticDraw = 24000;
    public static final int ApocalypticMinIntake = 8000;
    public static final long ApocalypticCost = 360_000_000L;
    public static final int Attempts = 24;
    public static final int ObstructedAfter = 3;
    public static final int CensusTicks = 20;
    public static final int SyncTicks = 10;
    public static final int Scale = 10000;
    private static final int[] Draws = {2000, 2500, 3600, 6500, 13500};
    private static final int[] Seconds = {40, 45, 55, 65, 75};

    private GridTuning() {
    }

    public static int drawFor(FragmentRarity Tier, boolean Box) {
        int Base = Draws[Tier.getTier() - 1];
        return Box ? Math.round(Base * BoxFactor) : Base;
    }

    public static int secondsFor(FragmentRarity Tier, boolean Box) {
        int Base = Seconds[Tier.getTier() - 1];
        return Box ? Math.round(Base / BoxFactor) : Base;
    }

    public static long costFor(FragmentRarity Tier, boolean Box) {
        return (long) drawFor(Tier, Box) * secondsFor(Tier, Box) * 20L;
    }

    public static int capFor(boolean Box) {
        return Box ? BoxCap : RectangleCap;
    }

    public static int apocalypticMinutes() {
        return (int) Math.ceil(ApocalypticCost / (double) ApocalypticDraw / 1200.0);
    }

    public static int etaSeconds(long Remaining, int Rate) {
        if (Rate <= 0) {
            return -1;
        }
        long Ticks = (Remaining + Rate - 1) / Rate;
        return (int) Math.min(Short.MAX_VALUE, Ticks / 20L);
    }

    public static String clock(int Seconds) {
        if (Seconds < 0) {
            return "--:--";
        }
        int Clamped = Math.min(Seconds, 599 * 60 + 59);
        return String.format("%d:%02d", Clamped / 60, Clamped % 60);
    }

    public static String grouped(long Value) {
        return String.format("%,d", Value);
    }
}
