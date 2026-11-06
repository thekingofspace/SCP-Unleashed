package net.rai.scp_unleashed.scp3008.plan;

import static net.rai.scp_unleashed.scp3008.plan.Scp3008Code.*;
import static net.rai.scp_unleashed.scp3008.plan.Scp3008Plan.*;

public final class Scp3008Painter {
    public static final int[][] ContentLights = {{13, 20}, {29, 36}};
    public static final int LightFirst = 18;
    public static final int LightStep = 16;
    public static final int SignLaneZ = 64;
    public static final int SignCrossX = 25;

    private Scp3008Painter() {
    }

    public static void paintArea(int MinX, int MinZ, int MaxX, int MaxZ, Scp3008Canvas Canvas) {
        for (int Ux = unitX(MinX); Ux <= unitX(MaxX); Ux++) {
            for (int Uz = unitZ(MinZ); Uz <= unitZ(MaxZ); Uz++) {
                paint(Ux, Uz, Canvas);
            }
        }
    }

    public static void paint(int Ux, int Uz, Scp3008Canvas Canvas) {
        int X0 = originX(Ux);
        int Z0 = originZ(Uz);
        if (!Canvas.overlaps(X0, Z0, X0 + Unit - 1, Z0 + Row - 1)) {
            return;
        }
        int Department = cachedDepartment(Ux, Uz);
        Shape Draw = new Shape(Canvas, X0, Z0);
        floors(Draw, Department);
        Canvas.fill(X0, CeilingY, Z0, X0 + Unit - 1, CeilingTop, Z0 + Row - 1, of(Scp3008Code.Ceiling, 0));
        markings(Draw);
        pillars(Draw, Ux, Uz);
        lights(Draw);
        signs(Draw, Department);
        for (Scp3008Plan.Plot Next : plots(Ux, Uz)) {
            switch (Next.Type()) {
                case RoomPlot -> rooms(Draw, Department, Next);
                case MarketPlot -> market(Draw, Department, Next);
                case GardenPlot -> garden(Draw, Next);
                case RackPlot -> racks(Draw, Next);
                case FoodPlot -> foodCourt(Draw, Next);
                case ExitPlot -> exit(Draw, Next);
                default -> storage(Draw, Next);
            }
        }
    }

    private record Shape(Scp3008Canvas Canvas, int X0, int Z0) {
        void set(int Lx, int Y, int Lz, int Code) {
            Canvas.set(X0 + Lx, Y, Z0 + Lz, Code);
        }

        void fill(int LowX, int LowY, int LowZ, int HighX, int HighY, int HighZ, int Code) {
            Canvas.fill(X0 + LowX, LowY, Z0 + LowZ, X0 + HighX, HighY, Z0 + HighZ, Code);
        }

        boolean near(int LowX, int LowZ, int HighX, int HighZ) {
            return Canvas.overlaps(X0 + LowX, Z0 + LowZ, X0 + HighX, Z0 + HighZ);
        }
    }

    private static void floors(Shape Draw, int Department) {
        int Plain = of(Scp3008Code.Floor, StyleConcrete);
        Draw.fill(0, FloorBase, 0, Unit - 1, FloorY - 1, Row - 1, Plain);
        Draw.fill(0, FloorY, 0, Lane - 1, FloorY, Row - 1, Plain);
        Draw.fill(Lane, FloorY, 0, Unit - 1, FloorY, Lane - 1, Plain);
        Draw.fill(Lane, FloorY, Lane, Unit - 1, FloorY, Row - 1, of(Scp3008Code.Floor, Department + 1));
    }

