package net.rai.scp_unleashed.orbital.client;

import net.rai.scp_unleashed.orbital.network.OrbitalStatusPayload;

public final class OrbitalClientState {
    private static int Mask = -1;
    private static long ReadyAt = Long.MIN_VALUE;

    private OrbitalClientState() {
    }

    public static void accept(OrbitalStatusPayload Payload) {
        Mask = Payload.Mask();
        ReadyAt = Payload.ReadyAt();
    }

    public static int mask() {
        return Mask;
    }

    public static long readyAt() {
        return ReadyAt;
    }

    public static void reset() {
        Mask = -1;
        ReadyAt = Long.MIN_VALUE;
    }
}
