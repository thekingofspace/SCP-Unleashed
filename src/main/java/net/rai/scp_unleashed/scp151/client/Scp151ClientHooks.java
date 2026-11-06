package net.rai.scp_unleashed.scp151.client;

import net.rai.scp_unleashed.scp151.network.Scp151BreathPayload;

public final class Scp151ClientHooks {
    private Scp151ClientHooks() {
    }

    public static void breath(Scp151BreathPayload Payload) {
        Scp151ClientState.apply(Payload);
    }
}
