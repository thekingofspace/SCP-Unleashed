package net.rai.scp_unleashed.scp001day.plan;

public final class Scp001DayPlan {
    public static final int Cell = 64;
    public static final int Street = 12;
    public static final int StreetY = 63;
    public static final int FloorHeight = 5;
    public static final int MinFloors = 6;
    public static final int MaxFloors = 40;
    public static final int Height = 320;
    public static final int SewerWalk = 53;
    public static final int SewerChannel = 51;
    public static final int SewerCeiling = 59;
    public static final int JunctionCeiling = 61;
    public static final int TunnelLow = 2;
    public static final int TunnelHigh = 10;
    public static final int ChannelLow = 5;
    public static final int ChannelHigh = 7;
    public static final int CoreWidth = 6;
    public static final int CoreLength = 8;
    public static final int BasementLevels = 2;
    public static final int RoomGrid = 9;
    public static final int ManholeAt = 38;
    public static final double OpenManholeOdds = 0.4;
    public static final double StreetHoleOdds = 0.25;
    public static final double BridgeOdds = 0.35;
    public static final double CollapseOdds = 0.55;
    public static final double OfficeOdds = 0.6;
    public static final double CrackOdds = 0.14;
    public static final double BrokenWindowOdds = 0.55;
    public static final double GlassWindowOdds = 0.2;
    public static final double BoardedWindowOdds = 0.1;
    public static final double StainOdds = 0.012;
    public static final double ResidueOdds = 0.004;
    public static final double RoomContainerOdds = 0.004;
    public static final int LampSpacing = 12;
    public static final int LampAcross = 6;
    public static final double EmergencyOdds = 0.08;
    public static final double FlickerOdds = 0.15;
    public static final double JunctionEmergencyOdds = 0.35;
    public static final int DrainSpacing = 24;
    public static final int DrainAcross = 3;
    public static final double SewerContainerOdds = 0.2;
    public static final double JunctionContainerOdds = 0.35;
    public static final int SaltTower = 11;
    public static final int SaltBridge = 12;
    public static final int SaltWindow = 13;
    public static final int SaltCrack = 14;
    public static final int SaltWound = 15;
    public static final int SaltCollapse = 16;
    public static final int SaltStreet = 17;
    public static final int SaltSewer = 18;
    public static final int SaltLot = 19;

    private static final ThreadLocal<Tower[]> Recent = ThreadLocal.withInitial(() -> new Tower[16]);

    private Scp001DayPlan() {
    }

    public record Tower(int CellX, int CellZ, int MinX, int MinZ, int MaxX, int MaxZ, int Floors, int Roof, int Collapse, int Material, int CoreX, int CoreZ,
                       boolean Offices, int Wounds, int[] WoundX, int[] WoundY, int[] WoundZ, int[] WoundRadius) {
        public boolean inside(int X, int Z) {
            return X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ;
        }

        public int solidTop() {
            return Roof - Collapse * FloorHeight;
        }

        public int floorBase(int Floor) {
            return StreetY + Floor * FloorHeight;
        }
    }

    public record Bridge(boolean AlongX, int From, int To, int Low, int High, int Floor, int Base) {
    }

    public static int cellOf(int Value) {
        return Math.floorDiv(Value, Cell);
    }

    public static int local(int Value) {
        return Math.floorMod(Value, Cell);
    }

    public static Tower tower(int CellX, int CellZ) {
        Tower[] Cache = Recent.get();
        int Slot = Math.floorMod(CellX * 31 + CellZ * 17, Cache.length);
        Tower Known = Cache[Slot];
        if (Known != null && Known.CellX == CellX && Known.CellZ == CellZ) {
            return Known;
        }
        Tower Made = makeTower(CellX, CellZ);
        Cache[Slot] = Made;
        return Made;
    }

    private static Tower makeTower(int CellX, int CellZ) {
        int Width = Scp001DayHash.range(CellX, CellZ, 0, SaltTower, 26, 44);
        int Depth = Scp001DayHash.range(CellX, CellZ, 1, SaltTower, 26, 44);
        int Spare = Cell - Street - 4;
        int OffsetX = Street + 2 + Scp001DayHash.range(CellX, CellZ, 2, SaltTower, 0, Math.max(0, Spare - Width));
        int OffsetZ = Street + 2 + Scp001DayHash.range(CellX, CellZ, 3, SaltTower, 0, Math.max(0, Spare - Depth));
        int MinX = CellX * Cell + OffsetX;
        int MinZ = CellZ * Cell + OffsetZ;
        int MaxX = MinX + Width - 1;
        int MaxZ = MinZ + Depth - 1;
        double Roll = Scp001DayHash.unit(CellX, CellZ, 4, SaltTower);
        int Floors = MinFloors + (int) Math.floor(Roll * Roll * (MaxFloors - MinFloors + 1));
        int Roof = StreetY + Floors * FloorHeight;
        int Collapse = Scp001DayHash.chance(CellX, CellZ, 5, SaltTower, CollapseOdds) ? Scp001DayHash.range(CellX, CellZ, 6, SaltTower, 1, Math.max(1, Floors / 3)) : 0;
        int Material = Scp001DayHash.range(CellX, CellZ, 7, SaltTower, 0, Scp001DayCode.Materials - 1);
        int CoreX = (MinX + MaxX) / 2 - CoreWidth / 2;
        int CoreZ = (MinZ + MaxZ) / 2 - CoreLength / 2;
        boolean Offices = Scp001DayHash.chance(CellX, CellZ, 8, SaltTower, OfficeOdds);
        int Wounds = Scp001DayHash.range(CellX, CellZ, 9, SaltTower, 0, 3);
        int[] WoundX = new int[Wounds];
        int[] WoundY = new int[Wounds];
        int[] WoundZ = new int[Wounds];
        int[] WoundRadius = new int[Wounds];
        for (int Index = 0; Index < Wounds; Index++) {
            int Side = Scp001DayHash.range(CellX, CellZ, 10 + Index, SaltWound, 0, 3);
            double Along = Scp001DayHash.unit(CellX, CellZ, 20 + Index, SaltWound);
            WoundY[Index] = StreetY + 6 + (int) Math.floor(Scp001DayHash.unit(CellX, CellZ, 30 + Index, SaltWound) * Math.max(1, Roof - StreetY - 10));
            WoundRadius[Index] = Scp001DayHash.range(CellX, CellZ, 40 + Index, SaltWound, 2, 5);
            if (Side == 0) {
                WoundX[Index] = MinX;
                WoundZ[Index] = MinZ + (int) (Along * Depth);
            } else if (Side == 1) {
                WoundX[Index] = MaxX;
                WoundZ[Index] = MinZ + (int) (Along * Depth);
            } else if (Side == 2) {
                WoundX[Index] = MinX + (int) (Along * Width);
                WoundZ[Index] = MinZ;
            } else {
                WoundX[Index] = MinX + (int) (Along * Width);
                WoundZ[Index] = MaxZ;
            }
        }
        return new Tower(CellX, CellZ, MinX, MinZ, MaxX, MaxZ, Floors, Roof, Collapse, Material, CoreX, CoreZ, Offices, Wounds, WoundX, WoundY, WoundZ, WoundRadius);
    }

