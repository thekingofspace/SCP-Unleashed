package net.rai.scp_unleashed.worldend;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class WorldEndEvents {
    private WorldEndEvents() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        WorldEnding.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Player) {
            WorldEnding.join(Player);
        }
    }

    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent Event) {
        WorldEnding.forget(List.of(Event.getEntity().getUUID()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void damage(LivingIncomingDamageEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Player && WorldEnding.holds(Player)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopped(ServerStoppedEvent Event) {
        WorldEnding.stopped(Event.getServer());
    }
}
