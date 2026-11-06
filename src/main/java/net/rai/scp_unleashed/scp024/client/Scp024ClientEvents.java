package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Stage;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp024ClientEvents {
    public static final ResourceLocation QuipLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_quip");
    public static final ResourceLocation StageEffects = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "show_stage");

    private Scp024ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp024Registry.Host.get(), Scp024HostRenderer::new);
        Event.registerEntityRenderer(Scp024Registry.Spotlight.get(), Scp024SpotlightRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp024Registry.Confetti.get(), Scp024ConfettiParticle.Provider::new);
        Event.registerSpriteSet(Scp024Registry.Streamer.get(), Scp024StreamerParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, QuipLayer, Scp024QuipOverlay::render);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(StageEffects, new Scp024StageEffects());
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level != null && Scp024Stage.inStage(Client.level)) {
            Event.setRed(0.0f);
            Event.setGreen(0.0f);
            Event.setBlue(0.0f);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp024ClientState.tick(Client);
        Scp024Crowd.tick(Client);
        Scp024Music.tick(Client);
        Scp024QuipOverlay.tick();
    }

    @SubscribeEvent
    public static void respawned(ClientPlayerNetworkEvent.Clone Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp024ClientState.inStage(Client)) {
            Scp024ClientState.clear(Client, List.of());
        }
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp024ClientState.inStage(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp024ClientState.reset();
        Scp024Music.stop(Minecraft.getInstance());
    }
}
