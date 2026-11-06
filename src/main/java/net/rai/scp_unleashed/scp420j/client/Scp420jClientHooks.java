package net.rai.scp_unleashed.scp420j.client;

import net.rai.scp_unleashed.scp420j.network.Scp420jFloatPayload;
import net.rai.scp_unleashed.scp420j.network.Scp420jSongPayload;

public final class Scp420jClientHooks {
    private Scp420jClientHooks() {
    }

    public static void floating(Scp420jFloatPayload Payload) {
        Scp420jClientState.apply(Payload);
    }

    public static void song(Scp420jSongPayload Payload) {
        Scp420jSongs.apply(Payload);
    }
}
