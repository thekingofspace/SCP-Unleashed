package net.rai.scp_unleashed.scp5001.world.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp5001.terminal.Scp5001TerminalPayload;
import net.rai.scp_unleashed.scp5001.terminal.client.Scp5001TerminalScreen;

public final class Scp5001WorldClientHooks {
    private Scp5001WorldClientHooks() {
    }

    public static void terminal(Scp5001TerminalPayload Payload) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new Scp5001TerminalScreen(Payload)));
    }
}
