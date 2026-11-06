package net.rai.scp_unleashed.scp015;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp015Events {
    private Scp015Events() {
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp015Growth.get(World).tick(World);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp015Registry.PipeItem.get());
        }
    }
}
