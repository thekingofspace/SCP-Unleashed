package net.rai.scp_unleashed.control.client;

import net.rai.scp_unleashed.control.network.ControlEventPayload;
import net.rai.scp_unleashed.control.network.ControlLookPayload;

public final class ControlClientHooks {
    private ControlClientHooks() {
    }

    public static void look(ControlLookPayload Payload) {
        ControlClientState.set(Payload.Player(), Payload.Active(), Payload.Look(), Payload.Identity());
        ControlPuppets.forget(Payload.Player());
    }

    public static void event(ControlEventPayload Payload) {
        ControlOverlay.trigger(Payload.Kind());
    }
}
