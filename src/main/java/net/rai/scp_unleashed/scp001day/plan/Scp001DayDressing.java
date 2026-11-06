package net.rai.scp_unleashed.scp001day.plan;

public final class Scp001DayDressing {
    public static final int FloorProp = 0;
    public static final int AnyAir = 1;
    public static final int CeilingProp = 2;
    public static final int Overwrite = 3;
    public static final int Surface = 4;
    public static final int SaltRoom = 31;
    public static final int SaltStreetProps = 32;
    public static final int SaltSewerProps = 33;
    public static final double StainOdds = 0.035;
    public static final double CampOdds = 0.12;
    public static final double CarOdds = 0.6;
    public static final double BusOdds = 0.15;
    public static final double ShelterOdds = 0.3;
    public static final double CrashOdds = 0.35;
    public static final int PipeHangers = 6;

    private Scp001DayDressing() {
    }

    @FunctionalInterface
    public interface Sink {
        void put(int X, int Y, int Z, int Code, int Mode);
    }

    public record Extra(int X, int Y, int Z, int Kind, int Facing, int Salt) {
    }

    @FunctionalInterface
    public interface ExtraSink {
        void accept(Extra Item);
    }

    public static int[][] columns(int ChunkX, int ChunkZ) {
        int[][] Columns = new int[256][Scp001DayPlan.Height];
        int MinX = ChunkX * 16;
        int MinZ = ChunkZ * 16;
        for (int Lx = 0; Lx < 16; Lx++) {
            for (int Lz = 0; Lz < 16; Lz++) {
                Scp001DayPlan.column(MinX + Lx, MinZ + Lz, Columns[Lx * 16 + Lz]);
            }
        }
        dress(ChunkX, ChunkZ, (X, Y, Z, Code, Mode) -> apply(Columns[(X - MinX) * 16 + (Z - MinZ)], Y, Code, Mode), Item -> {
        });
        return Columns;
    }

    public static boolean ground(int Code) {
        return Code == Scp001DayCode.Road || Code == Scp001DayCode.Sidewalk || Code == Scp001DayCode.Plaza || Code == Scp001DayCode.FloorSlab;
    }

    public static void apply(int[] Column, int Y, int Code, int Mode) {
        if (Mode == Surface) {
            if (Y >= 1 && Column[Y] == Scp001DayCode.Air && ground(Column[Y - 1])) {
                Column[Y - 1] = Code;
            }
            return;
        }
        if (Mode != Overwrite && Column[Y] != Scp001DayCode.Air) {
            return;
        }
        if (Mode == FloorProp && (Y < 1 || Column[Y - 1] == Scp001DayCode.Air || Column[Y - 1] == Scp001DayCode.Water)) {
            return;
        }
        if (Mode == CeilingProp && (Y + 1 >= Column.length || Column[Y + 1] == Scp001DayCode.Air)) {
            return;
        }
        Column[Y] = Code;
    }

    public static void dress(int ChunkX, int ChunkZ, Sink Out, ExtraSink Extras) {
        int MinX = ChunkX * 16;
        int MinZ = ChunkZ * 16;
        int MaxX = MinX + 15;
        int MaxZ = MinZ + 15;
        Sink Clipped = (X, Y, Z, Code, Mode) -> {
            if (X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ && Y > 0 && Y < Scp001DayPlan.Height) {
                Out.put(X, Y, Z, Code, Mode);
            }
        };
        ExtraSink ClippedExtras = Item -> {
            if (Item.X() >= MinX && Item.X() <= MaxX && Item.Z() >= MinZ && Item.Z() <= MaxZ) {
                Extras.accept(Item);
            }
        };
        int CellLowX = Scp001DayPlan.cellOf(MinX - 16);
        int CellHighX = Scp001DayPlan.cellOf(MaxX + 16);
        int CellLowZ = Scp001DayPlan.cellOf(MinZ - 16);
        int CellHighZ = Scp001DayPlan.cellOf(MaxZ + 16);
        for (int CellX = CellLowX; CellX <= CellHighX; CellX++) {
            for (int CellZ = CellLowZ; CellZ <= CellHighZ; CellZ++) {
                Scp001DayPlan.Tower Here = Scp001DayPlan.tower(CellX, CellZ);
                if (Here.MaxX() >= MinX && Here.MinX() <= MaxX && Here.MaxZ() >= MinZ && Here.MinZ() <= MaxZ) {
                    tower(Here, MinX, MinZ, MaxX, MaxZ, Clipped, ClippedExtras);
                }
                street(CellX, CellZ, true, Clipped, ClippedExtras);
                street(CellX, CellZ, false, Clipped, ClippedExtras);
                junction(CellX, CellZ, Clipped);
            }
        }
    }

