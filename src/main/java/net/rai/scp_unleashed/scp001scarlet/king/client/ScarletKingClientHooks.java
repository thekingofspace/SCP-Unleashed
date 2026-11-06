package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletConfirmPayload;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletRiteFxPayload;

public final class ScarletKingClientHooks {
    private ScarletKingClientHooks() {
    }

    public static void confirm(ScarletConfirmPayload Payload) {
        Minecraft.getInstance().setScreen(new ScarletConfirmScreen(Payload.Nonce(), Payload.Build()));
    }

    public static void riteFx(ScarletRiteFxPayload Payload) {
        ScarletRiteFx.start(Payload);
    }
}
