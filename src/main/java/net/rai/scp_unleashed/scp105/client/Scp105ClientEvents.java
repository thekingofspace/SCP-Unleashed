package net.rai.scp_unleashed.scp105.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp105.Scp105Photo;
import net.rai.scp_unleashed.scp105.Scp105PhotographItem;
import net.rai.scp_unleashed.scp105.Scp105Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp105ClientEvents {
    public static final ResourceLocation LookProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp105_look");
    public static final float Developing = 0.9f;

    private Scp105ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp105Registry.Iris.get(), Scp105Renderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp105Registry.Flash.get(), Scp105Particles.FlashProvider::new);
        Event.registerSpriteSet(Scp105Registry.Scrap.get(), Scp105Particles.ScrapProvider::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp105Registry.Photograph.get(), LookProperty, (Stack, World, Holder, Seed) -> {
                Scp105Photo Photo = Scp105PhotographItem.photoOf(Stack);
                if (Photo == null) {
                    return 0.0f;
                }
                return Photo.developing() ? Developing : Photo.Look() / 10.0f;
            });
            Scp105Display.register();
        });
    }
}