    private static void tower(Scp001DayPlan.Tower Here, int MinX, int MinZ, int MaxX, int MaxZ, Sink Raw, ExtraSink Extras) {
        Sink Out = (X, Y, Z, Code, Mode) -> {
            int Lx = X - Here.CoreX();
            int Lz = Z - Here.CoreZ();
            if (Lx >= -1 && Lx <= Scp001DayPlan.CoreWidth && Lz >= -1 && Lz <= Scp001DayPlan.CoreLength) {
                return;
            }
            if (X <= Here.MinX() || X >= Here.MaxX() || Z <= Here.MinZ() || Z >= Here.MaxZ()) {
                return;
            }
            Raw.put(X, Y, Z, Code, Mode);
        };
        if (Scp001DayHash.chance(Here.CellX(), Here.CellZ(), 0, SaltStreetProps + 5, 0.55)) {
            int Middle = (Here.MinZ() + Here.MaxZ()) / 2;
            int Side = Scp001DayHash.chance(Here.CellX(), Here.CellZ(), 1, SaltStreetProps + 5, 0.5) ? 3 : -3;
            Extras.accept(new Extra(Here.MinX() - 1, Scp001DayPlan.StreetY + 4, Middle + Side, 1, 3, Scp001DayHash.range(Here.CellX(), Here.CellZ(), 2, SaltStreetProps + 5, 0, 63)));
        }
        int RoomsX = Math.max(1, (Here.MaxX() - Here.MinX()) / Scp001DayPlan.RoomGrid + 1);
        int RoomsZ = Math.max(1, (Here.MaxZ() - Here.MinZ()) / Scp001DayPlan.RoomGrid + 1);
        for (int Floor = 0; Floor < Here.Floors(); Floor++) {
            int Base = Here.floorBase(Floor);
            if (Base + Scp001DayPlan.FloorHeight > Here.solidTop()) {
                break;
            }
            for (int Rx = 0; Rx < RoomsX; Rx++) {
                for (int Rz = 0; Rz < RoomsZ; Rz++) {
                    int X0 = Here.MinX() + 1 + Rx * Scp001DayPlan.RoomGrid;
                    int Z0 = Here.MinZ() + 1 + Rz * Scp001DayPlan.RoomGrid;
                    int X1 = Math.min(Here.MaxX() - 1, X0 + Scp001DayPlan.RoomGrid - 2);
                    int Z1 = Math.min(Here.MaxZ() - 1, Z0 + Scp001DayPlan.RoomGrid - 2);
                    if (X1 < MinX - 1 || X0 > MaxX + 1 || Z1 < MinZ - 1 || Z0 > MaxZ + 1 || X1 - X0 < 3 || Z1 - Z0 < 3) {
                        continue;
                    }
                    room(Here, Floor, Base, Rx, Rz, X0, Z0, X1, Z1, Out, Extras);
                }
            }
        }
    }

    private static int salt(Scp001DayPlan.Tower Here, int Floor, int Rx, int Rz) {
        return Here.CellX() * 73 + Here.CellZ() * 151 + Floor * 7 + Rx * 3 + Rz * 5;
    }

