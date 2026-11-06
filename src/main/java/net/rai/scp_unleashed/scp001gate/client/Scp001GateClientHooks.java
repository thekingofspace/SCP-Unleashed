package net.rai.scp_unleashed.scp001gate.client;

import net.rai.scp_unleashed.scp001gate.network.Scp001GateStrikePayload;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateWarnPayload;

public final class Scp001GateClientHooks {
    private Scp001GateClientHooks() {
    }

    public static void warn(Scp001GateWarnPayload Payload) {
        Scp001GateOverlay.show(Payload);
    }

    public static void strike(Scp001GateStrikePayload Payload) {
        Scp001GateEffects.strike(Payload);
    }
}
