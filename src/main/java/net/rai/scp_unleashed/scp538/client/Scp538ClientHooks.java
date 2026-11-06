package net.rai.scp_unleashed.scp538.client;

import net.rai.scp_unleashed.scp538.network.Scp538InfectionPayload;

public final class Scp538ClientHooks {
    private Scp538ClientHooks() {
    }

    public static void infection(Scp538InfectionPayload Payload) {
        Scp538ClientState.receive(Payload);
    }
}
