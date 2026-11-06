package net.rai.scp_unleashed.arrival.client;

import net.rai.scp_unleashed.arrival.network.ArrivalCardPayload;

public final class ArrivalClientHooks {
    private ArrivalClientHooks() {
    }

    public static void card(ArrivalCardPayload Payload) {
        ArrivalCard.queue(Payload.Dimension());
    }
}
