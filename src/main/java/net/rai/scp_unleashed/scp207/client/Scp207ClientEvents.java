package net.rai.scp_unleashed.scp207.client;

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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp207.Scp207Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp207ClientEvents {
    public static final ResourceLocation DrinkingProperty = Scp207Registry.location("scp207_drinking");

    private Scp207ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp207Registry.Bottle.get(), DrinkingProperty, Scp207BottleExtensions::drinking);
            ItemProperties.register(Scp207Registry.BottleOne.get(), DrinkingProperty, Scp207BottleExtensions::drinking);
            Scp207Display.register();
        });
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(Scp207BottleExtensions.Instance, Scp207Registry.Bottle.get(), Scp207Registry.BottleOne.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp207Registry.CrateEntity.get(), Scp207CrateRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp207CrateRenderer.BottleModel);
        Event.register(Scp207Display.Rack);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp207Haze.LayerId, Scp207Haze::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp207ClientState.tick(Client);
        Scp207Haze.tick(Client);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp207ClientState.reset();
        Scp207Haze.reset();
    }
}
