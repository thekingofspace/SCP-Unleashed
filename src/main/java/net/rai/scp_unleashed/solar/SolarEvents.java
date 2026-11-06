package net.rai.scp_unleashed.solar;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SolarEvents {
    private static final String PlaceAfter = "coal_generator";

    private SolarEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        ItemStack Panel = new ItemStack(SolarRegistry.SolarPanelItem.get());
        ItemStack Array = new ItemStack(SolarRegistry.SolarArrayItem.get());
        ItemStack After = present(Event, PlaceAfter);
        if (After == null) {
            Event.accept(Panel);
            Event.accept(Array);
            return;
        }
        Event.insertAfter(After, Panel, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        Event.insertAfter(Panel, Array, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    @Nullable
    private static ItemStack present(BuildCreativeModeTabContentsEvent Event, String Name) {
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
        if (!BuiltInRegistries.ITEM.containsKey(Id)) {
            return null;
        }
        ItemStack Wanted = new ItemStack(BuiltInRegistries.ITEM.get(Id));
        return Event.getParentEntries().contains(Wanted) && Event.getSearchEntries().contains(Wanted) ? Wanted : null;
    }
}
