package net.rai.scp_unleashed.door;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DoorCreativeTabs {
    private DoorCreativeTabs() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Facility.getKey()) {
            return;
        }
        for (DeferredItem<BlockItem> Door : DoorRegistry.DoorItems) {
            Event.accept(Door.get());
        }
        Event.accept(DoorRegistry.DoorButtonItem.get());
        Event.accept(DoorRegistry.DoorLeverItem.get());
        Event.accept(DoorRegistry.KeycardScannerItem.get());
        for (DeferredItem<KeycardItem> Card : DoorRegistry.Keycards) {
            Event.accept(Card.get());
        }
        Event.accept(DoorRegistry.Screwdriver.get());
    }
}
