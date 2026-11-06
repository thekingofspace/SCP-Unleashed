package net.rai.scp_unleashed.scp372;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.scp372.network.Scp372GlimpsePayload;
import net.rai.scp_unleashed.scp372.network.Scp372HushPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp372Glimpses {
    public static final int CheckInterval = 20;
    public static final int WaitMin = 1200;
    public static final int WaitMax = 4800;
    public static final int DimLight = 8;
    public static final int DarkLight = 4;
    public static final float DimRate = 1.4f;
    public static final float DarkRate = 1.8f;
    public static final int ResumeDelay = 400;
    public static final int ResumeMin = 400;
    public static final int ResumeMax = 1200;
    private static final Map<UUID, Integer> Remaining = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> Sent = new ConcurrentHashMap<>();

    private Scp372Glimpses() {
    }

    public static int roll(RandomSource Random) {
        return WaitMin + Random.nextInt(WaitMax - WaitMin + 1);
    }

    public static boolean blocked(MinecraftServer Server, long Now) {
        Scp372State State = Scp372State.get(Server);
        return State.isHeld() || Now < State.resumeAt();
    }

    public static boolean eligible(ServerPlayer User) {
        if (!User.isAlive() || User.isSpectator() || User.isSleeping() || User.isRemoved() || !Targeting.canWitness(User) || !AnomalyRealm.allows(User.level())) {
            return false;
        }
        if (!ReaderTargets.isSettled(User) || !Scp372Unlock.isUnlocked(User)) {
            return false;
        }
        return !Scp372Unlock.hasCase(User);
    }

    public static int light(ServerPlayer User) {
        BlockPos Eye = BlockPos.containing(User.getEyePosition());
        return User.level().getMaxLocalRawBrightness(Eye);
    }

    public static float rate(ServerPlayer User) {
        int Level = light(User);
        if (Level <= DarkLight) {
            return DarkRate;
        }
        return Level <= DimLight ? DimRate : 1.0f;
    }

    public static void tick(MinecraftServer Server) {
        if (Server.getTickCount() % CheckInterval != 0) {
            return;
        }
        sweep(Server, Server.overworld().getGameTime(), CheckInterval);
    }

    public static int sweep(MinecraftServer Server, long Now, int Elapsed) {
        if (blocked(Server, Now)) {
            return 0;
        }
        int Count = 0;
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            if (process(User, Elapsed)) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean offer(ServerPlayer User, long Now, int Elapsed) {
        return !blocked(User.server, Now) && process(User, Elapsed);
    }

    public static boolean process(ServerPlayer User, int Elapsed) {
        if (!eligible(User)) {
            return false;
        }
        UUID Id = User.getUUID();
        int Left = Remaining.computeIfAbsent(Id, Key -> roll(User.getRandom())) - Math.round(Elapsed * rate(User));
        if (Left > 0) {
            Remaining.put(Id, Left);
            return false;
        }
        send(User);
        return true;
    }

    public static void send(ServerPlayer User) {
        Remaining.put(User.getUUID(), roll(User.getRandom()));
        Sent.merge(User.getUUID(), 1, Integer::sum);
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp372GlimpsePayload(User.getRandom().nextInt(), light(User) <= DimLight));
        }
    }

    public static boolean force(ServerPlayer User) {
        if (blocked(User.server, User.server.overworld().getGameTime())) {
            return false;
        }
        send(User);
        return true;
    }

    public static void hurry(ServerPlayer User) {
        Remaining.put(User.getUUID(), 0);
    }

    public static void resumeAll(MinecraftServer Server) {
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Remaining.put(User.getUUID(), ResumeMin + User.getRandom().nextInt(ResumeMax - ResumeMin + 1));
        }
    }

    public static void hushAll(MinecraftServer Server, boolean Held) {
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            if (User.connection != null) {
                PacketDistributor.sendToPlayer(User, new Scp372HushPayload(Held));
            }
        }
    }

    public static int sentTo(UUID Player) {
        return Sent.getOrDefault(Player, 0);
    }

    public static int remaining(UUID Player) {
        return Remaining.getOrDefault(Player, -1);
    }

    public static void forget(UUID Player) {
        Remaining.remove(Player);
    }

    public static void clear() {
        Remaining.clear();
        Sent.clear();
    }
}
