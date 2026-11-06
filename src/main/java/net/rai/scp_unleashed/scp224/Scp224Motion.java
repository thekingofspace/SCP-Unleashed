package net.rai.scp_unleashed.scp224;

import net.minecraft.util.Mth;

public final class Scp224Motion {
    public static final int Period = 40;
    public static final int BeatEvery = Period / 2;
    public static final int BeatPhase = Period / 4;
    public static final float MaxSwing = 14.0f;
    public static final float PivotX = 8.0f;
    public static final float PivotY = 18.5f;
    public static final float DialX = 8.0f;
    public static final float DialY = 25.0f;
    public static final long DayLength = 24000L;
    public static final long HalfDay = 12000L;
    public static final long Hour = 1000L;
    public static final long DawnOffset = 6000L;

    private Scp224Motion() {
    }

    public static float swing(double Time) {
        return MaxSwing * Mth.sin((float) (Mth.TWO_PI * (Time % Period) / Period));
    }

    public static boolean isBeat(long Time) {
        return Math.floorMod(Time, (long) BeatEvery) == BeatPhase;
    }

    public static boolean isTock(long Time) {
        return Math.floorMod(Time / BeatEvery, 2L) == 1L;
    }

    public static float hourAngle(double DayTime) {
        double Clock = (DayTime + DawnOffset) % HalfDay;
        if (Clock < 0.0) {
            Clock += HalfDay;
        }
        return (float) (Clock / HalfDay * 360.0);
    }

    public static float minuteAngle(double DayTime) {
        double Clock = DayTime % Hour;
        if (Clock < 0.0) {
            Clock += Hour;
        }
        return (float) (Clock / Hour * 360.0);
    }
}
