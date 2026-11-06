package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import net.rai.scp_unleashed.scp018.Scp018Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp018ClientEvents {
    private Scp018ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp018Ball.setClientHook(BallEffects::tick);
        Event.enqueueWork(Scp018Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp018Registry.BallEntity.get(), Scp018BallRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp018Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp018Registry.Trail.get(), BallTrailParticle.Provider::new);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        BallClock.reset();
    }
}
