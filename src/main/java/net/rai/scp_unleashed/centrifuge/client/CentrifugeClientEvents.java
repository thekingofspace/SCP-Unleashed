package net.rai.scp_unleashed.centrifuge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class CentrifugeClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        CentrifugeBlockEntity.setClientEffects(CentrifugeClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(CentrifugeRegistry.CentrifugeEntity.get(), CentrifugeRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(CentrifugeRenderer.RotorModel);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(CentrifugeRegistry.CentrifugeMenuType.get(), CentrifugeScreen::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(CentrifugeRegistry.Grit.get(), CentrifugeGritParticle.Provider::new);
    }
}
