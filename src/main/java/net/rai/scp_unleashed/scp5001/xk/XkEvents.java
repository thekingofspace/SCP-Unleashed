package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class XkEvents {
    private XkEvents() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre Event) {
        Scp5001Xk.tick(Event.getServer());
        XkRising.tick(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void death(LivingDeathEvent Event) {
        if (!Event.isCanceled() && !Event.getEntity().level().isClientSide) {
            XkRising.died(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide && Event.getEntity() instanceof Yaldabaoth Made && !Scp5001Xk.adopt(Made)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Player) {
            Scp5001Xk.joined(Player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        Scp5001Xk.forgetSync(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp5001Xk.forgetSync(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        XkRising.clear();
        Scp5001Xk.reset();
    }
}
