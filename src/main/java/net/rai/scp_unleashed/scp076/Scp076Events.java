package net.rai.scp_unleashed.scp076;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp076Events {
    private Scp076Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp076Registry.Abel.get(), Scp076Abel.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp076Registry.AbelEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp076Registry.VaultStoneItem.get());
            Event.accept(Scp076Registry.VaultDoorItem.get());
            Event.accept(Scp076Registry.CoffinItem.get());
        }
    }
}
