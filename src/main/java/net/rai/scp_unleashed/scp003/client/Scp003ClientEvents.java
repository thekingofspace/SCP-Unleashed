package net.rai.scp_unleashed.scp003.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp003.Scp003Alpha;
import net.rai.scp_unleashed.scp003.Scp003Crawler;
import net.rai.scp_unleashed.scp003.Scp003Host;
import net.rai.scp_unleashed.scp003.Scp003Larva;
import net.rai.scp_unleashed.scp003.Scp003Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp003ClientEvents {
    private Scp003ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp003Registry.Larva.get(), Context -> new Scp003CreatureRenderer<>(Context, new Scp003GeoModel<Scp003Larva>("larva", null, 0.0f, 0.0f), 0.3f));
        Event.registerEntityRenderer(Scp003Registry.Crawler.get(), Context -> new Scp003CreatureRenderer<>(Context, new Scp003GeoModel<Scp003Crawler>("crawler", "look", 25.0f, 45.0f), 0.55f));
        Event.registerEntityRenderer(Scp003Registry.Host.get(), Context -> new Scp003CreatureRenderer<>(Context, new Scp003GeoModel<Scp003Host>("host", "look", 30.0f, 55.0f), 0.45f));
        Event.registerEntityRenderer(Scp003Registry.Alpha.get(), Context -> new Scp003CreatureRenderer<>(Context, new Scp003GeoModel<Scp003Alpha>("alpha", "look", 30.0f, 55.0f), 0.75f));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp003Registry.Flesh.get(), Scp003Particles.FleshProvider::new);
        Event.registerSpriteSet(Scp003Registry.Heal.get(), Scp003Particles.HealProvider::new);
        Event.registerSpriteSet(Scp003Registry.Spore.get(), Scp003Particles.SporeProvider::new);
    }
}
