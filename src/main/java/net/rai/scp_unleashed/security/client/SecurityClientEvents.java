package net.rai.scp_unleashed.security.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.OutletRenderer;
import net.rai.scp_unleashed.security.SecurityRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SecurityClientEvents {
    private SecurityClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(SecurityRegistry.SentryTurretEntity.get(), SentryTurretRenderer::new);
        Event.registerBlockEntityRenderer(SecurityRegistry.MotionDetectorEntity.get(), OutletRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(SentryTurretRenderer.YokeModel);
        Event.register(SentryTurretRenderer.GunModel);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(SecurityRegistry.SentryTurretMenuType.get(), SentryTurretScreen::new);
    }
}
