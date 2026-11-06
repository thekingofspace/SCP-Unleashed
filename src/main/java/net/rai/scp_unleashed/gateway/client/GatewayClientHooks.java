package net.rai.scp_unleashed.gateway.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.network.GatewayPromptPayload;

public final class GatewayClientHooks {
    private GatewayClientHooks() {
    }

    public static void prompt(GatewayPromptPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        GatewayDestination Destination = GatewayDestinations.get(Payload.Destination());
        if (Destination == null || Client.player == null || Client.screen instanceof ContentWarningScreen) {
            return;
        }
        if (Client.screen != null) {
            Client.screen.onClose();
        }
        Client.setScreen(new ContentWarningScreen(Payload.Gate(), Destination));
    }
}
