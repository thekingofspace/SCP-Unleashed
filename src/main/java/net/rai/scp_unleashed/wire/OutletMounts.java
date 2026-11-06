package net.rai.scp_unleashed.wire;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class OutletMounts {
    public static final double Pixels = 16.0;
    public static final double Reach = 3.0;
    public static final int SpotMin = 6;
    public static final int SpotMax = 10;
    private static final Direction[] Sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};

    private OutletMounts() {
    }

    public static List<Direction> faces(List<AABB> Boxes, int Width, int Height, int Depth, int Column, int Row, int Layer) {
        List<Direction> Found = new ArrayList<>();
        for (Direction Side : Sides) {
            boolean Outer = switch (Side) {
                case NORTH -> Layer == 0;
                case SOUTH -> Layer == Depth - 1;
                case WEST -> Column == 0;
                case EAST -> Column == Width - 1;
                default -> Row == Height - 1;
            };
            if (Outer && backed(Boxes, Side, Column * Pixels, Row * Pixels, Layer * Pixels)) {
                Found.add(Side);
            }
        }
        return Found;
    }

    public static AABB plate(Direction Side) {
        return switch (Side) {
            case NORTH -> new AABB(0.0, 0.0, 0.0, Pixels, Pixels, 1.0);
            case SOUTH -> new AABB(0.0, 0.0, Pixels - 1.0, Pixels, Pixels, Pixels);
            case WEST -> new AABB(0.0, 0.0, 0.0, 1.0, Pixels, Pixels);
            case EAST -> new AABB(Pixels - 1.0, 0.0, 0.0, Pixels, Pixels, Pixels);
            default -> new AABB(0.0, Pixels - 1.0, 0.0, Pixels, Pixels, Pixels);
        };
    }

    private static boolean backed(List<AABB> Boxes, Direction Side, double CellX, double CellY, double CellZ) {
        for (int U = SpotMin; U < SpotMax; U++) {
            for (int V = SpotMin; V < SpotMax; V++) {
                AABB Probe = switch (Side) {
                    case NORTH -> new AABB(CellX + U, CellY + V, CellZ, CellX + U + 1, CellY + V + 1, CellZ + Reach);
                    case SOUTH -> new AABB(CellX + U, CellY + V, CellZ + Pixels - Reach, CellX + U + 1, CellY + V + 1, CellZ + Pixels);
                    case WEST -> new AABB(CellX, CellY + U, CellZ + V, CellX + Reach, CellY + U + 1, CellZ + V + 1);
                    case EAST -> new AABB(CellX + Pixels - Reach, CellY + U, CellZ + V, CellX + Pixels, CellY + U + 1, CellZ + V + 1);
                    default -> new AABB(CellX + U, CellY + Pixels - Reach, CellZ + V, CellX + U + 1, CellY + Pixels, CellZ + V + 1);
                };
                if (!touches(Boxes, Probe)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean touches(List<AABB> Boxes, AABB Probe) {
        for (AABB Box : Boxes) {
            if (Box.intersects(Probe)) {
                return true;
            }
        }
        return false;
    }
}
