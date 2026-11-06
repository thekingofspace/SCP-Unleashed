package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Scp1730SectorCheck {
    public static final int Drop = 3;
    public static final int MaxSwim = 8;
    public static final int[] WingZones = {1, 2, 3};

    private Scp1730SectorCheck() {
    }

    public static List<String> check(Scp1730Plan Plan) {
        List<String> Problems = new ArrayList<>();
        for (int Zone = 0; Zone < 4; Zone++) {
            Scp1730Spot Entry = Plan.entry(Zone);
            if (Entry == null) {
                continue;
            }
            int Low = Plan.floorY(Zone) - Scp1730Sectors.BasinDepth - 3;
            int High = Plan.topY(Zone) + 3;
            Walk Closed = new Walk(Plan, Low, High, false, false);
            Closed.flood(Entry);
            Walk Open = new Walk(Plan, Low, High, true, false);
            Open.flood(Entry);
            Walk Dry = new Walk(Plan, Low, High, true, true);
            Dry.flood(Entry);
            wing(Plan, Zone, Closed, Open, Problems);
            for (String Kind : new String[]{"control", "card", "biocontrol", "loot", "setpiece"}) {
                for (Scp1730Fixture Each : Plan.fixtures(Kind, Zone)) {
                    int Y = Kind.equals("control") || Kind.equals("card") || Kind.equals("biocontrol") ? Plan.floorY(Zone) + 1 : Each.Y();
                    if (Open.near(Each.X(), Y, Each.Z(), 2) && !Dry.near(Each.X(), Y, Each.Z(), 2)) {
                        Problems.add("zone " + Zone + ": " + Kind + " at " + Each.X() + "," + Each.Y() + "," + Each.Z() + " needs a walk with the head under water");
                    }
                }
            }
            for (Scp1730RoomInfo Room : Plan.rooms()) {
                if (Room.Zone() != Zone || Room.Type().startsWith("stair")) {
                    continue;
                }
                for (int[] Door : Room.Doors()) {
                    int Ox = Scp1730Layout.Dx[Door[2]];
                    int Oz = Scp1730Layout.Dz[Door[2]];
                    if (Open.near(Door[0] + Ox, Room.Floor() + 1, Door[1] + Oz, 1) && !Dry.near(Door[0] + Ox, Room.Floor() + 1, Door[1] + Oz, 1)) {
                        Problems.add("zone " + Zone + ": the " + Room.Type() + " door needs a walk with the head under water");
                    }
                }
            }
            for (Scp1730Fixture Each : Plan.fixtures("setpiece", Zone)) {
                boolean Wing = Plan.inWing(Each.X(), Each.Y(), Each.Z());
                if (Scp1730SetPieces.biological(Zone, Each.Data(), Plan.seed()) != Wing && (Wing || Scp1730SetPieces.biological(Zone, Each.Data(), Plan.seed()))) {
                    Problems.add("zone " + Zone + ": set piece " + Each.Data() + " at " + Each.X() + "," + Each.Z() + (Wing ? " is not biological but sits in the wing" : " is biological but sits outside the wing"));
                }
            }
            for (Scp1730Basin Each : Plan.basins()) {
                if (Each.Zone() == Zone) {
                    escape(Plan, Each, Open, Problems);
                }
            }
        }
        return Problems;
    }

    static void wing(Scp1730Plan Plan, int Zone, Walk Closed, Walk Open, List<String> Problems) {
        List<Scp1730RoomInfo> Wings = new ArrayList<>();
        for (Scp1730RoomInfo Each : Plan.rooms(Scp1730Sectors.BioWing)) {
            if (Each.Zone() == Zone) {
                Wings.add(Each);
            }
        }
        boolean Wanted = false;
        for (int Each : WingZones) {
            Wanted |= Each == Zone;
        }
        if (!Wanted) {
            if (!Wings.isEmpty()) {
                Problems.add("zone " + Zone + ": holds a biological wing");
            }
            return;
        }
        if (Wings.size() != 1) {
            Problems.add("zone " + Zone + ": " + Wings.size() + " biological wings");
            return;
        }
        Scp1730RoomInfo Wing = Wings.getFirst();
        int Seals = 0;
        for (Scp1730Fixture Each : Plan.fixtures("bioseal", Zone)) {
            Seals++;
        }
        if (Seals != 9) {
            Problems.add("zone " + Zone + ": the wing door has " + Seals + " seal blocks");
        }
        Scp1730Fixture Outer = null;
        Scp1730Fixture Inner = null;
        for (Scp1730Fixture Each : Plan.fixtures("biocontrol", Zone)) {
            if (Each.Data() == Scp1730Sectors.OuterReader) {
                Outer = Each;
            } else if (Each.Data() == Scp1730Sectors.InnerReader) {
                Inner = Each;
            }
        }
        int F = Wing.Floor();
        if (Outer == null || Wing.contains(Outer.X(), Outer.Y(), Outer.Z())) {
            Problems.add("zone " + Zone + ": the wing has no reader outside its door");
        } else if (!Closed.near(Outer.X(), F + 1, Outer.Z(), 2)) {
            Problems.add("zone " + Zone + ": the wing reader cannot be reached");
        }
        if (Inner == null || !Wing.contains(Inner.X(), Inner.Y(), Inner.Z())) {
            Problems.add("zone " + Zone + ": the wing has no way out from inside");
        } else if (!Open.near(Inner.X(), F + 1, Inner.Z(), 2)) {
            Problems.add("zone " + Zone + ": the wing exit panel cannot be reached");
        }
        Scp1730Fixture Card = null;
        for (Scp1730Fixture Each : Plan.fixtures("card", Zone)) {
            if (Each.Data() == Scp1730Sectors.ClearanceCard) {
                Card = Each;
            }
        }
        if (Card == null) {
            Problems.add("zone " + Zone + ": no clearance card");
        } else if (Plan.inWing(Card.X(), Card.Y(), Card.Z())) {
            Problems.add("zone " + Zone + ": the clearance card lies inside the wing");
        } else if (!Closed.near(Card.X(), Plan.floorY(Zone) + 1, Card.Z(), 2)) {
            Problems.add("zone " + Zone + ": the clearance card cannot be reached");
        }
        for (int X = Wing.MinX() + 1; X < Wing.MaxX(); X++) {
            for (int Z = Wing.MinZ() + 1; Z < Wing.MaxZ(); Z++) {
                for (int Y = F; Y <= F + 2; Y++) {
                    if (Closed.seen(X, Y, Z)) {
                        Problems.add("zone " + Zone + ": the wing can be entered without opening its door at " + X + "," + Y + "," + Z);
                        return;
                    }
                }
            }
        }
        int Pieces = 0;
        for (Scp1730Fixture Each : Plan.fixtures("setpiece", Zone)) {
            if (!Plan.inWing(Each.X(), Each.Y(), Each.Z())) {
                continue;
            }
            Pieces++;
            if (!Open.near(Each.X(), Each.Y(), Each.Z(), 2)) {
                Problems.add("zone " + Zone + ": wing set piece " + Each.Data() + " cannot be reached with the door open");
            }
        }
        if (Pieces == 0) {
            Problems.add("zone " + Zone + ": the wing holds no set piece");
        }
    }

    static boolean water(Scp1730Plan Plan, int X, int Y, int Z) {
        return Scp1730Blocks.water(Plan.stateAt(X, Y, Z));
    }

    static boolean ladder(Scp1730Plan Plan, int X, int Y, int Z) {
        String State = Plan.stateAt(X, Y, Z);
        return State != null && State.startsWith(Scp1730Blocks.Ladder);
    }

    static void escape(Scp1730Plan Plan, Scp1730Basin Each, Walk Open, List<String> Problems) {
        List<int[]> Exits = new ArrayList<>();
        for (Scp1730Fixture Fixture : Plan.fixtures("ladder", Each.Zone())) {
            if (Fixture.X() < Each.MinX() - 1 || Fixture.X() > Each.MaxX() + 1 || Fixture.Z() < Each.MinZ() - 1 || Fixture.Z() > Each.MaxZ() + 1) {
                continue;
            }
            int Top = Fixture.Y();
            for (int Y = Top - Fixture.Data() + 1; Y <= Top; Y++) {
                if (!ladder(Plan, Fixture.X(), Y, Fixture.Z())) {
                    Problems.add("ladder at " + Fixture.X() + "," + Fixture.Z() + " is broken at Y " + Y);
                    break;
                }
            }
            if (!Open.near(Fixture.X(), Top + 1, Fixture.Z(), 1)) {
                Problems.add("ladder at " + Fixture.X() + "," + Fixture.Z() + " does not lead back to the walkway");
            }
            Exits.add(new int[]{Fixture.X(), Fixture.Z()});
        }
        if (Each.wet()) {
            swim(Plan, Each, Exits, Problems);
        } else {
            climb(Plan, Each, Open, Problems);
        }
    }

    static void swim(Scp1730Plan Plan, Scp1730Basin Each, List<int[]> Exits, List<String> Problems) {
        int Wide = Each.MaxX() - Each.MinX() + 1;
        int Deep = Each.MaxZ() - Each.MinZ() + 1;
        int[][] Distance = new int[Wide][Deep];
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        for (int[] Row : Distance) {
            java.util.Arrays.fill(Row, -1);
        }
        for (int[] Exit : Exits) {
            int X = Exit[0] - Each.MinX();
            int Z = Exit[1] - Each.MinZ();
            if (X >= 0 && Z >= 0 && X < Wide && Z < Deep) {
                Distance[X][Z] = 0;
                Queue.add(new int[]{X, Z});
            }
        }
        int Y = Each.TopY();
        while (!Queue.isEmpty()) {
            int[] Here = Queue.poll();
            for (int Dir = 0; Dir < 4; Dir++) {
                int Nx = Here[0] + Scp1730Layout.Dx[Dir];
                int Nz = Here[1] + Scp1730Layout.Dz[Dir];
                if (Nx < 0 || Nz < 0 || Nx >= Wide || Nz >= Deep || Distance[Nx][Nz] >= 0 || !water(Plan, Each.MinX() + Nx, Y, Each.MinZ() + Nz)) {
                    continue;
                }
                Distance[Nx][Nz] = Distance[Here[0]][Here[1]] + 1;
                Queue.add(new int[]{Nx, Nz});
            }
        }
        for (int Nx = 0; Nx < Wide; Nx++) {
            for (int Nz = 0; Nz < Deep; Nz++) {
                int X = Each.MinX() + Nx;
                int Z = Each.MinZ() + Nz;
                if (!water(Plan, X, Y, Z) || ladder(Plan, X, Y, Z)) {
                    continue;
                }
                if (Distance[Nx][Nz] < 0) {
                    Problems.add("water basin cell " + X + "," + Z + " has no ladder within reach");
                    return;
                }
                if (Distance[Nx][Nz] > MaxSwim) {
                    Problems.add("water basin cell " + X + "," + Z + " is " + Distance[Nx][Nz] + " strokes from a ladder");
                    return;
                }
            }
        }
    }

    static void climb(Scp1730Plan Plan, Scp1730Basin Each, Walk Open, List<String> Problems) {
        int Wide = Each.MaxX() - Each.MinX() + 1;
        int Deep = Each.MaxZ() - Each.MinZ() + 1;
        boolean[][] Done = new boolean[Wide][Deep];
        int Y = Each.BottomY() + 1;
        for (int Nx = 0; Nx < Wide; Nx++) {
            for (int Nz = 0; Nz < Deep; Nz++) {
                int X = Each.MinX() + Nx;
                int Z = Each.MinZ() + Nz;
                if (Done[Nx][Nz] || !Open.stand(X, Y, Z) && !ladder(Plan, X, Y, Z)) {
                    continue;
                }
                ArrayDeque<int[]> Queue = new ArrayDeque<>();
                Queue.add(new int[]{Nx, Nz});
                Done[Nx][Nz] = true;
                while (!Queue.isEmpty()) {
                    int[] Here = Queue.poll();
                    for (int Dir = 0; Dir < 4; Dir++) {
                        int Ax = Here[0] + Scp1730Layout.Dx[Dir];
                        int Az = Here[1] + Scp1730Layout.Dz[Dir];
                        if (Ax < 0 || Az < 0 || Ax >= Wide || Az >= Deep || Done[Ax][Az]) {
                            continue;
                        }
                        if (Open.stand(Each.MinX() + Ax, Y, Each.MinZ() + Az) || ladder(Plan, Each.MinX() + Ax, Y, Each.MinZ() + Az)) {
                            Done[Ax][Az] = true;
                            Queue.add(new int[]{Ax, Az});
                        }
                    }
                }
                if (!out(Plan, Each, Open, X, Y, Z)) {
                    Problems.add("drop below " + X + "," + Z + " has no way back up");
                    return;
                }
            }
        }
    }

    static boolean position(Walk Open, Scp1730Plan Plan, int X, int Y, int Z) {
        return Open.stand(X, Y, Z) || ladder(Plan, X, Y, Z) && Open.open(X, Y + 1, Z);
    }

    static boolean out(Scp1730Plan Plan, Scp1730Basin Each, Walk Open, int X, int Y, int Z) {
        java.util.Set<Long> Visited = new java.util.HashSet<>();
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        Queue.add(new int[]{X, Y, Z});
        Visited.add(Scp1730Builder.key(X, Y, Z));
        while (!Queue.isEmpty()) {
            int[] Here = Queue.poll();
            if (Open.seen(Here[0], Here[1], Here[2]) && Here[1] > Each.TopY()) {
                return true;
            }
            if (Here[0] < Each.MinX() - 4 || Here[0] > Each.MaxX() + 4 || Here[2] < Each.MinZ() - 4 || Here[2] > Each.MaxZ() + 4) {
                continue;
            }
            List<int[]> Next = new ArrayList<>();
            boolean Climbing = ladder(Plan, Here[0], Here[1], Here[2]);
            boolean Jump = Open.open(Here[0], Here[1] + 2, Here[2]);
            if (Climbing) {
                Next.add(new int[]{Here[0], Here[1] + 1, Here[2]});
            }
            for (int Dir = 0; Dir < 4; Dir++) {
                int Nx = Here[0] + Scp1730Layout.Dx[Dir];
                int Nz = Here[2] + Scp1730Layout.Dz[Dir];
                for (int Dy = 1; Dy >= -Drop; Dy--) {
                    if (Dy == 1 && !Jump && !Climbing) {
                        continue;
                    }
                    Next.add(new int[]{Nx, Here[1] + Dy, Nz});
                }
            }
            for (int[] Step : Next) {
                if (!position(Open, Plan, Step[0], Step[1], Step[2]) || !Visited.add(Scp1730Builder.key(Step[0], Step[1], Step[2]))) {
                    continue;
                }
                Queue.add(Step);
            }
        }
        return false;
    }

    static boolean bottom(Scp1730Plan Plan, int X, int Bottom, int Z) {
        String Floor = Plan.stateAt(X, Bottom, Z);
        String Feet = Plan.stateAt(X, Bottom + 1, Z);
        return Floor != null && !Scp1730Blocks.passable(Floor) && (Feet == null || Scp1730Blocks.passable(Feet) || Feet.startsWith(Scp1730Blocks.Ladder));
    }

    static final class Walk {
        final Scp1730Plan Plan;
        final int Low;
        final int High;
        final boolean BioOpen;
        final boolean Dry;
        final boolean[] Seen;
        final Scp1730RoomDressing.Cells Source;

        Walk(Scp1730Plan Plan, int Low, int High, boolean BioOpen, boolean Dry) {
            this.Plan = Plan;
            this.Low = Low;
            this.High = High;
            this.BioOpen = BioOpen;
            this.Dry = Dry;
            this.Source = Scp1730Cutoffs.through(Plan);
            this.Seen = new boolean[Scp1730Plan.Width * Scp1730Plan.Depth * (High - Low + 1)];
        }

        int index(int X, int Y, int Z) {
            return ((Y - Low) * Scp1730Plan.Depth + Z) * Scp1730Plan.Width + X;
        }

        boolean inside(int X, int Y, int Z) {
            return X >= 0 && Z >= 0 && X < Scp1730Plan.Width && Z < Scp1730Plan.Depth && Y >= Low && Y <= High;
        }

        boolean seen(int X, int Y, int Z) {
            return inside(X, Y, Z) && Seen[index(X, Y, Z)];
        }

        boolean open(int X, int Y, int Z) {
            String State = Source.at(X, Y, Z);
            if (BioOpen && State != null && State.startsWith(Scp1730Blocks.Seal) && Plan.wingSeal(X, Y, Z)) {
                return true;
            }
            return Scp1730Blocks.passable(State);
        }

        boolean solid(int X, int Y, int Z) {
            return !open(X, Y, Z);
        }

        boolean stand(int X, int Y, int Z) {
            if (!inside(X, Y, Z) || !inside(X, Y + 1, Z)) {
                return false;
            }
            if (Dry && water(Plan, X, Y + 1, Z)) {
                return false;
            }
            return open(X, Y, Z) && open(X, Y + 1, Z) && solid(X, Y - 1, Z) && !water(Plan, X, Y - 1, Z);
        }

        void flood(Scp1730Spot Entry) {
            int X = (int) Math.floor(Entry.X());
            int Y = (int) Math.floor(Entry.Y());
            int Z = (int) Math.floor(Entry.Z());
            int Start = Y;
            if (!stand(X, Start, Z)) {
                if (stand(X, Start + 1, Z)) {
                    Start++;
                } else if (stand(X, Start - 1, Z)) {
                    Start--;
                } else {
                    return;
                }
            }
            ArrayDeque<int[]> Queue = new ArrayDeque<>();
            Seen[index(X, Start, Z)] = true;
            Queue.add(new int[]{X, Start, Z});
            while (!Queue.isEmpty()) {
                int[] Here = Queue.poll();
                boolean Jump = open(Here[0], Here[1] + 2, Here[2]);
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Nx = Here[0] + Scp1730Layout.Dx[Dir];
                    int Nz = Here[2] + Scp1730Layout.Dz[Dir];
                    for (int Dy = 1; Dy >= -Drop; Dy--) {
                        int Ny = Here[1] + Dy;
                        if (Dy == 1 && !Jump) {
                            continue;
                        }
                        if (Dy < 0) {
                            boolean Clear = true;
                            for (int Step = 0; Step > Dy; Step--) {
                                if (!open(Nx, Here[1] + Step, Nz) || !open(Nx, Here[1] + Step + 1, Nz)) {
                                    Clear = false;
                                }
                            }
                            if (!Clear) {
                                break;
                            }
                        }
                        if (stand(Nx, Ny, Nz)) {
                            int Key = index(Nx, Ny, Nz);
                            if (!Seen[Key]) {
                                Seen[Key] = true;
                                Queue.add(new int[]{Nx, Ny, Nz});
                            }
                            break;
                        }
                    }
                }
            }
        }

        boolean near(int X, int Y, int Z, int Radius) {
            for (int Ox = -Radius; Ox <= Radius; Ox++) {
                for (int Oz = -Radius; Oz <= Radius; Oz++) {
                    for (int Oy = -1; Oy <= 1; Oy++) {
                        if (seen(X + Ox, Y + Oy, Z + Oz)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
