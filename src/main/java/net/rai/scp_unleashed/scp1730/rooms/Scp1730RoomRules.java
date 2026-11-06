package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730RoomRules {
    public static final String VoiceReason = "scp1730_silent";
    public static final ResourceLocation DriftId = Scp1730RoomsRegistry.location("scp1730_drift_gravity");
    public static final ResourceLocation SlowMobId = Scp1730RoomsRegistry.location("scp1730_slow_mob");
    public static final ResourceLocation SlowFlyId = Scp1730RoomsRegistry.location("scp1730_slow_fly");
    public static final ResourceLocation SlowPlayerId = Scp1730RoomsRegistry.location("scp1730_slow_player");
    public static final double DriftGravity = -0.75;
    public static final double SlowMob = -0.65;
    public static final double SlowPlayer = -0.15;
    public static final double SlowProjectile = 0.35;
    public static final String SlowedTag = "scp1730_slowed";
    public static final String FloatTag = "scp1730_floating";
    public static final int LoopMin = 3;
    public static final int LoopMax = 5;
    public static final long LoopPass = 6000L;
    public static final int TeleportRest = 8;
    public static final double DoorReach = 1.6;
    private static final Map<UUID, Vec3> Last = new ConcurrentHashMap<>();
    private static final Map<UUID, LoopVisit> Loops = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> Rested = new ConcurrentHashMap<>();
    private static final Set<UUID> Hushed = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ServerLevel> Touched = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerLevel> Shots = new ConcurrentHashMap<>();

    public static final class LoopVisit {
        public final Scp1730SpecialRoom Room;
        public int Entry = -1;
        public int Count;
        public int Limit;
        public long PassUntil;

        LoopVisit(Scp1730SpecialRoom Room, int Limit) {
            this.Room = Room;
            this.Limit = Limit;
        }
    }

    private Scp1730RoomRules() {
    }

    public static void tick(ServerLevel World) {
        List<Scp1730SpecialRoom> Rooms = Scp1730SpecialRooms.of(World);
        long Now = World.getGameTime();
        for (ServerPlayer User : World.players()) {
            player(World, User, Rooms, Now);
        }
        if (!Rooms.isEmpty()) {
            mobs(World, Rooms);
        }
    }

    public static void player(ServerLevel World, ServerPlayer User, List<Scp1730SpecialRoom> Rooms, long Now) {
        Vec3 Here = User.position();
        Vec3 Before = Last.put(User.getUUID(), Here);
        Scp1730SpecialRoom In = null;
        for (Scp1730SpecialRoom Each : Rooms) {
            if (Each.contains(Here.x, Here.y + 0.1, Here.z)) {
                In = Each;
                break;
            }
        }
        boolean Silent = In != null && In.Kind() == Scp1730RoomKind.Silent;
        if (Silent != Hushed.contains(User.getUUID())) {
            if (Silent) {
                Hushed.add(User.getUUID());
            } else {
                Hushed.remove(User.getUUID());
            }
            VoiceGate.mark(VoiceReason, User.getUUID(), Silent);
            VoiceGate.deafen(VoiceReason, User.getUUID(), Silent);
        }
        boolean Spectating = User.isSpectator();
        setModifier(User, Attributes.GRAVITY, DriftId, !Spectating && In != null && In.Kind() == Scp1730RoomKind.Drift ? DriftGravity : 0.0);
        setModifier(User, Attributes.MOVEMENT_SPEED, SlowPlayerId, !Spectating && In != null && In.Kind() == Scp1730RoomKind.Slow ? SlowPlayer : 0.0);
        if (In != null && In.Kind() == Scp1730RoomKind.Drift) {
            User.getPersistentData().putBoolean(FloatTag, true);
        } else if (User.onGround() || User.isInWater()) {
            User.getPersistentData().remove(FloatTag);
        }
        if (Before == null || Spectating || !User.isAlive() || Now < Rested.getOrDefault(User.getUUID(), 0L)) {
            return;
        }
        loop(World, User, Rooms, Before, Here, Now);
        vault(World, User, Rooms, Before, Here, Now);
    }

    static void loop(ServerLevel World, ServerPlayer User, List<Scp1730SpecialRoom> Rooms, Vec3 Before, Vec3 Here, long Now) {
        LoopVisit Visit = Loops.get(User.getUUID());
        Scp1730SpecialRoom Room = null;
        for (Scp1730SpecialRoom Each : Rooms) {
            if (Each.Kind() == Scp1730RoomKind.Loop && Each.contains(Here.x, Here.y + 0.1, Here.z)) {
                Room = Each;
                break;
            }
        }
        if (Visit != null && Visit.Room != Room && Room != null) {
            Visit = null;
        }
        if (Room == null) {
            if (Visit != null && Now > Visit.PassUntil && Visit.PassUntil > 0L) {
                Loops.remove(User.getUUID());
            } else if (Visit != null && Visit.PassUntil == 0L) {
                Visit.Entry = -1;
            }
            return;
        }
        if (Visit == null) {
            Visit = new LoopVisit(Room, LoopMin + World.random.nextInt(LoopMax - LoopMin + 1));
            Loops.put(User.getUUID(), Visit);
        }
        if (Visit.Entry < 0 && Room.within(Here)) {
            Visit.Entry = nearestDoor(Room, Here);
        }
        if (Visit.Entry < 0) {
            return;
        }
        for (int Index = 0; Index < Room.Doors().size(); Index++) {
            Scp1730SpecialRoom.Door Door = Room.Doors().get(Index);
            if (Index == Visit.Entry || !crossedOut(Door, Before, Here)) {
                continue;
            }
            if (Now < Visit.PassUntil) {
                return;
            }
            if (Visit.Count >= Visit.Limit) {
                Visit.PassUntil = Now + LoopPass;
                return;
            }
            Scp1730SpecialRoom.Door Entry = Room.Doors().get(Visit.Entry);
            int Turns = Math.floorMod(Entry.Dir() + 2 - Door.Dir(), 4);
            if (carry(World, User, Door, Entry, Room.Floor(), Room.Floor(), Turns, Now)) {
                Visit.Count++;
                if (Visit.Count >= Visit.Limit) {
                    Visit.PassUntil = Now + LoopPass;
                }
            }
            return;
        }
    }

    static void vault(ServerLevel World, ServerPlayer User, List<Scp1730SpecialRoom> Rooms, Vec3 Before, Vec3 Here, long Now) {
        Scp1730SpecialRoom Small = null;
        Scp1730SpecialRoom Hall = null;
        for (Scp1730SpecialRoom Each : Rooms) {
            if (Each.Kind() == Scp1730RoomKind.Vault && Small == null) {
                Small = Each;
            } else if (Each.Kind() == Scp1730RoomKind.VaultHall && Hall == null) {
                Hall = Each;
            }
        }
        if (Small == null || Hall == null || Small.Doors().isEmpty() || Hall.Doors().isEmpty()) {
            return;
        }
        Scp1730SpecialRoom.Door Outer = Small.Doors().getFirst();
        Scp1730SpecialRoom.Door Inner = Hall.Doors().getFirst();
        if (crossedIn(Outer, Before, Here, Small.Floor())) {
            carry(World, User, Outer, Inner, Small.Floor(), Hall.Floor(), Math.floorMod(Inner.Dir() - Outer.Dir(), 4), Now);
        } else if (crossedOut(Inner, Before, Here) && Math.abs(Here.y - (Hall.Floor() + 1)) < 2.5) {
            carry(World, User, Inner, Outer, Hall.Floor(), Small.Floor(), Math.floorMod(Outer.Dir() - Inner.Dir(), 4), Now);
        }
    }

    public static int nearestDoor(Scp1730SpecialRoom Room, Vec3 Point) {
        int Best = -1;
        double BestDistance = Double.MAX_VALUE;
        for (int Index = 0; Index < Room.Doors().size(); Index++) {
            Scp1730SpecialRoom.Door Door = Room.Doors().get(Index);
            double Distance = Point.distanceToSqr(Door.X() + 0.5, Point.y, Door.Z() + 0.5);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Index;
            }
        }
        return Best;
    }

    public static boolean crossedOut(Scp1730SpecialRoom.Door Door, Vec3 Before, Vec3 Here) {
        return Door.outward(Before) <= 0.0 && Door.outward(Here) > 0.0 && Math.abs(Door.lateral(Here)) <= DoorReach;
    }

    public static boolean crossedIn(Scp1730SpecialRoom.Door Door, Vec3 Before, Vec3 Here, int Floor) {
        return Door.outward(Before) > 0.0 && Door.outward(Here) <= 0.0 && Math.abs(Door.lateral(Here)) <= DoorReach && Math.abs(Here.y - (Floor + 1)) < 2.5;
    }

    public static Vec3 turn(Vec3 Vector, int Turns) {
        Vec3 Made = Vector;
        for (int Step = 0; Step < Turns; Step++) {
            Made = new Vec3(-Made.z, Made.y, Made.x);
        }
        return Made;
    }

    public static Vec3 map(Vec3 Point, Scp1730SpecialRoom.Door From, Scp1730SpecialRoom.Door To, int FromFloor, int ToFloor, int Turns) {
        Vec3 Offset = Point.subtract(From.X() + 0.5, FromFloor, From.Z() + 0.5);
        Vec3 Turned = turn(Offset, Turns);
        return new Vec3(To.X() + 0.5 + Turned.x, ToFloor + Turned.y, To.Z() + 0.5 + Turned.z);
    }

    static boolean carry(ServerLevel World, ServerPlayer User, Scp1730SpecialRoom.Door From, Scp1730SpecialRoom.Door To, int FromFloor, int ToFloor, int Turns, long Now) {
        Vec3 Target = map(User.position(), From, To, FromFloor, ToFloor, Turns);
        AABB Body = User.getBoundingBox().move(Target.subtract(User.position()));
        if (!World.noCollision(User, Body)) {
            return false;
        }
        Vec3 Motion = turn(User.getDeltaMovement(), Turns);
        float Yaw = User.getYRot() + Turns * 90.0f;
        User.teleportTo(World, Target.x, Target.y, Target.z, Yaw, User.getXRot());
        User.setYHeadRot(Yaw);
        User.setDeltaMovement(Motion);
        User.hurtMarked = true;
        Last.put(User.getUUID(), Target);
        Rested.put(User.getUUID(), Now + TeleportRest);
        World.playSound(null, Target.x, Target.y, Target.z, Scp1730RoomsRegistry.LoopShift.get(), SoundSource.AMBIENT, 0.25f, 0.9f + World.random.nextFloat() * 0.2f);
        return true;
    }

    static void mobs(ServerLevel World, List<Scp1730SpecialRoom> Rooms) {
        Set<UUID> Seen = new HashSet<>();
        for (Scp1730SpecialRoom Room : Rooms) {
            boolean Drift = Room.Kind() == Scp1730RoomKind.Drift;
            boolean Slow = Room.Kind() == Scp1730RoomKind.Slow;
            if (!Drift && !Slow) {
                continue;
            }
            AABB Box = Room.box();
            for (Entity Each : World.getEntities((Entity) null, Box, Candidate -> !(Candidate instanceof Player))) {
                if (!Room.contains(Each.getX(), Each.getY() + 0.1, Each.getZ())) {
                    continue;
                }
                if (Each instanceof LivingEntity Body) {
                    Seen.add(Body.getUUID());
                    Touched.put(Body.getUUID(), World);
                    setModifier(Body, Attributes.GRAVITY, DriftId, Drift ? DriftGravity : 0.0);
                    setModifier(Body, Attributes.MOVEMENT_SPEED, SlowMobId, Slow ? SlowMob : 0.0);
                    setModifier(Body, Attributes.FLYING_SPEED, SlowFlyId, Slow ? SlowMob : 0.0);
                    if (Drift) {
                        Body.getPersistentData().putBoolean(FloatTag, true);
                    }
                } else if (Each instanceof Projectile Shot && Slow) {
                    slowShot(Shot);
                }
            }
        }
        Iterator<Map.Entry<UUID, ServerLevel>> Walk = Touched.entrySet().iterator();
        while (Walk.hasNext()) {
            Map.Entry<UUID, ServerLevel> Entry = Walk.next();
            if (Entry.getValue() != World || Seen.contains(Entry.getKey())) {
                continue;
            }
            Entity Found = World.getEntity(Entry.getKey());
            if (Found instanceof LivingEntity Body) {
                clear(Body);
            }
            Walk.remove();
        }
        Iterator<Map.Entry<UUID, ServerLevel>> Flights = Shots.entrySet().iterator();
        while (Flights.hasNext()) {
            Map.Entry<UUID, ServerLevel> Entry = Flights.next();
            if (Entry.getValue() != World) {
                continue;
            }
            if (!(World.getEntity(Entry.getKey()) instanceof Projectile Shot) || !Shot.isAlive()) {
                Flights.remove();
                continue;
            }
            if (Scp1730SpecialRooms.at(World, Shot.position(), Scp1730RoomKind.Slow) == null) {
                Shot.getPersistentData().remove(SlowedTag);
                Shot.setDeltaMovement(Shot.getDeltaMovement().scale(1.0 / SlowProjectile));
                Shot.hurtMarked = true;
                Flights.remove();
            } else {
                double Gravity = Shot.getGravity();
                if (Gravity > 0.0 && !Shot.isNoGravity()) {
                    Shot.setDeltaMovement(Shot.getDeltaMovement().add(0.0, Gravity * (1.0 - SlowProjectile * SlowProjectile), 0.0));
                }
            }
        }
    }

    public static void slowShot(Projectile Shot) {
        if (Shot.getPersistentData().getBoolean(SlowedTag)) {
            return;
        }
        if (Shot.level() instanceof ServerLevel World) {
            Shots.put(Shot.getUUID(), World);
        }
        Shot.getPersistentData().putBoolean(SlowedTag, true);
        Shot.setDeltaMovement(Shot.getDeltaMovement().scale(SlowProjectile));
        Shot.hurtMarked = true;
    }

    public static void clear(LivingEntity Body) {
        setModifier(Body, Attributes.GRAVITY, DriftId, 0.0);
        setModifier(Body, Attributes.MOVEMENT_SPEED, SlowMobId, 0.0);
        setModifier(Body, Attributes.FLYING_SPEED, SlowFlyId, 0.0);
        setModifier(Body, Attributes.MOVEMENT_SPEED, SlowPlayerId, 0.0);
    }

    public static void setModifier(LivingEntity Body, Holder<Attribute> Kind, ResourceLocation Id, double Amount) {
        AttributeInstance Found = Body.getAttribute(Kind);
        if (Found == null) {
            return;
        }
        AttributeModifier Current = Found.getModifier(Id);
        if (Amount == 0.0) {
            if (Current != null) {
                Found.removeModifier(Id);
            }
            return;
        }
        if (Current != null && Current.amount() == Amount) {
            return;
        }
        Found.removeModifier(Id);
        Found.addTransientModifier(new AttributeModifier(Id, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static boolean cushioned(LivingEntity Body) {
        if (Body.getPersistentData().getBoolean(FloatTag)) {
            return true;
        }
        Scp1730SpecialRoom Here = Scp1730SpecialRooms.at(Body.level(), Body.position(), Scp1730RoomKind.Drift);
        return Here != null;
    }

    public static void landed(LivingEntity Body) {
        Body.getPersistentData().remove(FloatTag);
    }

    @Nullable
    public static LoopVisit loopOf(Player User) {
        return Loops.get(User.getUUID());
    }

    public static void forget(ServerPlayer User) {
        Last.remove(User.getUUID());
        Loops.remove(User.getUUID());
        Rested.remove(User.getUUID());
        if (Hushed.remove(User.getUUID())) {
            VoiceGate.mark(VoiceReason, User.getUUID(), false);
            VoiceGate.deafen(VoiceReason, User.getUUID(), false);
        }
        clear(User);
    }

    public static void reset() {
        Last.clear();
        Loops.clear();
        Rested.clear();
        for (UUID Each : Hushed) {
            VoiceGate.mark(VoiceReason, Each, false);
            VoiceGate.deafen(VoiceReason, Each, false);
        }
        Hushed.clear();
        Touched.clear();
        Shots.clear();
    }

    public static Map<UUID, LoopVisit> loops() {
        return new HashMap<>(Loops);
    }
}
