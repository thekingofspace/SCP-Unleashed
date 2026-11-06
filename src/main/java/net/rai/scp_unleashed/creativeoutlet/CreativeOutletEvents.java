package net.rai.scp_unleashed.creativeoutlet;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.registry.ModItems;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class CreativeOutletEvents {
    private CreativeOutletEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Creative.getKey()) {
            place(Event, ModItems.CreativeEnergySource.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            place(Event, ModItems.TetherOutlet.get());
        }
    }

    private static void place(BuildCreativeModeTabContentsEvent Event, Item Neighbour) {
        ItemStack Outlet = new ItemStack(CreativeOutletRegistry.OutletItem.get());
        ItemStack After = new ItemStack(Neighbour);
        if (Event.getParentEntries().contains(After) && Event.getSearchEntries().contains(After)) {
            Event.insertAfter(After, Outlet, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            Event.accept(Outlet);
        }
    }
}
