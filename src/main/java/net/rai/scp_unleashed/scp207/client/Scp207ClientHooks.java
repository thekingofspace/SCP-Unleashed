package net.rai.scp_unleashed.scp207.client;

import net.rai.scp_unleashed.scp207.network.Scp207TollPayload;

public final class Scp207ClientHooks {
    private Scp207ClientHooks() {
    }

    public static void toll(Scp207TollPayload Payload) {
        Scp207ClientState.apply(Payload);
    }
}
