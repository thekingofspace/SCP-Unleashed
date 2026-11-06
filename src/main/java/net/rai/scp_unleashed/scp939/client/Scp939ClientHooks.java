package net.rai.scp_unleashed.scp939.client;

import net.rai.scp_unleashed.scp939.network.Scp939BreathPayload;

public final class Scp939ClientHooks {
    private Scp939ClientHooks() {
    }

    public static void breath(Scp939BreathPayload Payload) {
        Scp939ClientState.apply(Payload);
    }
}
