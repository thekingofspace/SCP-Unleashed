package net.rai.scp_unleashed.scp173;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.dclass.duty.DClassGaze;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

public final class Scp173Sight {
    public static final int Window = 10;
    public static final int StaleTicks = 8;
    public static final int Heartbeat = 4;
    public static final int ChangeGap = 1;
    public static final int MaxReported = 64;
    public static final double MaxRange = 160.0;
    public static final double PlausibleCone = 0.2;
    public static final double PodRange = 16.0;
    public static final double PodCone = 0.34;
    public static final double SpawnSightRange = 96.0;
    private static final Map<UUID, Watcher> Watchers = new HashMap<>();
    private static final List<BiPredicate<ServerLevel, Scp173Statue>> Overseers = new CopyOnWriteArrayList<>();

    private Scp173Sight() {
    }

    private static final class Watcher {
        private long LastReport = Long.MIN_VALUE;
        @Nullable
        private ResourceKey<Level> Dimension;
        private final Map<Integer, Long> SeenAt = new HashMap<>();
        private Set<Integer> Latest = Set.of();

        private void reset(@Nullable ResourceKey<Level> Where) {
            LastReport = Long.MIN_VALUE;
            Dimension = Where;
            SeenAt.clear();
            Latest = Set.of();
        }
    }

    public static long clock(ServerLevel World) {
        return World.getServer().getTickCount();
    }

    public static void report(ServerPlayer User, List<Integer> Ids) {
        ServerLevel World = User.serverLevel();
        long Now = clock(World);
        Watcher Entry = Watchers.computeIfAbsent(User.getUUID(), Key -> new Watcher());
        if (Entry.Dimension != World.dimension()) {
            Entry.reset(World.dimension());
        }
        Set<Integer> Valid = new HashSet<>();
        int Count = 0;
        for (Integer Id : Ids) {
            if (Count++ >= MaxReported) {
                break;
            }
            Entity Found = World.getEntity(Id);
            if (Found instanceof Scp173Statue Statue && Statue.distanceToSqr(CameraViews.origin(User)) <= MaxRange * MaxRange) {
                Valid.add(Id);
                Entry.SeenAt.put(Id, Now);
            }
        }
        Entry.Latest = Valid;
        Entry.LastReport = Now;
        Entry.SeenAt.values().removeIf(Seen -> Now - Seen > Window * 8L);
    }

    public static void forget(UUID Player) {
        Watchers.remove(Player);
    }

    public static void reset(ServerPlayer User) {
        Watcher Entry = Watchers.get(User.getUUID());
        if (Entry != null) {
            Entry.reset(null);
        }
    }

    public static void clear() {
        Watchers.clear();
    }

    public static boolean canWatch(ServerPlayer User, Scp173Statue Statue) {
        return User.isAlive() && !User.isSpectator() && !User.isCreative() && !User.isSleeping() && User.level() == Statue.level() && Statue.distanceToSqr(CameraViews.origin(User)) <= MaxRange * MaxRange
                && Statue.trackers().contains(User.getUUID()) && !Senses.isBlindfolded(User) && Targeting.canWitness(User);
    }

    public static boolean observedBy(ServerPlayer User, Scp173Statue Statue, long Now) {
        if (!canWatch(User, Statue)) {
            return false;
        }
        Watcher Entry = Watchers.get(User.getUUID());
        boolean Here = Entry != null && Entry.Dimension == Statue.level().dimension();
        if (Here && Now - Entry.LastReport <= StaleTicks) {
            Long Seen = Entry.SeenAt.get(Statue.getId());
            return Seen != null && Now - Seen <= Window;
        }
        if (Here && Entry.Latest.contains(Statue.getId())) {
            return true;
        }
        return plausible(User.serverLevel(), CameraViews.eye(User), CameraViews.look(User), Statue.sightBox());
    }

    public static boolean reporting(ServerPlayer User, long Now) {
        Watcher Entry = Watchers.get(User.getUUID());
        return Entry != null && Entry.Dimension == User.level().dimension() && Now - Entry.LastReport <= StaleTicks;
    }

    public static boolean plausible(ServerLevel World, Vec3 Eye, Vec3 Look, AABB Box) {
        for (Vec3 Point : samples(Box)) {
            Vec3 Toward = Point.subtract(Eye);
            double Length = Toward.length();
            if (Length < 1.0e-3) {
                return true;
            }
            if (Toward.scale(1.0 / Length).dot(Look) >= PlausibleCone && clear(World, Eye, Point)) {
                return true;
            }
        }
        return false;
    }

