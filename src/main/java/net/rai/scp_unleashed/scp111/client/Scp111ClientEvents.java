package net.rai.scp_unleashed.scp111.client;

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
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111Egg;
import net.rai.scp_unleashed.scp111.Scp111Registry;
import net.rai.scp_unleashed.scp111.Scp111Variant;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp111ClientEvents {
    public static final ResourceLocation VariantProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp111_variant");

    private Scp111ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp111Registry.EggItem.get(), VariantProperty,
                    (Stack, World, Holder, Seed) -> Scp111Egg.of(Stack).Variant().ordinal() / (float) Scp111Variant.values().length);
            Scp111Display.register();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp111Registry.Snail.get(), Scp111Renderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp111Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp111Registry.Breath.get(), Scp111Particles.BreathProvider::new);
    }
}
