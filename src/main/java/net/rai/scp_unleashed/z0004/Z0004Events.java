package net.rai.scp_unleashed.z0004;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Z0004Events {
    private Z0004Events() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Z0004Registry.ChestItem.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Z0004Registry.Cutlass.get());
            Event.accept(Z0004Registry.CutlassEgg.get());
            Event.accept(Z0004Registry.HookEgg.get());
            Event.accept(Z0004Registry.PistolEgg.get());
            Event.accept(Z0004Registry.BlunderbussEgg.get());
            Event.accept(Z0004Registry.CaptainEgg.get());
        }
    }
}