    public static Bridge bridgeAcrossX(int CellX, int CellZ) {
        Tower West = tower(CellX - 1, CellZ);
        Tower East = tower(CellX, CellZ);
        return bridge(true, CellX, CellZ, West.MaxX + 1, East.MinX - 1, Math.max(West.MinZ, East.MinZ), Math.min(West.MaxZ, East.MaxZ), West, East);
    }

    public static Bridge bridgeAcrossZ(int CellX, int CellZ) {
        Tower North = tower(CellX, CellZ - 1);
        Tower South = tower(CellX, CellZ);
        return bridge(false, CellX, CellZ, North.MaxZ + 1, South.MinZ - 1, Math.max(North.MinX, South.MinX), Math.min(North.MaxX, South.MaxX), North, South);
    }

    private static Bridge bridge(boolean AlongX, int CellX, int CellZ, int From, int To, int OverlapLow, int OverlapHigh, Tower First, Tower Second) {
        int Salt = AlongX ? 1 : 2;
        if (!Scp001DayHash.chance(CellX, CellZ, Salt, SaltBridge, BridgeOdds) || OverlapHigh - OverlapLow < 8) {
            return null;
        }
        int TopFloor = Math.min(First.solidTop(), Second.solidTop()) - StreetY;
        int Highest = TopFloor / FloorHeight - 2;
        if (Highest < 2) {
            return null;
        }
        int Floor = Scp001DayHash.range(CellX, CellZ, Salt + 10, SaltBridge, 2, Highest);
        int Centre = OverlapLow + 3 + Scp001DayHash.range(CellX, CellZ, Salt + 20, SaltBridge, 0, OverlapHigh - OverlapLow - 6);
        return new Bridge(AlongX, From, To, Centre - 1, Centre + 1, Floor, StreetY + Floor * FloorHeight);
    }

    public static int code(int X, int Y, int Z) {
        int[] Column = new int[Height];
        column(X, Z, Column);
        return Y < 0 || Y >= Height ? Scp001DayCode.Air : Column[Y];
    }

    public static void column(int X, int Z, int[] Out) {
        java.util.Arrays.fill(Out, Scp001DayCode.Air);
        int CellX = cellOf(X);
        int CellZ = cellOf(Z);
        int U = local(X);
        int V = local(Z);
        Out[0] = Scp001DayCode.Bedrock;
        for (int Y = 1; Y < StreetY; Y++) {
            Out[Y] = Scp001DayCode.Stone;
        }
        boolean StreetU = U < Street;
        boolean StreetV = V < Street;
        if (StreetU || StreetV) {
            street(X, Z, CellX, CellZ, U, V, StreetU, StreetV, Out);
        } else {
            lot(X, Z, CellX, CellZ, U, V, Out);
        }
        Tower Here = tower(CellX, CellZ);
        if (!Here.inside(X, Z)) {
            if (!(StreetU && StreetV)) {
                bridges(X, Z, CellX, CellZ, Out);
            }
            corridor(Here, X, Z, U, Out);
        }
    }

    private static void corridor(Tower Here, int X, int Z, int U, int[] Out) {
        int Low = Here.CoreZ + CoreLength - 3;
        int High = Here.CoreZ + CoreLength - 2;
        if (Z < Low || Z > High || U < TunnelHigh || X >= Here.MinX) {
            return;
        }
        for (int Y = SewerWalk + 1; Y <= SewerWalk + 3; Y++) {
            Out[Y] = Scp001DayCode.Air;
        }
    }

    private static void bridges(int X, int Z, int CellX, int CellZ, int[] Out) {
        Bridge[] Candidates = {bridgeAcrossX(CellX, CellZ), bridgeAcrossX(CellX + 1, CellZ), bridgeAcrossZ(CellX, CellZ), bridgeAcrossZ(CellX, CellZ + 1)};
        for (Bridge Over : Candidates) {
            if (Over == null) {
                continue;
            }
            int Coordinate = Over.AlongX ? X : Z;
            int Side = Over.AlongX ? Z : X;
            if (Coordinate >= Over.From && Coordinate <= Over.To && Side >= Over.Low - 1 && Side <= Over.High + 1) {
                bridgeColumn(Over, Coordinate, Side, Out);
            }
        }
    }

