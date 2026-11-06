package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuinPlan {
    public static final String Guardian = "guardian";
    public static final String Centre = "centre";
    public static final String Entrance = "entrance";
    public static final String TrapTrigger = "trap_trigger";
    public static final String TrapFall = "trap_fall";
    private final String Name;
    private final int Page;
    private final int Tier;
    private final LinkedHashMap<Long, Integer> Cells = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<int[]>> Marks = new LinkedHashMap<>();
    private final Set<Long> Kept = new HashSet<>();
    private final Set<Long> Clear = new HashSet<>();
    private int MinX = Integer.MAX_VALUE;
    private int MinY = Integer.MAX_VALUE;
    private int MinZ = Integer.MAX_VALUE;
    private int MaxX = Integer.MIN_VALUE;
    private int MaxY = Integer.MIN_VALUE;
    private int MaxZ = Integer.MIN_VALUE;

    public RuinPlan(String Name, int Page, int Tier) {
        this.Name = Name;
        this.Page = Page;
        this.Tier = Tier;
    }

    @FunctionalInterface
    public interface Visitor {
        void visit(int X, int Y, int Z, RuinMat Mat, int Facing, int Variant);
    }

    public String name() {
        return Name;
    }

    public int page() {
        return Page;
    }

    public int tier() {
        return Tier;
    }

    public static long key(int X, int Y, int Z) {
        return ((long) (X + 0x100000) << 42) | ((long) (Y + 0x100000) << 21) | (long) (Z + 0x100000);
    }

    public static int keyX(long Key) {
        return (int) ((Key >> 42) & 0x1FFFFF) - 0x100000;
    }

    public static int keyY(long Key) {
        return (int) ((Key >> 21) & 0x1FFFFF) - 0x100000;
    }

    public static int keyZ(long Key) {
        return (int) (Key & 0x1FFFFF) - 0x100000;
    }

    private void include(int X, int Y, int Z) {
        MinX = Math.min(MinX, X);
        MinY = Math.min(MinY, Y);
        MinZ = Math.min(MinZ, Z);
        MaxX = Math.max(MaxX, X);
        MaxY = Math.max(MaxY, Y);
        MaxZ = Math.max(MaxZ, Z);
    }

    public void set(int X, int Y, int Z, RuinMat Mat) {
        set(X, Y, Z, Mat, RuinMat.South, 0);
    }

    public void set(int X, int Y, int Z, RuinMat Mat, int Facing) {
        set(X, Y, Z, Mat, Facing, 0);
    }

    public void set(int X, int Y, int Z, RuinMat Mat, int Facing, int Variant) {
        long Key = key(X, Y, Z);
        if (Kept.contains(Key) && Cells.containsKey(Key)) {
            return;
        }
        Cells.put(Key, Mat.ordinal() | (Facing & 7) << 8 | (Variant & 255) << 12);
        include(X, Y, Z);
    }

    public void force(int X, int Y, int Z, RuinMat Mat, int Facing, int Variant) {
        Cells.put(key(X, Y, Z), Mat.ordinal() | (Facing & 7) << 8 | (Variant & 255) << 12);
        include(X, Y, Z);
    }

    public void remove(int X, int Y, int Z) {
        long Key = key(X, Y, Z);
        if (!Kept.contains(Key)) {
            Cells.remove(Key);
        }
    }

    public void clearCell(int X, int Y, int Z) {
        long Key = key(X, Y, Z);
        if (!Kept.contains(Key)) {
            Cells.put(Key, RuinMat.Air.ordinal());
            include(X, Y, Z);
        }
    }

    public boolean has(int X, int Y, int Z) {
        return Cells.containsKey(key(X, Y, Z));
    }

    public RuinMat matAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code == null ? RuinMat.Air : RuinMat.byOrdinal(Code & 255);
    }

    public boolean solidAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code != null && RuinMat.byOrdinal(Code & 255).carriesLoad();
    }

    public boolean filled(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code != null && !RuinMat.byOrdinal(Code & 255).isAir();
    }

    public int facingAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code == null ? RuinMat.South : (Code >> 8) & 7;
    }

    public int variantAt(int X, int Y, int Z) {
        Integer Code = Cells.get(key(X, Y, Z));
        return Code == null ? 0 : (Code >> 12) & 255;
    }

    public int codeAt(long Key) {
        Integer Code = Cells.get(Key);
        return Code == null ? -1 : Code;
    }

    public void keep(int X, int Y, int Z) {
        Kept.add(key(X, Y, Z));
    }

    public boolean kept(int X, int Y, int Z) {
        return Kept.contains(key(X, Y, Z));
    }

    public void keepClear(int X, int Y, int Z) {
        Clear.add(key(X, Y, Z));
    }

    public boolean mustStayClear(int X, int Y, int Z) {
        return Clear.contains(key(X, Y, Z));
    }

    public void mark(String Label, int X, int Y, int Z, int Facing, int Extra) {
        Marks.computeIfAbsent(Label, Ignored -> new ArrayList<>()).add(new int[]{X, Y, Z, Facing, Extra});
    }

    public List<int[]> marks(String Label) {
        return Marks.getOrDefault(Label, List.of());
    }

    public Map<String, List<int[]>> allMarks() {
        return Marks;
    }

    public void dropMarks(String Label) {
        Marks.remove(Label);
    }

    public int size() {
        return Cells.size();
    }

    public List<Long> keys() {
        return new ArrayList<>(Cells.keySet());
    }

    public int[] low() {
        return new int[]{MinX, MinY, MinZ};
    }

    public int[] high() {
        return new int[]{MaxX, MaxY, MaxZ};
    }

    public void recomputeBounds() {
        MinX = Integer.MAX_VALUE;
        MinY = Integer.MAX_VALUE;
        MinZ = Integer.MAX_VALUE;
        MaxX = Integer.MIN_VALUE;
        MaxY = Integer.MIN_VALUE;
        MaxZ = Integer.MIN_VALUE;
        for (long Key : Cells.keySet()) {
            include(keyX(Key), keyY(Key), keyZ(Key));
        }
    }

    public void forEach(Visitor Visit) {
        for (Map.Entry<Long, Integer> Entry : Cells.entrySet()) {
            long Key = Entry.getKey();
            int Code = Entry.getValue();
            Visit.visit(keyX(Key), keyY(Key), keyZ(Key), RuinMat.byOrdinal(Code & 255), (Code >> 8) & 7, (Code >> 12) & 255);
        }
    }

    public int topAt(int X, int Z) {
        for (int Y = MaxY; Y >= MinY; Y--) {
            if (filled(X, Y, Z)) {
                return Y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
