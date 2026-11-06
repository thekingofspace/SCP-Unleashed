package net.rai.scp_unleashed.scp539.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp539.Scp539Helicopter;
import net.rai.scp_unleashed.scp539.Scp539Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp539ClientEvents {
    private Scp539ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            Scp539Display.register();
            Scp539Helicopter.setClientHook(Scp539HelicopterSounds::heard);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp539Registry.StoneEntity.get(), Scp539StoneRenderer::new);
        Event.registerEntityRenderer(Scp539Registry.HelicopterEntity.get(), Scp539HelicopterRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(Scp539HeldExtensions.Instance, Scp539Registry.Stone.get());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp539Overlay.LayerId, Scp539Overlay::render);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Scp539ClientGaze.frame(Event.getPartialTick());
    }

    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent Event) {
        Event.setMouseSensitivity(Scp539ClientGaze.sensitivity(Event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Scp539ClientGaze.tick();
        Scp539Overlay.tick();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp539ClientGaze.reset();
        Scp539Overlay.reset();
        Scp539HelicopterSounds.reset();
    }
}
