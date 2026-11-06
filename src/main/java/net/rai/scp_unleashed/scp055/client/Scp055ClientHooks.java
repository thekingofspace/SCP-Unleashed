package net.rai.scp_unleashed.scp055.client;

import net.rai.scp_unleashed.scp055.network.Scp055EpisodePayload;
import net.rai.scp_unleashed.scp055.network.Scp055WakePayload;

public final class Scp055ClientHooks {
    private Scp055ClientHooks() {
    }

    public static void episode(Scp055EpisodePayload Payload) {
        Scp055ClientEpisode.start(Payload);
    }

    public static void wake(Scp055WakePayload Payload) {
        Scp055ClientEpisode.wake(Payload);
    }
}
