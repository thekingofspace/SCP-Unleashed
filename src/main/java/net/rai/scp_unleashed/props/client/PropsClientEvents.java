package net.rai.scp_unleashed.props.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropsRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class PropsClientEvents {
    private PropsClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(PropsRegistry.SecurityCameraEntity.get(), SecurityCameraRenderer::new);
        Event.registerBlockEntityRenderer(PropsRegistry.WallClockEntity.get(), WallClockRenderer::new);
        Event.registerEntityRenderer(PropsRegistry.Seat.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(PropsRegistry.DecorationWorkbenchMenuType.get(), DecorationWorkbenchScreen::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(SecurityCameraRenderer.HeadModel);
        Event.register(WallClockRenderer.HourHand);
        Event.register(WallClockRenderer.MinuteHand);
    }
}
