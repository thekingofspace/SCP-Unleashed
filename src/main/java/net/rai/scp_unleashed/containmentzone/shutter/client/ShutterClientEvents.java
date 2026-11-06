package net.rai.scp_unleashed.containmentzone.shutter.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ShutterClientEvents {
    private ShutterClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(ShutterRegistry.Shutter.get(), ShutterRenderer::new);
    }
}
