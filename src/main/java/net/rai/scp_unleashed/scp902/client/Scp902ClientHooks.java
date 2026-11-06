package net.rai.scp_unleashed.scp902.client;

import net.rai.scp_unleashed.scp902.network.Scp902ScenePayload;

public final class Scp902ClientHooks {
    private Scp902ClientHooks() {
    }

    public static void scene(Scp902ScenePayload Payload) {
        Scp902ClientScene.receive(Payload.Kind(), Payload.Ticks());
    }
}