    private static void room(Scp001DayPlan.Tower Here, int Floor, int Base, int Rx, int Rz, int X0, int Z0, int X1, int Z1, Sink Out, ExtraSink Extras) {
        int Salt = salt(Here, Floor, Rx, Rz);
        int Y = Base + 1;
        double Roll = Scp001DayHash.unit(Salt, Floor, Rx * 31 + Rz, SaltRoom);
        int Kind = Floor == 0 ? 3 : (Roll < 0.45 ? 0 : (Roll < 0.7 ? 1 : (Roll < 0.82 && Here.Offices() ? 2 : 3)));
        if (Kind == 0) {
            int Desks = Scp001DayHash.range(Salt, 1, 0, SaltRoom, 1, 3);
            for (int Index = 0; Index < Desks; Index++) {
                int Dx = X0 + 1 + Scp001DayHash.range(Salt, 2, Index, SaltRoom, 0, Math.max(0, X1 - X0 - 3));
                int Dz = Z0 + 1 + Index * 2 + Scp001DayHash.range(Salt, 3, Index, SaltRoom, 0, 1);
                if (Dz > Z1 - 1) {
                    break;
                }
                int Facing = Scp001DayHash.range(Salt, 4, Index, SaltRoom, 0, 3);
                Out.put(Dx, Y, Dz, Scp001DayCode.Desk + Facing, FloorProp);
                int[] Behind = offset(Facing, -1);
                if (Scp001DayHash.chance(Salt, 5, Index, SaltRoom, 0.8)) {
                    boolean Tipped = Scp001DayHash.chance(Salt, 6, Index, SaltRoom, 0.25);
                    Out.put(Dx + Behind[0], Y, Dz + Behind[1], (Tipped ? Scp001DayCode.TippedChair : Scp001DayCode.Chair) + Facing, FloorProp);
                }
                if (Scp001DayHash.chance(Salt, 7, Index, SaltRoom, 0.4)) {
                    Out.put(Dx, Y + 1, Dz, Scp001DayCode.Papers + Scp001DayHash.range(Salt, 8, Index, SaltRoom, 0, 3), FloorProp);
                }
            }
            int Cabinets = Scp001DayHash.range(Salt, 9, 0, SaltRoom, 0, 2);
            for (int Index = 0; Index < Cabinets; Index++) {
                int Cx = X0 + Scp001DayHash.range(Salt, 10, Index, SaltRoom, 0, X1 - X0);
                Out.put(Cx, Y, Z0, Scp001DayCode.Cabinet + 2, FloorProp);
            }
            if (Scp001DayHash.chance(Salt, 11, 0, SaltRoom, 0.3)) {
                Out.put(X1, Y, Z1, Scp001DayCode.Plant, FloorProp);
            }
            if (Scp001DayHash.chance(Salt, 12, 0, SaltRoom, 0.15)) {
                Out.put(X1, Y, Z0, Scp001DayCode.Copier + 0, FloorProp);
            }
        } else if (Kind == 1) {
            int Litter = Scp001DayHash.range(Salt, 13, 0, SaltRoom, 2, 6);
            for (int Index = 0; Index < Litter; Index++) {
                int Px = X0 + Scp001DayHash.range(Salt, 14, Index, SaltRoom, 0, X1 - X0);
                int Pz = Z0 + Scp001DayHash.range(Salt, 15, Index, SaltRoom, 0, Z1 - Z0);
                int What = Scp001DayHash.range(Salt, 16, Index, SaltRoom, 0, 3);
                int Facing = Scp001DayHash.range(Salt, 17, Index, SaltRoom, 0, 3);
                int Code = What == 0 ? Scp001DayCode.TippedChair + Facing : (What == 1 ? Scp001DayCode.Papers + Facing : (What == 2 ? Scp001DayCode.PaperDrift : Scp001DayCode.Cabinet + Facing));
                Out.put(Px, Y, Pz, Code, FloorProp);
            }
            int Tiles = Scp001DayHash.range(Salt, 18, 0, SaltRoom, 1, 3);
            for (int Index = 0; Index < Tiles; Index++) {
                int Tx = X0 + 1 + Scp001DayHash.range(Salt, 19, Index, SaltRoom, 0, Math.max(0, X1 - X0 - 2));
                int Tz = Z0 + 1 + Scp001DayHash.range(Salt, 20, Index, SaltRoom, 0, Math.max(0, Z1 - Z0 - 2));
                Out.put(Tx, Y, Tz, Scp001DayCode.FallenTile, FloorProp);
            }
        } else if (Kind == 2) {
            barricade(Here, Base, X0, Z0, X1, Z1, Salt, Out);
            Out.put(X0 + 1, Y, Z0 + 1, Scp001DayCode.Bedroll, FloorProp);
            Out.put(X0 + 2, Y, Z0 + 1, Scp001DayCode.Bedroll, FloorProp);
            Out.put(X0 + 1, Y, Z0 + 3, Scp001DayCode.EmptyTin, FloorProp);
            Out.put(X0 + 3, Y, Z0 + 2, Scp001DayCode.EmptyTin, FloorProp);
            Extras.accept(new Extra(X0 + 1, Y, Z0 + 2, 2, 0, Salt));
        }
        for (int X = X0; X <= X1; X++) {
            for (int Z = Z0; Z <= Z1; Z++) {
                boolean Window = X == Here.MinX() + 1 || X == Here.MaxX() - 1 || Z == Here.MinZ() + 1 || Z == Here.MaxZ() - 1;
                double Mark = Scp001DayHash.unit(X, Y, Z, SaltRoom + 1);
                if (Window && Mark < StainOdds) {
                    Out.put(X, Y, Z, Mark < StainOdds * 0.5 ? Scp001DayCode.StainDark : Scp001DayCode.Stain, Surface);
                    if (Scp001DayHash.chance(X, Y, Z, SaltRoom + 2, 0.18)) {
                        int Facing = X == Here.MinX() + 1 ? 1 : (X == Here.MaxX() - 1 ? 3 : (Z == Here.MinZ() + 1 ? 2 : 0));
                        Extras.accept(new Extra(X, Y, Z, 0, Facing, Scp001DayHash.range(X, Y, Z, SaltRoom + 3, 0, 3)));
                    }
                } else if (Mark > 1.0 - 0.012) {
                    Out.put(X, Y, Z, Scp001DayCode.PaperDrift, FloorProp);
                }
            }
        }
    }

