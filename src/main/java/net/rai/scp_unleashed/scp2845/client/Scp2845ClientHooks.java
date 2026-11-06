package net.rai.scp_unleashed.scp2845.client;

import net.rai.scp_unleashed.scp2845.network.Scp2845ImpactPayload;

public final class Scp2845ClientHooks {
    private Scp2845ClientHooks() {
    }

    public static void impact(Scp2845ImpactPayload Payload) {
        Scp2845ClientEffects.impact(Payload.Centre(), Payload.Radius());
    }
}
