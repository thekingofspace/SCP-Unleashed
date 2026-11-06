package net.rai.scp_unleashed.scp087;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp087.network.Scp087ShiftPayload;
import net.rai.scp_unleashed.scp087.network.Scp087StatePayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp087Descent {
    public static final Logger Log = LogUtils.getLogger();
    public static final int MaxParty = 6;
    public static final double JoinReach = 14.0;
    public static final int SweepEvery = 10;
    public static final int InertWarpDelay = 50;
    public static final double MergeSpan = 16.0;
    public static final double DoorstepOut = 1.6;
    public static final double CarryReach = 16.0;
    public static final double SafeMargin = 6.0;
    private static final Map<Integer, Scp087Instance> Instances = new LinkedHashMap<>();
    private static final Map<UUID, Scp087Visit> Visits = new HashMap<>();
    private static final Set<UUID> Moving = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> LiveTokens = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> Evicting = ConcurrentHashMap.newKeySet();
    private static long InertWarpAt = -1L;

    private Scp087Descent() {
    }

    public static boolean isLive(@Nullable UUID Token) {
        return Token != null && LiveTokens.contains(Token);
    }

    @Nullable
    public static Scp087Visit visitOf(Player User) {
        return Visits.get(User.getUUID());
    }

    @Nullable
    public static Scp087Instance instanceAt(int Slot) {
        return Instances.get(Slot);
    }

    public static Collection<Scp087Instance> instances() {
        return Instances.values();
    }

    public static boolean isMoving(Player User) {
        return Moving.contains(User.getUUID());
    }

    public static boolean ending() {
        return InertWarpAt >= 0L;
    }

    public static Vec3 doorstep(BlockPos Lower, Direction Front, double Out) {
        return new Vec3(Lower.getX() + 0.5 + Front.getStepX() * Out, Lower.getY(), Lower.getZ() + 0.5 + Front.getStepZ() * Out);
    }

    @Nullable
    private static ServerPlayer player(MinecraftServer Server, UUID Id) {
        return Server.getPlayerList().getPlayer(Id);
    }

    public static List<ServerPlayer> members(MinecraftServer Server, Scp087Instance Instance) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (UUID Id : Instance.Members) {
            ServerPlayer User = player(Server, Id);
            if (User != null && User.isAlive() && Scp087Stairwell.inStairwell(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static void move(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw, float Pitch) {
        Moving.add(User.getUUID());
        try {
            User.stopRiding();
            if (User.isFallFlying()) {
                User.stopFallFlying();
            }
            User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, Pitch);
            User.setDeltaMovement(Vec3.ZERO);
            User.resetFallDistance();
        } finally {
            Moving.remove(User.getUUID());
        }
    }

    public static void enterByDoor(ServerPlayer User, BlockPos Lower, Direction Front) {
        ServerLevel From = User.serverLevel();
        if (Scp087Stairwell.level(User.server) == null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp087.no_stairwell").withStyle(ChatFormatting.RED), true);
            return;
        }
        Scp087Sounds.door(From, Vec3.atCenterOf(Lower.above()), 0.85f);
        Scp087World.get(User.server).setReturn(User.getUUID(), new Scp087World.Spot(From.dimension(), doorstep(Lower, Front, DoorstepOut), Front.toYRot()));
        enter(User);
    }

    public static boolean enter(ServerPlayer User) {
        ServerLevel Stairwell = Scp087Stairwell.level(User.server);
        if (Stairwell == null) {
            return false;
        }
        if (Visits.containsKey(User.getUUID())) {
            endVisit(User.server, User.getUUID());
        }
        if (Scp087World.get(User.server).returnOf(User.getUUID()) == null && !Scp087Stairwell.inStairwell(User)) {
            Scp087World.get(User.server).setReturn(User.getUUID(), new Scp087World.Spot(User.serverLevel().dimension(), User.position(), User.getYRot()));
        }
        Scp087Instance Joined = joinable(Stairwell);
        if (Joined == null) {
            Joined = claim(Stairwell, -1);
        }
        Scp087Visit Visit = new Scp087Visit(User.getUUID(), Joined);
        long Now = Stairwell.getGameTime();
        Visit.NextFace = Now + Scp087Encounters.FaceFirstDelay;
        Visit.NextAmbience = Now + Scp087Encounters.AmbienceFirstDelay;
        Visits.put(User.getUUID(), Visit);
        Joined.Members.add(User.getUUID());
        Vec3 Arrival = Scp087Stairwell.arrival(Joined.Slot);
        move(User, Stairwell, Arrival, 0.0f, 8.0f);
        Scp087Sounds.door(Stairwell, new Vec3(Joined.originX() + Scp087Layout.DoorX + 0.5, Scp087Layout.WalkTop + 1.0, 0.8), 0.8f);
        sync(User, Visit, true);
        return true;
    }

    @Nullable
    private static Scp087Instance joinable(ServerLevel Stairwell) {
        MinecraftServer Server = Stairwell.getServer();
        for (Scp087Instance Candidate : Instances.values()) {
            if (Candidate.Ending || Candidate.Wraps != 0 || Candidate.Members.isEmpty() || Candidate.Members.size() >= MaxParty) {
                continue;
            }
            Vec3 Arrival = Scp087Stairwell.arrival(Candidate.Slot);
            boolean Close = true;
            for (UUID Id : Candidate.Members) {
                ServerPlayer Member = player(Server, Id);
                if (Member == null || !Scp087Stairwell.inStairwell(Member) || Member.position().distanceTo(Arrival) > JoinReach) {
                    Close = false;
                    break;
                }
            }
            if (Close) {
                return Candidate;
            }
        }
        return null;
    }

    private static Scp087Instance claim(ServerLevel Stairwell, int Near) {
        int Slot = freeSlot(Near);
        ensureBuilt(Stairwell, Slot);
        clearSlot(Stairwell, Slot);
        Scp087Instance Created = new Scp087Instance(Slot);
        Created.HunterReady = Stairwell.getGameTime() + Scp087Encounters.HunterGrace;
        Instances.put(Slot, Created);
        LiveTokens.add(Created.Token);
        return Created;
    }

    private static int freeSlot(int Near) {
        if (Near < 0) {
            int Slot = 0;
            while (Instances.containsKey(Slot)) {
                Slot++;
            }
            return Slot;
        }
        for (int Gap = 1; ; Gap++) {
            if (Near - Gap >= 0 && !Instances.containsKey(Near - Gap)) {
                return Near - Gap;
            }
            if (!Instances.containsKey(Near + Gap)) {
                return Near + Gap;
            }
        }
    }

    public static void ensureBuilt(ServerLevel Stairwell, int Slot) {
        Scp087World Data = Scp087World.get(Stairwell.getServer());
        int Wanted = Scp087Layout.signature(Data.inert());
        if (Data.builtSignature(Slot) != Wanted) {
            int Changed = Scp087Layout.build(Stairwell, Slot, Data.inert());
            Data.markBuilt(Slot, Wanted);
            Log.debug("SCP-087 slot {} built ({} blocks changed)", Slot, Changed);
        }
    }

    private static void clearSlot(ServerLevel Stairwell, int Slot) {
        for (Entity Found : Stairwell.getEntities((Entity) null, Scp087Stairwell.box(Slot), Candidate -> !(Candidate instanceof Player))) {
            evict(Stairwell.getServer(), Found);
        }
    }

    public static void evict(MinecraftServer Server, Entity Found) {
        if (Found.isRemoved()) {
            return;
        }
        if (Found instanceof LivingEntity Living && Living.isAlive() && !(Found instanceof Scp087Hunter) && ControlBodies.isPersonLike(Found)) {
            relocate(Server, Found);
            return;
        }
        Found.discard();
    }

    public static void relocate(MinecraftServer Server, Entity Found) {
        if (Found.isRemoved() || Found instanceof Player || Found instanceof LivingEntity Living && !Living.isAlive() || !Evicting.add(Found.getUUID())) {
            return;
        }
        try {
            Scp087World.Spot Where = entranceSpot(Server);
            ServerLevel Target = Server.getLevel(Where.Dimension());
            if (Target == null) {
                Target = Server.overworld();
            }
            Vec3 Jitter = new Vec3((Found.getRandom().nextDouble() - 0.5) * 1.5, 0.0, (Found.getRandom().nextDouble() - 0.5) * 1.5);
            Found.stopRiding();
            Found.ejectPassengers();
            Found.changeDimension(new DimensionTransition(Target, Where.Pos().add(Jitter), Vec3.ZERO, Where.Yaw(), 0.0f, DimensionTransition.DO_NOTHING));
        } finally {
            Evicting.remove(Found.getUUID());
        }
    }

    public static Scp087World.Spot entranceSpot(MinecraftServer Server) {
        Scp087World Data = Scp087World.get(Server);
        BlockPos Door = Data.door();
        if (Door != null && Server.getLevel(Data.doorDimension()) != null) {
            return new Scp087World.Spot(Data.doorDimension(), doorstep(Door, Data.front(), DoorstepOut), Data.front().toYRot());
        }
        ServerLevel Home = Server.overworld();
        BlockPos Spawn = Home.getSharedSpawnPos();
        BlockPos Top = Home.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Spawn);
        return new Scp087World.Spot(Home.dimension(), Vec3.atBottomCenterOf(Top), 0.0f);
    }

    public static Scp087World.Spot outside(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Scp087World Data = Scp087World.get(Server);
        Scp087World.Spot Saved = Data.returnOf(User.getUUID());
        if (Saved != null && Server.getLevel(Saved.Dimension()) != null && Saved.Dimension() != Scp087Stairwell.Key) {
            return Saved;
        }
        if (Data.door() != null && Server.getLevel(Data.doorDimension()) != null) {
            return entranceSpot(Server);
        }
        DimensionTransition Home = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        return new Scp087World.Spot(Home.newLevel().dimension(), Home.pos(), Home.yRot());
    }

    public static void exitByDoor(ServerPlayer User) {
        ServerLevel Here = User.serverLevel();
        Scp087Sounds.door(Here, User.position().add(0.0, 1.0, 0.0), 0.85f);
        leave(User, null);
    }

    public static void leave(ServerPlayer User, @Nullable Component Message) {
        endVisit(User.server, User.getUUID());
        Scp087World.Spot Spot = outside(User);
        ServerLevel Target = User.server.getLevel(Spot.Dimension());
        if (Target == null) {
            Target = User.server.overworld();
        }
        if (!User.isAlive()) {
            return;
        }
        move(User, Target, Spot.Pos(), Spot.Yaw(), 0.0f);
        Scp087World.get(User.server).clearReturn(User.getUUID());
        Scp087Sounds.play(Target, Spot.Pos().add(0.0, 1.0, 0.0), Scp087Registry.DoorShut, SoundSource.BLOCKS, 0.9f, 0.85f);
        PacketDistributor.sendToPlayer(User, new Scp087StatePayload(false, 0, Scp087World.get(User.server).inert()));
        if (Message != null) {
            User.displayClientMessage(Message, true);
        }
    }

    public static void endVisit(MinecraftServer Server, UUID Id) {
        Scp087Visit Visit = Visits.remove(Id);
        if (Visit == null) {
            return;
        }
        Visit.Instance.Members.remove(Id);
        ServerLevel Stairwell = Scp087Stairwell.level(Server);
        if (Stairwell != null && Visit.Face != null && Stairwell.getEntity(Visit.Face) instanceof Scp087Face Face) {
            Face.discard();
        }
    }

    private static void teardown(ServerLevel Stairwell, Scp087Instance Instance) {
        Instances.remove(Instance.Slot);
        LiveTokens.remove(Instance.Token);
        clearSlot(Stairwell, Instance.Slot);
    }

    public static void tick(MinecraftServer Server) {
        Scp087Sounds.tick();
        ServerLevel Stairwell = Scp087Stairwell.level(Server);
        if (Stairwell == null) {
            Instances.clear();
            Visits.clear();
            LiveTokens.clear();
            return;
        }
        long Now = Stairwell.getGameTime();
        Scp087World Data = Scp087World.get(Server);
        if (InertWarpAt >= 0L && Now >= InertWarpAt) {
            InertWarpAt = -1L;
            warpAllOut(Server);
        }
        for (Scp087Visit Visit : new ArrayList<>(Visits.values())) {
            ServerPlayer User = player(Server, Visit.Visitor);
            if (User == null || !Scp087Stairwell.inStairwell(User) || Instances.get(Visit.Instance.Slot) != Visit.Instance) {
                if (User == null || !Moving.contains(User.getUUID())) {
                    endVisit(Server, Visit.Visitor);
                }
            }
        }
        for (Scp087Instance Instance : new ArrayList<>(Instances.values())) {
            Instance.Members.removeIf(Id -> Visits.get(Id) == null || Visits.get(Id).Instance != Instance);
            if (Instance.Members.isEmpty()) {
                teardown(Stairwell, Instance);
            }
        }
        if (!Data.inert()) {
            merge(Stairwell);
            for (Scp087Instance Instance : new ArrayList<>(Instances.values())) {
                if (!Instance.Ending) {
                    loop(Stairwell, Instance);
                }
            }
        }
        for (Scp087Instance Instance : new ArrayList<>(Instances.values())) {
            List<ServerPlayer> Members = members(Server, Instance);
            for (ServerPlayer User : Members) {
                Scp087Visit Visit = Visits.get(User.getUUID());
                if (Visit == null) {
                    continue;
                }
                Visit.Flights = Data.inert() ? Scp087Layout.flights(User.getY(), 0) : Scp087Layout.flights(User.getY(), Instance.Wraps);
                sync(User, Visit, false);
                if (!Data.inert() && !Instance.Ending) {
                    Scp087Encounters.tickMember(Stairwell, Instance, Visit, User, Now);
                }
            }
            if (!Data.inert() && !Instance.Ending) {
                Scp087Encounters.tickInstance(Stairwell, Instance, Members, Now);
            }
        }
        if (Server.getTickCount() % SweepEvery == 0) {
            for (ServerPlayer Stray : new ArrayList<>(Stairwell.players())) {
                if (Stray.isAlive() && !Visits.containsKey(Stray.getUUID()) && !Moving.contains(Stray.getUUID()) && !Scp087Rules.exempt(Stray) && !Stray.isSpectator()) {
                    leave(Stray, Component.translatable("message.scp_unleashed.scp087.outside").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        }
    }

    private static void loop(ServerLevel Stairwell, Scp087Instance Instance) {
        List<ServerPlayer> Members = members(Stairwell.getServer(), Instance);
        if (Members.isEmpty()) {
            return;
        }
        ServerPlayer Lowest = Members.get(0);
        double Low = Double.MAX_VALUE;
        double High = -Double.MAX_VALUE;
        for (ServerPlayer User : Members) {
            if (User.getY() < Low) {
                Low = User.getY();
                Lowest = User;
            }
            High = Math.max(High, User.getY());
        }
        if (Members.size() > 1 && High - Low > Scp087Layout.SplitSpan) {
            split(Stairwell, Instance, Lowest);
            return;
        }
        if (Low < Scp087Layout.WrapLow) {
            Instance.Wraps++;
            shift(Stairwell, Members, carried(Stairwell, Instance), 0.0, Scp087Layout.Period);
        } else if (High > Scp087Layout.WrapHigh && Instance.Wraps > 0) {
            Instance.Wraps--;
            shift(Stairwell, Members, carried(Stairwell, Instance), 0.0, -Scp087Layout.Period);
        }
    }

    private static List<Entity> carried(ServerLevel Stairwell, Scp087Instance Instance) {
        return Stairwell.getEntities((Entity) null, Scp087Stairwell.box(Instance.Slot), Candidate -> !(Candidate instanceof Player) && !Candidate.isRemoved());
    }

    public static void shift(ServerLevel Stairwell, List<ServerPlayer> Riders, List<Entity> Carried, double Dx, double Dy) {
        List<Entity> Snapped = new ArrayList<>();
        for (Entity Found : Carried) {
            double Landed = Found.getY() + Dy;
            while (Dy != 0.0 && Landed > Scp087Layout.TopFloor - SafeMargin) {
                Landed -= Scp087Layout.Period;
            }
            while (Dy != 0.0 && Landed < Scp087Layout.BottomFloor + SafeMargin) {
                Landed += Scp087Layout.Period;
            }
            if (Landed == Found.getY() + Dy) {
                Snapped.add(Found);
            }
            Found.moveTo(Found.getX() + Dx, Landed, Found.getZ(), Found.getYRot(), Found.getXRot());
            if (Found instanceof Mob Body) {
                Body.getNavigation().stop();
            }
            if (Found instanceof Scp087Hunter Hunter) {
                Hunter.shifted(Dx, Dy);
            }
        }
        for (ServerPlayer Rider : Riders) {
            Moving.add(Rider.getUUID());
            try {
                Rider.connection.teleport(Rider.getX() + Dx, Rider.getY() + Dy, Rider.getZ(), Rider.getYRot(), Rider.getXRot(), EnumSet.allOf(RelativeMovement.class));
            } finally {
                Moving.remove(Rider.getUUID());
            }
        }
        for (ServerPlayer Rider : Riders) {
            List<Integer> Ids = new ArrayList<>();
            for (Entity Found : Snapped) {
                if (Ids.size() < Scp087ShiftPayload.MaxIds) {
                    Ids.add(Found.getId());
                }
            }
            for (ServerPlayer Other : Riders) {
                if (Other != Rider && Ids.size() < Scp087ShiftPayload.MaxIds) {
                    Ids.add(Other.getId());
                }
            }
            PacketDistributor.sendToPlayer(Rider, new Scp087ShiftPayload(Dx, Dy, Ids));
        }
    }

    private static void split(ServerLevel Stairwell, Scp087Instance Instance, ServerPlayer Leaving) {
        Scp087Visit Visit = Visits.get(Leaving.getUUID());
        if (Visit == null) {
            return;
        }
        Scp087Instance Fresh = claim(Stairwell, Instance.Slot);
        Fresh.Wraps = Instance.Wraps;
        Fresh.HunterReady = Instance.HunterReady;
        List<ServerPlayer> Others = members(Stairwell.getServer(), Instance);
        Others.remove(Leaving);
        List<Entity> Carried = new ArrayList<>();
        for (Entity Found : carried(Stairwell, Instance)) {
            double Mine = Found.distanceToSqr(Leaving);
            if (Mine > CarryReach * CarryReach) {
                continue;
            }
            boolean Closest = true;
            for (ServerPlayer Other : Others) {
                if (Other.distanceToSqr(Found) <= Mine) {
                    Closest = false;
                    break;
                }
            }
            if (Found instanceof Scp087Hunter Hunter && Hunter.getTarget() == Leaving) {
                Closest = true;
            }
            if (Closest) {
                Carried.add(Found);
            }
        }
        for (Entity Found : Carried) {
            retag(Found, Instance, Fresh);
        }
        Instance.Members.remove(Leaving.getUUID());
        Fresh.Members.add(Leaving.getUUID());
        Visit.Instance = Fresh;
        shift(Stairwell, List.of(Leaving), Carried, Fresh.originX() - Instance.originX(), 0.0);
        Log.debug("SCP-087 party split: {} moved from slot {} to {}", Leaving.getGameProfile().getName(), Instance.Slot, Fresh.Slot);
    }

    private static void retag(Entity Found, Scp087Instance From, Scp087Instance To) {
        if (Found instanceof Scp087Hunter Hunter) {
            Hunter.bind(To.Token);
            if (Hunter.getUUID().equals(From.Hunter)) {
                From.Hunter = null;
            }
            if (To.Hunter == null) {
                To.Hunter = Hunter.getUUID();
            }
        } else if (Found instanceof Scp087Face Face) {
            Face.bind(To.Token);
        }
    }

    private static void merge(ServerLevel Stairwell) {
        List<Scp087Instance> All = new ArrayList<>(Instances.values());
        for (Scp087Instance Keep : All) {
            for (Scp087Instance Join : All) {
                if (Keep == Join || Keep.Ending || Join.Ending || Instances.get(Keep.Slot) != Keep || Instances.get(Join.Slot) != Join) {
                    continue;
                }
                if (Keep.Members.size() + Join.Members.size() > MaxParty || Keep.Slot > Join.Slot) {
                    continue;
                }
                if (canMerge(Stairwell, Keep, Join)) {
                    absorb(Stairwell, Keep, Join);
                }
            }
        }
    }

    private static boolean canMerge(ServerLevel Stairwell, Scp087Instance Keep, Scp087Instance Join) {
        List<ServerPlayer> Stay = members(Stairwell.getServer(), Keep);
        List<ServerPlayer> Come = members(Stairwell.getServer(), Join);
        if (Stay.isEmpty() || Come.isEmpty()) {
            return false;
        }
        double Low = Double.MAX_VALUE;
        double High = -Double.MAX_VALUE;
        for (ServerPlayer User : Stay) {
            double Depth = Scp087Layout.depthBlocks(User.getY(), Keep.Wraps);
            Low = Math.min(Low, Depth);
            High = Math.max(High, Depth);
        }
        for (ServerPlayer User : Come) {
            double Depth = Scp087Layout.depthBlocks(User.getY(), Join.Wraps);
            Low = Math.min(Low, Depth);
            High = Math.max(High, Depth);
        }
        if (High - Low > MergeSpan) {
            return false;
        }
        double Dx = Keep.originX() - Join.originX();
        double Dy = (double) (Keep.Wraps - Join.Wraps) * Scp087Layout.Period;
        for (ServerPlayer Newcomer : Come) {
            Vec3 Eye = Newcomer.getEyePosition().add(Dx, Dy, 0.0);
            if (Eye.y < Scp087Layout.BottomFloor + 8 || Eye.y > Scp087Layout.Ceiling - 1) {
                return false;
            }
            for (ServerPlayer Resident : Stay) {
                Vec3 Other = Resident.getEyePosition();
                double Gap = Eye.distanceTo(Other);
                Scp087Visit Seen = Visits.get(Resident.getUUID());
                float Fog = Scp087Darkness.fogFar(Seen == null ? 0 : Seen.Flights, false) + 2.0f;
                if (Gap < Fog && Stairwell.clip(new ClipContext(Other, Eye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Resident)).getType() == HitResult.Type.MISS) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void absorb(ServerLevel Stairwell, Scp087Instance Keep, Scp087Instance Join) {
        double Dx = Keep.originX() - Join.originX();
        double Dy = (double) (Keep.Wraps - Join.Wraps) * Scp087Layout.Period;
        List<ServerPlayer> Come = members(Stairwell.getServer(), Join);
        List<Entity> Carried = carried(Stairwell, Join);
        List<Entity> Kept = new ArrayList<>();
        for (Entity Found : Carried) {
            if (Found instanceof Scp087Hunter && Keep.Hunter != null) {
                Found.discard();
                continue;
            }
            retag(Found, Join, Keep);
            Kept.add(Found);
        }
        for (ServerPlayer User : Come) {
            Scp087Visit Visit = Visits.get(User.getUUID());
            if (Visit != null) {
                Visit.Instance = Keep;
                Keep.Members.add(User.getUUID());
            }
        }
        Join.Members.clear();
        Instances.remove(Join.Slot);
        LiveTokens.remove(Join.Token);
        shift(Stairwell, Come, Kept, Dx, Dy);
        Log.debug("SCP-087 parties merged: slot {} into {}", Join.Slot, Keep.Slot);
    }

    public static void sync(ServerPlayer User, Scp087Visit Visit, boolean Force) {
        if (!Force && Visit.Flights == Visit.Synced) {
            return;
        }
        Visit.Synced = Visit.Flights;
        PacketDistributor.sendToPlayer(User, new Scp087StatePayload(true, Visit.Flights, Scp087World.get(User.server).inert()));
    }

    public static boolean setDepth(ServerPlayer User, int Flights) {
        Scp087Visit Visit = Visits.get(User.getUUID());
        if (Visit == null || Scp087World.get(User.server).inert()) {
            return false;
        }
        Scp087Instance Instance = Visit.Instance;
        int Local = Scp087Layout.flights(User.getY(), 0);
        Instance.Wraps = Math.max(0, (Flights - Local + 1) / 2);
        for (UUID Id : Instance.Members) {
            Scp087Visit Other = Visits.get(Id);
            if (Other != null) {
                Other.Deepest = 0;
            }
        }
        return true;
    }

    public static void neutralise(MinecraftServer Server) {
        Scp087World Data = Scp087World.get(Server);
        if (Data.inert()) {
            return;
        }
        ServerLevel Stairwell = Scp087Stairwell.level(Server);
        long Now = Server.overworld().getGameTime();
        Data.setInert(true, Now);
        InertWarpAt = (Stairwell != null ? Stairwell.getGameTime() : Now) + InertWarpDelay;
        for (Scp087Instance Instance : Instances.values()) {
            Instance.Ending = true;
        }
        for (Scp087Visit Visit : Visits.values()) {
            ServerPlayer User = player(Server, Visit.Visitor);
            if (User != null) {
                Visit.Synced = -1;
                sync(User, Visit, true);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp087.silence").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
                if (Stairwell != null) {
                    Scp087Sounds.play(Stairwell, User.position(), Scp087Registry.Silence, SoundSource.AMBIENT, 1.0f, 0.8f);
                }
            }
        }
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Scp087Dossier.refresh(User);
        }
        Log.info("SCP-087 has been neutralised");
    }

    private static void warpAllOut(MinecraftServer Server) {
        for (Scp087Visit Visit : new ArrayList<>(Visits.values())) {
            ServerPlayer User = player(Server, Visit.Visitor);
            if (User == null) {
                endVisit(Server, Visit.Visitor);
                continue;
            }
            leave(User, Component.translatable("message.scp_unleashed.scp087.inert").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        ServerLevel Stairwell = Scp087Stairwell.level(Server);
        if (Stairwell != null) {
            for (Scp087Instance Instance : new ArrayList<>(Instances.values())) {
                teardown(Stairwell, Instance);
            }
        }
        Instances.clear();
        LiveTokens.clear();
    }

    public static void restore(MinecraftServer Server) {
        Scp087World.get(Server).setInert(false, 0L);
        InertWarpAt = -1L;
        for (Scp087Visit Visit : Visits.values()) {
            ServerPlayer User = player(Server, Visit.Visitor);
            if (User != null) {
                sync(User, Visit, true);
            }
        }
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Scp087Dossier.restore(User);
        }
    }

    public static void stopping() {
        Instances.clear();
        Visits.clear();
        LiveTokens.clear();
        Moving.clear();
        Evicting.clear();
        InertWarpAt = -1L;
        Scp087Sounds.clear();
    }

    @Nullable
    public static Scp087Instance instanceFor(Entity Found) {
        if (!Scp087Stairwell.inStairwell(Found)) {
            return null;
        }
        return Instances.get(Scp087Stairwell.slotAt(Found.getX()));
    }
}
