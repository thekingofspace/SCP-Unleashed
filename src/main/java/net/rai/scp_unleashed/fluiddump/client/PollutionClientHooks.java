package net.rai.scp_unleashed.fluiddump.client;

import net.rai.scp_unleashed.fluiddump.network.PollutionSyncPayload;

public final class PollutionClientHooks {
    private PollutionClientHooks() {
    }

    public static void receive(PollutionSyncPayload Payload) {
        PollutionClient.receive(Payload.Patches());
    }
}
