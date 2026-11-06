package net.rai.scp_unleashed.radio.client;

import net.rai.scp_unleashed.radio.network.RadioGlitchPayload;

public final class RadioClientHooks {
    private RadioClientHooks() {
    }

    public static void glitch(RadioGlitchPayload Payload) {
        RadioGlitchOverlay.start(Payload.Ticks(), Payload.Seed());
    }
}