    private static void barricade(Scp001DayPlan.Tower Here, int Base, int X0, int Z0, int X1, int Z1, int Salt, Sink Out) {
        int Y = Base + 1;
        int[] Doors = {X0 + 3, X0 + 4};
        for (int Index = 0; Index < Doors.length; Index++) {
            for (int Z : new int[]{Z0, Z1}) {
                Out.put(Doors[Index], Y, Z, Scp001DayCode.Cabinet + (Z == Z0 ? 2 : 0), FloorProp);
                Out.put(Doors[Index], Y + 1, Z, Scp001DayCode.Desk + (Z == Z0 ? 2 : 0), FloorProp);
            }
        }
        int[] Sides = {Z0 + 3, Z0 + 4};
        for (int Index = 0; Index < Sides.length; Index++) {
            for (int X : new int[]{X0, X1}) {
                Out.put(X, Y, Sides[Index], Scp001DayCode.Cabinet + (X == X0 ? 1 : 3), FloorProp);
                Out.put(X, Y + 1, Sides[Index], Scp001DayCode.Desk + (X == X0 ? 1 : 3), FloorProp);
            }
        }
    }

    private static int[] offset(int Facing, int Distance) {
        return switch (Facing) {
            case 0 -> new int[]{0, -Distance};
            case 1 -> new int[]{Distance, 0};
            case 2 -> new int[]{0, Distance};
            default -> new int[]{-Distance, 0};
        };
    }

