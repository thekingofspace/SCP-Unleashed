package net.rai.scp_unleashed.scp178;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp178.network.Scp178ReportPayload;
import net.rai.scp_unleashed.scp178.network.Scp178WatcherPayload;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class Scp178Watchers {
    public static final int MaxInstances = 8;
    public static final int FirstDelayMin = 300;
    public static final int FirstDelayMax = 500;
    public static final int SlowInterval = 900;
    public static final int FastInterval = 260;
    public static final int RampTicks = 12000;
    public static final int CapStep = 1500;
    public static final double SpawnNear = 16.0;
    public static final double SpawnFar = 28.0;
    public static final double CalmNear = 10.0;
    public static final double CalmFar = 22.0;
    public static final double CalmDrift = 0.05;
    public static final int DriftEvery = 100;
    public static final float MaxHealth = 30.0f;
    public static final int PatienceMin = 1400;
    public static final int PatienceMax = 3200;
    public static final double HuntSpeed = 0.24;
    public static final double LungeReach = 3.0;
    public static final double StrikeReach = 3.5;
    public static final double LungeLeap = 3.0;
    public static final int WhiffCooldown = 30;
    public static final double LoseDistance = 32.0;
    public static final int LoseTicks = 60;
    public static final double HiddenCone = 0.35;
    public static final double StruckDistance = 2.0;
    public static final double CloseFloor = 1.5;
    public static final double StandOff = 3.0;
    public static final int WindupTicks = 11;
    public static final int LungeCooldown = 55;
    public static final float LungeDamage = 6.0f;
    public static final double AlarmRange = 16.0;
    public static final double RestlessRange = 16.0;
    public static final int ReportWindow = 20;
    public static final int ReportLimit = 6;
    public static final double SyncStep = 0.25;
    public static final int SyncEvery = 10;

    private static final Map<UUID, Haunt> Haunts = new HashMap<>();
    private static final Map<UUID, LivingEntity> Bodies = new HashMap<>();
    static BiConsumer<ServerPlayer, Scp178WatcherPayload> Outbox = (User, Payload) -> PacketDistributor.sendToPlayer(User, Payload);

    private Scp178Watchers() {
    }

    public static final class Haunt {
        private final List<Scp178Phantom> Phantoms = new ArrayList<>();
        private int Worn;
        private int Timer = -1;
        private boolean Wearing;
        private int NextId = 1;
        private int Reports;
        private long WindowStart;
        @Nullable
        private Vec3 Last;

        public int worn() {
            return Worn;
        }

        public int timer() {
            return Timer;
        }

        public boolean wearing() {
            return Wearing;
        }

        public List<Scp178Phantom> phantoms() {
            return List.copyOf(Phantoms);
        }

        void setWorn(int Ticks) {
            Worn = Math.max(0, Ticks);
        }

        void setTimer(int Ticks) {
            Timer = Ticks;
        }
    }

    @Nullable
    public static Haunt haunt(UUID Owner) {
        return Haunts.get(Owner);
    }

    public static Haunt hauntFor(UUID Owner) {
        return Haunts.computeIfAbsent(Owner, Id -> new Haunt());
    }

    public static int count(UUID Owner) {
        Haunt State = Haunts.get(Owner);
        return State == null ? 0 : State.Phantoms.size();
    }

    public static int hostileCount(UUID Owner) {
        Haunt State = Haunts.get(Owner);
        if (State == null) {
            return 0;
        }
        int Found = 0;
        for (Scp178Phantom Each : State.Phantoms) {
            Found += Each.Hostile ? 1 : 0;
        }
        return Found;
    }

    public static int totalPhantoms() {
        int Total = 0;
        for (Haunt State : Haunts.values()) {
            Total += State.Phantoms.size();
        }
        return Total;
    }

    public static int cap(int Worn) {
        return Math.min(MaxInstances, 1 + Math.max(0, Worn) / CapStep);
    }

    public static int interval(int Worn, RandomSource Random) {
        float Ramp = Mth.clamp(Worn / (float) RampTicks, 0.0f, 1.0f);
        return Math.max(20, Math.round(Mth.lerp(Ramp, SlowInterval, FastInterval) * (0.8f + 0.4f * Random.nextFloat())));
    }

    public static int firstDelay(RandomSource Random) {
        return FirstDelayMin + Random.nextInt(FirstDelayMax - FirstDelayMin + 1);
    }

    public static boolean eligible(LivingEntity Body) {
        return Body.isAlive() && !Body.isRemoved() && !Body.isSpectator() && Targeting.isTargetable(Body) && Body.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    public static boolean isPrey(LivingEntity Body) {
        if (!eligible(Body)) {
            return false;
        }
        return !(Body instanceof Player User && (User.isCreative() || Scp268.isNeglected(User)));
    }

    public static void noteWearer(LivingEntity Body) {
        if (!(Body instanceof Player) && ControlBodies.isPersonLike(Body) && !Body.level().isClientSide) {
            Bodies.put(Body.getUUID(), Body);
        }
    }

    public static void serverTick(MinecraftServer Server) {
        Set<UUID> Present = new HashSet<>();
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Present.add(User.getUUID());
            tick(User);
        }
        for (Iterator<Map.Entry<UUID, LivingEntity>> Entries = Bodies.entrySet().iterator(); Entries.hasNext(); ) {
            Map.Entry<UUID, LivingEntity> Entry = Entries.next();
            LivingEntity Body = Entry.getValue();
            if (Body.isRemoved() || !Body.isAlive() || Present.contains(Entry.getKey()) || !Scp178GlassesItem.isWorn(Body)) {
                Entries.remove();
                continue;
            }
            Present.add(Entry.getKey());
            tick(Body);
        }
        Haunts.keySet().retainAll(Present);
    }

    public static void tick(LivingEntity Owner) {
        if (Owner.level().isClientSide) {
            return;
        }
        Haunt State = hauntFor(Owner.getUUID());
        boolean Wearing = Scp178GlassesItem.isWorn(Owner) && eligible(Owner);
        if (!Wearing) {
            if (State.Wearing || !State.Phantoms.isEmpty()) {
                end(Owner, State);
            }
            State.Wearing = false;
            State.Worn = 0;
            State.Timer = -1;
            State.Last = null;
            return;
        }
        if (!State.Wearing) {
            State.Worn = 0;
            State.Timer = firstDelay(Owner.getRandom());
        }
        State.Wearing = true;
        State.Worn++;
        Vec3 Here = Owner.position();
        State.Last = Here;
        if (--State.Timer <= 0) {
            trySpawn(Owner, State);
            State.Timer = interval(State.Worn, Owner.getRandom());
        }
        boolean Prey = isPrey(Owner);
        for (Scp178Phantom Phantom : List.copyOf(State.Phantoms)) {
            if (State.Phantoms.contains(Phantom)) {
                update(Owner, Phantom, Here, Prey);
            }
        }
    }

    private static void update(LivingEntity Owner, Scp178Phantom Phantom, Vec3 Here, boolean Prey) {
        RandomSource Random = Owner.getRandom();
        if (!Phantom.Hostile) {
            Phantom.Age++;
            if (--Phantom.DriftTimer <= 0) {
                Phantom.DriftTimer = DriftEvery + Random.nextInt(DriftEvery);
                Phantom.Drift = CalmNear + Random.nextDouble() * (CalmFar - CalmNear);
            }
            Phantom.Distance += Mth.clamp(Phantom.Drift - Phantom.Distance, -CalmDrift, CalmDrift);
            if (Phantom.Age >= Phantom.Patience) {
                turnHostile(Owner, Phantom);
            }
            sync(Owner, Phantom);
            return;
        }
        if (!Phantom.Anchored) {
            Phantom.PointX = Here.x + Math.cos(Phantom.Bearing) * Phantom.Distance;
            Phantom.PointZ = Here.z + Math.sin(Phantom.Bearing) * Phantom.Distance;
            Phantom.Anchored = true;
        }
        hunt(Phantom, Here, Prey ? CloseFloor : StandOff, HuntSpeed);
        if (Phantom.Windup >= 0) {
            if (!Prey) {
                Phantom.Windup = -1;
            } else if (--Phantom.Windup < 0) {
                strike(Owner, Phantom, Here);
            }
            return;
        }
        Phantom.Cooldown = Math.max(0, Phantom.Cooldown - 1);
        if (Phantom.Distance > LoseDistance) {
            if (++Phantom.Behind >= LoseTicks) {
                giveUp(Owner, Phantom, Here, Random);
                return;
            }
        } else {
            Phantom.Behind = 0;
        }
        if (Prey && Phantom.Cooldown == 0 && Phantom.Distance <= LungeReach) {
            Phantom.Windup = WindupTicks;
            send(Owner, Scp178WatcherPayload.of(Scp178WatcherPayload.Windup, Phantom.id()));
            return;
        }
        sync(Owner, Phantom);
    }

    private static void hunt(Scp178Phantom Phantom, Vec3 Here, double Stop, double Step) {
        double Dx = Here.x - Phantom.PointX;
        double Dz = Here.z - Phantom.PointZ;
        double Gap = Math.hypot(Dx, Dz);
        if (Gap > Stop) {
            double Move = Math.min(Step, Gap - Stop);
            Phantom.PointX += Dx / Gap * Move;
            Phantom.PointZ += Dz / Gap * Move;
        }
        Phantom.Distance = Math.hypot(Here.x - Phantom.PointX, Here.z - Phantom.PointZ);
    }

    private static void giveUp(LivingEntity Owner, Scp178Phantom Phantom, Vec3 Here, RandomSource Random) {
        Phantom.Hostile = false;
        Phantom.Anchored = false;
        Phantom.Behind = 0;
        Phantom.Windup = -1;
        Phantom.Age = 0;
        Phantom.Patience = PatienceMin + Random.nextInt(PatienceMax - PatienceMin + 1);
        Phantom.Bearing = Math.atan2(Phantom.PointZ - Here.z, Phantom.PointX - Here.x);
        Phantom.Drift = CalmFar;
        Phantom.DriftTimer = DriftEvery;
        send(Owner, new Scp178WatcherPayload(Scp178WatcherPayload.Calm, Phantom.id(), (float) Phantom.Distance, (float) Phantom.Bearing));
    }

    private static void strike(LivingEntity Owner, Scp178Phantom Phantom, Vec3 Here) {
        double Reach = Phantom.Distance;
        boolean Lands = Reach <= StrikeReach && isPrey(Owner);
        Phantom.Windup = -1;
        Phantom.Cooldown = Lands ? LungeCooldown : WhiffCooldown;
        hunt(Phantom, Here, StruckDistance, LungeLeap);
        Phantom.Sent = Phantom.Distance;
        send(Owner, new Scp178WatcherPayload(Scp178WatcherPayload.Attack, Phantom.id(), (float) Reach));
        if (Lands) {
            Owner.hurt(Owner.damageSources().source(Scp178Registry.Taken), LungeDamage);
        }
    }

    private static void sync(LivingEntity Owner, Scp178Phantom Phantom) {
        Phantom.SinceSent++;
        if (Math.abs(Phantom.Distance - Phantom.Sent) < SyncStep && Phantom.SinceSent < SyncEvery) {
            return;
        }
        if (Math.abs(Phantom.Distance - Phantom.Sent) < 1.0e-3) {
            Phantom.SinceSent = 0;
            return;
        }
        Phantom.Sent = Phantom.Distance;
        Phantom.SinceSent = 0;
        send(Owner, new Scp178WatcherPayload(Scp178WatcherPayload.Distance, Phantom.id(), (float) Phantom.Distance));
    }

    private static void send(LivingEntity Owner, Scp178WatcherPayload Payload) {
        if (Owner instanceof ServerPlayer User && User.connection != null) {
            Outbox.accept(User, Payload);
        }
    }

    public static void turnHostile(LivingEntity Owner, Scp178Phantom Phantom) {
        if (Phantom.Hostile) {
            return;
        }
        Phantom.Hostile = true;
        send(Owner, Scp178WatcherPayload.of(Scp178WatcherPayload.Hostile, Phantom.id()));
    }

    public static boolean trySpawn(LivingEntity Owner, Haunt State) {
        if (State.Phantoms.size() >= cap(State.Worn)) {
            return false;
        }
        return spawn(Owner, State, false) != null;
    }

    @Nullable
    public static Scp178Phantom spawn(LivingEntity Owner, Haunt State, boolean Hostile) {
        if (!State.Wearing || State.Phantoms.size() >= MaxInstances) {
            return null;
        }
        RandomSource Random = Owner.getRandom();
        double Distance = SpawnNear + Random.nextDouble() * (SpawnFar - SpawnNear);
        Scp178Phantom Phantom = new Scp178Phantom(State.NextId++, Distance, MaxHealth, PatienceMin + Random.nextInt(PatienceMax - PatienceMin + 1));
        Phantom.Sent = Distance;
        Phantom.Bearing = bearing(Owner, Random);
        State.Phantoms.add(Phantom);
        send(Owner, new Scp178WatcherPayload(Scp178WatcherPayload.Spawn, Phantom.id(), (float) Distance, (float) Phantom.Bearing));
        if (Hostile) {
            turnHostile(Owner, Phantom);
        }
        return Phantom;
    }

    public static double bearing(LivingEntity Owner, RandomSource Random) {
        Vec3 Look = Owner.getViewVector(1.0f);
        Vec3 Flat = new Vec3(Look.x, 0.0, Look.z);
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        if (Flat.lengthSqr() < 1.0e-4) {
            return Angle;
        }
        Flat = Flat.normalize();
        for (int Attempt = 0; Attempt < 12; Attempt++) {
            if (Math.cos(Angle) * Flat.x + Math.sin(Angle) * Flat.z < HiddenCone) {
                return Angle;
            }
            Angle = Random.nextDouble() * Mth.TWO_PI;
        }
        return Angle;
    }

    public static void report(ServerPlayer User, Scp178ReportPayload Payload) {
        Haunt State = Haunts.get(User.getUUID());
        if (State == null || !State.Wearing) {
            return;
        }
        long Now = User.server.getTickCount();
        if (Now - State.WindowStart >= ReportWindow) {
            State.WindowStart = Now;
            State.Reports = 0;
        }
        if (++State.Reports > ReportLimit) {
            return;
        }
        Scp178Phantom Phantom = find(State, Payload.Id());
        if (Phantom == null) {
            return;
        }
        if (Payload.Kind() == Scp178ReportPayload.Stared) {
            turnHostile(User, Phantom);
        } else if (Payload.Kind() == Scp178ReportPayload.Struck) {
            struck(User, State, Phantom);
        }
    }

    private static void struck(ServerPlayer User, Haunt State, Scp178Phantom Phantom) {
        float Strength = User.getAttackStrengthScale(0.5f);
        float Damage = (float) User.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2f + Strength * Strength * 0.8f);
        User.resetAttackStrengthTicker();
        Phantom.Health -= Damage;
        turnHostile(User, Phantom);
        for (Scp178Phantom Other : List.copyOf(State.Phantoms)) {
            if (Other != Phantom && Math.abs(Other.Distance - Phantom.Distance) <= AlarmRange) {
                turnHostile(User, Other);
            }
        }
        if (Phantom.Health <= 0.0f) {
            State.Phantoms.remove(Phantom);
            send(User, Scp178WatcherPayload.of(Scp178WatcherPayload.Killed, Phantom.id()));
        }
    }

    @Nullable
    private static Scp178Phantom find(Haunt State, int Id) {
        for (Scp178Phantom Each : State.Phantoms) {
            if (Each.id() == Id) {
                return Each;
            }
        }
        return null;
    }

    public static boolean restless(Player Sleeper) {
        Haunt State = Haunts.get(Sleeper.getUUID());
        if (State == null) {
            return false;
        }
        for (Scp178Phantom Each : State.Phantoms) {
            if (Each.Hostile && Each.Distance <= RestlessRange) {
                return true;
            }
        }
        return false;
    }

    private static void end(LivingEntity Owner, Haunt State) {
        State.Phantoms.clear();
        send(Owner, Scp178WatcherPayload.clear());
    }

    public static int clear(LivingEntity Owner) {
        Haunt State = Haunts.get(Owner.getUUID());
        if (State == null) {
            return 0;
        }
        int Removed = State.Phantoms.size();
        end(Owner, State);
        return Removed;
    }

    public static void forget(LivingEntity Owner) {
        Haunt State = Haunts.remove(Owner.getUUID());
        if (State != null && !State.Phantoms.isEmpty()) {
            send(Owner, Scp178WatcherPayload.clear());
        }
        Bodies.remove(Owner.getUUID());
    }

    public static void reset() {
        Haunts.clear();
        Bodies.clear();
    }
}
