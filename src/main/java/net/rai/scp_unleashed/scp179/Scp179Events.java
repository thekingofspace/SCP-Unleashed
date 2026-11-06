package net.rai.scp_unleashed.scp179;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp179Events {
    private Scp179Events() {
    }

    @SubscribeEvent
    public static void pollOmens(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        if (Server.overworld().getGameTime() % Scp179Omens.PollTicks == 0L) {
            Scp179Omens.get(Server).poll(Server);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(Scp179Registry.TerminalItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(SpaceCoordinatorItem.create(Scp179Registry.TargetId));
        }
    }
}
