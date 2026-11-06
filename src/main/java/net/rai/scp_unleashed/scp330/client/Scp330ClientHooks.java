package net.rai.scp_unleashed.scp330.client;

import net.rai.scp_unleashed.scp330.network.Scp330MarkPayload;

public final class Scp330ClientHooks {
    private Scp330ClientHooks() {
    }

    public static void mark(Scp330MarkPayload Payload) {
        Scp330ClientState.set(Payload.EntityId(), Payload.Flags());
    }
}
