package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class Scp1730Layout {
    public static final int Slots = 8;
    public static final int SlotSize = 16;
    public static final int Origin = 16;
    public static final int[] Dx = {0, 1, 0, -1};
    public static final int[] Dz = {-1, 0, 1, 0};
    public static final String[] Facings = {"north", "east", "south", "west"};

    public static final class Door {
        public final int Sx;
        public final int Sz;
        public final int Dir;

        Door(int Sx, int Sz, int Dir) {
            this.Sx = Sx;
            this.Sz = Sz;
            this.Dir = Dir;
        }

        public int centreX() {
            int X0 = Origin + Sx * SlotSize;
            return Dir == 1 ? X0 + 15 : Dir == 3 ? X0 : X0 + 7;
        }

        public int centreZ() {
            int Z0 = Origin + Sz * SlotSize;
            return Dir == 2 ? Z0 + 15 : Dir == 0 ? Z0 : Z0 + 7;
        }
    }

    public static final class Room {
        public final String Type;
        public final int Sx;
        public final int Sz;
        public final int W;
        public final int D;
        public final List<Door> Doors = new ArrayList<>();
        public int Floor;
        public int Height;
        public int Id;
        public int Bx0;
        public int Bz0;
        public int Bx1;
        public int Bz1;

        Room(String Type, int Sx, int Sz, int W, int D) {
            this.Type = Type;
            this.Sx = Sx;
            this.Sz = Sz;
            this.W = W;
            this.D = D;
            this.Bx0 = x0();
            this.Bz0 = z0();
            this.Bx1 = x1();
            this.Bz1 = z1();
        }

        public void inset(int West, int North, int East, int South) {
            Bx0 = x0() + West;
            Bz0 = z0() + North;
            Bx1 = x1() - East;
            Bz1 = z1() - South;
        }

        public int doorX(Door Each) {
            return Each.Dir == 1 ? Bx1 : Each.Dir == 3 ? Bx0 : Each.centreX();
        }

        public int doorZ(Door Each) {
            return Each.Dir == 2 ? Bz1 : Each.Dir == 0 ? Bz0 : Each.centreZ();
        }

        public int x0() {
            return Origin + Sx * SlotSize;
        }

        public int z0() {
            return Origin + Sz * SlotSize;
        }

        public int x1() {
            return Origin + (Sx + W) * SlotSize - 1;
        }

        public int z1() {
            return Origin + (Sz + D) * SlotSize - 1;
        }

        public boolean covers(int Px, int Pz) {
            return Px >= Sx && Px < Sx + W && Pz >= Sz && Pz < Sz + D;
        }
    }

    public record Spec(String Type, int W, int D, int MinDoors, int MaxDoors) {
    }

    public final int ZoneIndex;
    public final boolean[][] Active = new boolean[Slots][Slots];
    public final int[][] Owner = new int[Slots][Slots];
    public final List<Room> Rooms = new ArrayList<>();
    public final boolean[][][] Links = new boolean[Slots][Slots][4];
    public final boolean[][] Corridor = new boolean[Slots][Slots];
    public Room Entry;
    public Room Exit;

    private Scp1730Layout(int ZoneIndex) {
        this.ZoneIndex = ZoneIndex;
    }

    public static Scp1730Layout build(int ZoneIndex, Random Rng, int[] EntryRect, List<Spec> Required, List<Spec> Fillers, Spec EntrySpec, Spec ExitSpec, String Bridgeless) {
        for (int Attempt = 0; Attempt < 400; Attempt++) {
            Scp1730Layout Made = new Scp1730Layout(ZoneIndex);
            if (Made.attempt(Rng, EntryRect, Required, Fillers, EntrySpec, ExitSpec, Bridgeless)) {
                return Made;
            }
        }
        return null;
    }

    public static Scp1730Layout fixed(int ZoneIndex, Random Rng, Spec EntrySpec, Spec ExitSpec) {
        int[][] Stairs = {{7, 0, 1, 2}, {0, 6, 1, 2}, {7, 6, 1, 2}};
        Object[][] Table = switch (ZoneIndex) {
            case 0 -> new Object[][]{{"cafeteria", 2, 0, 2, 2}, {"office", 4, 0, 1, 2}, {"elevator", 5, 0, 1, 2}, {"security", 6, 0, 1, 1}, {"checkpoint", 0, 3, 1, 2},
                    {"breaker", 1, 3, 1, 1}, {"holding_cell", 1, 4, 1, 1}, {"holding_cell", 2, 3, 1, 1}, {"storage", 4, 6, 1, 1}};
            case 1 -> new Object[][]{{"cell_block", 0, 0, 2, 2}, {"test_chamber", 2, 0, 2, 2}, {"medical", 4, 0, 1, 2}, {"dorms", 5, 0, 2, 1}, {"breaker", 0, 3, 1, 1},
                    {"holding_cell", 0, 4, 1, 1}, {"holding_cell", 1, 3, 1, 1}, {"research", 4, 6, 1, 1}, {Scp1730Sectors.BioWing, 5, 3, 2, 2}};
            case 2 -> new Object[][]{{"heavy_cell", 0, 0, 2, 2}, {"server", 2, 0, 2, 2}, {"generator", 4, 0, 2, 2}, {"pit", 6, 0, 2, 2}, {"tesla", 0, 3, 1, 2},
                    {"armoury", 1, 3, 1, 2}, {"holding_cell", 2, 3, 1, 1}, {"holding_cell", 2, 4, 1, 1}, {Scp1730Sectors.Reservoir, 4, 3, 2, 2},
                    {Scp1730Sectors.BioWing, 3, 5, 2, 2}};
            default -> new Object[][]{{"sanctum", 0, 0, 3, 4}, {"holding_cell", 4, 0, 1, 1}, {Scp1730Sectors.BioWing, 4, 2, 2, 2}};
        };
        for (int Attempt = 0; Attempt < 64; Attempt++) {
            Scp1730Layout Made = new Scp1730Layout(ZoneIndex);
            for (int[] Row : Made.Owner) {
                Arrays.fill(Row, -1);
            }
            for (int X = 0; X < Slots; X++) {
                Arrays.fill(Made.Active[X], true);
            }
            if (ZoneIndex == 0) {
                Made.Entry = new Room(EntrySpec.Type(), 0, 0, 2, 2);
            } else {
                int[] Rect = Stairs[ZoneIndex - 1];
                Made.Entry = new Room(EntrySpec.Type(), Rect[0], Rect[1], Rect[2], Rect[3]);
            }
            Made.claim(Made.Entry);
            if (ExitSpec != null) {
                int[] Rect = Stairs[ZoneIndex];
                Made.Exit = new Room(ExitSpec.Type(), Rect[0], Rect[1], Rect[2], Rect[3]);
                Made.claim(Made.Exit);
            }
            for (Object[] Row : Table) {
                Made.claim(new Room((String) Row[0], (Integer) Row[1], (Integer) Row[2], (Integer) Row[3], (Integer) Row[4]));
            }
            for (int X = 0; X < Slots; X++) {
                for (int Z = 0; Z < Slots; Z++) {
                    Made.Corridor[X][Z] = Made.Owner[X][Z] < 0;
                }
            }
            Made.spanCorridors(Rng);
            boolean Doors = true;
            for (Room Each : Made.Rooms) {
                Doors &= Made.chooseDoors(Rng, Each);
            }
            if (!Doors) {
                continue;
            }
            Made.prune();
            Made.addLoops(Rng);
            if (!Made.connected(null)) {
                continue;
            }
            boolean Bridged = false;
            for (Room Each : Made.Rooms) {
                if (Each.Type.equals("pit") && !Made.connected(Each)) {
                    Bridged = true;
                }
            }
            if (!Bridged) {
                return Made;
            }
        }
        throw new IllegalStateException("scp1730 fixed layout failed for zone " + ZoneIndex);
    }

    private boolean attempt(Random Rng, int[] EntryRect, List<Spec> Required, List<Spec> Fillers, Spec EntrySpec, Spec ExitSpec, String Bridgeless) {
        for (int[] Row : Owner) {
            Arrays.fill(Row, -1);
        }
        for (int X = 0; X < Slots; X++) {
            Arrays.fill(Active[X], true);
        }
        int Bites = Rng.nextInt(5);
        for (int Index = 0; Index < Bites; Index++) {
            int Corner = Rng.nextInt(4);
            int Bx = (Corner & 1) == 0 ? 0 : Slots - 1;
            int Bz = (Corner & 2) == 0 ? 0 : Slots - 1;
            int Span = 1 + Rng.nextInt(2);
            for (int Step = 0; Step < Span; Step++) {
                int Px = Rng.nextBoolean() ? Bx : Bx + (Bx == 0 ? Step : -Step);
                int Pz = Rng.nextBoolean() ? Bz : Bz + (Bz == 0 ? Step : -Step);
                Active[Px][Pz] = false;
            }
        }
        if (EntryRect != null) {
            Entry = new Room(EntrySpec.Type(), EntryRect[0], EntryRect[1], EntryRect[2], EntryRect[3]);
            for (int X = EntryRect[0]; X < EntryRect[0] + EntryRect[2]; X++) {
                for (int Z = EntryRect[1]; Z < EntryRect[1] + EntryRect[3]; Z++) {
                    Active[X][Z] = true;
                }
            }
            claim(Entry);
        } else {
            Entry = place(Rng, EntrySpec, null, 300);
            if (Entry == null) {
                return false;
            }
        }
        List<Spec> Order = new ArrayList<>(Required);
        Order.sort((A, B) -> Integer.compare(B.W() * B.D(), A.W() * A.D()));
        if (ExitSpec != null) {
            Exit = placeFar(Rng, ExitSpec);
            if (Exit == null) {
                return false;
            }
        }
        for (Spec Each : Order) {
            if (place(Rng, Each, null, 300) == null) {
                return false;
            }
        }
        List<Spec> Extra = new ArrayList<>(Fillers);
        for (int Index = Extra.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            Spec Held = Extra.get(Index);
            Extra.set(Index, Extra.get(Swap));
            Extra.set(Swap, Held);
        }
        for (Spec Each : Extra) {
            place(Rng, Each, null, 60);
        }
        for (int X = 0; X < Slots; X++) {
            for (int Z = 0; Z < Slots; Z++) {
                Corridor[X][Z] = Active[X][Z] && Owner[X][Z] < 0;
            }
        }
        spanCorridors(Rng);
        for (Room Each : Rooms) {
            if (!chooseDoors(Rng, Each)) {
                return false;
            }
        }
        prune();
        addLoops(Rng);
        if (!connected(null)) {
            return false;
        }
        if (Bridgeless != null) {
            for (Room Each : Rooms) {
                if (Each.Type.equals(Bridgeless) && !connected(Each)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void claim(Room Made) {
        Made.Id = Rooms.size();
        Rooms.add(Made);
        for (int X = Made.Sx; X < Made.Sx + Made.W; X++) {
            for (int Z = Made.Sz; Z < Made.Sz + Made.D; Z++) {
                Owner[X][Z] = Made.Id;
            }
        }
    }

    private boolean fits(int Sx, int Sz, int W, int D) {
        if (Sx < 0 || Sz < 0 || Sx + W > Slots || Sz + D > Slots) {
            return false;
        }
        for (int X = Sx; X < Sx + W; X++) {
            for (int Z = Sz; Z < Sz + D; Z++) {
                if (!Active[X][Z] || Owner[X][Z] >= 0) {
                    return false;
                }
            }
        }
        boolean Free = false;
        for (int X = Sx; X < Sx + W; X++) {
            for (int Z = Sz; Z < Sz + D; Z++) {
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Nx = X + Dx[Dir];
                    int Nz = Z + Dz[Dir];
                    if (Nx >= Sx && Nx < Sx + W && Nz >= Sz && Nz < Sz + D) {
                        continue;
                    }
                    if (Nx >= 0 && Nz >= 0 && Nx < Slots && Nz < Slots && Active[Nx][Nz] && Owner[Nx][Nz] < 0) {
                        Free = true;
                    }
                }
            }
        }
        return Free;
    }

    private Room place(Random Rng, Spec Wanted, int[] Unused, int Tries) {
        for (int Try = 0; Try < Tries; Try++) {
            boolean Turn = Wanted.W() != Wanted.D() && Rng.nextBoolean();
            int W = Turn ? Wanted.D() : Wanted.W();
            int D = Turn ? Wanted.W() : Wanted.D();
            int Sx = Rng.nextInt(Slots - W + 1);
            int Sz = Rng.nextInt(Slots - D + 1);
            if (fits(Sx, Sz, W, D)) {
                Room Made = new Room(Wanted.Type(), Sx, Sz, W, D);
                claim(Made);
                return Made;
            }
        }
        return null;
    }

    private Room placeFar(Random Rng, Spec Wanted) {
        int[][] Distance = slotDistances(Entry);
        List<int[]> Options = new ArrayList<>();
        for (int Turn = 0; Turn < 2; Turn++) {
            int W = Turn == 1 ? Wanted.D() : Wanted.W();
            int D = Turn == 1 ? Wanted.W() : Wanted.D();
            for (int Sx = 0; Sx <= Slots - W; Sx++) {
                for (int Sz = 0; Sz <= Slots - D; Sz++) {
                    if (fits(Sx, Sz, W, D) && Distance[Sx][Sz] >= 0) {
                        Options.add(new int[]{Sx, Sz, W, D, Distance[Sx][Sz]});
                    }
                }
            }
        }
        if (Options.isEmpty()) {
            return null;
        }
        Options.sort((A, B) -> Integer.compare(B[4], A[4]));
        int Keep = Math.max(1, Options.size() / 5);
        int[] Pick = Options.get(Rng.nextInt(Keep));
        Room Made = new Room(Wanted.Type(), Pick[0], Pick[1], Pick[2], Pick[3]);
        claim(Made);
        return Made;
    }

    private int[][] slotDistances(Room From) {
        int[][] Distance = new int[Slots][Slots];
        for (int[] Row : Distance) {
            Arrays.fill(Row, -1);
        }
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        for (int X = From.Sx; X < From.Sx + From.W; X++) {
            for (int Z = From.Sz; Z < From.Sz + From.D; Z++) {
                Distance[X][Z] = 0;
                Queue.add(new int[]{X, Z});
            }
        }
        while (!Queue.isEmpty()) {
            int[] Cell = Queue.poll();
            for (int Dir = 0; Dir < 4; Dir++) {
                int Nx = Cell[0] + Dx[Dir];
                int Nz = Cell[1] + Dz[Dir];
                if (Nx >= 0 && Nz >= 0 && Nx < Slots && Nz < Slots && Active[Nx][Nz] && Distance[Nx][Nz] < 0) {
                    Distance[Nx][Nz] = Distance[Cell[0]][Cell[1]] + 1;
                    Queue.add(new int[]{Nx, Nz});
                }
            }
        }
        return Distance;
    }

    private boolean corridorAt(int X, int Z) {
        return X >= 0 && Z >= 0 && X < Slots && Z < Slots && Corridor[X][Z];
    }

    private void link(int X, int Z, int Dir) {
        Links[X][Z][Dir] = true;
        int Nx = X + Dx[Dir];
        int Nz = Z + Dz[Dir];
        if (Nx >= 0 && Nz >= 0 && Nx < Slots && Nz < Slots) {
            Links[Nx][Nz][(Dir + 2) % 4] = true;
        }
    }

    private void spanCorridors(Random Rng) {
        boolean[][] Seen = new boolean[Slots][Slots];
        for (int Sx = 0; Sx < Slots; Sx++) {
            for (int Sz = 0; Sz < Slots; Sz++) {
                if (!Corridor[Sx][Sz] || Seen[Sx][Sz]) {
                    continue;
                }
                List<int[]> Frontier = new ArrayList<>();
                Seen[Sx][Sz] = true;
                addFrontier(Frontier, Sx, Sz, Seen);
                while (!Frontier.isEmpty()) {
                    int[] Edge = Frontier.remove(Rng.nextInt(Frontier.size()));
                    int Nx = Edge[0] + Dx[Edge[2]];
                    int Nz = Edge[1] + Dz[Edge[2]];
                    if (Seen[Nx][Nz]) {
                        continue;
                    }
                    Seen[Nx][Nz] = true;
                    link(Edge[0], Edge[1], Edge[2]);
                    addFrontier(Frontier, Nx, Nz, Seen);
                }
            }
        }
    }

    private void addFrontier(List<int[]> Frontier, int X, int Z, boolean[][] Seen) {
        for (int Dir = 0; Dir < 4; Dir++) {
            int Nx = X + Dx[Dir];
            int Nz = Z + Dz[Dir];
            if (corridorAt(Nx, Nz) && !Seen[Nx][Nz]) {
                Frontier.add(new int[]{X, Z, Dir});
            }
        }
    }

    private boolean chooseDoors(Random Rng, Room Made) {
        List<Door> Options = new ArrayList<>();
        for (int X = Made.Sx; X < Made.Sx + Made.W; X++) {
            for (int Z = Made.Sz; Z < Made.Sz + Made.D; Z++) {
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Nx = X + Dx[Dir];
                    int Nz = Z + Dz[Dir];
                    if (Made.covers(Nx, Nz)) {
                        continue;
                    }
                    if (corridorAt(Nx, Nz)) {
                        Options.add(new Door(X, Z, Dir));
                    }
                }
            }
        }
        if (Options.isEmpty()) {
            return false;
        }
        Spec Wanted = specOf(Made);
        int Count = Wanted.MinDoors() + (Wanted.MaxDoors() > Wanted.MinDoors() ? Rng.nextInt(Wanted.MaxDoors() - Wanted.MinDoors() + 1) : 0);
        Count = Math.min(Count, Options.size());
        for (int Index = Options.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            Door Held = Options.get(Index);
            Options.set(Index, Options.get(Swap));
            Options.set(Swap, Held);
        }
        List<Door> Chosen = new ArrayList<>();
        if ((Made.Type.equals("checkpoint") || Made.Type.equals("tesla")) && (Made.W == 2 || Made.D == 2)) {
            Door First = null;
            Door Second = null;
            for (Door Each : Options) {
                boolean Start = Made.W == 2 ? Each.Sx == Made.Sx : Each.Sz == Made.Sz;
                if (Start && First == null) {
                    First = Each;
                } else if (!Start && Second == null) {
                    Second = Each;
                }
            }
            if (First == null || Second == null) {
                return false;
            }
            Chosen.add(First);
            Chosen.add(Second);
            Made.Doors.addAll(Chosen);
            for (Door Each : Chosen) {
                int Nx = Each.Sx + Dx[Each.Dir];
                int Nz = Each.Sz + Dz[Each.Dir];
                Links[Nx][Nz][(Each.Dir + 2) % 4] = true;
            }
            return true;
        }
        boolean[] SideUsed = new boolean[4];
        for (Door Each : Options) {
            if (Chosen.size() >= Count) {
                break;
            }
            if (!SideUsed[Each.Dir] || Chosen.size() >= 2) {
                Chosen.add(Each);
                SideUsed[Each.Dir] = true;
            }
        }
        for (Door Each : Options) {
            if (Chosen.size() >= Count) {
                break;
            }
            if (!Chosen.contains(Each)) {
                Chosen.add(Each);
            }
        }
        Made.Doors.addAll(Chosen);
        for (Door Each : Chosen) {
            int Nx = Each.Sx + Dx[Each.Dir];
            int Nz = Each.Sz + Dz[Each.Dir];
            Links[Nx][Nz][(Each.Dir + 2) % 4] = true;
        }
        return true;
    }

    private Spec specOf(Room Made) {
        return Scp1730Builder.specFor(ZoneIndex, Made.Type);
    }

    private int degree(int X, int Z) {
        int Count = 0;
        for (int Dir = 0; Dir < 4; Dir++) {
            if (Links[X][Z][Dir]) {
                Count++;
            }
        }
        return Count;
    }

    private boolean doorInto(int X, int Z, int Dir) {
        int Nx = X + Dx[Dir];
        int Nz = Z + Dz[Dir];
        return Nx >= 0 && Nz >= 0 && Nx < Slots && Nz < Slots && Owner[Nx][Nz] >= 0 && Links[X][Z][Dir];
    }

    private void prune() {
        boolean Changed = true;
        while (Changed) {
            Changed = false;
            for (int X = 0; X < Slots; X++) {
                for (int Z = 0; Z < Slots; Z++) {
                    if (!Corridor[X][Z]) {
                        continue;
                    }
                    boolean Doors = false;
                    for (int Dir = 0; Dir < 4; Dir++) {
                        if (doorInto(X, Z, Dir)) {
                            Doors = true;
                        }
                    }
                    if (!Doors && degree(X, Z) <= 1) {
                        Corridor[X][Z] = false;
                        for (int Dir = 0; Dir < 4; Dir++) {
                            if (Links[X][Z][Dir]) {
                                Links[X][Z][Dir] = false;
                                int Nx = X + Dx[Dir];
                                int Nz = Z + Dz[Dir];
                                if (Nx >= 0 && Nz >= 0 && Nx < Slots && Nz < Slots) {
                                    Links[Nx][Nz][(Dir + 2) % 4] = false;
                                }
                            }
                        }
                        Changed = true;
                    }
                }
            }
        }
    }

    private void addLoops(Random Rng) {
        for (int X = 0; X < Slots; X++) {
            for (int Z = 0; Z < Slots; Z++) {
                if (!Corridor[X][Z]) {
                    continue;
                }
                for (int Dir = 1; Dir <= 2; Dir++) {
                    int Nx = X + Dx[Dir];
                    int Nz = Z + Dz[Dir];
                    if (corridorAt(Nx, Nz) && !Links[X][Z][Dir] && Rng.nextInt(100) < 30) {
                        link(X, Z, Dir);
                    }
                }
            }
        }
    }

    private boolean connected(Room Without) {
        int Nodes = Slots * Slots + Rooms.size();
        boolean[] Seen = new boolean[Nodes];
        ArrayDeque<Integer> Queue = new ArrayDeque<>();
        int Start = Slots * Slots + Entry.Id;
        Seen[Start] = true;
        Queue.add(Start);
        while (!Queue.isEmpty()) {
            int Node = Queue.poll();
            if (Node >= Slots * Slots) {
                Room Here = Rooms.get(Node - Slots * Slots);
                if (Here == Without) {
                    continue;
                }
                for (Door Each : Here.Doors) {
                    int Nx = Each.Sx + Dx[Each.Dir];
                    int Nz = Each.Sz + Dz[Each.Dir];
                    int Next = Nx * Slots + Nz;
                    if (!Seen[Next]) {
                        Seen[Next] = true;
                        Queue.add(Next);
                    }
                }
            } else {
                int X = Node / Slots;
                int Z = Node % Slots;
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (!Links[X][Z][Dir]) {
                        continue;
                    }
                    int Nx = X + Dx[Dir];
                    int Nz = Z + Dz[Dir];
                    if (Nx < 0 || Nz < 0 || Nx >= Slots || Nz >= Slots) {
                        continue;
                    }
                    int Next = Owner[Nx][Nz] >= 0 && !Corridor[Nx][Nz] ? Slots * Slots + Owner[Nx][Nz] : Nx * Slots + Nz;
                    if (Owner[Nx][Nz] >= 0 && !Corridor[Nx][Nz]) {
                        Room Target = Rooms.get(Owner[Nx][Nz]);
                        boolean HasDoor = false;
                        for (Door Each : Target.Doors) {
                            if (Each.Sx == Nx && Each.Sz == Nz && Each.Dir == (Dir + 2) % 4) {
                                HasDoor = true;
                            }
                        }
                        if (!HasDoor) {
                            continue;
                        }
                    }
                    if (!Seen[Next]) {
                        Seen[Next] = true;
                        Queue.add(Next);
                    }
                }
            }
        }
        for (Room Each : Rooms) {
            if (Each == Without) {
                continue;
            }
            if (!Seen[Slots * Slots + Each.Id]) {
                return false;
            }
        }
        return true;
    }
}