    private static void street(int X, int Z, int CellX, int CellZ, int U, int V, boolean StreetU, boolean StreetV, int[] Out) {
        boolean Junction = StreetU && StreetV;
        int Across = StreetU && !Junction ? U : V;
        boolean Curb = !Junction && (Across <= 1 || Across >= Street - 2);
        Out[StreetY] = Curb ? Scp001DayCode.Sidewalk : Scp001DayCode.Road;
        if (Curb) {
            Out[StreetY + 1] = Scp001DayCode.Curb;
        }
        if (Junction) {
            junction(X, Z, U, V, Out);
        } else {
            int Along = StreetU ? V : U;
            tunnel(X, Z, Across, Along, Out);
            if (Across == 3 && Along == ManholeAt) {
                manhole(X, Z, CellX, CellZ, StreetU, Out);
            } else if (drain(Across, Along)) {
                Out[StreetY] = Scp001DayCode.StormDrain;
                for (int Y = SewerCeiling; Y < StreetY; Y++) {
                    Out[Y] = Scp001DayCode.Air;
                }
            }
        }
        if (!Junction && Out[StreetY] == Scp001DayCode.Road && streetHole(X, Z, CellX, CellZ, StreetU)) {
            Out[StreetY] = Scp001DayCode.Air;
            for (int Y = SewerCeiling - 1; Y < StreetY; Y++) {
                Out[Y] = Scp001DayCode.Air;
            }
            if (Out[SewerWalk] == Scp001DayCode.SewerBrick || Out[SewerWalk] == Scp001DayCode.SewerStained || Out[SewerWalk] == Scp001DayCode.SewerCracked) {
                double Roll = Scp001DayHash.unit(X, 0, Z, SaltStreet);
                if (Roll < 0.35) {
                    Out[SewerWalk + 1] = Scp001DayCode.RubbleSlab;
                } else if (Roll < 0.6) {
                    Out[SewerWalk + 1] = Scp001DayCode.Gravel;
                } else if (Roll < 0.82) {
                    Out[SewerWalk + 1] = Scp001DayHash.chance(X, 6, Z, SaltStreet, 0.5) ? Scp001DayCode.Rubble : Scp001DayCode.Debris;
                    Out[SewerWalk + 2] = Scp001DayCode.RubbleSlab;
                }
            }
        } else if (Out[StreetY] == Scp001DayCode.Road) {
            double Mark = Scp001DayHash.unit(X, 1, Z, SaltStreet);
            if (Mark < ResidueOdds) {
                Out[StreetY] = Scp001DayCode.Residue;
            } else if (Mark < ResidueOdds + StainOdds) {
                Out[StreetY] = Scp001DayCode.Stain;
            }
        }
    }

    public static boolean drain(int Across, int Along) {
        return Across == DrainAcross && Math.floorMod(Along - Street, DrainSpacing) == DrainSpacing / 2 && Math.abs(Along - ManholeAt) > 2;
    }

    public static int sewerLamp(int X, int Z, int Along) {
        if (Math.floorMod(Along, LampSpacing) != LampSpacing / 2) {
            return Scp001DayCode.Air;
        }
        double Roll = Scp001DayHash.unit(X, SewerCeiling, Z, SaltSewer + 1);
        return Roll < EmergencyOdds ? Scp001DayCode.EmergencyLamp : (Roll < EmergencyOdds + FlickerOdds ? Scp001DayCode.FlickerLamp : Scp001DayCode.DeadLamp);
    }

    public static int junctionLamp(int X, int Z) {
        return Scp001DayHash.chance(X, JunctionCeiling, Z, SaltSewer + 1, JunctionEmergencyOdds) ? Scp001DayCode.EmergencyLamp : Scp001DayCode.DeadLamp;
    }

    @FunctionalInterface
    public interface LampSink {
        void accept(int X, int Y, int Z, int Code);
    }

    public static void sewerLamps(int CellX, int CellZ, LampSink Out) {
        int OriginX = CellX * Cell;
        int OriginZ = CellZ * Cell;
        for (int Along = Street; Along < Cell; Along++) {
            int AlongZX = OriginX + LampAcross;
            int AlongZZ = OriginZ + Along;
            int Code = sewerLamp(AlongZX, AlongZZ, Along);
            if (Code != Scp001DayCode.Air) {
                Out.accept(AlongZX, SewerCeiling - 1, AlongZZ, Code);
            }
            int AlongXX = OriginX + Along;
            int AlongXZ = OriginZ + LampAcross;
            Code = sewerLamp(AlongXX, AlongXZ, Along);
            if (Code != Scp001DayCode.Air) {
                Out.accept(AlongXX, SewerCeiling - 1, AlongXZ, Code);
            }
        }
        Out.accept(OriginX + Street / 2, JunctionCeiling - 1, OriginZ + Street / 2, junctionLamp(OriginX + Street / 2, OriginZ + Street / 2));
    }

    private static boolean streetHole(int X, int Z, int CellX, int CellZ, boolean StreetU) {
        int Salt = StreetU ? 1 : 2;
        if (!Scp001DayHash.chance(CellX, CellZ, Salt, SaltStreet, StreetHoleOdds)) {
            return false;
        }
        int Along = Street + 4 + Scp001DayHash.range(CellX, CellZ, Salt + 4, SaltStreet, 0, Cell - Street - 8);
        int Radius = Scp001DayHash.range(CellX, CellZ, Salt + 8, SaltStreet, 2, 3);
        int CentreAcross = 6;
        int Du = (StreetU ? local(X) - CentreAcross : local(X) - Along);
        int Dv = (StreetU ? local(Z) - Along : local(Z) - CentreAcross);
        double Jag = 0.75 + 0.5 * Scp001DayHash.unit(X, 2, Z, SaltStreet);
        return Du * Du + Dv * Dv <= Radius * Radius * Jag;
    }

    private static int sewerWall(int X, int Y, int Z) {
        double Roll = Scp001DayHash.unit(X, Y, Z, SaltSewer);
        return Roll < 0.18 ? Scp001DayCode.SewerStained : (Roll < 0.3 ? Scp001DayCode.SewerCracked : Scp001DayCode.SewerBrick);
    }

    private static void tunnel(int X, int Z, int Across, int Along, int[] Out) {
        if (Across < TunnelLow || Across > TunnelHigh) {
            return;
        }
        if (Across == TunnelLow || Across == TunnelHigh) {
            for (int Y = SewerChannel; Y <= SewerCeiling; Y++) {
                Out[Y] = sewerWall(X, Y, Z);
            }
            return;
        }
        Out[SewerCeiling] = sewerWall(X, SewerCeiling, Z);
        for (int Y = SewerWalk + 1; Y < SewerCeiling; Y++) {
            Out[Y] = Scp001DayCode.Air;
        }
        if (Across == LampAcross) {
            int Lamp = sewerLamp(X, Z, Along);
            if (Lamp != Scp001DayCode.Air) {
                Out[SewerCeiling - 1] = Lamp;
            }
        }
        if (Across >= ChannelLow && Across <= ChannelHigh) {
            Out[SewerChannel] = Scp001DayCode.SewerBrick;
            Out[SewerChannel + 1] = Scp001DayCode.Water;
            Out[SewerWalk] = Scp001DayCode.Air;
        } else {
            Out[SewerWalk] = sewerWall(X, SewerWalk, Z);
        }
    }

