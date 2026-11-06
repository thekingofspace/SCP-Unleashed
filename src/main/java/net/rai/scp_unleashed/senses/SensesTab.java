package net.rai.scp_unleashed.senses;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SensesTab {
    private SensesTab() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Equipment.getKey()) {
            return;
        }
        Event.accept(SensesRegistry.Earplugs.get());
        Event.accept(SensesRegistry.Blindfold.get());
    }
}
