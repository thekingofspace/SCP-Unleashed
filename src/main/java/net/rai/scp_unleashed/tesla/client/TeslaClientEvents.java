package net.rai.scp_unleashed.tesla.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.tesla.TeslaGateBlockEntity;
import net.rai.scp_unleashed.tesla.TeslaRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class TeslaClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        TeslaGateBlockEntity.setClientEffects(TeslaClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(TeslaRegistry.TeslaGateEntity.get(), TeslaGateRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(TeslaRegistry.Spark.get(), TeslaSparkParticle.Provider::new);
    }
}