    private static void markings(Shape Draw) {
        Draw.fill(0, FloorY, Lane, 0, FloorY, Row - 1, of(Line, 1));
        Draw.fill(Lane - 1, FloorY, Lane, Lane - 1, FloorY, Row - 1, of(Line, 1));
        Draw.fill(Lane, FloorY, 0, Unit - 1, FloorY, 0, of(Line, 0));
        Draw.fill(Lane, FloorY, Lane - 1, Unit - 1, FloorY, Lane - 1, of(Line, 0));
        for (int Lz = 15; Lz < Row; Lz += 10) {
            Draw.set(1, FloorY, Lz, of(Arrow, 0, South));
        }
        for (int Lz = 20; Lz < Row; Lz += 10) {
            Draw.set(Lane - 2, FloorY, Lz, of(Arrow, 0, North));
        }
        for (int Lx = 15; Lx < Unit; Lx += 10) {
            Draw.set(Lx, FloorY, 1, of(Arrow, 0, West));
        }
        for (int Lx = 20; Lx < Unit; Lx += 10) {
            Draw.set(Lx, FloorY, Lane - 2, of(Arrow, 0, East));
        }
    }

    private static void pillars(Shape Draw, int Ux, int Uz) {
        for (int Index = 0; Index < PillarRows.length; Index++) {
            int Lz = PillarRows[Index];
            if (!Draw.near(PillarLow - 1, Lz - 1, PillarHigh + 1, Lz + 4)) {
                continue;
            }
            Draw.fill(PillarLow, Stand, Lz, PillarHigh, Stand + 1, Lz + 3, of(Pillar, PartBase));
            Draw.fill(PillarLow, Stand + 2, Lz, PillarHigh, BandY - 1, Lz + 3, of(Pillar, PartShaft));
            Draw.fill(PillarLow, BandY, Lz, PillarHigh, BandY, Lz + 3, of(Pillar, PartBand));
            Draw.fill(PillarLow, BandY + 1, Lz, PillarHigh, CeilingY - 1, Lz + 3, of(Pillar, PartShaft));
            if (Scp3008Hash.pick(Scp3008Hash.mix(Ux, Uz, Index * 7 + 71), EmergencyOdds) == 0) {
                int X = Draw.X0() + PillarHigh + 1;
                int Z = Draw.Z0() + Lz + 1;
                Draw.set(PillarHigh + 1, EmergencyY, Lz + 1, of(Emergency, arrowToExit(X, Z, East), East));
                Draw.set(PillarLow - 1, EmergencyY, Lz + 2, of(Emergency, arrowToExit(X - 5, Z + 1, West), West));
            }
        }
    }

    public static int arrowToExit(int X, int Z, int Facing) {
        int[] Door = nearestExit(X, Z);
        int Dx = Door[0] + 1 - X;
        int Dz = Door[2] - 1 - Z;
        int Toward = Math.abs(Dx) >= Math.abs(Dz) ? (Dx >= 0 ? East : West) : (Dz >= 0 ? South : North);
        if (Toward == Facing) {
            return ArrowBack;
        }
        if (Toward == opposite(Facing)) {
            return ArrowAhead;
        }
        return Toward == clockwise(Facing) ? ArrowLeft : ArrowRight;
    }

    private static void cable(Shape Draw, int Lx, int FromY, int Lz) {
        Draw.set(Lx, FromY, Lz, of(Cable, CableClamp));
        Draw.fill(Lx, FromY + 1, Lz, Lx, CeilingY - 2, Lz, of(Cable, CableSpan));
        Draw.set(Lx, CeilingY - 1, Lz, of(Cable, CableCanopy));
    }

    public static int[] hangers(int[] Span) {
        return new int[]{Span[0] + 1, Span[1] - 1};
    }

    private static void lights(Shape Draw) {
        for (int Lz = LightFirst; Lz < Row; Lz += LightStep) {
            for (int[] Span : ContentLights) {
                if (!Draw.near(Span[0], Lz, Span[1], Lz)) {
                    continue;
                }
                Draw.fill(Span[0], LightY, Lz, Span[1], LightY, Lz, of(Light, 0));
                for (int Lx : hangers(Span)) {
                    cable(Draw, Lx, LightY + 1, Lz);
                }
            }
        }
    }