    private static void junction(int X, int Z, int U, int V, int[] Out) {
        boolean Edge = U == 0 || U == Street - 1 || V == 0 || V == Street - 1;
        boolean OpenU = V >= TunnelLow + 1 && V <= TunnelHigh - 1;
        boolean OpenV = U >= TunnelLow + 1 && U <= TunnelHigh - 1;
        if (Edge && !((U == 0 || U == Street - 1) && OpenU) && !((V == 0 || V == Street - 1) && OpenV)) {
            for (int Y = SewerChannel; Y <= JunctionCeiling; Y++) {
                Out[Y] = sewerWall(X, Y, Z);
            }
            return;
        }
        int Ceiling = Edge ? SewerCeiling : JunctionCeiling;
        Out[Ceiling] = sewerWall(X, Ceiling, Z);
        for (int Y = SewerWalk + 1; Y < Ceiling; Y++) {
            Out[Y] = Scp001DayCode.Air;
        }
        if (U == Street / 2 && V == Street / 2) {
            Out[Ceiling - 1] = junctionLamp(X, Z);
        }
        boolean Channel = (U >= ChannelLow && U <= ChannelHigh) || (V >= ChannelLow && V <= ChannelHigh);
        if (Channel) {
            Out[SewerChannel] = Scp001DayCode.SewerBrick;
            Out[SewerChannel + 1] = Scp001DayCode.Water;
            Out[SewerWalk] = Scp001DayCode.Air;
        } else {
            Out[SewerWalk] = sewerWall(X, SewerWalk, Z);
        }
    }

    private static void manhole(int X, int Z, int CellX, int CellZ, boolean StreetU, int[] Out) {
        int Ladder = StreetU ? Scp001DayCode.LadderEast : Scp001DayCode.LadderSouth;
        for (int Y = SewerWalk + 1; Y < StreetY; Y++) {
            Out[Y] = Ladder;
        }
        boolean Open = Scp001DayHash.chance(CellX, CellZ, StreetU ? 1 : 2, SaltSewer, OpenManholeOdds);
        Out[StreetY] = Open ? Scp001DayCode.Air : Scp001DayCode.ManholeCover;
        Out[StreetY + 1] = Scp001DayCode.Air;
    }

    private static void bridgeColumn(Bridge Over, int Coordinate, int Side, int[] Out) {
        boolean Wall = Side == Over.Low - 1 || Side == Over.High + 1;
        Out[Over.Base] = Scp001DayCode.FloorSlab;
        Out[Over.Base + 4] = Scp001DayCode.FloorSlab;
        for (int Y = Over.Base + 1; Y < Over.Base + 4; Y++) {
            if (Wall) {
                boolean Window = Y == Over.Base + 2 && Math.floorMod(Coordinate, 3) != 0;
                Out[Y] = Window ? (Scp001DayHash.chance(Coordinate, Y, Side, SaltWindow, 0.5) ? Scp001DayCode.Air : Scp001DayCode.Glass) : Scp001DayCode.Facade;
            } else {
                Out[Y] = Scp001DayCode.Air;
            }
        }
        if (Scp001DayHash.chance(Coordinate, Over.Base, Side, SaltWound, 0.06)) {
            Out[Over.Base + 4] = Scp001DayCode.Air;
        }
    }

    private static void lot(int X, int Z, int CellX, int CellZ, int U, int V, int[] Out) {
        Tower Here = tower(CellX, CellZ);
        if (!Here.inside(X, Z)) {
            Out[StreetY] = Scp001DayCode.Plaza;
            int Gap = Math.min(Math.min(Math.abs(X - Here.MinX), Math.abs(X - Here.MaxX)), Math.min(Math.abs(Z - Here.MinZ), Math.abs(Z - Here.MaxZ)));
            if (Gap <= 3 && Here.Collapse > 0) {
                pile(X, Z, Gap, Out);
            } else if (Scp001DayHash.unit(X, 5, Z, SaltLot) < StainOdds) {
                Out[StreetY] = Scp001DayCode.Stain;
            }
            return;
        }
        towerColumn(Here, X, Z, Out);
        settle(Here, X, Z, Out);
    }

    private static void pile(int X, int Z, int Gap, int[] Out) {
        double Roll = Scp001DayHash.unit(X, 3, Z, SaltLot);
        int Height = 3 - Gap + (Roll < 0.3 ? -1 : (Roll > 0.8 ? 1 : 0));
        if (Height <= 0 || (Gap == 3 && Roll < 0.6)) {
            return;
        }
        for (int Level = 1; Level < Height; Level++) {
            Out[StreetY + Level] = Scp001DayHash.chance(X, 10 + Level, Z, SaltLot, 0.6) ? Scp001DayCode.Rubble : Scp001DayCode.Debris;
        }
        double Top = Scp001DayHash.unit(X, 4, Z, SaltLot);
        int Cap;
        if (Top < 0.45) {
            Cap = Scp001DayCode.RubbleSlab;
        } else if (Top < 0.75) {
            Cap = Scp001DayCode.RubbleStair + Scp001DayHash.range(X, 7, Z, SaltLot, 0, 3);
        } else if (Top < 0.9) {
            Cap = Scp001DayCode.Gravel;
        } else {
            Cap = Scp001DayCode.RubbleWall;
        }
        Out[StreetY + Height] = Cap;
    }

    public static final int SettleCache = 96;
    private static final java.util.concurrent.ConcurrentHashMap<Long, Settled> Settles = new java.util.concurrent.ConcurrentHashMap<>();

    public record Settled(int Low, int Span, java.util.BitSet Loose, int[] PileBase, int[] PileHeight, int Removed) {
    }

    public static void forget() {
        Settles.clear();
    }

    public static Settled settled(Tower Here) {
        long Key = ((long) Here.CellX << 32) ^ (Here.CellZ & 0xFFFFFFFFL);
        Settled Found = Settles.get(Key);
        if (Found != null) {
            return Found;
        }
        if (Settles.size() > SettleCache) {
            Settles.clear();
        }
        return Settles.computeIfAbsent(Key, Ignored -> collapse(Here));
    }

