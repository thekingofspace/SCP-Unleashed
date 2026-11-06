package net.rai.scp_unleashed.scp001.client;

import net.rai.scp_unleashed.scp001.network.Scp001GlarePayload;

public final class Scp001ClientHooks {
    private Scp001ClientHooks() {
    }

    public static void glare(Scp001GlarePayload Payload) {
        Scp001Glare.start(Payload.Strength(), Payload.Ticks());
    }
}
