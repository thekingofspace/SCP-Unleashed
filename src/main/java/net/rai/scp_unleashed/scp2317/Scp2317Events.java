package net.rai.scp_unleashed.scp2317;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp2317Events {
    private Scp2317Events() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp2317Registry.DoorItem.get());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp2317Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        if (!Event.isCanceled() && !Event.getEntity().level().isClientSide) {
            Scp2317Ritual.died(Event.getEntity(), Event.getSource());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp2317Ritual.track(User);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp2317Ritual.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp2317Ritual.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp2317Ritual.forgetAll();
        Scp2317Builder.forget();
    }
}
