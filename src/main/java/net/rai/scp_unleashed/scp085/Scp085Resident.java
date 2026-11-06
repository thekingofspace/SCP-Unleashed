package net.rai.scp_unleashed.scp085;

public final class Scp085Resident {
    private static volatile boolean Shown;

    private Scp085Resident() {
    }

    public static boolean shown() {
        return Shown;
    }

    public static void show(boolean Resident) {
        Shown = Resident;
    }
}
