package net.rai.scp_unleashed.pressure;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class PressureEvents {
    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            PressureNetworkManager.tickLevel(World);
        }
    }

    @SubscribeEvent
    public static void levelSave(LevelEvent.Save Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            PressureNetworkManager.saveAll(World);
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            PressureNetworkManager.forget(World);
        }
    }
}
