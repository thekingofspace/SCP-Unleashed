package net.rai.scp_unleashed.scp465.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp465.Scp465Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp465ClientEvents {
    private Scp465ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp465Registry.BoxEntity.get(), Scp465BoxRenderer::new);
        Event.registerEntityRenderer(Scp465Registry.Guest.get(), Scp465GuestRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp465BoxModels.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> Scp465BoxModels.reload());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp465Registry.Confetti.get(), Scp465Particles.ConfettiProvider::new);
        Event.registerSpriteSet(Scp465Registry.Streamer.get(), Scp465Particles.StreamerProvider::new);
    }
}
