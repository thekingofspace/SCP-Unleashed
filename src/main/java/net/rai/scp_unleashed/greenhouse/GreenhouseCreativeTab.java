package net.rai.scp_unleashed.greenhouse;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GreenhouseCreativeTab {
    private GreenhouseCreativeTab() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Decoration.getKey()) {
            return;
        }
        for (DeferredItem<BlockItem> Entry : GreenhouseRegistry.creativeOrder()) {
            Event.accept(Entry.get());
        }
    }
}
