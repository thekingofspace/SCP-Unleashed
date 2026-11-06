package net.rai.scp_unleashed.orbital.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.orbital.OrbitalDesignatorItem;
import net.rai.scp_unleashed.orbital.OrbitalEvents;
import net.rai.scp_unleashed.orbital.OrbitalPartItem;
import net.rai.scp_unleashed.orbital.OrbitalRegistry;
import net.rai.scp_unleashed.orbital.OrbitalStrikeEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class OrbitalClientEvents {
    public static final ResourceLocation FlashLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "orbital_flash");

    private OrbitalClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        OrbitalStrikeEntity.setClientHooks(OrbitalClientEffects::tick, OrbitalClientEffects::strike);
        OrbitalDesignatorItem.setClock(() -> {
            Minecraft Game = Minecraft.getInstance();
            return Game.level == null ? Long.MIN_VALUE : Game.level.getGameTime();
        });
        OrbitalPartItem.setKnownMask(OrbitalClientState::mask);
        OrbitalEvents.setClientStatus(OrbitalClientState::accept);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(OrbitalRegistry.StrikeType.get(), OrbitalStrikeRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, FlashLayer, OrbitalClientEffects::flash);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        OrbitalClientEffects.clientTick();
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        OrbitalClientEffects.shake(Event);
    }

    @SubscribeEvent
    public static void renderSky(RenderLevelStageEvent Event) {
        OrbitalSkyGlints.render(Event);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        OrbitalClientEffects.reset();
        OrbitalClientState.reset();
    }
}
