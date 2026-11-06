package net.rai.scp_unleashed.stasis.client;

import net.rai.scp_unleashed.stasis.network.StasisEffectPayload;

public final class StasisClientHooks {
    private StasisClientHooks() {
    }

    public static void effect(StasisEffectPayload Payload) {
        StasisClientEffects.play(Payload);
    }
}
