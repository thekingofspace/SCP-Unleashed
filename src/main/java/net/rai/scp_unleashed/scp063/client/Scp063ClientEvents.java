package net.rai.scp_unleashed.scp063.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp063.Scp063Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp063ClientEvents {
    public static final ResourceLocation BrushingProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp063_brushing");

    private Scp063ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp063Registry.Toothbrush.get(), BrushingProperty, Scp063ToothbrushExtensions::strokePhase);
            Scp063Display.register();
        });
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(Scp063ToothbrushExtensions.Instance, Scp063Registry.Toothbrush.get());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp063Registry.FoamSpeck.get(), Scp063FoamParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp063Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp063Registry.RestingEntity.get(), Scp063RestingRenderer::new);
    }
}
