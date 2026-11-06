package net.rai.scp_unleashed.daevite.client;

import net.rai.scp_unleashed.daevite.network.DaeviteSkyPayload;

public final class DaeviteClientHooks {
    private DaeviteClientHooks() {
    }

    public static void sky(DaeviteSkyPayload Payload) {
        DaeviteSkyClient.receive(Payload);
    }
}
