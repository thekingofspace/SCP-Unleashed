package net.rai.scp_unleashed.scp106;

public final class Scp106Modes {
    private static final String[] Keys = {"wander", "hunt", "sated", "contained", "breaching", "emerging"};

    private Scp106Modes() {
    }

    public static String key(byte Mode) {
        return Mode >= 0 && Mode < Keys.length ? Keys[Mode] : Keys[0];
    }
}
