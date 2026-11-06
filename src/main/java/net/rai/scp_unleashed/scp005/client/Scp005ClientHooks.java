package net.rai.scp_unleashed.scp005.client;

import net.rai.scp_unleashed.scp005.network.HoldsPayload;

public final class Scp005ClientHooks {
    private Scp005ClientHooks() {
    }

    public static void holds(HoldsPayload Payload) {
        HeldDoors.replace(Payload.Doors());
    }
}
