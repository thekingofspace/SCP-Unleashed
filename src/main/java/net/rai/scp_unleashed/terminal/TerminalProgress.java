package net.rai.scp_unleashed.terminal;

public final class TerminalProgress {
    public static final char Fill = '#';
    public static final char Empty = '-';
    public static final int ScanWidth = 3;
    public static final int ScanStepMs = 70;

    private TerminalProgress() {
    }

    public static int clamp(int Percent) {
        return Math.max(0, Math.min(100, Percent));
    }

    public static int percent(float Fraction) {
        if (Float.isNaN(Fraction)) {
            return 0;
        }
        return clamp((int) Math.floor(Fraction * 100.0f + 1.0e-4f));
    }

    public static int filled(int Percent, int Cells) {
        return clamp(Percent) * Math.max(0, Cells) / 100;
    }

    public static String cells(int Percent, int Cells) {
        int Filled = filled(Percent, Cells);
        return String.valueOf(Fill).repeat(Filled) + String.valueOf(Empty).repeat(Math.max(0, Cells) - Filled);
    }

    public static String bar(int Percent, int Cells) {
        return "[" + cells(Percent, Cells) + "] " + clamp(Percent) + "%";
    }

    public static int scanStart(long Now, int Cells) {
        int Room = Math.max(1, Cells - ScanWidth + 1);
        int Cycle = Math.max(1, Room * 2 - 2);
        int Phase = (int) Math.floorMod(Now / ScanStepMs, (long) Cycle);
        return Room <= 1 ? 0 : (Phase < Room ? Phase : Cycle - Phase);
    }

    public static int readyTotal(int FullDiameter) {
        return Math.max(0, FullDiameter) * Math.max(0, FullDiameter);
    }

    public static int ready(int Full, int FullDiameter) {
        return Math.max(0, Math.min(Full, readyTotal(FullDiameter)));
    }

    public static boolean agrees(int Percent, int Ready, int Total) {
        if (Total <= 0) {
            return true;
        }
        return Math.abs(clamp(Percent) - Ready * 100.0 / Total) <= 100.0 / Total + 1.0;
    }

    public static String scanner(long Now, int Cells) {
        int Size = Math.max(0, Cells);
        StringBuilder Inside = new StringBuilder(String.valueOf(Empty).repeat(Size));
        int Start = scanStart(Now, Size);
        for (int Index = Start; Index < Math.min(Size, Start + ScanWidth); Index++) {
            Inside.setCharAt(Index, Fill);
        }
        return "[" + Inside + "]";
    }
}
