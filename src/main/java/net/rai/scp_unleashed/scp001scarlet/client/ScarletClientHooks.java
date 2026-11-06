package net.rai.scp_unleashed.scp001scarlet.client;

import net.rai.scp_unleashed.scp001scarlet.network.ScarletSyncPayload;

public final class ScarletClientHooks {
    private ScarletClientHooks() {
    }

    public static void sync(ScarletSyncPayload Payload) {
        ScarletClient.receive(Payload);
    }
}
