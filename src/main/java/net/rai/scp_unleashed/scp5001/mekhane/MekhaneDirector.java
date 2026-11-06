package net.rai.scp_unleashed.scp5001.mekhane;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.core.FightAudio;
import net.rai.scp_unleashed.scp5001.core.Scp5001Duel;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneFxPayload;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneSitePayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

import static net.rai.scp_unleashed.scp5001.core.FightAudio.layer;

public final class MekhaneDirector {
    public static final double HoverLift = 60.0;
    public static final double ArenaLift = 110.0;
    public static final double VolumeReach = 150.0;
    public static final double VolumeFloor = 40.0;
    public static final double VolumeCeiling = 260.0;
    public static final float YawRate = 5.0f;
    public static final double DashReach = 48.0;
    public static final double DashStop = 20.0;
    public static final double SurgeRate = 1.1;
    public static final double ChestLift = 33.0;
    public static final double Reach = 36.0;
    public static final double PortalReach = 30.0;
    public static final double FxRange = 1024.0;
    public static final int SearchInterval = 20;
    public static final int SyncInterval = 10;
    public static final int ResummonLead = 40;
    public static final int HoldRadius = 3;
    public static final double BeamLift = 24.0;
    public static final int HumEvery = 100;
    public static final int[] Swings = {342, 892, 1132, 1382};
    private static final Logger Log = LogUtils.getLogger();
    private static boolean Resumed;
    private static boolean Warned;
    private static int Linked = -1;
    @Nullable
    private static Mekhane Held;

    private MekhaneDirector() {
    }

    public static boolean awake(Scp5001State.Phase Phase) {
        return Phase == Scp5001State.Phase.Xk1 || Phase == Scp5001State.Phase.Xk2;
    }

    public static void serverStarting() {
        Resumed = false;
        Held = null;
    }

    public static boolean engage(ServerLevel World) {
        MinecraftServer Server = World.getServer();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Scp5001State State = Scp5001State.get(Server);
        if (!awake(State.phase()) || Protocol.stage() != MekhaneProtocol.Stage.Idle) {
            return false;
        }
        Protocol.stage(MekhaneProtocol.Stage.Engaged, Server.overworld().getGameTime());
        return true;
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Scp5001State State = Scp5001State.get(Server);
        long Now = World.getGameTime();
        FightAudio.tick(World);
        if (!Resumed) {
            Resumed = true;
            resume(World, Protocol, State, Now);
        }
        switch (Protocol.stage()) {
            case Engaged -> engaged(World, Protocol, State, Now);
            case Summon -> summon(World, Protocol, State, Now);
            case Duel -> duel(World, Protocol, State, Now);
            case Clear -> clear(World, Protocol, State, Now);
            default -> {
            }
        }
        if (Now % SyncInterval == 0 && Protocol.stage() != MekhaneProtocol.Stage.Idle) {
            sync(World, Protocol, Now);
        }
    }

    public static void resume(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        if (Protocol.stage() == MekhaneProtocol.Stage.Duel) {
            int Elapsed = (int) (Now - Protocol.stageTick());
            if (Elapsed < MekhaneTimeline.Banish) {
                int Start = MekhaneTimeline.beatStart(Elapsed);
                Protocol.stageTick(Now - Start);
                Protocol.cursor(Start - 1);
                Protocol.stall(0);
                if (Start > 0) {
                    Entity Serpent = serpent(World, Protocol, true);
                    Mekhane Giant = giant(World, Protocol, place(Protocol, Start));
                    if (Serpent instanceof Scp5001Duel Duel) {
                        safely(() -> Duel.duelEngage(Giant, arena(Protocol)));
                    }
                }
            }
        }
        if (Protocol.stage() == MekhaneProtocol.Stage.Summon || Protocol.stage() == MekhaneProtocol.Stage.Duel) {
            force(World, Protocol, true);
        }
    }

    private static void engaged(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        if (!awake(State.phase())) {
            Protocol.reset();
            sync(World, Protocol, Now);
            return;
        }
        if (Protocol.met()) {
            beginSummon(World, Protocol, State, Now);
        }
    }

