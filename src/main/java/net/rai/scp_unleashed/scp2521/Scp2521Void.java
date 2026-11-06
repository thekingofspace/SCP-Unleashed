package net.rai.scp_unleashed.scp2521;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp2521Void {
    public static final int Spacing = 1024;
    public static final int Columns = 64;
    public static final int FloorY = 160;
    public static final int StandinBaseX = -2560000;
    public static final int StandinBaseZ = -2560000;
    public static final int FloorWest = -2;
    public static final int FloorEast = 2;
    public static final int FloorNorth = -5;
    public static final int FloorSouth = 2;
    public static final double StarerOffset = -3.5;
    public static final float ArrivalYaw = 180.0f;
    public static final float StarerYaw = 0.0f;
    public static final int TicketRadius = 2;
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static volatile ResourceKey<Level> Standin;

    private Scp2521Void() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Found = Standin;
        return Found != null ? Found : Scp2521Registry.Void;
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

    public static boolean exempt(Player User) {
        return User.isSpectator() || User.isCreative() && User.hasPermissions(2);
    }

    public static int baseX() {
        return Standin != null ? StandinBaseX : 0;
    }

    public static int baseZ() {
        return Standin != null ? StandinBaseZ : 0;
    }

    public static BlockPos centre(int Plot) {
        int Column = Math.floorMod(Plot, Columns);
        int Row = Math.floorDiv(Plot, Columns);
        return new BlockPos(baseX() + Column * Spacing, FloorY, baseZ() + Row * Spacing);
    }

    public static int plotAt(double X, double Z) {
        int Column = Math.floorDiv((int) Math.floor(X) - baseX() + Spacing / 2, Spacing);
        int Row = Math.floorDiv((int) Math.floor(Z) - baseZ() + Spacing / 2, Spacing);
        if (Column < 0 || Column >= Columns || Row < 0) {
            return -1;
        }
        return Row * Columns + Column;
    }

    public static Vec3 arrival(int Plot) {
        BlockPos Centre = centre(Plot);
        return new Vec3(Centre.getX() + 0.5, FloorY + 1.0, Centre.getZ() + 0.5);
    }

    public static Vec3 starer(int Plot) {
        BlockPos Centre = centre(Plot);
        return new Vec3(Centre.getX() + 0.5, FloorY + 1.0, Centre.getZ() + 0.5 + StarerOffset);
    }

    public static AABB area(int Plot) {
        BlockPos Centre = centre(Plot);
        return new AABB(Centre.getX() + FloorWest - 4, FloorY - 80, Centre.getZ() + FloorNorth - 4, Centre.getX() + FloorEast + 5, FloorY + 12, Centre.getZ() + FloorSouth + 5);
    }

    public static BlockState floor() {
        return Blocks.BLACK_CONCRETE.defaultBlockState();
    }

    public static void hold(ServerLevel World, int Plot) {
        BlockPos Centre = centre(Plot);
        World.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(Centre), TicketRadius, Centre);
    }

    public static void release(ServerLevel World, int Plot) {
        BlockPos Centre = centre(Plot);
        World.getChunkSource().removeRegionTicket(TicketType.PORTAL, new ChunkPos(Centre), TicketRadius, Centre);
    }

    public static void build(ServerLevel World, int Plot) {
        BlockPos Centre = centre(Plot);
        BlockState Floor = floor();
        for (int X = FloorWest; X <= FloorEast; X++) {
            for (int Z = FloorNorth; Z <= FloorSouth; Z++) {
                World.setBlock(Centre.offset(X, 0, Z), Floor, Flags);
            }
        }
    }

    public static void open(ServerLevel World, int Plot) {
        BlockPos Centre = centre(Plot);
        for (int X = FloorWest; X <= FloorEast; X++) {
            for (int Z = FloorNorth; Z <= FloorSouth; Z++) {
                BlockPos Cell = Centre.offset(X, 0, Z);
                if (!World.getBlockState(Cell).isAir()) {
                    World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Flags);
                }
            }
        }
    }

    public static boolean built(ServerLevel World, int Plot) {
        BlockPos Centre = centre(Plot);
        for (int X = FloorWest; X <= FloorEast; X++) {
            for (int Z = FloorNorth; Z <= FloorSouth; Z++) {
                if (!World.getBlockState(Centre.offset(X, 0, Z)).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int wipe(ServerLevel World, int Plot) {
        open(World, Plot);
        int Removed = 0;
        for (Entity Left : World.getEntitiesOfClass(Entity.class, area(Plot), Found -> !(Found instanceof Player))) {
            Left.discard();
            Removed++;
        }
        release(World, Plot);
        return Removed;
    }
}
