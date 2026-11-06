package net.rai.scp_unleashed.scp105.photo096;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp105Photo096Events {
    private Scp105Photo096Events() {
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && (User.tickCount + User.getId()) % Scp105Photo096Gift.SweepInterval == 0) {
            Scp105Photo096Gift.sweep(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp105Photo096Registry.Photograph.get());
        }
    }
}