    public static void beginSummon(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        BlockPos Site = site(World, Protocol, State);
        int Surface = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Site.getX(), Site.getZ());
        Vec3 Base = new Vec3(Site.getX() + 0.5, Math.max(Surface, World.getSeaLevel()) + HoverLift, Site.getZ() + 0.5);
        Protocol.arena(Base, 0.0f);
        Protocol.stage(MekhaneProtocol.Stage.Summon, Now);
        Protocol.restarts(0);
        Protocol.lone(false);
        Protocol.banished(false);
        State.phase(Scp5001State.Phase.Summoning, Now);
        force(World, Protocol, true);
        sync(World, Protocol, Now);
    }

    public static BlockPos site(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State) {
        if (Protocol.hasEmitter()) {
            return Protocol.emitter();
        }
        if (State.located()) {
            return State.core();
        }
        if (Protocol.hasConsole()) {
            return Protocol.console();
        }
        return World.getSharedSpawnPos();
    }

    private static void summon(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        if (State.phase() != Scp5001State.Phase.Summoning) {
            abort(World, Protocol, Now);
            return;
        }
        int Elapsed = (int) (Now - Protocol.stageTick());
        for (int Cue = Protocol.cursor() + 1; Cue <= Elapsed; Cue++) {
            summonCue(World, Protocol, Cue);
        }
        Protocol.cursor(Elapsed);
        if (Elapsed >= MekhaneTimeline.DescentStart) {
            Vec3 Base = Protocol.arena();
            float Drop = MekhaneTimeline.descent(Elapsed);
            Vec3 Descent = Base.add(0.0, MekhaneTimeline.DescentHeight * Drop, 0.0);
            Mekhane Giant = giant(World, Protocol, Descent);
            Giant.place(Descent, Protocol.yaw());
            hold(World, Giant);
            Giant.drive(MekhaneTimeline.Act.Descend, 0, 0.0f, 1.0f - Drop * 0.6f);
        }
        if (Elapsed >= MekhaneTimeline.SummonTicks) {
            beginDuel(World, Protocol, State, Now);
        }
    }

    private static void summonCue(ServerLevel World, MekhaneProtocol Protocol, int Cue) {
        Vec3 Emitter = Vec3.atCenterOf(site(World, Protocol, Scp5001State.get(World.getServer())));
        Vec3 Column = Emitter.add(0.0, BeamLift, 0.0);
        Vec3 Base = Protocol.arena();
        if (Cue < MekhaneTimeline.AlarmEnd && Cue % MekhaneTimeline.AlarmEvery == 0) {
            play(World, Emitter, "mekhane.alarm", 20, layer(Scp5001MekhaneRegistry.Alarm), layer(Scp5001MekhaneRegistry.Alarm, 0.95f, 0.8f, 1),
                    layer(Scp5001MekhaneRegistry.AlarmDrone, 1.0f, 0.9f, 2));
            if (Protocol.hasConsole()) {
                play(World, Vec3.atCenterOf(Protocol.console()), "mekhane.alarm_console", 20, layer(Scp5001MekhaneRegistry.Alarm, 1.06f, 0.45f, 0));
            }
            fx(World, MekhaneFxPayload.Alarm, Emitter, 1.0f);
        }
        if (Cue == 0) {
            play(World, Emitter, "mekhane.rumble", 0, layer(Scp5001MekhaneRegistry.Rumble, 1.0f, 0.7f, 0));
            fx(World, MekhaneFxPayload.Rumble, Emitter, 0.6f);
        }
        if (Cue == MekhaneTimeline.BeamCharge) {
            play(World, Emitter, "mekhane.beam_charge", 0, layer(Scp5001MekhaneRegistry.BeamCharge), layer(Scp5001MekhaneRegistry.BeamCharge, 0.9f, 0.7f, 14));
        }
        if (Cue == MekhaneTimeline.BeamFire) {
            play(World, Column, "mekhane.beam_fire", 0, layer(Scp5001MekhaneRegistry.BeamFire), layer(Scp5001MekhaneRegistry.ImpactBody, 0.9f, 0.8f, 1),
                    layer(Scp5001MekhaneRegistry.Thunder, 1.0f, 4), layer(Scp5001MekhaneRegistry.Rumble, 1.0f, 0.8f, 12));
            fx(World, MekhaneFxPayload.Flash, Base, 0.7f);
            fx(World, MekhaneFxPayload.Rumble, Emitter, 1.0f);
        }
        if (Cue > MekhaneTimeline.BeamFire && Cue < MekhaneTimeline.SummonTicks && (Cue - MekhaneTimeline.BeamFire) % HumEvery == 20) {
            play(World, Column, "mekhane.beam_hum", 60, layer(Scp5001MekhaneRegistry.BeamHum));
        }
        if (Cue == MekhaneTimeline.DescentStart) {
            Vec3 Top = Base.add(0.0, MekhaneTimeline.DescentHeight + ChestLift, 0.0);
            play(World, Top, "mekhane.descend", 0, layer(Scp5001MekhaneRegistry.Descend), layer(Scp5001MekhaneRegistry.Bell, 1.0f, 6),
                    layer(Scp5001MekhaneRegistry.Rumble, 1.0f, 0.7f, 20));
        }
        if (Cue == MekhaneTimeline.DescentStart + 220) {
            Vec3 At = Base.add(0.0, MekhaneTimeline.DescentHeight * MekhaneTimeline.descent(Cue) + ChestLift, 0.0);
            play(World, At, "mekhane.descend", 0, layer(Scp5001MekhaneRegistry.Descend, 0.94f, 0.8f, 0));
        }
        if (Cue > MekhaneTimeline.DescentStart && (Cue - MekhaneTimeline.DescentStart) % 80 == 0 && Cue < MekhaneTimeline.SummonTicks - 40) {
            float Drop = MekhaneTimeline.descent(Cue);
            Vec3 At = Base.add(0.0, MekhaneTimeline.DescentHeight * Drop + ChestLift, 0.0);
            float Toll = 1.0f - 0.05f * ((Cue / 80) % 3);
            play(World, At, "mekhane.bell", 40, layer(Scp5001MekhaneRegistry.Bell, Toll, 0.85f, 0), layer(Scp5001MekhaneRegistry.Bell, Toll * 0.94f, 0.7f, 1));
            ratchet(World, At, 0.7f);
        }
        if (Cue == MekhaneTimeline.SummonTicks - 40) {
            Vec3 Chest = Base.add(0.0, ChestLift, 0.0);
            play(World, Chest, "mekhane.unfold", 0, layer(Scp5001MekhaneRegistry.WingFlare), layer(Scp5001MekhaneRegistry.ImpactBody, 0.85f, 0.6f, 3),
                    layer(Scp5001MekhaneRegistry.Gears, 1.0f, 0.8f, 2), layer(Scp5001MekhaneRegistry.Gears, 0.92f, 0.7f, 8), layer(Scp5001MekhaneRegistry.SigilHum, 1.0f, 0.7f, 10));
            fx(World, MekhaneFxPayload.Shock, Chest, 0.8f);
        }
    }

    private static void beginDuel(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        Entity Serpent = serpent(World, Protocol, true);
        Vec3 Base = Protocol.arena();
        float Facing = Protocol.yaw();
        if (Serpent != null) {
            Vec3 Way = Serpent.position().subtract(Base);
            if (Way.horizontalDistanceSqr() > 1.0) {
                Facing = (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
            }
        }
        Protocol.arena(Base, Facing);
        State.phase(Scp5001State.Phase.Duel, Now);
        Protocol.stage(MekhaneProtocol.Stage.Duel, Now);
    }

    public static double ground(MekhaneProtocol Protocol) {
        return Protocol.arena().y - HoverLift;
    }

    public static Vec3 forward(float Yaw) {
        float Radians = Yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(Radians), 0.0, Mth.cos(Radians));
    }

    public static Vec3 forward(MekhaneProtocol Protocol) {
        return forward(Protocol.yaw());
    }

    public static Vec3 arena(MekhaneProtocol Protocol) {
        Vec3 Base = Protocol.arena();
        return new Vec3(Base.x, ground(Protocol) + ArenaLift, Base.z);
    }

    public static Vec3 place(MekhaneProtocol Protocol, int Elapsed) {
        Vec3 Base = Protocol.arena();
        Vec3 Offset = MekhaneFlight.path(Elapsed);
        return new Vec3(Base.x + Offset.x, ground(Protocol) + Offset.y, Base.z + Offset.z);
    }

    public static Vec3 contain(MekhaneProtocol Protocol, Vec3 Point) {
        Vec3 Base = Protocol.arena();
        Vec3 Flat = new Vec3(Point.x - Base.x, 0.0, Point.z - Base.z);
        double Reach = Flat.length();
        if (Reach > VolumeReach) {
            Flat = Flat.scale(VolumeReach / Reach);
        }
        double Floor = ground(Protocol);
        return new Vec3(Base.x + Flat.x, Mth.clamp(Point.y, Floor + VolumeFloor, Floor + VolumeCeiling), Base.z + Flat.z);
    }

    public static Vec3 chest(Mekhane Giant) {
        return Giant.position().add(0.0, ChestLift, 0.0);
    }

    public static Vec3 chest(MekhaneProtocol Protocol) {
        return Protocol.arena().add(0.0, ChestLift, 0.0);
    }

    public static Vec3 axeHead(Mekhane Giant) {
        return chest(Giant).add(forward(Giant.getYRot()).scale(20.0)).add(0.0, -4.0, 0.0);
    }

    public static Vec3 sigilHand(Mekhane Giant) {
        Vec3 Ahead = forward(Giant.getYRot());
        Vec3 Left = new Vec3(Ahead.z, 0.0, -Ahead.x);
        return chest(Giant).add(Ahead.scale(9.0)).add(Left.scale(9.0)).add(0.0, -3.0, 0.0);
    }

    public static Vec3 sigilHand(MekhaneProtocol Protocol) {
        Vec3 Ahead = forward(Protocol);
        Vec3 Left = new Vec3(Ahead.z, 0.0, -Ahead.x);
        return chest(Protocol).add(Ahead.scale(9.0)).add(Left.scale(9.0)).add(0.0, -3.0, 0.0);
    }

    private static void duel(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        Scp5001State.Phase Phase = State.phase();
        if (Phase != Scp5001State.Phase.Duel && !(Phase == Scp5001State.Phase.Saved && Protocol.banished())) {
            abort(World, Protocol, Now);
            return;
        }
        int Elapsed = (int) (Now - Protocol.stageTick());
        Entity Serpent = serpent(World, Protocol, false);
        if (Elapsed < MekhaneTimeline.Banish && Serpent == null && !Protocol.lone()) {
            Protocol.stageTick(Protocol.stageTick() + 1);
            Protocol.stall(Protocol.stall() + 1);
            int Hold = Math.max(0, Elapsed);
            Vec3 Circle = new Vec3(Math.cos(Now * 0.01) * 26.0, 6.0 * Math.sin(Now * 0.02), Math.sin(Now * 0.01) * 26.0);
            Vec3 Spot = place(Protocol, Hold).add(Circle);
            Mekhane Waiting = giant(World, Protocol, Spot);
            Waiting.glide(Spot, Waiting.position().add(Circle.cross(new Vec3(0.0, 1.0, 0.0)).normalize()), YawRate);
            hold(World, Waiting);
            Waiting.drive(MekhaneTimeline.Act.Fly, MekhaneTimeline.damage(Elapsed), MekhaneTimeline.charge(Math.min(Elapsed, 200)), 1.0f);
            if (Protocol.stall() >= MekhaneConfig.watchdogTicks()) {
                watchdog(World, Protocol, State, Now);
            }
            return;
        }
        Protocol.stall(0);
        Scp5001Duel Duel = Serpent instanceof Scp5001Duel Found ? Found : null;
        Mekhane Giant = giant(World, Protocol, place(Protocol, Elapsed));
        pose(Protocol, Giant, Duel, Elapsed);
        keepLoaded(World, Protocol, Giant, Elapsed);
        if (Duel != null && Elapsed > 0 && Giant.getId() != Linked) {
            safely(() -> Duel.duelEngage(Giant, arena(Protocol)));
        }
        Linked = Giant.getId();
        for (int Cue = Protocol.cursor() + 1; Cue <= Elapsed; Cue++) {
            duelCue(World, Protocol, State, Giant, Duel, Cue, Now);
        }
        Protocol.cursor(Elapsed);
        if (Duel != null && Elapsed >= MekhaneTimeline.BindStart && Elapsed < MekhaneTimeline.Banish) {
            Vec3 Portal = Giant.portal();
            float Progress = MekhaneTimeline.bind(Elapsed);
            safely(() -> Duel.duelBind(Portal, Progress));
        }
        if (Elapsed >= MekhaneTimeline.DuelTicks) {
            Giant.discard();
            Protocol.mekhaneId(null);
            Protocol.stage(MekhaneProtocol.Stage.Clear, Now);
            force(World, Protocol, false);
            sync(World, Protocol, Now);
        }
    }

    private static void keepLoaded(ServerLevel World, MekhaneProtocol Protocol, Entity Giant, int Elapsed) {
        hold(World, Giant);
        if (World.getGameTime() % 5 != 0) {
            return;
        }
        net.minecraft.world.level.ChunkPos Here = new net.minecraft.world.level.ChunkPos(Giant.blockPosition());
        net.minecraft.world.level.ChunkPos Ahead = new net.minecraft.world.level.ChunkPos(BlockPos.containing(place(Protocol, Elapsed + 30)));
        if (!Ahead.equals(Here)) {
            World.getChunkSource().addRegionTicket(Scp5001MekhaneRegistry.Ticket, Ahead, HoldRadius, Ahead);
        }
    }

    public static void hold(ServerLevel World, Entity Giant) {
        net.minecraft.world.level.ChunkPos Here = new net.minecraft.world.level.ChunkPos(Giant.blockPosition());
        World.getChunkSource().addRegionTicket(Scp5001MekhaneRegistry.Ticket, Here, HoldRadius, Here);
    }

    private static void watchdog(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        Protocol.restarts(Protocol.restarts() + 1);
        Protocol.stall(0);
        if (Protocol.restarts() > MekhaneConfig.restartLimit()) {
            Log.warn("SCP-5001 emergency protocol found no opponent after {} restarts; finishing the recontainment alone", Protocol.restarts() - 1);
            Protocol.lone(true);
            return;
        }
        Log.info("SCP-5001 emergency protocol stalled; restarting the duel from the standoff");
        State.phase(Scp5001State.Phase.Summoning, Now);
        Protocol.stage(MekhaneProtocol.Stage.Summon, Now - MekhaneTimeline.SummonTicks + ResummonLead);
        Protocol.cursor(MekhaneTimeline.SummonTicks - ResummonLead);
    }

    private static void pose(MekhaneProtocol Protocol, Mekhane Giant, @Nullable Scp5001Duel Duel, int Elapsed) {
        Vec3 Path = place(Protocol, Elapsed);
        Vec3 Target = Duel != null ? head(Duel, Protocol) : arena(Protocol).add(forward(Protocol).scale(40.0));
        float Surge = MekhaneTimeline.dash(Elapsed);
        Vec3 Lunge = Vec3.ZERO;
        if (Surge > 0.0f) {
            Vec3 Toward = Target.subtract(Path.add(0.0, ChestLift, 0.0));
            double Length = Toward.length();
            if (Length > DashStop) {
                Lunge = Toward.scale(Math.min(DashReach, Length - DashStop) / Length * Surge);
            }
        }
        Lunge = Giant.surge(Lunge, SurgeRate);
        Vec3 Goal = contain(Protocol, Path.add(0.0, ChestLift, 0.0).add(Lunge)).subtract(0.0, ChestLift, 0.0);
        Vec3 Velocity = MekhaneFlight.velocity(Elapsed);
        Vec3 Look = Target;
        boolean Travel = Elapsed >= MekhaneTimeline.Chase && Elapsed < MekhaneTimeline.Dive && MekhaneTimeline.dash(Elapsed) <= 0.0f
                || Elapsed >= MekhaneTimeline.Ascend;
        if (Travel && Velocity.horizontalDistanceSqr() > 0.01) {
            Look = Goal.add(Velocity.multiply(1.0, 0.0, 1.0).normalize().scale(40.0));
        }
        if (Elapsed >= MekhaneTimeline.LookDown && Elapsed < MekhaneTimeline.Ascend) {
            Look = Goal.add(forward(Giant.getYRot()).scale(40.0));
        }
        Giant.glide(Goal, Look, YawRate);
        Vec3 Accel = MekhaneFlight.acceleration(Elapsed);
        Vec3 Ahead = forward(Giant.getYRot());
        Vec3 Right = new Vec3(-Ahead.z, 0.0, Ahead.x);
        double Flat = Math.sqrt(Velocity.x * Velocity.x + Velocity.z * Velocity.z);
        float Dive = (float) Mth.clamp(-Velocity.y * 26.0 + Flat * 9.0, -24.0, 32.0);
        float Bank = (float) Mth.clamp(Accel.dot(Right) * 900.0, -32.0, 32.0);
        if (Elapsed >= MekhaneTimeline.SlamHit && Elapsed < MekhaneTimeline.SlamGround) {
            Dive = -26.0f;
            Bank = 18.0f;
        }
        if (Elapsed >= MekhaneTimeline.BindBeat && Elapsed < 2230) {
            Bank = (float) (14.0 * Math.sin(Elapsed * 0.09));
            Dive = (float) (6.0 * Math.sin(Elapsed * 0.13));
        }
        Giant.tilt(Dive, Bank);
        Giant.drive(MekhaneTimeline.act(Elapsed), MekhaneTimeline.damage(Elapsed), MekhaneTimeline.charge(Elapsed), 1.0f - MekhaneTimeline.dissolve(Elapsed));
        Giant.aim(Target, MekhaneTimeline.laser(Elapsed));
        if (Elapsed >= MekhaneTimeline.PortalOpen - 1 && Elapsed < MekhaneTimeline.PortalOpen + 1 && Giant.portalOpen() <= 0.0f) {
            Vec3 Away = Target.subtract(chest(Giant));
            Vec3 Out = Away.lengthSqr() < 1.0 ? forward(Giant.getYRot()) : Away.normalize();
            Giant.portal(contain(Protocol, Target.add(Out.scale(PortalReach))));
        }
        Giant.opening(MekhaneTimeline.portal(Elapsed));
    }

    private static Vec3 head(Scp5001Duel Duel, MekhaneProtocol Protocol) {
        try {
            Vec3 Found = Duel.duelHead();
            return Found == null ? arena(Protocol) : Found;
        } catch (RuntimeException Problem) {
            return arena(Protocol);
        }
    }

    private static void attack(@Nullable Scp5001Duel Duel, int Move, Vec3 Target) {
        if (Duel != null) {
            safely(() -> Duel.duelAttack(Move, Target));
        }
    }

    private static void duelCue(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, Mekhane Giant, @Nullable Scp5001Duel Duel, int Cue, long Now) {
        Vec3 Chest = chest(Giant);
        Vec3 Head = Duel != null ? head(Duel, Protocol) : arena(Protocol);
        Vec3 Ahead = forward(Giant.getYRot());
        Vec3 Left = new Vec3(Ahead.z, 0.0, -Ahead.x);
        switch (Cue) {
            case 0 -> {
                if (Duel != null) {
                    safely(() -> Duel.duelEngage(Giant, arena(Protocol)));
                }
                play(World, Chest, "mekhane.raise", 0, layer(Scp5001MekhaneRegistry.Bell), layer(Scp5001MekhaneRegistry.Bell, 0.94f, 0.8f, 1),
                        layer(Scp5001MekhaneRegistry.SigilHum, 1.0f, 0.8f, 6));
                ratchet(World, Chest, 0.9f);
            }
            case 160, 1060, 1440 -> attack(Duel, Scp5001Duel.Roar, Chest.add(0.0, 10.0, 0.0));
            case 120, 640, 1700, 2440 -> play(World, Chest, "mekhane.flare", 40, layer(Scp5001MekhaneRegistry.WingFlare), layer(Scp5001MekhaneRegistry.WingBeat, 0.9f, 0.7f, 6));
            case 320 -> attack(Duel, Scp5001Duel.Bite, Chest);
            case 360, 1160 -> strike(World, Duel, axeHead(Giant), Head, Cue == 360 ? 0.8f : 1.1f);
            case 412 -> attack(Duel, Scp5001Duel.Lunge, Chest);
            case 452, 1272 -> deflect(World, Chest.add(Ahead.scale(8.0)));
            case 550 -> attack(Duel, Scp5001Duel.Bite, Chest.add(Left.scale(8.0)).add(0.0, 6.0, 0.0));
            case 590 -> {
                Vec3 Torn = Chest.add(Left.scale(8.0)).add(0.0, 6.0, 0.0);
                sparks(World, Torn, 1.0f);
                plate(World, Torn, 1.0f);
            }
            case 760, 1000 -> ratchet(World, Chest, 0.7f);
            case 872 -> attack(Duel, Scp5001Duel.Sweep, Chest.subtract(0.0, 18.0, 0.0));
            case 920, 1410 -> strike(World, Duel, axeHead(Giant), Head, Cue == 920 ? 0.9f : 0.8f);
            case 1120 -> attack(Duel, Scp5001Duel.Lunge, Chest);
            case 1196 -> attack(Duel, Scp5001Duel.Sweep, Chest.subtract(0.0, 18.0, 0.0));
            case 1370 -> attack(Duel, Scp5001Duel.Bite, Chest);
            case 1480 -> attack(Duel, Scp5001Duel.Lunge, Chest.subtract(0.0, 26.0, 0.0));
            case MekhaneTimeline.SlamHit -> {
                sparks(World, Chest.add(0.0, 8.0, 0.0), 1.4f);
                play(World, Chest, "mekhane.plate", 0, layer(Scp5001MekhaneRegistry.PlateBreak), layer(Scp5001MekhaneRegistry.ImpactBody, 1.0f, 0.8f, 0),
                        layer(Scp5001MekhaneRegistry.Clang, 0.9f, 1));
                fx(World, MekhaneFxPayload.Flash, Chest, 0.5f);
            }
            case MekhaneTimeline.SlamGround -> {
                Vec3 Impact = new Vec3(Giant.getX(), ground(Protocol) + 1.0, Giant.getZ());
                play(World, Impact, "mekhane.slam", 0, layer(Scp5001MekhaneRegistry.Slam), layer(Scp5001MekhaneRegistry.ImpactBody, 0.9f, 1),
                        layer(Scp5001MekhaneRegistry.ImpactBody, 0.85f, 0.8f, 3),
                        layer(Scp5001MekhaneRegistry.Thunder, 0.92f, 4), layer(Scp5001MekhaneRegistry.Rumble, 1.0f, 10));
                fx(World, MekhaneFxPayload.Shock, Impact, 1.8f);
                fx(World, MekhaneFxPayload.Rumble, Impact, 1.0f);
                fx(World, MekhaneFxPayload.Flash, Impact, 0.4f);
            }
            case 1600 -> {
                play(World, Chest, "mekhane.flare", 0, layer(Scp5001MekhaneRegistry.WingFlare), layer(Scp5001MekhaneRegistry.WingBeat, 0.94f, 8),
                        layer(Scp5001MekhaneRegistry.WingBeat, 0.9f, 0.8f, 18));
                ratchet(World, Chest, 0.8f);
            }
            case MekhaneTimeline.BindBeat -> attack(Duel, Scp5001Duel.Coil, Chest);
            case 1860, 1980, 2100 -> play(World, Chest, "mekhane.strain", 40, layer(Scp5001MekhaneRegistry.Strain), layer(Scp5001MekhaneRegistry.Gears, 0.9f, 0.6f, 8));
            case 1900, 2050, 2200 -> {
                Vec3 Torn = Chest.add(0.0, Cue == 1900 ? 2.0 : -6.0, 0.0);
                sparks(World, Torn, 1.2f);
                plate(World, Torn, 0.9f);
            }
            case 2230 -> {
                ratchet(World, Chest, 0.9f);
                play(World, Chest, "mekhane.strain", 0, layer(Scp5001MekhaneRegistry.Strain, 1.06f, 4));
            }
            case MekhaneTimeline.Hurl -> {
                if (Duel != null) {
                    safely(() -> Duel.duelReact(Chest, 1.6f));
                }
                play(World, Chest, "mekhane.hurl", 0, layer(Scp5001MekhaneRegistry.Hurl), layer(Scp5001MekhaneRegistry.ImpactBody, 1.0f, 0.9f, 2),
                        layer(Scp5001MekhaneRegistry.Thunder, 1.0f, 5));
                fx(World, MekhaneFxPayload.Shock, Chest, 1.4f);
                fx(World, MekhaneFxPayload.Flash, Chest, 0.6f);
            }
            case 2480 -> play(World, sigilHand(Giant), "mekhane.laser_charge", 0, layer(Scp5001MekhaneRegistry.LaserCharge), layer(Scp5001MekhaneRegistry.SigilHum, 1.0f, 4));
            case MekhaneTimeline.LaserStart -> {
                play(World, sigilHand(Giant), "mekhane.laser", 0, layer(Scp5001MekhaneRegistry.Laser), layer(Scp5001MekhaneRegistry.BeamFire, 1.0f, 0.8f, 0),
                        layer(Scp5001MekhaneRegistry.Thunder, 1.0f, 0.8f, 3));
                fx(World, MekhaneFxPayload.Flash, sigilHand(Giant), 0.5f);
            }
            case MekhaneTimeline.PortalOpen -> play(World, Head, "mekhane.portal_open", 0, layer(Scp5001MekhaneRegistry.PortalOpen),
                    layer(Scp5001MekhaneRegistry.ImpactBody, 0.85f, 0.7f, 2), layer(Scp5001MekhaneRegistry.Rumble, 1.0f, 0.8f, 8));
            case MekhaneTimeline.Banish -> banish(World, Protocol, State, Duel, Head, Now);
            case MekhaneTimeline.LookDown -> ratchet(World, Chest, 0.8f);
            case MekhaneTimeline.Ascend -> play(World, Chest, "mekhane.ascend", 0, layer(Scp5001MekhaneRegistry.Ascend), layer(Scp5001MekhaneRegistry.WingFlare, 1.0f, 4),
                    layer(Scp5001MekhaneRegistry.Bell, 0.95f, 0.8f, 20), layer(Scp5001MekhaneRegistry.Bell, 0.9f, 0.6f, 21));
            default -> {
            }
        }
        for (int[] Window : MekhaneTimeline.Dashes) {
            if (Cue == Window[0]) {
                play(World, Chest, "mekhane.dash", 30, layer(Scp5001MekhaneRegistry.Dash), layer(Scp5001MekhaneRegistry.WingBeat, 1.0f, 0.8f, 0),
                        layer(Scp5001MekhaneRegistry.WingBeat, 0.9f, 0.6f, 9));
            }
        }
        for (int Swing : Swings) {
            if (Cue == Swing) {
                play(World, axeHead(Giant), "mekhane.swing", 14, layer(Scp5001MekhaneRegistry.AxeSwing), layer(Scp5001MekhaneRegistry.AxeSwing, 0.86f, 0.6f, 6));
            }
        }
        MekhaneTimeline.Act Act = MekhaneTimeline.act(Cue);
        if ((Act == MekhaneTimeline.Act.Fly || Act == MekhaneTimeline.Act.Ascend) && Cue % 56 == 0 || Act == MekhaneTimeline.Act.Chase && Cue % 40 == 0) {
            play(World, Chest, "mekhane.wing", 24, layer(Scp5001MekhaneRegistry.WingBeat, 1.0f, 0.8f, 0), layer(Scp5001MekhaneRegistry.WingBeat, 0.92f, 0.65f, 7));
        }
        if (Cue % 300 == 150 && (Act == MekhaneTimeline.Act.Fly || Act == MekhaneTimeline.Act.Chase || Act == MekhaneTimeline.Act.Idle)) {
            ratchet(World, Chest, 0.5f);
        }
        if (Cue > MekhaneTimeline.LaserStart && Cue < MekhaneTimeline.Banish && (Cue - MekhaneTimeline.LaserStart) % 60 == 0) {
            Vec3 From = sigilHand(Giant);
            if (Duel != null) {
                float Push = Cue == MekhaneTimeline.LaserStart + 60 ? 1.2f : 0.5f;
                safely(() -> Duel.duelReact(From, Push));
            }
            play(World, From, "mekhane.laser_pulse", 50, layer(Scp5001MekhaneRegistry.Laser, 1.0f, 0.75f, 0));
            play(World, Head, "mekhane.laser_hit", 50, layer(Scp5001MekhaneRegistry.ImpactBody, 1.1f, 0.45f, 1));
        }
        if (Cue >= 2480 && Cue < MekhaneTimeline.Banish && (Cue - 2480) % 100 == 50) {
            play(World, sigilHand(Giant), "mekhane.sigil", 80, layer(Scp5001MekhaneRegistry.SigilHum, 1.0f, 0.8f, 0));
        }
        if (Cue >= MekhaneTimeline.PortalFull && Cue < MekhaneTimeline.Banish && (Cue - MekhaneTimeline.PortalFull) % 50 == 0) {
            Vec3 Gate = Giant.portalOpen() > 0.0f ? Giant.portal() : Head;
            play(World, Gate, "mekhane.pull", 40, layer(Scp5001MekhaneRegistry.PortalPull));
        }
    }

    private static void strike(ServerLevel World, @Nullable Scp5001Duel Duel, Vec3 From, Vec3 Head, float Power) {
        if (Duel != null) {
            safely(() -> Duel.duelReact(From, Power));
        }
        play(World, Head, "mekhane.strike", 20, layer(Scp5001MekhaneRegistry.AxeImpact), layer(Scp5001MekhaneRegistry.ImpactBody, 1.0f, Math.min(1.0f, 0.75f + Power * 0.25f), 1),
                layer(Scp5001MekhaneRegistry.ImpactBody, 0.9f, 0.8f, 3), layer(Scp5001MekhaneRegistry.Clang, 0.95f, 0.7f, 1), layer(Scp5001MekhaneRegistry.Thunder, 1.0f, 0.7f, 6));
        fx(World, MekhaneFxPayload.Shock, Head, Power);
        fx(World, MekhaneFxPayload.Flash, Head, 0.4f * Power);
    }

    private static void deflect(ServerLevel World, Vec3 At) {
        play(World, At, "mekhane.deflect", 20, layer(Scp5001MekhaneRegistry.WingFlare), layer(Scp5001MekhaneRegistry.Clang, 1.0f, 1),
                layer(Scp5001MekhaneRegistry.ImpactBody, 1.15f, 0.55f, 1));
        sparks(World, At, 0.8f);
    }

    private static void plate(ServerLevel World, Vec3 At, float Power) {
        play(World, At, "mekhane.plate", 30, layer(Scp5001MekhaneRegistry.PlateBreak, 1.0f, Power, 0), layer(Scp5001MekhaneRegistry.Clang, 0.9f, 0.8f * Power, 1),
                layer(Scp5001MekhaneRegistry.ImpactBody, 1.1f, 0.6f * Power, 0));
    }

    private static void ratchet(ServerLevel World, Vec3 At, float Volume) {
        play(World, At, "mekhane.gears", 30, layer(Scp5001MekhaneRegistry.Gears, 1.0f, Volume, 0), layer(Scp5001MekhaneRegistry.Gears, 0.94f, Volume * 0.85f, 5),
                layer(Scp5001MekhaneRegistry.Gears, 1.06f, Volume * 0.75f, 10));
    }

    private static void play(ServerLevel World, Vec3 At, String Key, int Cooldown, FightAudio.Layer... Layers) {
        FightAudio.cue(World, At, Key, Cooldown, SoundSource.AMBIENT, Layers);
    }

    private static void sparks(ServerLevel World, Vec3 At, float Power) {
        fx(World, MekhaneFxPayload.Sparks, At, Power);
    }

    private static void banish(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, @Nullable Scp5001Duel Duel, Vec3 Head, long Now) {
        if (Duel != null) {
            safely(Duel::duelBanish);
        }
        Protocol.banished(true);
        Protocol.serpentId(null);
        State.phase(Scp5001State.Phase.Saved, Now);
        play(World, Head, "mekhane.banish", 0, layer(Scp5001MekhaneRegistry.PortalSeal), layer(Scp5001MekhaneRegistry.ImpactBody, 0.9f, 2),
                layer(Scp5001MekhaneRegistry.Thunder, 0.95f, 3), layer(Scp5001MekhaneRegistry.Rumble, 1.0f, 12), layer(Scp5001MekhaneRegistry.Bell, 0.9f, 0.8f, 30));
        fx(World, MekhaneFxPayload.Shock, Head, 2.0f);
        fx(World, MekhaneFxPayload.Flash, Head, 1.0f);
        fx(World, MekhaneFxPayload.Rumble, Head, 1.0f);
        for (ServerPlayer Each : World.getServer().getPlayerList().getPlayers()) {
            Each.sendSystemMessage(Component.translatable("message.scp_unleashed.scp5001_emergency.saved").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC));
        }
    }

    private static void clear(ServerLevel World, MekhaneProtocol Protocol, Scp5001State State, long Now) {
        if (Now - Protocol.stageTick() < MekhaneConfig.clearTicks()) {
            return;
        }
        if (State.phase() == Scp5001State.Phase.Saved) {
            State.phase(Scp5001State.Phase.Dormant, Now);
        }
        Protocol.reset();
        sync(World, Protocol, Now);
    }

    public static void abort(ServerLevel World, MekhaneProtocol Protocol, long Now) {
        Entity Giant = Protocol.mekhaneId() == null ? null : World.getEntity(Protocol.mekhaneId());
        if (Giant != null) {
            Giant.discard();
        }
        if (Held != null) {
            Held.discard();
            Held = null;
        }
        force(World, Protocol, false);
        Protocol.reset();
        sync(World, Protocol, Now);
    }

    @Nullable
    public static Entity serpent(ServerLevel World, MekhaneProtocol Protocol, boolean Search) {
        UUID Known = Protocol.serpentId();
        if (Known != null) {
            Entity Found = World.getEntity(Known);
            if (Found instanceof Scp5001Duel && Found.isAlive()) {
                return Found;
            }
        }
        if (!Search && World.getGameTime() % SearchInterval != 0) {
            return null;
        }
        for (Entity Each : World.getAllEntities()) {
            if (Each instanceof Scp5001Duel && Each.isAlive()) {
                Protocol.serpentId(Each.getUUID());
                return Each;
            }
        }
        return null;
    }

    public static Mekhane giant(ServerLevel World, MekhaneProtocol Protocol) {
        return giant(World, Protocol, Protocol.arena().add(0.0, MekhaneTimeline.DescentHeight, 0.0));
    }

    public static Mekhane giant(ServerLevel World, MekhaneProtocol Protocol, Vec3 Spawn) {
        UUID Known = Protocol.mekhaneId();
        if (Known != null && Held != null && !Held.isRemoved() && Held.level() == World && Known.equals(Held.getUUID())) {
            return Held;
        }
        if (Known != null && World.getEntity(Known) instanceof Mekhane Found && Found.isAlive()) {
            Held = Found;
            return Found;
        }
        Mekhane Made = new Mekhane(Scp5001MekhaneRegistry.MekhaneType.get(), World);
        Made.place(Spawn, Protocol.yaw());
        hold(World, Made);
        World.addFreshEntity(Made);
        Protocol.mekhaneId(Made.getUUID());
        Held = Made;
        return Made;
    }

    private static void force(ServerLevel World, MekhaneProtocol Protocol, boolean On) {
        if (On) {
            Vec3 Base = Protocol.arena();
            int X = Mth.floor(Base.x) >> 4;
            int Z = Mth.floor(Base.z) >> 4;
            if (Protocol.forced() && (Protocol.forcedX() != X || Protocol.forcedZ() != Z)) {
                World.setChunkForced(Protocol.forcedX(), Protocol.forcedZ(), false);
            }
            World.setChunkForced(X, Z, true);
            Protocol.forced(true, X, Z);
        } else if (Protocol.forced()) {
            World.setChunkForced(Protocol.forcedX(), Protocol.forcedZ(), false);
            Protocol.forced(false, 0, 0);
        }
    }

    public static void fx(ServerLevel World, int Kind, Vec3 At, float Power) {
        MekhaneFxPayload Payload = new MekhaneFxPayload(Kind, At.x, At.y, At.z, Power);
        for (ServerPlayer Each : World.players()) {
            if (Each.position().distanceToSqr(At) <= FxRange * FxRange) {
                PacketDistributor.sendToPlayer(Each, Payload);
            }
        }
    }

    public static void sync(ServerLevel World, MekhaneProtocol Protocol, long Now) {
        BlockPos Emitter = site(World, Protocol, Scp5001State.get(World.getServer()));
        MekhaneSitePayload Payload = new MekhaneSitePayload(Protocol.stage().ordinal(), (int) Math.max(0, Now - Protocol.stageTick()), Emitter, Protocol.arena().y);
        for (ServerPlayer Each : World.players()) {
            PacketDistributor.sendToPlayer(Each, Payload);
        }
    }

    public static void syncTo(ServerPlayer Player) {
        MinecraftServer Server = Player.getServer();
        if (Server == null) {
            return;
        }
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        ServerLevel World = Server.overworld();
        long Now = World.getGameTime();
        BlockPos Emitter = site(World, Protocol, Scp5001State.get(Server));
        PacketDistributor.sendToPlayer(Player, new MekhaneSitePayload(Protocol.stage().ordinal(), (int) Math.max(0, Now - Protocol.stageTick()), Emitter, Protocol.arena().y));
    }

    private static void safely(Runnable Call) {
        try {
            Call.run();
        } catch (RuntimeException Problem) {
            if (!Warned) {
                Warned = true;
                Log.warn("SCP-5001 duel opponent failed a choreography call", Problem);
            }
        }
    }
}
