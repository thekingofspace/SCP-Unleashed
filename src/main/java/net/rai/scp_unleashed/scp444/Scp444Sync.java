package net.rai.scp_unleashed.scp444;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.scp444.network.Scp444StatePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp444Sync {
    public static final String Reason = "scp444";
    private static final Map<UUID, Boolean> Sent = new HashMap<>();

    private Scp444Sync() {
    }

    public static void update(ServerPlayer User) {
        boolean Silenced = Scp444Silence.isSilenced(User);
        Boolean Previous = Sent.get(User.getUUID());
        if (Previous != null && Previous == Silenced) {
            return;
        }
        Sent.put(User.getUUID(), Silenced);
        VoiceGate.mark(Reason, User.getUUID(), Silenced);
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp444StatePayload(Silenced));
        }
    }

    public static void resend(ServerPlayer User) {
        Sent.remove(User.getUUID());
        update(User);
    }

    public static void forget(UUID Id) {
        Sent.remove(Id);
        VoiceGate.mark(Reason, Id, false);
    }

    public static void clear() {
        for (UUID Id : Sent.keySet()) {
            VoiceGate.mark(Reason, Id, false);
        }
        Sent.clear();
    }
}
