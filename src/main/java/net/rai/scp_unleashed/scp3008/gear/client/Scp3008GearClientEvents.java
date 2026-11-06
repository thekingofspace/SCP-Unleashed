package net.rai.scp_unleashed.scp3008.gear.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp3008GearClientEvents {
    private Scp3008GearClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp3008GearRegistry.ThrownSpear.get(), Scp3008ThrownSpearRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp3008GearRegistry.Spear.get(), ResourceLocation.withDefaultNamespace("throwing"),
                    (Stack, World, Holder, Seed) -> Holder != null && Holder.isUsingItem() && Holder.getUseItem() == Stack ? 1.0f : 0.0f);
            ItemProperties.register(Scp3008GearRegistry.Shield.get(), ResourceLocation.withDefaultNamespace("blocking"),
                    (Stack, World, Holder, Seed) -> Holder != null && Holder.isUsingItem() && Holder.getUseItem() == Stack ? 1.0f : 0.0f);
        });
    }
}
