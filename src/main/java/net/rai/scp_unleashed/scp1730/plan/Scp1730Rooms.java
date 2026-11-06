package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Scp1730Rooms {
    private static final String M = Scp1730Blocks.Mod;
    private final Scp1730Builder B;
    private final Scp1730Grid G;
    private final Random Rng;

    Scp1730Rooms(Scp1730Builder Builder) {
        this.B = Builder;
        this.G = Builder.Grid;
        this.Rng = Builder.Rng;
    }

    public Scp1730Blocks.Palette paletteOf(Scp1730Layout.Room Made, int Zone) {
        return switch (Made.Type) {
            case "lobby", "cafeteria", "elevator", "checkpoint" -> Scp1730Blocks.Ez;
            case "office", "small_office", "security" -> Scp1730Blocks.Office;
            case "breaker", "generator", "server" -> Scp1730Blocks.Power;
            case "medical" -> Scp1730Blocks.Medical;
            case "test_chamber", "research" -> Scp1730Blocks.Research;
            case "cell_block", "dorms" -> Scp1730Blocks.Lcz;
            case "heavy_cell", "collapsed", "holding_cell" -> Scp1730Blocks.Containment;
            case "tesla", "armoury", "pit" -> Scp1730Blocks.Hcz;
            case "sanctum" -> Chamber;
            default -> Scp1730Variants.palette(Made.Type, Zone, Scp1730AnomalousRooms.palette(Made.Type, Zone, B.corridorPalette(Zone)));
        };
    }

    public static final Scp1730Blocks.Palette Chamber = new ChamberPalette();

    private static final class ChamberPalette extends Scp1730Blocks.Palette {
        ChamberPalette() {
            super("containment_room");
        }

        @Override
        public String wall(int Level, int Height) {
            return Scp1730Blocks.ChamberStone;
        }
    }

    public String lampOf(int Zone) {
        return switch (Zone) {
            case 0 -> Scp1730Blocks.Ez.Lamp;
            case 1 -> Scp1730Blocks.Lcz.Lamp;
            case 2 -> Scp1730Blocks.Hcz.Lamp;
            default -> Scp1730Blocks.Containment.Lamp;
        };
    }

    public void ceilingLight(int X, int Y, int Z, int Zone, boolean AlongX) {
        if (!G.empty(X, Y, Z) || G.empty(X, Y + 1, Z)) {
            return;
        }
        int Roll = Rng.nextInt(100);
        int[] Working = {18, 12, 8, 0};
        int[] Emergency = {30, 34, 38, 50};
        int[] Broken = {30, 30, 30, 25};
        if (Roll < Working[Zone]) {
            G.set(X, Y, Z, M + "fluorescent_light[face=ceiling,facing=" + (AlongX ? "east" : "north") + ",lit=true]");
        } else if (Roll < Working[Zone] + Emergency[Zone]) {
            G.set(X, Y, Z, Scp1730Blocks.facing(Scp1730Blocks.EmergencyLight, "down"));
        } else if (Roll < Working[Zone] + Emergency[Zone] + Broken[Zone]) {
            G.set(X, Y, Z, Scp1730Blocks.facing(Scp1730Blocks.BrokenLight, "down"));
        } else {
            G.set(X, Y, Z, Scp1730Blocks.facing(Scp1730Blocks.DeadLight, "down"));
        }
    }

    public void wallLight(int X, int Y, int Z, Scp1730Builder.StairFrame Frame, int U, int V, int Zone) {
        if (!G.empty(X, Y, Z)) {
            return;
        }
        String Out = U <= 1 ? Frame.facing(1, 0) : U >= 14 ? Frame.facing(-1, 0) : V <= 1 ? Frame.facing(0, 1) : Frame.facing(0, -1);
        int Roll = Rng.nextInt(100);
        String Light = Roll < 55 ? Scp1730Blocks.EmergencyLight : Roll < 80 ? Scp1730Blocks.BrokenLight : Scp1730Blocks.DeadLight;
        if (Zone >= 2 && Roll >= 80) {
            Light = Scp1730Blocks.AlarmLight;
        }
        G.set(X, Y, Z, Scp1730Blocks.facing(Light, Out));
    }

    public void doorSign(Scp1730Layout.Room Made, Scp1730Layout.Door Each, int X, int Y, int Z, int Zone) {
        String Sign = switch (Made.Type) {
            case "checkpoint" -> "security_checkpoint";
            case "cafeteria" -> "cafeteria";
            case "elevator" -> "elevator";
            case "medical" -> "medical";
            case "server" -> "server_room";
            case "armoury" -> "armory";
            case "breaker", "generator" -> "maintenance";
            case "heavy_cell" -> "heavy_containment";
            case "cell_block" -> "light_containment";
            case "lobby" -> "entrance_zone";
            case "dorms" -> "evacuation_shelter";
            default -> null;
        };
        String Facing = Each.Dir % 2 == 0 ? "west" : "north";
        if (!G.empty(X, Y, Z)) {
            return;
        }
        if (Made.Type.equals("pit") || Made.Type.equals("holding_cell")) {
            String[] Warn = {Scp1730Blocks.SignKeter, Scp1730Blocks.SignMemetic, Scp1730Blocks.SignBiohazard};
            G.set(X, Y, Z, Scp1730Blocks.facing(Warn[Rng.nextInt(Warn.length)], Facing));
            return;
        }
        if (Made.Type.equals("generator") || Made.Type.equals("breaker") || Made.Type.equals("server")) {
            G.set(X, Y, Z, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.SignVoltage : M + "facility_sign_" + Sign + "_small", Facing));
            return;
        }
        if (Sign != null) {
            G.set(X, Y, Z, M + "facility_sign_" + Sign + "_small[facing=" + Facing + "]");
        }
    }

    final class Box {
        final Scp1730Layout.Room Room;
        final int Zone;
        final int X0;
        final int X1;
        final int Z0;
        final int Z1;
        final int F;
        final int H;
        final Scp1730Blocks.Palette Look;

        Box(Scp1730Layout.Room Room, int Zone) {
            this.Room = Room;
            this.Zone = Zone;
            this.X0 = Room.Bx0 + 1;
            this.X1 = Room.Bx1 - 1;
            this.Z0 = Room.Bz0 + 1;
            this.Z1 = Room.Bz1 - 1;
            this.F = Room.Floor;
            this.H = Room.Height;
            this.Look = paletteOf(Room, Zone);
        }

        int cx() {
            return (X0 + X1) / 2;
        }

        int cz() {
            return (Z0 + Z1) / 2;
        }

        boolean free(int X, int Y, int Z) {
            return X >= X0 && X <= X1 && Z >= Z0 && Z <= Z1 && B.freeCell(X, Y, Z);
        }

        boolean put(int X, int Y, int Z, String State) {
            if (!free(X, Y, Z)) {
                return false;
            }
            G.set(X, Y, Z, State);
            return true;
        }

        boolean tall(int X, int Z, String Base, String Props) {
            if (!free(X, F + 1, Z) || !free(X, F + 2, Z)) {
                return false;
            }
            G.set(X, F + 1, Z, Base + "[" + Props + ",half=lower]");
            G.set(X, F + 2, Z, Base + "[" + Props + ",half=upper]");
            return true;
        }

        boolean stack(int X, int Z, String Base, String Facing, int Count) {
            for (int Level = 1; Level <= Count; Level++) {
                if (!free(X, F + Level, Z)) {
                    return false;
                }
            }
            for (int Level = 1; Level <= Count; Level++) {
                String Part = Count == 1 ? "single" : Level == 1 ? "bottom" : Level == Count ? "top" : "middle";
                G.set(X, F + Level, Z, Base + "[facing=" + Facing + ",part=" + Part + "]");
            }
            return true;
        }

        List<int[]> wallCells() {
            List<int[]> Cells = new ArrayList<>();
            for (int X = X0; X <= X1; X++) {
                Cells.add(new int[]{X, Z0, 2});
                Cells.add(new int[]{X, Z1, 0});
            }
            for (int Z = Z0 + 1; Z < Z1; Z++) {
                Cells.add(new int[]{X0, Z, 1});
                Cells.add(new int[]{X1, Z, 3});
            }
            return Cells;
        }

        void clearLine(int Ax, int Az, int Bx, int Bz, int Half) {
            int Sx = Integer.signum(Bx - Ax);
            int Sz = Integer.signum(Bz - Az);
            int X = Ax;
            int Z = Az;
            while (true) {
                for (int Ox = -Half; Ox <= Half; Ox++) {
                    for (int Oz = -Half; Oz <= Half; Oz++) {
                        for (int Level = 1; Level <= 2; Level++) {
                            B.keepClear(X + Ox, F + Level, Z + Oz);
                        }
                    }
                }
                if (X == Bx && Z == Bz) {
                    break;
                }
                if (X != Bx) {
                    X += Sx;
                } else {
                    Z += Sz;
                }
            }
        }

        void paths() {
            paths(1);
        }

        void paths(int Half) {
            for (Scp1730Layout.Door Each : Room.Doors) {
                int Ix = Room.doorX(Each) - Scp1730Layout.Dx[Each.Dir];
                int Iz = Room.doorZ(Each) - Scp1730Layout.Dz[Each.Dir];
                for (int Offset = 0; Offset <= (Half == 0 ? 1 : 0); Offset++) {
                    clearLine(Ix + Offset, Iz + Offset, cx() + Offset, Iz + Offset, Half);
                    clearLine(cx() + Offset, Iz + Offset, cx() + Offset, cz() + Offset, Half);
                    clearLine(Ix + Offset, Iz + Offset, Ix + Offset, cz() + Offset, Half);
                    clearLine(Ix + Offset, cz() + Offset, cx() + Offset, cz() + Offset, Half);
                }
            }
        }

        void lights(int Every) {
            for (int X = X0 + Every / 2; X <= X1; X += Every) {
                for (int Z = Z0 + Every / 2; Z <= Z1; Z += Every) {
                    ceilingLight(X, F + H, Z, Zone, false);
                }
            }
        }

        void walls() {
            for (int[] Cell : wallCells()) {
                int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
                int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
                if (Scp1730Builder.isWallLike(G.get(Wx, F + 2, Wz))) {
                    B.RoomWalls.add(new int[]{Wx, Wz, Zone});
                }
            }
        }

        void turret() {
            B.TurretSpots.add(new int[]{cx(), F + H, cz(), Zone});
        }

        void mess(int Count) {
            for (int Index = 0; Index < Count; Index++) {
                int X = X0 + Rng.nextInt(X1 - X0 + 1);
                int Z = Z0 + Rng.nextInt(Z1 - Z0 + 1);
                int Roll = Rng.nextInt(100);
                if (Roll < 18) {
                    put(X, F + 1, Z, Scp1730Blocks.PaperStack + "[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]");
                } else if (Roll < 26) {
                    put(X, F + 1, Z, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.CableTangle : Scp1730Blocks.FallenPanel, face(Rng.nextInt(4))));
                } else if (Roll < 30) {
                    B.decal(X, F, Z, "up", Scp1730Builder.Puddle, Zone);
                } else if (Roll < 45) {
                    put(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, Scp1730Layout.Facings[Rng.nextInt(4)]));
                } else if (Roll < 55 && Zone > 0) {
                    put(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
                } else if (Roll < 65) {
                    put(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, Scp1730Layout.Facings[Rng.nextInt(4)]));
                } else if (Roll < 75) {
                    put(X, F + 1, Z, M + "paper_stack[facing=" + Scp1730Layout.Facings[Rng.nextInt(4)] + ",variant=" + Rng.nextInt(6) + "]");
                } else {
                    int Kind = B.pick(Zone == 0 ? new int[]{Scp1730Builder.Footprints, Scp1730Builder.Grime, Scp1730Builder.Scorch, Scp1730Builder.Puddle}
                            : new int[]{Scp1730Builder.Blood, Scp1730Builder.Scorch, Scp1730Builder.Footprints, Scp1730Builder.Oil, Scp1730Builder.Claws});
                    B.decal(X, F, Z, "up", Kind, Zone);
                }
            }
            for (int Index = 0; Index < Count / 2; Index++) {
                List<int[]> Cells = wallCells();
                int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
                int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
                int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
                int Kind = B.pick(new int[]{Scp1730Builder.Bullets, Scp1730Builder.Scorch, Scp1730Builder.Cracks, Scp1730Builder.Claws, Scp1730Builder.Blood, Scp1730Builder.Rust,
                        Scp1730Builder.Grime, Scp1730Builder.Corrosion, Scp1730Builder.Rust});
                B.decal(Wx, F + 1 + Rng.nextInt(Math.min(3, H)), Wz, Scp1730Layout.Facings[Cell[2]], Kind, Zone);
            }
        }

        Scp1730Layout.Door door() {
            return Room.Doors.get(0);
        }

        boolean container(int X, int Z, String Base, String Facing, int Tier) {
            if (Base.endsWith("locker")) {
                if (!tall(X, Z, Base, "facing=" + Facing + ",open=false")) {
                    return false;
                }
            } else if (!put(X, F + 1, Z, Base + "[facing=" + Facing + ",open=false]")) {
                return false;
            }
            B.loot(X, F + 1, Z, Facing, Tier, Zone);
            return true;
        }

        boolean wallContainer(String Base, int Tier) {
            List<int[]> Cells = wallCells();
            B.shuffle(Cells);
            for (int[] Cell : Cells) {
                if (container(Cell[0], Cell[1], Base, face(Cell[2]), Tier)) {
                    return true;
                }
            }
            return false;
        }

        void breach(int Debris, boolean Collapse, boolean Trail) {
            for (int Pile = 0; Pile < Debris; Pile++) {
                List<int[]> Cells = wallCells();
                int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
                int Size = 1 + Rng.nextInt(3);
                for (int Ox = -Size; Ox <= Size; Ox++) {
                    for (int Oz = -Size; Oz <= Size; Oz++) {
                        if (Math.abs(Ox) + Math.abs(Oz) > Size || Rng.nextInt(3) == 0) {
                            continue;
                        }
                        put(Cell[0] + Ox, F + 1, Cell[1] + Oz, Scp1730Blocks.facing(Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
                    }
                }
                B.decal(Cell[0], F, Cell[1], "up", Scp1730Builder.Cracks, Zone);
            }
            for (int Stain = 0; Stain < 3 + Zone; Stain++) {
                List<int[]> Cells = wallCells();
                int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
                int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
                int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
                B.decal(Wx, F + Math.min(H, 2 + Rng.nextInt(2)), Wz, face(Cell[2]), B.pick(new int[]{Scp1730Builder.Rust, Scp1730Builder.Grime, Scp1730Builder.Corrosion}), Zone);
                B.decal(Cell[0], F, Cell[1], "up", B.pick(new int[]{Scp1730Builder.Puddle, Scp1730Builder.Grime, Scp1730Builder.Corrosion}), Zone);
            }
            for (int Window = 0; Window < 2; Window++) {
                List<int[]> Cells = wallCells();
                int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
                int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
                int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
                if (Scp1730Builder.isWallLike(G.get(Wx, F + 2, Wz)) && G.empty(Cell[0], F + 2, Cell[1])) {
                    G.set(Wx, F + 2, Wz, Scp1730Blocks.facing(Scp1730Blocks.BoardedWindow, face(Cell[2])));
                }
            }
            for (int Cable = 0; Cable < 2; Cable++) {
                int X = X0 + Rng.nextInt(X1 - X0 + 1);
                int Z = Z0 + Rng.nextInt(Z1 - Z0 + 1);
                if (G.empty(X, F + H, Z) && G.get(X, F + H + 1, Z) != null) {
                    G.set(X, F + H, Z, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
                }
            }
            if (Collapse && X1 - X0 > 5 && Z1 - Z0 > 5) {
                int Hx = X0 + 2 + Rng.nextInt(X1 - X0 - 3);
                int Hz = Z0 + 2 + Rng.nextInt(Z1 - Z0 - 3);
                for (int Ox = -1; Ox <= 0; Ox++) {
                    for (int Oz = -1; Oz <= 0; Oz++) {
                        B.ceilingHole(Hx + Ox, F + H, Hz + Oz);
                        put(Hx + Ox, F + 1, Hz + Oz, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.FallenPanel : Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
                    }
                }
                if (G.empty(Hx + 1, F + H, Hz) && G.get(Hx + 1, F + H + 1, Hz) != null) {
                    G.set(Hx + 1, F + H, Hz, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
                }
            }
            if (Trail && Zone > 0 && !Room.Doors.isEmpty()) {
                Scp1730Layout.Door Out = Room.Doors.get(Rng.nextInt(Room.Doors.size()));
                int Ix = -Scp1730Layout.Dx[Out.Dir];
                int Iz = -Scp1730Layout.Dz[Out.Dir];
                int Length = 5 + Rng.nextInt(5);
                int Wander = 0;
                for (int Step = 1; Step <= Length; Step++) {
                    if (Rng.nextInt(3) == 0) {
                        Wander += Rng.nextBoolean() ? 1 : -1;
                        Wander = Math.max(-1, Math.min(1, Wander));
                    }
                    int X = Room.doorX(Out) + Ix * Step + (Ix == 0 ? Wander : 0);
                    int Z = Room.doorZ(Out) + Iz * Step + (Iz == 0 ? Wander : 0);
                    if (Step % 3 == 0) {
                        B.decal(X, F, Z, "up", Scp1730Builder.Blood, Zone);
                    } else if (G.empty(X, F + 1, Z)) {
                        G.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
                    }
                }
            }
        }
    }

    public void furnish(Scp1730Layout.Room Made, int Zone) {
        Box Room = new Box(Made, Zone);
        if (!Made.Type.equals("cell_block") && !Made.Type.equals("lobby")) {
            Room.paths();
        }
        switch (Scp1730RoomDressing.route(B, Room)) {
            case "lobby" -> lobby(Room);
            case "checkpoint" -> checkpoint(Room);
            case "office", "small_office" -> office(Room);
            case "cafeteria" -> cafeteria(Room);
            case "elevator" -> elevator(Room);
            case "security" -> security(Room);
            case "breaker" -> breaker(Room);
            case "storage" -> storage(Room);
            case "cell_block" -> cellBlock(Room);
            case "test_chamber" -> testChamber(Room);
            case "medical" -> medical(Room);
            case "dorms" -> dorms(Room);
            case "research" -> research(Room);
            case "heavy_cell" -> heavyCell(Room);
            case "tesla" -> tesla(Room);
            case "server" -> server(Room);
            case "generator" -> generator(Room);
            case "armoury" -> armoury(Room);
            case "pit" -> pit(Room);
            case "sanctum" -> sanctum(Room);
            case "collapsed" -> collapsed(Room);
            case "holding_cell" -> holdingCell(Room);
            default -> {
            }
        }
        if (!Made.Type.equals("sanctum")) {
            Scp1730RoomDressing.settle(B, Room);
            Room.walls();
        }
        if (Zone < 3 && !Made.Type.equals("pit") && !Made.Type.equals("tesla") && !Made.Type.equals(Scp1730Sectors.BioWing) && B.Fixtures.stream().noneMatch(Each -> Each.Type().equals("painter") && Each.Zone() == Zone)
                && Rng.nextInt(3) == 0) {
            B.fixture("painter", Room.cx(), Room.F + 1, Room.cz(), "north", 0, Zone);
        }
    }

    private String face(int Dir) {
        return Scp1730Layout.Facings[(Dir % 4 + 4) % 4];
    }

    private void lobby(Box R) {
        int Size = 5;
        int Wide = R.X1 - R.X0 + 1;
        int Deep = R.Z1 - R.Z0 + 1;
        int CountX = Math.max(2, Math.min(4, Wide / Size));
        int CountZ = Math.max(2, Math.min(4, Deep / Size));
        int[] StartX = new int[CountX];
        int[] EndX = new int[CountX];
        int[] StartZ = new int[CountZ];
        int[] EndZ = new int[CountZ];
        for (int Index = 0; Index < CountX; Index++) {
            StartX[Index] = R.X0 + Index * Size;
            EndX[Index] = Index < CountX - 1 ? StartX[Index] + Size - 2 : R.X1;
        }
        for (int Index = 0; Index < CountZ; Index++) {
            StartZ[Index] = R.Z0 + Index * Size;
            EndZ[Index] = Index < CountZ - 1 ? StartZ[Index] + Size - 2 : R.Z1;
        }
        boolean[][][] Open = new boolean[CountX][CountZ][4];
        boolean[][] Seen = new boolean[CountX][CountZ];
        java.util.ArrayDeque<int[]> Stack = new java.util.ArrayDeque<>();
        Stack.push(new int[]{Rng.nextInt(CountX), Rng.nextInt(CountZ)});
        Seen[Stack.peek()[0]][Stack.peek()[1]] = true;
        while (!Stack.isEmpty()) {
            int[] Here = Stack.peek();
            List<Integer> Ways = new ArrayList<>();
            for (int Dir = 0; Dir < 4; Dir++) {
                int Nx = Here[0] + Scp1730Layout.Dx[Dir];
                int Nz = Here[1] + Scp1730Layout.Dz[Dir];
                if (Nx >= 0 && Nz >= 0 && Nx < CountX && Nz < CountZ && !Seen[Nx][Nz]) {
                    Ways.add(Dir);
                }
            }
            if (Ways.isEmpty()) {
                Stack.pop();
                continue;
            }
            int Dir = Ways.get(Rng.nextInt(Ways.size()));
            int Nx = Here[0] + Scp1730Layout.Dx[Dir];
            int Nz = Here[1] + Scp1730Layout.Dz[Dir];
            Open[Here[0]][Here[1]][Dir] = true;
            Open[Nx][Nz][(Dir + 2) % 4] = true;
            Seen[Nx][Nz] = true;
            Stack.push(new int[]{Nx, Nz});
        }
        for (int Loop = 0; Loop < 2; Loop++) {
            int Cx = Rng.nextInt(CountX - 1);
            int Cz = Rng.nextInt(CountZ);
            Open[Cx][Cz][1] = true;
            Open[Cx + 1][Cz][3] = true;
        }
        Scp1730Layout.Door Main = R.door();
        List<int[]> DoorCells = new ArrayList<>();
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            int Ix = R.Room.doorX(Each) - Scp1730Layout.Dx[Each.Dir];
            int Iz = R.Room.doorZ(Each) - Scp1730Layout.Dz[Each.Dir];
            DoorCells.add(new int[]{cellOf(Ix, StartX, EndX), cellOf(Iz, StartZ, EndZ), Ix, Iz, Each.Dir});
        }
        int[] Blocked = null;
        for (int Cx = 0; Cx < CountX && Blocked == null; Cx++) {
            for (int Cz = 0; Cz < CountZ && Blocked == null; Cz++) {
                int Degree = 0;
                int Way = -1;
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (Open[Cx][Cz][Dir]) {
                        Degree++;
                        Way = Dir;
                    }
                }
                boolean Door = false;
                for (int[] Each : DoorCells) {
                    if (Each[0] == Cx && Each[1] == Cz) {
                        Door = true;
                    }
                }
                if (Degree == 1 && !Door && (Cx == 0 || Cz == 0 || Cx == CountX - 1 || Cz == CountZ - 1)) {
                    Blocked = new int[]{Cx, Cz, Way};
                }
            }
        }
        Scp1730Blocks.Palette Look = R.Look;
        for (int Index = 0; Index < CountX - 1; Index++) {
            int X = EndX[Index] + 1;
            for (int Z = R.Z0; Z <= R.Z1; Z++) {
                for (int Level = 1; Level <= R.H; Level++) {
                    G.set(X, R.F + Level, Z, Look.wall(Level, R.H));
                }
            }
        }
        for (int Index = 0; Index < CountZ - 1; Index++) {
            int Z = EndZ[Index] + 1;
            for (int X = R.X0; X <= R.X1; X++) {
                for (int Level = 1; Level <= R.H; Level++) {
                    G.set(X, R.F + Level, Z, Look.wall(Level, R.H));
                }
            }
        }
        for (int Cx = 0; Cx < CountX; Cx++) {
            for (int Cz = 0; Cz < CountZ; Cz++) {
                for (int Dir = 1; Dir <= 2; Dir++) {
                    if (!Open[Cx][Cz][Dir]) {
                        continue;
                    }
                    boolean Jammed = Blocked != null && (Blocked[0] == Cx && Blocked[1] == Cz && Blocked[2] == Dir
                            || Blocked[0] == Cx + Scp1730Layout.Dx[Dir] && Blocked[1] == Cz + Scp1730Layout.Dz[Dir] && Blocked[2] == (Dir + 2) % 4);
                    for (int Step = 1; Step <= 2; Step++) {
                        int X = Dir == 1 ? EndX[Cx] + 1 : StartX[Cx] + Step;
                        int Z = Dir == 2 ? EndZ[Cz] + 1 : StartZ[Cz] + Step;
                        for (int Level = 1; Level <= 2; Level++) {
                            G.set(X, R.F + Level, Z, Jammed ? Scp1730Blocks.facing(Scp1730Blocks.Shutter, Dir == 1 ? "east" : "south") : null);
                        }
                        if (Jammed) {
                            G.set(X, R.F + 3, Z, Scp1730Blocks.facing(Scp1730Blocks.Shutter, Dir == 1 ? "east" : "south"));
                            continue;
                        }
                        G.set(X, R.F, Z, Look.marked(Dir == 1 ? "east" : "south"));
                        for (int Level = 1; Level <= 2; Level++) {
                            B.keepClear(X, R.F + Level, Z);
                            B.keepClear(X - (Dir == 1 ? 1 : 0), R.F + Level, Z - (Dir == 2 ? 1 : 0));
                            B.keepClear(X + (Dir == 1 ? 1 : 0), R.F + Level, Z + (Dir == 2 ? 1 : 0));
                        }
                    }
                }
            }
        }
        for (int[] Each : DoorCells) {
            int Ix = Each[2];
            int Iz = Each[3];
            int Lx = Each[4] % 2 == 0 ? 1 : 0;
            int Lz = Each[4] % 2 == 0 ? 0 : 1;
            for (int Side = -1; Side <= 1; Side++) {
                for (int Depth = 0; Depth <= 1; Depth++) {
                    int X = Ix + Lx * Side - Scp1730Layout.Dx[Each[4]] * Depth;
                    int Z = Iz + Lz * Side - Scp1730Layout.Dz[Each[4]] * Depth;
                    for (int Level = 1; Level <= 2; Level++) {
                        G.set(X, R.F + Level, Z, null);
                        B.keepClear(X, R.F + Level, Z);
                    }
                }
            }
        }
        for (int Cx = 0; Cx < CountX; Cx++) {
            for (int Cz = 0; Cz < CountZ; Cz++) {
                int Mx = (StartX[Cx] + EndX[Cx]) / 2;
                int Mz = (StartZ[Cz] + EndZ[Cz]) / 2;
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (!Open[Cx][Cz][Dir]) {
                        continue;
                    }
                    int Gx = Dir == 1 ? EndX[Cx] : Dir == 3 ? StartX[Cx] : StartX[Cx] + 1;
                    int Gz = Dir == 2 ? EndZ[Cz] : Dir == 0 ? StartZ[Cz] : StartZ[Cz] + 1;
                    R.clearLine(Gx, Gz, Mx, Gz, 0);
                    R.clearLine(Mx, Gz, Mx, Mz, 0);
                    R.clearLine(Gx + (Dir % 2 == 0 ? 1 : 0), Gz + (Dir % 2 == 1 ? 1 : 0), Mx + 1, Gz + (Dir % 2 == 1 ? 1 : 0), 0);
                    R.clearLine(Mx + 1, Gz + (Dir % 2 == 1 ? 1 : 0), Mx + 1, Mz + 1, 0);
                }
                for (int[] Each : DoorCells) {
                    if (Each[0] == Cx && Each[1] == Cz) {
                        R.clearLine(Each[2], Each[3], Mx, Each[3], 0);
                        R.clearLine(Mx, Each[3], Mx, Mz, 0);
                        R.clearLine(Each[2] + 1, Each[3] + 1, Mx + 1, Each[3] + 1, 0);
                        R.clearLine(Mx + 1, Each[3] + 1, Mx + 1, Mz + 1, 0);
                    }
                }
            }
        }
        int MainX = DoorCells.get(0)[0];
        int MainZ = DoorCells.get(0)[1];
        int ArrivalX = (StartX[MainX] + EndX[MainX]) / 2;
        int ArrivalZ = (StartZ[MainZ] + EndZ[MainZ]) / 2;
        for (int Ox = 0; Ox <= 1; Ox++) {
            for (int Oz = 0; Oz <= 1; Oz++) {
                B.keepClear(ArrivalX + Ox, R.F + 1, ArrivalZ + Oz);
                B.keepClear(ArrivalX + Ox, R.F + 2, ArrivalZ + Oz);
            }
        }
        List<String> Themes = new ArrayList<>(List.of("reception", "queue", "nook", "defence", "office", "trail", "nook", "clutter", "queue", "clutter", "reception", "clutter",
                "defence", "clutter", "office"));
        B.shuffle(Themes);
        int Next = 0;
        int Lights = 0;
        for (int Cx = 0; Cx < CountX; Cx++) {
            for (int Cz = 0; Cz < CountZ; Cz++) {
                int X0 = StartX[Cx];
                int X1 = EndX[Cx];
                int Z0 = StartZ[Cz];
                int Z1 = EndZ[Cz];
                boolean Wing = Blocked != null && Blocked[0] == Cx && Blocked[1] == Cz;
                String Theme = Wing ? "wing" : Cx == MainX && Cz == MainZ ? "checkpoint" : Themes.get(Next++ % Themes.size());
                lobbyCell(R, Theme, X0, X1, Z0, Z1, Main);
                int Mx = (X0 + X1) / 2;
                int Mz = (Z0 + Z1) / 2;
                int Roll = Rng.nextInt(100);
                String Light = Wing ? Scp1730Blocks.DeadLight : Lights < 2 && Roll < 18 ? null : Roll < 50 ? Scp1730Blocks.BrokenLight : Roll < 68 ? Scp1730Blocks.EmergencyLight : Scp1730Blocks.DeadLight;
                if (G.empty(Mx, R.F + R.H, Mz) && G.get(Mx, R.F + R.H + 1, Mz) != null) {
                    if (Light == null) {
                        G.set(Mx, R.F + R.H, Mz, M + "fluorescent_light[face=ceiling,facing=north,lit=true]");
                        Lights++;
                    } else {
                        G.set(Mx, R.F + R.H, Mz, Scp1730Blocks.facing(Light, "down"));
                    }
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        for (int Mark = 0; Mark < 18; Mark++) {
            int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
            int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
            int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
            B.decal(Wx, R.F + 1 + Rng.nextInt(2), Wz, face(Cell[2]), B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Rust, Scp1730Builder.Grime, Scp1730Builder.Corrosion,
                    Scp1730Builder.Bullets, Scp1730Builder.Claws, Scp1730Builder.Cracks}), R.Zone);
        }
        for (int Index = 0; Index < CountX - 1; Index++) {
            for (int Mark = 0; Mark < 4; Mark++) {
                int X = EndX[Index] + 1;
                int Z = R.Z0 + Rng.nextInt(Deep);
                B.decal(X, R.F + 1 + Rng.nextInt(2), Z, Rng.nextBoolean() ? "east" : "west", B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Claws, Scp1730Builder.Bullets,
                        Scp1730Builder.Rust, Scp1730Builder.Grime}), R.Zone);
            }
        }
        R.put(R.X0, R.F + R.H, R.Z0, M + "ez_dummy_camera_dome[facing=south]");
        R.mess(14);
        float Yaw = Scp1730Builder.yawToward(ArrivalX + 0.5, ArrivalZ + 0.5, R.Room.doorX(Main) + 0.5, R.Room.doorZ(Main) + 0.5);
        B.Arrival = new Scp1730Spot(ArrivalX + 0.5, R.F + 1, ArrivalZ + 0.5, Yaw + 180.0f);
    }

    private static int cellOf(int Value, int[] Start, int[] End) {
        for (int Index = 0; Index < Start.length; Index++) {
            if (Value >= Start[Index] && Value <= End[Index] + 1) {
                return Index;
            }
        }
        return Value < Start[0] ? 0 : Start.length - 1;
    }

    private void lobbyCell(Box R, String Theme, int X0, int X1, int Z0, int Z1, Scp1730Layout.Door Main) {
        int Mx = (X0 + X1) / 2;
        int Mz = (Z0 + Z1) / 2;
        String Toward = face(Main.Dir);
        List<int[]> Free = new ArrayList<>();
        for (int X = X0; X <= X1; X++) {
            for (int Z = Z0; Z <= Z1; Z++) {
                Free.add(new int[]{X, Z});
            }
        }
        B.shuffle(Free);
        int Cursor = 0;
        switch (Theme) {
            case "checkpoint" -> {
                boolean AlongX = Main.Dir % 2 == 0;
                for (int Step = -2; Step <= 2; Step++) {
                    int X = AlongX ? Mx + Step : Mx + 1;
                    int Z = AlongX ? Mz + 1 : Mz + Step;
                    if (Math.abs(Step) == 2) {
                        R.put(X, R.F + 1, Z, M + "metal_desk[facing=" + Toward + ",left=false,right=false]");
                        R.put(X, R.F + 2, Z, M + "security_monitor[facing=" + Toward + ",lit=false,screen=0]");
                    } else {
                        R.put(X, R.F + 1, Z, M + "ez_turnstile[facing=" + Toward + "]");
                    }
                }
                int Bx = X0;
                int Bz = Z1;
                if (R.put(Bx, R.F + 1, Bz, M + "metal_desk[facing=east,left=false,right=false]")) {
                    R.put(Bx, R.F + 2, Bz, M + "security_monitor[facing=east,lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]");
                    for (int Level = 1; Level <= 2; Level++) {
                        R.put(Bx + 1, R.F + Level, Bz, Scp1730Blocks.facing(Scp1730Blocks.BrokenGlass, "east"));
                        R.put(Bx, R.F + Level, Bz - 1, Scp1730Blocks.facing(Scp1730Blocks.BrokenGlass, "north"));
                        R.put(Bx + 1, R.F + Level, Bz - 1, R.Look.Pillar);
                    }
                }
                for (int Shard = 0; Shard < 3; Shard++) {
                    int[] Cell = Free.get(Cursor++ % Free.size());
                    R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
                }
            }
            case "reception" -> {
                for (int X = X0; X <= X1; X++) {
                    if (R.put(X, R.F + 1, Z1, M + "reception_desk[facing=north,left=" + (X > X0) + ",right=" + (X < X1) + ",shape=straight]")) {
                        R.put(X, R.F + 2, Z1, X == Mx ? Scp1730Blocks.facing(Scp1730Blocks.CrackedTerminal, "north") : Scp1730Blocks.PaperStack + "[facing=north,variant=" + Rng.nextInt(6) + "]");
                    }
                }
                R.container(X0, Z0, M + "filing_cabinet", "south", 0);
                R.put(X1, R.F + 1, Z0, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))));
            }
            case "queue" -> {
                for (int Row = Z0 + 1; Row <= Z1 - 1; Row += 2) {
                    boolean FromLeft = ((Row - Z0) / 2) % 2 == 0;
                    for (int X = X0; X <= X1; X++) {
                        boolean Gap = FromLeft ? X == X1 : X == X0;
                        if (!Gap) {
                            R.put(X, R.F + 1, Row, "minecraft:iron_bars[east=true,west=true,north=false,south=false]");
                        }
                    }
                }
                R.put(X0, R.F + 1, Z1, Scp1730Blocks.facing(Scp1730Blocks.OverturnedBench, "east"));
            }
            case "nook" -> {
                int Hx = X0 + Rng.nextInt(Math.max(1, X1 - X0));
                int Hz = Z0 + Rng.nextInt(Math.max(1, Z1 - Z0));
                for (int Ox = 0; Ox <= 1; Ox++) {
                    for (int Oz = 0; Oz <= 1; Oz++) {
                        int X = Hx + Ox;
                        int Z = Hz + Oz;
                        B.ceilingHole(X, R.F + R.H, Z);
                        G.set(X, R.F, Z, Scp1730Blocks.RubbleFloor);
                        R.put(X, R.F + 1, Z, Scp1730Blocks.facing((Ox + Oz) % 2 == 0 ? Scp1730Blocks.FallenPanel : Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
                    }
                }
                for (int[] Hang : new int[][]{{Hx - 1, Hz}, {Hx + 2, Hz + 1}, {Hx, Hz - 1}}) {
                    if (G.empty(Hang[0], R.F + R.H, Hang[1]) && G.get(Hang[0], R.F + R.H + 1, Hang[1]) != null) {
                        G.set(Hang[0], R.F + R.H, Hang[1], Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.DanglingPanel : Scp1730Blocks.HangingCables, "down"));
                    }
                }
                for (int Seat = 0; Seat < 4; Seat++) {
                    int[] Cell = Free.get(Cursor++ % Free.size());
                    R.put(Cell[0], R.F + 1, Cell[1], Seat % 2 == 0 ? Scp1730Blocks.facing(Scp1730Blocks.OverturnedBench, face(Rng.nextInt(4))) : M + "ez_waiting_chairs[facing=" + face(Rng.nextInt(4)) + "]");
                }
                for (int Patch = 0; Patch < 2; Patch++) {
                    int[] Cell = Free.get(Cursor++ % Free.size());
                    R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.MouldPatch, "up"));
                    B.decal(Cell[0], R.F, Cell[1], "up", Scp1730Builder.Puddle, R.Zone);
                }
            }
            case "defence" -> {
                boolean AlongX = Main.Dir % 2 == 0;
                for (int Step = -2; Step <= 2; Step++) {
                    int X = AlongX ? Mx + Step : Mx;
                    int Z = AlongX ? Mz : Mz + Step;
                    R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Step == 0 ? Scp1730Blocks.Barricade : Scp1730Blocks.Sandbags, Toward));
                }
                int[] Cell = Free.get(Cursor++ % Free.size());
                R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Rng.nextInt(4))));
                Cell = Free.get(Cursor++ % Free.size());
                R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.AmmoCrate, face(Rng.nextInt(4))));
                for (int Shot = 0; Shot < 4; Shot++) {
                    int[] Spot = Free.get(Cursor++ % Free.size());
                    B.decal(Spot[0], R.F, Spot[1], "up", B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Blood, Scp1730Builder.Footprints}), R.Zone);
                }
            }
            case "office" -> {
                R.tall(X1, Z0, M + "locker", "facing=west,open=true");
                R.put(X1, R.F + 2, Mz, Scp1730Blocks.facing(Scp1730Blocks.WeaponRack, "west"));
                if (R.put(X0, R.F + 1, Z0, M + "metal_desk[facing=east,left=false,right=false]")) {
                    R.put(X0, R.F + 2, Z0, M + "security_monitor[facing=east,lit=false,screen=" + Rng.nextInt(10) + "]");
                }
                R.put(X0, R.F + 1, Z1, Scp1730Blocks.facing(Scp1730Blocks.CableTangle, "north"));
            }
            case "trail" -> {
                int Vx = X0;
                int Vz = Mz;
                if (Scp1730Builder.isWallLike(G.get(Vx - 1, R.F + 1, Vz)) && G.empty(Vx, R.F + 1, Vz)) {
                    G.set(Vx, R.F + 1, Vz, Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, "east"));
                    B.decal(Vx - 1, R.F + 2, Vz, "east", Scp1730Builder.Claws, R.Zone);
                }
                for (int X = Vx + 1; X <= X1; X++) {
                    if (G.empty(X, R.F + 1, Vz)) {
                        G.set(X, R.F + 1, Vz, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
                    } else {
                        B.decal(X, R.F, Vz, "up", Scp1730Builder.Blood, R.Zone);
                    }
                }
            }
            case "wing" -> {
                for (int X = X0; X <= X1; X++) {
                    R.put(X, R.F + 1, Z0, M + "steel_shelf[facing=south]");
                }
                R.put(Mx, R.F + 1, Mz, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, "east"));
                R.put(X1, R.F + 1, Z1, M + "supply_crate[facing=west,open=true]");
            }
            default -> {
            }
        }
        String[] Junk = {Scp1730Blocks.OverturnedChair, Scp1730Blocks.Rubble, Scp1730Blocks.CableTangle, Scp1730Blocks.FallenPanel, Scp1730Blocks.OverturnedDesk, Scp1730Blocks.OverturnedBench};
        int Count = Theme.equals("clutter") ? 6 : 3;
        for (int Index = 0; Index < Count; Index++) {
            int[] Cell = Free.get(Cursor++ % Free.size());
            int Roll = Rng.nextInt(10);
            if (Roll < 6) {
                R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Junk[Rng.nextInt(Junk.length)], face(Rng.nextInt(4))));
            } else if (Roll < 8) {
                R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.PaperStack + "[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]");
            } else {
                R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.GlassShards : Scp1730Blocks.MouldPatch, "up"));
            }
        }
        if (Theme.equals("clutter") && Rng.nextBoolean()) {
            R.tall(X1, Z1, M + "vending_machine", "facing=north");
        }
    }

    private void holdingCell(Box R) {
        Scp1730Layout.Door Main = R.door();
        int Into = (Main.Dir + 2) % 4;
        int Ix = Scp1730Layout.Dx[Into];
        int Iz = Scp1730Layout.Dz[Into];
        int Lx = Into % 2 == 0 ? 1 : 0;
        int Lz = Into % 2 == 0 ? 0 : 1;
        int StartX = R.Room.doorX(Main);
        int StartZ = R.Room.doorZ(Main);
        int Span0 = Ix != 0 ? R.Z0 : R.X0;
        int Span1 = Ix != 0 ? R.Z1 : R.X1;
        int Centre = (Span0 + Span1) / 2;
        int Depth = Ix != 0 ? (R.X1 - R.X0 + 1) : (R.Z1 - R.Z0 + 1);
        int WallDepth = Math.max(4, Depth / 2 - 1);
        String Toward = face(Into);
        String Back = face(Main.Dir);
        Scp1730Blocks.Palette Cell = Scp1730Blocks.Containment;
        for (int Along = Span0; Along <= Span1; Along++) {
            int X = Ix != 0 ? StartX + Ix * WallDepth : Along;
            int Z = Ix != 0 ? Along : StartZ + Iz * WallDepth;
            boolean Opening = Math.abs(Along - Centre) <= 1;
            for (int Level = 1; Level <= R.H; Level++) {
                if (Opening && Level <= 3) {
                    G.set(X, R.F + Level, Z, null);
                    B.keepClear(X, R.F + Level, Z);
                } else {
                    boolean Slit = Level == 2 && Math.abs(Along - Centre) == 3;
                    G.set(X, R.F + Level, Z, Slit ? Scp1730Blocks.Glass : Cell.wall(Level, R.H));
                }
            }
            if (Opening) {
                G.set(X, R.F, Z, Cell.marked(Toward));
            }
        }
        int Ox = Ix != 0 ? StartX + Ix * WallDepth : Centre;
        int Oz = Ix != 0 ? Centre : StartZ + Iz * WallDepth;
        B.keepClear(Ox - Ix, R.F + 1, Oz - Iz);
        B.keepClear(Ox + Ix, R.F + 1, Oz + Iz);
        int Sx = Ox + Lx * 3 - Ix;
        int Sz = Oz + Lz * 3 - Iz;
        if (G.empty(Sx, R.F + 3, Sz) && R.H >= 3) {
            G.set(Sx, R.F + 3, Sz, Scp1730Blocks.facing(new String[]{Scp1730Blocks.SignKeter, Scp1730Blocks.SignMemetic, Scp1730Blocks.SignBiohazard}[Rng.nextInt(3)], Back));
        }
        if (G.empty(Ox - Lx * 2 - Ix, R.F + 2, Oz - Lz * 2 - Iz)) {
            G.set(Ox - Lx * 2 - Ix, R.F + 2, Oz - Lz * 2 - Iz, M + "containment_room_cell_plaque[facing=" + Back + ",number=" + (1 + Rng.nextInt(16)) + "]");
        }
        if (G.empty(Ox - Ix, R.F + R.H, Oz - Iz)) {
            G.set(Ox - Ix, R.F + R.H, Oz - Iz, Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, "down"));
        }
        R.put(Ox - Ix * 2 + Lx * 2, R.F + 1, Oz - Iz * 2 + Lz * 2, Scp1730Blocks.facing(Scp1730Blocks.CellDoorWreck, face(Into + 1)));
        int Px = Ox + Ix * Math.max(2, (Depth - WallDepth) / 2);
        int Pz = Oz + Iz * Math.max(2, (Depth - WallDepth) / 2);
        int Number = B.SetPieces.next(R.Zone, Boolean.FALSE);
        String Scene = Scp1730SetPieces.scene(R.Zone, Number, B.Seed);
        B.fixture("setpiece", Px, R.F + 1, Pz, Back, Number, R.Zone);
        if (Scene.equals(Scp1730SetPieces.Generic)) {
            R.put(Px + Lx * 2, R.F + 1, Pz + Lz * 2, M + "containment_room_restraint_anchor[facing=" + Back + "]");
            for (int Level = R.H; Level >= R.H - 1; Level--) {
                if (G.empty(Px - Lx * 2, R.F + Level, Pz - Lz * 2)) {
                    G.set(Px - Lx * 2, R.F + Level, Pz - Lz * 2, Scp1730Blocks.Chain);
                }
            }
        } else {
            Scp1730CellScenes.dress(B, R, Scene, new Scp1730CellScenes.Cell(StartX, StartZ, Ix, Iz, Lx, Lz, Span0, Span1, Centre, Depth, WallDepth, Px, Pz, Toward, Back));
        }
        for (int Mark = 0; Mark < 8; Mark++) {
            B.decal(Px + Rng.nextInt(5) - 2, R.F, Pz + Rng.nextInt(5) - 2, "up", B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Scorch, Scp1730Builder.Blood, Scp1730Builder.Cracks}), R.Zone);
        }
        for (int Mark = -2; Mark <= 2; Mark++) {
            int Wx = Ox + Lx * Mark;
            int Wz = Oz + Lz * Mark;
            if (Math.abs(Mark) >= 2) {
                B.decal(Wx, R.F + 1 + Rng.nextInt(2), Wz, Back, B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Scorch}), R.Zone);
                B.decal(Wx, R.F + 1 + Rng.nextInt(2), Wz, Toward, Scp1730Builder.Claws, R.Zone);
            }
        }
        int Bodies = 2 + Rng.nextInt(2);
        int Laid = 0;
        for (int Try = 0; Try < 20 && Laid < Bodies; Try++) {
            int Step = 1 + Rng.nextInt(Math.max(1, WallDepth - 1));
            int Across = Rng.nextInt(Span1 - Span0 + 1) + Span0;
            int X = Ix != 0 ? StartX + Ix * Step : Across;
            int Z = Ix != 0 ? Across : StartZ + Iz * Step;
            if (R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Rng.nextInt(4))))) {
                B.decal(X, R.F, Z, "up", Scp1730Builder.Blood, R.Zone);
                Laid++;
            }
        }
        List<int[]> Cells = R.wallCells();
        int Holes = 0;
        for (int[] Each : Cells) {
            int Dist = Ix != 0 ? (Each[0] - StartX) * Ix : (Each[1] - StartZ) * Iz;
            if (Dist > 0 && Dist < WallDepth && Holes < 4 && Rng.nextBoolean()) {
                int Wx = Each[0] - Scp1730Layout.Dx[Each[2]];
                int Wz = Each[1] - Scp1730Layout.Dz[Each[2]];
                B.decal(Wx, R.F + 1 + Rng.nextInt(2), Wz, face(Each[2]), Scp1730Builder.Bullets, R.Zone);
                Holes++;
            }
        }
        int Cx = Ix != 0 ? StartX + Ix * 1 : (Rng.nextBoolean() ? Span0 : Span1);
        int Cz = Ix != 0 ? (Rng.nextBoolean() ? Span0 : Span1) : StartZ + Iz * 1;
        String CrateFacing = Ix != 0 ? (Cz == Span0 ? "south" : "north") : (Cx == Span0 ? "east" : "west");
        if (R.Zone > 0 && R.put(Cx, R.F + 1, Cz, M + "supply_crate[facing=" + CrateFacing + ",open=false]")) {
            B.loot(Cx, R.F + 1, Cz, CrateFacing, R.Zone >= 2 ? 2 : 1, R.Zone);
        } else {
            R.put(Cx, R.F + 1, Cz, Scp1730Blocks.facing(Scp1730Blocks.AmmoCrate, CrateFacing));
        }
        R.lights(5);
        R.breach(1, false, false);
    }

    private void checkpoint(Box R) {
        R.breach(1, false, false);
        boolean AlongZ = R.Room.D == 2;
        int Cx = R.cx();
        int Cz = R.cz();
        if (AlongZ) {
            R.clearLine(Cx, R.Z0, Cx, R.Z1, 1);
        } else {
            R.clearLine(R.X0, Cz, R.X1, Cz, 1);
        }
        String Forward = AlongZ ? "north" : "west";
        for (int Across = (AlongZ ? R.X0 : R.Z0); Across <= (AlongZ ? R.X1 : R.Z1); Across++) {
            int X = AlongZ ? Across : Cx;
            int Z = AlongZ ? Cz : Across;
            if (Math.abs(Across - (AlongZ ? Cx : Cz)) <= 1) {
                G.set(X, R.F, Z, R.Look.marked(Forward));
                continue;
            }
            if (Math.abs(Across - (AlongZ ? Cx : Cz)) <= 4) {
                R.put(X, R.F + 1, Z, M + "ez_turnstile[facing=" + Forward + "]");
                G.set(X, R.F, Z, R.Look.marked(Forward));
            } else {
                for (int Level = 1; Level <= 4; Level++) {
                    if (Level == 2 || Level == 3) {
                        R.put(X, R.F + Level, Z, Scp1730Blocks.Glass);
                    } else {
                        R.put(X, R.F + Level, Z, R.Look.wall(Level == 1 ? 1 : 3, 3));
                    }
                }
            }
        }
        int Side = Rng.nextBoolean() ? 1 : -1;
        for (int Step = 2; Step <= 4; Step++) {
            int X = AlongZ ? (Side > 0 ? R.X1 - 1 : R.X0 + 1) : Cx + Step;
            int Z = AlongZ ? Cz + Step : (Side > 0 ? R.Z1 - 1 : R.Z0 + 1);
            String Toward = AlongZ ? (Side > 0 ? "west" : "east") : (Side > 0 ? "north" : "south");
            if (R.put(X, R.F + 1, Z, M + "metal_desk[facing=" + Toward + ",left=false,right=false]")) {
                R.put(X, R.F + 2, Z, M + "security_monitor[facing=" + Toward + ",lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]");
            }
        }
        for (int Mark = -1; Mark <= 1; Mark += 2) {
            String Out = AlongZ ? (Mark > 0 ? "south" : "north") : (Mark > 0 ? "east" : "west");
            R.put(AlongZ ? Cx + 5 : Cx + Mark, R.F + 2, AlongZ ? Cz + Mark : Cz + 5, M + "keycard_scanner[facing=" + Out + ",level=2,status=idle]");
        }
        R.lights(5);
        R.mess(6);
        R.turret();
    }

    private void office(Box R) {
        R.wallContainer(M + "filing_cabinet", 0);
        R.breach(2, false, false);
        int Width = R.X1 - R.X0 + 1;
        int Depth = R.Z1 - R.Z0 + 1;
        for (int X = R.X0 + 3; X <= R.X1 - 3; X += 4) {
            for (int Z = R.Z0 + 3; Z <= R.Z1 - 3; Z += 4) {
                if (Rng.nextInt(5) == 0) {
                    R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.OverturnedDesk, face(Rng.nextInt(4))));
                    continue;
                }
                String Toward = face(Rng.nextInt(4));
                if (R.put(X, R.F + 1, Z, M + "office_desk[facing=" + Toward + ",left=false,right=false]")) {
                    int Dir = Scp1730Builder.dirOf(Toward);
                    R.put(X, R.F + 2, Z, Rng.nextBoolean() ? M + "crt_monitor[facing=" + Toward + ",lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]"
                            : M + "paper_stack[facing=" + Toward + ",variant=" + Rng.nextInt(6) + "]");
                    int Sx = X + Scp1730Layout.Dx[Dir];
                    int Sz = Z + Scp1730Layout.Dz[Dir];
                    if (Rng.nextInt(3) == 0) {
                        R.put(Sx, R.F + 1, Sz, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Dir + 1)));
                    } else {
                        R.put(Sx, R.F + 1, Sz, M + "office_chair[facing=" + face(Dir + 2) + "]");
                    }
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= Width * Depth / 30 + 3) {
                break;
            }
            String Out = face(Cell[2]);
            int Roll = Rng.nextInt(5);
            boolean Done = switch (Roll) {
                case 0, 1 -> R.put(Cell[0], R.F + 1, Cell[1], M + "filing_cabinet[facing=" + Out + ",open=" + (Rng.nextInt(3) == 0) + "]");
                case 2 -> R.put(Cell[0], R.F + 1, Cell[1], M + "binder_shelf[facing=" + Out + "]");
                case 3 -> R.tall(Cell[0], Cell[1], M + "whiteboard", "facing=" + Out + ",drawing=" + Rng.nextInt(4));
                default -> R.put(Cell[0], R.F + 1, Cell[1], M + "potted_plant[facing=" + Out + "]");
            };
            if (Done) {
                Count++;
            }
        }
        R.lights(5);
        R.mess(6 + Width * Depth / 60);
    }

    private void cafeteria(Box R) {
        R.breach(3, true, false);
        int Cx = R.cx();
        int Cz = R.cz();
        for (int Tx = R.X0 + 3; Tx + 2 <= R.X1 - 3; Tx += 6) {
            for (int Tz = R.Z0 + 4; Tz + 1 <= R.Z1 - 3; Tz += 6) {
                boolean Wrecked = Rng.nextInt(4) == 0;
                for (int Ox = 0; Ox < 3; Ox++) {
                    for (int Oz = 0; Oz < 2; Oz++) {
                        int X = Tx + Ox;
                        int Z = Tz + Oz;
                        if (Wrecked) {
                            if (Ox == 1 && Oz == 0) {
                                R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.OverturnedDesk, "east"));
                            }
                            continue;
                        }
                        String Props = "north=" + (Oz > 0) + ",south=" + (Oz < 1) + ",west=" + (Ox > 0) + ",east=" + (Ox < 2);
                        R.put(X, R.F + 1, Z, M + "cafeteria_table[" + Props + "]");
                    }
                    if (Rng.nextInt(4) > 0) {
                        R.put(Tx + Ox, R.F + 1, Tz - 1, Rng.nextInt(5) == 0 ? Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, "west") : M + "cafeteria_chair[facing=south]");
                    }
                    if (Rng.nextInt(4) > 0) {
                        R.put(Tx + Ox, R.F + 1, Tz + 2, Rng.nextInt(5) == 0 ? Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, "east") : M + "cafeteria_chair[facing=north]");
                    }
                }
            }
        }
        Scp1730Layout.Door Main = R.door();
        int Back = (Main.Dir + 2) % 4;
        List<int[]> Cells = R.wallCells();
        for (int[] Cell : Cells) {
            if (Cell[2] != Main.Dir) {
                continue;
            }
            String Out = face(Cell[2]);
            int Along = Main.Dir % 2 == 0 ? Cell[0] : Cell[1];
            if (Along % 7 == 3) {
                R.tall(Cell[0], Cell[1], M + "vending_machine", "facing=" + Out);
            } else if (R.put(Cell[0], R.F + 1, Cell[1], M + "metal_desk[facing=" + Out + ",left=true,right=true]")) {
                int Roll = Rng.nextInt(6);
                if (Roll == 0) {
                    R.put(Cell[0], R.F + 2, Cell[1], M + "microwave[facing=" + Out + ",lit=false]");
                } else if (Roll == 1) {
                    R.put(Cell[0], R.F + 2, Cell[1], M + "coffee_machine[facing=" + Out + ",lit=false]");
                }
            }
        }
        for (int Index = 0; Index < 4; Index++) {
            R.put(R.X0 + Rng.nextInt(R.X1 - R.X0 + 1), R.F + 1, R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1), M + "trash_can[facing=north]");
        }
        R.lights(6);
        R.mess(16);
    }

    private void elevator(Box R) {
        R.breach(2, true, false);
        boolean AlongZ = R.Room.D == 2;
        Scp1730Layout.Door Main = R.door();
        int WallDir;
        if (AlongZ) {
            WallDir = Main.Dir == 1 ? 3 : 1;
        } else {
            WallDir = Main.Dir == 2 ? 0 : 2;
        }
        int Into = (WallDir + 2) % 4;
        String Out = face(Into);
        int Count = 0;
        for (int Along = 4; Along <= 24 && Count < 3; Along += 9) {
            int X;
            int Z;
            if (AlongZ) {
                X = WallDir == 1 ? R.X1 + 1 : R.X0 - 1;
                Z = R.Z0 + Along;
            } else {
                X = R.X0 + Along;
                Z = WallDir == 2 ? R.Z1 + 1 : R.Z0 - 1;
            }
            int Lx = AlongZ ? 0 : 1;
            int Lz = AlongZ ? 1 : 0;
            int Ix = Scp1730Layout.Dx[Into];
            int Iz = Scp1730Layout.Dz[Into];
            if (!R.free(X + Ix, R.F + 1, Z + Iz) || !R.free(X + Ix + Lx, R.F + 1, Z + Iz + Lz)) {
                continue;
            }
            boolean Swap = Into == 0 || Into == 1;
            for (int Level = 1; Level <= 3; Level++) {
                G.set(X, R.F + Level, Z, Scp1730Blocks.facing(Swap ? Scp1730Blocks.ElevatorRight : Scp1730Blocks.ElevatorLeft, Out));
                G.set(X + Lx, R.F + Level, Z + Lz, Scp1730Blocks.facing(Swap ? Scp1730Blocks.ElevatorLeft : Scp1730Blocks.ElevatorRight, Out));
            }
            R.put(X + Ix, R.F + 4, Z + Iz, M + "facility_sign_elevator_small[facing=" + Out + "]");
            B.decal(X + Ix, R.F, Z + Iz, "up", Scp1730Builder.Scorch, R.Zone);
            Count++;
        }
        List<int[]> Cells = R.wallCells();
        for (int[] Cell : Cells) {
            if (Cell[2] == WallDir && Rng.nextInt(3) > 0) {
                R.put(Cell[0], R.F + 1, Cell[1], M + "ez_waiting_chairs[facing=" + face(Cell[2]) + "]");
            }
        }
        R.lights(5);
        R.mess(7);
    }

    private void security(Box R) {
        R.wallContainer(M + "locker", 2);
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        boolean Card = false;
        int Monitors = 0;
        for (int[] Cell : Cells) {
            String Out = face(Cell[2]);
            if (Monitors < 4 && R.put(Cell[0], R.F + 1, Cell[1], M + "metal_desk[facing=" + Out + ",left=false,right=false]")) {
                Monitors++;
                if (!Card) {
                    G.set(Cell[0], R.F + 2, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.Card, Out));
                    B.fixture("card", Cell[0], R.F + 2, Cell[1], Out, 0, R.Zone);
                    Card = true;
                } else {
                    R.put(Cell[0], R.F + 2, Cell[1], M + "security_monitor[facing=" + Out + ",lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]");
                }
                int Sx = Cell[0] + Scp1730Layout.Dx[Cell[2]];
                int Sz = Cell[1] + Scp1730Layout.Dz[Cell[2]];
                R.put(Sx, R.F + 1, Sz, M + "office_chair[facing=" + face(Cell[2] + 2) + "]");
            } else if (Monitors >= 4 && Rng.nextInt(4) == 0) {
                if (Rng.nextBoolean()) {
                    R.tall(Cell[0], Cell[1], M + "locker", "facing=" + Out + ",open=" + Rng.nextBoolean());
                } else {
                    R.put(Cell[0], R.F + 1, Cell[1], M + "filing_cabinet[facing=" + Out + ",open=true]");
                }
            }
        }
        for (int[] Cell : Cells) {
            if (R.put(Cell[0], R.F + 3, Cell[1], M + "wall_monitor[facing=" + face(Cell[2]) + ",hanging=false,lit=true,screen=" + Rng.nextInt(10) + "]")) {
                break;
            }
        }
        R.lights(5);
        R.mess(5);
    }

    private void controlPanel(Box R, String Kind, int Data, boolean Objective) {
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int[] Cell : Cells) {
            if (!R.free(Cell[0], R.F + 2, Cell[1]) || !R.free(Cell[0], R.F + 1, Cell[1])) {
                continue;
            }
            String Out = face(Cell[2]);
            G.set(Cell[0], R.F + 2, Cell[1], Scp1730Blocks.with(Scp1730Blocks.Control, "facing=" + Out + ",kind=" + Kind + ",active=false"));
            B.fixture("control", Cell[0], R.F + 2, Cell[1], Out, Data, R.Zone);
            B.keepClear(Cell[0], R.F + 1, Cell[1]);
            int Ix = Cell[0] + Scp1730Layout.Dx[Cell[2]];
            int Iz = Cell[1] + Scp1730Layout.Dz[Cell[2]];
            B.keepClear(Ix, R.F + 1, Iz);
            B.keepClear(Ix, R.F + 2, Iz);
            G.set(Ix, R.F, Iz, R.Look.marked(Out));
            if (Objective) {
                R.put(Cell[0], R.F + 3, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, Out));
            }
            return;
        }
    }

    private void breaker(Box R) {
        controlPanel(R, "breaker", 2, false);
        R.wallContainer(M + "supply_crate", 0);
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 7) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count % 4) {
                case 0 -> R.stack(Cell[0], Cell[1], M + "hcz_pipe_manifold", Out, R.H);
                case 1 -> R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.Generator, Out)) && R.put(Cell[0], R.F + 2, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.Generator, Out));
                case 2 -> R.put(Cell[0], R.F + 1, Cell[1], M + "supply_crate[facing=" + Out + ",open=" + Rng.nextBoolean() + "]");
                default -> R.put(Cell[0], R.F + 2, Cell[1], M + "hcz_valve_panel[facing=" + Out + "]");
            };
            if (Done) {
                Count++;
            }
        }
        for (int X = R.X0 + 2; X <= R.X1 - 2; X += 2) {
            R.put(X, R.F + R.H, R.cz(), Scp1730Blocks.facing(Scp1730Blocks.CableTray, "down"));
        }
        R.lights(6);
        R.mess(4);
    }

    private void storage(Box R) {
        R.wallContainer(M + "supply_crate", new int[]{0, 0, 1, 3}[R.Zone]);
        R.wallContainer(M + "supply_crate", new int[]{0, 1, 0, 1}[R.Zone]);
        String[] Shelves = R.Zone == 0 ? new String[]{"binder_shelf", "steel_shelf", "archive_boxes"} : R.Zone == 1 ? new String[]{"steel_shelf", "chemical_shelf", "specimen_shelf"}
                : new String[]{"steel_shelf", "steel_shelf", "archive_boxes"};
        for (int X = R.X0 + 2; X <= R.X1 - 2; X += 3) {
            for (int Z = R.Z0 + 2; Z <= R.Z1 - 2; Z++) {
                if (Rng.nextInt(7) == 0) {
                    continue;
                }
                R.put(X, R.F + 1, Z, M + Shelves[Rng.nextInt(Shelves.length)] + "[facing=" + (X % 2 == 0 ? "east" : "west") + "]");
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int Index = 0; Index < 6 && Index < Cells.size(); Index++) {
            int[] Cell = Cells.get(Index);
            String Out = face(Cell[2]);
            if (R.Zone == 2) {
                R.stack(Cell[0], Cell[1], M + "hcz_hazard_crate", Out, 1 + Rng.nextInt(2));
            } else {
                R.put(Cell[0], R.F + 1, Cell[1], Rng.nextBoolean() ? M + "supply_crate[facing=" + Out + ",open=" + Rng.nextBoolean() + "]" : M + "chemical_barrel[facing=" + Out + "]");
            }
        }
        R.lights(6);
        R.mess(5);
    }

    private void cellBlock(Box R) {
        Scp1730Layout.Door Main = R.door();
        boolean HallAlongX = Main.Dir % 2 == 1;
        int Cx = R.cx();
        int Cz = R.cz();
        Scp1730Blocks.Palette Cell = Scp1730Blocks.Containment;
        int Start = HallAlongX ? R.X0 : R.Z0;
        int End = HallAlongX ? R.X1 : R.Z1;
        int Lo = HallAlongX ? R.Z0 : R.X0;
        int Hi = HallAlongX ? R.Z1 : R.X1;
        int Centre = HallAlongX ? Cz : Cx;
        for (int Along = Start; Along <= End; Along++) {
            for (int Across = Lo; Across <= Hi; Across++) {
                boolean Vestibule = Along < Start + 4 || Along > End - 4;
                boolean Hall = Math.abs(Across - Centre) <= 2;
                if (Vestibule || Hall) {
                    int X = HallAlongX ? Along : Across;
                    int Z = HallAlongX ? Across : Along;
                    B.keepClear(X, R.F + 1, Z);
                    B.keepClear(X, R.F + 2, Z);
                }
            }
        }
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            if (Each.Dir % 2 == (HallAlongX ? 1 : 0)) {
                continue;
            }
            int Ix = -Scp1730Layout.Dx[Each.Dir];
            int Iz = -Scp1730Layout.Dz[Each.Dir];
            int X = R.Room.doorX(Each) + Ix;
            int Z = R.Room.doorZ(Each) + Iz;
            R.clearLine(X, Z, HallAlongX ? X : Centre, HallAlongX ? Centre : Z, 1);
        }
        int Number = 1 + Rng.nextInt(4);
        int Smashing = Number + Rng.nextInt(Math.max(1, ((End - Start - 8) / 8) * 2));
        for (int Side = -1; Side <= 1; Side += 2) {
            int WallLine = Centre + Side * 3;
            int Far = Side < 0 ? Lo : Hi;
            for (int Along = Start + 4; Along <= End - 4; Along++) {
                int X = HallAlongX ? Along : WallLine;
                int Z = HallAlongX ? WallLine : Along;
                boolean Route = B.cleared(X, R.F + 1, Z);
                for (int Level = 1; Level <= R.H; Level++) {
                    G.set(X, R.F + Level, Z, Route && Level <= 2 ? null : Cell.wall(Level, R.H));
                }
            }
            for (int Depth = WallLine; Depth != Far + Side; Depth += Side) {
                for (int Cap : new int[]{Start + 3, End - 3}) {
                    int X = HallAlongX ? Cap : Depth;
                    int Z = HallAlongX ? Depth : Cap;
                    boolean Route = B.cleared(X, R.F + 1, Z);
                    for (int Level = 1; Level <= R.H; Level++) {
                        G.set(X, R.F + Level, Z, Route && Level <= 2 ? null : Cell.wall(Level, R.H));
                    }
                }
            }
            String HallFacing = HallAlongX ? (Side < 0 ? "south" : "north") : (Side < 0 ? "east" : "west");
            String CellFacing = Scp1730Blocks.opposite(HallFacing);
            for (int Along = Start + 4; Along + 6 <= End - 4; Along += 8) {
                int Divider = Along + 7;
                if (Divider < End - 3) {
                    for (int Depth = WallLine + Side; Depth != Far + Side; Depth += Side) {
                        int X = HallAlongX ? Divider : Depth;
                        int Z = HallAlongX ? Depth : Divider;
                        for (int Level = 1; Level <= R.H; Level++) {
                            if (!B.cleared(X, R.F + 1, Z) || Level > 3) {
                                G.set(X, R.F + Level, Z, Cell.wall(Level, R.H));
                            } else {
                                G.set(X, R.F + Level, Z, null);
                            }
                        }
                    }
                }
                int Roll = Rng.nextInt(100);
                for (int Wide = 1; Wide <= 3; Wide++) {
                    int X = HallAlongX ? Along + Wide : WallLine;
                    int Z = HallAlongX ? WallLine : Along + Wide;
                    for (int Level = 1; Level <= 3; Level++) {
                        G.set(X, R.F + Level, Z, null);
                    }
                    if (Roll < 55) {
                        G.set(X, R.F + 3, Z, Scp1730Blocks.facing(Scp1730Blocks.Shutter, HallFacing));
                    } else if (Roll < 70) {
                        for (int Level = 1; Level <= 3; Level++) {
                            G.set(X, R.F + Level, Z, Scp1730Blocks.facing(Scp1730Blocks.Shutter, HallFacing));
                        }
                    } else if (Roll < 85) {
                        G.set(X, R.F + 3, Z, Scp1730Blocks.facing(Scp1730Blocks.Shutter, HallFacing));
                        if (Wide == 2) {
                            G.set(X, R.F + 2, Z, Scp1730Blocks.facing(Scp1730Blocks.Shutter, HallFacing));
                        }
                    }
                    G.set(X, R.F, Z, Cell.marked(HallFacing));
                }
                boolean Smashed = false;
                for (int Level = 2; Level <= 3; Level++) {
                    int X = HallAlongX ? Along + 5 : WallLine;
                    int Z = HallAlongX ? WallLine : Along + 5;
                    int Pane = Rng.nextInt(100);
                    G.set(X, R.F + Level, Z, Pane < 30 ? null : Pane < 75 ? Scp1730Blocks.facing(Scp1730Blocks.BrokenGlass, HallAlongX ? "north" : "east") : Scp1730Blocks.Glass);
                    Smashed |= Pane < 75;
                }
                if (Smashed) {
                    for (int Lean = -1; Lean <= 1; Lean += 2) {
                        int X = HallAlongX ? Along + 5 : WallLine + Lean;
                        int Z = HallAlongX ? WallLine + Lean : Along + 5;
                        if (G.empty(X, R.F + 1, Z)) {
                            G.set(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
                        }
                    }
                }
                if (Number == Smashing) {
                    for (int Level = 1; Level <= 2; Level++) {
                        int X = HallAlongX ? Along + 4 : WallLine;
                        int Z = HallAlongX ? WallLine : Along + 4;
                        G.set(X, R.F + Level, Z, null);
                    }
                    for (int Lean = -1; Lean <= 1; Lean += 2) {
                        int X = HallAlongX ? Along + 4 : WallLine + Lean * 1;
                        int Z = HallAlongX ? WallLine + Lean : Along + 4;
                        if (G.empty(X, R.F + 1, Z)) {
                            G.set(X, R.F + 1, Z, Scp1730Blocks.facing(Lean == -Side ? Scp1730Blocks.Rubble : Scp1730Blocks.FallenPanel, HallFacing));
                        }
                    }
                    int Hx = HallAlongX ? Along + 4 : WallLine;
                    int Hz = HallAlongX ? WallLine : Along + 4;
                    B.decal(HallAlongX ? Hx - 1 : Hx, R.F + 1, HallAlongX ? Hz : Hz - 1, HallFacing, Scp1730Builder.Claws, R.Zone);
                    B.decal(HallAlongX ? Hx + 1 : Hx, R.F + 2, HallAlongX ? Hz : Hz + 1, HallFacing, Scp1730Builder.Cracks, R.Zone);
                    B.decal(HallAlongX ? Hx : Hx + Side, R.F, HallAlongX ? Hz + Side : Hz, "up", Scp1730Builder.Cracks, R.Zone);
                }
                int Px = HallAlongX ? Along + 6 : WallLine - Side;
                int Pz = HallAlongX ? WallLine - Side : Along + 6;
                int Plx = HallAlongX ? Px : WallLine;
                int Plz = HallAlongX ? WallLine : Pz;
                if (G.empty(Px, R.F + 2, Pz) && Scp1730SupportCheck.solid(G.get(Plx, R.F + 2, Plz))) {
                    G.set(Px, R.F + 2, Pz, M + "containment_room_cell_plaque[facing=" + HallFacing + ",number=" + Math.min(16, Number) + "]");
                }
                if (Rng.nextInt(3) == 0 && G.empty(Px, R.F + 3, Pz) && Scp1730SupportCheck.solid(G.get(Plx, R.F + 3, Plz))) {
                    G.set(Px, R.F + 3, Pz, Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, HallFacing));
                }
                Number++;
                int Back = Far;
                int Bx = HallAlongX ? Along + 2 : Back;
                int Bz = HallAlongX ? Back : Along + 2;
                int Kind = Rng.nextInt(5);
                if (Kind <= 1) {
                    for (int Bunk = 0; Bunk < 2; Bunk++) {
                        int X = HallAlongX ? Along + 1 + Bunk * 4 : Back;
                        int Z = HallAlongX ? Back : Along + 1 + Bunk * 4;
                        int Ex = X - (HallAlongX ? 0 : Side);
                        int Ez = Z - (HallAlongX ? Side : 0);
                        if (R.free(X, R.F + 1, Z) && R.free(Ex, R.F + 1, Ez)) {
                            G.set(X, R.F + 1, Z, M + "lcz_cell_bunk[facing=" + HallFacing + ",pair=main]");
                            G.set(Ex, R.F + 1, Ez, M + "lcz_cell_bunk[facing=" + HallFacing + ",pair=extension]");
                        }
                    }
                } else if (Kind == 2) {
                    for (int Spot = 0; Spot < 3; Spot++) {
                        int X = HallAlongX ? Along + 1 + Spot * 2 : Back;
                        int Z = HallAlongX ? Back : Along + 1 + Spot * 2;
                        R.stack(X, Z, M + "containment_room_specimen_tank", HallFacing, 2);
                    }
                } else if (Kind == 3) {
                    for (int Spot = 0; Spot < 5; Spot++) {
                        int X = HallAlongX ? Along + 1 + Spot : Back;
                        int Z = HallAlongX ? Back : Along + 1 + Spot;
                        R.put(X, R.F + 1, Z, M + (Spot % 2 == 0 ? "specimen_shelf" : "steel_shelf") + "[facing=" + HallFacing + "]");
                    }
                } else {
                    int Mx = HallAlongX ? Along + 3 : (WallLine + Far) / 2;
                    int Mz = HallAlongX ? (WallLine + Far) / 2 : Along + 3;
                    R.put(Mx, R.F + 1, Mz, M + "containment_room_restraint_anchor[facing=" + HallFacing + "]");
                    R.put(Mx, R.F + 2, Mz, Scp1730Blocks.Chain);
                }
                int Mx = HallAlongX ? Along + 3 : (WallLine + Far) / 2;
                int Mz = HallAlongX ? (WallLine + Far) / 2 : Along + 3;
                for (int Mark = 0; Mark < 3; Mark++) {
                    int X = Mx + Rng.nextInt(5) - 2;
                    int Z = Mz + Rng.nextInt(5) - 2;
                    if (Rng.nextBoolean()) {
                        B.decal(X, R.F, Z, "up", B.pick(new int[]{Scp1730Builder.Blood, Scp1730Builder.Claws, Scp1730Builder.Scorch, Scp1730Builder.Footprints}), R.Zone);
                    } else {
                        R.put(X, R.F + 1, Z, Rng.nextBoolean() ? Scp1730Blocks.PaperStack + "[facing=north,variant=" + Rng.nextInt(6) + "]" : Scp1730Blocks.facing(Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
                    }
                }
                for (int Extra = 0; Extra < 5; Extra++) {
                    int X = Mx + Rng.nextInt(5) - 2;
                    int Z = Mz + Rng.nextInt(5) - 2;
                    String[] Junk = {Scp1730Blocks.OverturnedChair, Scp1730Blocks.MouldPatch, Scp1730Blocks.GlassShards, Scp1730Blocks.CableTangle, Scp1730Blocks.FallenPanel,
                            Scp1730Blocks.DragMarks, Scp1730Blocks.Rubble, Scp1730Blocks.PaperStack};
                    String Pick = Junk[Rng.nextInt(Junk.length)];
                    boolean Flat = Pick.equals(Scp1730Blocks.MouldPatch) || Pick.equals(Scp1730Blocks.GlassShards) || Pick.equals(Scp1730Blocks.DragMarks);
                    R.put(X, R.F + 1, Z, Pick.equals(Scp1730Blocks.PaperStack) ? Pick + "[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]"
                            : Scp1730Blocks.facing(Pick, Flat ? "up" : face(Rng.nextInt(4))));
                    B.decal(X + Rng.nextInt(3) - 1, R.F, Z + Rng.nextInt(3) - 1, "up", B.pick(new int[]{Scp1730Builder.Puddle, Scp1730Builder.Grime, Scp1730Builder.Blood, Scp1730Builder.Cracks}), R.Zone);
                }
                for (int Mark = 0; Mark < 3; Mark++) {
                    int Offset = Rng.nextInt(7);
                    int Level = R.F + 1 + Rng.nextInt(3);
                    int Pick = B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Scorch, Scp1730Builder.Rust, Scp1730Builder.Grime, Scp1730Builder.Bullets, Scp1730Builder.Corrosion,
                            Scp1730Builder.Cracks});
                    if (Mark == 0) {
                        B.decal(HallAlongX ? Along + Offset : Far + Side, Level, HallAlongX ? Far + Side : Along + Offset, HallFacing, Pick, R.Zone);
                    } else {
                        int Wall = Mark == 1 ? Along - 1 : Along + 7;
                        int Depth = WallLine + Side * (1 + Rng.nextInt(Math.max(1, Math.abs(Far - WallLine))));
                        String Into = HallAlongX ? (Mark == 1 ? "east" : "west") : (Mark == 1 ? "south" : "north");
                        B.decal(HallAlongX ? Wall : Depth, Level, HallAlongX ? Depth : Wall, Into, Pick, R.Zone);
                    }
                }
                if (Rng.nextBoolean()) {
                    int X = Mx + Rng.nextInt(3) - 1;
                    int Z = Mz + Rng.nextInt(3) - 1;
                    if (G.empty(X, R.F + R.H, Z) && G.get(X, R.F + R.H + 1, Z) != null) {
                        G.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.DanglingPanel : Scp1730Blocks.HangingCables, "down"));
                    }
                }
                int Wx = HallAlongX ? Along + 3 : Far - Side * 0;
                int Wz = HallAlongX ? Far : Along + 3;
                B.decal(HallAlongX ? Wx : Wx + Side, R.F + 2, HallAlongX ? Wz + Side : Wz, HallAlongX ? (Side < 0 ? "south" : "north") : (Side < 0 ? "east" : "west"),
                        B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Cracks, Scp1730Builder.Scorch}), R.Zone);
                ceilingLight(Mx, R.F + R.H, Mz, R.Zone, false);
            }
        }
        for (int Along = Start + 2; Along <= End; Along += 5) {
            int X = HallAlongX ? Along : Cx;
            int Z = HallAlongX ? Cz : Along;
            ceilingLight(X, R.F + R.H, Z, R.Zone, HallAlongX);
        }
        for (int Along = Start + 1; Along <= End - 1; Along++) {
            for (int Wide = -2; Wide <= 2; Wide += 4) {
                int X = HallAlongX ? Along : Centre + Wide;
                int Z = HallAlongX ? Centre + Wide : Along;
                if (G.get(X, R.F, Z) != null && Along > Start + 3 && Along < End - 3) {
                    G.set(X, R.F, Z, Cell.marked(HallAlongX ? (Wide < 0 ? "north" : "south") : (Wide < 0 ? "west" : "east")));
                }
            }
        }
        int Span = End - Start;
        int Fall = Start + 5 + Rng.nextInt(Math.max(1, Span - 11));
        int FallLane = Rng.nextBoolean() ? -1 : 1;
        for (int Step = 0; Step <= 1; Step++) {
            for (int Wide = 1; Wide <= 2; Wide++) {
                int X = HallAlongX ? Fall + Step : Centre + FallLane * Wide;
                int Z = HallAlongX ? Centre + FallLane * Wide : Fall + Step;
                B.ceilingHole(X, R.F + R.H, Z);
                B.rubbleFloor(X, R.F, Z);
                if (G.empty(X, R.F + 1, Z) && G.empty(X, R.F + 2, Z)) {
                    G.set(X, R.F + 1, Z, Scp1730Blocks.facing((Step + Wide) % 2 == 0 ? Scp1730Blocks.Rubble : Scp1730Blocks.FallenPanel, face(Rng.nextInt(4))));
                }
            }
        }
        for (int Step : new int[]{-1, 2}) {
            for (int Wide = 0; Wide <= 2; Wide++) {
                int X = HallAlongX ? Fall + Step : Centre + FallLane * Wide;
                int Z = HallAlongX ? Centre + FallLane * Wide : Fall + Step;
                if (G.empty(X, R.F + R.H, Z) && G.get(X, R.F + R.H + 1, Z) != null && Rng.nextInt(3) != 0) {
                    G.set(X, R.F + R.H, Z, Scp1730Blocks.facing((Step + Wide) % 2 == 0 ? Scp1730Blocks.DanglingPanel : Scp1730Blocks.HangingCables, "down"));
                }
            }
        }
        int Lane = Rng.nextBoolean() ? -1 : 1;
        boolean Defended = false;
        for (int Along = Start + 4; Along <= End - 4; Along += 2 + Rng.nextInt(2)) {
            if (Along >= Fall - 1 && Along <= Fall + 2) {
                continue;
            }
            Lane = -Lane;
            int Width = Rng.nextInt(3) == 0 ? 3 : 2;
            boolean Defence = !Defended && Rng.nextInt(3) == 0;
            for (int Step = 0; Step < Width; Step++) {
                int Wide = Lane * (2 - Step);
                int X = HallAlongX ? Along : Centre + Wide;
                int Z = HallAlongX ? Centre + Wide : Along;
                if (!G.empty(X, R.F + 1, Z) || !G.empty(X, R.F + 2, Z)) {
                    continue;
                }
                String[] Junk = {Scp1730Blocks.Rubble, Scp1730Blocks.FallenPanel, Scp1730Blocks.CableTangle, Scp1730Blocks.OverturnedBench, Scp1730Blocks.BodyBag, Scp1730Blocks.Gurney,
                        Scp1730Blocks.OverturnedDesk, Scp1730Blocks.Rubble, Scp1730Blocks.OverturnedChair};
                String Pick = Defence ? (Step == Width - 1 ? Scp1730Blocks.Barricade : Scp1730Blocks.Sandbags) : Junk[Rng.nextInt(Junk.length)];
                String Facing = Defence || Pick.equals(Scp1730Blocks.Gurney) ? (HallAlongX ? "east" : "south") : face(Rng.nextInt(4));
                G.set(X, R.F + 1, Z, Scp1730Blocks.facing(Pick, Facing));
                B.decal(X + (HallAlongX ? 1 : 0), R.F, Z + (HallAlongX ? 0 : 1), "up", B.pick(new int[]{Scp1730Builder.Puddle, Scp1730Builder.Blood, Scp1730Builder.Footprints, Scp1730Builder.Scorch,
                        Scp1730Builder.Grime}), R.Zone);
            }
            if (Defence) {
                Defended = true;
                int X = HallAlongX ? Along - 1 : Centre + Lane * 2;
                int Z = HallAlongX ? Centre + Lane * 2 : Along - 1;
                if (G.empty(X, R.F + 1, Z)) {
                    G.set(X, R.F + 1, Z, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.AmmoCrate : Scp1730Blocks.BodyBag, HallAlongX ? "north" : "east"));
                }
                B.decal(HallAlongX ? Along + 1 : Centre - Lane, R.F, HallAlongX ? Centre - Lane : Along + 1, "up", Scp1730Builder.Scorch, R.Zone);
            }
        }
        for (int Hang = 0; Hang < 6; Hang++) {
            int Along = Start + Rng.nextInt(Span + 1);
            int Wide = Rng.nextInt(5) - 2;
            int X = HallAlongX ? Along : Centre + Wide;
            int Z = HallAlongX ? Centre + Wide : Along;
            if (G.empty(X, R.F + R.H, Z) && G.get(X, R.F + R.H + 1, Z) != null) {
                G.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Hang % 3 == 1 ? Scp1730Blocks.DanglingPanel : Scp1730Blocks.HangingCables, "down"));
            }
        }
        for (int Mouth = 0; Mouth < 2; Mouth++) {
            for (int Item = 0; Item < 5; Item++) {
                int Along = Mouth == 0 ? Start + Rng.nextInt(4) : End - Rng.nextInt(4);
                int Across = Lo + Rng.nextInt(Hi - Lo + 1);
                int X = HallAlongX ? Along : Across;
                int Z = HallAlongX ? Across : Along;
                if (G.empty(X, R.F + 1, Z) && G.get(X, R.F, Z) != null) {
                    int Roll = Rng.nextInt(4);
                    G.set(X, R.F + 1, Z, Roll == 0 ? Scp1730Blocks.PaperStack + "[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]"
                            : Scp1730Blocks.facing(Roll == 1 ? Scp1730Blocks.GlassShards : Roll == 2 ? Scp1730Blocks.DragMarks : Scp1730Blocks.MouldPatch, "up"));
                }
                B.decal(X, R.F, Z, "up", B.pick(new int[]{Scp1730Builder.Puddle, Scp1730Builder.Grime, Scp1730Builder.Footprints, Scp1730Builder.Blood}), R.Zone);
            }
        }
        R.turret();
        R.mess(8);
    }

    private void testChamber(Box R) {
        R.breach(2, true, true);
        Scp1730Layout.Door Main = R.door();
        int Into = (Main.Dir + 2) % 4;
        int Ix = Scp1730Layout.Dx[Into];
        int Iz = Scp1730Layout.Dz[Into];
        int Lx = Into % 2 == 0 ? 1 : 0;
        int Lz = Into % 2 == 0 ? 0 : 1;
        int StartX = R.Room.doorX(Main);
        int StartZ = R.Room.doorZ(Main);
        int LineX = Ix != 0 ? StartX + Ix * 9 : 0;
        int LineZ = Iz != 0 ? StartZ + Iz * 9 : 0;
        int Span0 = Ix != 0 ? R.Z0 : R.X0;
        int Span1 = Ix != 0 ? R.Z1 : R.X1;
        int Gap = Rng.nextBoolean() ? Span0 + 2 : Span1 - 4;
        String Toward = face(Into);
        String Back = face(Main.Dir);
        for (int Along = Span0; Along <= Span1; Along++) {
            int X = Ix != 0 ? LineX : Along;
            int Z = Ix != 0 ? Along : LineZ;
            boolean Doorway = Along >= Gap && Along <= Gap + 2;
            for (int Level = 1; Level <= R.H; Level++) {
                if (Doorway && Level <= 3) {
                    G.set(X, R.F + Level, Z, null);
                    continue;
                }
                boolean Window = Level >= 2 && Level <= 4 && Along > Span0 + 1 && Along < Span1 - 1 && !(Along >= Gap - 1 && Along <= Gap + 3);
                if (Window && Rng.nextInt(12) == 0) {
                    G.set(X, R.F + Level, Z, null);
                } else {
                    G.set(X, R.F + Level, Z, Window ? Scp1730Blocks.Glass : R.Look.wall(Level, R.H));
                }
            }
            if (Doorway) {
                for (int Level = 1; Level <= 2; Level++) {
                    B.keepClear(X, R.F + Level, Z);
                    B.keepClear(X - Ix, R.F + Level, Z - Iz);
                    B.keepClear(X + Ix, R.F + Level, Z + Iz);
                }
                G.set(X, R.F, Z, R.Look.marked(Toward));
            }
        }
        for (int Along = Span0 + 2; Along <= Span1 - 2; Along++) {
            int X = (Ix != 0 ? LineX - Ix : Along);
            int Z = (Ix != 0 ? Along : LineZ - Iz);
            if (Along >= Gap - 1 && Along <= Gap + 3) {
                continue;
            }
            if (R.put(X, R.F + 1, Z, M + "lab_bench_desk[facing=" + Back + ",left=false,right=false]")) {
                if (Along % 3 == 0) {
                    R.put(X, R.F + 2, Z, M + "computer_terminal[facing=" + Back + ",lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]");
                }
                if (Rng.nextBoolean()) {
                    R.put(X - Ix, R.F + 1, Z - Iz, M + "lab_stool[facing=" + Toward + "]");
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        boolean Lever = false;
        for (int[] Cell : Cells) {
            int Depth = Ix != 0 ? (Cell[0] - StartX) * Ix : (Cell[1] - StartZ) * Iz;
            if (Depth < 1 || Depth > 6 || Cell[2] == Into || !R.free(Cell[0], R.F + 2, Cell[1])) {
                continue;
            }
            if (!Lever) {
                String Out = face(Cell[2]);
                G.set(Cell[0], R.F + 2, Cell[1], Scp1730Blocks.with(Scp1730Blocks.Control, "facing=" + Out + ",kind=release,active=false"));
                B.fixture("control", Cell[0], R.F + 2, Cell[1], Out, 1, R.Zone);
                R.put(Cell[0], R.F + 3, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, Out));
                B.keepClear(Cell[0], R.F + 1, Cell[1]);
                B.keepClear(Cell[0] + Scp1730Layout.Dx[Cell[2]], R.F + 1, Cell[1] + Scp1730Layout.Dz[Cell[2]]);
                Lever = true;
            } else if (R.tall(Cell[0], Cell[1], M + "whiteboard", "facing=" + face(Cell[2]) + ",drawing=" + Rng.nextInt(4))) {
                break;
            }
        }
        int Ccx = Ix != 0 ? LineX + Ix * 10 : R.cx();
        int Ccz = Ix != 0 ? R.cz() : LineZ + Iz * 10;
        R.put(Ccx, R.F + 1, Ccz, M + "lab_table[facing=" + Back + ",left=false,right=false]");
        R.stack(Ccx + Lx * 5, Ccz + Lz * 5, M + "containment_room_specimen_tank", Back, 2);
        R.stack(Ccx - Lx * 5, Ccz - Lz * 5, M + "containment_room_padded_panel", Back, 2);
        for (int Index = 0; Index < 6; Index++) {
            B.decal(Ccx + Rng.nextInt(9) - 4, R.F, Ccz + Rng.nextInt(9) - 4, "up", B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Blood, Scp1730Builder.Claws, Scp1730Builder.Cracks}), R.Zone);
        }
        int Vx = Ccx;
        int Vz = Ccz;
        for (int Step = 0; Step < 16 && G.empty(Vx + Ix, R.F + R.H, Vz + Iz); Step++) {
            Vx += Ix;
            Vz += Iz;
        }
        if (R.free(Vx, R.F + R.H, Vz)) {
            Scp1730Sectors.mount(G, Vx, R.F + R.H, Vz, M + "containment_room_sedation_vent[facing=" + Back + "]", Back);
        }
        B.TurretSpots.add(new int[]{Ccx + Lx * 2, R.F + R.H, Ccz + Lz * 2, R.Zone});
        R.lights(5);
        R.mess(6);
    }

    private void medical(Box R) {
        R.wallContainer(M + "medical_cabinet", 1);
        if (R.Zone >= 1) {
            R.wallContainer(M + "medical_cabinet", 1);
        }
        R.breach(1, false, true);
        List<int[]> Cells = R.wallCells();
        boolean AlongZ = R.Room.D == 2;
        int Beds = 0;
        for (int[] Cell : Cells) {
            boolean LongWall = AlongZ ? Cell[2] == 1 || Cell[2] == 3 : Cell[2] == 0 || Cell[2] == 2;
            int Along = AlongZ ? Cell[1] : Cell[0];
            if (!LongWall || Along % 4 != 2) {
                continue;
            }
            String Out = face(Cell[2]);
            int Hx = Cell[0] + Scp1730Layout.Dx[Cell[2]];
            int Hz = Cell[1] + Scp1730Layout.Dz[Cell[2]];
            if (R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Hx, R.F + 1, Hz)) {
                if (Rng.nextInt(5) == 0) {
                    R.put(Hx, R.F + 1, Hz, Scp1730Blocks.facing(Scp1730Blocks.OverturnedDesk, face(Cell[2] + 1)));
                    continue;
                }
                String Facing = face(Cell[2] + 2);
                G.set(Hx, R.F + 1, Hz, M + "hospital_bed[facing=" + Facing + ",part=foot]");
                G.set(Cell[0], R.F + 1, Cell[1], M + "hospital_bed[facing=" + Facing + ",part=head]");
                int Sx = Cell[0] + (AlongZ ? 0 : 1);
                int Sz = Cell[1] + (AlongZ ? 1 : 0);
                if (Rng.nextBoolean()) {
                    R.tall(Sx, Sz, M + "iv_stand", "facing=" + Out);
                } else {
                    R.put(Sx, R.F + 1, Sz, M + "vitals_monitor[facing=" + Out + ",lit=" + Rng.nextBoolean() + "]");
                }
                if (Rng.nextBoolean()) {
                    B.decal(Hx, R.F, Hz, "up", Scp1730Builder.Blood, R.Zone);
                }
                Beds++;
            }
        }
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 5) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count) {
                case 0 -> R.put(Cell[0], R.F + 1, Cell[1], M + "medical_cabinet[facing=" + Out + ",open=true]");
                case 1 -> R.put(Cell[0], R.F + 1, Cell[1], M + "medical_cart[facing=" + Out + "]");
                case 2 -> R.put(Cell[0], R.F + 1, Cell[1], M + "wheelchair[facing=" + face(Cell[2] + 1) + "]");
                case 3 -> R.put(Cell[0], R.F + 1, Cell[1], M + "biohazard_bin[facing=" + Out + "]");
                default -> R.put(Cell[0], R.F + 1, Cell[1], M + "medical_supply_shelf[facing=" + Out + "]");
            };
            if (Done) {
                Count++;
            }
        }
        R.lights(5);
        R.mess(6);
    }

    private void dorms(Box R) {
        R.wallContainer(M + "locker", 0);
        R.breach(2, false, true);
        List<int[]> Cells = R.wallCells();
        for (int[] Cell : Cells) {
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            if (Along % 3 != 0) {
                continue;
            }
            String Facing = face(Cell[2]);
            int Ex = Cell[0] + Scp1730Layout.Dx[Cell[2]];
            int Ez = Cell[1] + Scp1730Layout.Dz[Cell[2]];
            if (R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Ex, R.F + 1, Ez)) {
                G.set(Cell[0], R.F + 1, Cell[1], M + "lcz_cell_bunk[facing=" + Facing + ",pair=main]");
                G.set(Ex, R.F + 1, Ez, M + "lcz_cell_bunk[facing=" + Facing + ",pair=extension]");
                int Lx = Cell[2] % 2 == 0 ? 1 : 0;
                int Lz = Cell[2] % 2 == 0 ? 0 : 1;
                if (Rng.nextInt(3) == 0) {
                    R.tall(Cell[0] + Lx, Cell[1] + Lz, M + "locker", "facing=" + Facing + ",open=" + Rng.nextBoolean());
                }
            }
        }
        R.lights(5);
        R.mess(12);
    }

    private void research(Box R) {
        R.wallContainer(M + "medical_cabinet", 1);
        for (int X = R.X0 + 3; X <= R.X1 - 3; X += 4) {
            int Z = R.cz();
            for (int Wide = -1; Wide <= 1; Wide++) {
                if (R.put(X + Wide, R.F + 1, Z, M + "lab_table[facing=north,left=" + (Wide > -1) + ",right=" + (Wide < 1) + "]") && Rng.nextInt(3) == 0) {
                    R.put(X + Wide, R.F + 2, Z, M + "paper_stack[facing=north,variant=" + Rng.nextInt(6) + "]");
                }
            }
            R.put(X, R.F + 1, Z + 1, M + "lab_stool[facing=north]");
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        String[] Wall = {"chemical_shelf", "specimen_shelf", "lab_shelf", "chemical_barrel", "biohazard_bin"};
        for (int Index = 0; Index < 7 && Index < Cells.size(); Index++) {
            int[] Cell = Cells.get(Index);
            R.put(Cell[0], R.F + 1, Cell[1], M + Wall[Index % Wall.length] + "[facing=" + face(Cell[2]) + "]");
        }
        R.lights(5);
        R.mess(6);
    }

    private void heavyCell(Box R) {
        R.breach(4, true, true);
        Scp1730Layout.Door Main = R.door();
        int Into = (Main.Dir + 2) % 4;
        int Ix = Scp1730Layout.Dx[Into];
        int Iz = Scp1730Layout.Dz[Into];
        int Lx = Into % 2 == 0 ? 1 : 0;
        int Lz = Into % 2 == 0 ? 0 : 1;
        int StartX = R.Room.doorX(Main);
        int StartZ = R.Room.doorZ(Main);
        int Span0 = Ix != 0 ? R.Z0 : R.X0;
        int Span1 = Ix != 0 ? R.Z1 : R.X1;
        int Centre = Ix != 0 ? R.cz() : R.cx();
        String Toward = face(Into);
        String Back = face(Main.Dir);
        for (int Thick = 0; Thick < 2; Thick++) {
            int Line = 7 + Thick;
            for (int Along = Span0; Along <= Span1; Along++) {
                int X = Ix != 0 ? StartX + Ix * Line : Along;
                int Z = Ix != 0 ? Along : StartZ + Iz * Line;
                boolean Opening = Math.abs(Along - Centre) <= 2;
                for (int Level = 1; Level <= R.H; Level++) {
                    if (Opening && Level <= 5) {
                        G.set(X, R.F + Level, Z, null);
                        continue;
                    }
                    boolean Window = Thick == 0 && Level >= 3 && Level <= 4 && Math.abs(Along - Centre) >= 5 && Math.abs(Along - Centre) <= 10;
                    G.set(X, R.F + Level, Z, Window ? Scp1730Blocks.Glass : Scp1730Blocks.Containment.wall(Level, R.H));
                }
                if (Opening) {
                    G.set(X, R.F, Z, Scp1730Blocks.Containment.marked(Toward));
                }
            }
        }
        int Side = Rng.nextBoolean() ? 1 : -1;
        for (int Wide = 1; Wide <= 2; Wide++) {
            int Along = Centre + Side * Wide;
            for (int Level = 1; Level <= 5; Level++) {
                int X = Ix != 0 ? StartX + Ix * 8 : Along;
                int Z = Ix != 0 ? Along : StartZ + Iz * 8;
                G.set(X, R.F + Level, Z, Scp1730Blocks.facing(Scp1730Blocks.HeavyDoor, Back));
            }
        }
        int Hx = Ix != 0 ? StartX + Ix * 10 : Centre - Side * 3;
        int Hz = Ix != 0 ? Centre - Side * 3 : StartZ + Iz * 10;
        R.put(Hx, R.F + 1, Hz, Scp1730Blocks.facing(Scp1730Blocks.CellDoorWreck, Toward));
        int Plx = Ix != 0 ? StartX + Ix * 6 : Centre + 4;
        int Plz = Ix != 0 ? Centre + 4 : StartZ + Iz * 6;
        R.put(Plx, R.F + 3, Plz, M + "containment_room_cell_plaque[facing=" + Back + ",number=" + (1 + Rng.nextInt(16)) + "]");
        int Hsx = Ix != 0 ? StartX + Ix * 6 : Centre - 4;
        int Hsz = Ix != 0 ? Centre - 4 : StartZ + Iz * 6;
        R.put(Hsx, R.F + 3, Hsz, M + "containment_room_hazard_sign[facing=" + Back + ",half=left,variant=" + Rng.nextInt(4) + "]");
        if (R.free(Hsx - Lx, R.F + 2, Hsz - Lz)) {
            Scp1730Sectors.mount(G, Hsx - Lx, R.F + 2, Hsz - Lz, M + "containment_room_status_panel[facing=" + Back + ",powered=false]", Back);
        }
        int Cx = Ix != 0 ? StartX + Ix * 19 : R.cx();
        int Cz = Ix != 0 ? R.cz() : StartZ + Iz * 19;
        for (int Ox = -6; Ox <= 6; Ox += 4) {
            int X = Cx + Lx * Ox;
            int Z = Cz + Lz * Ox;
            R.put(X, R.F + 1, Z, M + "containment_room_restraint_anchor[facing=" + Back + "]");
        }
        for (int Index = 0; Index < 18; Index++) {
            int X = Cx + Rng.nextInt(19) - 9;
            int Z = Cz + Rng.nextInt(19) - 9;
            B.decal(X, R.F, Z, "up", B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Blood, Scp1730Builder.Scorch, Scp1730Builder.Cracks, Scp1730Builder.Footprints}), R.Zone);
        }
        List<int[]> Cells = R.wallCells();
        for (int[] Cell : Cells) {
            int Depth = Ix != 0 ? (Cell[0] - StartX) * Ix : (Cell[1] - StartZ) * Iz;
            if (Depth > 9 && Rng.nextInt(4) == 0) {
                int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
                int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
                B.decal(Wx, R.F + 1 + Rng.nextInt(5), Wz, face(Cell[2]), B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Cracks, Scp1730Builder.Scorch, Scp1730Builder.Blood}), R.Zone);
            }
            if (Depth > 12 && Rng.nextInt(18) == 0) {
                if (R.free(Cell[0], R.F + 6, Cell[1])) {
                    Scp1730Sectors.mount(G, Cell[0], R.F + 6, Cell[1], M + "containment_room_sedation_vent[facing=" + face(Cell[2]) + "]", face(Cell[2]));
                }
            }
            if (Depth > 0 && Depth < 6 && Rng.nextInt(6) == 0) {
                R.put(Cell[0], R.F + 3, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, face(Cell[2])));
            }
        }
        for (int Index = 0; Index < 6; Index++) {
            R.put(Cx + Rng.nextInt(15) - 7, R.F + 1, Cz + Rng.nextInt(15) - 7, Scp1730Blocks.facing(Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
        }
        for (int Ox = -6; Ox <= 6; Ox += 4) {
            int X = Cx + Lx * Ox;
            int Z = Cz + Lz * Ox;
            int Length = 2 + Rng.nextInt(R.H - 3);
            for (int Level = 0; Level < Length; Level++) {
                R.put(X, R.F + R.H - Level, Z, Scp1730Blocks.Chain);
            }
        }
        for (int Index = 0; Index < 3; Index++) {
            int Depth = 12 + Rng.nextInt(14);
            int Across = Rng.nextInt(21) - 10;
            R.stack(StartX + Ix * Depth + Lx * Across, StartZ + Iz * Depth + Lz * Across, M + "containment_room_padded_panel", Back, 1 + Rng.nextInt(2));
        }
        R.lights(6);
        B.TurretSpots.add(new int[]{StartX + Ix * 4, R.F + R.H, StartZ + Iz * 4, R.Zone});
        R.mess(5);
    }

    private void tesla(Box R) {
        boolean AlongZ = R.Room.D == 2;
        int Cx = R.cx();
        int Cz = R.cz();
        String Front = AlongZ ? "north" : "east";
        for (int Across = (AlongZ ? R.X0 : R.Z0); Across <= (AlongZ ? R.X1 : R.Z1); Across++) {
            int X = AlongZ ? Across : Cx;
            int Z = AlongZ ? Cz : Across;
            int Offset = Across - (AlongZ ? Cx : Cz);
            boolean Gate = Offset == -1 || Offset == 0;
            for (int Level = 1; Level <= R.H; Level++) {
                if (Gate && Level <= 3) {
                    continue;
                }
                G.set(X, R.F + Level, Z, Scp1730Blocks.Hcz.wall(Level, R.H));
            }
        }
        for (int Part = 0; Part < 6; Part++) {
            int Column = Part % 2 - 1;
            int Row = Part / 2;
            int X = AlongZ ? Cx + Column : Cx;
            int Z = AlongZ ? Cz : Cz + Column;
            G.set(X, R.F + 1 + Row, Z, M + "tesla_gate[facing=" + Front + ",glow=0,part=" + Part + "]");
        }
        B.fixture("tesla", Cx, R.F + 1, Cz, Front, 0, R.Zone);
        for (int Along = -3; Along <= 3; Along++) {
            if (Along == 0) {
                continue;
            }
            for (int Wide = -2; Wide <= 2; Wide++) {
                int X = AlongZ ? Cx + Wide : Cx + Along;
                int Z = AlongZ ? Cz + Along : Cz + Wide;
                G.set(X, R.F, Z, R.Look.marked(AlongZ ? (Along < 0 ? "north" : "south") : (Along < 0 ? "west" : "east")));
            }
        }
        for (int Mark = -1; Mark <= 1; Mark += 2) {
            String Out = AlongZ ? (Mark < 0 ? "north" : "south") : (Mark < 0 ? "west" : "east");
            R.put(AlongZ ? Cx + 3 : Cx + Mark, R.F + 3, AlongZ ? Cz + Mark : Cz + 3, M + "containment_room_hazard_sign[facing=" + Out + ",half=left,variant=" + Rng.nextInt(4) + "]");
        }
        R.lights(5);
        R.mess(5);
        for (int Index = 0; Index < 4; Index++) {
            B.decal(Cx + Rng.nextInt(5) - 2, R.F, Cz + Rng.nextInt(5) - 2, "up", Scp1730Builder.Scorch, R.Zone);
        }
    }

    private void server(Box R) {
        R.breach(2, true, false);
        Scp1730Layout.Door Main = R.door();
        boolean RowsAlongX = Main.Dir % 2 == 0;
        int Count = 0;
        for (int Row = (RowsAlongX ? R.Z0 : R.X0) + 4; Row <= (RowsAlongX ? R.Z1 : R.X1) - 3; Row += 4) {
            String Facing = Count % 2 == 0 ? (RowsAlongX ? "south" : "east") : (RowsAlongX ? "north" : "west");
            for (int Along = (RowsAlongX ? R.X0 : R.Z0) + 2; Along <= (RowsAlongX ? R.X1 : R.Z1) - 2; Along++) {
                int X = RowsAlongX ? Along : Row;
                int Z = RowsAlongX ? Row : Along;
                if (Rng.nextInt(9) == 0) {
                    R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, Facing));
                    continue;
                }
                if (R.tall(X, Z, M + "server_rack", "facing=" + Facing)) {
                    if (R.H >= 4) {
                        G.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Scp1730Blocks.CableTray, "down"));
                    }
                }
            }
            Count++;
        }
        for (int Along = (RowsAlongX ? R.X0 : R.Z0) + 3; Along <= (RowsAlongX ? R.X1 : R.Z1); Along += 6) {
            for (int Row = (RowsAlongX ? R.Z0 : R.X0) + 2; Row <= (RowsAlongX ? R.Z1 : R.X1); Row += 4) {
                int X = RowsAlongX ? Along : Row;
                int Z = RowsAlongX ? Row : Along;
                ceilingLight(X, R.F + R.H, Z, R.Zone, RowsAlongX);
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int Index = 0; Index < 3 && Index < Cells.size(); Index++) {
            int[] Cell = Cells.get(Index);
            R.stack(Cell[0], Cell[1], M + "hcz_monitor_console", face(Cell[2]), 1);
        }
        for (int Index = 0; Index < 10; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0), "up", B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Oil, Scp1730Builder.Footprints}), R.Zone);
        }
        R.turret();
        R.mess(4);
    }

    private void generator(Box R) {
        R.breach(2, false, true);
        if (R.Zone == 2) {
            controlPanel(R, "breaker", 2, true);
        }
        Scp1730Layout.Door Main = R.door();
        boolean AlongX = Main.Dir % 2 == 0;
        int Units = 0;
        for (int Unit = -1; Unit <= 1; Unit += 2) {
            int Ux = AlongX ? R.cx() + Unit * 7 : R.cx();
            int Uz = AlongX ? R.cz() : R.cz() + Unit * 7;
            int Ax = AlongX ? 1 : 2;
            int Az = AlongX ? 2 : 1;
            boolean Room = true;
            for (int Ox = -Ax; Ox <= Ax; Ox++) {
                for (int Oz = -Az; Oz <= Az; Oz++) {
                    if (!R.free(Ux + Ox, R.F + 1, Uz + Oz)) {
                        Room = false;
                    }
                }
            }
            if (!Room) {
                continue;
            }
            for (int Ox = -Ax; Ox <= Ax; Ox++) {
                for (int Oz = -Az; Oz <= Az; Oz++) {
                    String Out = Math.abs(Ox) == Ax ? (Ox > 0 ? "east" : "west") : Oz > 0 ? "south" : "north";
                    int Tall = 4;
                    for (int Level = 1; Level <= Tall; Level++) {
                        G.set(Ux + Ox, R.F + Level, Uz + Oz, Level == Tall ? Scp1730Blocks.Hcz.Floor : Scp1730Blocks.facing(Scp1730Blocks.Generator, Out));
                    }
                }
            }
            for (int Level = 5; Level <= R.H; Level++) {
                G.set(Ux, R.F + Level, Uz, M + "hcz_pipe_manifold[facing=north,part=" + (Level == 5 ? "bottom" : Level == R.H ? "top" : "middle") + "]");
            }
            Units++;
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 9) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count % 3) {
                case 0 -> R.stack(Cell[0], Cell[1], M + "hcz_pipe_manifold", Out, R.H);
                case 1 -> R.stack(Cell[0], Cell[1], M + "hcz_steam_vent", Out, 2);
                default -> R.put(Cell[0], R.F + 2, Cell[1], M + "hcz_valve_panel[facing=" + Out + "]");
            };
            if (Done) {
                Count++;
            }
        }
        for (int Index = 0; Index < 4; Index++) {
            int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
            R.put(Cell[0], R.F + 4, Cell[1], M + "hcz_caged_lamp[facing=" + face(Cell[2]) + ",part=single]");
        }
        R.lights(7);
        R.turret();
        R.mess(6);
    }

    private void armoury(Box R) {
        R.wallContainer(M + "locker", 2);
        R.wallContainer(M + "locker", 2);
        if (R.Zone >= 2) {
            R.container(R.cx() - 2, R.cz(), M + "supply_crate", "east", 3);
        }
        R.breach(2, false, true);
        List<int[]> Cells = R.wallCells();
        int Index = 0;
        for (int[] Cell : Cells) {
            String Out = face(Cell[2]);
            int Roll = Index++ % 5;
            switch (Roll) {
                case 0, 1 -> R.tall(Cell[0], Cell[1], M + "locker", "facing=" + Out + ",open=" + (Rng.nextInt(3) == 0));
                case 2 -> R.put(Cell[0], R.F + 2, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.WeaponRack, Out));
                case 3 -> R.put(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.AmmoCrate, Out));
                default -> R.stack(Cell[0], Cell[1], M + "hcz_hazard_crate", Out, 1 + Rng.nextInt(2));
            }
        }
        R.put(R.cx(), R.F + 1, R.cz(), M + "metal_desk[facing=north,left=false,right=false]");
        R.put(R.cx(), R.F + 2, R.cz(), M + "paper_stack[facing=north,variant=" + Rng.nextInt(6) + "]");
        R.put(R.cx() + 2, R.F + 1, R.cz(), M + "supply_crate[facing=west,open=true]");
        R.lights(5);
        R.turret();
        R.mess(5);
    }

    private void pit(Box R) {
        int Cx = R.cx();
        int Cz = R.cz();
        int Half = 7 + Rng.nextInt(3);
        int HalfZ = 7 + Rng.nextInt(3);
        int MinX = Cx - Half + 1;
        int MaxX = Cx + Half;
        int MinZ = Cz - HalfZ + 1;
        int MaxZ = Cz + HalfZ;
        int Bottom = R.F - Scp1730Builder.PitDepth;
        for (int X = MinX - 3; X <= MaxX + 3; X++) {
            for (int Z = MinZ - 3; Z <= MaxZ + 3; Z++) {
                boolean Hole = X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ;
                int Ring = Math.max(Math.max(MinX - X, X - MaxX), Math.max(MinZ - Z, Z - MaxZ));
                if (Hole) {
                    G.set(X, R.F, Z, null);
                    for (int Level = 1; Level <= 3; Level++) {
                        B.keepClear(X, R.F + Level, Z);
                    }
                    for (int Y = Bottom + 1; Y < R.F; Y++) {
                        G.set(X, Y, Z, null);
                    }
                    G.set(X, Bottom, Z, Scp1730Blocks.PitVoid);
                } else if (Ring == 1) {
                    G.set(X, R.F, Z, (X + Z) % 2 == 0 ? Scp1730Blocks.RedGlow : Scp1730Blocks.RedRim);
                    for (int Y = Bottom; Y < R.F; Y++) {
                        G.set(X, Y, Z, Y >= R.F - 1 ? Scp1730Blocks.RedGlow : Y >= R.F - 6 ? Scp1730Blocks.Hcz.WallMiddle : Scp1730Blocks.PitVoid);
                    }
                } else if (Ring <= 3) {
                    G.set(X, R.F, Z, Scp1730Blocks.RedRim);
                }
            }
        }
        B.Pits.add(new Scp1730Pit(MinX, MinZ, MaxX, MaxZ, R.F + 1, Bottom, R.Zone));
        for (int X = MinX - 1; X <= MaxX + 1; X++) {
            for (int Z = MinZ - 1; Z <= MaxZ + 1; Z++) {
                int Ring = Math.max(Math.max(MinX - X, X - MaxX), Math.max(MinZ - Z, Z - MaxZ));
                if (Ring != 1 || Rng.nextInt(10) < 6) {
                    continue;
                }
                boolean EdgeX = Z == MinZ - 1 || Z == MaxZ + 1;
                boolean Corner = (X == MinX - 1 || X == MaxX + 1) && EdgeX;
                if (Corner || B.cleared(X, R.F + 1, Z)) {
                    continue;
                }
                G.set(X, R.F + 1, Z, EdgeX ? "minecraft:iron_bars[east=true,west=true,north=false,south=false]" : "minecraft:iron_bars[north=true,south=true,east=false,west=false]");
            }
        }
        for (int Index = 0; Index < 5; Index++) {
            int X = MinX + 1 + Rng.nextInt(MaxX - MinX - 1);
            int Z = MinZ + 1 + Rng.nextInt(MaxZ - MinZ - 1);
            int Length = 3 + Rng.nextInt(8);
            for (int Level = 0; Level < Length; Level++) {
                int Y = R.F + R.H - Level;
                if (G.empty(X, Y, Z)) {
                    G.set(X, Y, Z, Scp1730Blocks.Chain);
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Lights = 0;
        for (int[] Cell : Cells) {
            if (Lights < 6 && R.put(Cell[0], R.F + 3, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, face(Cell[2])))) {
                Lights++;
            }
        }
        for (int Index = 0; Index < 4; Index++) {
            int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
            R.put(Cell[0], R.F + 1, Cell[1], Rng.nextBoolean() ? M + "chemical_barrel[facing=" + face(Cell[2]) + "]" : Scp1730Blocks.facing(Scp1730Blocks.Rubble, face(Cell[2])));
        }
        for (int Index = 0; Index < 8; Index++) {
            int X = MinX - 3 + Rng.nextInt(MaxX - MinX + 7);
            int Z = MinZ - 3 + Rng.nextInt(MaxZ - MinZ + 7);
            if (G.get(X, R.F, Z) != null && G.get(X, R.F, Z).equals(Scp1730Blocks.RedRim) && G.empty(X, R.F + 1, Z)) {
                G.set(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
            }
        }
        for (int X = R.X0 + 3; X <= R.X1; X += 8) {
            for (int Z = R.Z0 + 3; Z <= R.Z1; Z += 8) {
                if (X < MinX - 1 || X > MaxX + 1 || Z < MinZ - 1 || Z > MaxZ + 1) {
                    ceilingLight(X, R.F + R.H, Z, R.Zone, false);
                }
            }
        }
        R.turret();
    }

    private void collapsed(Box R) {
        R.wallContainer(M + "supply_crate", R.Zone >= 2 ? 3 : 0);
        R.breach(4, true, false);
        for (int Index = 0; Index < 14; Index++) {
            int X = R.X0 + Rng.nextInt(R.X1 - R.X0 + 1);
            int Z = R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1);
            R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
        }
        for (int Index = 0; Index < 5; Index++) {
            int X = R.X0 + 1 + Rng.nextInt(R.X1 - R.X0 - 1);
            int Z = R.Z0 + 1 + Rng.nextInt(R.Z1 - R.Z0 - 1);
            int Length = 1 + Rng.nextInt(3);
            for (int Level = 0; Level < Length; Level++) {
                R.put(X, R.F + R.H - Level, Z, Scp1730Blocks.Chain);
            }
        }
        for (int Index = 0; Index < 4; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0), "up", B.pick(new int[]{Scp1730Builder.Cracks, Scp1730Builder.Oil, Scp1730Builder.Scorch}), R.Zone);
        }
        R.lights(6);
        R.mess(4);
    }

    private void sanctum(Box R) {
        Scp1730Layout.Room Made = R.Room;
        Scp1730Layout.Door Main = R.door();
        boolean LongZ = Made.D > Made.W;
        boolean DoorAtStart = LongZ ? Main.Sz == Made.Sz : Main.Sx == Made.Sx;
        int Into = LongZ ? (DoorAtStart ? 2 : 0) : (DoorAtStart ? 1 : 3);
        int Ix = Scp1730Layout.Dx[Into];
        int Iz = Scp1730Layout.Dz[Into];
        int Lx = Into % 2 == 0 ? 1 : 0;
        int Lz = Into % 2 == 0 ? 0 : 1;
        int Start = LongZ ? (DoorAtStart ? Made.z0() : Made.z1()) : (DoorAtStart ? Made.x0() : Made.x1());
        int DividerLine = Start + (LongZ ? Iz : Ix) * 15;
        int Span0 = LongZ ? R.X0 : R.Z0;
        int Span1 = LongZ ? R.X1 : R.Z1;
        int Mid = (Span0 + Span1) / 2;
        int F = R.F;
        int Ceiling = F + R.H + 1;
        int AnteTop = F + 10;
        for (int Along = Span0 - 1; Along <= Span1 + 1; Along++) {
            for (int Level = 1; Level <= R.H; Level++) {
                int X = LongZ ? Along : DividerLine;
                int Z = LongZ ? DividerLine : Along;
                boolean Arch = Math.abs(Along - Mid) <= 3 && Level <= 9 && !(Math.abs(Along - Mid) == 3 && Level >= 8);
                G.set(X, F + Level, Z, Arch ? null : Level % 6 == 4 ? Scp1730Blocks.facing(Scp1730Blocks.ChamberGlyph, face(Into)) : Scp1730Blocks.ChamberStone);
            }
        }
        for (int Depth = 1; Depth <= 14; Depth++) {
            for (int Along = Span0; Along <= Span1; Along++) {
                int X = LongZ ? Along : Start + Ix * Depth;
                int Z = LongZ ? Start + Iz * Depth : Along;
                G.set(X, AnteTop, Z, Scp1730Blocks.ChamberStone);
                G.set(X, F, Z, Math.abs(Along - Mid) <= 1 ? Scp1730Blocks.BronzeInlay : Scp1730Blocks.ChamberFloor);
            }
        }
        for (int Along = Span0 + 4; Along <= Span1 - 4; Along += 8) {
            for (int Depth = 4; Depth <= 10; Depth += 6) {
                int X = LongZ ? Along : Start + Ix * Depth;
                int Z = LongZ ? Start + Iz * Depth : Along;
                if (Math.abs(Along - Mid) <= 3) {
                    continue;
                }
                if (Depth == 4) {
                    for (int Level = 1; Level < 10; Level++) {
                        G.set(X, F + Level, Z, Level == 5 ? Scp1730Blocks.facing(Scp1730Blocks.ChamberGlyph, face(Into + 1)) : Scp1730Blocks.ChamberStone);
                    }
                    G.set(X - Lx, F + 5, Z - Lz, Scp1730Blocks.facing(Scp1730Blocks.EmergencyLight, Lx == 1 ? "west" : "north"));
                } else {
                    for (int Level = 0; Level < 3 + Rng.nextInt(4); Level++) {
                        R.put(X, AnteTop - 1 - Level, Z, Scp1730Blocks.Chain);
                    }
                }
            }
        }
        for (int Index = 0; Index < 8; Index++) {
            int Depth = 2 + Rng.nextInt(11);
            int Along = Span0 + Rng.nextInt(Span1 - Span0 + 1);
            int X = LongZ ? Along : Start + Ix * Depth;
            int Z = LongZ ? Start + Iz * Depth : Along;
            R.put(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, face(Rng.nextInt(4))));
        }
        int AnteMid = 7;
        int CacheX = LongZ ? Span0 + 1 : Start + Ix * 12;
        int CacheZ = LongZ ? Start + Iz * 12 : Span0 + 1;
        if (G.empty(CacheX, F + 1, CacheZ)) {
            String CacheFacing = LongZ ? "east" : "south";
            G.set(CacheX, F + 1, CacheZ, M + "supply_crate[facing=" + CacheFacing + ",open=false]");
            B.loot(CacheX, F + 1, CacheZ, CacheFacing, 3, 3);
            int Bx = CacheX + (LongZ ? 1 : 0);
            int Bz = CacheZ + (LongZ ? 0 : 1);
            if (G.empty(Bx, F + 1, Bz)) {
                G.set(Bx, F + 1, Bz, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, LongZ ? "north" : "east"));
            }
        }
        B.TurretSpots.add(new int[]{LongZ ? Mid - 8 : Start + Ix * AnteMid, AnteTop - 1, LongZ ? Start + Iz * AnteMid : Mid - 8, 3});
        B.TurretSpots.add(new int[]{LongZ ? Mid + 8 : Start + Ix * AnteMid, AnteTop - 1, LongZ ? Start + Iz * AnteMid : Mid + 8, 3});
        int ChamberStart = DividerLine;
        int ChamberEnd = LongZ ? (DoorAtStart ? Made.z1() : Made.z0()) : (DoorAtStart ? Made.x1() : Made.x0());
        int Ccx;
        int Ccz;
        int Far = 12;
        if (LongZ) {
            Ccx = Mid;
            Ccz = ChamberEnd - Iz * Far;
        } else {
            Ccx = ChamberEnd - Ix * Far;
            Ccz = Mid;
        }
        int ChamberCentreX = LongZ ? Mid : (ChamberStart + ChamberEnd) / 2;
        int ChamberCentreZ = LongZ ? (ChamberStart + ChamberEnd) / 2 : Mid;
        for (int Depth = 1; Depth <= 46; Depth++) {
            for (int Along = Span0; Along <= Span1; Along++) {
                int X = LongZ ? Along : ChamberStart + Ix * Depth;
                int Z = LongZ ? ChamberStart + Iz * Depth : Along;
                double Dist = Math.hypot(X - Ccx, Z - Ccz);
                boolean Ring = Math.abs(Dist - 11.0) < 0.75 || Math.abs(Dist - 13.5) < 0.6;
                boolean Spoke = Math.abs(Along - Mid) <= 1 && Dist > 13.5;
                G.set(X, F, Z, Ring || Spoke ? Scp1730Blocks.BronzeInlay : Scp1730Blocks.ChamberFloor);
                G.set(X, Ceiling, Z, Scp1730Blocks.ChamberStone);
            }
        }
        for (int Ox = -5; Ox <= 5; Ox++) {
            for (int Oz = -5; Oz <= 5; Oz++) {
                G.set(Ccx + Ox, F + 1, Ccz + Oz, Math.abs(Ox) == 5 || Math.abs(Oz) == 5 ? Scp1730Blocks.ChamberGlyph + "[facing=" + (Math.abs(Ox) == 5 ? (Ox > 0 ? "east" : "west") : (Oz > 0 ? "south" : "north")) + "]" : Scp1730Blocks.ChamberStone);
                if (Math.abs(Ox) <= 3 && Math.abs(Oz) <= 3) {
                    G.set(Ccx + Ox, F + 2, Ccz + Oz, Math.abs(Ox) <= 1 && Math.abs(Oz) <= 1 ? Scp1730Blocks.BronzeInlay : Scp1730Blocks.ChamberFloor);
                }
            }
        }
        int ChamberMin = Math.min(ChamberStart, ChamberEnd);
        int ChamberMax = Math.max(ChamberStart, ChamberEnd);
        int[][] WallRuns = LongZ ? new int[][]{{Span0 - 1, 1}, {Span1 + 1, 3}} : new int[][]{{Span0 - 1, 2}, {Span1 + 1, 0}};
        for (int[] Run : WallRuns) {
            for (int Depth = ChamberMin + 1; Depth < ChamberMax; Depth++) {
                int Wx = LongZ ? Run[0] : Depth;
                int Wz = LongZ ? Depth : Run[0];
                int Ix0 = Wx + Scp1730Layout.Dx[Run[1]];
                int Iz0 = Wz + Scp1730Layout.Dz[Run[1]];
                for (int Level : new int[]{4, 11, 18}) {
                    if ((Depth + Level) % 2 == 0) {
                        G.set(Wx, F + Level, Wz, Scp1730Blocks.facing(Scp1730Blocks.ChamberGlyph, face(Run[1])));
                    }
                }
                int Phase = (Depth - ChamberMin) % 8;
                if (Phase == 3 || Phase == 4) {
                    for (int Level = 1; Level < R.H; Level++) {
                        G.set(Ix0, F + Level, Iz0, Level % 7 == 0 ? Scp1730Blocks.facing(Scp1730Blocks.ChamberGlyph, face(Run[1])) : Scp1730Blocks.ChamberStone);
                    }
                    if (Phase == 3) {
                        int Ox = Ix0 + Scp1730Layout.Dx[Run[1]];
                        int Oz = Iz0 + Scp1730Layout.Dz[Run[1]];
                        for (int Level : new int[]{7, 15}) {
                            if (G.empty(Ox, F + Level, Oz)) {
                                G.set(Ox, F + Level, Oz, Scp1730Blocks.facing(Rng.nextInt(4) == 0 ? Scp1730Blocks.BrokenLight : Scp1730Blocks.EmergencyLight, face(Run[1])));
                            }
                        }
                    }
                }
            }
        }
        for (int Along = Span0; Along <= Span1; Along++) {
            int Wx = LongZ ? Along : ChamberEnd;
            int Wz = LongZ ? ChamberEnd : Along;
            for (int Level : new int[]{4, 11, 18}) {
                if ((Along + Level) % 2 == 0) {
                    G.set(Wx, F + Level, Wz, Scp1730Blocks.facing(Scp1730Blocks.ChamberGlyph, face(Into + 2)));
                }
            }
        }
        for (int Ox = -5; Ox <= 5; Ox += 10) {
            for (int Oz = -5; Oz <= 5; Oz += 10) {
                for (int Y = Ceiling - 1; Y >= F + 2; Y--) {
                    if (G.empty(Ccx + Ox, Y, Ccz + Oz)) {
                        G.set(Ccx + Ox, Y, Ccz + Oz, Scp1730Blocks.Chain);
                    }
                }
                G.set(Ccx + Ox * 3 / 5, F + 3, Ccz + Oz * 3 / 5, "minecraft:soul_lantern[hanging=false]");
            }
        }
        String Face = face(Into + 2);
        G.set(Ccx, F + 3, Ccz, Scp1730Blocks.facing(Scp1730Blocks.Thresher, Face));
        B.fixture("thresher", Ccx, F + 3, Ccz, Face, 0, 3);
        for (int Ox = -2; Ox <= 2; Ox++) {
            for (int Oz = -2; Oz <= 2; Oz++) {
                for (int Level = 3; Level <= 11; Level++) {
                    if (Ox == 0 && Oz == 0 && Level == 3) {
                        continue;
                    }
                    B.keepClear(Ccx + Ox, F + Level, Ccz + Oz);
                }
            }
        }
        int Gx = LongZ ? Mid : Ccx - Ix * 14;
        int Gz = LongZ ? Ccz - Iz * 14 : Mid;
        B.fixture("guardian", Gx, F + 1, Gz, Face, 0, 3);
        for (int Index = 0; Index < 8; Index++) {
            double Angle = Math.PI * 2 * Index / 8 + Math.PI / 8;
            int Px = (int) Math.round(ChamberCentreX + Math.cos(Angle) * 17);
            int Pz = (int) Math.round(ChamberCentreZ + Math.sin(Angle) * 17);
            if (Math.hypot(Px - Ccx, Pz - Ccz) < 9) {
                continue;
            }
            for (int Ox = 0; Ox <= 1; Ox++) {
                for (int Oz = 0; Oz <= 1; Oz++) {
                    for (int Level = 1; Level <= R.H; Level++) {
                        boolean Glyph = Level % 7 == 4;
                        String Out = Ox == 0 ? "west" : "east";
                        G.set(Px + Ox, F + Level, Pz + Oz, Glyph ? Scp1730Blocks.facing(Scp1730Blocks.ChamberGlyph, (Oz == 0 ? "north" : "south")) : Scp1730Blocks.ChamberStone);
                    }
                }
            }
            String[] Sides = {"north", "east", "south", "west"};
            int[][] Offsets = {{0, -1}, {2, 0}, {1, 2}, {-1, 1}};
            int Pick = Rng.nextInt(4);
            int Lxp = Px + Offsets[Pick][0];
            int Lzp = Pz + Offsets[Pick][1];
            if (G.empty(Lxp, F + 6, Lzp)) {
                G.set(Lxp, F + 6, Lzp, Scp1730Blocks.facing(Rng.nextInt(3) == 0 ? Scp1730Blocks.BrokenLight : Scp1730Blocks.EmergencyLight, Sides[Pick]));
            }
        }
        for (int Index = 0; Index < 26; Index++) {
            int X = (LongZ ? Span0 : Math.min(ChamberStart, ChamberEnd)) + 2 + Rng.nextInt(43);
            int Z = (LongZ ? Math.min(ChamberStart, ChamberEnd) : Span0) + 2 + Rng.nextInt(43);
            if (!G.empty(X, Ceiling - 1, Z) || B.cleared(X, Ceiling - 1, Z)) {
                continue;
            }
            int Length = 6 + Rng.nextInt(10);
            boolean Lantern = Rng.nextInt(3) > 0;
            for (int Level = 1; Level <= Length; Level++) {
                int Y = Ceiling - Level;
                if (!G.empty(X, Y, Z) || B.cleared(X, Y, Z) || Y <= F + 4) {
                    break;
                }
                G.set(X, Y, Z, Level == Length && Lantern ? "minecraft:soul_lantern[hanging=true]" : Scp1730Blocks.Chain);
            }
        }
        int ChamberLow = LongZ ? Math.min(ChamberStart, ChamberEnd) : Math.min(ChamberStart, ChamberEnd);
        int ChamberHigh = LongZ ? Math.max(ChamberStart, ChamberEnd) : Math.max(ChamberStart, ChamberEnd);
        int Symbols = 0;
        for (int Level : new int[]{5, 6}) {
            for (int Along = 2; Along <= 44; Along += 3) {
                int[][] Spots;
                if (LongZ) {
                    Spots = new int[][]{{Span0, ChamberLow + Along, 1}, {Span1, ChamberLow + Along, 3}, {Span0 + Along, ChamberEnd - Iz, Into + 2}};
                } else {
                    Spots = new int[][]{{ChamberLow + Along, Span0, 2}, {ChamberLow + Along, Span1, 0}, {ChamberEnd - Ix, Span0 + Along, Into + 2}};
                }
                for (int[] Spot : Spots) {
                    if ((Along / 3 + Level) % 2 == 1) {
                        continue;
                    }
                    if (Spot[0] < Math.min(R.X0, R.X1) || Spot[1] < Math.min(R.Z0, R.Z1)) {
                        continue;
                    }
                    if (!G.empty(Spot[0], F + Level, Spot[1]) || B.cleared(Spot[0], F + Level, Spot[1])) {
                        continue;
                    }
                    int Depth = LongZ ? (Spot[1] - ChamberStart) * Iz : (Spot[0] - ChamberStart) * Ix;
                    if (Depth < 2) {
                        continue;
                    }
                    int Glyph = Rng.nextInt(12);
                    String Out = face(Spot[2]);
                    G.set(Spot[0], F + Level, Spot[1], Scp1730Blocks.with(Scp1730Blocks.Symbol, "facing=" + Out + ",glyph=" + Glyph));
                    B.fixture("symbol", Spot[0], F + Level, Spot[1], Out, Glyph, 3);
                    Symbols++;
                }
            }
        }
        for (int Index = 0; Index < 4; Index++) {
            int Along = Span0 + 6 + Rng.nextInt(Span1 - Span0 - 12);
            if (Math.abs(Along - Mid) <= 4) {
                continue;
            }
            int X = LongZ ? Along : Start + Ix;
            int Z = LongZ ? Start + Iz : Along;
            int Glyph = Rng.nextInt(12);
            String Out = face(Into);
            if (G.empty(X, F + 3, Z) && !B.cleared(X, F + 3, Z) && Scp1730SupportCheck.solid(G.get(X - Ix, F + 3, Z - Iz))) {
                G.set(X, F + 3, Z, Scp1730Blocks.with(Scp1730Blocks.Symbol, "facing=" + Out + ",glyph=" + Glyph));
                B.fixture("symbol", X, F + 3, Z, Out, Glyph, 3);
            }
        }
    }

    public void descentDressing(Scp1730Builder.StairFrame Frame, int Fl, int Fu) {
        for (int Y = Fl + 8; Y < Fu - 4; Y += 7) {
            for (int U = 6; U <= 9; U += 3) {
                int V = 12 + Rng.nextInt(10);
                int X = Frame.x(U, V);
                int Z = Frame.z(U, V);
                if (G.empty(X, Y, Z)) {
                    G.set(X, Y, Z, Scp1730Blocks.Chain);
                }
            }
        }
    }
}