    private static void signs(Shape Draw, int Department) {
        if (Draw.near(PillarLow, SignLaneZ, PillarHigh, SignLaneZ)) {
            for (int Part = 0; Part < 4; Part++) {
                Draw.set(PillarLow + Part, SignTop, SignLaneZ, of(Sign, sign(Department, Part, 0)));
                Draw.set(PillarLow + Part, SignBottom, SignLaneZ, of(Sign, sign(Department, Part + 4, 0)));
            }
            cable(Draw, PillarLow, SignTop + 1, SignLaneZ);
            cable(Draw, PillarHigh, SignTop + 1, SignLaneZ);
        }
        if (Draw.near(SignCrossX, PillarLow, SignCrossX, PillarHigh)) {
            for (int Part = 0; Part < 4; Part++) {
                Draw.set(SignCrossX, SignTop, PillarHigh - Part, of(Sign, sign(Department, Part, 1)));
                Draw.set(SignCrossX, SignBottom, PillarHigh - Part, of(Sign, sign(Department, Part + 4, 1)));
            }
            cable(Draw, SignCrossX, SignTop + 1, PillarLow);
            cable(Draw, SignCrossX, SignTop + 1, PillarHigh);
        }
    }

    private static int salt(Scp3008Plan.Plot Next, int Index) {
        return Scp3008Hash.mix(Next.Salt(), Index, 97);
    }

    private static void rooms(Shape Draw, int Department, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        int Wall = of(DisplayWall, 0);
        Draw.fill(24, Stand, S, 25, Stand + 4, S + L - 1, Wall);
        room(Draw, Department, Next, true);
        room(Draw, Department, Next, false);
    }

    public static final int RoomDepth = 14;
    public static final int CrossDepth = 8;
    public static final int WallFront = 5;
    public static final int WallBack = 13;
    public static final int[] BeamRows = {WallFront, CrossDepth, CrossDepth + 3};
    public static final int NorthArm = 1;
    public static final int EastArm = 2;
    public static final int SouthArm = 4;
    public static final int WestArm = 8;
    public static final int RafterY = Stand + 4;

    public static boolean beamRow(int Depth) {
        for (int Row : BeamRows) {
            if (Row == Depth) {
                return true;
            }
        }
        return false;
    }

    public static int roomX(boolean West, int Depth) {
        return West ? Lane + Depth : Unit - 1 - Depth;
    }

    private static void room(Shape Draw, int Department, Scp3008Plan.Plot Next, boolean West) {
        int S = Next.Start();
        int L = Next.Length();
        int W = L - 2;
        int Wall = of(DisplayWall, 0);
        int Base = West ? 0 : 8;
        int Near = roomX(West, WallFront);
        int Back = roomX(West, WallBack);
        Draw.fill(Near, Stand, S, Back, RafterY - 1, S, Wall);
        Draw.fill(Near, Stand, S + L - 1, Back, RafterY - 1, S + L - 1, Wall);
        int Spine = West ? EastArm : WestArm;
        int Aisle = West ? WestArm : EastArm;
        for (int D = WallFront; D <= WallBack; D++) {
            int X = roomX(West, D);
            int Along = Spine | (D > WallFront ? Aisle : 0);
            boolean Row = beamRow(D);
            Draw.set(X, RafterY, S, of(Rafter, Along | (Row ? SouthArm : 0)));
            Draw.set(X, RafterY, S + L - 1, of(Rafter, Along | (Row ? NorthArm : 0)));
            if (Row) {
                Draw.fill(X, RafterY, S + 1, X, RafterY, S + L - 2, of(Rafter, NorthArm | SouthArm));
            }
        }
        int[] Area = rugArea(Department, W);
        if (Area == null) {
            return;
        }
        int Colour = rugFor(Department, salt(Next, Base + 1));
        for (int D = Area[0]; D <= Area[1]; D++) {
            for (int Across = Area[2]; Across <= Area[3]; Across++) {
                boolean Edge = D == Area[0] || D == Area[1] || Across == Area[2] || Across == Area[3];
                Draw.set(roomX(West, D), FloorY, S + 1 + Across, of(Scp3008Code.Floor, rugStyle(Colour, Edge)));
            }
        }
    }

    private static int goods(int Department, int Hash) {
        int[] Pool = goodsFor(Department);
        return Pool[Scp3008Hash.pick(Hash, Pool.length)];
    }

