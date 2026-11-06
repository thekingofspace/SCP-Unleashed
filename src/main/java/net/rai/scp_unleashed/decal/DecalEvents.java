package net.rai.scp_unleashed.decal;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class DecalEvents {
    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Decals.getKey()) {
            for (DecalKind Kind : DecalKind.values()) {
                Event.accept(DecalRegistry.Decals.get(Kind).get());
            }
            Event.accept(DecalRegistry.DecalScraper.get());
        }
    }
}
