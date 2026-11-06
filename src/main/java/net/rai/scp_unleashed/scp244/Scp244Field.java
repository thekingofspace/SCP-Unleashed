package net.rai.scp_unleashed.scp244;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class Scp244Field {
    private final BlockPos Origin;
    private final long[] Keys;
    private final float[] Distances;
    private final Long2IntOpenHashMap Index;
    private final AABB Bounds;

    public Scp244Field(BlockPos Origin, long[] Keys, float[] Distances) {
        this.Origin = Origin.immutable();
        this.Keys = Keys;
        this.Distances = Distances;
        this.Index = new Long2IntOpenHashMap(Keys.length);
        this.Index.defaultReturnValue(-1);
        int MinX = Origin.getX();
        int MinY = Origin.getY();
        int MinZ = Origin.getZ();
        int MaxX = MinX;
        int MaxY = MinY;
        int MaxZ = MinZ;
        for (int Slot = 0; Slot < Keys.length; Slot++) {
            long Key = Keys[Slot];
            Index.put(Key, Slot);
            int X = BlockPos.getX(Key);
            int Y = BlockPos.getY(Key);
            int Z = BlockPos.getZ(Key);
            MinX = Math.min(MinX, X);
            MinY = Math.min(MinY, Y);
            MinZ = Math.min(MinZ, Z);
            MaxX = Math.max(MaxX, X);
            MaxY = Math.max(MaxY, Y);
            MaxZ = Math.max(MaxZ, Z);
        }
        this.Bounds = new AABB(MinX, MinY, MinZ, MaxX + 1, MaxY + 1, MaxZ + 1);
    }

    public BlockPos origin() {
        return Origin;
    }

    public int size() {
        return Keys.length;
    }

    public long key(int Slot) {
        return Keys[Slot];
    }

    public float distance(int Slot) {
        return Distances[Slot];
    }

    public int indexOf(long Key) {
        return Index.get(Key);
    }

    public int indexOf(BlockPos Pos) {
        return Index.get(Pos.asLong());
    }

    public boolean contains(BlockPos Pos) {
        return Index.containsKey(Pos.asLong());
    }

    public AABB bounds() {
        return Bounds;
    }

    public float distanceAt(BlockPos Pos) {
        int Slot = indexOf(Pos);
        return Slot < 0 ? -1.0f : Distances[Slot];
    }
}
