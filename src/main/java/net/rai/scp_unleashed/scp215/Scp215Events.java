package net.rai.scp_unleashed.scp215;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp215Events {
    private Scp215Events() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (Event.getEntity() instanceof Scp215Phantom && !Event.getLevel().isClientSide()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp215Registry.Glasses.get());
        }
    }
}
