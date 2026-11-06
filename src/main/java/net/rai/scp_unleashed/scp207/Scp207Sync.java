package net.rai.scp_unleashed.scp207;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp207.network.Scp207TollPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp207Sync {
    public static final float TollStep = 0.004f;
    public static final int HeartbeatTicks = 200;
    private static final Map<UUID, Sent> Last = new HashMap<>();

    private Scp207Sync() {
    }

    private record Sent(float Toll, float Rate, long At) {
    }

    public static void toll(ServerPlayer User, Scp207Toll.State Current, boolean Force) {
        if (User.connection == null) {
            return;
        }
        float Rate = Current.rate(Scp207Toll.isPaused(User));
        long Now = User.serverLevel().getGameTime();
        Sent Previous = Last.get(User.getUUID());
        if (!Force && Previous != null && Math.abs(Previous.Toll() - Current.Toll()) < TollStep && Math.abs(Previous.Rate() - Rate) < 1.0e-9f
                && Now - Previous.At() < HeartbeatTicks) {
            return;
        }
        PacketDistributor.sendToPlayer(User, new Scp207TollPayload(Current.Toll(), Rate));
        Last.put(User.getUUID(), new Sent(Current.Toll(), Rate, Now));
    }

    public static void toll(ServerPlayer User) {
        toll(User, Scp207Toll.stateOf(User), true);
    }

    public static void all(ServerPlayer User) {
        toll(User);
    }

    public static void forget(ServerPlayer User) {
        Last.remove(User.getUUID());
    }

    public static void forgetAll() {
        Last.clear();
    }
}
