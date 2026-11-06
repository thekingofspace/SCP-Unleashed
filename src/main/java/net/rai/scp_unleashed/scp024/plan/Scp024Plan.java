package net.rai.scp_unleashed.scp024.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Scp024Plan {
    public static final int Down = 0;
    public static final int Up = 1;
    public static final int North = 2;
    public static final int South = 3;
    public static final int West = 4;
    public static final int East = 5;
    private static final Scp024Mat[] Mats = Scp024Mat.values();
    private final String Name;
    private final LinkedHashMap<Long, Integer> Cells = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<int[]>> Marks = new LinkedHashMap<>();
    private int MinX = Integer.MAX_VALUE;
    private int MinY = Integer.MAX_VALUE;
    private int MinZ = Integer.MAX_VALUE;
    private int MaxX = Integer.MIN_VALUE;
    private int MaxY = Integer.MIN_VALUE;
    private int MaxZ = Integer.MIN_VALUE;

    public Scp024Plan(String Name) {
        this.Name = Name;
    }

    @FunctionalInterface
    public interface Visitor {
        void visit(int X, int Y, int Z, Scp024Mat Mat, int Facing, int Variant);
    }

    public String name() {
        return Name;
    }

    private static long key(int X, int Y, int Z) {
        return ((long) (X + 0x100000) << 42) | ((long) (Y + 0x100000) << 21) | (long) (Z + 0x100000);
    }

    private static int unpack(long Key, int Shift) {
        return (int) ((Key >> Shift) & 0x1FFFFF) - 0x100000;
    }

    public void include(int X, int Y, int Z) {
        MinX = Math.min(MinX, X);
        MinY = Math.min(MinY, Y);
        MinZ = Math.min(MinZ, Z);
        MaxX = Math.max(MaxX, X);
        MaxY = Math.max(MaxY, Y);
        MaxZ = Math.max(MaxZ, Z);
    }

    public void set(int X, int Y, int Z, Scp024Mat Mat) {
        set(X, Y, Z, Mat, South, 0);
    }

    public void set(int X, int Y, int Z, Scp024Mat Mat, int Facing) {
        set(X, Y, Z, Mat, Facing, 0);
    }

    public void set(int X, int Y, int Z, Scp024Mat Mat, int Facing, int Variant) {
        Cells.put(key(X, Y, Z), Mat.ordinal() | (Facing & 7) << 8 | (Variant & 255) << 12);
        include(X, Y, Z);
    }

    public void fill(int X1, int Y1, int Z1, int X2, int Y2, int Z2, Scp024Mat Mat) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    set(X, Y, Z, Mat);
                }
            }
        }
    }

    public void fillIfEmpty(int X1, int Y1, int Z1, int X2, int Y2, int Z2, Scp024Mat Mat) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    if (matAt(X, Y, Z).isAir()) {
                        set(X, Y, Z, Mat);
                    }
                }
            }
        }
    }

    public void remove(int X, int Y, int Z) {
        Cells.remove(key(X, Y, Z));
    }

    public boolean has(int X, int Y, int Z) {
        return Cells.containsKey(key(X, Y, Z));
    }

    public Scp024Mat matAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code == null ? Scp024Mat.Air : Mats[Code & 255];
    }

    public int facingAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code == null ? South : (Code >> 8) & 7;
    }

    public int variantAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code == null ? 0 : (Code >> 12) & 255;
    }

    public void mark(String Label, int X, int Y, int Z) {
        mark(Label, X, Y, Z, South);
    }

    public void mark(String Label, int X, int Y, int Z, int Facing) {
        Marks.computeIfAbsent(Label, Ignored -> new ArrayList<>()).add(new int[]{X, Y, Z, Facing});
        include(X, Y, Z);
    }

    public List<int[]> marks(String Label) {
        return Marks.getOrDefault(Label, List.of());
    }

    public int[] mark(String Label) {
        List<int[]> Found = marks(Label);
        if (Found.isEmpty()) {
            throw new IllegalStateException("Plan " + Name + " has no mark " + Label);
        }
        return Found.get(0);
    }

    public Map<String, List<int[]>> allMarks() {
        return Marks;
    }

    public int size() {
        return Cells.size();
    }

    public void forEach(Visitor Target) {
        for (Map.Entry<Long, Integer> Entry : Cells.entrySet()) {
            long Key = Entry.getKey();
            int Code = Entry.getValue();
            Target.visit(unpack(Key, 42), unpack(Key, 21), unpack(Key, 0), Mats[Code & 255], (Code >> 8) & 7, (Code >> 12) & 255);
        }
    }

    public int[] low() {
        return new int[]{MinX, MinY, MinZ};
    }

    public int[] high() {
        return new int[]{MaxX, MaxY, MaxZ};
    }

    public void stage(int Width, int Height, int Depth, boolean Floor, int Pit, boolean Lit) {
        if (Floor) {
            fill(0, 0, 0, Width - 1, 0, Depth - 1, Scp024Mat.StageFloor);
        }
        int Bottom = Floor ? 0 : -Pit;
        for (int Y = Bottom; Y <= Height; Y++) {
            Scp024Mat Band = Lit && (Y == Height || Y == 3 && Floor) ? Scp024Mat.Marquee : Scp024Mat.StageWall;
            for (int X = -1; X <= Width; X++) {
                set(X, Y, -1, Band);
                set(X, Y, Depth, Band);
            }
            for (int Z = 0; Z < Depth; Z++) {
                set(-1, Y, Z, Band);
                set(Width, Y, Z, Band);
            }
        }
        fill(-1, Height + 1, -1, Width, Height + 1, Depth, Scp024Mat.Barrier);
        if (Lit) {
            for (int X = 1; X < Width - 1; X += 4) {
                for (int Z = 1; Z < Depth - 1; Z += 4) {
                    if (matAt(X, Height, Z).isAir()) {
                        set(X, Height, Z, Scp024Mat.Light);
                    }
                }
            }
        }
        include(-1, Bottom, -1);
        include(Width, Height + 1, Depth);
    }

    public void rigs(int Width, int Height, int Depth) {
        mark("rig", 1, Height, 1);
        mark("rig", Width - 2, Height, 1);
        mark("rig", 1, Height, Depth - 2);
        mark("rig", Width - 2, Height, Depth - 2);
    }
}
