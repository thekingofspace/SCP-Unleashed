package net.rai.scp_unleashed.rocketry.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rocketry.RocketryBursts;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteBoosterEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RocketryClientEvents {
    private RocketryClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        RocketEntity.setClientEffects(RocketryClientEffects::rocketTick);
        DropPodEntity.setClientEffects(RocketryClientEffects::podTick);
        SatelliteBoosterEntity.setClientEffects(RocketryClientEffects::boosterTick);
        RocketryBursts.setHandler(RocketryClientEffects::burst);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(RocketryRegistry.RocketType.get(), RocketRenderer::new);
        Event.registerEntityRenderer(RocketryRegistry.DropPodType.get(), DropPodRenderer::new);
        Event.registerEntityRenderer(RocketryRegistry.SatelliteBoosterType.get(), SatelliteBoosterRenderer::new);
        Event.registerBlockEntityRenderer(RocketryRegistry.SatellitePlatformEntity.get(), SatellitePlatformRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : RocketryModels.All) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void modelsBaked(ModelEvent.BakingCompleted Event) {
        RocketryModels.clear();
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(RocketryRegistry.RocketPlatformMenu.get(), LaunchPlatformScreen::new);
        Event.register(RocketryRegistry.SatellitePlatformMenu.get(), LaunchPlatformScreen::new);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        RocketryClientEffects.clientTick();
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        RocketryClientEffects.shake(Event);
    }
}
