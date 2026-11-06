package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp2317World {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, Scp2317Registry.location("scp2317_desert"));
    public static final int StandinBaseX = 2304000;
    public static final int StandinBaseZ = -2816000;
    public static final double RegionRadius = 160.0;
    private static volatile ResourceKey<Level> Standin;

    private Scp2317World() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Borrowed = Standin;
        return Borrowed != null ? Borrowed : Key;
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static int baseX() {
        return standingIn() ? StandinBaseX : 0;
    }

    public static int baseZ() {
        return standingIn() ? StandinBaseZ : 0;
    }

    public static BlockPos at(int X, int Y, int Z) {
        return new BlockPos(baseX() + X, Y, baseZ() + Z);
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == key();
    }

    public static boolean inside(@Nullable Entity Body) {
        return Body != null && inside(Body.level());
    }

    public static boolean strict(@Nullable Level World) {
        return inside(World) && !standingIn();
    }

    public static boolean nearRegion(double X, double Z) {
        double Dx = X - baseX() - 0.5;
        double Dz = Z - baseZ() - 0.5;
        return Dx * Dx + Dz * Dz <= RegionRadius * RegionRadius;
    }

    public static boolean present(Entity Body) {
        return inside(Body) && !Body.isSpectator() && Body.isAlive() && (!standingIn() || nearRegion(Body.getX(), Body.getZ()));
    }

    public static List<ServerPlayer> players(MinecraftServer Server) {
        List<ServerPlayer> Found = new ArrayList<>();
        ServerLevel World = level(Server);
        if (World == null) {
            return Found;
        }
        for (ServerPlayer User : World.players()) {
            if (present(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static boolean occupied(MinecraftServer Server) {
        return !players(Server).isEmpty();
    }

    public static Vec3 centre() {
        return new Vec3(baseX() + 0.5, Scp2317Plan.Ground + 1.0, baseZ() + 0.5);
    }

    public static Vec3 vase() {
        return new Vec3(baseX() + 0.5, Scp2317Plan.Ground + 2.5, baseZ() + 0.5);
    }

    public static BlockPos vasePos() {
        return at(0, Scp2317Plan.Ground + 2, 0);
    }

    public static BlockPos kingPos() {
        return at(0, Scp2317Plan.CaveFloor + 1, 0);
    }

    public static BlockPos farDoor() {
        return at(Scp2317Plan.FarDoorX, Scp2317Plan.Ground + 1, Scp2317Plan.DoorZ);
    }

    public static Vec3 arrival(int Index) {
        double Angle = Index * 2.399963;
        double Spread = Index == 0 ? 0.0 : 0.8;
        return new Vec3(baseX() + Scp2317Plan.FarDoorX + 1.0 + Math.cos(Angle) * Spread, Scp2317Plan.Ground + 1.0, baseZ() + Scp2317Plan.DoorZ - 3.0 + Math.sin(Angle) * Spread);
    }
}
