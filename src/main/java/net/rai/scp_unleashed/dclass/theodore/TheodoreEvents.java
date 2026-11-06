package net.rai.scp_unleashed.dclass.theodore;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class TheodoreEvents {
    private TheodoreEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(TheodoreRegistry.TheodoreType.get(), Theodore.createAttributes().build());
        Event.put(TheodoreRegistry.AmalgamationType.get(), TheodoreAmalgamation.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        TheodoreCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(TheodoreRegistry.Arsenic.get());
            Event.accept(TheodoreRegistry.TheodoreBlood.get());
            Event.accept(TheodoreRegistry.TheodoreEgg.get());
            Event.accept(TheodoreRegistry.AmalgamationEgg.get());
            Event.accept(TheodoreRegistry.DogTags.get());
        }
    }
}
