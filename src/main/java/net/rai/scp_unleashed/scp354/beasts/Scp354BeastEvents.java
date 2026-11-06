package net.rai.scp_unleashed.scp354.beasts;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp354BeastEvents {
    private Scp354BeastEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp354BeastRegistry.Brute.get(), Scp354Brute.createAttributes().build());
        Event.put(Scp354BeastRegistry.Crawler.get(), Scp354Crawler.createAttributes().build());
        Event.put(Scp354BeastRegistry.Wingbeast.get(), Scp354Wingbeast.createAttributes().build());
        Event.put(Scp354BeastRegistry.Leaper.get(), Scp354Leaper.createAttributes().build());
        Event.put(Scp354BeastRegistry.Charger.get(), Scp354Charger.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<DeferredSpawnEggItem> Egg : Scp354BeastRegistry.Eggs) {
                Event.accept(Egg.get());
            }
        }
    }
}
