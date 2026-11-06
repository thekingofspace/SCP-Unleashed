package net.rai.scp_unleashed.scp140.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp140.network.Scp140ChroniclePayload;

public final class Scp140ClientHooks {
    private Scp140ClientHooks() {
    }

    public static void chronicle(Scp140ChroniclePayload Payload) {
        Scp140ChronicleView.accept(Payload);
        Minecraft Game = Minecraft.getInstance();
        if (Payload.Open() && Game.player != null && !(Game.screen instanceof Scp140BookScreen)) {
            Game.setScreen(new Scp140BookScreen());
        }
    }
}
