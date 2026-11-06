package net.rai.scp_unleashed.scp143;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp143.network.Scp143CutPayload;
import net.rai.scp_unleashed.scp143.network.Scp143PetalPayload;
import net.rai.scp_unleashed.scp268.Scp268;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp143Petals {
    public static final int MaxLive = 192;
    public static final int HardCap = 256;
    public static final double ShedChance = 0.32;
    public static final double PersistentFactor = 0.25;
    public static final double TargetRate = 0.025;
    public static final int TargetEvery = 10;
    public static final int ScanRange = 32;
    public static final int ViewRange = 48;
    public static final double SendRange = 96.0;
    public static final int Headroom = 14;
    public static final int AimSpread = 3;
    public static final double HitMargin = 0.12;
    public static final double DriftKick = 0.015;
    public static final double WindLead = 1.0;
    public static final int ChopRadius = 5;
    public static final int ChopHeight = 12;
    public static final long BlossomMemory = 2400L;
    private static final Map<ResourceKey<Level>, Flock> Flocks = new HashMap<>();

    private Scp143Petals() {
    }

    private static final class Flock {
        private final List<Live> Petals = new ArrayList<>();
        private final List<Scp143PetalPath.Spawn> Outgoing = new ArrayList<>();
        private final Map<Long, Long> Blossoms = new HashMap<>();
        private int NextId = 1;
    }

    private static final class Live {
        private final Scp143PetalPath.Spawn Petal;
        private final double[] Point = new double[3];
        private double Px;
        private double Py;
        private double Pz;

        private Live(Scp143PetalPath.Spawn Petal) {
            this.Petal = Petal;
            this.Px = Petal.X();
            this.Py = Petal.Y();
            this.Pz = Petal.Z();
            this.Point[0] = Petal.X();
            this.Point[1] = Petal.Y();
            this.Point[2] = Petal.Z();
        }
    }

    private static Flock flock(ServerLevel World) {
        return Flocks.computeIfAbsent(World.dimension(), Key -> new Flock());
    }

    public static void clear() {
        Flocks.clear();
    }

    public static int live(ServerLevel World) {
        Flock Found = Flocks.get(World.dimension());
        return Found == null ? 0 : Found.Petals.size();
    }

    public static boolean hittable(LivingEntity Target) {
        if (!Target.isAlive() || Target.isSpectator() || Target.isInvulnerable() || Target instanceof ArmorStand) {
            return false;
        }
        return !(Target instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    public static boolean emit(ServerLevel World, double X, double Y, double Z, RandomSource Random, boolean Burst) {
        Flock Found = flock(World);
        if (Found.Petals.size() >= (Burst ? HardCap : MaxLive)) {
            return false;
        }
        long Now = World.getGameTime();
        double[] Wind = Scp143Breeze.wind(World, BlockPos.containing(X, Y, Z), Now);
        float DriftX = (float) ((Random.nextDouble() - 0.5) * 2.0 * DriftKick + Wind[0] * 0.3);
        float DriftZ = (float) ((Random.nextDouble() - 0.5) * 2.0 * DriftKick + Wind[1] * 0.3);
        int Id = Found.NextId;
        Found.NextId = Found.NextId >= Integer.MAX_VALUE - 1 ? 1 : Found.NextId + 1;
        Scp143PetalPath.Spawn Petal = new Scp143PetalPath.Spawn(Id, X, Y, Z, DriftX, DriftZ, (float) Wind[0], (float) Wind[1], Random.nextInt(), Now);
        Found.Petals.add(new Live(Petal));
        Found.Outgoing.add(Petal);
        return true;
    }

    private static boolean isBlossom(BlockState State) {
        return State.getBlock() instanceof Scp143LeavesBlock;
    }

    private static boolean persistent(BlockState State) {
        return State.hasProperty(LeavesBlock.PERSISTENT) && State.getValue(LeavesBlock.PERSISTENT);
    }

    private static boolean canFall(ServerLevel World, BlockPos Leaf) {
        return !Scp143PetalPath.landed(World, Leaf.getX() + 0.5, Leaf.getY() - 0.05, Leaf.getZ() + 0.5);
    }

    private static void release(ServerLevel World, BlockPos Leaf, RandomSource Random, boolean Burst) {
        emit(World, Leaf.getX() + 0.15 + Random.nextDouble() * 0.7, Leaf.getY() - 0.02, Leaf.getZ() + 0.15 + Random.nextDouble() * 0.7, Random, Burst);
    }

    private static int roll(double Expected, RandomSource Random) {
        int Whole = (int) Math.floor(Expected);
        return Whole + (Random.nextDouble() < Expected - Whole ? 1 : 0);
    }

    public static void shed(ServerLevel World, BlockPos Pos, BlockState State, RandomSource Random) {
        flock(World).Blossoms.put(ChunkPos.asLong(Pos), World.getGameTime());
        if (!World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, ViewRange)) {
            return;
        }
        double Expected = ShedChance * Scp143Breeze.shedFactor(World, Pos, World.getGameTime()) * (persistent(State) ? PersistentFactor : 1.0);
        int Count = roll(Expected, Random);
        if (Count <= 0 || !canFall(World, Pos)) {
            return;
        }
        for (int Index = 0; Index < Count; Index++) {
            release(World, Pos, Random, false);
        }
        if (Random.nextInt(14) == 0) {
            World.playSound(null, Pos, Scp143Registry.Chime.get(), SoundSource.BLOCKS, 0.25f, 0.9f + Random.nextFloat() * 0.4f);
        }
    }

    public static void shake(ServerLevel World, BlockPos Centre, int Count, RandomSource Random) {
        List<BlockPos> Found = leavesNear(World, Centre, 2, -1, 4);
        scatter(World, Found, Count, Random);
        if (!Found.isEmpty()) {
            World.playSound(null, Centre, Scp143Registry.Rustle.get(), SoundSource.BLOCKS, 0.7f, 0.95f + Random.nextFloat() * 0.2f);
        }
    }

    public static void knock(ServerLevel World, BlockPos Log, RandomSource Random) {
        List<BlockPos> Found = leavesNear(World, Log, 4, 1, ChopHeight);
        scatter(World, Found, 2 + Random.nextInt(2), Random);
    }

    public static void chopped(ServerLevel World, BlockPos Log, RandomSource Random) {
        List<BlockPos> Found = leavesNear(World, Log, ChopRadius, 0, ChopHeight);
        scatter(World, Found, 10 + Random.nextInt(7), Random);
        if (!Found.isEmpty()) {
            World.playSound(null, Log, Scp143Registry.Rustle.get(), SoundSource.BLOCKS, 1.0f, 0.85f + Random.nextFloat() * 0.2f);
            World.playSound(null, Log, Scp143Registry.Chime.get(), SoundSource.BLOCKS, 0.5f, 0.8f + Random.nextFloat() * 0.3f);
        }
    }

    private static void scatter(ServerLevel World, List<BlockPos> Found, int Count, RandomSource Random) {
        if (Found.isEmpty()) {
            return;
        }
        for (int Index = 0; Index < Count; Index++) {
            release(World, Found.get(Random.nextInt(Found.size())), Random, true);
        }
    }

    private static List<BlockPos> leavesNear(ServerLevel World, BlockPos Centre, int Radius, int Below, int Above) {
        List<BlockPos> Found = new ArrayList<>();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                if (Dx * Dx + Dz * Dz > Radius * Radius + 1) {
                    continue;
                }
                for (int Dy = -Below; Dy <= Above; Dy++) {
                    Cursor.set(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz);
                    if (!World.isLoaded(Cursor)) {
                        continue;
                    }
                    if (isBlossom(World.getBlockState(Cursor)) && canFall(World, Cursor)) {
                        Found.add(Cursor.immutable());
                    }
                }
            }
        }
        return Found;
    }

    public static void tick(ServerLevel World) {
        long Now = World.getGameTime();
        if (Now % TargetEvery == 0) {
            target(World, Now);
        }
        Flock Found = Flocks.get(World.dimension());
        if (Found == null) {
            return;
        }
        if (!Found.Petals.isEmpty()) {
            advance(World, Found, Now);
        }
        flush(World, Found);
    }

    private static boolean blossomsNear(Flock Found, ChunkPos Here, long Now) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                Long Seen = Found.Blossoms.get(ChunkPos.asLong(Here.x + Dx, Here.z + Dz));
                if (Seen != null && Now - Seen <= BlossomMemory) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void target(ServerLevel World, long Now) {
        List<ServerPlayer> Watchers = World.players();
        Flock Found = Flocks.get(World.dimension());
        if (Watchers.isEmpty() || Found == null || Found.Blossoms.isEmpty()) {
            return;
        }
        if (Now % (TargetEvery * 60) == 0) {
            Found.Blossoms.values().removeIf(Seen -> Now - Seen > BlossomMemory);
        }
        Set<Integer> Seen = new HashSet<>();
        RandomSource Random = World.getRandom();
        for (ServerPlayer Watcher : Watchers) {
            if (Watcher.isSpectator()) {
                continue;
            }
            AABB Box = Watcher.getBoundingBox().inflate(ScanRange, 16.0, ScanRange);
            for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Box, Candidate -> hittable(Candidate) && !Scp268.isNeglected(Candidate) && Targeting.isTargetable(Candidate))) {
                if (!Seen.add(Target.getId()) || !blossomsNear(Found, Target.chunkPosition(), Now)) {
                    continue;
                }
                aim(World, Target, Now, Random);
            }
        }
    }

    private static void aim(ServerLevel World, LivingEntity Target, long Now, RandomSource Random) {
        int Bx = Target.getBlockX();
        int Bz = Target.getBlockZ();
        int Start = (int) Math.floor(Target.getY() + Target.getBbHeight()) + 1;
        List<BlockPos> Candidates = new ArrayList<>();
        boolean Natural = false;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -AimSpread; Dx <= AimSpread; Dx++) {
            for (int Dz = -AimSpread; Dz <= AimSpread; Dz++) {
                for (int Y = Start; Y <= Start + Headroom; Y++) {
                    Cursor.set(Bx + Dx, Y, Bz + Dz);
                    if (!World.isLoaded(Cursor)) {
                        break;
                    }
                    BlockState State = World.getBlockState(Cursor);
                    if (isBlossom(State)) {
                        Candidates.add(Cursor.immutable());
                        Natural |= !persistent(State);
                        break;
                    }
                    if (Scp143PetalPath.landed(World, Cursor.getX() + 0.5, Cursor.getY() + 0.5, Cursor.getZ() + 0.5)) {
                        break;
                    }
                }
            }
        }
        if (Candidates.isEmpty()) {
            return;
        }
        BlockPos Here = Target.blockPosition();
        double Expected = TargetRate * TargetEvery * Scp143Breeze.shedFactor(World, Here, Now) * (Natural ? 1.0 : PersistentFactor);
        int Count = roll(Expected, Random);
        if (Count <= 0) {
            return;
        }
        double[] Wind = Scp143Breeze.wind(World, Here, Now);
        double Height = Target.getY() + Target.getBbHeight() * 0.6;
        for (int Index = 0; Index < Count; Index++) {
            BlockPos Best = null;
            double BestScore = Double.MAX_VALUE;
            for (BlockPos Leaf : Candidates) {
                double Fall = Math.max(0.0, Leaf.getY() - Height) / Scp143PetalPath.MeanFall + Scp143PetalPath.FallTime;
                double LandX = Leaf.getX() + 0.5 + Wind[0] * Math.max(0.0, Fall - Scp143PetalPath.WindTime) * WindLead;
                double LandZ = Leaf.getZ() + 0.5 + Wind[1] * Math.max(0.0, Fall - Scp143PetalPath.WindTime) * WindLead;
                double Score = Math.hypot(LandX - Target.getX(), LandZ - Target.getZ()) + Random.nextDouble() * 1.6;
                if (Score < BestScore) {
                    BestScore = Score;
                    Best = Leaf;
                }
            }
            release(World, Best, Random, false);
        }
        if (Random.nextInt(9) == 0) {
            World.playSound(null, Candidates.get(0), Scp143Registry.Rustle.get(), SoundSource.BLOCKS, 0.35f, 1.0f + Random.nextFloat() * 0.2f);
        }
    }

    private static void advance(ServerLevel World, Flock Found, long Now) {
        Map<Long, List<Live>> Sections = new HashMap<>();
        Iterator<Live> Each = Found.Petals.iterator();
        while (Each.hasNext()) {
            Live Petal = Each.next();
            double Age = Now - Petal.Petal.Born();
            if (Age > Scp143PetalPath.Life) {
                Each.remove();
                continue;
            }
            Petal.Px = Petal.Point[0];
            Petal.Py = Petal.Point[1];
            Petal.Pz = Petal.Point[2];
            Scp143PetalPath.position(Petal.Petal, Age, Petal.Point);
            if (Age <= 0.0) {
                Petal.Px = Petal.Point[0];
                Petal.Py = Petal.Point[1];
                Petal.Pz = Petal.Point[2];
            }
            BlockPos Cell = BlockPos.containing(Petal.Point[0], Petal.Point[1], Petal.Point[2]);
            if (!World.isLoaded(Cell) || Scp143PetalPath.landed(World, Petal.Point[0], Petal.Point[1], Petal.Point[2])) {
                Each.remove();
                continue;
            }
            Sections.computeIfAbsent(SectionPos.asLong(Cell), Key -> new ArrayList<>()).add(Petal);
        }
        for (Map.Entry<Long, List<Live>> Group : Sections.entrySet()) {
            SectionPos Section = SectionPos.of(Group.getKey());
            AABB Area = new AABB(Section.minBlockX(), Section.minBlockY(), Section.minBlockZ(), Section.maxBlockX() + 1, Section.maxBlockY() + 1, Section.maxBlockZ() + 1)
                    .inflate(1.5);
            List<LivingEntity> Targets = World.getEntitiesOfClass(LivingEntity.class, Area, Scp143Petals::hittable);
            if (Targets.isEmpty()) {
                continue;
            }
            for (Live Petal : Group.getValue()) {
                Vec3 From = new Vec3(Petal.Px, Petal.Py, Petal.Pz);
                Vec3 To = new Vec3(Petal.Point[0], Petal.Point[1], Petal.Point[2]);
                for (LivingEntity Target : Targets) {
                    AABB Box = Target.getBoundingBox().inflate(HitMargin);
                    if (Box.contains(To) || Box.clip(From, To).isPresent()) {
                        Scp143Bleeding.cut(Target, To);
                        Found.Petals.remove(Petal);
                        PacketDistributor.sendToPlayersNear(World, null, To.x, To.y, To.z, SendRange, new Scp143CutPayload(Petal.Petal.Id(), (float) To.x, (float) To.y, (float) To.z));
                        break;
                    }
                }
            }
        }
    }

    private static void flush(ServerLevel World, Flock Found) {
        if (Found.Outgoing.isEmpty()) {
            return;
        }
        Map<Long, List<Scp143PetalPath.Spawn>> Groups = new HashMap<>();
        for (Scp143PetalPath.Spawn Petal : Found.Outgoing) {
            long Key = ((long) ((int) Math.floor(Petal.X()) >> 5) << 32) ^ ((int) Math.floor(Petal.Z()) >> 5) & 0xFFFFFFFFL;
            Groups.computeIfAbsent(Key, Unused -> new ArrayList<>()).add(Petal);
        }
        Found.Outgoing.clear();
        for (List<Scp143PetalPath.Spawn> Group : Groups.values()) {
            for (int Start = 0; Start < Group.size(); Start += Scp143PetalPayload.MaxPetals) {
                List<Scp143PetalPath.Spawn> Slice = List.copyOf(Group.subList(Start, Math.min(Group.size(), Start + Scp143PetalPayload.MaxPetals)));
                Scp143PetalPath.Spawn First = Slice.get(0);
                PacketDistributor.sendToPlayersNear(World, null, First.X(), First.Y(), First.Z(), SendRange, new Scp143PetalPayload(Slice));
            }
        }
    }

    public static void resend(ServerPlayer Watcher) {
        Flock Found = Flocks.get(Watcher.serverLevel().dimension());
        if (Found == null || Found.Petals.isEmpty()) {
            return;
        }
        List<Scp143PetalPath.Spawn> Near = new ArrayList<>();
        for (Live Petal : Found.Petals) {
            if (Watcher.distanceToSqr(Petal.Petal.X(), Petal.Petal.Y(), Petal.Petal.Z()) <= SendRange * SendRange && Near.size() < Scp143PetalPayload.MaxPetals) {
                Near.add(Petal.Petal);
            }
        }
        if (!Near.isEmpty()) {
            PacketDistributor.sendToPlayer(Watcher, new Scp143PetalPayload(Near));
        }
    }
}
