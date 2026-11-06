package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp860Director {
    public static final int FillTicks = 400;
    public static final int DrainTicks = 160;
    public static final double StillPace = 0.06;
    public static final int PaceWindow = 20;
    public static final double OffPath = 3.0;
    public static final double OnPath = 2.5;
    public static final int OffGrace = 20;
    public static final int OnSettle = 60;
    public static final double PackReach = 40.0;
    public static final int PackSize = 3;
    public static final float RustleAt = 0.35f;
    public static final float GrowlAt = 0.6f;
    public static final float NearAt = 0.85f;
    public static final float PatienceCalm = 0.35f;
    public static final float StrikeChance = 0.4f;
    public static final float JoinChance = 0.2f;
    public static final int MaxPatienceHunters = 2;
    public static final int SureStrike = 100;
    public static final int GraceTicks = 200;
    public static final int RampTicks = 300;
    public static final int PerPlayer = 4;
    public static final int GlobalCap = 12;
    public static final int SpawnEvery = 60;
    public static final double SpawnNear = 22.0;
    public static final double SpawnFar = 32.0;
    public static final double SpawnOffPath = 9.0;
    public static final double SpawnSide = 10.0;
    public static final double SpawnSideFar = 16.0;
    public static final int SpawnTries = 24;
    public static final double DespawnReach = 64.0;
    public static final int Windup = 24;
    public static final int RoarCooldown = 200;
    public static final float RustleVolume = 0.4f;
    public static final double RustleNear = 9.0;
    public static final double RustleFar = 14.0;
    public static final int RustleMin = 60;
    public static final int RustleMax = 100;
    public static final int GrowlMin = 80;
    public static final int GrowlMax = 120;
    public static final int RescueDepth = 8;
    private static final Map<MinecraftServer, Map<UUID, Mood>> Moods = new ConcurrentHashMap<>();

    public static final class Mood {
        public float Patience;
        public int OffTicks;
        public int OnTicks;
        public boolean Pack;
        public boolean Strike;
        public int Full;
        public int Inside;
        public int TellWait = RustleMin;
        public int GrowlWait = GrowlMin;
        public int SpawnWait = SpawnEvery;
        public int RoarWait;
        public final double[] Paces = new double[PaceWindow];
        public int Cursor;
        @Nullable
        public Vec3 Last;

        public double pace() {
            double Sum = 0.0;
            for (double Value : Paces) {
                Sum += Value;
            }
            return Sum / PaceWindow;
        }
    }

    private Scp860Director() {
    }

    public static Mood mood(ServerPlayer User) {
        return Moods.computeIfAbsent(User.server, Key -> new ConcurrentHashMap<>()).computeIfAbsent(User.getUUID(), Key -> new Mood());
    }

    @Nullable
    public static Mood peek(MinecraftServer Server, UUID Id) {
        Map<UUID, Mood> Found = Moods.get(Server);
        return Found == null ? null : Found.get(Id);
    }

    public static void joined(ServerPlayer User) {
        Map<UUID, Mood> Found = Moods.computeIfAbsent(User.server, Key -> new ConcurrentHashMap<>());
        Found.put(User.getUUID(), new Mood());
    }

    public static void left(ServerPlayer User) {
        Map<UUID, Mood> Found = Moods.get(User.server);
        if (Found != null) {
            Found.remove(User.getUUID());
        }
        ServerLevel World = Scp860Forest.level(User.server);
        if (World != null) {
            for (Scp860Watcher Watcher : watchers(World)) {
                if (User.getUUID().equals(Watcher.huntTarget())) {
                    Watcher.calm();
                }
            }
        }
    }

    public static void forget(MinecraftServer Server) {
        Moods.remove(Server);
    }

    public static float patience(ServerPlayer User) {
        Mood Found = peek(User.server, User.getUUID());
        return Found == null ? 0.0f : Found.Patience;
    }

    public static boolean offPath(ServerPlayer User) {
        return Scp860Forest.distance(User.server, User.getX(), User.getZ()) > OffPath;
    }

    public static List<Scp860Watcher> watchers(ServerLevel World) {
        return World.getEntitiesOfClass(Scp860Watcher.class, Scp860Builder.region(), Watcher -> Watcher.isAlive() && Watcher.forestBound());
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Scp860Forest.level(Server);
        if (World == null) {
            return;
        }
        List<ServerPlayer> Inside = Scp860Forest.participants(World);
        Map<UUID, Mood> Found = Moods.get(Server);
        if (Found != null) {
            Found.keySet().removeIf(Id -> Inside.stream().noneMatch(User -> User.getUUID().equals(Id)));
        }
        if (Inside.isEmpty()) {
            return;
        }
        List<Scp860Watcher> All = watchers(World);
        for (ServerPlayer User : Inside) {
            step(World, User, mood(User), All);
        }
    }

    public static void step(ServerLevel World, ServerPlayer User, Mood State, List<Scp860Watcher> All) {
        State.Inside++;
        Vec3 Now = User.position();
        double Moved = State.Last == null ? 0.0 : Math.sqrt((Now.x - State.Last.x) * (Now.x - State.Last.x) + (Now.z - State.Last.z) * (Now.z - State.Last.z));
        State.Last = Now;
        State.Paces[State.Cursor] = Moved;
        State.Cursor = (State.Cursor + 1) % PaceWindow;
        boolean Still = State.Inside > PaceWindow && State.pace() < StillPace;
        if (Still) {
            State.Patience = Math.min(1.0f, State.Patience + 1.0f / FillTicks);
        } else {
            State.Patience = Math.max(0.0f, State.Patience - 1.0f / DrainTicks);
        }
        double Gap = Scp860Forest.distance(World.getServer(), User.getX(), User.getZ());
        if (Gap > OffPath) {
            State.OffTicks++;
            State.OnTicks = 0;
        } else if (Gap <= OnPath) {
            State.OnTicks++;
            State.OffTicks = 0;
        }
        rescue(World, User);
        if (User.isCreative() || User.isSpectator()) {
            return;
        }
        if (State.RoarWait > 0) {
            State.RoarWait--;
        }
        if (State.OffTicks >= OffGrace && !State.Pack) {
            State.Pack = true;
            charge(World, User, State, All);
        }
        if (State.Pack && State.OnTicks >= OnSettle) {
            State.Pack = false;
            if (!State.Strike) {
                release(User, All);
            }
        }
        if (State.Pack && World.getGameTime() % 20 == 0) {
            pack(World, User, All);
        }
        if (State.Patience >= 1.0f) {
            State.Full++;
        } else {
            State.Full = 0;
        }
        if (State.Patience >= 1.0f && World.getGameTime() % 20 == 0) {
            int Hunting = hunters(User, All);
            RandomSource Dice = World.getRandom();
            boolean Sure = State.Full >= SureStrike && Hunting == 0;
            if (Hunting == 0 && (Sure || Dice.nextFloat() < StrikeChance) || Hunting > 0 && Hunting < MaxPatienceHunters && Dice.nextFloat() < JoinChance) {
                if (strike(World, User, State, All) || Sure) {
                    State.Strike = true;
                }
            }
        }
        if (State.Strike && !State.Pack && State.Patience < PatienceCalm && Gap <= OnPath) {
            State.Strike = false;
            release(User, All);
        }
        tells(World, User, State, All);
        State.SpawnWait--;
        if (State.SpawnWait <= 0) {
            State.SpawnWait = SpawnEvery;
            int Wanted = State.Inside < GraceTicks ? 0 : Math.min(PerPlayer, 1 + (State.Inside - GraceTicks) / RampTicks);
            if (State.Pack) {
                Wanted = Math.max(Wanted, PackSize);
            }
            int Mine = 0;
            for (Scp860Watcher Watcher : All) {
                if (User.getUUID().equals(Watcher.watched())) {
                    Mine++;
                }
            }
            if (Mine < Wanted && All.size() < GlobalCap) {
                Scp860Watcher Made = spawn(World, User, State.Pack);
                if (Made != null) {
                    All.add(Made);
                    if (State.Pack) {
                        Made.hunt(User);
                    }
                }
            }
        }
    }

    public static int hunters(ServerPlayer User, List<Scp860Watcher> All) {
        int Count = 0;
        for (Scp860Watcher Watcher : All) {
            if (User.getUUID().equals(Watcher.huntTarget())) {
                Count++;
            }
        }
        return Count;
    }

    public static void pack(ServerLevel World, ServerPlayer User, List<Scp860Watcher> All) {
        for (Scp860Watcher Watcher : All) {
            if (Watcher.huntTarget() == null && Watcher.distanceTo(User) <= PackReach) {
                Watcher.hunt(User);
            }
        }
    }

    public static void charge(ServerLevel World, ServerPlayer User, Mood State, List<Scp860Watcher> All) {
        List<Scp860Watcher> Fresh = new ArrayList<>();
        Scp860Watcher Leader = null;
        for (Scp860Watcher Watcher : All) {
            if (Watcher.huntTarget() == null && Watcher.distanceTo(User) <= PackReach) {
                Fresh.add(Watcher);
                if (Leader == null || Watcher.distanceToSqr(User) < Leader.distanceToSqr(User)) {
                    Leader = Watcher;
                }
            }
        }
        boolean Loud = Leader != null && State.RoarWait <= 0;
        if (Loud) {
            State.RoarWait = RoarCooldown;
        }
        for (Scp860Watcher Watcher : Fresh) {
            Watcher.hunt(User, Loud ? Windup : 0, Loud && Watcher == Leader);
        }
    }

    public static boolean strike(ServerLevel World, ServerPlayer User, Mood State, List<Scp860Watcher> All) {
        Scp860Watcher Best = null;
        for (Scp860Watcher Watcher : All) {
            if (Watcher.huntTarget() != null || Watcher.distanceTo(User) > PackReach) {
                continue;
            }
            if (Best == null || Watcher.distanceToSqr(User) < Best.distanceToSqr(User)) {
                Best = Watcher;
            }
        }
        if (Best == null) {
            return false;
        }
        boolean Loud = State.RoarWait <= 0 && hunters(User, All) == 0;
        if (Loud) {
            State.RoarWait = RoarCooldown;
        }
        Best.hunt(User, Loud ? Windup : 0, Loud);
        return true;
    }

    public static void release(ServerPlayer User, List<Scp860Watcher> All) {
        for (Scp860Watcher Watcher : All) {
            if (User.getUUID().equals(Watcher.huntTarget())) {
                Watcher.calm();
            }
        }
    }

    private static void tells(ServerLevel World, ServerPlayer User, Mood State, List<Scp860Watcher> All) {
        RandomSource Dice = World.getRandom();
        if (State.Patience >= RustleAt) {
            State.TellWait--;
            if (State.TellWait <= 0) {
                State.TellWait = RustleMin + Dice.nextInt(RustleMax - RustleMin + 1);
                double Angle = Dice.nextDouble() * Math.PI * 2.0;
                double Reach = RustleNear + Dice.nextDouble() * (RustleFar - RustleNear);
                Vec3 Spot = User.position().add(Math.cos(Angle) * Reach, 0.6, Math.sin(Angle) * Reach);
                Scp860Sounds.hostile(World, Spot, Scp860Registry.RustleLayers, RustleVolume, 0.85f + Dice.nextFloat() * 0.3f);
            }
        }
        if (State.Patience >= GrowlAt) {
            State.GrowlWait--;
            if (State.GrowlWait <= 0) {
                State.GrowlWait = GrowlMin + Dice.nextInt(GrowlMax - GrowlMin + 1);
                Scp860Watcher Closest = null;
                for (Scp860Watcher Watcher : All) {
                    if (User.getUUID().equals(Watcher.watched()) && (Closest == null || Watcher.distanceToSqr(User) < Closest.distanceToSqr(User))) {
                        Closest = Watcher;
                    }
                }
                if (Closest != null) {
                    Closest.growl(State.Patience >= NearAt);
                }
            }
        }
    }

    public static void rescue(ServerLevel World, ServerPlayer User) {
        boolean Outside = !Scp860Forest.plan(World.getServer()).inside(Scp860Forest.localX(User.getX()), Scp860Forest.localZ(User.getZ()));
        if (!Outside && User.getY() > Scp860Forest.Ground - RescueDepth) {
            return;
        }
        Vec3 Point = nearestPathPoint(World.getServer(), User.position());
        User.teleportTo(World, Point.x, Point.y, Point.z, User.getYRot(), User.getXRot());
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static Vec3 nearestPathPoint(MinecraftServer Server, Vec3 From) {
        Scp860Plan Layout = Scp860Forest.plan(Server);
        int Best = 0;
        double Closest = Double.MAX_VALUE;
        double Lx = Scp860Forest.localX(From.x);
        double Lz = Scp860Forest.localZ(From.z);
        for (int Index = 0; Index < Layout.samples(); Index += 2) {
            double Dx = Layout.sampleX(Index) - Lx;
            double Dz = Layout.sampleZ(Index) - Lz;
            double Gap = Dx * Dx + Dz * Dz;
            if (Gap < Closest) {
                Closest = Gap;
                Best = Index;
            }
        }
        return Scp860Forest.pathPoint(Server, Best);
    }

    @Nullable
    public static Scp860Watcher spawn(ServerLevel World, ServerPlayer User, boolean Pack) {
        MinecraftServer Server = World.getServer();
        Scp860Plan Layout = Scp860Forest.plan(Server);
        RandomSource Dice = World.getRandom();
        int Here = Layout.nearest(Scp860Forest.localX(User.getX()), Scp860Forest.localZ(User.getZ()));
        if (Here < 0) {
            Here = 0;
        }
        double Rank = Layout.rank(Here);
        for (int Try = 0; Try < SpawnTries; Try++) {
            Vec3 Spot = candidate(Server, Layout, Dice, User, Here, Rank, Pack || Try > SpawnTries / 2);
            if (Spot == null) {
                continue;
            }
            BlockPos Feet = groundAt(World, Spot);
            if (Feet == null) {
                continue;
            }
            Vec3 Place = Vec3.atBottomCenterOf(Feet);
            if (!Pack && seen(World, Place)) {
                continue;
            }
            Scp860Watcher Made = Scp860Registry.Watcher.get().create(World);
            if (Made == null) {
                return null;
            }
            Made.moveTo(Place.x, Place.y, Place.z, Dice.nextFloat() * 360.0f, 0.0f);
            if (!World.noCollision(Made)) {
                continue;
            }
            Made.bindToForest(Scp860State.get(Server).generation());
            Made.watch(User);
            Made.finalizeSpawn(World, World.getCurrentDifficultyAt(Feet), MobSpawnType.EVENT, null);
            World.addFreshEntity(Made);
            return Made;
        }
        return null;
    }

    @Nullable
    private static Vec3 candidate(MinecraftServer Server, Scp860Plan Layout, RandomSource Dice, ServerPlayer User, int Here, double Rank, boolean Anywhere) {
        int Steps = (int) Math.round((SpawnNear + Dice.nextDouble() * (SpawnFar - SpawnNear)) / Scp860Plan.SampleStep);
        int Ahead = -1;
        for (int Sign : new int[]{1, -1}) {
            int Index = Here + Sign * Steps;
            if (Index >= 0 && Index < Layout.samples() && (Anywhere || Layout.rank(Index) <= Rank)) {
                Ahead = Index;
                break;
            }
        }
        if (Ahead < 0) {
            double Angle = Dice.nextDouble() * Math.PI * 2.0;
            double Reach = SpawnNear + Dice.nextDouble() * (SpawnFar - SpawnNear);
            return new Vec3(User.getX() + Math.cos(Angle) * Reach, Scp860Forest.Ground + 1.0, User.getZ() + Math.sin(Angle) * Reach);
        }
        int Next = Math.min(Layout.samples() - 1, Ahead + 1);
        int Prior = Math.max(0, Ahead - 1);
        double Tx = Layout.sampleX(Next) - Layout.sampleX(Prior);
        double Tz = Layout.sampleZ(Next) - Layout.sampleZ(Prior);
        double Length = Math.max(1.0e-6, Math.sqrt(Tx * Tx + Tz * Tz));
        double Side = (Dice.nextBoolean() ? 1.0 : -1.0) * (SpawnSide + Dice.nextDouble() * (SpawnSideFar - SpawnSide));
        double Lx = Layout.sampleX(Ahead) - Tz / Length * Side;
        double Lz = Layout.sampleZ(Ahead) + Tx / Length * Side;
        if (Layout.distance(Lx, Lz) < SpawnOffPath || !Layout.inside(Lx, Lz)) {
            return null;
        }
        return new Vec3(Scp860Forest.baseX() + Lx, Scp860Forest.Ground + 1.0, Scp860Forest.baseZ() + Lz);
    }

    @Nullable
    public static BlockPos groundAt(ServerLevel World, Vec3 Spot) {
        BlockPos.MutableBlockPos Cursor = BlockPos.containing(Spot.x, Scp860Forest.Ground + 6, Spot.z).mutable();
        for (int Step = 0; Step < 10; Step++) {
            BlockPos Below = Cursor.below();
            if (World.getBlockState(Below).isFaceSturdy(World, Below, net.minecraft.core.Direction.UP) && World.getBlockState(Cursor).getCollisionShape(World, Cursor).isEmpty()
                    && World.getBlockState(Cursor.above()).getCollisionShape(World, Cursor.above()).isEmpty()) {
                return Cursor.immutable();
            }
            Cursor.move(net.minecraft.core.Direction.DOWN);
        }
        return null;
    }

    public static boolean seen(ServerLevel World, Vec3 Spot) {
        Vec3 Target = Spot.add(0.0, 1.2, 0.0);
        for (ServerPlayer User : Scp860Forest.participants(World)) {
            Vec3 Eye = User.getEyePosition();
            if (Eye.distanceTo(Target) > SpawnFar + 8.0) {
                continue;
            }
            if (World.clip(new ClipContext(Eye, Target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, User)).getType() != HitResult.Type.BLOCK) {
                return true;
            }
        }
        return false;
    }

    public static List<ServerPlayer> sortedNear(ServerLevel World, Vec3 From, double Reach) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : Scp860Forest.participants(World)) {
            if (User.position().distanceTo(From) <= Reach) {
                Found.add(User);
            }
        }
        Found.sort(Comparator.comparingDouble(User -> User.position().distanceToSqr(From)));
        return Found;
    }

    public static AABB around(Vec3 Centre, double Reach) {
        return new AABB(Centre, Centre).inflate(Reach);
    }

    public static double clampBand(double Value, double Low, double High) {
        return Mth.clamp(Value, Low, High);
    }

    public static boolean standable(ServerLevel World, BlockPos Feet) {
        return net.rai.scp_unleashed.gateway.GatewayTravel.standable(World, Feet, EntityType.PLAYER);
    }
}
