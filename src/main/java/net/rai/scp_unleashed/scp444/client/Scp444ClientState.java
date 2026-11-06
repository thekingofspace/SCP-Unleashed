package net.rai.scp_unleashed.scp444.client;

import net.rai.scp_unleashed.scp444.network.Scp444StatePayload;

public final class Scp444ClientState {
    private static volatile boolean Silenced;
    private static long Since;

    private Scp444ClientState() {
    }

    public static void receive(Scp444StatePayload Payload) {
        if (Payload.Silenced() && !Silenced) {
            Since = System.currentTimeMillis();
        }
        Silenced = Payload.Silenced();
    }

    public static boolean isSilenced() {
        return Silenced;
    }

    public static long since() {
        return Since;
    }

    public static void reset() {
        Silenced = false;
        Since = 0L;
    }
}
