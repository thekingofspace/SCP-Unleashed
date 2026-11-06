package net.rai.scp_unleashed.scp428.client;

import net.rai.scp_unleashed.scp428.network.Scp428HoldPayload;

public final class Scp428ClientHooks {
    private Scp428ClientHooks() {
    }

    public static void hold(Scp428HoldPayload Payload) {
        Scp428ClientHold.update(Payload.Holders(), Payload.Presses(), Payload.Required());
    }
}
