package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp610.network.Scp610StatePayload;

public final class Scp610Sync {
    private Scp610Sync() {
    }

    public static void update(LivingEntity Subject) {
        if (Subject.level().isClientSide || !(Subject instanceof ServerPlayer User)) {
            return;
        }
        Scp610Trace Trace = User.getData(Scp610Registry.Trace.get());
        int Stage = Scp610Infection.stageOf(User);
        if (Trace.Sent == Stage) {
            return;
        }
        Trace.Sent = Stage;
        if (User.connection != null) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Scp610StatePayload(User.getId(), Stage));
        }
    }

    public static void resend(LivingEntity Subject) {
        if (Subject instanceof ServerPlayer User) {
            User.getData(Scp610Registry.Trace.get()).Sent = -1;
            update(User);
        }
    }

    public static void sendTo(ServerPlayer Watcher, ServerPlayer Subject) {
        if (Watcher.connection != null) {
            PacketDistributor.sendToPlayer(Watcher, new Scp610StatePayload(Subject.getId(), Scp610Infection.stageOf(Subject)));
        }
    }
}
