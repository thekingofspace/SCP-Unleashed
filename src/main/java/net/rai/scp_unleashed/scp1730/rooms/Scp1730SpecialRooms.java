package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730RoomInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Scp1730SpecialRooms {
    private static final Map<Long, List<Scp1730SpecialRoom>> Built = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, List<Scp1730SpecialRoom>> Pinned = new ConcurrentHashMap<>();
    private static final AtomicInteger PinVersion = new AtomicInteger();
    private static volatile ResourceKey<Level> ClientDimension;
    private static volatile List<Scp1730SpecialRoom> ClientRooms = List.of();

    private Scp1730SpecialRooms() {
    }

    public static List<Scp1730SpecialRoom> of(@Nullable Level World) {
        if (World == null) {
            return List.of();
        }
        if (World.isClientSide) {
            return World.dimension() == ClientDimension ? ClientRooms : List.of();
        }
        List<Scp1730SpecialRoom> Extra = Pinned.get(World.dimension());
        List<Scp1730SpecialRoom> Facility = World instanceof ServerLevel Server ? facility(Server) : List.of();
        if (Extra == null || Extra.isEmpty()) {
            return Facility;
        }
        if (Facility.isEmpty()) {
            return Extra;
        }
        List<Scp1730SpecialRoom> Both = new ArrayList<>(Facility);
        Both.addAll(Extra);
        return Both;
    }

    public static List<Scp1730SpecialRoom> facility(ServerLevel World) {
        if (!Scp1730World.inside(World)) {
            return List.of();
        }
        Scp1730State State = Scp1730State.get(World.getServer());
        if (State.run() < 0) {
            return List.of();
        }
        return forRun(State.run());
    }

    public static List<Scp1730SpecialRoom> forRun(int Run) {
        long Key = (long) Run << 1 | (Scp1730World.standingIn() ? 1L : 0L);
        return Built.computeIfAbsent(Key, Unused -> build(Run));
    }

    private static List<Scp1730SpecialRoom> build(int Run) {
        Scp1730Plan Plan = Scp1730World.plan(Run);
        int Ox = Scp1730World.originX(Run);
        int Oz = Scp1730World.originZ();
        List<Scp1730SpecialRoom> Found = new ArrayList<>();
        for (Scp1730RoomInfo Info : Plan.rooms()) {
            Scp1730RoomKind Kind = Scp1730RoomKind.of(Info.Type());
            if (Kind == null) {
                continue;
            }
            List<Scp1730SpecialRoom.Door> Doors = new ArrayList<>();
            for (int[] Each : Info.Doors()) {
                Doors.add(new Scp1730SpecialRoom.Door(Ox + Each[0], Oz + Each[1], Each[2]));
            }
            Found.add(new Scp1730SpecialRoom(Kind, Ox + Info.MinX(), Oz + Info.MinZ(), Ox + Info.MaxX(), Oz + Info.MaxZ(), Info.Floor(), Info.Height(), List.copyOf(Doors)));
        }
        return Collections.unmodifiableList(Found);
    }

    @Nullable
    public static Scp1730SpecialRoom at(@Nullable Level World, double X, double Y, double Z) {
        for (Scp1730SpecialRoom Each : of(World)) {
            if (Each.contains(X, Y, Z)) {
                return Each;
            }
        }
        return null;
    }

    @Nullable
    public static Scp1730SpecialRoom at(@Nullable Level World, Vec3 Point) {
        return at(World, Point.x, Point.y, Point.z);
    }

    @Nullable
    public static Scp1730SpecialRoom at(@Nullable Level World, Vec3 Point, Scp1730RoomKind Kind) {
        for (Scp1730SpecialRoom Each : of(World)) {
            if (Each.Kind() == Kind && Each.contains(Point)) {
                return Each;
            }
        }
        return null;
    }

    @Nullable
    public static Scp1730SpecialRoom first(@Nullable Level World, Scp1730RoomKind Kind) {
        for (Scp1730SpecialRoom Each : of(World)) {
            if (Each.Kind() == Kind) {
                return Each;
            }
        }
        return null;
    }

    public static boolean silentAt(@Nullable Level World, double X, double Y, double Z) {
        for (Scp1730SpecialRoom Each : of(World)) {
            if (Each.Kind() == Scp1730RoomKind.Silent && Each.contains(X, Y, Z)) {
                return true;
            }
        }
        return false;
    }

    public static boolean silenced(@Nullable Entity Target) {
        return Target != null && silentAt(Target.level(), Target.getX(), Target.getY() + 0.1, Target.getZ());
    }

    public static void pin(Level World, Scp1730SpecialRoom Room) {
        Pinned.computeIfAbsent(World.dimension(), Key -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(Room);
        PinVersion.incrementAndGet();
    }

    public static void unpin(Level World, Scp1730SpecialRoom Room) {
        List<Scp1730SpecialRoom> Found = Pinned.get(World.dimension());
        if (Found != null) {
            Found.remove(Room);
        }
        PinVersion.incrementAndGet();
    }

    public static int pinVersion() {
        return PinVersion.get();
    }

    public static void acceptClient(@Nullable ResourceKey<Level> Dimension, List<Scp1730SpecialRoom> Rooms) {
        ClientDimension = Dimension;
        ClientRooms = List.copyOf(Rooms);
    }

    public static void resetClient() {
        ClientDimension = null;
        ClientRooms = List.of();
    }

    public static void clear() {
        Built.clear();
        Pinned.clear();
        PinVersion.incrementAndGet();
    }
}
