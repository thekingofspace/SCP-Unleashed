package net.rai.scp_unleashed.scp354;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp354Events {
    private Scp354Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp354Registry.Tentacle.get(), Scp354Tentacle.createAttributes().build());
        Event.put(Scp354Registry.Lion.get(), Scp354Lion.createAttributes().build());
        Event.put(Scp354Registry.Lizard.get(), Scp354Lizard.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp354Registry.TentacleEgg.get());
            Event.accept(Scp354Registry.LionEgg.get());
            Event.accept(Scp354Registry.LizardEgg.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<? extends Item> Entry : Scp354Registry.BlockItems) {
                Event.accept(Entry.get());
            }
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp354Emergence.tick(World);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp354Grab.clear();
    }
}
