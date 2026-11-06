package net.rai.scp_unleashed.daeviteruins.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DaeviteRuinsClientEvents {
    private DaeviteRuinsClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(DaeviteRuinsRegistry.EmberBolt.get(), NoopRenderer::new);
        Event.registerEntityRenderer(DaeviteRuinsRegistry.Flare.get(), NoopRenderer::new);
        Event.registerEntityRenderer(DaeviteRuinsRegistry.ThrownSpear.get(), DaeviteThrownSpearRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(DaeviteRuinsRegistry.Spear.get(), ResourceLocation.withDefaultNamespace("throwing"),
                (Stack, World, Holder, Seed) -> Holder != null && Holder.isUsingItem() && Holder.getUseItem() == Stack ? 1.0f : 0.0f));
    }
}
