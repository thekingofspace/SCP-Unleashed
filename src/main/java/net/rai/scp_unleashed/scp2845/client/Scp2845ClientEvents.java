package net.rai.scp_unleashed.scp2845.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp2845.Scp2845Comet;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp2845ClientEvents {
    public static final ResourceLocation OverlayLayer = Scp2845Registry.location("scp2845_overlay");

    private Scp2845ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp2845Comet.setClientHook(Scp2845ClientEffects::cometTick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp2845Registry.DeerType.get(), Scp2845Renderer::new);
        Event.registerEntityRenderer(Scp2845Registry.SpiritType.get(), Scp2845SpiritRenderer::new);
        Event.registerEntityRenderer(Scp2845Registry.CometType.get(), Scp2845CometRenderer::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, OverlayLayer, Scp2845ClientEffects::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp2845ClientEffects.tick();
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        Scp2845ClientEffects.shake(Event);
    }
}
