package net.rai.scp_unleashed.chunkloader;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ChunkLoaderEvents {
    private ChunkLoaderEvents() {
    }

    @SubscribeEvent
    public static void registerTickets(RegisterTicketControllersEvent Event) {
        Event.register(ChunkLoaderTickets.Controller);
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(ChunkLoaderRegistry.ChunkLoaderItem.get());
        }
    }
}
