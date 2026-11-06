package net.rai.scp_unleashed.intro.client;

import net.rai.scp_unleashed.intro.network.IntroAwaitPayload;
import net.rai.scp_unleashed.intro.network.IntroCuePayload;
import net.rai.scp_unleashed.intro.network.IntroScenePayload;

public final class IntroClientHooks {
    private IntroClientHooks() {
    }

    public static void await(IntroAwaitPayload Payload) {
        IntroClient.await(Payload.Token(), Payload.Stage());
    }

    public static void scene(IntroScenePayload Payload) {
        IntroClient.scene(Payload.Token(), Payload.Scene(), Payload.EntityId(), Payload.Origin());
    }

    public static void cue(IntroCuePayload Payload) {
        switch (Payload.Cue()) {
            case IntroCuePayload.Crossing -> IntroClient.crossing();
            case IntroCuePayload.Leaving -> IntroClient.leaving();
            case IntroCuePayload.Arrived -> IntroClient.arrived();
            default -> IntroClient.abort();
        }
    }
}
