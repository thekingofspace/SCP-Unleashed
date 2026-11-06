package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp860Forest {
    public static final ResourceKey<Level> Key = Scp860Registry.Forest;
    public static final int Size = Scp860Plan.Size;
    public static final int Ground = 64;
    public static final int Margin = 6;
    public static final int StandinBaseX = -2048000;
    public static final int StandinBaseZ = 2048000;
    public static final double ArrivalSpread = 0.7;
    private static volatile ResourceKey<Level> Standin;
    private static final Map<MinecraftServer, Scp860Plan> Plans = new ConcurrentHashMap<>();

    private Scp860Forest() {
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

    public static boolean strict(@Nullable Level World) {
        return inside(World) && !standingIn();
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

    public static int baseX() {
        return standingIn() ? StandinBaseX : 0;
    }

    public static int baseZ() {
        return standingIn() ? StandinBaseZ : 0;
    }

    public static boolean nearRegion(double X, double Z) {
        return X >= baseX() - Margin && X < baseX() + Size + Margin && Z >= baseZ() - Margin && Z < baseZ() + Size + Margin;
    }

    public static boolean nearRegion(BlockPos Pos) {
        return nearRegion(Pos.getX() + 0.5, Pos.getZ() + 0.5);
    }

    public static boolean covers(@Nullable Level World, BlockPos Pos) {
        return inside(World) && (!standingIn() || nearRegion(Pos));
    }

    public static boolean exempt(Player User) {
        return User.isSpectator() || User.isCreative() && User.hasPermissions(2);
    }

    public static Scp860Record record(Player User) {
        return User.getData(Scp860Registry.Ledger);
    }

    public static boolean participant(Player User) {
        if (User instanceof ServerPlayer) {
            return record(User).Active;
        }
        return inside(User);
    }

    public static boolean present(ServerPlayer User) {
        return participant(User) && inside(User) && !User.isSpectator() && (!standingIn() || nearRegion(User.getX(), User.getZ()));
    }

    public static boolean bound(Entity Target) {
        if (!inside(Target)) {
            return false;
        }
        if (Target instanceof Player User) {
            return !User.isSpectator() && participant(User);
        }
        return !standingIn() || nearRegion(Target.getX(), Target.getZ());
    }

    public static List<ServerPlayer> participants(ServerLevel World) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : World.players()) {
            if (present(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static boolean occupied(MinecraftServer Server) {
        ServerLevel World = level(Server);
        return World != null && !participants(World).isEmpty();
    }

    public static Scp860Plan plan(MinecraftServer Server) {
        long Seed = Scp860State.get(Server).seed();
        Scp860Plan Found = Plans.get(Server);
        if (Found == null || Found.Seed != Seed) {
            Found = new Scp860Plan(Seed);
            Plans.put(Server, Found);
        }
        return Found;
    }

    public static void forget(MinecraftServer Server) {
        Plans.remove(Server);
    }

    public static double localX(double X) {
        return X - baseX();
    }

    public static double localZ(double Z) {
        return Z - baseZ();
    }

    public static double distance(MinecraftServer Server, double X, double Z) {
        return plan(Server).distance(localX(X), localZ(Z));
    }

    public static Vec3 arrival(MinecraftServer Server, int Index) {
        Scp860Plan Layout = plan(Server);
        double Angle = Index * 2.399963;
        double Spread = Index == 0 ? 0.0 : ArrivalSpread;
        return new Vec3(baseX() + Layout.ArrivalX + Math.cos(Angle) * Spread, Ground + 1.0, baseZ() + Layout.ArrivalZ + Math.sin(Angle) * Spread);
    }

    public static BlockPos arrivalDoor(MinecraftServer Server) {
        Scp860Plan Layout = plan(Server);
        return new BlockPos(baseX() + Layout.ArrivalDoorX, Ground + 1, baseZ() + Layout.ArrivalDoorZ);
    }

    public static BlockPos exitDoor(MinecraftServer Server) {
        Scp860Plan Layout = plan(Server);
        return new BlockPos(baseX() + Layout.ExitDoorX, Ground + 1, baseZ() + Layout.ExitDoorZ);
    }

    public static Vec3 pathPoint(MinecraftServer Server, int Sample) {
        Scp860Plan Layout = plan(Server);
        return new Vec3(baseX() + Layout.sampleX(Sample), Ground + 1.0, baseZ() + Layout.sampleZ(Sample));
    }
}
