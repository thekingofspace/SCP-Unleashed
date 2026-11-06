package net.rai.scp_unleashed.scp294.client;

import net.rai.scp_unleashed.scp294.network.Scp294RadiancePayload;

public final class Scp294ClientHooks {
    private Scp294ClientHooks() {
    }

    public static void radiance(Scp294RadiancePayload Payload) {
        if (Payload.Ticks() <= 0) {
            Scp294Radiance.reset();
            return;
        }
        Scp294Radiance.start(Payload.Ticks());
    }
}