    private static void street(int CellX, int CellZ, boolean AlongZ, Sink Out, ExtraSink Extras) {
        int OriginX = CellX * Scp001DayPlan.Cell;
        int OriginZ = CellZ * Scp001DayPlan.Cell;
        int Salt = CellX * 977 + CellZ * 331 + (AlongZ ? 1 : 2);
        int Y = Scp001DayPlan.StreetY + 1;
        int Cars = Scp001DayHash.chance(Salt, 0, 0, SaltStreetProps, CarOdds) ? Scp001DayHash.range(Salt, 1, 0, SaltStreetProps, 1, 3) : 0;
        for (int Index = 0; Index < Cars; Index++) {
            int Lane = Scp001DayHash.chance(Salt, 2, Index, SaltStreetProps, 0.5) ? 3 : 6;
            int Along = Scp001DayPlan.Street + 2 + Index * 16 + Scp001DayHash.range(Salt, 3, Index, SaltStreetProps, 0, 8);
            if (Along + 5 >= Scp001DayPlan.Cell - 1 || Math.abs(Along - Scp001DayPlan.ManholeAt) < 6) {
                continue;
            }
            int Colour = Scp001DayHash.range(Salt, 4, Index, SaltStreetProps, 0, Scp001DayCode.CarColours - 1);
            vehicle(OriginX, OriginZ, AlongZ, Lane, Along, 3, 5, Scp001DayCode.CarBody + Colour, Scp001DayCode.CarBody + Colour, Y, Out);
            if (Scp001DayHash.chance(Salt, 5, Index, SaltStreetProps, CrashOdds)) {
                int Other = Scp001DayHash.range(Salt, 6, Index, SaltStreetProps, 0, Scp001DayCode.CarColours - 1);
                crossVehicle(OriginX, OriginZ, AlongZ, Along + 5, Lane == 3 ? 4 : 2, Scp001DayCode.CarBody + Other, Y, Out);
            }
        }
        if (Scp001DayHash.chance(Salt, 7, 0, SaltStreetProps, BusOdds)) {
            int Along = Scp001DayPlan.Street + 40 + Scp001DayHash.range(Salt, 8, 0, SaltStreetProps, 0, 4);
            if (Math.abs(Along + 4 - Scp001DayPlan.ManholeAt) > 8) {
                vehicle(OriginX, OriginZ, AlongZ, 6, Along, 3, 9, Scp001DayCode.BusBody, Scp001DayCode.BusTrim, Y, Out);
            }
        }
        boolean Sheltered = Scp001DayHash.chance(Salt, 9, 0, SaltStreetProps, ShelterOdds);
        int ShelterAt = Scp001DayPlan.Street + 20 + Scp001DayHash.range(Salt, 10, 0, SaltStreetProps, 0, 8);
        for (int Along = Scp001DayPlan.Street + 4; Along < Scp001DayPlan.Cell; Along += 16) {
            lamp(OriginX, OriginZ, AlongZ, 1, Along, Y, true, Out);
            int Far = Along + 8 < Scp001DayPlan.Cell ? Along + 8 : Along;
            if (!Sheltered || Far < ShelterAt - 1 || Far > ShelterAt + 4) {
                lamp(OriginX, OriginZ, AlongZ, Scp001DayPlan.Street - 2, Far, Y, false, Out);
            }
        }
        if (Sheltered) {
            shelter(OriginX, OriginZ, AlongZ, ShelterAt, Y, Out);
        }
        int Trails = Scp001DayHash.range(Salt, 11, 0, SaltStreetProps, 0, 3);
        for (int Index = 0; Index < Trails; Index++) {
            int Along = Scp001DayPlan.Street + Scp001DayHash.range(Salt, 12, Index, SaltStreetProps, 2, Scp001DayPlan.Cell - Scp001DayPlan.Street - 3);
            int Start = Scp001DayHash.range(Salt, 13, Index, SaltStreetProps, 4, 7);
            int Toward = Scp001DayHash.chance(Salt, 14, Index, SaltStreetProps, 0.5) ? 1 : -1;
            for (int Step = 0; Step < 6; Step++) {
                int Across = Start + Toward * Step;
                if (Across < 2 || Across > Scp001DayPlan.Street - 3) {
                    break;
                }
                int Wobble = Scp001DayHash.range(Salt, 15 + Step, Index, SaltStreetProps, -1, 1);
                put(OriginX, OriginZ, AlongZ, Across, Along + Wobble, Y, Step % 2 == 0 ? Scp001DayCode.Stain : Scp001DayCode.StainDark, Surface, Out);
            }
        }
        int Drifts = Scp001DayHash.range(Salt, 22, 0, SaltStreetProps, 3, 9);
        for (int Index = 0; Index < Drifts; Index++) {
            int Across = Scp001DayHash.range(Salt, 23, Index, SaltStreetProps, 2, Scp001DayPlan.Street - 3);
            int Along = Scp001DayPlan.Street + Scp001DayHash.range(Salt, 24, Index, SaltStreetProps, 0, Scp001DayPlan.Cell - Scp001DayPlan.Street - 1);
            int What = Scp001DayHash.range(Salt, 25, Index, SaltStreetProps, 0, 3);
            if (What == 3) {
                put(OriginX, OriginZ, AlongZ, Across, Along, Y, Scp001DayCode.PaperDrift, FloorProp, Out);
            } else {
                put(OriginX, OriginZ, AlongZ, Across, Along, Y, What == 0 ? Scp001DayCode.Residue : (What == 1 ? Scp001DayCode.StainDark : Scp001DayCode.Stain), Surface, Out);
            }
        }
        sewer(OriginX, OriginZ, AlongZ, Salt, Out);
    }

