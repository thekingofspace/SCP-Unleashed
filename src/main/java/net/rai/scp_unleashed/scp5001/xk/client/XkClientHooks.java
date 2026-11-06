package net.rai.scp_unleashed.scp5001.xk.client;

import net.rai.scp_unleashed.scp5001.xk.network.XkSkyPayload;
import net.rai.scp_unleashed.scp5001.xk.network.YaldabaothPayload;

public final class XkClientHooks {
    private XkClientHooks() {
    }

    public static void sky(XkSkyPayload Payload) {
        XkSky.receive(Payload.Sky(), Payload.Snap());
    }

    public static void yaldabaoth(YaldabaothPayload Payload) {
        YaldabaothClient.receive(Payload);
    }
}
