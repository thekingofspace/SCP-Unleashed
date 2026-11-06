package net.rai.scp_unleashed.scp427.client;

import net.rai.scp_unleashed.scp427.network.Scp427ConvulsePayload;
import net.rai.scp_unleashed.scp427.network.Scp427StatePayload;

public final class Scp427ClientHooks {
    private Scp427ClientHooks() {
    }

    public static void state(Scp427StatePayload Payload) {
        Scp427ClientState.receive(Payload);
    }

    public static void convulse(Scp427ConvulsePayload Payload) {
        Scp427ClientState.convulse(Payload);
    }
}
