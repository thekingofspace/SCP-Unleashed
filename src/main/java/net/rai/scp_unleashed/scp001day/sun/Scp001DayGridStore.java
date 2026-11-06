package net.rai.scp_unleashed.scp001day.sun;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.core.BlockPos;

public final class Scp001DayGridStore {
    public static final int Span = 128;
    public static final int Coarse = 8;
    public static final int CoarseSpan = Span / Coarse;
    public static final int Lead = 24;
    public static final int Below = 40;
    public static final int Drift = 12;
    public static final int DirtyBudget = 512;
    public static final int DirtyBacklog = 16384;
    public static final int Columns = Span * Span;
    public static final int GridBytes = Span * Span * Span * 2;
    public static final int CoarseBytes = CoarseSpan * CoarseSpan * CoarseSpan;

    public interface Memory {
        ByteBuffer alloc(int Bytes);

        ByteBuffer calloc(int Bytes);

        void free(ByteBuffer Buffer);
    }

    public interface Cells {
        boolean column(int X, int Z, int Y0, int[] Out);

        int cell(int X, int Y, int Z);
    }

    private final Memory Heap;
    private final LongArrayFIFOQueue Dirty = new LongArrayFIFOQueue();
    private final LongOpenHashSet DirtySet = new LongOpenHashSet();
    private final int[] CoarseCounts = new int[CoarseBytes];
    private final int[] Column = new int[Span];
    private ByteBuffer Live;
    private ByteBuffer Building;
    private ByteBuffer CoarseData;
    private Object LiveDimension;
    private Object BuildDimension;
    private int OriginX;
    private int OriginY;
    private int OriginZ;
    private int BuildX;
    private int BuildY;
    private int BuildZ;
    private int BuildIndex;
    private volatile boolean Ready;
    private volatile boolean Stale;
    private volatile boolean Overflow;
    private int Version;
    private int BuildLoaded;
    private int LiveLoaded;

    public Scp001DayGridStore(Memory Heap) {
        this.Heap = Heap;
    }

    public static int index(int Lx, int Ly, int Lz) {
        return ((Lz * Span + Ly) * Span + Lx) * 2;
    }

    public static boolean inside(int Lx, int Ly, int Lz) {
        return Lx >= 0 && Ly >= 0 && Lz >= 0 && Lx < Span && Ly < Span && Lz < Span;
    }

    public static int coarseIndex(int Lx, int Ly, int Lz) {
        return ((Lz / Coarse) * CoarseSpan + Ly / Coarse) * CoarseSpan + Lx / Coarse;
    }

    public static int wantX(double CameraX, SunVector Sun) {
        double Flat = Sun.horizontal();
        return (int) Math.floor(CameraX + (Flat > 1.0e-6 ? Sun.X() / Flat * Lead : 0.0)) - Span / 2;
    }

    public static int wantY(double CameraY) {
        return (int) Math.floor(CameraY) - Below;
    }

    public static int wantZ(double CameraZ, SunVector Sun) {
        double Flat = Sun.horizontal();
        return (int) Math.floor(CameraZ + (Flat > 1.0e-6 ? Sun.Z() / Flat * Lead : 0.0)) - Span / 2;
    }

    public boolean ready() {
        return Ready;
    }

    public boolean building() {
        return Building != null;
    }

    public int progress() {
        return Building == null ? (Ready ? 100 : 0) : Math.round(100.0f * BuildIndex / Columns);
    }

    public int loaded() {
        return Ready ? Math.round(100.0f * LiveLoaded / Columns) : Math.round(100.0f * BuildLoaded / Math.max(1, BuildIndex));
    }

    public int version() {
        return Version;
    }

    public int originX() {
        return OriginX;
    }

    public int originY() {
        return OriginY;
    }

    public int originZ() {
        return OriginZ;
    }

    public Object dimension() {
        return LiveDimension;
    }

    public ByteBuffer live() {
        return Ready ? Live : null;
    }

    public ByteBuffer coarse() {
        return Ready ? CoarseData : null;
    }

    public int pending() {
        synchronized (Dirty) {
            return Dirty.size();
        }
    }

    public void invalidate(int ChunkX, int ChunkZ) {
        if (!Ready) {
            return;
        }
        int MinX = ChunkX * 16;
        int MinZ = ChunkZ * 16;
        if (MinX + 15 >= OriginX && MinX < OriginX + Span && MinZ + 15 >= OriginZ && MinZ < OriginZ + Span) {
            Stale = true;
        }
    }

    public void markDirty(long Packed) {
        synchronized (Dirty) {
            if (DirtySet.add(Packed)) {
                Dirty.enqueue(Packed);
                if (Dirty.size() > DirtyBacklog) {
                    Dirty.clear();
                    DirtySet.clear();
                    Overflow = true;
                    Ready = false;
                }
            }
        }
    }

