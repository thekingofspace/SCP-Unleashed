package net.rai.scp_unleashed.scp409;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp409.network.Scp409StatePayload;

public final class Scp409Sync {
    private Scp409Sync() {
    }

    public static int growthOf(LivingEntity Subject) {
        if (!Scp409Infection.isInfected(Subject)) {
            return 0;
        }
        return Math.round(Scp409Infection.growth(Subject, Scp409Infection.elapsed(Subject)) * Scp409Infection.GrowthSteps);
    }

    public static Scp409StatePayload stateOf(LivingEntity Subject) {
        return new Scp409StatePayload(Subject.getId(), Scp409Infection.stageOf(Subject), growthOf(Subject));
    }

    public static void update(LivingEntity Subject) {
        if (Subject.level().isClientSide) {
            return;
        }
        Scp409Trace Trace = Subject.getData(Scp409Registry.Trace.get());
        Scp409StatePayload Payload = stateOf(Subject);
        if (Trace.SentStage == Payload.Stage() && Trace.SentGrowth == Payload.Growth()) {
            return;
        }
        Trace.SentStage = Payload.Stage();
        Trace.SentGrowth = Payload.Growth();
        broadcast(Subject, Payload);
    }

    public static void idle(ServerPlayer User) {
        Scp409Trace Trace = User.getExistingDataOrNull(Scp409Registry.Trace.get());
        if (Trace != null && Trace.SentStage > 0) {
            update(User);
        }
    }

    public static void resend(LivingEntity Subject) {
        Scp409Trace Trace = Subject.getExistingDataOrNull(Scp409Registry.Trace.get());
        if (Trace != null) {
            Trace.SentStage = -1;
            Trace.SentGrowth = -1;
        }
        if (Trace != null || Scp409Infection.isInfected(Subject)) {
            update(Subject);
        }
    }

    public static void sendTo(ServerPlayer Watcher, LivingEntity Subject) {
        if (Watcher.connection != null) {
            PacketDistributor.sendToPlayer(Watcher, stateOf(Subject));
        }
    }

    public static void shattered(LivingEntity Subject) {
        if (!Subject.level().isClientSide) {
            broadcast(Subject, new Scp409StatePayload(Subject.getId(), Scp409StatePayload.Shattered, 0));
            Scp409Trace Trace = Subject.getData(Scp409Registry.Trace.get());
            Trace.SentStage = 0;
            Trace.SentGrowth = 0;
        }
    }

    private static void broadcast(LivingEntity Subject, Scp409StatePayload Payload) {
        if (Subject instanceof ServerPlayer User) {
            if (User.connection != null) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, Payload);
            }
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(Subject, Payload);
        }
    }
}
