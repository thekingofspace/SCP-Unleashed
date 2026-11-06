package net.rai.scp_unleashed.door.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DoorClientEvents {
    private DoorClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        PanelDoorBlockEntity.setClientEffects(HeavyDoorSounds::cue);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(DoorRegistry.PanelDoor.get(), PanelDoorRenderer::new);
        Event.registerBlockEntityRenderer(DoorRegistry.DoorLeverEntity.get(), DoorLeverRenderer::new);
    }

    @SubscribeEvent
    public static void registerTileModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : PanelDoorRenderer.allTileModels()) {
            Event.register(Model);
        }
        Event.register(DoorLeverRenderer.HandleModel);
    }
}
