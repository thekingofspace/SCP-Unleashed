package net.rai.scp_unleashed.labware;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public final class LabClock {
    public static final long DayLength = 24000L;
    public static final long HourLength = 1000L;
    public static final int DawnHour = 6;

    private LabClock() {
    }

    public static long day(long DayTime) {
        return Math.floorDiv(DayTime, DayLength) + 1L;
    }

    public static String time(long DayTime) {
        long Within = Math.floorMod(DayTime, DayLength);
        long Hour = (Within / HourLength + DawnHour) % 24L;
        long Minute = Within % HourLength * 60L / HourLength;
        return String.format(Locale.ROOT, "%02d:%02d", Hour, Minute);
    }

    public static MutableComponent stamp(long DayTime) {
        return Component.translatable("labware.scp_unleashed.clock", day(DayTime), time(DayTime));
    }
}
