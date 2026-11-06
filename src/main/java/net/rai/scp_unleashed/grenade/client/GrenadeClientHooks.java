package net.rai.scp_unleashed.grenade.client;

import net.rai.scp_unleashed.grenade.network.GrenadeFlashPayload;

public final class GrenadeClientHooks {
    private GrenadeClientHooks() {
    }

    public static void onFlash(GrenadeFlashPayload Payload) {
        FlashOverlay.flash(Payload.White(), Payload.Ring());
    }
}
