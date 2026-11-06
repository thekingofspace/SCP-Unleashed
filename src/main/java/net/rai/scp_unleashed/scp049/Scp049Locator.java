package net.rai.scp_unleashed.scp049;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Scp049Locator {
    public static final int RegionChunks = 34;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<UUID, CompletableFuture<Optional<BlockPos>>> Pending = new ConcurrentHashMap<>();
    private static ExecutorService Worker;

    private Scp049Locator() {
    }

    private static synchronized ExecutorService worker() {
        if (Worker == null || Worker.isShutdown()) {
            Worker = Executors.newSingleThreadExecutor(Task -> {
                Thread Runner = new Thread(Task, "SCP-049 village locator");
                Runner.setDaemon(true);
                return Runner;
            });
        }
        return Worker;
    }

    public static int ringsFor(int MaxDistance) {
        return Math.max(1, (int) Math.ceil(MaxDistance / (RegionChunks * 16.0)) + 1);
    }

    public static void start(ServerLevel World, UUID CaseId, BlockPos Origin, int MaxDistance) {
        if (Pending.containsKey(CaseId)) {
            return;
        }
        int Rings = ringsFor(MaxDistance);
        CompletableFuture<Optional<BlockPos>> Task = CompletableFuture.supplyAsync(() -> {
            try {
                BlockPos Pos = World.findNearestMapStructure(StructureTags.VILLAGE, Origin, Rings, false);
                if (Pos == null) {
                    return Optional.<BlockPos>empty();
                }
                double Dx = Pos.getX() - Origin.getX();
                double Dz = Pos.getZ() - Origin.getZ();
                return Dx * Dx + Dz * Dz <= (double) MaxDistance * MaxDistance ? Optional.of(Pos) : Optional.<BlockPos>empty();
            } catch (RuntimeException Problem) {
                Log.warn("SCP-049 could not search for a village", Problem);
                return Optional.<BlockPos>empty();
            }
        }, worker());
        Pending.put(CaseId, Task);
    }

    public static boolean isRunning(UUID CaseId) {
        return Pending.containsKey(CaseId);
    }

    public static Optional<Optional<BlockPos>> poll(UUID CaseId) {
        CompletableFuture<Optional<BlockPos>> Task = Pending.get(CaseId);
        if (Task == null || !Task.isDone()) {
            return Optional.empty();
        }
        Pending.remove(CaseId);
        return Optional.of(Task.getNow(Optional.empty()));
    }

    public static void clear() {
        for (CompletableFuture<Optional<BlockPos>> Task : Pending.values()) {
            Task.cancel(true);
        }
        Pending.clear();
    }
}
