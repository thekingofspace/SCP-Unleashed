package net.rai.scp_unleashed.scp427;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp427.network.Scp427ConvulsePayload;
import net.rai.scp_unleashed.scp427.network.Scp427StatePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp427Sync {
    private static final Map<UUID, Scp427StatePayload> Sent = new HashMap<>();

    private Scp427Sync() {
    }

    public static Scp427StatePayload stateOf(ServerPlayer User) {
        Scp427State State = Scp427Exposure.state(User);
        boolean Open = Scp427Exposure.isOpen(User);
        if (State == null) {
            return new Scp427StatePayload(Open, 0, 0);
        }
        int Stage = Scp427Exposure.isExempt(User) ? 0 : Scp427Exposure.stageOf(State.Ticks);
        return new Scp427StatePayload(Open, Stage, Math.min(State.Transform, Scp427Transformation.Length + 1));
    }

    public static void update(ServerPlayer User) {
        if (User.connection == null) {
            return;
        }
        Scp427StatePayload Payload = stateOf(User);
        Scp427StatePayload Before = Sent.get(User.getUUID());
        if (Payload.equals(Before) || Before == null && !Payload.Open() && Payload.Stage() == 0 && Payload.Transform() == 0) {
            return;
        }
        Sent.put(User.getUUID(), Payload);
        PacketDistributor.sendToPlayer(User, Payload);
    }

    public static void forget(ServerPlayer User) {
        Sent.remove(User.getUUID());
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp427StatePayload(false, 0, 0));
        }
    }

    public static void convulse(LivingEntity Subject, boolean Active) {
        broadcast(Subject, Active ? 0 : Scp427ConvulsePayload.Stopped);
    }

    public static void vanish(LivingEntity Subject) {
        broadcast(Subject, Scp427ConvulsePayload.Overcome);
    }

    private static void broadcast(LivingEntity Subject, int Elapsed) {
        if (Subject.level().isClientSide) {
            return;
        }
        Scp427ConvulsePayload Payload = new Scp427ConvulsePayload(Subject.getId(), Elapsed);
        if (Subject instanceof ServerPlayer User) {
            if (User.connection != null) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, Payload);
            }
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(Subject, Payload);
        }
    }

    public static void sendTo(ServerPlayer Watcher, LivingEntity Subject) {
        Scp427State State = Scp427Exposure.state(Subject);
        if (State != null && State.transforming() && Watcher.connection != null) {
            PacketDistributor.sendToPlayer(Watcher, new Scp427ConvulsePayload(Subject.getId(), State.Transform));
        }
    }
}
