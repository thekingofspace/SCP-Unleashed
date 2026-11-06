package net.rai.scp_unleashed.trinitite;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class TrinititeEvents {
    private TrinititeEvents() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Resources.getKey()) {
            return;
        }
        for (DeferredItem<? extends Item> Entry : TrinititeRegistry.resourceOrder()) {
            Event.accept(Entry.get());
        }
    }
}