    private static void put(int OriginX, int OriginZ, boolean AlongZ, int Across, int Along, int Y, int Code, int Mode, Sink Out) {
        if (AlongZ) {
            Out.put(OriginX + Across, Y, OriginZ + Along, Code, Mode);
        } else {
            Out.put(OriginX + Along, Y, OriginZ + Across, Code, Mode);
        }
    }

    private static void vehicle(int OriginX, int OriginZ, boolean AlongZ, int Lane, int Along, int Width, int Length, int Body, int Trim, int Y, Sink Out) {
        for (int A = 0; A < Length; A++) {
            for (int W = 0; W < Width; W++) {
                boolean Corner = (A == 1 || A == Length - 2) && (W == 0 || W == Width - 1);
                put(OriginX, OriginZ, AlongZ, Lane + W, Along + A, Y, Corner ? Scp001DayCode.Wheel : Trim, AnyAir, Out);
                boolean End = A == 0 || A == Length - 1;
                put(OriginX, OriginZ, AlongZ, Lane + W, Along + A, Y + 1, End ? Body : Scp001DayCode.CarGlass, AnyAir, Out);
                if (!End) {
                    put(OriginX, OriginZ, AlongZ, Lane + W, Along + A, Y + 2, Body, AnyAir, Out);
                }
            }
        }
    }

    private static void crossVehicle(int OriginX, int OriginZ, boolean AlongZ, int Along, int Lane, int Body, int Y, Sink Out) {
        for (int A = 0; A < 5; A++) {
            for (int W = 0; W < 3; W++) {
                int Across = Lane + A;
                if (Across < 1 || Across > Scp001DayPlan.Street - 2) {
                    continue;
                }
                boolean Corner = (A == 1 || A == 3) && (W == 0 || W == 2);
                put(OriginX, OriginZ, AlongZ, Across, Along + W, Y, Corner ? Scp001DayCode.Wheel : Body, AnyAir, Out);
                boolean End = A == 0 || A == 4;
                put(OriginX, OriginZ, AlongZ, Across, Along + W, Y + 1, End ? Body : Scp001DayCode.CarGlass, AnyAir, Out);
                if (!End) {
                    put(OriginX, OriginZ, AlongZ, Across, Along + W, Y + 2, Body, AnyAir, Out);
                }
            }
        }
    }

    private static void lamp(int OriginX, int OriginZ, boolean AlongZ, int Across, int Along, int Y, boolean Inward, Sink Out) {
        for (int Height = 0; Height <= 4; Height++) {
            put(OriginX, OriginZ, AlongZ, Across, Along, Y + Height, Scp001DayCode.LampPole, Overwrite, Out);
        }
        int Reach = Inward ? 1 : -1;
        put(OriginX, OriginZ, AlongZ, Across, Along, Y + 5, Scp001DayCode.LampArm, Overwrite, Out);
        put(OriginX, OriginZ, AlongZ, Across + Reach, Along, Y + 5, Scp001DayCode.LampArm, Overwrite, Out);
        put(OriginX, OriginZ, AlongZ, Across + Reach * 2, Along, Y + 5, Scp001DayCode.LampHead, Overwrite, Out);
    }

