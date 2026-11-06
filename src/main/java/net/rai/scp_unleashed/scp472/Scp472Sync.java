package net.rai.scp_unleashed.scp472;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp472.network.Scp472MindPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp472Sync {
    public static final float Step = 0.5f;
    public static final int RefreshTicks = 100;
    private static final Map<UUID, Sent> Last = new HashMap<>();

    private Scp472Sync() {
    }

    private record Sent(float Sanity, long At) {
    }

    public static void mind(ServerPlayer User, boolean Force) {
        if (User.connection == null) {
            return;
        }
        float Sanity = Scp472Mind.sanity(User);
        long Now = User.serverLevel().getGameTime();
        Sent Previous = Last.get(User.getUUID());
        if (!Force && Previous != null && Math.abs(Previous.Sanity() - Sanity) < Step && Scp472Mind.stage(Previous.Sanity()) == Scp472Mind.stage(Sanity)
                && (Sanity >= Scp472Mind.Full || Now - Previous.At() < RefreshTicks)) {
            return;
        }
        if (!Force && Previous == null && Sanity >= Scp472Mind.Full) {
            Last.put(User.getUUID(), new Sent(Sanity, Now));
            return;
        }
        PacketDistributor.sendToPlayer(User, new Scp472MindPayload(Sanity));
        Last.put(User.getUUID(), new Sent(Sanity, Now));
    }

    public static void forget(ServerPlayer User) {
        Last.remove(User.getUUID());
    }

    public static void forgetAll() {
        Last.clear();
    }
}
