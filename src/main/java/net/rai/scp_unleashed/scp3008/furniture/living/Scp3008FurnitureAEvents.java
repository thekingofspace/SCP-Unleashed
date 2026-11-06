package net.rai.scp_unleashed.scp3008.furniture.living;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008FurnitureAEvents {
    private Scp3008FurnitureAEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.ScpBlocks.getKey()) {
            return;
        }
        for (DeferredItem<BlockItem> Entry : Scp3008FurnitureARegistry.FurnitureItems.values()) {
            Event.accept(Entry.get());
        }
    }
}
