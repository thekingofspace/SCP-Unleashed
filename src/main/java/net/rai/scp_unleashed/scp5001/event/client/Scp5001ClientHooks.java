package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001BurstPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ExposurePayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001OpenPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001StatePayload;

public final class Scp5001ClientHooks {
    private Scp5001ClientHooks() {
    }

    public static void state(Scp5001StatePayload Payload) {
        Scp5001ClientState.accept(Payload);
    }

    public static void open(Scp5001OpenPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Payload.Screen() == Scp5001OpenPayload.GeneratorScreen) {
            Client.setScreen(new Scp5001GeneratorScreen(Payload.Pos(), Payload.Linked(), Payload.Node()));
        } else {
            Client.setScreen(new Scp5001ConsoleScreen(Payload.Pos()));
        }
    }

    public static void exposure(Scp5001ExposurePayload Payload) {
        Scp5001ClientState.exposure(Payload.Stacks(), Payload.Max());
        if (Payload.Pulse()) {
            Scp5001Hallucinations.pulse();
        }
    }

    public static void burst(Scp5001BurstPayload Payload) {
        Scp5001Hallucinations.burst(Payload.X(), Payload.Y(), Payload.Z(), Payload.Radius());
    }
}
