package net.rai.scp_unleashed.scp610.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp610.network.Scp610DialoguePayload;
import net.rai.scp_unleashed.scp610.network.Scp610StatePayload;

public final class Scp610ClientHooks {
    private Scp610ClientHooks() {
    }

    public static void state(Scp610StatePayload Payload) {
        Scp610ClientState.set(Payload.EntityId(), Payload.Stage());
    }

    public static void dialogue(Scp610DialoguePayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.screen instanceof Scp610DialogueScreen Open && Open.at(Payload.Pos())) {
            Open.continueWith(Payload.Mood(), Payload.Lines(), Payload.Options());
            return;
        }
        Client.setScreen(new Scp610DialogueScreen(Payload.Pos(), Payload.Mood(), Payload.Lines(), Payload.Options()));
    }
}
