package net.rai.scp_unleashed.scp2521;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp2521.network.Scp2521ScenePayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp2521Trips {
    public static final int StageWorld = 0;
    public static final int StageVoid = 1;
    public static final int StageFall = 2;
    public static final AttributeModifier Pinned = new AttributeModifier(Scp2521Registry.location("scp2521_pinned"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final AttributeModifier Grounded = new AttributeModifier(Scp2521Registry.location("scp2521_grounded"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final double[] Reaches = {3.0, 2.6, 3.5, 2.2, 4.0};
    public static final float[] Turns = {0.0f, 30.0f, -30.0f, 60.0f, -60.0f, 100.0f, -100.0f, 180.0f};
    public static final double GrabReach = 1.7;
    public static final double HoldSlack = 0.08;
    public static final float VoidPitch = -14.0f;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<UUID, Run> Active = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> Cooling = new ConcurrentHashMap<>();

    public static final class Run {
        public final UUID Owner;
        public final ResourceKey<Level> Dimension;
        public final Vec3 Spot;
        public final float Yaw;
        public final float Pitch;
        public final int Plot;
        @Nullable
        public UUID Taker;
        @Nullable
        public UUID Starer;
        @Nullable
        public UUID Tendrils;
        public int Ticks;
        public int Stage = StageWorld;
        public boolean Moving;
        @Nullable
        public Vec3 TakerStart;
        public final List<Entity> Spawned = new ArrayList<>();

        public Run(UUID Owner, ResourceKey<Level> Dimension, Vec3 Spot, float Yaw, float Pitch, int Plot) {
            this.Owner = Owner;
            this.Dimension = Dimension;
            this.Spot = Spot;
            this.Yaw = Yaw;
            this.Pitch = Pitch;
            this.Plot = Plot;
        }

        public Scp2521Trip record() {
            return new Scp2521Trip(true, Dimension.location(), Spot.x, Spot.y, Spot.z, Yaw, Pitch, Plot, Stage, Ticks);
        }
    }

    private Scp2521Trips() {
    }

    @Nullable
    public static Run run(UUID Owner) {
        return Active.get(Owner);
    }

    public static boolean active(UUID Owner) {
        return Active.containsKey(Owner);
    }

    public static List<Run> runs() {
        return new ArrayList<>(Active.values());
    }

    public static int count() {
        return Active.size();
    }

    public static boolean cooling(ServerPlayer User) {
        Long Until = Cooling.get(User.getUUID());
        return Until != null && User.server.overworld().getGameTime() < Until;
    }

    public static void cool(ServerPlayer User) {
        Cooling.put(User.getUUID(), User.server.overworld().getGameTime() + Scp2521Scene.Cooldown);
    }

    public static void uncool(UUID Owner) {
        Cooling.remove(Owner);
    }

    public static boolean eligible(ServerPlayer User) {
        return User.isAlive() && !User.isSpectator() && !User.isRemoved() && !active(User.getUUID()) && !cooling(User) && !Scp2521Void.inside(User)
                && Scp2521Void.level(User.server) != null;
    }

    public static boolean start(ServerPlayer User) {
        if (!eligible(User)) {
            return false;
        }
        MinecraftServer Server = User.server;
        ServerLevel Here = User.serverLevel();
        Scp2521State State = Scp2521State.get(Server);
        int Plot = State.claimPlot(User.getUUID());
        Run Made = new Run(User.getUUID(), Here.dimension(), User.position(), User.getYRot(), User.getXRot(), Plot);
        Vec3 Spot = takerSpot(Here, User);
        Scp2521 Taker = Scp2521Registry.CreatureType.get().create(Here);
        if (Taker == null) {
            State.freePlot(Plot);
            return false;
        }
        Taker.setRole(Scp2521.RoleTaker);
        Taker.setOwner(User.getUUID());
        Taker.setPos(Spot.x, Spot.y, Spot.z);
        Taker.face(User.position());
        Taker.setSquash(squashAt(Here, BlockPos.containing(Spot)));
        Taker.setMode(Scp2521.ModeRise);
        Made.Taker = Taker.getUUID();
        Made.Spawned.add(Taker);
        Made.TakerStart = Spot;
        Active.put(User.getUUID(), Made);
        Here.addFreshEntity(Taker);
        User.closeContainer();
        User.stopRiding();
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        lock(User, true);
        User.setData(Scp2521Registry.TripAttachment, Made.record());
        Taker.play(Scp2521Registry.RiseLayers, 1.0f, 1.0f);
        send(User, Scp2521ScenePayload.Begin, 0, Taker.getId(), Made.Yaw, Made.Pitch);
        return true;
    }

    public static Vec3 takerSpot(ServerLevel World, ServerPlayer User) {
        float Yaw = User.getYRot() * Mth.DEG_TO_RAD;
        for (double Reach : Reaches) {
            for (float Turn : Turns) {
                float Angle = Yaw + Turn * Mth.DEG_TO_RAD;
                Vec3 Probe = User.position().add(-Mth.sin(Angle) * Reach, 0.0, Mth.cos(Angle) * Reach);
                Vec3 Standing = standable(World, Probe);
                if (Standing != null) {
                    return Standing;
                }
            }
        }
        float Angle = Yaw;
        return User.position().add(-Mth.sin(Angle) * 2.5, 0.0, Mth.cos(Angle) * 2.5);
    }

    @Nullable
    public static Vec3 standable(ServerLevel World, Vec3 Probe) {
        BlockPos Base = BlockPos.containing(Probe);
        for (int Dy = 1; Dy >= -3; Dy--) {
            BlockPos Floor = Base.offset(0, Dy - 1, 0);
            BlockPos Feet = Base.offset(0, Dy, 0);
            if (World.getBlockState(Floor).isFaceSturdy(World, Floor, net.minecraft.core.Direction.UP) && World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty()
                    && World.getBlockState(Feet.above()).getCollisionShape(World, Feet.above()).isEmpty() && World.getFluidState(Feet).isEmpty()) {
                return new Vec3(Probe.x, Feet.getY(), Probe.z);
            }
        }
        return null;
    }

    public static float squashAt(ServerLevel World, BlockPos Feet) {
        int Free = 0;
        for (int Up = 0; Up < 4; Up++) {
            BlockPos Cell = Feet.above(Up);
            if (!World.getBlockState(Cell).getCollisionShape(World, Cell).isEmpty()) {
                break;
            }
            Free++;
        }
        return Free >= 4 ? 1.0f : Mth.clamp((Free + 0.3f) / Scp2521.Height, 0.5f, 1.0f);
    }

    public static void tick(MinecraftServer Server) {
        for (Run Each : runs()) {
            ServerPlayer User = Server.getPlayerList().getPlayer(Each.Owner);
            if (User == null || User.isRemoved()) {
                continue;
            }
            Each.Ticks++;
            try {
                step(Server, User, Each);
            } catch (RuntimeException Problem) {
                Log.error("SCP-2521 trip for {} failed, sending them home", User.getGameProfile().getName(), Problem);
                finish(User, Each, true);
            }
        }
    }

    private static void step(MinecraftServer Server, ServerPlayer User, Run Each) {
        int Ticks = Each.Ticks;
        keep(User);
        if (Each.Stage == StageWorld) {
            if (User.level().dimension() != Each.Dimension) {
                finish(User, Each, true);
                return;
            }
            pin(User, Each.Spot);
            ServerLevel Here = User.serverLevel();
            Entity Found = Each.Taker == null ? null : Here.getEntity(Each.Taker);
            Scp2521 Taker = Found instanceof Scp2521 Creature ? Creature : null;
            if (Taker != null) {
                Taker.face(User.position());
                if (Ticks % Scp2521.RustleEvery == 0) {
                    Taker.play(Scp2521Registry.RustleLayers, 0.6f, 0.9f);
                }
            }
            if (Ticks == Scp2521Scene.BindAt) {
                bind(Here, User, Each);
            }
            if (Ticks == Scp2521Scene.GrabAt && Taker != null) {
                Taker.setMode(Scp2521.ModeGrab);
                Taker.play(Scp2521Registry.GrabLayers, 1.0f, 1.0f);
            }
            if (Ticks > Scp2521Scene.GrabAt && Ticks <= Scp2521Scene.GrabAt + Scp2521Scene.GlideTicks && Taker != null && Each.TakerStart != null) {
                Vec3 Toward = User.position().subtract(Each.TakerStart);
                double Distance = Math.max(0.0, Toward.horizontalDistance() - GrabReach);
                Vec3 Way = new Vec3(Toward.x, 0.0, Toward.z).normalize().scale(Distance);
                float Share = Scp2521Scene.smooth((Ticks - Scp2521Scene.GrabAt) / (float) Scp2521Scene.GlideTicks);
                Taker.setPos(Each.TakerStart.x + Way.x * Share, Each.TakerStart.y, Each.TakerStart.z + Way.z * Share);
            }
            if (Ticks == Scp2521Scene.CarryAt) {
                if (Taker != null) {
                    Taker.setMode(Scp2521.ModeCarry);
                    Taker.play(Scp2521Registry.RiseLayers, 0.8f, 0.7f);
                }
                Entity Bound = Each.Tendrils == null ? null : Here.getEntity(Each.Tendrils);
                if (Bound instanceof Scp2521Tendrils Tendrils) {
                    Tendrils.drag();
                }
            }
            if (Ticks >= Scp2521Scene.VoidAt) {
                enterVoid(Server, User, Each);
            }
            return;
        }
        if (Each.Stage == StageVoid) {
            if (!Scp2521Void.inside(User)) {
                finish(User, Each, true);
                return;
            }
            pin(User, Scp2521Void.arrival(Each.Plot));
            if (Ticks >= Scp2521Scene.FallAt) {
                ServerLevel Void = User.serverLevel();
                Scp2521Void.open(Void, Each.Plot);
                Each.Stage = StageFall;
                User.setData(Scp2521Registry.TripAttachment, Each.record());
                playAt(Void, User.position(), Scp2521Registry.FallLayers, 1.0f, 1.0f);
                send(User, Scp2521ScenePayload.Fall, Ticks, -1, 0.0f, 0.0f);
            } else if ((Ticks - Scp2521Scene.VoidAt) % 70 == 20) {
                Entity Found = Each.Starer == null ? null : User.serverLevel().getEntity(Each.Starer);
                if (Found instanceof Scp2521 Starer) {
                    Starer.play(List.of(Scp2521Registry.Stare), 0.4f, 0.8f);
                }
            }
            return;
        }
        if (!Scp2521Void.inside(User)) {
            finish(User, Each, true);
            return;
        }
        Vec3 Arrival = Scp2521Void.arrival(Each.Plot);
        if (Math.abs(User.getX() - Arrival.x) > 0.3 || Math.abs(User.getZ() - Arrival.z) > 0.3) {
            User.connection.teleport(Arrival.x, User.getY(), Arrival.z, User.getYRot(), User.getXRot());
        }
        User.resetFallDistance();
        if (Ticks >= Scp2521Scene.HomeAt) {
            finish(User, Each, true);
        }
    }

    private static void bind(ServerLevel Here, ServerPlayer User, Run Each) {
        Scp2521Tendrils Tendrils = Scp2521Registry.TendrilType.get().create(Here);
        if (Tendrils == null) {
            return;
        }
        Tendrils.setPos(User.getX(), User.getY(), User.getZ());
        Tendrils.begin(User.getUUID(), User.getBbHeight());
        Each.Tendrils = Tendrils.getUUID();
        Each.Spawned.add(Tendrils);
        Here.addFreshEntity(Tendrils);
        playAt(Here, User.position(), Scp2521Registry.BindLayers, 1.0f, 1.0f);
    }

    private static void enterVoid(MinecraftServer Server, ServerPlayer User, Run Each) {
        ServerLevel Void = Scp2521Void.level(Server);
        if (Void == null) {
            finish(User, Each, true);
            return;
        }
        dismissWorld(Server, Each);
        Scp2521Void.hold(Void, Each.Plot);
        Scp2521Void.build(Void, Each.Plot);
        Vec3 Arrival = Scp2521Void.arrival(Each.Plot);
        Vec3 Facing = Scp2521Void.starer(Each.Plot);
        Scp2521 Starer = Scp2521Registry.CreatureType.get().create(Void);
        if (Starer != null) {
            Starer.setRole(Scp2521.RoleStarer);
            Starer.setOwner(User.getUUID());
            Starer.setPos(Facing.x, Facing.y, Facing.z);
            Starer.turn(Scp2521Void.StarerYaw);
            Starer.setMode(Scp2521.ModeStare);
            Each.Starer = Starer.getUUID();
            Each.Spawned.add(Starer);
            Void.addFreshEntity(Starer);
        }
        move(User, Each, Void, Arrival, Scp2521Void.ArrivalYaw, VoidPitch);
        Each.Stage = StageVoid;
        User.setData(Scp2521Registry.TripAttachment, Each.record());
        playAt(Void, Arrival, Scp2521Registry.DroneLayers, 0.9f, 1.0f);
        send(User, Scp2521ScenePayload.Void, Each.Ticks, Starer == null ? -1 : Starer.getId(), Scp2521Void.ArrivalYaw, VoidPitch);
    }

    private static void dismissWorld(MinecraftServer Server, Run Each) {
        for (Entity Made : new ArrayList<>(Each.Spawned)) {
            if (!(Made instanceof Scp2521 Creature && Creature.role() == Scp2521.RoleStarer)) {
                Made.discard();
                Each.Spawned.remove(Made);
            }
        }
        ServerLevel Home = Server.getLevel(Each.Dimension);
        if (Home == null) {
            return;
        }
        for (UUID Id : new UUID[]{Each.Taker, Each.Tendrils}) {
            Entity Found = Id == null ? null : Home.getEntity(Id);
            if (Found != null) {
                Found.discard();
            }
        }
        Each.Taker = null;
        Each.Tendrils = null;
    }

    public static void cleanup(MinecraftServer Server, Run Each) {
        dismissWorld(Server, Each);
        for (Entity Made : Each.Spawned) {
            Made.discard();
        }
        Each.Spawned.clear();
        ServerLevel Void = Scp2521Void.level(Server);
        if (Void != null) {
            Entity Found = Each.Starer == null ? null : Void.getEntity(Each.Starer);
            if (Found != null) {
                Found.discard();
            }
            Scp2521Void.wipe(Void, Each.Plot);
        }
        Each.Starer = null;
        Scp2521State State = Scp2521State.get(Server);
        if (Each.Owner.equals(State.plotOwner(Each.Plot))) {
            State.freePlot(Each.Plot);
        }
    }

    public static void finish(ServerPlayer User, Run Each, boolean Home) {
        MinecraftServer Server = User.server;
        Active.remove(Each.Owner);
        cleanup(Server, Each);
        lock(User, false);
        User.setData(Scp2521Registry.TripAttachment, Scp2521Trip.None);
        Scp2521State.get(Server).markTaken(Each.Owner);
        cool(User);
        if (Home) {
            ServerLevel Back = Server.getLevel(Each.Dimension);
            if (Back == null || Scp2521Void.inside(Back)) {
                respawn(User);
            } else {
                move(User, Each, Back, Each.Spot, Each.Yaw, Each.Pitch);
                playAt(Back, Each.Spot, List.of(Scp2521Registry.Return), 0.6f, 1.0f);
            }
            send(User, Scp2521ScenePayload.Home, Each.Ticks, -1, Each.Yaw, Each.Pitch);
        } else {
            send(User, Scp2521ScenePayload.Clear, 0, -1, 0.0f, 0.0f);
        }
    }

    public static boolean abort(ServerPlayer User, boolean Home) {
        Run Each = Active.get(User.getUUID());
        if (Each == null) {
            return false;
        }
        finish(User, Each, Home);
        return true;
    }

    public static void leave(ServerPlayer User) {
        Run Each = Active.remove(User.getUUID());
        if (Each == null) {
            return;
        }
        cleanup(User.server, Each);
        lock(User, false);
        User.setData(Scp2521Registry.TripAttachment, Each.record());
    }

    public static void died(ServerPlayer User) {
        Run Each = Active.remove(User.getUUID());
        if (Each != null) {
            cleanup(User.server, Each);
        }
        lock(User, false);
        User.setData(Scp2521Registry.TripAttachment, Scp2521Trip.None);
    }

    public static boolean loggedIn(ServerPlayer User) {
        Scp2521Trip Saved = User.getData(Scp2521Registry.TripAttachment);
        if (Active.containsKey(User.getUUID())) {
            return false;
        }
        if (Saved.active()) {
            User.setData(Scp2521Registry.TripAttachment, Scp2521Trip.None);
            lock(User, false);
            MinecraftServer Server = User.server;
            Scp2521State State = Scp2521State.get(Server);
            if (Saved.Plot() >= 0 && User.getUUID().equals(State.plotOwner(Saved.Plot()))) {
                ServerLevel Void = Scp2521Void.level(Server);
                if (Void != null) {
                    Scp2521Void.wipe(Void, Saved.Plot());
                }
                State.freePlot(Saved.Plot());
            }
            ServerLevel Back = Server.getLevel(Saved.dimension());
            if (Back == null || Scp2521Void.inside(Back)) {
                respawn(User);
            } else {
                User.teleportTo(Back, Saved.X(), Saved.Y(), Saved.Z(), Saved.Yaw(), Saved.Pitch());
                User.setDeltaMovement(Vec3.ZERO);
                User.resetFallDistance();
            }
            send(User, Scp2521ScenePayload.Clear, 0, -1, 0.0f, 0.0f);
            return true;
        }
        if (Scp2521Void.inside(User) && !Scp2521Void.standingIn() && !Scp2521Void.exempt(User)) {
            respawn(User);
            send(User, Scp2521ScenePayload.Clear, 0, -1, 0.0f, 0.0f);
            return true;
        }
        return false;
    }

    public static void stopping(MinecraftServer Server) {
        for (Run Each : runs()) {
            ServerPlayer User = Server.getPlayerList().getPlayer(Each.Owner);
            if (User != null) {
                leave(User);
            } else {
                Active.remove(Each.Owner);
                cleanup(Server, Each);
            }
        }
    }

    public static int recover(MinecraftServer Server) {
        Scp2521State State = Scp2521State.get(Server);
        ServerLevel Void = Scp2521Void.level(Server);
        int Freed = 0;
        for (Map.Entry<Integer, UUID> Entry : State.plots().entrySet()) {
            if (Active.containsKey(Entry.getValue())) {
                continue;
            }
            if (Void != null) {
                Scp2521Void.wipe(Void, Entry.getKey());
            }
            State.freePlot(Entry.getKey());
            Freed++;
        }
        return Freed;
    }

    public static void forgetAll() {
        Active.clear();
        Cooling.clear();
    }

    public static boolean wants(MinecraftServer Server, Entity Target) {
        UUID Owner = Target instanceof Scp2521 Creature ? Creature.owner() : Target instanceof Scp2521Tendrils Tendrils ? Tendrils.owner() : null;
        if (Owner == null) {
            return false;
        }
        Run Each = Active.get(Owner);
        if (Each == null) {
            return false;
        }
        UUID Id = Target.getUUID();
        return Id.equals(Each.Taker) || Id.equals(Each.Starer) || Id.equals(Each.Tendrils);
    }

    public static void keep(ServerPlayer User) {
        User.resetFallDistance();
        User.clearFire();
        User.setAirSupply(User.getMaxAirSupply());
        User.setTicksFrozen(0);
        if (User.isSprinting()) {
            User.setSprinting(false);
        }
    }

    public static void pin(ServerPlayer User, Vec3 Spot) {
        User.setDeltaMovement(0.0, Math.min(0.0, User.getDeltaMovement().y), 0.0);
        if (User.position().distanceToSqr(Spot) > HoldSlack * HoldSlack) {
            User.connection.teleport(Spot.x, Spot.y, Spot.z, User.getYRot(), User.getXRot());
        }
    }

    public static void move(ServerPlayer User, Run Each, ServerLevel World, Vec3 Spot, float Yaw, float Pitch) {
        Each.Moving = true;
        try {
            User.closeContainer();
            User.stopRiding();
            User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, Pitch);
        } finally {
            Each.Moving = false;
        }
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static void lock(ServerPlayer User, boolean Applied) {
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance Jump = User.getAttribute(Attributes.JUMP_STRENGTH);
        if (Speed != null) {
            Speed.removeModifier(Pinned.id());
            if (Applied) {
                Speed.addTransientModifier(Pinned);
            }
        }
        if (Jump != null) {
            Jump.removeModifier(Grounded.id());
            if (Applied) {
                Jump.addTransientModifier(Grounded);
            }
        }
    }

    public static boolean locked(ServerPlayer User) {
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(Pinned.id());
    }

    public static void respawn(ServerPlayer User) {
        DimensionTransition Respawn = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        ServerLevel World = Respawn.newLevel();
        Vec3 Spot = Respawn.pos();
        float Yaw = Respawn.yRot();
        if (Scp2521Void.inside(World)) {
            World = User.server.overworld();
            Spot = Vec3.atBottomCenterOf(World.getSharedSpawnPos());
            Yaw = 0.0f;
        }
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static void playAt(ServerLevel World, Vec3 Spot, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Spot.x, Spot.y + 1.0, Spot.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static void send(ServerPlayer User, int Kind, int Ticks, int Target, float Yaw, float Pitch) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp2521ScenePayload(Kind, Ticks, Target, Yaw, Pitch));
        }
    }

    public static AABB around(Vec3 Spot, double Range) {
        return new AABB(Spot.subtract(Range, Range, Range), Spot.add(Range, Range, Range));
    }
}
