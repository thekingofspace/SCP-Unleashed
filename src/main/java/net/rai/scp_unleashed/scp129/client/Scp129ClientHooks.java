package net.rai.scp_unleashed.scp129.client;

import net.rai.scp_unleashed.scp129.network.Scp129EpisodePayload;
import net.rai.scp_unleashed.scp129.network.Scp129StatePayload;

public final class Scp129ClientHooks {
    private Scp129ClientHooks() {
    }

    public static void state(Scp129StatePayload Payload) {
        Scp129ClientState.apply(Payload);
    }

    public static void episode(Scp129EpisodePayload Payload) {
        Scp129ClientState.apply(Payload);
    }
}
