package net.rai.scp_unleashed.advmaterials;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class AdvancedMaterialsEvents {
    private AdvancedMaterialsEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) -> AdvancedMaterialsBenchBlock.itemHandlerAt(World, Pos, State, Side),
                AdvancedMaterialsRegistry.Bench.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(AdvancedMaterialsRegistry.BenchItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(AdvancedMaterialsRegistry.MagnetizedSteelItem.get());
            for (DeferredItem<BlockItem> Door : AdvancedMaterialsRegistry.MagnetizedDoorItems) {
                Event.accept(Door.get());
            }
        }
    }
}
