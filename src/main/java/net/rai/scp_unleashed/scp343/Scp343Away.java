package net.rai.scp_unleashed.scp343;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class Scp343Away {
    public static final long ShortestAway = 2400L;
    public static final long LongestAway = 7200L;
    public static final long Retry = 600L;
    public static final int Interval = 20;
    public static final int NearMin = 10;
    public static final int NearMax = 28;
    public static final double FarFrom = 48.0;
    public static final int ThroneMin = 2;
    public static final int ThroneMax = 7;
    public static final int OpenTries = 4;
    public static final long ThronePatience = 12000L;
    private static final Logger Log = LogUtils.getLogger();

    private Scp343Away() {
    }

    public record Plan(ServerLevel World, Scp343Walls.Emergence Spot, @Nullable UUID Greet) {
    }

    public static void depart(Scp343God God) {
        if (!(God.level() instanceof ServerLevel World) || God.isRemoved()) {
            return;
        }
        Scp343Data Data = Scp343Data.get(World.getServer());
        BlockPos From = God.blockPosition();
        God.prepareAway();
        CompoundTag Body = new CompoundTag();
        God.saveWithoutId(Body);
        Body.putString("id", EntityType.getKey(God.getType()).toString());
        Body.remove("Passengers");
        long Now = World.getServer().overworld().getGameTime();
        long Delay = ShortestAway + World.getRandom().nextInt((int) (LongestAway - ShortestAway + 1));
        Data.putAway(new Scp343Data.Away(God.getUUID(), Body, World.dimension(), From, Now + Delay));
        God.discard();
    }

    public static void tick(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        if (Now % Interval != 0L) {
            return;
        }
        Scp343Data Data = Scp343Data.get(Server);
        for (Scp343Data.Away Record : Data.away()) {
            if (Now >= Record.ReturnAt()) {
                bringBack(Server, Data, Record, Now);
            }
        }
    }

    public static int returnAll(MinecraftServer Server) {
        Scp343Data Data = Scp343Data.get(Server);
        long Now = Server.overworld().getGameTime();
        int Count = 0;
        for (Scp343Data.Away Record : Data.away()) {
            if (bringBack(Server, Data, Record, Now)) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean bringBack(MinecraftServer Server, Scp343Data Data, Scp343Data.Away Record, long Now) {
        return bringBack(Server, Data, Record, Now, randomFor(Server, Record), Scp343Walls.Anywhere);
    }

    public static boolean bringBack(MinecraftServer Server, Scp343Data Data, Scp343Data.Away Record, long Now, RandomSource Random, Predicate<BlockPos> Allowed) {
        Optional<Plan> Found = plan(Server, Data, Record, Random, Allowed);
        if (Found.isEmpty()) {
            Data.putAway(Record.delayed(Now + Retry));
            return false;
        }
        return emerge(Server, Data, Record, Found.get());
    }

    private static RandomSource randomFor(MinecraftServer Server, Scp343Data.Away Record) {
        ServerLevel From = Server.getLevel(Record.From());
        return (From == null ? Server.overworld() : From).getRandom();
    }

    public static boolean emerge(MinecraftServer Server, Scp343Data Data, Scp343Data.Away Record, Plan Target) {
        ServerLevel World = Target.World();
        if (alreadyHere(Server, Record.Id())) {
            Data.returned(Record.Id());
            return true;
        }
        Entity Made = EntityType.create(Record.Body(), World).orElse(null);
        if (!(Made instanceof Scp343God God)) {
            Log.warn("SCP-343 record {} could not be recreated and was dropped", Record.Id());
            Data.returned(Record.Id());
            return false;
        }
        if (!Scp343Walls.usable(World, Target.Spot(), Scp343Walls.Anywhere)) {
            Data.putAway(Record.delayed(World.getServer().overworld().getGameTime() + Retry));
            return false;
        }
        God.prepareEmergence(Target.Spot());
        God.seek(Target.Greet());
        if (!World.addFreshEntity(God)) {
            Data.putAway(Record.delayed(World.getServer().overworld().getGameTime() + Retry));
            return false;
        }
        God.announceEmergence(World);
        Data.returned(Record.Id());
        return true;
    }

    private static boolean alreadyHere(MinecraftServer Server, UUID Id) {
        for (ServerLevel World : Server.getAllLevels()) {
            Entity Found = World.getEntity(Id);
            if (Found != null && !Found.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Plan> plan(MinecraftServer Server, Scp343Data Data, Scp343Data.Away Record) {
        return plan(Server, Data, Record, randomFor(Server, Record), Scp343Walls.Anywhere);
    }

    public static Optional<Plan> plan(MinecraftServer Server, Scp343Data Data, Scp343Data.Away Record, RandomSource Random, Predicate<BlockPos> Allowed) {
        ServerLevel From = Server.getLevel(Record.From());
        if (From == null) {
            From = Server.overworld();
        }
        List<BlockPos> Thrones = new ArrayList<>(Data.thrones(From.dimension()));
        Thrones.removeIf(Allowed.negate());
        if (!Thrones.isEmpty()) {
            Optional<Plan> Home = atThrone(From, Thrones, Record.FromPos(), Random, Allowed);
            long Late = Server.overworld().getGameTime() - Record.ReturnAt();
            if (Home.isPresent() || Late < ThronePatience) {
                return Home;
            }
        }
        List<ServerPlayer> Candidates = people(Server, From, Allowed);
        if (Candidates.isEmpty()) {
            return Optional.empty();
        }
        ServerPlayer Chosen = Candidates.get(Random.nextInt(Candidates.size()));
        ServerLevel Where = Chosen.serverLevel();
        Vec3 Avoid = Where == From ? Vec3.atBottomCenterOf(Record.FromPos()) : null;
        return Scp343Walls.open(Where, Chosen.blockPosition(), NearMin, NearMax, Random, Avoid, FarFrom, OpenTries, Allowed).map(Spot -> new Plan(Where, Spot, Chosen.getUUID()));
    }

    private static Optional<Plan> atThrone(ServerLevel World, List<BlockPos> Thrones, BlockPos From, RandomSource Random, Predicate<BlockPos> Allowed) {
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : Thrones) {
            if (!Scp343Walls.ticking(World, Pos) || !Scp343Thrones.valid(World, Pos)) {
                continue;
            }
            double Distance = Pos.distSqr(From);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Pos;
            }
        }
        if (Best == null) {
            return Optional.empty();
        }
        Optional<Scp343Walls.Emergence> Spot = Scp343Walls.near(World, Best, ThroneMin, ThroneMax, Random, true, null, 0.0, Allowed);
        if (Spot.isEmpty()) {
            Spot = Scp343Walls.around(World, Scp343Thrones.front(World, Best), Random, Allowed);
        }
        if (Spot.isEmpty()) {
            Spot = Scp343Walls.near(World, Best, 1, ThroneMax, Random, false, null, 0.0, Allowed);
        }
        return Spot.map(Found -> new Plan(World, Found, null));
    }

    public static List<ServerPlayer> people(MinecraftServer Server, ServerLevel Preferred) {
        return people(Server, Preferred, Scp343Walls.Anywhere);
    }

    public static List<ServerPlayer> people(MinecraftServer Server, ServerLevel Preferred, Predicate<BlockPos> Allowed) {
        List<ServerPlayer> Same = new ArrayList<>();
        List<ServerPlayer> Any = new ArrayList<>();
        for (ServerPlayer Candidate : Server.getPlayerList().getPlayers()) {
            if (Candidate instanceof FakePlayer || !ReaderTargets.isTargetable(Candidate) || !Targeting.isTargetable(Candidate) || !Allowed.test(Candidate.blockPosition())) {
                continue;
            }
            Any.add(Candidate);
            if (Candidate.serverLevel() == Preferred) {
                Same.add(Candidate);
            }
        }
        return Same.isEmpty() ? Any : Same;
    }
}
