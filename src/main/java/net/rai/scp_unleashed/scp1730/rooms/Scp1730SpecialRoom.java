package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record Scp1730SpecialRoom(Scp1730RoomKind Kind, int MinX, int MinZ, int MaxX, int MaxZ, int Floor, int Height, List<Door> Doors) {
    public record Door(int X, int Z, int Dir) {
        public static final int[] Dx = {0, 1, 0, -1};
        public static final int[] Dz = {-1, 0, 1, 0};

        public int outX() {
            return Dx[Dir];
        }

        public int outZ() {
            return Dz[Dir];
        }

        public int sideX() {
            return Dir % 2 == 0 ? 1 : 0;
        }

        public int sideZ() {
            return Dir % 2 == 0 ? 0 : 1;
        }

        public Vec3 centre(int Floor) {
            return new Vec3(X + 0.5, Floor + 1, Z + 0.5);
        }

        public double outward(Vec3 Point) {
            return (Point.x - (X + 0.5)) * outX() + (Point.z - (Z + 0.5)) * outZ();
        }

        public double lateral(Vec3 Point) {
            return (Point.x - (X + 0.5)) * sideX() + (Point.z - (Z + 0.5)) * sideZ();
        }

        public float yaw() {
            return switch (Dir) {
                case 0 -> 180.0f;
                case 1 -> -90.0f;
                case 3 -> 90.0f;
                default -> 0.0f;
            };
        }
    }

    public AABB box() {
        return new AABB(MinX, Floor, MinZ, MaxX + 1, Floor + Height + 2, MaxZ + 1);
    }

    public AABB inner() {
        return new AABB(MinX + 1, Floor + 1, MinZ + 1, MaxX, Floor + Height + 1, MaxZ);
    }

    public boolean contains(double X, double Y, double Z) {
        return X >= MinX && X < MaxX + 1 && Z >= MinZ && Z < MaxZ + 1 && Y >= Floor && Y < Floor + Height + 2;
    }

    public boolean contains(Vec3 Point) {
        return contains(Point.x, Point.y, Point.z);
    }

    public boolean holds(BlockPos Pos) {
        return contains(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5);
    }

    public boolean within(Vec3 Point) {
        return Point.x >= MinX + 1 && Point.x < MaxX && Point.z >= MinZ + 1 && Point.z < MaxZ && Point.y >= Floor + 0.5 && Point.y < Floor + Height + 1.5;
    }

    public boolean within(AABB Body) {
        return Body.minX >= MinX + 1 && Body.maxX <= MaxX && Body.minZ >= MinZ + 1 && Body.maxZ <= MaxZ && Body.minY >= Floor + 0.5 && Body.minY < Floor + Height + 1;
    }

    public Vec3 centre() {
        return new Vec3((MinX + MaxX + 1) / 2.0, Floor + 1, (MinZ + MaxZ + 1) / 2.0);
    }

    public double distanceToEdge(Vec3 Point) {
        double Dx = Math.min(Point.x - MinX, MaxX + 1 - Point.x);
        double Dz = Math.min(Point.z - MinZ, MaxZ + 1 - Point.z);
        return Math.min(Dx, Dz);
    }
}