    private static void market(Shape Draw, int Department, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        int Index = 0;
        for (int R = S + 1; R + 1 <= S + L - 2; R += 5) {
            for (int Segment = 12; Segment <= 37; Segment += 4) {
                int Hash = salt(Next, Index * 16 + Segment);
                int Goods = goods(Department, Hash);
                boolean Topped = Scp3008Hash.chance(Hash >>> 4, 0.2);
                for (int Lx = Segment; Lx < Segment + 4 && Lx <= 37; Lx++) {
                    if (Lx == 24 || Lx == 25) {
                        continue;
                    }
                    Draw.fill(Lx, Stand, R, Lx, Stand + 1, R, of(Shelf, Goods, North));
                    Draw.fill(Lx, Stand, R + 1, Lx, Stand + 1, R + 1, of(Shelf, Goods, South));
                    if (Topped) {
                        Draw.set(Lx, Stand + 2, R, of(Crate, crate(false, false), North));
                    }
                }
            }
            int EndHash = salt(Next, Index * 16 + 3);
            if (Scp3008Hash.chance(EndHash, 0.55)) {
                Draw.set(11, Stand, R, of(Crate, crate(true, false), Scp3008Code.West));
            }
            if (Scp3008Hash.chance(EndHash >>> 5, 0.55)) {
                Draw.set(38, Stand, R + 1, of(Crate, crate(true, false), Scp3008Code.East));
            }
            Index++;
        }
    }

    private static void storage(Shape Draw, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        int Index = 0;
        for (int Lx : new int[]{12, 15, 18, 21, 28, 31, 34, 37}) {
            for (int Lz = S + 1; Lz <= S + L - 2; Lz += 3) {
                int Hash = salt(Next, Index++);
                if (!Scp3008Hash.chance(Hash, 0.8)) {
                    continue;
                }
                int Facing = Lx < 25 ? Scp3008Code.West : Scp3008Code.East;
                Draw.set(Lx, Stand, Lz, of(Crate, crate(Scp3008Hash.chance(Hash >>> 6, 0.45), false), Facing));
                if (Scp3008Hash.chance(Hash >>> 12, 0.45)) {
                    Draw.set(Lx, Stand + 1, Lz, of(Crate, crate(false, false), Facing));
                }
            }
        }
    }

    private static void garden(Shape Draw, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        int Index = 0;
        for (int R = S + 1; R <= S + L - 2; R += 4) {
            for (int Lx = 12; Lx <= 37; Lx++) {
                if (Lx == 24 || Lx == 25) {
                    continue;
                }
                int Hash = salt(Next, Index * 40 + Lx);
                int Variant = (R == S + 1 || R + 4 > S + L - 2) && Lx % 6 == 0 ? 3 : Scp3008Hash.pick(Hash, 3);
                Draw.set(Lx, Stand, R, of(Planter, Variant));
            }
            int EndHash = salt(Next, Index * 40 + 1);
            if (Scp3008Hash.chance(EndHash, 0.6)) {
                Draw.set(11, Stand, R, of(Crate, crate(true, false), Scp3008Code.West));
            }
            if (Scp3008Hash.chance(EndHash >>> 5, 0.6)) {
                Draw.set(38, Stand, R, of(Crate, crate(true, false), Scp3008Code.East));
            }
            Index++;
        }
    }

    private static void racks(Shape Draw, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        int Top = Stand + 12;
        for (int Pair : new int[]{12, 18, 24, 30, 36}) {
            for (int Lx = Pair; Lx <= Pair + 1; Lx++) {
                int Facing = Lx == Pair ? Scp3008Code.West : Scp3008Code.East;
                for (int Lz = S; Lz < S + L; Lz++) {
                    if ((Lz - S) % 4 == 0) {
                        for (int Y = Stand; Y <= Top; Y++) {
                            boolean Beam = (Y - Stand) % 3 == 2 && Y < Top;
                            Draw.set(Lx, Y, Lz, of(RackPost, Scp3008Code.rackPost(true, Beam && Lz > S, Beam && Lz < S + L - 1, Y == Stand)));
                        }
                        continue;
                    }
                    for (int Level = 1; Level <= 4; Level++) {
                        Draw.set(Lx, Stand - 1 + Level * 3, Lz, of(RackBeam, 1));
                    }
                    for (int Level = 0; Level <= 4; Level++) {
                        int Hash = salt(Next, (Lx * 131 + Lz) * 8 + Level);
                        if (Scp3008Hash.chance(Hash, 0.55)) {
                            Draw.set(Lx, Stand + Level * 3, Lz, of(Crate, crate(Scp3008Hash.chance(Hash >>> 7, 0.3), false), Facing));
                        }
                    }
                }
            }
        }
    }

