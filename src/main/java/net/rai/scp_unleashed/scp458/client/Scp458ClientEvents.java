package net.rai.scp_unleashed.scp458.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp458.Scp458Registry;
import net.rai.scp_unleashed.scp458.Scp458SliceItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp458ClientEvents {
    public static final ResourceLocation OpenProperty = Scp458Registry.location("scp458_open");
    public static final ResourceLocation PizzaProperty = Scp458Registry.location("scp458_pizza");

    private Scp458ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp458Registry.BoxItem.get(), OpenProperty, Scp458BoxItemRenderer::property);
            ItemProperties.register(Scp458Registry.Slice.get(), PizzaProperty, (Stack, World, Bearer, Seed) -> Scp458SliceItem.pizza(Stack).ordinal());
            Scp458Display.register();
        });
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp458Models.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> Scp458Models.reload());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp458Registry.BoxEntity.get(), Scp458BoxRenderer::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer Drawer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (Drawer == null) {
                    Drawer = new Scp458BoxItemRenderer();
                }
                return Drawer;
            }
        }, Scp458Registry.BoxItem.get());
    }
}
