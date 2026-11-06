package net.rai.scp_unleashed.marksman.client;

import net.rai.scp_unleashed.marksman.network.CoinRicochetPayload;

public final class MarksmanClientHooks {
    private MarksmanClientHooks() {
    }

    public static void onRicochet(CoinRicochetPayload Payload) {
        CoinRicochetEffects.onRicochet(Payload);
    }
}
