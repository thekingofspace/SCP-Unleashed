package net.rai.scp_unleashed.scp472.client;

import net.rai.scp_unleashed.scp472.network.Scp472MindPayload;

public final class Scp472ClientHooks {
    private Scp472ClientHooks() {
    }

    public static void mind(Scp472MindPayload Payload) {
        Scp472ClientState.apply(Payload);
    }
}
