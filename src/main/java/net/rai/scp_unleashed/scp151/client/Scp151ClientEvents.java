package net.rai.scp_unleashed.scp151.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp151ClientEvents {
    private Scp151ClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp151Overlay.LayerId, Scp151Overlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp151Overlay.tick(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void breathe(LivingBreatheEvent Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Event.getEntity() != Client.player) {
            return;
        }
        Scp151ClientState.breathe(Event, Client.level.getGameTime());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp151Overlay.reset();
        Scp151ClientState.reset();
    }
}
