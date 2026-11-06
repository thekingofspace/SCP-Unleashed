package net.rai.scp_unleashed.scp327.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp327.Scp327Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp327ClientEvents {
    private Scp327ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp327Registry.Siren.get(), Scp327Renderer::new);
        Event.registerEntityRenderer(Scp327Registry.Pup.get(), Scp327Renderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp327Overlay.LayerId, Scp327Overlay::render);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Scp327ClientLure.frame(Event.getPartialTick());
    }

    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent Event) {
        Event.setMouseSensitivity(Scp327ClientLure.sensitivity(Event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Pre Event) {
        if (Event.getEntity() instanceof LocalPlayer User && User == Minecraft.getInstance().player) {
            Scp327ClientLure.drift(User);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp327ClientLure.tick();
        Scp327ClientStruggle.tick(Client);
        Scp327Choir.tick(Client);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp327ClientLure.reset();
        Scp327ClientStruggle.reset();
        Scp327Choir.reset(Minecraft.getInstance());
    }
}
