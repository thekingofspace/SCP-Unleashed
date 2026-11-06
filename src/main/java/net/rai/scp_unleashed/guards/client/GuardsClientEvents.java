package net.rai.scp_unleashed.guards.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.inkwire.client.InkGhostRenderer;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GuardsClientEvents {
    private GuardsClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(GuardsRegistry.PatrolManagerMenuType.get(), PatrolManagerScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(GuardsRegistry.PatrolNodeEntity.get(), InkGhostRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(GuardsRegistry.PatrolOrders.get(), GuardsRegistry.location("written"),
                    (Stack, World, Holder, Seed) -> Stack.has(GuardsRegistry.Route.get()) ? 1.0f : 0.0f);
            ItemProperties.register(GuardsRegistry.PatrolLinker.get(), GuardsRegistry.location("bound"),
                    (Stack, World, Holder, Seed) -> Stack.has(GuardsRegistry.Route.get()) ? 1.0f : 0.0f);
        });
    }
}
