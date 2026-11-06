package net.rai.scp_unleashed.scp012.client;

import net.rai.scp_unleashed.scp012.network.Scp012StatePayload;

public final class Scp012ClientState {
    private static boolean Composer;
    private static boolean Exposed;
    private static int Meter;

    private Scp012ClientState() {
    }

    public static void apply(Scp012StatePayload Payload) {
        Composer = Payload.Composer();
        Exposed = Payload.Exposed();
        Meter = Payload.Meter();
    }

    public static boolean isComposer() {
        return Composer;
    }

    public static boolean isExposed() {
        return Exposed;
    }

    public static int meter() {
        return Meter;
    }

    public static void reset() {
        Composer = false;
        Exposed = false;
        Meter = 0;
    }
}
