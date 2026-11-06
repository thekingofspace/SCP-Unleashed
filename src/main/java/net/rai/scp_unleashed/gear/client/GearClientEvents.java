package net.rai.scp_unleashed.gear.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.GearRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GearClientEvents {
    private GearClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        GearArmorModels.locations().forEach((Piece, Location) -> Event.registerLayerDefinition(Location, () -> GearModelLayers.create(Piece)));
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers Event) {
        GearArmorModels.bake(Event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        GearArmorModels.locations().keySet().forEach(Piece -> Event.registerItem(new GearArmorExtensions(Piece), GearRegistry.item(Piece).get()));
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, GearVisorOverlay.LayerId, GearVisorOverlay::render);
    }
}
