package net.rai.scp_unleashed.scp002.client;

import net.rai.scp_unleashed.scp002.network.ConsumePayload;
import net.rai.scp_unleashed.scp002.network.ImpactPayload;

public final class Scp002ClientHooks {
    private Scp002ClientHooks() {
    }

    public static void impact(ImpactPayload Payload) {
        Scp002ClientEffects.impact(Payload.Centre(), Payload.Radius());
    }

    public static void consume(ConsumePayload Payload) {
        Scp002ClientEffects.consume(Payload.Intensity());
    }
}
