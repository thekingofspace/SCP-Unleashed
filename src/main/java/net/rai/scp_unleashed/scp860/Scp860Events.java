package net.rai.scp_unleashed.scp860;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp860Events {
    public static final int EvictEvery = 20;
    private static final Map<MinecraftServer, Integer> Occupancy = new ConcurrentHashMap<>();

    private Scp860Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp860Registry.Watcher.get(), Scp860Watcher.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp860Registry.Key.get());
            Event.accept(Scp860Registry.Egg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp860Registry.DoorItem.get());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp860Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent Event) {
        MinecraftServer Server = Event.getServer();
        Occupancy.put(Server, 0);
        ServerLevel World = Scp860Forest.level(Server);
        if (World != null && !Scp860Forest.standingIn()) {
            reset(World);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        MinecraftServer Server = Event.getServer();
        Occupancy.remove(Server);
        Scp860Builder.forget(Server);
        Scp860Director.forget(Server);
        Scp860Forest.forget(Server);
        Scp860Trip.forgetAll();
    }

    public static void reset(ServerLevel World) {
        Scp860State.get(World.getServer()).reseed();
        Scp860Forest.forget(World.getServer());
        Scp860Builder.forget(World.getServer());
        Scp860Builder.clearEntities(World);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        ServerLevel World = Scp860Forest.level(Server);
        if (World == null) {
            return;
        }
        int Count = Scp860Forest.participants(World).size();
        int Before = Occupancy.getOrDefault(Server, 0);
        Occupancy.put(Server, Count);
        if (Before > 0 && Count == 0) {
            reset(World);
        }
        Scp860Builder.tick(World);
        Scp860Director.tick(Server);
        if (Server.getTickCount() % EvictEvery == 0 && !Scp860Forest.standingIn()) {
            for (ServerPlayer User : new ArrayList<>(World.players())) {
                if (!Scp860Forest.record(User).Active && !Scp860Forest.exempt(User) && User.isAlive()) {
                    Scp860Trip.loggedIn(User);
                }
            }
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        Scp860Trip.track(User);
        if (Scp860Forest.present(User)) {
            User.getFoodData().setExhaustion(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp860Trip.loggedIn(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp860Director.left(User);
            Scp860Trip.forget(User.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void respawnPosition(PlayerRespawnPositionEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || Event.isFromEndFight()) {
            return;
        }
        DimensionTransition Target = Scp860Trip.respawnTarget(User);
        if (Target != null) {
            Event.setDimensionTransition(Target);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isEndConquered()) {
            Scp860Trip.respawned(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp860Trip.changedDimension(User, Event.getFrom(), Event.getTo());
        }
    }
}
