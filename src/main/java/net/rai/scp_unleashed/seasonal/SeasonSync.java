package net.rai.scp_unleashed.seasonal;

import net.rai.scp_unleashed.config.ScpConfigs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.seasonal.network.SeasonsPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SeasonSync {
    public static final int CheckInterval = 1200;

    public interface Outbox {
        void toPlayer(ServerPlayer Receiver, SeasonsPayload Payload);

        void toAll(MinecraftServer Server, SeasonsPayload Payload);
    }

    public static final Outbox Network = new Outbox() {
        @Override
        public void toPlayer(ServerPlayer Receiver, SeasonsPayload Payload) {
            PacketDistributor.sendToPlayer(Receiver, Payload);
        }

        @Override
        public void toAll(MinecraftServer Server, SeasonsPayload Payload) {
            PacketDistributor.sendToAllPlayers(Payload);
        }
    };

    private static Outbox Current = Network;
    private static volatile boolean ConfigChanged;

    private SeasonSync() {
    }

    public static void useOutbox(Outbox Replacement) {
        Current = Replacement;
    }

    public static void resetOutbox() {
        Current = Network;
    }

    public static boolean due(int TickCount) {
        return TickCount % CheckInterval == 0;
    }

    public static SeasonsPayload payload() {
        return new SeasonsPayload(Seasons.serverMask());
    }

    public static boolean update(MinecraftServer Server) {
        if (!Seasons.refresh()) {
            return false;
        }
        Current.toAll(Server, payload());
        return true;
    }

    public static void send(ServerPlayer Receiver) {
        if (Receiver.getServer() != null && update(Receiver.getServer())) {
            return;
        }
        Current.toPlayer(Receiver, payload());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToClient(SeasonsPayload.PayloadType, SeasonsPayload.PayloadCodec, (Payload, Context) -> Seasons.applyClient(Payload.Mask()));
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Joined) {
            send(Joined);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Moved) {
            send(Moved);
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent Event) {
        Seasons.refresh();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Seasons.clearSession();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        if (ConfigChanged || due(Server.getTickCount())) {
            ConfigChanged = false;
            update(Server);
        }
    }

    @SubscribeEvent
    public static void configReloaded(ModConfigEvent.Reloading Event) {
        if (Event.getConfig().getSpec() == ScpConfigs.Server) {
            ConfigChanged = true;
        }
    }
}
