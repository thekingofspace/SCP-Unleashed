package net.rai.scp_unleashed.powerzone;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class PowerZoneCreativeTab {
    private PowerZoneCreativeTab() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Decoration.getKey()) {
            return;
        }
        for (DeferredItem<BlockItem> Entry : PowerZoneRegistry.creativeOrder()) {
            Event.accept(Entry.get());
        }
    }
}
