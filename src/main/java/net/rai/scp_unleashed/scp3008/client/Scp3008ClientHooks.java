package net.rai.scp_unleashed.scp3008.client;

import net.rai.scp_unleashed.scp3008.network.Scp3008FadePayload;

public final class Scp3008ClientHooks {
    private Scp3008ClientHooks() {
    }

    public static void fade(Scp3008FadePayload Payload) {
        Scp3008ClientState.fade(Payload.Phase(), Payload.Ticks());
    }
}
