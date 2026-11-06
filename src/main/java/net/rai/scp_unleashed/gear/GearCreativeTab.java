package net.rai.scp_unleashed.gear;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class GearCreativeTab {
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Equipment.getKey()) {
            return;
        }
        for (GearPiece Piece : GearPiece.values()) {
            Event.accept(GearRegistry.item(Piece).get());
        }
    }
}