    private static void foodCourt(Shape Draw, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        Draw.fill(Lane, FloorY, S, Unit - 1, FloorY, S + L - 1, of(Scp3008Code.Floor, StyleFoodCourt));
        Draw.fill(38, Stand, S + 3, 39, Stand + 4, S + L - 4, of(DisplayWall, 0));
        for (int Centre : new int[]{S + 8, S + 17, S + 26}) {
            for (int Part = 0; Part < 3; Part++) {
                Draw.set(37, Stand + 3, Centre - 1 + Part, of(Menu, Part, Scp3008Code.West));
            }
        }
        Draw.fill(34, Stand, S + 6, 34, Stand, S + L - 7, of(Counter, 0, Scp3008Code.West));
        Draw.set(36, Stand, S + 4, of(Crate, crate(true, true), Scp3008Code.West));
        Draw.set(36, Stand, S + L - 5, of(Crate, crate(true, true), Scp3008Code.West));
        int Index = 0;
        for (int Lx : new int[]{14, 20, 26}) {
            for (int Lz = S + 5; Lz <= S + L - 5; Lz += 6) {
                Draw.set(Lx, Stand, Lz, of(FoodTable, 0));
                Draw.set(Lx, Stand, Lz - 1, of(Stool, 0, South));
                Draw.set(Lx, Stand, Lz + 1, of(Stool, 0, North));
                int Hash = salt(Next, Index++);
                double Roll = Scp3008Hash.unit(Hash);
                int Food = Roll < 0.3 ? FoodNone : Roll < 0.62 ? FoodMeatballs : Roll < 0.77 ? FoodFries : Roll < 0.9 ? FoodDrink : FoodJam;
                if (Food != FoodNone) {
                    Draw.set(Lx, Stand + 1, Lz, of(Tray, Food));
                }
            }
        }
    }

    private static void exit(Shape Draw, Scp3008Plan.Plot Next) {
        int S = Next.Start();
        int L = Next.Length();
        if (!Draw.near(Lane, S, Unit - 1, S + L - 1)) {
            return;
        }
        int WallZ = S + ExitWallOffset;
        Draw.fill(18, FloorY, S + 2, 31, FloorY, WallZ - 1, of(Hatch, 0));
        Draw.fill(12, Stand, WallZ, 37, Stand + 5, WallZ + 1, of(ExitWall, 0));
        Draw.set(ExitDoorX, Stand, WallZ, of(ExitDoor, door(false, true), North));
        Draw.set(ExitDoorX, Stand + 1, WallZ, of(ExitDoor, door(true, true), North));
        Draw.set(ExitDoorX + 1, Stand, WallZ, of(ExitDoor, door(false, false), North));
        Draw.set(ExitDoorX + 1, Stand + 1, WallZ, of(ExitDoor, door(true, false), North));
        Draw.set(ExitDoorX + 1, Stand + 3, WallZ - 1, of(ExitSign, 0, North));
        Draw.set(ExitDoorX, Stand + 3, WallZ - 1, of(ExitSign, 1, North));
        Draw.set(ExitDoorX - 2, Stand + 2, WallZ - 1, of(Emergency, ArrowAhead, North));
        Draw.set(ExitDoorX + 3, Stand + 2, WallZ - 1, of(Emergency, ArrowAhead, North));
        for (int Lx = 11; Lx <= 17; Lx += 2) {
            Draw.set(Lx, FloorY, S + 8, of(Arrow, 0, Scp3008Code.East));
        }
        for (int Lx = 32; Lx <= 38; Lx += 2) {
            Draw.set(Lx, FloorY, S + 8, of(Arrow, 0, Scp3008Code.West));
        }
    }
}
