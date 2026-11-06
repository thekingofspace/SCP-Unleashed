package net.rai.scp_unleashed.semanticblocker;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp148.Scp148Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SemanticBlockerEvents {
    private SemanticBlockerEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.ScpBlocks.getKey()) {
            return;
        }
        ItemStack Blocker = new ItemStack(SemanticBlockerRegistry.BlockerItem.get());
        ItemStack Ward = new ItemStack(Scp148Registry.WardItem.get());
        if (Event.getParentEntries().contains(Ward) && Event.getSearchEntries().contains(Ward)) {
            Event.insertAfter(Ward, Blocker, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            Event.accept(Blocker);
        }
    }
}
