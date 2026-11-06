package net.rai.scp_unleashed.rifttech.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlockEntity;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlockEntity;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RiftTechClientEvents {
    private RiftTechClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        RiftCollectorBlockEntity.setClientEffects(CollectorClientEffects::tick);
        InstabilityPylonBlockEntity.setClientEffects(PylonClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(RiftTechRegistry.RiftCollectorEntity.get(), CollectorRenderer::new);
        Event.registerBlockEntityRenderer(RiftTechRegistry.InstabilityPylonEntity.get(), PylonRenderer::new);
        Event.registerEntityRenderer(RiftTechRegistry.ApocalypticRift.get(), ApocalypticRiftRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(CollectorRenderer.YawModel);
        Event.register(CollectorRenderer.HeadModel);
        Event.register(CollectorRenderer.HeadActiveModel);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(RiftTechRegistry.RiftCollectorMenuType.get(), CollectorScreen::new);
        Event.register(RiftTechRegistry.PylonMenuType.get(), PylonScreen::new);
    }
}
