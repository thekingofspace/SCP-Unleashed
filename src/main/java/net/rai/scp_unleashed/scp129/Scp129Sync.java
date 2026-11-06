package net.rai.scp_unleashed.scp129;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp129.network.Scp129EpisodePayload;
import net.rai.scp_unleashed.scp129.network.Scp129StatePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp129Sync {
    private static final Map<UUID, Integer> Sent = new HashMap<>();

    private Scp129Sync() {
    }

    public static void check(ServerPlayer User, int Stage) {
        Integer Last = Sent.get(User.getUUID());
        if (Last == null || Last != Stage) {
            send(User, Stage);
        }
    }

    public static void send(ServerPlayer User) {
        send(User, Scp129Course.stageOf(Scp129Infection.stateOf(User).Course()));
    }

    public static void send(ServerPlayer User, int Stage) {
        if (User.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(User, new Scp129StatePayload(Stage));
        Sent.put(User.getUUID(), Stage);
    }

    public static void episode(ServerPlayer User, int Kind, int Ticks, float Strength) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp129EpisodePayload(Kind, Ticks, Strength));
        }
    }

    public static void forget(ServerPlayer User) {
        Sent.remove(User.getUUID());
    }

    public static void forgetAll() {
        Sent.clear();
    }
}
