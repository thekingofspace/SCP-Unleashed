package net.rai.scp_unleashed.scp163.client;

import net.rai.scp_unleashed.scp163.network.Scp163ImpactPayload;

public final class Scp163ClientHooks {
    private Scp163ClientHooks() {
    }

    public static void impact(Scp163ImpactPayload Payload) {
        Scp163ClientEffects.impact(Payload.Centre(), Payload.Radius());
    }
}
