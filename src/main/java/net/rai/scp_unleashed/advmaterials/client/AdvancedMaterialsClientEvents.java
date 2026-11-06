package net.rai.scp_unleashed.advmaterials.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlockEntity;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class AdvancedMaterialsClientEvents {
    private AdvancedMaterialsClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        AdvancedMaterialsBenchBlockEntity.setClientEffects(BenchClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(AdvancedMaterialsRegistry.BenchMenu.get(), AdvancedMaterialsScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(AdvancedMaterialsRegistry.DoorEntity.get(), MagnetizedDoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerTileModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : MagnetizedDoorRenderer.allTileModels()) {
            Event.register(Model);
        }
    }
}
