package net.rai.scp_unleashed.scp539.client;

import net.rai.scp_unleashed.scp539.network.Scp539GazePayload;
import net.rai.scp_unleashed.scp539.network.Scp539WarnPayload;

public final class Scp539ClientHooks {
    private Scp539ClientHooks() {
    }

    public static void gaze(Scp539GazePayload Payload) {
        Scp539ClientGaze.apply(Payload);
    }

    public static void warn(Scp539WarnPayload Payload) {
        Scp539Overlay.warn(Payload.Ticks());
    }
}
