package net.rai.scp_unleashed.stasis;

public final class StasisClock {
    private static volatile long ClientTime;

    private StasisClock() {
    }

    public static long client() {
        return ClientTime;
    }

    public static void setClient(long Time) {
        ClientTime = Time;
    }
}
