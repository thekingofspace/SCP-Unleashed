package net.rai.scp_unleashed.scp335.client;

import net.rai.scp_unleashed.scp335.network.Scp335StatePayload;

public final class Scp335ClientHooks {
    private Scp335ClientHooks() {
    }

    public static void state(Scp335StatePayload Payload) {
        Scp335ClientState.apply(Payload);
    }
}
