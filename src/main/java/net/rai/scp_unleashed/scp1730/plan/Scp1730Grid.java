package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class Scp1730Grid {
    public static final int MaxY = 255;
    private final List<String> Palette = new ArrayList<>();
    private final Map<String, Integer> Index = new HashMap<>();
    private final Map<Long, short[]> Sections = new HashMap<>();
    private final Map<Long, TreeSet<Integer>> Stacks = new HashMap<>();
    private final Map<Long, int[]> Frozen = new HashMap<>();

    public Scp1730Grid() {
        Palette.add(null);
    }

    private static long sectionKey(int Sx, int Sy, int Sz) {
        return ((long) Sx & 0xFFFFFL) << 40 | ((long) Sy & 0xFFFFFL) << 20 | ((long) Sz & 0xFFFFFL);
    }

    private static long columnKey(int Sx, int Sz) {
        return ((long) Sx & 0xFFFFFFL) << 24 | ((long) Sz & 0xFFFFFFL);
    }

    public boolean inside(int X, int Y, int Z) {
        return X >= 0 && Z >= 0 && X < Scp1730Plan.Width && Z < Scp1730Plan.Depth && Y >= 0 && Y <= MaxY;
    }

    public int code(String State) {
        if (State == null) {
            return 0;
        }
        Integer Found = Index.get(State);
        if (Found == null) {
            Found = Palette.size();
            Palette.add(State);
            Index.put(State, Found);
        }
        return Found;
    }

    public void set(int X, int Y, int Z, String State) {
        if (!inside(X, Y, Z)) {
            return;
        }
        int Code = code(State);
        long Key = sectionKey(X >> 4, Y >> 4, Z >> 4);
        short[] Section = Sections.get(Key);
        if (Section == null) {
            if (Code == 0) {
                return;
            }
            Section = new short[4096];
            Sections.put(Key, Section);
            Stacks.computeIfAbsent(columnKey(X >> 4, Z >> 4), Unused -> new TreeSet<>()).add(Y >> 4);
        }
        Section[(Y & 15) << 8 | (Z & 15) << 4 | (X & 15)] = (short) Code;
    }

    public void fill(int X0, int Y0, int Z0, int X1, int Y1, int Z1, String State) {
        for (int Y = Math.min(Y0, Y1); Y <= Math.max(Y0, Y1); Y++) {
            for (int Z = Math.min(Z0, Z1); Z <= Math.max(Z0, Z1); Z++) {
                for (int X = Math.min(X0, X1); X <= Math.max(X0, X1); X++) {
                    set(X, Y, Z, State);
                }
            }
        }
    }

    public String get(int X, int Y, int Z) {
        if (!inside(X, Y, Z)) {
            return null;
        }
        short[] Section = Sections.get(sectionKey(X >> 4, Y >> 4, Z >> 4));
        if (Section == null) {
            return null;
        }
        return Palette.get(Section[(Y & 15) << 8 | (Z & 15) << 4 | (X & 15)]);
    }

    public boolean empty(int X, int Y, int Z) {
        return get(X, Y, Z) == null;
    }

    public void freeze() {
        Frozen.clear();
        for (Map.Entry<Long, TreeSet<Integer>> Entry : Stacks.entrySet()) {
            int[] Levels = new int[Entry.getValue().size()];
            int Cursor = 0;
            for (int Level : Entry.getValue()) {
                Levels[Cursor++] = Level;
            }
            Frozen.put(Entry.getKey(), Levels);
        }
    }

    public void paint(int MinX, int MinZ, int MaxX, int MaxZ, Scp1730Canvas Canvas) {
        int X0 = Math.max(0, MinX);
        int Z0 = Math.max(0, MinZ);
        int X1 = Math.min(Scp1730Plan.Width - 1, MaxX);
        int Z1 = Math.min(Scp1730Plan.Depth - 1, MaxZ);
        if (X0 > X1 || Z0 > Z1) {
            return;
        }
        for (int Sx = X0 >> 4; Sx <= X1 >> 4; Sx++) {
            for (int Sz = Z0 >> 4; Sz <= Z1 >> 4; Sz++) {
                int[] Levels = Frozen.get(columnKey(Sx, Sz));
                if (Levels == null) {
                    continue;
                }
                int Ax = Math.max(X0, Sx << 4);
                int Bx = Math.min(X1, (Sx << 4) + 15);
                int Az = Math.max(Z0, Sz << 4);
                int Bz = Math.min(Z1, (Sz << 4) + 15);
                for (int Sy : Levels) {
                    short[] Section = Sections.get(sectionKey(Sx, Sy, Sz));
                    if (Section == null) {
                        continue;
                    }
                    int BaseY = Sy << 4;
                    for (int Ly = 0; Ly < 16; Ly++) {
                        int Row = Ly << 8;
                        for (int Z = Az; Z <= Bz; Z++) {
                            int Line = Row | (Z & 15) << 4;
                            for (int X = Ax; X <= Bx; X++) {
                                int Code = Section[Line | (X & 15)];
                                if (Code != 0) {
                                    Canvas.put(X, BaseY + Ly, Z, Palette.get(Code));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int sectionCount() {
        return Sections.size();
    }

    public List<String> palette() {
        return Palette.subList(1, Palette.size());
    }

    public long checksum() {
        long Hash = 1125899906842597L;
        List<Long> Keys = new ArrayList<>(Sections.keySet());
        Keys.sort(Long::compare);
        for (long Key : Keys) {
            short[] Section = Sections.get(Key);
            Hash = Hash * 31 + Key;
            for (short Code : Section) {
                String State = Palette.get(Code);
                Hash = Hash * 31 + (State == null ? 0 : State.hashCode());
            }
        }
        return Hash ^ Arrays.hashCode(new int[]{Sections.size()});
    }
}
