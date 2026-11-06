package net.rai.scp_unleashed.scp472.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp472.Scp472Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp472ClientEvents {
    private Scp472ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp472Registry.BloodstoneEntity.get(), Scp472StoneRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp472StoneRenderer.GlowModel);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp472Overlay.LayerId, Scp472Overlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp472Heartbeat.tick(Client);
        Scp472Visions.tick(Client);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp472ClientState.reset();
        Scp472Heartbeat.reset();
        Scp472Visions.reset();
    }
}
