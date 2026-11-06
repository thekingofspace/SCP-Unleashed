package net.rai.scp_unleashed.scp5001.world.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.engine.client.Scp5001EngineRenderer;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp5001WorldClientEvents {
    private Scp5001WorldClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp5001WorldRegistry.EngineEntity.get(), Scp5001EngineRenderer::new);
    }
}