    private static Settled collapse(Tower Here) {
        int Width = Here.MaxX - Here.MinX + 1;
        int Depth = Here.MaxZ - Here.MinZ + 1;
        int Low = StreetY;
        int High = Math.min(Height - 1, Here.Roof + FloorHeight);
        int Span = High - Low + 1;
        int[][] Raw = new int[Width * Depth][];
        for (int Dx = 0; Dx < Width; Dx++) {
            for (int Dz = 0; Dz < Depth; Dz++) {
                int[] Column = new int[Height];
                java.util.Arrays.fill(Column, Scp001DayCode.Air);
                towerColumn(Here, Here.MinX + Dx, Here.MinZ + Dz, Column);
                Raw[Dx * Depth + Dz] = Column;
            }
        }
        java.util.BitSet Reached = flood(Raw, Width, Depth, Low, Span, null);
        if (Here.Collapse > 0) {
            java.util.BitSet Trimmed = new java.util.BitSet(Width * Depth * Span);
            boolean[] Exposed = new boolean[Width * Depth * Span];
            boolean[] Bearing = new boolean[Width * Depth * Span];
            for (int Index = 0; Index < Width * Depth; Index++) {
                for (int Y = 0; Y < Span; Y++) {
                    int Cell = Index * Span + Y;
                    if (!Reached.get(Cell)) {
                        continue;
                    }
                    int Code = Raw[Index][Low + Y];
                    boolean Open = true;
                    for (int Above = 1; Above <= FloorHeight && Y + Above < Span; Above++) {
                        if (Reached.get(Cell + Above)) {
                            Open = false;
                            break;
                        }
                    }
                    Exposed[Cell] = Code == Scp001DayCode.FloorSlab && Open && Low + Y < Here.Roof;
                    boolean Below = Y > 0 && Reached.get(Cell - 1);
                    Bearing[Cell] = Code != Scp001DayCode.FloorSlab || !Open || Below;
                }
            }
            for (int Dx = 0; Dx < Width; Dx++) {
                for (int Dz = 0; Dz < Depth; Dz++) {
                    for (int Y = 0; Y < Span; Y++) {
                        int Cell = (Dx * Depth + Dz) * Span + Y;
                        if (!Exposed[Cell]) {
                            continue;
                        }
                        boolean Held = false;
                        for (int Ox = Math.max(0, Dx - Cantilever); Ox <= Math.min(Width - 1, Dx + Cantilever) && !Held; Ox++) {
                            for (int Oz = Math.max(0, Dz - Cantilever); Oz <= Math.min(Depth - 1, Dz + Cantilever); Oz++) {
                                if (Bearing[(Ox * Depth + Oz) * Span + Y]) {
                                    Held = true;
                                    break;
                                }
                            }
                        }
                        if (!Held) {
                            Trimmed.set(Cell);
                        }
                    }
                }
            }
            if (!Trimmed.isEmpty()) {
                Reached = flood(Raw, Width, Depth, Low, Span, Trimmed);
            }
        }
        java.util.BitSet Loose = new java.util.BitSet(Width * Depth * Span);
        int[] PileBase = new int[Width * Depth];
        int[] PileHeight = new int[Width * Depth];
        int Removed = 0;
        for (int Index = 0; Index < Width * Depth; Index++) {
            int Count = 0;
            int Lowest = -1;
            for (int Y = 0; Y < Span; Y++) {
                if (Scp001DaySupport.kind(Raw[Index][Low + Y]) != Scp001DaySupport.Empty && !Reached.get(Index * Span + Y)) {
                    Loose.set(Index * Span + Y);
                    Count++;
                    if (Lowest < 0) {
                        Lowest = Y;
                    }
                }
            }
            Removed += Count;
            int CoreLocalX = Index / Depth + Here.MinX - Here.CoreX;
            int CoreLocalZ = Index % Depth + Here.MinZ - Here.CoreZ;
            if (Count == 0 || (CoreLocalX >= -1 && CoreLocalX <= CoreWidth && CoreLocalZ >= -1 && CoreLocalZ <= CoreLength)) {
                continue;
            }
            int Floor = Lowest - 1;
            while (Floor > 0 && (Scp001DaySupport.kind(Raw[Index][Low + Floor]) == Scp001DaySupport.Empty || Loose.get(Index * Span + Floor))) {
                Floor--;
            }
            if (Scp001DaySupport.kind(Raw[Index][Low + Floor]) != Scp001DaySupport.Full) {
                continue;
            }
            PileBase[Index] = Low + Floor;
            PileHeight[Index] = Math.min(PileLimit, 1 + Count / PileShare);
        }
        return new Settled(Low, Span, Loose, PileBase, PileHeight, Removed);
    }

    public static final int Cantilever = 2;

    private static java.util.BitSet flood(int[][] Raw, int Width, int Depth, int Low, int Span, java.util.BitSet Gone) {
        java.util.BitSet Reached = new java.util.BitSet(Width * Depth * Span);
        int[] Open = new int[Width * Depth * Span];
        int Head = 0;
        int Tail = 0;
        for (int Index = 0; Index < Width * Depth; Index++) {
            if (Scp001DaySupport.kind(Raw[Index][Low]) != Scp001DaySupport.Empty && (Gone == null || !Gone.get(Index * Span))) {
                Reached.set(Index * Span);
                Open[Tail++] = Index * Span;
            }
        }
        int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (Head < Tail) {
            int Cell = Open[Head++];
            int Index = Cell / Span;
            int Y = Cell % Span;
            int Dx = Index / Depth;
            int Dz = Index % Depth;
            int Code = Raw[Index][Low + Y];
            for (int[] Step : Steps) {
                int Nx = Dx + Step[0];
                int Ny = Y + Step[1];
                int Nz = Dz + Step[2];
                if (Nx < 0 || Nz < 0 || Nx >= Width || Nz >= Depth || Ny < 0 || Ny >= Span) {
                    continue;
                }
                int Next = (Nx * Depth + Nz) * Span + Ny;
                if (!Reached.get(Next) && (Gone == null || !Gone.get(Next)) && Scp001DaySupport.touches(Code, Raw[Nx * Depth + Nz][Low + Ny], Step[0], Step[1], Step[2])) {
                    Reached.set(Next);
                    Open[Tail++] = Next;
                }
            }
        }
        return Reached;
    }

    public static final int PileLimit = 3;
    public static final int PileShare = 6;

