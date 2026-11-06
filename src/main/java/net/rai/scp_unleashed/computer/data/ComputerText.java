package net.rai.scp_unleashed.computer.data;

import net.minecraft.util.StringUtil;

import java.util.Locale;

public final class ComputerText {
    public static final int MinutesPerDay = 1440;
    public static final int TicksPerDay = 24000;

    private ComputerText() {
    }

    public static String clean(String Raw, int Max) {
        if (Raw == null) {
            return "";
        }
        String Filtered = StringUtil.filterText(Raw, false).strip();
        return Filtered.length() > Max ? Filtered.substring(0, Max).strip() : Filtered;
    }

    public static String cleanLines(String Raw, int Max, int MaxLines) {
        if (Raw == null) {
            return "";
        }
        String Filtered = StringUtil.filterText(Raw, true);
        StringBuilder Built = new StringBuilder();
        int Lines = 1;
        for (int Index = 0; Index < Filtered.length() && Built.length() < Max; Index++) {
            char Letter = Filtered.charAt(Index);
            if (Letter == '\n') {
                if (Lines >= MaxLines) {
                    continue;
                }
                Lines++;
            }
            Built.append(Letter);
        }
        return Built.toString().stripTrailing();
    }

    public static String upper(String Raw, int Max) {
        return clean(Raw, Max).toUpperCase(Locale.ROOT);
    }

    public static int clockMinute(long DayTime) {
        long Tick = Math.floorMod(DayTime, (long) TicksPerDay);
        return (int) ((Tick * 60L / 1000L + 360L) % MinutesPerDay);
    }

    public static String clock(int Minute) {
        int Wrapped = Math.floorMod(Minute, MinutesPerDay);
        return String.format(Locale.ROOT, "%02d:%02d", Wrapped / 60, Wrapped % 60);
    }

    public static String clockOf(long DayTime) {
        return clock(clockMinute(DayTime));
    }

    public static String port(int Port) {
        return String.format(Locale.ROOT, "%04d", Port);
    }
}
