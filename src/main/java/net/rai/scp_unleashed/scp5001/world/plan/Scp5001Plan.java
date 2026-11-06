package net.rai.scp_unleashed.scp5001.world.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Scp5001Plan {
    public static final int Width = 151;
    public static final int Height = 56;
    public static final int Depth = 111;
    public static final int CentreX = 75;
    public static final int CentreZ = 55;
    public static final int Version = 2;
    public static final int Levels = 3;
    public static final int LevelPitch = 16;
    public static final int RoomHeight = 6;
    public static final int CorridorHeight = 5;
    public static final int ShellThickness = 2;
    public static final int EntryLevel = 2;
    public static final String Outside = "#outside";
    public static final int EntryWidth = 3;
    public static final int EntryHeight = 4;
    public static final long Salt = 0x5001_5EEDL;
    public static final String KindRoom = "room";
    public static final String KindHall = "hall";
    public static final String KindStair = "stair";
    public static final String KindCorridor = "corridor";
    public static final String KindGallery = "gallery";

    private final long Seed;
    private final short[] Cells = new short[Width * Height * Depth];
    private final List<String> Palette = new ArrayList<>();
    private final Map<String, Short> Lookup = new HashMap<>();
    final int[][] Region = new int[Levels][Width * Depth];
    final List<Scp5001Room> Rooms = new ArrayList<>();
    final List<Scp5001Anchor> Anchors = new ArrayList<>();
    final List<Scp5001Slot> Slots = new ArrayList<>();
    final Random Random;
    private long Checksum;

    private Scp5001Plan(long Seed) {
        this.Seed = Seed;
        this.Random = new Random(Seed ^ Salt);
        index(Scp5001Blocks.Air);
    }

    public static Scp5001Plan build(long Seed) {
        Scp5001Plan Plan = new Scp5001Plan(Seed);
        Plan.fillShell();
        Scp5001Layout Layout = new Scp5001Layout(Plan);
        Layout.lay();
        Scp5001Builder Builder = new Scp5001Builder(Plan, Layout);
        Builder.build();
        new Scp5001Dressing(Plan, Layout).dress();
        Plan.finish();
        return Plan;
    }

    public static int floorY(int Level) {
        return ShellThickness + Level * LevelPitch;
    }

    public static int entryX() {
        return CentreX - 1;
    }

    public static int entryY() {
        return floorY(EntryLevel) + 1;
    }

    public long seed() {
        return Seed;
    }

    public long checksum() {
        return Checksum;
    }

    public List<Scp5001Room> rooms() {
        return Collections.unmodifiableList(Rooms);
    }

    public List<Scp5001Anchor> anchors() {
        return Collections.unmodifiableList(Anchors);
    }

    public List<Scp5001Slot> slots() {
        return Collections.unmodifiableList(Slots);
    }

    public int roomCount() {
        int Count = 0;
        for (Scp5001Room Each : Rooms) {
            if (!Each.corridor() && !Each.Type.equals("void")) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean inside(int X, int Y, int Z) {
        return X >= 0 && X < Width && Y >= 0 && Y < Height && Z >= 0 && Z < Depth;
    }

    public static boolean hullCell(int X, int Y, int Z) {
        return inside(X, Y, Z) && Scp5001Hull.hull(X - CentreX, Y, Z - CentreZ);
    }

    public static boolean interiorCell(int X, int Y, int Z) {
        return inside(X, Y, Z) && Scp5001Hull.interior(X - CentreX, Y, Z - CentreZ);
    }

    public static boolean shellCell(int X, int Y, int Z) {
        return hullCell(X, Y, Z) && !interiorCell(X, Y, Z);
    }

    public static int band(int X, int Z) {
        return Scp5001Hull.band(X - CentreX, Z - CentreZ);
    }

    private static int index(int X, int Y, int Z) {
        return (Y * Depth + Z) * Width + X;
    }

    private short index(String State) {
        Short Found = Lookup.get(State);
        if (Found != null) {
            return Found;
        }
        short Made = (short) Palette.size();
        Palette.add(State);
        Lookup.put(State, Made);
        return Made;
    }

    void set(int X, int Y, int Z, String State) {
        if (inside(X, Y, Z) && (State.equals(Outside) || Cells[index(X, Y, Z)] != outsideIndex())) {
            Cells[index(X, Y, Z)] = index(State);
        }
    }

    private short outsideIndex() {
        Short Found = Lookup.get(Outside);
        return Found == null ? -1 : Found;
    }

    public String get(int X, int Y, int Z) {
        if (!inside(X, Y, Z)) {
            return Outside;
        }
        return Palette.get(Cells[index(X, Y, Z)]);
    }

    public boolean air(int X, int Y, int Z) {
        return inside(X, Y, Z) && Cells[index(X, Y, Z)] == 0;
    }

    public int region(int Level, int X, int Z) {
        if (Level < 0 || Level >= Levels || X < 0 || X >= Width || Z < 0 || Z >= Depth) {
            return 0;
        }
        return Region[Level][X * Depth + Z];
    }

    public Scp5001Room room(int Id) {
        return Id > 0 && Id <= Rooms.size() ? Rooms.get(Id - 1) : null;
    }

    private void fillShell() {
        String Fill = Scp5001Blocks.Graphite;
        for (int Y = 0; Y < Height; Y++) {
            for (int Z = 0; Z < Depth; Z++) {
                for (int X = 0; X < Width; X++) {
                    if (!hullCell(X, Y, Z)) {
                        set(X, Y, Z, Outside);
                    } else if (!interiorCell(X, Y, Z)) {
                        boolean Seam = Y % 4 == 2 && Y > ShellThickness && Y < Height - 12;
                        set(X, Y, Z, Seam ? Scp5001Blocks.ShellSeam : Scp5001Blocks.Shell);
                    } else {
                        set(X, Y, Z, Fill);
                    }
                }
            }
        }
    }

    public void paint(int MinX, int MinZ, int MaxX, int MaxZ, int Phase, Scp5001Canvas Canvas) {
        paint(MinX, MinZ, MaxX, MaxZ, 0, Height - 1, Phase, Canvas);
    }

    public void paint(int MinX, int MinZ, int MaxX, int MaxZ, int MinY, int MaxY, int Phase, Scp5001Canvas Canvas) {
        int X0 = Math.max(0, MinX);
        int Z0 = Math.max(0, MinZ);
        int X1 = Math.min(Width - 1, MaxX);
        int Z1 = Math.min(Depth - 1, MaxZ);
        for (int Y = Math.max(0, MinY); Y <= Math.min(Height - 1, MaxY); Y++) {
            for (int Z = Z0; Z <= Z1; Z++) {
                for (int X = X0; X <= X1; X++) {
                    String State = Palette.get(Cells[index(X, Y, Z)]);
                    if (State.equals(Outside)) {
                        continue;
                    }
                    boolean OnShell = State.startsWith(Scp5001Blocks.Mod + "scp5001_shell");
                    if (Phase == 0 && OnShell || Phase == 1 && !OnShell) {
                        Canvas.put(X, Y, Z, State);
                    }
                }
            }
        }
    }

    public List<Scp5001Slot> slotsIn(int MinX, int MinZ, int MaxX, int MaxZ) {
        List<Scp5001Slot> Found = new ArrayList<>();
        for (Scp5001Slot Each : Slots) {
            if (Each.X() >= MinX && Each.X() <= MaxX && Each.Z() >= MinZ && Each.Z() <= MaxZ) {
                Found.add(Each);
            }
        }
        return Found;
    }

    public List<Scp5001Anchor> anchors(String Id) {
        List<Scp5001Anchor> Found = new ArrayList<>();
        for (Scp5001Anchor Each : Anchors) {
            if (Each.Type().equals(Id)) {
                Found.add(Each);
            }
        }
        return Found;
    }

    public int paletteSize() {
        return Palette.size();
    }

    private void finish() {
        long Hash = 1125899906842597L;
        for (short Value : Cells) {
            Hash = 31 * Hash + Value;
        }
        for (String State : Palette) {
            Hash = 31 * Hash + State.hashCode();
        }
        for (Scp5001Slot Each : Slots) {
            Hash = 31 * Hash + Each.hashCode();
        }
        Checksum = Hash;
    }
}
