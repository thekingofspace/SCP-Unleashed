package net.rai.scp_unleashed.tesla;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class TeslaEvents {
    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(TeslaRegistry.TeslaGateItem.get());
        }
    }

    @SubscribeEvent
    public static void cancelShockKnockback(LivingKnockBackEvent Event) {
        if (TeslaGateBlockEntity.isShocked(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }
}
