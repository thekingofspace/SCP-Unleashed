package net.rai.scp_unleashed.scp217.client;

import net.rai.scp_unleashed.scp217.network.Scp217PulsePayload;

public final class Scp217PulseHooks {
    private Scp217PulseHooks() {
    }

    public static void receive(Scp217PulsePayload Payload) {
        Scp217PulseClient.receive(Payload);
    }
}
