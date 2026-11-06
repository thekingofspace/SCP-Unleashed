package net.rai.scp_unleashed.dclass.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.client.ControlLooks;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.DClassRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DClassClientEvents {
    private DClassClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ControlLooks.register(BuiltInRegistries.ENTITY_TYPE.getKey(DClassRegistry.DClassType.get()), new DClassControlLook());
            CloningMachineRenderer.registerSpecimen(CloningKind.Personnel, World -> DClassRegistry.DClassType.get().create(World));
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(DClassRegistry.DClassType.get(), DClassRenderer::new);
        Event.registerBlockEntityRenderer(DClassRegistry.CloningMachineEntity.get(), CloningMachineRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(DClassControlLook.ArmsLayer, DClassControlLook::createArms);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(DClassRegistry.CloningMenuType.get(), CloningScreen::new);
        Event.register(DClassRegistry.InventoryMenuType.get(), DClassInventoryScreen::new);
    }

    @SubscribeEvent
    public static void registerReloads(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> {
            DClassSkins.reset();
            DClassControlLook.forgetModel();
        });
    }
}
