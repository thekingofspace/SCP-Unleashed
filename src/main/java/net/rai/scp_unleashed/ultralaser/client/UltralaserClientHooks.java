package net.rai.scp_unleashed.ultralaser.client;

import net.rai.scp_unleashed.ultralaser.network.UltralaserBlastPayload;
import net.rai.scp_unleashed.ultralaser.network.UltralaserPrimingPayload;

public final class UltralaserClientHooks {
    private UltralaserClientHooks() {
    }

    public static void onBlast(UltralaserBlastPayload Payload) {
        UltralaserEffects.onBlast(Payload);
    }

    public static void onPriming(UltralaserPrimingPayload Payload) {
        UltralaserPrimingSound.handle(Payload);
    }
}
