package net.rai.scp_unleashed.scp513.client;

import net.rai.scp_unleashed.scp513.network.Scp513StatePayload;

public final class Scp513ClientHooks {
    private Scp513ClientHooks() {
    }

    public static void state(Scp513StatePayload Payload) {
        Scp513ClientState.receive(Payload.Elapsed(), Payload.Seed());
    }
}