    private static void settle(Tower Here, int X, int Z, int[] Out) {
        Settled Fallen = settled(Here);
        if (Fallen.Removed() == 0) {
            return;
        }
        int Index = (X - Here.MinX) * (Here.MaxZ - Here.MinZ + 1) + (Z - Here.MinZ);
        for (int Y = 0; Y < Fallen.Span(); Y++) {
            if (Fallen.Loose().get(Index * Fallen.Span() + Y)) {
                Out[Fallen.Low() + Y] = Scp001DayCode.Air;
            }
        }
        int Pile = Fallen.PileHeight()[Index];
        for (int Level = 1; Level <= Pile; Level++) {
            int Y = Fallen.PileBase()[Index] + Level;
            if (Y >= Height || Out[Y] != Scp001DayCode.Air) {
                break;
            }
            boolean Cap = Level == Pile || Y + 1 >= Height || Out[Y + 1] != Scp001DayCode.Air;
            double Roll = Scp001DayHash.unit(X, Y, Z, SaltCollapse + 3);
            if (Cap) {
                Out[Y] = Roll < 0.45 ? Scp001DayCode.RubbleSlab : (Roll < 0.75 ? Scp001DayCode.RubbleStair + Scp001DayHash.range(X, Y, Z, SaltCollapse + 4, 0, 3) : Scp001DayCode.Gravel);
                break;
            }
            Out[Y] = Roll < 0.6 ? Scp001DayCode.Rubble : Scp001DayCode.Debris;
        }
    }

    private static int columnTop(Tower Here, int X, int Z) {
        if (Here.Collapse <= 0) {
            return Here.Roof;
        }
        int PatchX = Math.floorDiv(X - Here.MinX, 3);
        int PatchZ = Math.floorDiv(Z - Here.MinZ, 3);
        int Drop = (int) Math.floor(Scp001DayHash.unit(PatchX + Here.CellX * 101, PatchZ + Here.CellZ * 103, 0, SaltCollapse) * (Here.Collapse + 1));
        return Here.Roof - Math.min(Here.Collapse, Drop) * FloorHeight;
    }

    private static void towerColumn(Tower Here, int X, int Z, int[] Out) {
        boolean EdgeX = X == Here.MinX || X == Here.MaxX;
        boolean EdgeZ = Z == Here.MinZ || Z == Here.MaxZ;
        boolean Facade = EdgeX || EdgeZ;
        boolean Corner = EdgeX && EdgeZ;
        int Top = columnTop(Here, X, Z);
        int FacadeCode = Scp001DayCode.Facade + Here.Material;
        int CoreLocalX = X - Here.CoreX;
        int CoreLocalZ = Z - Here.CoreZ;
        boolean InCore = CoreLocalX >= 0 && CoreLocalX < CoreWidth && CoreLocalZ >= 0 && CoreLocalZ < CoreLength;
        boolean CoreWall = InCore && (CoreLocalX == 0 || CoreLocalX == CoreWidth - 1 || CoreLocalZ == 0 || CoreLocalZ == CoreLength - 1);
        int Lowest = -BasementLevels;
        for (int Floor = Lowest; Floor <= Here.Floors; Floor++) {
            int Base = Here.floorBase(Floor);
            if (Base > Top) {
                break;
            }
            Out[Base] = InCore && !CoreWall ? coreSlab(Here, Floor, CoreLocalX, CoreLocalZ, Base) : (Floor < 0 && !InCore ? Scp001DayCode.Stone : (Facade ? FacadeCode : Scp001DayCode.FloorSlab));
            if (Floor == Here.Floors || Base == Top) {
                if (Facade && Base == Top && Here.Collapse > 0 && Top < Here.Roof && Scp001DayHash.chance(X, Base, Z, SaltCollapse, 0.22)) {
                    Out[Base + 1] = Scp001DayCode.Rebar;
                }
                continue;
            }
            for (int Level = 1; Level < FloorHeight; Level++) {
                int Y = Base + Level;
                Out[Y] = Floor < 0 ? basementCell(Here, X, Z, Floor, Level, InCore, CoreWall, CoreLocalX, CoreLocalZ, Y) : storeyCell(Here, X, Z, Floor, Level, Facade, Corner, EdgeX, InCore, CoreWall, CoreLocalX, CoreLocalZ, FacadeCode, Y);
            }
        }
        for (int Index = 0; Index < Here.Wounds; Index++) {
            int Dx = X - Here.WoundX[Index];
            int Dz = Z - Here.WoundZ[Index];
            int Radius = Here.WoundRadius[Index];
            if (Math.abs(Dx) > Radius + 1 || Math.abs(Dz) > Radius + 1) {
                continue;
            }
            for (int Y = Here.WoundY[Index] - Radius - 1; Y <= Here.WoundY[Index] + Radius + 1; Y++) {
                if (Y <= StreetY || Y >= Height) {
                    continue;
                }
                int Dy = Y - Here.WoundY[Index];
                double Jag = 0.7 + 0.6 * Scp001DayHash.unit(X, Y, Z, SaltWound);
                if (Dx * Dx + Dy * Dy + Dz * Dz <= Radius * Radius * Jag) {
                    Out[Y] = Scp001DayCode.Air;
                }
            }
        }
    }

    private static int basementCell(Tower Here, int X, int Z, int Floor, int Level, boolean InCore, boolean CoreWall, int CoreLocalX, int CoreLocalZ, int Y) {
        if (InCore && !CoreWall) {
            return coreInterior(Here, Floor, CoreLocalX, CoreLocalZ, Level);
        }
        if (Floor == -BasementLevels && Level <= 3) {
            int CorridorLow = Here.CoreZ + CoreLength - 3;
            int CorridorHigh = Here.CoreZ + CoreLength - 2;
            if (Z >= CorridorLow && Z <= CorridorHigh && X <= Here.CoreX) {
                return Scp001DayCode.Air;
            }
        }
        return InCore ? Scp001DayCode.CoreWall : Scp001DayCode.Stone;
    }

