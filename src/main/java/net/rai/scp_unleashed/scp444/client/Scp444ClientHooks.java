package net.rai.scp_unleashed.scp444.client;

import net.minecraft.client.Minecraft;

public final class Scp444ClientHooks {
    private Scp444ClientHooks() {
    }

    public static void open() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player != null && !(Client.screen instanceof Scp444BookScreen)) {
            Client.setScreen(new Scp444BookScreen());
        }
    }
}
