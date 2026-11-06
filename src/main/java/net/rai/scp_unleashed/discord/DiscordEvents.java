package net.rai.scp_unleashed.discord;

import net.rai.scp_unleashed.config.ScpConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEvent;
import net.rai.scp_unleashed.scp140.api.DaeviteGodSlainEvent;
import net.rai.scp_unleashed.scp140.api.DaeviteSummonEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DiscordEvents {
    private DiscordEvents() {
    }

    public static boolean isReal(Object Candidate) {
        return Candidate instanceof ServerPlayer && !(Candidate instanceof FakePlayer);
    }

    public static boolean isFirstJoin(ServerPlayer User) {
        return User.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) <= 0;
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (isReal(Event.getEntity()) && Event.getEntity() instanceof ServerPlayer User) {
            DiscordNotices.playerJoined(User, isFirstJoin(User));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (isReal(Event.getEntity()) && Event.getEntity() instanceof ServerPlayer User) {
            DiscordNotices.playerLeft(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (isReal(Event.getEntity()) && Event.getEntity() instanceof ServerPlayer User) {
            DiscordNotices.playerDied(User, Event.getSource());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void godSummoned(DaeviteSummonEvent Event) {
        if (Event.isAnswered()) {
            DiscordNotices.daeviteGod(Event.getLevel(), Event.getAltar(), true, null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void godSlain(DaeviteGodSlainEvent Event) {
        DiscordNotices.daeviteGod(Event.getLevel(), Event.getWhere(), false, Event.getSlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void riftFormed(ApocalypticRiftEvent.Formed Event) {
        DiscordNotices.rift(Event.getLevel(), Event.getAnchor(), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void riftCollapsed(ApocalypticRiftEvent.Collapsed Event) {
        DiscordNotices.rift(Event.getLevel(), BlockPos.containing(Event.getCentre()), false);
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent Event) {
        Discord.invalidate();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Discord.shutdown();
    }

    private static boolean isOurs(Object Spec) {
        return Spec == ScpConfigs.Server || Spec == ScpConfigs.Common;
    }

    @SubscribeEvent
    public static void configLoaded(ModConfigEvent.Loading Event) {
        if (isOurs(Event.getConfig().getSpec())) {
            Discord.invalidate();
        }
    }

    @SubscribeEvent
    public static void configReloaded(ModConfigEvent.Reloading Event) {
        if (isOurs(Event.getConfig().getSpec())) {
            Discord.invalidate();
        }
    }

    @SubscribeEvent
    public static void configUnloaded(ModConfigEvent.Unloading Event) {
        if (isOurs(Event.getConfig().getSpec())) {
            Discord.invalidate();
        }
    }
}
