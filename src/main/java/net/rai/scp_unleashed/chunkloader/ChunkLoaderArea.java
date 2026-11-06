package net.rai.scp_unleashed.chunkloader;

import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.Comparator;

public final class ChunkLoaderArea {
    public static final int HardMaxRadius = 32;
    public static final int ChunkSize = 16;
    private static final int[] OffsetX;
    private static final int[] OffsetZ;

    static {
        int Side = side(HardMaxRadius);
        Integer[] Order = new Integer[Side * Side];
        for (int Index = 0; Index < Order.length; Index++) {
            Order[Index] = Index;
        }
        Arrays.sort(Order, Comparator.<Integer>comparingInt(Index -> ring(Index % Side - HardMaxRadius, Index / Side - HardMaxRadius))
                .thenComparingInt(Index -> square(Index % Side - HardMaxRadius) + square(Index / Side - HardMaxRadius))
                .thenComparingDouble(Index -> Math.atan2(Index / Side - HardMaxRadius, Index % Side - HardMaxRadius)));
        OffsetX = new int[Order.length];
        OffsetZ = new int[Order.length];
        for (int Index = 0; Index < Order.length; Index++) {
            OffsetX[Index] = Order[Index] % Side - HardMaxRadius;
            OffsetZ[Index] = Order[Index] / Side - HardMaxRadius;
        }
    }

    private ChunkLoaderArea() {
    }

    private static int square(int Value) {
        return Value * Value;
    }

    public static int ring(int Dx, int Dz) {
        return Math.max(Math.abs(Dx), Math.abs(Dz));
    }

    public static int side(int Radius) {
        return Math.max(0, Radius) * 2 + 1;
    }

    public static int chunkCount(int Radius) {
        int Side = side(Radius);
        return Side * Side;
    }

    public static int blockSpan(int Radius) {
        return side(Radius) * ChunkSize;
    }

    public static int offsetX(int Index) {
        return OffsetX[Index];
    }

    public static int offsetZ(int Index) {
        return OffsetZ[Index];
    }

    public static int demand(int Radius, int Base, int PerChunk) {
        long Total = (long) Math.max(0, Base) + (long) Math.max(0, PerChunk) * chunkCount(Radius);
        return (int) Math.min(Integer.MAX_VALUE, Total);
    }

    public static boolean contains(ChunkPos Centre, int Radius, long Chunk) {
        return ring(ChunkPos.getX(Chunk) - Centre.x, ChunkPos.getZ(Chunk) - Centre.z) <= Radius;
    }

    public static long chunkAt(ChunkPos Centre, int Index) {
        return ChunkPos.asLong(Centre.x + OffsetX[Index], Centre.z + OffsetZ[Index]);
    }
}
