package net.rai.scp_unleashed.extraction;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class ExtractionEvents {
    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Anomalies.getKey()) {
            Event.accept(ExtractionRegistry.RiftExtractor.get());
            Event.accept(ExtractionRegistry.RiftDetector.get());
            for (FragmentRarity Grade : FragmentRarity.values()) {
                Event.accept(ExtractionRegistry.fragmentFor(Grade));
            }
        }
        if (Event.getTabKey() == ModCreativeTabs.Creative.getKey()) {
            Event.accept(ExtractionRegistry.RiftConjurer.get());
        }
    }
}
