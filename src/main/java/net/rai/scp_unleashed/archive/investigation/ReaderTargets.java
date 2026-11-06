package net.rai.scp_unleashed.archive.investigation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class ReaderTargets {
    public static final int JoinGrace = 200;
    private static final Map<UUID, Long> JoinedAt = new ConcurrentHashMap<>();
    private static final List<Handoff> Listeners = new CopyOnWriteArrayList<>();

    private ReaderTargets() {
    }

    @FunctionalInterface
    public interface Handoff {
        void moved(MinecraftServer Server, UUID CaseId, UUID From, UUID To);
    }

    public static void onHandoff(Handoff Listener) {
        Listeners.add(Listener);
    }

    public static void moved(MinecraftServer Server, UUID CaseId, UUID From, UUID To) {
        for (Handoff Listener : Listeners) {
            Listener.moved(Server, CaseId, From, To);
        }
    }

    public static void joined(ServerPlayer Reader) {
        JoinedAt.put(Reader.getUUID(), Reader.server.overworld().getGameTime());
    }

    public static void left(UUID Reader) {
        JoinedAt.remove(Reader);
    }

    public static void clear() {
        JoinedAt.clear();
    }

    public static boolean isTargetable(ServerPlayer Candidate) {
        return Candidate.isAlive() && !Candidate.isRemoved() && !Candidate.hasDisconnected() && !Candidate.isSpectator();
    }

    public static boolean isSettled(ServerPlayer Reader) {
        Long Joined = JoinedAt.get(Reader.getUUID());
        return Joined == null || Reader.server.overworld().getGameTime() - Joined >= JoinGrace;
    }

    public static boolean isReady(ServerPlayer Reader) {
        return isTargetable(Reader) && isSettled(Reader);
    }

    public static boolean anyTargetable(MinecraftServer Server) {
        for (ServerPlayer Candidate : Server.getPlayerList().getPlayers()) {
            if (isTargetable(Candidate)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ServerPlayer online(MinecraftServer Server, @Nullable UUID Reader) {
        if (Reader == null) {
            return null;
        }
        ServerPlayer Found = Server.getPlayerList().getPlayer(Reader);
        return Found != null && isTargetable(Found) ? Found : null;
    }

    @Nullable
    public static ServerPlayer next(MinecraftServer Server, @Nullable UUID Excluded, ToIntFunction<UUID> Load, Predicate<ServerPlayer> Wanted) {
        List<ServerPlayer> Lightest = new ArrayList<>();
        int Least = Integer.MAX_VALUE;
        for (ServerPlayer Candidate : Server.getPlayerList().getPlayers()) {
            if (Candidate.getUUID().equals(Excluded) || !isTargetable(Candidate) || !Wanted.test(Candidate)) {
                continue;
            }
            int Busy = Load.applyAsInt(Candidate.getUUID());
            if (Busy < Least) {
                Least = Busy;
                Lightest.clear();
            }
            if (Busy == Least) {
                Lightest.add(Candidate);
            }
        }
        return Lightest.isEmpty() ? null : Lightest.get(Server.overworld().random.nextInt(Lightest.size()));
    }
}
