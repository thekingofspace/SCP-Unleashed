package net.rai.scp_unleashed.scp323;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp323.network.Scp323BondPayload;
import net.rai.scp_unleashed.scp323.network.Scp323WatchPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp323Sync {
    public static final int ResyncInterval = 5;
    private static final Map<UUID, Integer> SentStages = new HashMap<>();

    private Scp323Sync() {
    }

    public static Scp323BondPayload payloadFor(Entity Target) {
        return new Scp323BondPayload(Target.getId(), Scp323Bond.of(Target).Stage());
    }

    public static void bond(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel)) {
            return;
        }
        int Stage = Scp323Bond.of(Target).Stage();
        if (Target instanceof ServerPlayer User) {
            SentStages.put(User.getUUID(), Stage);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(Target, new Scp323BondPayload(Target.getId(), Stage));
    }

    public static void tell(ServerPlayer Viewer, Entity Target) {
        if (Viewer.connection != null && Scp323Bond.isBound(Target)) {
            PacketDistributor.sendToPlayer(Viewer, payloadFor(Target));
        }
    }

    public static void refresh(ServerPlayer User) {
        int Stage = Scp323Bond.of(User).Stage();
        Integer Sent = SentStages.get(User.getUUID());
        if (Sent == null && Stage == 0 || Sent != null && Sent == Stage) {
            return;
        }
        bond(User);
    }

    public static void resend(ServerPlayer User) {
        SentStages.remove(User.getUUID());
        if (User.connection != null) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, payloadFor(User));
            SentStages.put(User.getUUID(), Scp323Bond.of(User).Stage());
        }
    }

    public static void watch(ServerPlayer User, int CreatureId) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp323WatchPayload(CreatureId));
        }
    }

    public static void forget(UUID Id) {
        SentStages.remove(Id);
    }

    public static void clear() {
        SentStages.clear();
    }
}
