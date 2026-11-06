package net.rai.scp_unleashed.stranger.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.stranger.StrangerVariants;
import net.rai.scp_unleashed.stranger.network.StrangerDialoguePayload;
import net.rai.scp_unleashed.stranger.network.StrangerVanishPayload;

public final class StrangerClientHooks {
    private StrangerClientHooks() {
    }

    public static void openDialogue(StrangerDialoguePayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        Client.setScreen(new StrangerDialogueScreen(Payload.EntityId(), StrangerVariants.get(Payload.Variant()), Payload.Dialogue(), Payload.Pages()));
    }

    public static void vanish(StrangerVanishPayload Payload) {
        StrangerClientEffects.vanish(Payload.Colour());
    }
}
