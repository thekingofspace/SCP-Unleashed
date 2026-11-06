package net.rai.scp_unleashed.scp323.client;

import net.rai.scp_unleashed.scp323.network.Scp323BondPayload;
import net.rai.scp_unleashed.scp323.network.Scp323LurePayload;
import net.rai.scp_unleashed.scp323.network.Scp323WatchPayload;

public final class Scp323ClientHooks {
    private Scp323ClientHooks() {
    }

    public static void lure(Scp323LurePayload Payload) {
        Scp323ClientLure.apply(Payload);
    }

    public static void bond(Scp323BondPayload Payload) {
        Scp323ClientBonds.set(Payload.EntityId(), Payload.Stage());
    }

    public static void watch(Scp323WatchPayload Payload) {
        Scp323ClientWatch.set(Payload.CreatureId());
    }
}
