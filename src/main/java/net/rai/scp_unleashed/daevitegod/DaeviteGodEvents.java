package net.rai.scp_unleashed.daevitegod;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteGodEvents {
    private DaeviteGodEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(DaeviteGodRegistry.God.get(), DaeviteGod.createAttributes().build());
        Event.put(DaeviteGodRegistry.Spirit.get(), DaeviteGodSpirit.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(DaeviteGodRegistry.Crown.get());
            Event.accept(DaeviteGodRegistry.SpawnEgg.get());
        }
    }
}
