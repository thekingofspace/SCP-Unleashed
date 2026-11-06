package net.rai.scp_unleashed.scp4666;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp4666.network.Scp4666PosePayload;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp4666Pose {
    public static final int ResendEvery = 20;
    private static final Map<MinecraftServer, Set<UUID>> Lying = new ConcurrentHashMap<>();

    private Scp4666Pose() {
    }

    public static void lie(ServerPlayer User, boolean Down) {
        User.setForcedPose(Down ? Pose.SLEEPING : null);
        User.setPose(Down ? Pose.SLEEPING : Pose.STANDING);
        User.refreshDimensions();
        Set<UUID> Held = Lying.computeIfAbsent(User.server, Key -> ConcurrentHashMap.newKeySet());
        if (Down) {
            Held.add(User.getUUID());
        } else {
            Held.remove(User.getUUID());
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Scp4666PosePayload(User.getId(), Down));
    }

    public static void forget(ServerPlayer User) {
        Set<UUID> Held = Lying.get(User.server);
        if (Held != null) {
            Held.remove(User.getUUID());
        }
    }

    public static boolean lying(ServerPlayer User) {
        Set<UUID> Held = Lying.get(User.server);
        return Held != null && Held.contains(User.getUUID()) && User.getPose() == Pose.SLEEPING;
    }

    public static void tick(MinecraftServer Server) {
        Set<UUID> Held = Lying.get(Server);
        if (Held == null || Held.isEmpty() || Server.getTickCount() % ResendEvery != 0) {
            return;
        }
        for (UUID Id : new HashSet<>(Held)) {
            ServerPlayer User = find(Server, Id);
            if (User == null || !User.isAlive()) {
                Held.remove(Id);
                continue;
            }
            if (User.getPose() != Pose.SLEEPING) {
                User.setPose(Pose.SLEEPING);
                User.refreshDimensions();
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Scp4666PosePayload(User.getId(), true));
        }
    }

    private static ServerPlayer find(MinecraftServer Server, UUID Id) {
        ServerPlayer Online = Server.getPlayerList().getPlayer(Id);
        if (Online != null) {
            return Online;
        }
        ServerLevel World = Scp4666World.level(Server);
        return World != null && World.getPlayerByUUID(Id) instanceof ServerPlayer Found ? Found : null;
    }

    public static void releaseAll(MinecraftServer Server) {
        Set<UUID> Held = Lying.remove(Server);
        if (Held == null) {
            return;
        }
        for (UUID Id : Held) {
            ServerPlayer User = find(Server, Id);
            if (User != null && User.isAlive()) {
                lie(User, false);
            }
        }
        Lying.remove(Server);
    }
}
