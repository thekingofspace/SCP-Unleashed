package net.rai.scp_unleashed.intro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class IntroWorld {
    public static final int Spacing = 2048;
    public static final int Columns = 64;
    public static final int BaseY = 64;
    public static final int MarginX = 256;
    public static final int MarginZ = 256;
    public static final BlockPos OfficeOffset = new BlockPos(0, 0, 0);
    public static final BlockPos GateRoomOffset = new BlockPos(0, 0, 40);
    public static final BlockPos OfficeSize = new BlockPos(15, 6, 15);
    public static final BlockPos GateRoomSize = new BlockPos(25, 11, 32);
    public static final BlockPos PlayerChair = new BlockPos(7, 1, 10);
    public static final BlockPos ScientistChair = new BlockPos(7, 1, 7);
    public static final BlockPos FolderSpot = new BlockPos(7, 2, 9);
    public static final Direction PlayerFacing = Direction.NORTH;
    public static final Direction ScientistFacing = Direction.SOUTH;
    public static final float OfficePitch = 30.0f;
    public static final Vec3 GateArrival = new Vec3(12.5, 1.0, 25.5);
    public static final float GateYaw = 180.0f;
    public static final BlockPos GateCore = new BlockPos(12, 0, 8);
    public static final AABB GateField = new AABB(10.0, 1.0, 8.3, 15.0, 7.0, 8.7);
    public static final AABB GateApproach = new AABB(1.0, 1.0, 9.0, 24.0, 10.0, 31.0);
    public static final int Signature = 1;
    private static volatile ResourceKey<Level> Standin;

    private IntroWorld() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Found = Standin;
        return Found != null ? Found : IntroRegistry.Dimension;
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

    public static BlockPos plotOrigin(int Plot) {
        int Column = Math.floorMod(Plot, Columns);
        int Row = Math.floorDiv(Plot, Columns);
        return new BlockPos(MarginX + Column * Spacing, BaseY, MarginZ + Row * Spacing);
    }

    public static int plotAt(double X, double Z) {
        int Column = Math.floorDiv((int) Math.floor(X) - MarginX + Spacing / 2, Spacing);
        int Row = Math.floorDiv((int) Math.floor(Z) - MarginZ + Spacing / 2, Spacing);
        if (Column < 0 || Column >= Columns || Row < 0) {
            return -1;
        }
        return Row * Columns + Column;
    }

    public static BlockPos officeOrigin(int Plot) {
        return plotOrigin(Plot).offset(OfficeOffset);
    }

    public static BlockPos gateRoomOrigin(int Plot) {
        return plotOrigin(Plot).offset(GateRoomOffset);
    }

    public static AABB plotBounds(int Plot) {
        BlockPos Office = officeOrigin(Plot);
        BlockPos Room = gateRoomOrigin(Plot);
        AABB First = new AABB(Vec3.atLowerCornerOf(Office), Vec3.atLowerCornerOf(Office.offset(OfficeSize)));
        AABB Second = new AABB(Vec3.atLowerCornerOf(Room), Vec3.atLowerCornerOf(Room.offset(GateRoomSize)));
        return First.minmax(Second);
    }

    public static List<ChunkPos> plotChunks(int Plot) {
        AABB Bounds = plotBounds(Plot);
        List<ChunkPos> Found = new ArrayList<>();
        int MinX = ((int) Math.floor(Bounds.minX)) >> 4;
        int MaxX = ((int) Math.floor(Bounds.maxX)) >> 4;
        int MinZ = ((int) Math.floor(Bounds.minZ)) >> 4;
        int MaxZ = ((int) Math.floor(Bounds.maxZ)) >> 4;
        for (int X = MinX; X <= MaxX; X++) {
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                Found.add(new ChunkPos(X, Z));
            }
        }
        return Found;
    }

    public static BlockPos playerChair(int Plot) {
        return officeOrigin(Plot).offset(PlayerChair);
    }

    public static BlockPos scientistChair(int Plot) {
        return officeOrigin(Plot).offset(ScientistChair);
    }

    public static Vec3 officeSeat(int Plot) {
        return Vec3.atBottomCenterOf(playerChair(Plot));
    }

    public static float officeYaw() {
        return PlayerFacing.toYRot();
    }

    public static Vec3 gateArrival(int Plot) {
        return Vec3.atLowerCornerOf(gateRoomOrigin(Plot)).add(GateArrival);
    }

    public static BlockPos gateCore(int Plot) {
        return gateRoomOrigin(Plot).offset(GateCore);
    }

    public static AABB gateField(int Plot) {
        return GateField.move(Vec3.atLowerCornerOf(gateRoomOrigin(Plot)));
    }

    public static Vec3 gateCentre(int Plot) {
        return gateField(Plot).getCenter();
    }

    public static AABB gateApproach(int Plot) {
        return GateApproach.move(Vec3.atLowerCornerOf(gateRoomOrigin(Plot)));
    }
}
