package net.rai.scp_unleashed.scp217;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp217.network.Scp217StatePayload;

public final class Scp217Sync {
    private Scp217Sync() {
    }

    public static int stateOf(LivingEntity Subject) {
        if (!Scp217Clockwork.isInfected(Subject)) {
            return 0;
        }
        int Elapsed = Scp217Clockwork.elapsed(Subject);
        return Scp217StatePayload.encode(Scp217Clockwork.stageOf(Elapsed), Scp217Clockwork.isPerfect(Subject), Scp217Clockwork.isWinding(Elapsed));
    }

    public static void update(LivingEntity Subject) {
        if (Subject.level().isClientSide) {
            return;
        }
        Scp217Trace Trace = Subject.getData(Scp217Registry.Trace.get());
        int State = stateOf(Subject);
        if (Trace.Sent == State) {
            return;
        }
        Trace.Sent = State;
        broadcast(Subject, State);
    }

    public static void idle(ServerPlayer User) {
        Scp217Trace Trace = User.getExistingDataOrNull(Scp217Registry.Trace.get());
        if (Trace != null && Trace.Sent != 0) {
            update(User);
        }
    }

    public static void resend(LivingEntity Subject) {
        Scp217Trace Trace = Subject.getExistingDataOrNull(Scp217Registry.Trace.get());
        if (Trace != null) {
            Trace.Sent = -1;
        }
        if (Trace != null || Scp217Clockwork.isInfected(Subject)) {
            update(Subject);
        }
    }

    public static void sendTo(ServerPlayer Watcher, LivingEntity Subject) {
        if (Watcher.connection != null) {
            PacketDistributor.sendToPlayer(Watcher, new Scp217StatePayload(Subject.getId(), stateOf(Subject)));
        }
    }

    private static void broadcast(LivingEntity Subject, int State) {
        Scp217StatePayload Payload = new Scp217StatePayload(Subject.getId(), State);
        if (Subject instanceof ServerPlayer User) {
            if (User.connection != null) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, Payload);
            }
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(Subject, Payload);
        }
    }
}
