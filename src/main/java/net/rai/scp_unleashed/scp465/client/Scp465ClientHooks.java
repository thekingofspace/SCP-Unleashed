package net.rai.scp_unleashed.scp465.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp465.network.Scp465LetterPayload;

public final class Scp465ClientHooks {
    private Scp465ClientHooks() {
    }

    public static void letter(Scp465LetterPayload Payload) {
        Minecraft.getInstance().setScreen(new Scp465LetterScreen(Payload.Line(), Payload.Name()));
    }
}
