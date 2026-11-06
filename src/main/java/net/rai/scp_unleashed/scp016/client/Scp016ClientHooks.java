package net.rai.scp_unleashed.scp016.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp016.network.Scp016StatePayload;

public final class Scp016ClientHooks {
    private static int Stage;
    private static float Progress;

    private Scp016ClientHooks() {
    }

    public static void state(Scp016StatePayload Payload) {
        Stage = Payload.Stage();
        Progress = Payload.Progress();
    }

    public static int stage() {
        return Minecraft.getInstance().player == null ? 0 : Stage;
    }

    public static float progress() {
        return Progress;
    }

    public static void reset() {
        Stage = 0;
        Progress = 0.0f;
    }
}
