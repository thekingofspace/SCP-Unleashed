package net.rai.scp_unleashed.worldend;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.discord.DiscordApocalypse;
import net.rai.scp_unleashed.worldend.network.WorldEndPayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorldEnding {
    public static final int FreezeTicks = 360;
    public static final int FinalGrace = 200;
    public static final String KickKey = "message.scp_unleashed.worldend.kick";
    private static final Logger Log = LogUtils.getLogger();
    private static final List<String> DryRun = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Frozen> Held = new HashMap<>();
    private static boolean ForceDryRun;
    @Nullable
    private static WorldEndTarget Pending;
    @Nullable
    private static MinecraftServer PendingServer;

    public record Frozen(Vec3 Position, float Yaw, float Pitch) {
    }

    private WorldEnding() {
    }

    public static boolean begin(MinecraftServer Server, Component Reason, ResourceLocation Source) {
        return begin(Server, Reason, Source, null);
    }

    public static boolean begin(MinecraftServer Server, Component Reason, ResourceLocation Source, @Nullable Vec3 Focus) {
        WorldEndState State = WorldEndState.get(Server);
        if (State.stage() == WorldEndState.Stage.Freezing || State.stage() == WorldEndState.Stage.Final) {
            return false;
        }
        State.begin(Component.Serializer.toJson(Reason, Server.registryAccess()), Source.toString());
        State.focus(Focus != null, Focus == null ? 0.0 : Focus.x, Focus == null ? 0.0 : Focus.y, Focus == null ? 0.0 : Focus.z);
        Held.clear();
        Log.warn("World ending begun by {}: '{}'. scpWorldEndings={}, allowWorldDeletion={}, dry run={}", Source, Reason.getString(), WorldEndRules.endings(Server),
                WorldEndConfig.allowDeletion(), dryRun(Server));
        broadcast(Server, State);
        DiscordApocalypse.worldEnding(Server, Source, Reason, Focus);
        return true;
    }

    public static void focus(MinecraftServer Server, @Nullable Vec3 Focus) {
        WorldEndState State = WorldEndState.get(Server);
        State.focus(Focus != null, Focus == null ? 0.0 : Focus.x, Focus == null ? 0.0 : Focus.y, Focus == null ? 0.0 : Focus.z);
    }

    public static void cancel(MinecraftServer Server) {
        WorldEndState State = WorldEndState.get(Server);
        if (State.stage() == WorldEndState.Stage.Idle) {
            return;
        }
        Log.info("World ending cancelled");
        State.stage(WorldEndState.Stage.Idle);
        Held.clear();
        Pending = null;
        PendingServer = null;
        broadcast(Server, State);
    }

    public static boolean running(MinecraftServer Server) {
        WorldEndState.Stage Now = WorldEndState.get(Server).stage();
        return Now == WorldEndState.Stage.Freezing || Now == WorldEndState.Stage.Final;
    }

    public static WorldEndState.Stage stage(MinecraftServer Server) {
        return WorldEndState.get(Server).stage();
    }

    public static boolean holds(ServerPlayer Player) {
        MinecraftServer Server = Player.getServer();
        return Server != null && running(Server);
    }

    public static boolean dryRun(MinecraftServer Server) {
        return ForceDryRun || Server instanceof GameTestServer;
    }

    public static void forceDryRun(boolean Value) {
        ForceDryRun = Value;
    }

    public static List<String> dryRunRecords() {
        synchronized (DryRun) {
            return List.copyOf(DryRun);
        }
    }

    public static void clearDryRun() {
        DryRun.clear();
    }

    @Nullable
    public static WorldEndTarget pending() {
        return Pending;
    }

    static void tick(MinecraftServer Server) {
        WorldEndState State = WorldEndState.get(Server);
        switch (State.stage()) {
            case Freezing -> {
                State.advance();
                hold(Server);
                if (State.elapsed() % 100 == 0) {
                    broadcast(Server, State);
                }
                if (State.elapsed() >= FreezeTicks) {
                    conclude(Server, State);
                }
            }
            case Final -> {
                State.advance();
                hold(Server);
                if (Pending == null) {
                    conclude(Server, State);
                } else if (State.elapsed() >= FinalGrace && Server.isRunning()) {
                    Log.warn("World ending: the host did not leave in time, stopping the server");
                    Server.halt(false);
                }
            }
            default -> {
            }
        }
    }

    private static void hold(MinecraftServer Server) {
        for (ServerPlayer Player : Server.getPlayerList().getPlayers()) {
            Frozen Spot = Held.computeIfAbsent(Player.getUUID(), Key -> new Frozen(Player.position(), Player.getYRot(), Player.getXRot()));
            Player.setDeltaMovement(Vec3.ZERO);
            Player.fallDistance = 0.0f;
            if (Player.position().distanceToSqr(Spot.Position()) > 0.0025) {
                Player.connection.teleport(Spot.Position().x, Spot.Position().y, Spot.Position().z, Player.getYRot(), Player.getXRot());
            }
        }
    }

    private static void conclude(MinecraftServer Server, WorldEndState State) {
        boolean Rule = WorldEndRules.endings(Server);
        boolean Allowed = WorldEndConfig.allowDeletion();
        if (!Rule || !Allowed) {
            Log.warn("World ending finished without deleting anything (scpWorldEndings={}, allowWorldDeletion={}), releasing players", Rule, Allowed);
            release(Server, State);
            return;
        }
        WorldEndTarget Target = WorldEndTarget.resolve(Server);
        if (dryRun(Server)) {
            String Where = Target == null ? "an unresolved world" : Target.Folder().toString();
            if (DiscordApocalypse.lastWords(Server, State.source(), Target == null ? Server.getWorldData().getLevelName() : Target.Name())) {
                DryRun.add("discord final notice flushed");
            }
            DryRun.add("delete " + Where);
            DryRun.add(Server.isDedicatedServer() ? "kick and halt" : "disconnect host");
            Log.warn("World ending dry run: would delete {} and stop the server", Where);
            release(Server, State);
            return;
        }
        if (Target == null) {
            Log.error("World ending: no safe world folder could be resolved, releasing players instead");
            release(Server, State);
            return;
        }
        Pending = Target;
        PendingServer = Server;
        State.stage(WorldEndState.Stage.Final);
        Log.warn("World ending: final stage for world '{}' at {}", Target.Name(), Target.Folder());
        DiscordApocalypse.lastWords(Server, State.source(), Target.Name());
        Component Kick = Component.translatable(KickKey);
        if (Server.isDedicatedServer()) {
            for (ServerPlayer Player : List.copyOf(Server.getPlayerList().getPlayers())) {
                Player.connection.disconnect(Kick);
            }
            Server.halt(false);
            return;
        }
        for (ServerPlayer Player : List.copyOf(Server.getPlayerList().getPlayers())) {
            if (Server.isSingleplayerOwner(Player.getGameProfile())) {
                PacketDistributor.sendToPlayer(Player, payload(Server, State));
            } else {
                Player.connection.disconnect(Kick);
            }
        }
    }

    private static void release(MinecraftServer Server, WorldEndState State) {
        State.stage(WorldEndState.Stage.Released);
        Held.clear();
        broadcast(Server, State);
    }

    static void stopped(MinecraftServer Server) {
        WorldEndTarget Target = Pending;
        if (Target == null || PendingServer != Server) {
            return;
        }
        Pending = null;
        PendingServer = null;
        if (dryRun(Server) || !WorldEndConfig.allowDeletion()) {
            Log.warn("World ending: deletion skipped at shutdown");
            return;
        }
        Target.delete();
    }

    static void forget(Iterable<UUID> Left) {
        for (UUID Each : Left) {
            Held.remove(Each);
        }
    }

    public static WorldEndPayload payload(MinecraftServer Server, WorldEndState State) {
        int Stage = switch (State.stage()) {
            case Idle -> WorldEndPayload.Idle;
            case Freezing -> WorldEndPayload.Freezing;
            case Released -> WorldEndPayload.Released;
            case Final -> WorldEndPayload.Final;
        };
        return new WorldEndPayload(Stage, State.elapsed(), FreezeTicks, State.focused(), new Vec3(State.focusX(), State.focusY(), State.focusZ()),
                State.reasonComponent(Server.registryAccess()));
    }

    static void broadcast(MinecraftServer Server, WorldEndState State) {
        PacketDistributor.sendToAllPlayers(payload(Server, State));
    }

    static void join(ServerPlayer Player) {
        MinecraftServer Server = Player.getServer();
        if (Server == null) {
            return;
        }
        WorldEndState State = WorldEndState.get(Server);
        if (State.stage() == WorldEndState.Stage.Freezing) {
            PacketDistributor.sendToPlayer(Player, payload(Server, State));
        }
    }
}
