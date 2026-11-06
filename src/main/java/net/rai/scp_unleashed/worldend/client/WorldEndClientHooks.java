package net.rai.scp_unleashed.worldend.client;

import net.rai.scp_unleashed.worldend.network.WorldEndPayload;

public final class WorldEndClientHooks {
    private WorldEndClientHooks() {
    }

    public static void receive(WorldEndPayload Payload) {
        WorldEndClient.receive(Payload);
    }
}