    private static int storeyCell(Tower Here, int X, int Z, int Floor, int Level, boolean Facade, boolean Corner, boolean EdgeX, boolean InCore, boolean CoreWall,
                                  int CoreLocalX, int CoreLocalZ, int FacadeCode, int Y) {
        if (Facade) {
            if (Corner) {
                return FacadeCode;
            }
            int Along = EdgeX ? Z - Here.MinZ : X - Here.MinX;
            int Span = EdgeX ? Here.MaxZ - Here.MinZ : Here.MaxX - Here.MinX;
            int Side = EdgeX ? (X == Here.MinX ? 0 : 1) : (Z == Here.MinZ ? 2 : 3);
            if (bridgeDoor(Here, X, Z, Side, Y)) {
                return Scp001DayCode.Air;
            }
            boolean WindowColumn = Along >= 2 && Along <= Span - 2 && Math.floorMod(Along, 4) >= 1 && Math.floorMod(Along, 4) <= 2;
            if (Floor == 0) {
                int Middle = Span / 2;
                if (Side == 0 && Math.abs(Along - Middle) <= 1 && Level <= 3) {
                    return Scp001DayCode.Air;
                }
                if (WindowColumn && Level >= 1 && Level <= 3) {
                    return windowFill(Here, Floor, Side, Along / 4, Y);
                }
                return FacadeCode;
            }
            if (WindowColumn && (Level == 2 || Level == 3)) {
                return windowFill(Here, Floor, Side, Along / 4, Y);
            }
            if (!WindowColumn && Along >= 1 && Along <= Span - 1 && Scp001DayHash.chance(Here.CellX * 7 + Side, Floor + Here.CellZ * 211, Along, SaltCrack, CrackOdds / 4.0)
                    && Level <= 1 + Scp001DayHash.range(Here.CellX * 7 + Side, Floor + Here.CellZ * 211, Along, SaltCrack + 1, 1, 3)) {
                return Scp001DayCode.Air;
            }
            return FacadeCode;
        }
        if (InCore) {
            if (CoreWall) {
                if (CoreLocalX == CoreWidth - 1 && CoreLocalZ == CoreLength - 2 && Level <= 2) {
                    return Scp001DayCode.Air;
                }
                return Scp001DayCode.CoreWall;
            }
            return coreInterior(Here, Floor, CoreLocalX, CoreLocalZ, Level);
        }
        if (Here.Offices && Floor >= 1) {
            int Rx = Math.floorMod(X - Here.MinX, RoomGrid);
            int Rz = Math.floorMod(Z - Here.MinZ, RoomGrid);
            boolean WallX = Rx == 0 && X > Here.MinX + 1 && X < Here.MaxX - 1;
            boolean WallZ = Rz == 0 && Z > Here.MinZ + 1 && Z < Here.MaxZ - 1;
            if (WallX || WallZ) {
                int Door = WallX ? Rz : Rx;
                if ((Door == 4 || Door == 5) && Level <= 2) {
                    return Scp001DayCode.Air;
                }
                if (Scp001DayHash.chance(X, Floor, Z, SaltWound, 0.04)) {
                    return Scp001DayCode.Air;
                }
                return interiorWall(Here);
            }
        }
        if (Level == FloorHeight - 1 && Math.floorMod(X - Here.MinX, 6) == 3 && Math.floorMod(Z - Here.MinZ, 6) == 3) {
            return Scp001DayCode.DeadLamp;
        }
        return Scp001DayCode.Air;
    }

    public static int interiorWall(Tower Here) {
        int Variant = Math.floorMod(Here.CellX() * 7 + Here.CellZ() * 13, 4);
        return Variant == 0 ? Scp001DayCode.InteriorWall : (Variant == 1 ? Scp001DayCode.WallPanel : (Variant == 2 ? Scp001DayCode.WallPaperBlue : Scp001DayCode.WallPaperYellow));
    }

    private static boolean bridgeDoor(Tower Here, int X, int Z, int Side, int Y) {
        Bridge Over;
        if (Side == 0) {
            Over = bridgeAcrossX(Here.CellX, Here.CellZ);
        } else if (Side == 1) {
            Over = bridgeAcrossX(Here.CellX + 1, Here.CellZ);
        } else if (Side == 2) {
            Over = bridgeAcrossZ(Here.CellX, Here.CellZ);
        } else {
            Over = bridgeAcrossZ(Here.CellX, Here.CellZ + 1);
        }
        if (Over == null) {
            return false;
        }
        int Across = Over.AlongX ? Z : X;
        return Across >= Over.Low && Across <= Over.High && Y > Over.Base && Y < Over.Base + 4;
    }

    private static int windowFill(Tower Here, int Floor, int Side, int Index, int Y) {
        double Roll = Scp001DayHash.unit(Here.CellX * 13 + Side, Floor, Index + Here.CellZ * 7, SaltWindow);
        if (Roll < BrokenWindowOdds) {
            return Scp001DayCode.Air;
        }
        if (Roll < BrokenWindowOdds + GlassWindowOdds) {
            return Scp001DayCode.Glass;
        }
        if (Roll < BrokenWindowOdds + GlassWindowOdds + BoardedWindowOdds) {
            return Scp001DayCode.Boards;
        }
        return Scp001DayCode.Bars;
    }

    private static boolean evenFlight(int Floor) {
        return Math.floorMod(Floor, 2) == 0;
    }

    private static int coreSlab(Tower Here, int Floor, int CoreLocalX, int CoreLocalZ, int Base) {
        if (Floor == -BasementLevels) {
            return Scp001DayCode.FloorSlab;
        }
        int Below = Floor - 1;
        boolean LaneA = CoreLocalX == 1 || CoreLocalX == 2;
        boolean LaneB = CoreLocalX == 3 || CoreLocalX == 4;
        if (evenFlight(Below)) {
            if (LaneA && CoreLocalZ >= 1 && CoreLocalZ <= 4) {
                return Scp001DayCode.Air;
            }
            if (LaneA && CoreLocalZ == 5) {
                return Scp001DayCode.StairSouth;
            }
        } else {
            if (LaneB && CoreLocalZ >= 2 && CoreLocalZ <= 5) {
                return Scp001DayCode.Air;
            }
            if (LaneB && CoreLocalZ == 1) {
                return Scp001DayCode.StairNorth;
            }
        }
        return Scp001DayCode.FloorSlab;
    }

    private static int coreInterior(Tower Here, int Floor, int CoreLocalX, int CoreLocalZ, int Level) {
        if (Floor >= Here.Floors) {
            return Scp001DayCode.Air;
        }
        boolean LaneA = CoreLocalX == 1 || CoreLocalX == 2;
        boolean LaneB = CoreLocalX == 3 || CoreLocalX == 4;
        if (evenFlight(Floor)) {
            if (LaneA && CoreLocalZ >= 1 && CoreLocalZ <= 4 && Level == CoreLocalZ) {
                return Scp001DayCode.StairSouth;
            }
        } else {
            if (LaneB && CoreLocalZ >= 2 && CoreLocalZ <= 5 && Level == 6 - CoreLocalZ) {
                return Scp001DayCode.StairNorth;
            }
        }
        return Scp001DayCode.Air;
    }

