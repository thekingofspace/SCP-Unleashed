package net.rai.scp_unleashed.scp3008.plywood;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008PlywoodEvents {
    private Scp3008PlywoodEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            accept(Event, Scp3008PlywoodRegistry.Placeables);
        } else if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            accept(Event, Scp3008PlywoodRegistry.Parts);
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent Event, List<DeferredItem<? extends Item>> Entries) {
        for (DeferredItem<? extends Item> Entry : Entries) {
            Event.accept(Entry.get());
        }
    }
}
