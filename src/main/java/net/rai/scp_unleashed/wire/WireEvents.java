package net.rai.scp_unleashed.wire;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class WireEvents {
    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            WireNetworkManager.tickLevel(World);
        }
    }

    @SubscribeEvent
    public static void blockChanged(BlockEvent.NeighborNotifyEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            WireNetworkManager.blockChanged(World, Event.getPos());
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            WireNetworkManager.forget(World);
        }
    }
}
