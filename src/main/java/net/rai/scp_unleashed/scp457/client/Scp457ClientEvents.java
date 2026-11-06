package net.rai.scp_unleashed.scp457.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Effects;
import net.rai.scp_unleashed.scp457.Scp457Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp457ClientEvents {
    public static final int LoopCheck = 10;

    private Scp457ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp457Effects.setClientTicker(Scp457BodyFire::tick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp457Registry.Burning.get(), Scp457Renderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp457Registry.Flame.get(), Scp457Particles.FlameProvider::new);
        Event.registerSpriteSet(Scp457Registry.Lick.get(), Scp457Particles.LickProvider::new);
        Event.registerSpriteSet(Scp457Registry.Ember.get(), Scp457Particles.EmberProvider::new);
        Event.registerSpriteSet(Scp457Registry.Ash.get(), Scp457Particles.AshProvider::new);
        Event.registerSpriteSet(Scp457Registry.Steam.get(), Scp457Particles.SteamProvider::new);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null || Client.isPaused() || Client.level.getGameTime() % LoopCheck != 0) {
            return;
        }
        for (Scp457 Burning : Client.level.getEntitiesOfClass(Scp457.class, Client.player.getBoundingBox().inflate(Scp457CrackleSound.Range), Scp457::isAlive)) {
            Scp457CrackleSound.ensure(Burning);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp457CrackleSound.clear();
    }
}
