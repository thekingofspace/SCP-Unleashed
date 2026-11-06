package net.rai.scp_unleashed.scp4666;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayStash;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class Scp4666World {
    public static final ResourceKey<Level> Key = Scp4666Registry.Dimension;
    private static volatile ResourceKey<Level> Standin;
    public static final int Signature = 9;
    public static final int RegionRadius = 62;
    public static final double FogStart = 24.0;
    public static final double FogEdge = 50.0;
    public static final double TurnRadius = 50.0;
    public static final double TurnInset = 3.0;
    public static final double StalkRadius = 30.0;
    public static final double StalkHalfWidth = 1.6;
    public static final double SpawnClearing = 5.0;
    public static final double CabinClearing = 19.0;
    public static final int SkyReach = 24;
    private static long Stilled;
    private static volatile boolean Announced;
    private static final Logger Log = LogUtils.getLogger();

    private Scp4666World() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Found = Standin;
        return Found != null ? Found : Key;
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static boolean participant(Player User) {
        if (!standingIn()) {
            return true;
        }
        if (!(User instanceof ServerPlayer Visitor)) {
            return false;
        }
        GatewayStash.Entry Found = GatewayStash.get(Visitor.server).entry(Visitor.getUUID());
        return Found != null && (Scp4666Destination.Id.equals(Found.Destination) || Scp4666Destination.StandinId.equals(Found.Destination));
    }

    public static boolean nearSite(MinecraftServer Server, BlockPos Pos) {
        BlockPos Centre = centre(Server);
        return Math.abs(Pos.getX() - Centre.getX()) <= RegionRadius && Math.abs(Pos.getZ() - Centre.getZ()) <= RegionRadius;
    }

    public static boolean covers(@Nullable Level World, BlockPos Pos) {
        if (!inside(World)) {
            return false;
        }
        return !standingIn() || World.getServer() != null && nearSite(World.getServer(), Pos);
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == key();
    }

    public static boolean inside(Entity Target) {
        return inside(Target.level());
    }

    public static boolean still(Level World, BlockPos Pos) {
        if (!stills(World.dimension(), World.getServer() != null && nearSite(World.getServer(), Pos))) {
            return false;
        }
        Stilled++;
        return true;
    }

    public static boolean stills(ResourceKey<Level> Dimension, boolean NearSite) {
        if (Dimension == Key) {
            if (!Announced) {
                Announced = true;
                Log.info("SCP-4666: snowfall is stilled in {}, no snow will settle there", Dimension.location());
            }
            return true;
        }
        return standingIn() && Dimension == Standin && NearSite;
    }

    public static long stilled() {
        return Stilled;
    }

    public static boolean openToSky(BlockGetter World, BlockPos Pos) {
        BlockPos.MutableBlockPos Probe = Pos.mutable();
        for (int Step = 0; Step < SkyReach; Step++) {
            BlockState Found = World.getBlockState(Probe);
            if (Found.blocksMotion() || !Found.getFluidState().isEmpty()) {
                return false;
            }
            Probe.move(Direction.UP);
        }
        return true;
    }

    public static boolean exempt(Player User) {
        return User.isSpectator() || User.isCreative() && User.hasPermissions(2);
    }

    public static List<ServerPlayer> visitors(ServerLevel World) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : World.players()) {
            if (!User.isSpectator() && participant(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static List<ServerPlayer> living(ServerLevel World) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : World.players()) {
            if (!User.isSpectator() && User.isAlive() && participant(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static void ensureBuilt(ServerLevel World) {
        if (Scp4666Builder.pending(World.getServer())) {
            Scp4666Builder.buildNow(World);
            return;
        }
        upgrade(World);
    }

    public static boolean inProgress(Scp4666State State) {
        return State.started() && State.built() >= 0;
    }

    public static void upgrade(ServerLevel World) {
        Scp4666State State = Scp4666State.get(World.getServer());
        if (State.built() == Signature) {
            return;
        }
        if (inProgress(State)) {
            Scp4666Builder.migrate(World);
        } else if (mayRebuild(World)) {
            Scp4666Builder.buildNow(World);
        }
    }

    public static boolean mayRebuild(ServerLevel World) {
        return Scp4666State.get(World.getServer()).built() < 0 || visitors(World).isEmpty();
    }

    public static Vec3 arrival(MinecraftServer Server, int Index) {
        Scp4666Map Layout = Scp4666Map.get(Server);
        double Offset = (Index % 3 - 1) * 0.8;
        return Layout.Spawn.add(Offset, 0.0, 0.0);
    }

    public static BlockPos centre(MinecraftServer Server) {
        return Scp4666Map.get(Server).Centre;
    }
}
