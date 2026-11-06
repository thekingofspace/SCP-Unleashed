package net.rai.scp_unleashed.scp244.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp244.Scp244Registry;
import net.rai.scp_unleashed.scp244.Scp244VaseBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp244ClientEvents {
    private Scp244ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp244VaseBlockEntity.setClientTicker(Scp244VaseEffects::tick);
        Event.enqueueWork(Scp244Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp244Registry.VaseEntity.get(), Scp244VaseRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Lid : Scp244VaseRenderer.lidModels()) {
            Event.register(Lid);
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp244Registry.Vapour.get(), Scp244VapourParticle.Provider::new);
    }
}
