package net.rai.scp_unleashed.scp012.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp012ClientEvents {
    private Scp012ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp012Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp012Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ScoreOverlay.LayerId, ScoreOverlay::render);
        Event.registerAbove(VanillaGuiLayers.HOTBAR, ComposerMarker.LayerId, ComposerMarker::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        ScoreOverlay.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        ScoreOverlay.reset();
        Scp012ClientState.reset();
    }
}
