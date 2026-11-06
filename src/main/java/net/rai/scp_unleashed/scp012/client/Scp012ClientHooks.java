package net.rai.scp_unleashed.scp012.client;

import net.rai.scp_unleashed.scp012.network.Scp012StatePayload;

public final class Scp012ClientHooks {
    private Scp012ClientHooks() {
    }

    public static void state(Scp012StatePayload Payload) {
        Scp012ClientState.apply(Payload);
    }
}
