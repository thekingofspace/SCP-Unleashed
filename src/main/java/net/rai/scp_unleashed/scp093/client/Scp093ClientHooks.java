package net.rai.scp_unleashed.scp093.client;

import net.rai.scp_unleashed.scp093.network.Scp093FlashPayload;
import net.rai.scp_unleashed.scp093.network.Scp093VisionPayload;

public final class Scp093ClientHooks {
    private Scp093ClientHooks() {
    }

    public static void flash(Scp093FlashPayload Payload) {
        Scp093ClientState.flash(Payload);
    }

    public static void vision(Scp093VisionPayload Payload) {
        Scp093ClientState.vision(Payload);
    }

    public static void thud(double X, double Y, double Z, float Weight) {
        Scp093ClientState.thud(X, Y, Z, Weight);
    }
}
