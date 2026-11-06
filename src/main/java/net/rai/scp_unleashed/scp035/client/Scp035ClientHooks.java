package net.rai.scp_unleashed.scp035.client;

import net.rai.scp_unleashed.scp035.network.Scp035LurePayload;

public final class Scp035ClientHooks {
    private Scp035ClientHooks() {
    }

    public static void lure(Scp035LurePayload Payload) {
        Scp035ClientLure.apply(Payload);
    }
}
