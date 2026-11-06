package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp020.network.Scp020StatePayload;

public final class Scp020ClientState {
    private static int Intensity;

    private Scp020ClientState() {
    }

    public static void apply(Scp020StatePayload Payload) {
        Intensity = Math.max(0, Math.min(Scp020Affliction.MaxIntensity, Payload.Intensity()));
    }

    public static int intensity() {
        return Minecraft.getInstance().player == null ? 0 : Intensity;
    }

    public static float strength() {
        return intensity() / (float) Scp020Affliction.MaxIntensity;
    }

    public static void reset() {
        Intensity = 0;
    }
}