    public boolean step(Object Dimension, double CameraX, double CameraY, double CameraZ, SunVector Sun, Cells Source, long Deadline, int ColumnCap) {
        if (CoarseData == null) {
            CoarseData = Heap.calloc(CoarseBytes);
        }
        if (Overflow) {
            Overflow = false;
            Ready = false;
            if (Building != null) {
                Heap.free(Building);
                Building = null;
            }
        }
        int WantX = wantX(CameraX, Sun);
        int WantY = wantY(CameraY);
        int WantZ = wantZ(CameraZ, Sun);
        boolean Moved = Stale || !Ready || LiveDimension != Dimension || Math.abs(WantX - OriginX) > Drift || Math.abs(WantY - OriginY) > Drift || Math.abs(WantZ - OriginZ) > Drift;
        if (Moved && (Building == null || BuildDimension != Dimension)) {
            Stale = false;
            begin(Dimension, WantX, WantY, WantZ);
        }
        boolean Changed = false;
        if (Building != null) {
            Changed = continueBuild(Source, Deadline, ColumnCap);
        }
        if (Ready && drainDirty(Source)) {
            Changed = true;
        }
        if (Changed) {
            Version++;
        }
        return Changed;
    }

    private void begin(Object Dimension, int X, int Y, int Z) {
        if (Building == null) {
            Building = Heap.alloc(GridBytes);
        }
        BuildDimension = Dimension;
        BuildX = X;
        BuildY = Y;
        BuildZ = Z;
        BuildIndex = 0;
        BuildLoaded = 0;
    }

    private boolean continueBuild(Cells Source, long Deadline, int ColumnCap) {
        int Done = 0;
        while (BuildIndex < Columns && Done < ColumnCap && System.nanoTime() < Deadline) {
            if (fillColumn(Source, BuildIndex % Span, BuildIndex / Span)) {
                BuildLoaded++;
            }
            BuildIndex++;
            Done++;
        }
        if (BuildIndex < Columns) {
            return false;
        }
        ByteBuffer Previous = Live;
        Live = Building;
        Building = null;
        if (Previous != null) {
            Heap.free(Previous);
        }
        LiveDimension = BuildDimension;
        OriginX = BuildX;
        OriginY = BuildY;
        OriginZ = BuildZ;
        LiveLoaded = BuildLoaded;
        rebuildCoarse();
        Ready = true;
        return true;
    }

    private boolean fillColumn(Cells Source, int Lx, int Lz) {
        Arrays.fill(Column, Scp001DayRay.Clear);
        boolean Loaded = Source.column(BuildX + Lx, BuildZ + Lz, BuildY, Column);
        for (int Ly = 0; Ly < Span; Ly++) {
            int Index = index(Lx, Ly, Lz);
            Building.put(Index, (byte) Scp001DayRay.mask(Column[Ly]));
            Building.put(Index + 1, (byte) Scp001DayRay.filter(Column[Ly]));
        }
        return Loaded;
    }

    private boolean drainDirty(Cells Source) {
        long[] Batch;
        int Count;
        synchronized (Dirty) {
            Count = Math.min(Dirty.size(), DirtyBudget);
            Batch = new long[Count];
            for (int Index = 0; Index < Count; Index++) {
                Batch[Index] = Dirty.dequeueLong();
                DirtySet.remove(Batch[Index]);
            }
        }
        if (Live == null) {
            return false;
        }
        boolean Changed = false;
        for (int Index = 0; Index < Count; Index++) {
            int X = BlockPos.getX(Batch[Index]);
            int Y = BlockPos.getY(Batch[Index]);
            int Z = BlockPos.getZ(Batch[Index]);
            int Lx = X - OriginX;
            int Ly = Y - OriginY;
            int Lz = Z - OriginZ;
            if (!inside(Lx, Ly, Lz)) {
                continue;
            }
            int Cell = Source.cell(X, Y, Z);
            int Offset = index(Lx, Ly, Lz);
            boolean WasEmpty = empty(Live, Offset);
            Live.put(Offset, (byte) Scp001DayRay.mask(Cell));
            Live.put(Offset + 1, (byte) Scp001DayRay.filter(Cell));
            boolean NowEmpty = empty(Live, Offset);
            if (WasEmpty != NowEmpty) {
                int CoarseIndex = coarseIndex(Lx, Ly, Lz);
                CoarseCounts[CoarseIndex] += NowEmpty ? -1 : 1;
                CoarseData.put(CoarseIndex, (byte) (CoarseCounts[CoarseIndex] > 0 ? 1 : 0));
            }
            Changed = true;
        }
        return Changed;
    }

    public static boolean empty(ByteBuffer Grid, int Offset) {
        return Grid.get(Offset) == 0 && (Grid.get(Offset + 1) & 255) == 255;
    }

    private void rebuildCoarse() {
        Arrays.fill(CoarseCounts, 0);
        for (int Lz = 0; Lz < Span; Lz++) {
            for (int Ly = 0; Ly < Span; Ly++) {
                for (int Lx = 0; Lx < Span; Lx++) {
                    if (!empty(Live, index(Lx, Ly, Lz))) {
                        CoarseCounts[coarseIndex(Lx, Ly, Lz)]++;
                    }
                }
            }
        }
        for (int Index = 0; Index < CoarseCounts.length; Index++) {
            CoarseData.put(Index, (byte) (CoarseCounts[Index] > 0 ? 1 : 0));
        }
    }

    public void release() {
        Ready = false;
        Overflow = false;
        if (Live != null) {
            Heap.free(Live);
            Live = null;
        }
        if (Building != null) {
            Heap.free(Building);
            Building = null;
        }
        if (CoarseData != null) {
            Heap.free(CoarseData);
            CoarseData = null;
        }
        LiveDimension = null;
        synchronized (Dirty) {
            Dirty.clear();
            DirtySet.clear();
        }
    }
}
