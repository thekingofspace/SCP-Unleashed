package net.rai.scp_unleashed.scpsign;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ScpSignEvents {
    private ScpSignEvents() {
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Facility.getKey()) {
            return;
        }
        Event.accept(ScpSignRegistry.SignItem.get());
        Event.accept(ScpSignRegistry.SmallSignItem.get());
    }
}
