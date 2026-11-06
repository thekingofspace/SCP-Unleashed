package net.rai.scp_unleashed.scp420j.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp420j.Scp420jRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp420jClientEvents {
    private Scp420jClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp420jDisplay::register);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp420jRegistry.Ring.get(), Scp420jRingParticle.Provider::new);
        Event.registerSpriteSet(Scp420jRegistry.Note.get(), Scp420jNoteParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp420jVivid.LayerId, Scp420jVivid::render);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Pre Event) {
        if (Event.getEntity() instanceof LocalPlayer User && User == Minecraft.getInstance().player) {
            Scp420jSteer.steer(User);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp420jClientState.tick(Client);
        Scp420jSongs.tick(Client);
        Scp420jVivid.tick();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp420jClientState.reset();
        Scp420jSongs.reset();
        Scp420jVivid.reset();
    }
}
