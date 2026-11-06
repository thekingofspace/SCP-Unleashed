package net.rai.scp_unleashed.intro;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.intro.network.IntroActionPayload;
import net.rai.scp_unleashed.intro.network.IntroAwaitPayload;
import net.rai.scp_unleashed.intro.network.IntroCuePayload;
import net.rai.scp_unleashed.intro.network.IntroScenePayload;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.props.SeatBlock;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IntroDirector {
    public static final int AwaitStart = 0;
    public static final int AwaitOffice = 1;
    public static final int AwaitGate = 2;
    public static final int AwaitReturn = 3;
    public static final int ResendEvery = 100;
    public static final int CrossingDelay = 12;
    public static final int ReturnLinger = 400;
    public static final int GateLimit = 900;
    public static final double StandBack = 0.8;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<MinecraftServer, Map<UUID, IntroSession>> Sessions = new ConcurrentHashMap<>();
    private static int NextToken = 1;

    private IntroDirector() {
    }

    public static IntroRecord record(ServerPlayer User) {
        return User.getData(IntroRegistry.Record.get());
    }

    public static void setRecord(ServerPlayer User, IntroRecord Value) {
        User.setData(IntroRegistry.Record.get(), Value);
    }

    private static Map<UUID, IntroSession> sessions(MinecraftServer Server) {
        return Sessions.computeIfAbsent(Server, Key -> new ConcurrentHashMap<>());
    }

    @Nullable
    public static IntroSession session(ServerPlayer User) {
        Map<UUID, IntroSession> Found = Sessions.get(User.server);
        return Found == null ? null : Found.get(User.getUUID());
    }

    public static boolean playing(ServerPlayer User) {
        IntroSession Found = session(User);
        return Found != null && Found.playing();
    }

    public static boolean veteran(ServerPlayer User) {
        return User.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > IntroSettings.VeteranTicks;
    }

    @Nullable
    public static ServerPlayer find(MinecraftServer Server, UUID Id) {
        ServerPlayer Online = Server.getPlayerList().getPlayer(Id);
        if (Online != null) {
            return Online;
        }
        for (ServerLevel World : Server.getAllLevels()) {
            if (World.getPlayerByUUID(Id) instanceof ServerPlayer Found) {
                return Found;
            }
        }
        return null;
    }

    private static int nextToken() {
        int Made = NextToken++;
        if (NextToken > 1_000_000) {
            NextToken = 1;
        }
        return Made;
    }

    public static void send(ServerPlayer User, CustomPacketPayload Payload) {
        if (User instanceof FakePlayer || User.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(User, Payload);
    }

    public static void loggedIn(ServerPlayer User) {
        IntroSession Old = sessions(User.server).remove(User.getUUID());
        if (Old != null) {
            tidy(User.server, Old);
        }
        IntroRecord Current = record(User);
        if (Current.active()) {
            restore(User);
            Current = record(User);
        } else if (IntroWorld.inside(User)) {
            evict(User);
        }
        if (Current.Done()) {
            return;
        }
        if (!IntroSettings.enabled(User.server)) {
            markDone(User);
            return;
        }
        if (!Current.Started() && veteran(User)) {
            markDone(User);
            return;
        }
        queue(User, false);
    }

    public static void loggedOut(ServerPlayer User) {
        IntroSession Found = sessions(User.server).remove(User.getUUID());
        if (Found != null) {
            tidy(User.server, Found);
            vacate(User.server, Found.Plot);
        }
    }

    public static int plotOf(ServerPlayer User) {
        IntroPlots Plots = IntroPlots.get(User.server);
        return Plots.hasPlot(User.getUUID()) ? Plots.plotOf(User.getUUID()) : -1;
    }

    public static void vacate(MinecraftServer Server, int Plot) {
        ServerLevel Intro = IntroWorld.level(Server);
        if (Intro == null || Plot < 0) {
            return;
        }
        IntroBuilder.release(Intro, Plot);
    }

    public static void markDone(ServerPlayer User) {
        setRecord(User, record(User).finished());
    }

    public static IntroSession queue(ServerPlayer User, boolean Replay) {
        IntroSession Made = new IntroSession(User.getUUID());
        Made.Replay = Replay;
        Made.Token = nextToken();
        sessions(User.server).put(User.getUUID(), Made);
        send(User, new IntroAwaitPayload(Made.Token, AwaitStart));
        return Made;
    }

    public static void ready(ServerPlayer User, int Token, boolean Skip) {
        IntroSession Found = session(User);
        if (Found == null || Found.Token != Token) {
            return;
        }
        switch (Found.Stage) {
            case IntroSession.Waiting -> {
                if (!Found.Replay && (Skip || record(User).Done() || !IntroSettings.enabled(User.server))) {
                    markDone(User);
                    sessions(User.server).remove(User.getUUID());
                    send(User, new IntroCuePayload(IntroCuePayload.Abort));
                    return;
                }
                start(User, Found);
            }
            case IntroSession.Office -> {
                if (!Found.Ready) {
                    Found.Ready = true;
                    Found.Ticks = 0;
                    Entity Doc = scientist(User, Found);
                    send(User, new IntroScenePayload(Found.Token, 1, Doc == null ? -1 : Doc.getId(), IntroWorld.officeOrigin(Found.Plot)));
                }
            }
            case IntroSession.Gate -> {
                if (!Found.Ready) {
                    Found.Ready = true;
                    Found.Ticks = 0;
                    send(User, new IntroScenePayload(Found.Token, 2, -1, IntroWorld.gateRoomOrigin(Found.Plot)));
                }
            }
            case IntroSession.Returning -> {
                sessions(User.server).remove(User.getUUID());
                send(User, new IntroCuePayload(IntroCuePayload.Arrived));
            }
            default -> {
            }
        }
    }

    public static void action(ServerPlayer User, int Token, int Action) {
        IntroSession Found = session(User);
        if (Found == null || Found.Token != Token) {
            return;
        }
        if (Action == IntroActionPayload.Skip && Found.playing()) {
            Found.Skipped = true;
            finish(User, Found);
            return;
        }
        if (Found.Stage != IntroSession.Office || !Found.Ready) {
            return;
        }
        if (Action == IntroActionPayload.Stand) {
            stand(User, Found);
        } else if (Action == IntroActionPayload.DialogueDone) {
            ServerLevel Intro = IntroWorld.level(User.server);
            if (Intro != null) {
                enterGate(User, Found, Intro);
            }
        }
    }

    public static IntroRecord.Spot spotOf(ServerPlayer User) {
        return new IntroRecord.Spot(User.level().dimension(), User.getX(), User.getY(), User.getZ(), User.getYRot(), User.getXRot(), User.fallDistance);
    }

    private static void start(ServerPlayer User, IntroSession Found) {
        ServerLevel Intro = IntroWorld.level(User.server);
        if (Intro == null) {
            Log.warn("The intro dimension is missing; skipping the intro for {}", User.getGameProfile().getName());
            sessions(User.server).remove(User.getUUID());
            send(User, new IntroCuePayload(IntroCuePayload.Abort));
            return;
        }
        int Plot = IntroPlots.get(User.server).plotOf(User.getUUID());
        Found.Plot = Plot;
        if (!IntroBuilder.fresh(Intro, Plot)) {
            sessions(User.server).remove(User.getUUID());
            send(User, new IntroCuePayload(IntroCuePayload.Abort));
            return;
        }
        IntroRecord Current = record(User);
        setRecord(User, Current.active() ? Current.resumed() : Current.begun(spotOf(User)));
        hold(Intro, Plot, User.getUUID(), true);
        enterOffice(User, Found, Intro);
    }

    private static void enterOffice(ServerPlayer User, IntroSession Found, ServerLevel Intro) {
        clearCast(Intro, Found.Plot);
        BlockPos Chair = IntroWorld.playerChair(Found.Plot);
        move(User, Intro, IntroWorld.officeSeat(Found.Plot), IntroWorld.officeYaw(), IntroWorld.OfficePitch);
        BlockState Seat = Intro.getBlockState(Chair);
        if (Seat.getBlock() instanceof SeatBlock && !(User instanceof FakePlayer)) {
            PropSeatEntity.sit(Intro, Chair, Seat, User);
        }
        User.setYRot(IntroWorld.officeYaw());
        User.setXRot(IntroWorld.OfficePitch);
        IntroScientist Doc = new IntroScientist(IntroRegistry.Scientist.get(), Intro);
        BlockPos Other = IntroWorld.scientistChair(Found.Plot);
        Doc.moveTo(Other.getX() + 0.5, Other.getY(), Other.getZ() + 0.5, IntroWorld.ScientistFacing.toYRot(), 0.0f);
        Doc.setYHeadRot(IntroWorld.ScientistFacing.toYRot());
        Doc.setYBodyRot(IntroWorld.ScientistFacing.toYRot());
        Doc.setOwner(User.getUUID());
        Intro.addFreshEntity(Doc);
        seatOf(Intro, Other, Doc);
        Found.Scientist = Doc.getUUID();
        staff(Intro, Found.Plot, User.getUUID());
        Found.Stage = IntroSession.Office;
        Found.Ready = false;
        Found.Stood = false;
        Found.Ticks = 0;
        Found.Token = nextToken();
        send(User, new IntroAwaitPayload(Found.Token, AwaitOffice));
    }

    public static final double[][] Shifts = {
            {IntroStaff.RoleGuard, 12.5, 3.45, -1, 0.0, 0, IntroStaff.EndHigh},
            {IntroStaff.RoleScientist, 1.6, 3.45, 1, 0.075, 20, 11.2},
            {IntroStaff.RoleTalker, 12.4, 2.1, -1, 0.066, 200, IntroStaff.EndHigh}
    };

    private static void staff(ServerLevel Intro, int Plot, UUID Owner) {
        BlockPos Office = IntroWorld.officeOrigin(Plot);
        IntroStaff Previous = null;
        for (double[] Shift : Shifts) {
            IntroStaff Walker = new IntroStaff(IntroRegistry.Staff.get(), Intro);
            Walker.setup(Owner, Office, (int) Shift[0], Shift[1], Shift[2], (int) Shift[3], Shift[4], (int) Shift[5], Shift[6]);
            Intro.addFreshEntity(Walker);
            Previous = Walker;
        }
        if (Previous != null && Previous.role() == IntroStaff.RoleTalker) {
            IntroStaff Partner = new IntroStaff(IntroRegistry.Staff.get(), Intro);
            Partner.setup(Owner, Office, IntroStaff.RoleTalker, Shifts[2][1] + IntroStaff.Spacing, 2.75, -1, Shifts[2][4], (int) Shifts[2][5], IntroStaff.EndHigh);
            Partner.follow(Previous);
            Intro.addFreshEntity(Partner);
        }
    }

    private static void seatOf(ServerLevel Intro, BlockPos Pos, Entity Rider) {
        BlockState State = Intro.getBlockState(Pos);
        if (!(State.getBlock() instanceof SeatBlock) || !State.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        Direction Facing = State.getValue(HorizontalDirectionalBlock.FACING);
        PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), Intro);
        Seat.moveTo(Pos.getX() + 0.5 - Facing.getStepX() * PropSeatEntity.BackShift, Pos.getY(), Pos.getZ() + 0.5 - Facing.getStepZ() * PropSeatEntity.BackShift,
                Facing.toYRot(), 0.0f);
        Intro.addFreshEntity(Seat);
        if (!Rider.startRiding(Seat, true)) {
            Seat.discard();
        }
    }

    private static void stand(ServerPlayer User, IntroSession Found) {
        if (Found.Stood) {
            return;
        }
        Found.Stood = true;
        Entity Doc = scientist(User, Found);
        if (Doc == null) {
            return;
        }
        Entity Seat = Doc.getVehicle();
        Doc.stopRiding();
        if (Seat != null) {
            Seat.discard();
        }
        BlockPos Chair = IntroWorld.scientistChair(Found.Plot);
        Direction Back = IntroWorld.ScientistFacing.getOpposite();
        double X = Chair.getX() + 0.5 + Back.getStepX() * StandBack;
        double Z = Chair.getZ() + 0.5 + Back.getStepZ() * StandBack;
        float Yaw = (float) (Mth.atan2(User.getZ() - Z, User.getX() - X) * Mth.RAD_TO_DEG) - 90.0f;
        Doc.moveTo(X, Chair.getY(), Z, Yaw, 0.0f);
        Doc.setYHeadRot(Yaw);
        if (Doc instanceof IntroScientist Scientist) {
            Scientist.setYBodyRot(Yaw);
            Scientist.setStanding(true);
        }
    }

    private static void enterGate(ServerPlayer User, IntroSession Found, ServerLevel Intro) {
        clearCast(Intro, Found.Plot);
        Found.Scientist = null;
        move(User, Intro, IntroWorld.gateArrival(Found.Plot), IntroWorld.GateYaw, 0.0f);
        Found.Stage = IntroSession.Gate;
        Found.Ready = false;
        Found.Ticks = 0;
        Found.Token = nextToken();
        send(User, new IntroAwaitPayload(Found.Token, AwaitGate));
    }

    public static void finish(ServerPlayer User, IntroSession Found) {
        IntroRecord Current = record(User);
        IntroRecord.Spot Back = Current.Return().orElseGet(() -> respawnSpot(User));
        setRecord(User, Current.finished());
        Found.Stage = IntroSession.Returning;
        Found.Ready = false;
        Found.Ticks = 0;
        Found.Token = nextToken();
        send(User, new IntroCuePayload(IntroCuePayload.Leaving));
        tidy(User.server, Found);
        land(User, Back);
        vacate(User.server, Found.Plot);
        send(User, new IntroAwaitPayload(Found.Token, AwaitReturn));
    }

    public static void restore(ServerPlayer User) {
        IntroRecord Current = record(User);
        IntroRecord.Spot Back = Current.Return().orElseGet(() -> respawnSpot(User));
        setRecord(User, Current.cleared());
        IntroSession Found = session(User);
        if (Found != null) {
            tidy(User.server, Found);
        }
        land(User, Back);
        vacate(User.server, Found != null && Found.Plot >= 0 ? Found.Plot : plotOf(User));
    }

    private static void land(ServerPlayer User, IntroRecord.Spot Back) {
        ServerLevel Target = User.server.getLevel(Back.Dimension());
        IntroRecord.Spot Spot = Back;
        if (Target == null || IntroWorld.inside(Target)) {
            Spot = respawnSpot(User);
            Target = User.server.getLevel(Spot.Dimension());
            if (Target == null) {
                Target = User.server.overworld();
            }
        }
        move(User, Target, Spot.position(), Spot.Yaw(), Spot.Pitch());
        User.fallDistance = Spot.Fall();
    }

    public static IntroRecord.Spot respawnSpot(ServerPlayer User) {
        DimensionTransition Home = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        if (IntroWorld.inside(Home.newLevel())) {
            ServerLevel World = User.server.overworld();
            return new IntroRecord.Spot(World.dimension(), World.getSharedSpawnPos().getX() + 0.5, World.getSharedSpawnPos().getY(), World.getSharedSpawnPos().getZ() + 0.5,
                    0.0f, 0.0f, 0.0f);
        }
        return new IntroRecord.Spot(Home.newLevel().dimension(), Home.pos().x, Home.pos().y, Home.pos().z, Home.yRot(), 0.0f, 0.0f);
    }

    private static void evict(ServerPlayer User) {
        land(User, respawnSpot(User));
    }

    public static void move(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw, float Pitch) {
        Entity Vehicle = User.getVehicle();
        User.stopRiding();
        if (Vehicle instanceof PropSeatEntity) {
            Vehicle.discard();
        }
        if (User.isSleeping()) {
            User.stopSleepInBed(true, true);
        }
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        World.getChunkAt(BlockPos.containing(Spot));
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, Pitch);
        User.setYRot(Yaw);
        User.setXRot(Pitch);
        User.setYHeadRot(Yaw);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    @Nullable
    public static Entity scientist(ServerPlayer User, IntroSession Found) {
        ServerLevel Intro = IntroWorld.level(User.server);
        if (Intro == null || Found.Scientist == null) {
            return null;
        }
        return Intro.getEntity(Found.Scientist);
    }

    public static void clearCast(ServerLevel Intro, int Plot) {
        if (Plot < 0) {
            return;
        }
        AABB Bounds = IntroWorld.plotBounds(Plot).inflate(2.0);
        List<Entity> Doomed = new ArrayList<>();
        for (Entity Found : Intro.getEntities((Entity) null, Bounds, Found -> Found instanceof IntroScientist || Found instanceof IntroStaff || Found instanceof PropSeatEntity)) {
            Doomed.add(Found);
        }
        for (Entity Found : Doomed) {
            Found.ejectPassengers();
            Found.stopRiding();
            Found.discard();
        }
    }

    public static void hold(ServerLevel Intro, int Plot, UUID Owner, boolean Add) {
        if (Plot < 0) {
            return;
        }
        for (ChunkPos Chunk : IntroWorld.plotChunks(Plot)) {
            IntroRegistry.Tickets.forceChunk(Intro, Owner, Chunk.x, Chunk.z, Add, true);
        }
    }

    private static void tidy(MinecraftServer Server, IntroSession Found) {
        ServerLevel Intro = IntroWorld.level(Server);
        if (Intro == null || Found.Plot < 0) {
            return;
        }
        ServerPlayer User = find(Server, Found.Player);
        if (User != null && User.getVehicle() instanceof PropSeatEntity) {
            User.stopRiding();
        }
        clearCast(Intro, Found.Plot);
        hold(Intro, Found.Plot, Found.Player, false);
        Found.Scientist = null;
    }

    public static void abort(ServerPlayer User) {
        IntroSession Found = sessions(User.server).remove(User.getUUID());
        if (Found != null) {
            tidy(User.server, Found);
            if (!IntroWorld.inside(User)) {
                vacate(User.server, Found.Plot);
            }
        }
        IntroRecord Current = record(User);
        if (Current.active()) {
            setRecord(User, Current.cleared());
        }
        send(User, new IntroCuePayload(IntroCuePayload.Abort));
    }

    public static void replay(ServerPlayer User) {
        IntroSession Found = session(User);
        if (Found != null && Found.playing() || record(User).active()) {
            restore(User);
        }
        IntroSession Old = sessions(User.server).remove(User.getUUID());
        if (Old != null) {
            tidy(User.server, Old);
        }
        queue(User, true);
    }

    public static void reset(ServerPlayer User) {
        setRecord(User, record(User).reset());
    }

    public static void respawned(ServerPlayer User) {
        IntroSession Found = session(User);
        if (Found == null || !Found.playing() || IntroWorld.inside(User)) {
            return;
        }
        boolean Replay = Found.Replay;
        abort(User);
        if (!record(User).Done()) {
            queue(User, Replay);
        }
    }

    public static void tick(MinecraftServer Server) {
        Map<UUID, IntroSession> All = Sessions.get(Server);
        if (All == null || All.isEmpty()) {
            return;
        }
        for (IntroSession Found : new ArrayList<>(All.values())) {
            ServerPlayer User = find(Server, Found.Player);
            if (User == null || User.isRemoved()) {
                All.remove(Found.Player);
                tidy(Server, Found);
                vacate(Server, Found.Plot);
                continue;
            }
            step(User, Found);
        }
    }

    public static void step(ServerPlayer User, IntroSession Found) {
        Found.Ticks++;
        if (Found.Stage == IntroSession.Waiting) {
            if (Found.Ticks % ResendEvery == 0) {
                send(User, new IntroAwaitPayload(Found.Token, AwaitStart));
            }
            return;
        }
        if (Found.Stage == IntroSession.Returning) {
            if (Found.Ticks > ReturnLinger) {
                sessions(User.server).remove(User.getUUID());
            } else if (Found.Ticks % ResendEvery == 0) {
                send(User, new IntroAwaitPayload(Found.Token, AwaitReturn));
            }
            return;
        }
        if (!IntroWorld.inside(User)) {
            abort(User);
            return;
        }
        User.getFoodData().setExhaustion(0.0f);
        User.setRemainingFireTicks(0);
        User.setAirSupply(User.getMaxAirSupply());
        if (!Found.Ready) {
            if (Found.Ticks % ResendEvery == 0) {
                send(User, new IntroAwaitPayload(Found.Token, IntroSession.awaitStage(Found.Stage)));
            }
            return;
        }
        if (Found.Stage == IntroSession.Gate) {
            if (User.getBoundingBox().intersects(IntroWorld.gateField(Found.Plot)) || Found.Ticks > GateLimit) {
                Found.Stage = IntroSession.Crossing;
                Found.Ticks = 0;
                send(User, new IntroCuePayload(IntroCuePayload.Crossing));
            } else if (!IntroWorld.gateApproach(Found.Plot).inflate(1.0).contains(User.position())) {
                move(User, (ServerLevel) User.level(), IntroWorld.gateArrival(Found.Plot), IntroWorld.GateYaw, 0.0f);
            }
            return;
        }
        if (Found.Stage == IntroSession.Crossing && Found.Ticks >= CrossingDelay) {
            finish(User, Found);
        }
    }

    public static void stopped(MinecraftServer Server) {
        Sessions.remove(Server);
        IntroBuilder.forget(Server);
    }

    public static Optional<IntroSession> find(ServerPlayer User) {
        return Optional.ofNullable(session(User));
    }
}
