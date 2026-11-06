package net.rai.scp_unleashed.scp085.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp085.Scp085DrawingBlock;
import net.rai.scp_unleashed.scp085.Scp085Registry;
import net.rai.scp_unleashed.scp085.Scp085Resident;
import net.rai.scp_unleashed.scp085.Scp085Scene;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp085ClientEvents {
    public static final ResourceLocation SceneProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp085_scene");
    public static final ResourceLocation ResidentProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp085_resident");

    private Scp085ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp085Registry.SheetEntity.get(), Scp085SheetRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp085Registry.DrawingItem.get(), SceneProperty, (Stack, World, Holder, Seed) -> {
                Scp085Scene Scene = Scp085DrawingBlock.sceneOf(Stack);
                return Scene == null ? 0.0f : Scp085Scene.Drawings.indexOf(Scene) / 10.0f;
            });
            ItemProperties.register(Scp085Registry.HomeSheetItem.get(), ResidentProperty, (Stack, World, Holder, Seed) -> Scp085Resident.shown() ? 1.0f : 0.0f);
            Scp085Display.register();
        });
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp085Resident.show(false);
    }
}