    public static List<Vec3> samples(AABB Box) {
        Vec3 Centre = Box.getCenter();
        double Low = Box.minY + 0.15;
        double High = Box.maxY - 0.15;
        return List.of(Centre, new Vec3(Centre.x, High, Centre.z), new Vec3(Centre.x, Low, Centre.z),
                new Vec3(Box.minX + 0.05, Centre.y, Box.minZ + 0.05), new Vec3(Box.maxX - 0.05, Centre.y, Box.maxZ - 0.05),
                new Vec3(Box.minX + 0.05, High, Box.maxZ - 0.05), new Vec3(Box.maxX - 0.05, High, Box.minZ + 0.05));
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To) {
        Vec3 Toward = From.subtract(To);
        double Length = Toward.length();
        if (Length < 0.1) {
            return true;
        }
        Vec3 End = To.add(Toward.scale(0.06 / Length));
        return World.clip(new ClipContext(From, End, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    public static Vec3 podEye(Scp131Pod Pod) {
        return Pod.lookFrom();
    }

    public static boolean podSees(Scp131Pod Pod, Entity Statue) {
        if (!Pod.isAlive() || Pod.isRemoved() || Pod.level() != Statue.level() || !Targeting.canWitness(Pod)) {
            return false;
        }
        Vec3 Eye = podEye(Pod);
        AABB Box = Statue instanceof Scp173Statue Sculpture ? Sculpture.sightBox() : Statue.getBoundingBox();
        Vec3 Centre = Box.getCenter();
        if (Eye.distanceToSqr(Centre) > PodRange * PodRange) {
            return false;
        }
        Vec3 Look = Vec3.directionFromRotation(0.0f, Pod.getYHeadRot());
        Vec3 Flat = new Vec3(Centre.x - Eye.x, 0.0, Centre.z - Eye.z);
        if (Flat.lengthSqr() > 0.25 && Flat.normalize().dot(Look) < PodCone) {
            return false;
        }
        for (Vec3 Point : samples(Box)) {
            if (clear(Pod.level(), Eye, Point)) {
                return true;
            }
        }
        return false;
    }

    public static List<Scp131Pod> podObservers(ServerLevel World, Scp173Statue Statue) {
        return World.getEntitiesOfClass(Scp131Pod.class, Statue.getBoundingBox().inflate(PodRange), Pod -> podSees(Pod, Statue));
    }

    public static List<ServerPlayer> playerObservers(ServerLevel World, Scp173Statue Statue) {
        long Now = clock(World);
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : World.players()) {
            if (observedBy(User, Statue, Now)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static boolean personSees(LivingEntity Viewer, Scp173Statue Statue, AABB Box) {
        return DClassGaze.watcher(Viewer) && !DClassGaze.blinking(Viewer) && Scp173Witness.canSee(Viewer, Statue, Box);
    }

    public static List<LivingEntity> personObservers(ServerLevel World, Scp173Statue Statue) {
        AABB Box = Statue.sightBox();
        return World.getEntitiesOfClass(LivingEntity.class, Box.inflate(Scp173Witness.Range), Viewer -> personSees(Viewer, Statue, Box));
    }

    public static boolean personWatches(ServerLevel World, Scp173Statue Statue) {
        AABB Box = Statue.sightBox();
        for (LivingEntity Viewer : World.getEntitiesOfClass(LivingEntity.class, Box.inflate(Scp173Witness.Range), DClassGaze::watcher)) {
            if (!DClassGaze.blinking(Viewer) && Scp173Witness.canSee(Viewer, Statue, Box)) {
                return true;
            }
        }
        return false;
    }

    public static boolean personCouldSee(ServerLevel World, AABB Box) {
        for (LivingEntity Viewer : World.getEntitiesOfClass(LivingEntity.class, Box.inflate(Scp173Witness.Range), DClassGaze::watcher)) {
            if (DClassGaze.couldSee(Viewer, Box)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isObserved(ServerLevel World, Scp173Statue Statue) {
        long Now = clock(World);
        for (ServerPlayer User : World.players()) {
            if (observedBy(User, Statue, Now)) {
                return true;
            }
        }
        if (personWatches(World, Statue)) {
            return true;
        }
        for (Scp131Pod Pod : World.getEntitiesOfClass(Scp131Pod.class, Statue.getBoundingBox().inflate(PodRange))) {
            if (podSees(Pod, Statue)) {
                return true;
            }
        }
        for (BiPredicate<ServerLevel, Scp173Statue> Overseer : Overseers) {
            if (Overseer.test(World, Statue)) {
                return true;
            }
        }
        return false;
    }

    public static void addWatcher(BiPredicate<ServerLevel, Scp173Statue> Overseer) {
        Overseers.add(Overseer);
    }

    public static boolean anyoneCouldSee(ServerLevel World, AABB Box) {
        Vec3 Centre = Box.getCenter();
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || User.isCreative() || !User.isAlive() || Senses.isBlindfolded(User) || !Targeting.canWitness(User) || CameraViews.origin(User).distanceToSqr(Centre) > SpawnSightRange * SpawnSightRange) {
                continue;
            }
            if (plausible(World, CameraViews.eye(User), CameraViews.look(User), Box)) {
                return true;
            }
        }
        if (personCouldSee(World, Box)) {
            return true;
        }
        for (Scp131Pod Pod : World.getEntitiesOfClass(Scp131Pod.class, Box.inflate(PodRange))) {
            Vec3 Eye = podEye(Pod);
            for (Vec3 Point : samples(Box)) {
                if (Eye.distanceToSqr(Point) <= PodRange * PodRange && clear(World, Eye, Point)) {
                    return true;
                }
            }
        }
        return false;
    }
}
