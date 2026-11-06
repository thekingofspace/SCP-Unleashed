package net.rai.scp_unleashed.facilitysign;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FacilitySignEvents {
    private FacilitySignEvents() {
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Facility.getKey()) {
            return;
        }
        for (FacilitySignDesign Design : FacilitySignDesign.values()) {
            if (Design.hasHeader()) {
                Event.accept(FacilitySignRegistry.HeaderItems.get(Design).get());
            }
            Event.accept(FacilitySignRegistry.SignItems.get(Design).get());
            Event.accept(FacilitySignRegistry.PlaqueItems.get(Design).get());
        }
    }
}