    private static void shelter(int OriginX, int OriginZ, boolean AlongZ, int Along, int Y, Sink Out) {
        int Back = Scp001DayPlan.Street - 1;
        int Front = Scp001DayPlan.Street - 2;
        for (int A = 0; A < 4; A++) {
            for (int Height = 0; Height < 3; Height++) {
                put(OriginX, OriginZ, AlongZ, Back, Along + A, Y + Height, A == 0 || A == 3 ? Scp001DayCode.ShelterPost : Scp001DayCode.ShelterGlass, Overwrite, Out);
            }
            put(OriginX, OriginZ, AlongZ, Back, Along + A, Y + 3, Scp001DayCode.ShelterRoof, Overwrite, Out);
            put(OriginX, OriginZ, AlongZ, Front, Along + A, Y + 3, Scp001DayCode.ShelterRoof, Overwrite, Out);
            if (A == 1 || A == 2) {
                put(OriginX, OriginZ, AlongZ, Front, Along + A, Y, Scp001DayCode.Bench + (AlongZ ? 1 : 2), Overwrite, Out);
            }
        }
        for (int Height = 0; Height < 3; Height++) {
            put(OriginX, OriginZ, AlongZ, Front, Along, Y + Height, Scp001DayCode.ShelterPost, Overwrite, Out);
            put(OriginX, OriginZ, AlongZ, Front, Along + 3, Y + Height, Scp001DayCode.ShelterPost, Overwrite, Out);
        }
    }

    private static void sewer(int OriginX, int OriginZ, boolean AlongZ, int Salt, Sink Out) {
        int PipeY = Scp001DayPlan.SewerCeiling - 2;
        int Pipe = AlongZ ? Scp001DayCode.PipeZ : Scp001DayCode.PipeX;
        for (int Along = Scp001DayPlan.Street; Along < Scp001DayPlan.Cell; Along++) {
            if (Along == Scp001DayPlan.ManholeAt) {
                continue;
            }
            put(OriginX, OriginZ, AlongZ, Scp001DayPlan.TunnelHigh - 1, Along, PipeY, Pipe, AnyAir, Out);
            if (Math.floorMod(Along, PipeHangers) == PipeHangers / 2) {
                put(OriginX, OriginZ, AlongZ, Scp001DayPlan.TunnelHigh - 1, Along, PipeY + 1, Scp001DayCode.PipeY, AnyAir, Out);
            }
            if (Math.floorMod(Along, 16) == 8) {
                put(OriginX, OriginZ, AlongZ, Scp001DayPlan.TunnelHigh - 1, Along, PipeY - 1, Scp001DayCode.Valve, AnyAir, Out);
            }
            if (Math.floorMod(Along + Salt, 20) == 0) {
                for (int Across = Scp001DayPlan.ChannelLow; Across <= Scp001DayPlan.ChannelHigh; Across++) {
                    put(OriginX, OriginZ, AlongZ, Across, Along, Scp001DayPlan.SewerWalk, Scp001DayCode.Grate, AnyAir, Out);
                }
            }
        }
    }

    private static void junction(int CellX, int CellZ, Sink Out) {
        int Salt = CellX * 541 + CellZ * 113;
        if (!Scp001DayHash.chance(Salt, 0, 0, SaltSewerProps, CampOdds)) {
            return;
        }
        int X = CellX * Scp001DayPlan.Cell + 1;
        int Z = CellZ * Scp001DayPlan.Cell + 1;
        int Y = Scp001DayPlan.SewerWalk + 1;
        Out.put(X + 1, Y, Z, Scp001DayCode.Bedroll, FloorProp);
        Out.put(X + 2, Y, Z, Scp001DayCode.Bedroll, FloorProp);
        Out.put(X, Y, Z + 2, Scp001DayCode.Lantern, FloorProp);
        Out.put(X + 3, Y, Z + 1, Scp001DayCode.EmptyTin, FloorProp);
        Out.put(X + 2, Y, Z + 2, Scp001DayCode.EmptyTin, FloorProp);
        Out.put(X + 1, Y, Z + 3, Scp001DayCode.PaperDrift, FloorProp);
    }

    public static boolean camp(int CellX, int CellZ) {
        return Scp001DayHash.chance(CellX * 541 + CellZ * 113, 0, 0, SaltSewerProps, CampOdds);
    }
}
