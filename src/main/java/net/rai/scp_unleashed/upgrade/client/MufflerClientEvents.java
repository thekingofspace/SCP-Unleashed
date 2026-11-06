package net.rai.scp_unleashed.upgrade.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MufflerClientEvents {
    private MufflerClientEvents() {
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        MufflerClient.clear();
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        MufflerClient.serverSound(false);
    }
}
