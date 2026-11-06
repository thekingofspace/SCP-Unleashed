package net.rai.scp_unleashed.scp294.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp294.Scp294CupItem;
import net.rai.scp_unleashed.scp294.Scp294Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp294ClientEvents {
    public static final ResourceLocation FilledProperty = Scp294Registry.location("scp294_filled");

    private Scp294ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp294Registry.Cup.get(), FilledProperty, (Stack, World, Holder, Seed) -> Scp294CupItem.isFilled(Stack) ? 1.0f : 0.0f);
            Scp294Display.register();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp294Registry.MachineEntity.get(), Scp294MachineRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(Scp294Registry.KeyboardMenu.get(), Scp294KeyboardScreen::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp294MachineRenderer.CupModel);
        Event.register(Scp294MachineRenderer.LiquidModel);
        Event.register(Scp294MachineRenderer.StreamModel);
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register((Stack, Layer) -> Layer == 0 ? 0xFF000000 | Scp294CupItem.colourOf(Stack) : 0xFFFFFFFF, Scp294Registry.Cup.get());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAboveAll(Scp294Radiance.LayerId, Scp294Radiance::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp294Radiance.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp294Radiance.reset();
    }

    @SubscribeEvent
    public static void respawned(ClientPlayerNetworkEvent.Clone Event) {
        Scp294Radiance.reset();
    }
}
