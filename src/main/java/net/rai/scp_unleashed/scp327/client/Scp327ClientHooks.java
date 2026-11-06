package net.rai.scp_unleashed.scp327.client;

import net.rai.scp_unleashed.scp327.network.Scp327HeldPayload;
import net.rai.scp_unleashed.scp327.network.Scp327LurePayload;

public final class Scp327ClientHooks {
    private Scp327ClientHooks() {
    }

    public static void lure(Scp327LurePayload Payload) {
        Scp327ClientLure.apply(Payload);
    }

    public static void held(Scp327HeldPayload Payload) {
        Scp327ClientStruggle.held(Payload.Held());
    }
}
