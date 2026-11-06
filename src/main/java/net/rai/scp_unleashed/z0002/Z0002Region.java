package net.rai.scp_unleashed.z0002;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record Z0002Region(BlockPos Centre, int Radius) {
    public static final double EscapeMargin = 0.3;

    public int size() {
        return Radius * 2 + 1;
    }

    public int limit() {
        return Radius * Radius + Radius;
    }

    public int minX() {
        return Centre.getX() - Radius;
    }

    public int minZ() {
        return Centre.getZ() - Radius;
    }

    public int maxX() {
        return Centre.getX() + Radius;
    }

    public int maxZ() {
        return Centre.getZ() + Radius;
    }

    public int floorY() {
        return Centre.getY();
    }

    public int topY() {
        return Centre.getY() + Radius;
    }

    public boolean contains(int X, int Y, int Z) {
        int Dy = Y - Centre.getY();
        if (Dy < 0 || Dy > Radius) {
            return false;
        }
        int Dx = X - Centre.getX();
        int Dz = Z - Centre.getZ();
        return Dx * Dx + Dy * Dy + Dz * Dz <= limit();
    }

    public boolean contains(BlockPos Pos) {
        return contains(Pos.getX(), Pos.getY(), Pos.getZ());
    }

    public boolean isShell(int X, int Y, int Z) {
        if (!contains(X, Y, Z)) {
            return false;
        }
        return !contains(X + 1, Y, Z) || !contains(X - 1, Y, Z) || !contains(X, Y, Z + 1) || !contains(X, Y, Z - 1) || !contains(X, Y + 1, Z);
    }

    public boolean isShell(BlockPos Pos) {
        return isShell(Pos.getX(), Pos.getY(), Pos.getZ());
    }

    public boolean isFloor(int X, int Y, int Z) {
        return Y == floorY() && contains(X, Y, Z) && !isShell(X, Y, Z);
    }

    public boolean isFloor(BlockPos Pos) {
        return isFloor(Pos.getX(), Pos.getY(), Pos.getZ());
    }

    public int index(int X, int Y, int Z) {
        int Size = size();
        return (X - minX()) + (Z - minZ()) * Size + (Y - floorY()) * Size * Size;
    }

    public int index(BlockPos Pos) {
        return index(Pos.getX(), Pos.getY(), Pos.getZ());
    }

    public BlockPos position(int Index) {
        int Size = size();
        int Layer = Size * Size;
        int Y = Index / Layer;
        int Rest = Index - Y * Layer;
        int Z = Rest / Size;
        int X = Rest - Z * Size;
        return new BlockPos(minX() + X, floorY() + Y, minZ() + Z);
    }

    public void forEach(Consumer<BlockPos> Visitor) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Y = floorY(); Y <= topY(); Y++) {
            for (int Z = minZ(); Z <= maxZ(); Z++) {
                for (int X = minX(); X <= maxX(); X++) {
                    if (contains(X, Y, Z)) {
                        Visitor.accept(Cursor.set(X, Y, Z));
                    }
                }
            }
        }
    }

    public List<ChunkPos> chunks() {
        List<ChunkPos> Found = new ArrayList<>();
        for (int Cx = minX() >> 4; Cx <= maxX() >> 4; Cx++) {
            for (int Cz = minZ() >> 4; Cz <= maxZ() >> 4; Cz++) {
                Found.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found;
    }

    public AABB box() {
        return new AABB(minX(), floorY(), minZ(), maxX() + 1, topY() + 1, maxZ() + 1);
    }

    public Vec3 floorCentre() {
        return new Vec3(Centre.getX() + 0.5, Centre.getY() + 1.0, Centre.getZ() + 0.5);
    }

    public double reach(Vec3 Point) {
        double Dx = Point.x - (Centre.getX() + 0.5);
        double Dy = Point.y - (Centre.getY() + 0.5);
        double Dz = Point.z - (Centre.getZ() + 0.5);
        return Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
    }

    public boolean holds(Vec3 Feet, double BodyHeight) {
        if (Feet.y < floorY() + 1.0 - 0.6) {
            return false;
        }
        return reach(Feet.add(0.0, BodyHeight * 0.5, 0.0)) <= Radius - EscapeMargin;
    }

    public boolean touches(Vec3 Point) {
        return Point.y >= floorY() - 0.5 && reach(Point) <= Radius + 1.0;
    }

    public boolean overlaps(Z0002Region Other, int Gap) {
        double Dx = Centre.getX() - Other.Centre.getX();
        double Dy = Centre.getY() - Other.Centre.getY();
        double Dz = Centre.getZ() - Other.Centre.getZ();
        return Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) < Radius + Other.Radius + Gap;
    }
}
