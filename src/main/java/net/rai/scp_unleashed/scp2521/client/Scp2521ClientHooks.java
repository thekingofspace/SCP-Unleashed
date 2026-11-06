package net.rai.scp_unleashed.scp2521.client;

import net.rai.scp_unleashed.scp2521.network.Scp2521ScenePayload;

public final class Scp2521ClientHooks {
    private Scp2521ClientHooks() {
    }

    public static void scene(Scp2521ScenePayload Payload) {
        Scp2521ClientScene.receive(Payload);
    }

    public static boolean cinematic() {
        return Scp2521ClientScene.cinematic();
    }
}
