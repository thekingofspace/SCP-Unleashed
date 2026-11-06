package net.rai.scp_unleashed.grenade.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.GrenadeItem;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.senses.client.EarplugMuffle;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GrenadeClientEvents {
    private GrenadeClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        GrenadeEntity.setClientHook(GrenadeClientEffects::tick);
        EarplugMuffle.addSource(FlashOverlay::muffle);
        EarplugMuffle.addExemption(Instance -> Instance instanceof FlashRingSound);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(GrenadeRegistry.Grenade.get(), GrenadeRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(FlashBlur.LayerId, FlashBlur::render);
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, FlashOverlay.LayerId, FlashOverlay::render);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        GrenadeClientExtensions Extensions = new GrenadeClientExtensions();
        for (DeferredItem<GrenadeItem> Entry : GrenadeRegistry.creativeOrder()) {
            Event.registerItem(Extensions, Entry.get());
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        FlashOverlay.tick();
        FlashBlur.tick();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        FlashOverlay.reset();
        FlashBlur.reset();
    }
}
