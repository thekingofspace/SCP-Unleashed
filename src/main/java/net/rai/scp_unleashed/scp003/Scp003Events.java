package net.rai.scp_unleashed.scp003;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp003Events {
    private Scp003Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp003Registry.Larva.get(), Scp003Larva.createAttributes().build());
        Event.put(Scp003Registry.Crawler.get(), Scp003Crawler.createAttributes().build());
        Event.put(Scp003Registry.Host.get(), Scp003Host.createAttributes().build());
        Event.put(Scp003Registry.Alpha.get(), Scp003Alpha.createAttributes().build());
    }

    @SubscribeEvent
    public static void tickColony(ServerTickEvent.Post Event) {
        Scp003Colony.get(Event.getServer()).tick(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp003Registry.BoardItem.get());
            Event.accept(Scp003Registry.LarvaEgg.get());
            Event.accept(Scp003Registry.CrawlerEgg.get());
            Event.accept(Scp003Registry.HostEgg.get());
            Event.accept(Scp003Registry.AlphaEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp003Registry.PlateItem.get());
        }
    }
}
