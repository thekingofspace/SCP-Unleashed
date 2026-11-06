package net.rai.scp_unleashed.instability;

public final class ReactorTime {
    private ReactorTime() {
    }

    public static String clock(int Seconds) {
        int Clamped = Math.max(0, Seconds);
        int Hours = Clamped / 3600;
        int Minutes = Clamped / 60 % 60;
        int Remainder = Clamped % 60;
        return Hours > 0 ? String.format("%d:%02d:%02d", Hours, Minutes, Remainder) : String.format("%d:%02d", Minutes, Remainder);
    }
}
