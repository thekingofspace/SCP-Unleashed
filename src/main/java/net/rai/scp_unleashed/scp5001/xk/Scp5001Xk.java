package net.rai.scp_unleashed.scp5001.xk;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.xk.network.XkSkyPayload;
import net.rai.scp_unleashed.scp5001.xk.network.YaldabaothPayload;
import net.rai.scp_unleashed.worldend.WorldEnding;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Scp5001Xk {
    public static final String Prefix = "message.scp_unleashed.scp5001_xk.";
    public static final int SavedClearTicks = 1200;
    public static final int CancelClearTicks = 60;
    public static final double DreadRange = 56.0;
    public static final double BarRange = 220.0;
    public static final int SyncEvery = 2;
    public static final int SkyEvery = 100;
    private static final Logger Log = LogUtils.getLogger();
    @Nullable
    private static UUID BodyId;
    @Nullable
    private static ServerBossEvent Bar;
    private static final Set<UUID> Synced = new HashSet<>();
    private static int ClearOverride = -1;

    private Scp5001Xk() {
    }

    public static boolean active(XkState.Stage Stage) {
        return Stage == XkState.Stage.Xk1 || Stage == XkState.Stage.Xk2 || Stage == XkState.Stage.Ended;
    }

    public static boolean rising(MinecraftServer Server) {
        XkState.Stage Stage = XkState.get(Server).stage();
        if (!active(Stage)) {
            return false;
        }
        Scp5001State.Phase Phase = Scp5001State.get(Server).phase();
        return Phase != Scp5001State.Phase.Saved;
    }

    public static boolean paused(Scp5001State.Phase Phase) {
        return Phase == Scp5001State.Phase.Summoning || Phase == Scp5001State.Phase.Duel;
    }

    public static void tick(MinecraftServer Server) {
        Scp5001State Shared = Scp5001State.get(Server);
        XkState State = XkState.get(Server);
        ServerLevel World = Server.overworld();
        long Time = World.getGameTime();
        Scp5001State.Phase Phase = Shared.phase();
        switch (Phase) {
            case Xk1 -> {
                if (State.stage() != XkState.Stage.Xk1) {
                    enterFirst(Server, State);
                }
                State.firstElapsed(State.firstElapsed() + advance(World, State));
                if (State.firstElapsed() >= Scp5001XkConfig.firstPhaseTicks()) {
                    Shared.phase(Scp5001State.Phase.Xk2, Time);
                    enterSecond(Server, State);
                }
            }
            case Xk2 -> {
                if (State.stage() != XkState.Stage.Xk2) {
                    enterSecond(Server, State);
                }
                State.secondElapsed(State.secondElapsed() + advance(World, State));
                float Sky = (float) Math.min(1.0, State.secondElapsed() / (double) Scp5001XkConfig.skyTicks());
                setSky(Server, State, Sky, false);
                if (Sky >= 1.0f) {
                    Shared.phase(Scp5001State.Phase.Ended, Time);
                    enterEnded(Server, State);
                }
            }
            case Ended -> {
                if (State.stage() != XkState.Stage.Ended) {
                    enterEnded(Server, State);
                }
                State.lastDayTime(World.getDayTime());
            }
            case Summoning, Duel -> {
                State.lastDayTime(World.getDayTime());
                if (active(State.stage())) {
                    if (WorldEnding.running(Server)) {
                        WorldEnding.cancel(Server);
                    }
                    YaldabaothFlight Flight = State.flight();
                    if (!Flight.present() && !Flight.banished()) {
                        manifest(Server, State, false);
                    }
                    if (Phase == Scp5001State.Phase.Summoning) {
                        Flight.gather(arena(Server, Shared));
                    }
                }
            }
            case Saved -> {
                if (State.stage() != XkState.Stage.Clearing && State.stage() != XkState.Stage.Idle) {
                    beginClearing(Server, State, ClearOverride > 0 ? ClearOverride : SavedClearTicks);
                }
            }
            default -> {
                if (active(State.stage())) {
                    beginClearing(Server, State, CancelClearTicks);
                }
            }
        }
        if (State.stage() == XkState.Stage.Clearing) {
            clearTick(Server, State);
        }
        if (WorldEnding.running(Server) && Shared.phase() != Scp5001State.Phase.Ended && Scp5001XkRegistry.Source.toString().equals(net.rai.scp_unleashed.worldend.WorldEndState.get(Server).source())) {
            WorldEnding.cancel(Server);
        }
        being(Server, World, State);
        if (Time % SkyEvery == 0) {
            PacketDistributor.sendToAllPlayers(new XkSkyPayload(State.sky(), false));
        }
    }

    private static long advance(ServerLevel World, XkState State) {
        long Now = World.getDayTime();
        long Last = State.lastDayTime();
        State.lastDayTime(Now);
        if (Last < 0L) {
            return 1L;
        }
        long Delta = Now - Last;
        return Delta >= 1L && Delta <= 24000L ? Delta : 1L;
    }

    private static void enterFirst(MinecraftServer Server, XkState State) {
        State.stage(XkState.Stage.Xk1);
        State.firstElapsed(0L);
        State.secondElapsed(0L);
        State.lastDayTime(-1L);
        State.endingBegun(false);
        setSky(Server, State, 0.0f, true);
        if (!State.announced()) {
            State.announced(true);
            announce(Server);
        }
        Log.warn("SCP-5001: XK-1 begun");
    }

    private static void enterSecond(MinecraftServer Server, XkState State) {
        State.stage(XkState.Stage.Xk2);
        State.lastDayTime(-1L);
        if (!State.flight().present() && !State.flight().banished()) {
            manifest(Server, State, true);
        }
        for (ServerPlayer Player : Server.getPlayerList().getPlayers()) {
            Player.sendSystemMessage(Component.translatable(Prefix + "manifest").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            Player.playNotifySound(Scp5001XkRegistry.RoarLow.get(), SoundSource.HOSTILE, 0.8f, 0.55f);
            Player.playNotifySound(Scp5001XkRegistry.Tremor.get(), SoundSource.AMBIENT, 0.7f, 0.5f);
        }
        Log.warn("SCP-5001: XK-2 begun, Yaldabaoth manifested");
    }

    private static void enterEnded(MinecraftServer Server, XkState State) {
        State.stage(XkState.Stage.Ended);
        setSky(Server, State, 1.0f, false);
        if (!State.endingBegun()) {
            State.endingBegun(true);
            Vec3 Focus = State.flight().present() ? State.flight().head() : null;
            WorldEnding.begin(Server, Component.translatable(Prefix + "ending"), Scp5001XkRegistry.Source, Focus);
        }
    }

    private static void beginClearing(MinecraftServer Server, XkState State, int Ticks) {
        Log.info("SCP-5001: XK cleared, the sky returns over {} ticks", Ticks);
        State.stage(XkState.Stage.Clearing);
        State.clearing(State.sky(), Ticks);
        YaldabaothFlight Flight = State.flight();
        if (Flight.present() && Flight.mode() != YaldabaothFlight.Mode.Banish) {
            Flight.vanish();
            PacketDistributor.sendToAllPlayers(gone());
        }
        XkRising.clear();
        if (WorldEnding.running(Server)) {
            WorldEnding.cancel(Server);
        }
    }

    private static void clearTick(MinecraftServer Server, XkState State) {
        State.clearTick();
        float Left = 1.0f - Math.min(1.0f, State.clearElapsed() / (float) State.clearTicks());
        State.sky(State.clearFrom() * Left);
        if (State.clearElapsed() % 10 == 0) {
            PacketDistributor.sendToAllPlayers(new XkSkyPayload(State.sky(), false));
        }
        if (State.clearElapsed() >= State.clearTicks()) {
            State.wipe();
            PacketDistributor.sendToAllPlayers(new XkSkyPayload(0.0f, true));
        }
    }

    public static void setSky(MinecraftServer Server, XkState State, float Sky, boolean Snap) {
        float Before = State.sky();
        State.sky(Sky);
        if (Snap || Math.abs(Before - Sky) > 0.02f) {
            PacketDistributor.sendToAllPlayers(new XkSkyPayload(State.sky(), Snap));
        }
    }

    private static void announce(MinecraftServer Server) {
        for (ServerPlayer Player : Server.getPlayerList().getPlayers()) {
            Player.sendSystemMessage(Component.translatable(Prefix + "broadcast.head").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            Player.sendSystemMessage(Component.translatable(Prefix + "broadcast.body").withStyle(ChatFormatting.RED));
            Player.playNotifySound(Scp5001XkRegistry.Broadcast.get(), SoundSource.AMBIENT, 1.0f, 0.8f);
            Player.playNotifySound(Scp5001XkRegistry.Horn.get(), SoundSource.AMBIENT, 1.0f, 0.55f);
            Player.playNotifySound(Scp5001XkRegistry.Tremor.get(), SoundSource.AMBIENT, 0.9f, 0.45f);
        }
    }

    public static Vec3 arena(MinecraftServer Server, Scp5001State Shared) {
        ServerLevel World = Server.overworld();
        if (Shared.located()) {
            BlockPos Core = Shared.core();
            int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Core.getX(), Core.getZ());
            return new Vec3(Core.getX() + 0.5, Math.max(Ground + 70.0, Core.getY() + 90.0), Core.getZ() + 0.5);
        }
        List<ServerPlayer> Players = World.players();
        if (!Players.isEmpty()) {
            ServerPlayer First = Players.get(0);
            return First.position().add(0.0, 80.0, 0.0);
        }
        BlockPos Spawn = World.getSharedSpawnPos();
        return new Vec3(Spawn.getX(), Spawn.getY() + 100.0, Spawn.getZ());
    }

    public static void manifest(MinecraftServer Server, XkState State, boolean Rising) {
        Scp5001State Shared = Scp5001State.get(Server);
        ServerLevel World = Server.overworld();
        Vec3 Start;
        Vec3 Toward;
        double Height;
        if (Shared.located()) {
            BlockPos Core = Shared.core();
            int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Core.getX(), Core.getZ());
            Start = Vec3.atCenterOf(Core);
            Toward = new Vec3(1.0, 0.0, 0.3);
            Height = Math.min(World.getMaxBuildHeight() - 20.0, Math.max(Ground + 90.0, 160.0));
        } else {
            List<ServerPlayer> Players = World.players();
            Vec3 Base = Players.isEmpty() ? Vec3.atCenterOf(World.getSharedSpawnPos()) : Players.get(World.random.nextInt(Players.size())).position();
            double Angle = World.random.nextDouble() * Math.PI * 2.0;
            Start = Base.add(Math.cos(Angle) * 220.0, 0.0, Math.sin(Angle) * 220.0);
            Height = Math.min(World.getMaxBuildHeight() - 20.0, 170.0);
            Start = new Vec3(Start.x, Height, Start.z);
            Toward = Base.subtract(Start);
            Rising = false;
        }
        State.flight().manifest(Start, Toward, Rising, Height);
        Synced.clear();
    }

    public static void dismissed(MinecraftServer Server) {
        XkState State = XkState.get(Server);
        State.flight().dismiss();
        PacketDistributor.sendToAllPlayers(gone());
    }

    private static YaldabaothPayload gone() {
        return new YaldabaothPayload(YaldabaothPayload.Gone, Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), 0.0f, 0, 0, Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), 0.0f, List.of(), 0L);
    }

    private static YaldabaothPayload payload(YaldabaothFlight Flight, boolean Full, long Stamp) {
        int Shown = Flight.mode() == YaldabaothFlight.Mode.Banish ? YaldabaothPayload.Banishing : YaldabaothPayload.Present;
        return new YaldabaothPayload(Shown, Flight.head(), Flight.facing(), Flight.jaw(), Flight.flinch(), Flight.mode().ordinal(), Flight.portal(), Flight.bindDirection(),
                Flight.bindProgress(), Full ? Flight.trail() : List.of(), Stamp);
    }

    @Nullable
    public static Yaldabaoth body(ServerLevel World) {
        if (BodyId == null) {
            return null;
        }
        Entity Found = World.getEntity(BodyId);
        return Found instanceof Yaldabaoth Made && Made.isAlive() ? Made : null;
    }

    public static int count(ServerLevel World) {
        int Count = 0;
        for (Entity Each : World.getAllEntities()) {
            if (Each instanceof Yaldabaoth && !Each.isRemoved()) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean adopt(Yaldabaoth Made) {
        return BodyId != null && BodyId.equals(Made.getUUID());
    }

    private static void being(MinecraftServer Server, ServerLevel World, XkState State) {
        YaldabaothFlight Flight = State.flight();
        Yaldabaoth Body = body(World);
        if (!Flight.present()) {
            if (Body != null) {
                Body.discard();
            }
            BodyId = null;
            bar(Server, World, State, false);
            return;
        }
        Flight.tick(World, World.random);
        if (Flight.finishedBanish()) {
            Flight.vanish();
            if (Body != null) {
                Body.discard();
            }
            BodyId = null;
            PacketDistributor.sendToAllPlayers(gone());
            bar(Server, World, State, false);
            return;
        }
        YaldabaothFlight.Mode Mode = Flight.mode();
        Vec3 Head = Flight.head();
        BlockPos Spot = BlockPos.containing(Head.x, Math.max(World.getMinBuildHeight(), Math.min(World.getMaxBuildHeight() - 1, Head.y)), Head.z);
        boolean Holding = Mode == YaldabaothFlight.Mode.Gather || Mode == YaldabaothFlight.Mode.Arena || Mode == YaldabaothFlight.Mode.Attack
                || Mode == YaldabaothFlight.Mode.React || Mode == YaldabaothFlight.Mode.Bind || Mode == YaldabaothFlight.Mode.Dive;
        if (Holding && World.getGameTime() % 20 == 0) {
            ChunkPos Chunk = new ChunkPos(Spot);
            World.getChunkSource().addRegionTicket(Scp5001XkRegistry.Ticket, Chunk, 2, Chunk);
        }
        boolean Ticking = World.isPositionEntityTicking(Spot);
        if (Body != null && (!Ticking || Mode == YaldabaothFlight.Mode.Banish)) {
            Body.discard();
            Body = null;
            BodyId = null;
        }
        if (Body == null && Ticking && Mode != YaldabaothFlight.Mode.Banish) {
            Yaldabaoth Made = Scp5001XkRegistry.YaldabaothType.get().create(World);
            if (Made != null) {
                Made.follow(Head, Flight.facing());
                Made.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
                BodyId = Made.getUUID();
                if (World.addFreshEntity(Made)) {
                    Body = Made;
                } else {
                    BodyId = null;
                }
            }
        }
        if (Body != null) {
            Body.follow(Head, Flight.facing());
        }
        YaldabaothVoice.tick(World, Flight);
        long Time = World.getGameTime();
        if (Time % SyncEvery == 0) {
            for (ServerPlayer Player : World.players()) {
                boolean Full = Synced.add(Player.getUUID());
                PacketDistributor.sendToPlayer(Player, payload(Flight, Full, Time));
            }
        }
        if (Time % 20 == 0) {
            dread(World, Flight);
            bar(Server, World, State, true);
            WorldEnding.focus(Server, Head);
        }
    }

    public static void clearTicksForTests(int Ticks) {
        ClearOverride = Ticks;
    }

    public static void reset() {
        BodyId = null;
        if (Bar != null) {
            Bar.removeAllPlayers();
        }
        Bar = null;
        Synced.clear();
        YaldabaothVoice.reset();
        net.rai.scp_unleashed.scp5001.core.FightAudio.reset();
    }

    public static void forgetSync(UUID Player) {
        Synced.remove(Player);
    }

    private static void dread(ServerLevel World, YaldabaothFlight Flight) {
        for (ServerPlayer Player : World.players()) {
            if (Player.isSpectator() || Player.isCreative()) {
                continue;
            }
            if (Flight.nearest(Player.getEyePosition()) < DreadRange) {
                Player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, true, false, false));
            }
        }
    }

    private static void bar(MinecraftServer Server, ServerLevel World, XkState State, boolean Shown) {
        if (!Shown) {
            if (Bar != null) {
                Bar.removeAllPlayers();
                Bar = null;
            }
            return;
        }
        if (Bar == null) {
            Bar = new ServerBossEvent(Component.translatable("entity.scp_unleashed.yaldabaoth"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
            Bar.setDarkenScreen(true);
        }
        Bar.setProgress(Math.max(0.0f, 1.0f - State.sky()));
        Vec3 Head = State.flight().head();
        for (ServerPlayer Player : Server.getPlayerList().getPlayers()) {
            boolean Near = Player.level() == World && State.flight().nearest(Player.position()) < BarRange;
            if (Near) {
                Bar.addPlayer(Player);
            } else {
                Bar.removePlayer(Player);
            }
        }
        if (Head == null) {
            Bar.removeAllPlayers();
        }
    }

    public static void joined(ServerPlayer Player) {
        MinecraftServer Server = Player.getServer();
        if (Server == null) {
            return;
        }
        XkState State = XkState.get(Server);
        Synced.remove(Player.getUUID());
        PacketDistributor.sendToPlayer(Player, new XkSkyPayload(State.sky(), true));
        if (!State.flight().present()) {
            PacketDistributor.sendToPlayer(Player, gone());
        }
    }

    public static void start(MinecraftServer Server) {
        XkState State = XkState.get(Server);
        if (WorldEnding.running(Server)) {
            WorldEnding.cancel(Server);
        }
        State.wipe();
        PacketDistributor.sendToAllPlayers(gone());
        Scp5001State.get(Server).phase(Scp5001State.Phase.Xk1, Server.overworld().getGameTime());
    }

    public static void cancel(MinecraftServer Server) {
        XkState State = XkState.get(Server);
        WorldEnding.cancel(Server);
        State.flight().vanish();
        State.wipe();
        XkRising.clear();
        Scp5001State.get(Server).phase(Scp5001State.Phase.Dormant, Server.overworld().getGameTime());
        PacketDistributor.sendToAllPlayers(new XkSkyPayload(0.0f, true));
        PacketDistributor.sendToAllPlayers(gone());
        bar(Server, Server.overworld(), State, false);
        Log.info("SCP-5001: XK cancelled, world restored");
    }

    public static String skip(MinecraftServer Server) {
        Scp5001State Shared = Scp5001State.get(Server);
        XkState State = XkState.get(Server);
        long Time = Server.overworld().getGameTime();
        switch (Shared.phase()) {
            case Xk1 -> {
                State.firstElapsed(Long.MAX_VALUE / 4L);
                return "xk2";
            }
            case Xk2 -> {
                State.secondElapsed(Scp5001XkConfig.skyTicks());
                return "ended";
            }
            case Ended -> {
                return "ended";
            }
            case Summoning, Duel, Saved -> {
                return "held";
            }
            default -> {
                start(Server);
                Shared.phase(Scp5001State.Phase.Xk1, Time);
                return "xk1";
            }
        }
    }
}
