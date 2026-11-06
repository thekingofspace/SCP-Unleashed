package net.rai.scp_unleashed.solar.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.solar.SolarRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SolarClientEvents {
    private SolarClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(SolarRegistry.SolarPanelEntity.get(), SolarRenderer::new);
        Event.registerBlockEntityRenderer(SolarRegistry.SolarArrayEntity.get(), SolarRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ResourceLocation Location : SolarRenderer.Models) {
            Event.register(ModelResourceLocation.standalone(Location));
        }
    }
}