    public static int arrivalX(int Index) {
        return 6 + (Index % 3) - 1;
    }

    public static int arrivalZ(int Index) {
        return 6 + (Index / 3 % 3) - 1;
    }

    public static int arrivalY() {
        return SewerWalk + 1;
    }

    public static void containers(int ChunkX, int ChunkZ, ContainerSink Sink) {
        int MinX = ChunkX * 16;
        int MinZ = ChunkZ * 16;
        for (int X = MinX; X < MinX + 16; X++) {
            for (int Z = MinZ; Z < MinZ + 16; Z++) {
                int U = local(X);
                int V = local(Z);
                boolean StreetU = U < Street;
                boolean StreetV = V < Street;
                if (StreetU || StreetV) {
                    if (StreetU && StreetV) {
                        boolean Camp = Scp001DayDressing.camp(cellOf(X), cellOf(Z));
                        if (Camp && U == 3 && V == 1) {
                            Sink.accept(X, SewerWalk + 1, Z, 2);
                        } else if ((U == 1 || U == Street - 2) && (V == 1 || V == Street - 2) && Scp001DayHash.chance(X, SewerWalk, Z, SaltSewer, JunctionContainerOdds)) {
                            Sink.accept(X, SewerWalk + 1, Z, 1);
                        }
                    } else {
                        int Across = StreetU ? U : V;
                        int Along = StreetU ? V : U;
                        if ((Across == 3 || Across == 9) && Along % 12 == 6 && Along != ManholeAt && Scp001DayHash.chance(X, SewerWalk, Z, SaltSewer, SewerContainerOdds)) {
                            Sink.accept(X, SewerWalk + 1, Z, 1);
                        }
                    }
                    continue;
                }
                Tower Here = tower(cellOf(X), cellOf(Z));
                if (!Here.inside(X, Z) || X == Here.MinX || X == Here.MaxX || Z == Here.MinZ || Z == Here.MaxZ) {
                    continue;
                }
                int CoreLocalX = X - Here.CoreX;
                int CoreLocalZ = Z - Here.CoreZ;
                if (CoreLocalX >= -1 && CoreLocalX <= CoreWidth && CoreLocalZ >= -1 && CoreLocalZ <= CoreLength) {
                    continue;
                }
                boolean NearWall = X == Here.MinX + 1 || X == Here.MaxX - 1 || Z == Here.MinZ + 1 || Z == Here.MaxZ - 1;
                if (!NearWall) {
                    continue;
                }
                int Top = columnTop(Here, X, Z);
                for (int Floor = 0; Floor < Here.Floors; Floor++) {
                    int Base = Here.floorBase(Floor);
                    if (Base + FloorHeight > Top) {
                        break;
                    }
                    if (Scp001DayHash.chance(X, Base, Z, SaltLot, RoomContainerOdds)) {
                        Sink.accept(X, Base + 1, Z, 0);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface ContainerSink {
        void accept(int X, int Y, int Z, int Kind);
    }

    public static final int FarCells = 6;

    public static int farTop() {
        return StreetY + MaxFloors * FloorHeight + 1;
    }

    public static double farTransmit(double X, double Y, double Z, double DirX, double DirY, double DirZ, double MaxLength) {
        if (DirY <= 0.0) {
            return 0.0;
        }
        double Length = Math.min(MaxLength, (farTop() - Y) / DirY);
        if (Length <= 0.0) {
            return 1.0;
        }
        int CellX = Math.floorDiv((int) Math.floor(X), Cell);
        int CellZ = Math.floorDiv((int) Math.floor(Z), Cell);
        int StepX = DirX > 0.0 ? 1 : (DirX < 0.0 ? -1 : 0);
        int StepZ = DirZ > 0.0 ? 1 : (DirZ < 0.0 ? -1 : 0);
        double DeltaX = StepX == 0 ? Double.POSITIVE_INFINITY : Cell / Math.abs(DirX);
        double DeltaZ = StepZ == 0 ? Double.POSITIVE_INFINITY : Cell / Math.abs(DirZ);
        double NextX = StepX > 0 ? ((CellX + 1) * (double) Cell - X) / DirX : (StepX < 0 ? (CellX * (double) Cell - X) / DirX : Double.POSITIVE_INFINITY);
        double NextZ = StepZ > 0 ? ((CellZ + 1) * (double) Cell - Z) / DirZ : (StepZ < 0 ? (CellZ * (double) Cell - Z) / DirZ : Double.POSITIVE_INFINITY);
        for (int Visit = 0; Visit <= FarCells; Visit++) {
            if (hits(tower(CellX, CellZ), X, Y, Z, DirX, DirY, DirZ, Length)) {
                return 0.0;
            }
            if (NextX < NextZ) {
                if (NextX > Length) {
                    return 1.0;
                }
                CellX += StepX;
                NextX += DeltaX;
            } else {
                if (NextZ > Length) {
                    return 1.0;
                }
                CellZ += StepZ;
                NextZ += DeltaZ;
            }
        }
        return 1.0;
    }

    private static boolean hits(Tower Here, double X, double Y, double Z, double DirX, double DirY, double DirZ, double Length) {
        double Near = 0.0;
        double Far = Length;
        double[] Low = {Here.MinX, StreetY, Here.MinZ};
        double[] High = {Here.MaxX + 1.0, Here.solidTop() + 1.0, Here.MaxZ + 1.0};
        double[] Origin = {X, Y, Z};
        double[] Direction = {DirX, DirY, DirZ};
        for (int Axis = 0; Axis < 3; Axis++) {
            if (Math.abs(Direction[Axis]) < 1.0e-12) {
                if (Origin[Axis] < Low[Axis] || Origin[Axis] > High[Axis]) {
                    return false;
                }
                continue;
            }
            double First = (Low[Axis] - Origin[Axis]) / Direction[Axis];
            double Second = (High[Axis] - Origin[Axis]) / Direction[Axis];
            Near = Math.max(Near, Math.min(First, Second));
            Far = Math.min(Far, Math.max(First, Second));
            if (Near > Far) {
                return false;
            }
        }
        return Near <= Far;
    }
}
