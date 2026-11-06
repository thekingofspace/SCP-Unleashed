package net.rai.scp_unleashed.food;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FoodCreativeTabs {
    private FoodCreativeTabs() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            for (DeferredItem<SnackItem> Entry : FoodRegistry.snacks()) {
                Event.accept(Entry.get());
            }
        } else if (Event.getTabKey() == ModCreativeTabs.Decoration.getKey()) {
            Event.accept(FoodRegistry.VendingMachineItem.get());
        }
    }
}
