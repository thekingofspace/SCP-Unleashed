package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuinDecay {
    public record Wear(double Crack, double Scorch, double Collapse, double Holes, double Growth, double Rubble, int KeepHeight) {
    }

    private static final int[][] Sides = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final int[][] Around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private RuinDecay() {
    }

    public static void apply(RuinCanvas Canvas, Wear Settings) {
        weather(Canvas, Settings.Crack());
        scorch(Canvas, Settings.Scorch());
        collapse(Canvas, Settings.Collapse(), Settings.KeepHeight());
        holes(Canvas, Settings.Holes(), Settings.KeepHeight());
        List<long[]> Fallen = gravity(Canvas.plan());
        debris(Canvas, Fallen, Settings.Rubble());
        rubble(Canvas, Settings.Rubble());
        growth(Canvas, Settings.Growth());
        settle(Canvas.plan());
        secure(Canvas.plan());
        settle(Canvas.plan());
    }

    public static boolean standable(RuinPlan Plan, int X, int Y, int Z) {
        if (Plan.filled(X, Y, Z) || Plan.filled(X, Y + 1, Z)) {
            return false;
        }
        RuinMat Below = Plan.matAt(X, Y - 1, Z);
        return fullTop(Plan, X, Y - 1, Z) || Below.shape() == RuinMat.Shape.Slab || Below.shape() == RuinMat.Shape.Stairs;
    }

    public static void secure(RuinPlan Plan) {
        List<int[]> Kept = new ArrayList<>();
        for (int[] Spot : Plan.marks(RuinPlan.Guardian)) {
            int[] Found = null;
            for (int Reach = 0; Reach <= 4 && Found == null; Reach++) {
                for (int Dy = 0; Dy >= -4 && Found == null; Dy--) {
                    for (int Dx = -Reach; Dx <= Reach && Found == null; Dx++) {
                        for (int Dz = -Reach; Dz <= Reach && Found == null; Dz++) {
                            if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Reach) {
                                continue;
                            }
                            int X = Spot[0] + Dx;
                            int Y = Spot[1] + Dy;
                            int Z = Spot[2] + Dz;
                            if (standable(Plan, X, Y, Z)) {
                                Found = new int[]{X, Y, Z, Spot[3], Spot[4]};
                            }
                        }
                    }
                }
            }
            if (Found != null) {
                Kept.add(Found);
            }
        }
        Plan.dropMarks(RuinPlan.Guardian);
        for (int[] Spot : Kept) {
            Plan.mark(RuinPlan.Guardian, Spot[0], Spot[1], Spot[2], Spot[3], Spot[4]);
        }
        for (long Key : Plan.keys()) {
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            if (Mat != RuinMat.Chest && Mat != RuinMat.TrappedChest && Mat != RuinMat.Spawner && Mat != RuinMat.Barrel && Mat != RuinMat.DecoratedPot) {
                continue;
            }
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (!fullTop(Plan, X, Y - 1, Z)) {
                int Drop = Y - 1;
                while (Drop > Plan.low()[1] && !fullTop(Plan, X, Drop - 1, Z) && !Plan.filled(X, Drop - 1, Z)) {
                    Drop--;
                }
                if (Drop < Y && fullTop(Plan, X, Drop - 1, Z) && !Plan.filled(X, Drop, Z)) {
                    Plan.force(X, Y, Z, RuinMat.Air, RuinMat.South, 0);
                    Plan.force(X, Drop, Z, Mat, (Code >> 8) & 7, (Code >> 12) & 255);
                    Plan.keep(X, Drop, Z);
                    Y = Drop;
                } else {
                    Plan.force(X, Y - 1, Z, RuinMat.SmoothBasalt, RuinMat.Up, 0);
                    Plan.keep(X, Y - 1, Z);
                }
            }
            if (Mat != RuinMat.Chest && Mat != RuinMat.TrappedChest) {
                continue;
            }
            RuinMat Above = Plan.matAt(X, Y + 1, Z);
            if (Plan.filled(X, Y + 1, Z) && (Above.isOpaqueCube() || Above.carriesLoad()) && !Plan.kept(X, Y + 1, Z)) {
                Plan.clearCell(X, Y + 1, Z);
            }
        }
    }

    public static void settle(RuinPlan Plan) {
        for (int Round = 0; Round < 4; Round++) {
            gravity(Plan);
            if (attachments(Plan) == 0) {
                break;
            }
        }
    }

    private static void weather(RuinCanvas Canvas, double Amount) {
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        for (long Key : Plan.keys()) {
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Plan.kept(X, Y, Z)) {
                continue;
            }
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            double Roll = Random.at(X, Y, Z, 101);
            if (Roll >= Amount) {
                continue;
            }
            double Pick = Random.at(X, Y, Z, 102);
            RuinMat Aged = switch (Mat) {
                case CutRedSandstone -> Pick < 0.6 ? RuinMat.RedSandstone : RuinMat.SmoothRedSandstone;
                case SmoothRedSandstone -> RuinMat.RedSandstone;
                case ChiseledRedSandstone -> Pick < 0.5 ? RuinMat.CutRedSandstone : RuinMat.ChiseledRedSandstone;
                case BlackstoneBricks -> RuinMat.CrackedBlackstoneBricks;
                case PolishedBlackstone, ChiseledBlackstone -> Pick < 0.5 ? RuinMat.Blackstone : RuinMat.CrackedBlackstoneBricks;
                case Copper -> RuinMat.AgedCopper;
                case RedNetherBricks -> Pick < 0.5 ? RuinMat.CrackedNetherBricks : RuinMat.RedNetherBricks;
                case PolishedGranite -> RuinMat.Granite;
                case GoldBlock -> RuinMat.RawGoldBlock;
                case CrimsonPlanks -> RuinMat.DarkOakPlanks;
                case RedWool -> Pick < 0.3 ? RuinMat.NetherWartBlock : RuinMat.RedWool;
                default -> Mat;
            };
            if (Aged != Mat) {
                Plan.force(X, Y, Z, Aged, (Code >> 8) & 7, (Code >> 12) & 255);
            }
        }
    }

    private static void scorch(RuinCanvas Canvas, double Amount) {
        if (Amount <= 0) {
            return;
        }
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        for (long Key : Plan.keys()) {
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Plan.kept(X, Y, Z)) {
                continue;
            }
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            double Patch = Random.smooth3(X, Y, Z, 4.0, 211) * 0.7 + Random.at(X, Y, Z, 212) * 0.3;
            if (Patch > Amount * 0.95) {
                continue;
            }
            double Pick = Random.at(X, Y, Z, 213);
            int Facing = (Code >> 8) & 7;
            int Variant = (Code >> 12) & 255;
            switch (Mat) {
                case RuinBrick, RedNetherBricks -> Plan.force(X, Y, Z, Pick < 0.55 ? RuinMat.NetherBricks : RuinMat.CrackedNetherBricks, RuinMat.Up, 0);
                case RedSandstone, CutRedSandstone, SmoothRedSandstone, Granite, PolishedGranite -> Plan.force(X, Y, Z, Pick < 0.55 ? RuinMat.BrownTerracotta
                        : Pick < 0.8 ? RuinMat.Blackstone : RuinMat.Basalt, RuinMat.Up, 0);
                case Terracotta, RedTerracotta -> Plan.force(X, Y, Z, RuinMat.BrownTerracotta, RuinMat.Up, 0);
                case RedSandstoneSlab, CutRedSandstoneSlab, SmoothRedSandstoneSlab, PolishedGraniteSlab -> Plan.force(X, Y, Z, RuinMat.BlackstoneSlab, Facing, Variant);
                case RedSandstoneStairs, SmoothRedSandstoneStairs, PolishedGraniteStairs -> Plan.force(X, Y, Z, RuinMat.BlackstoneStairs, Facing, Variant);
                case RedSandstoneWall, GraniteWall -> Plan.force(X, Y, Z, RuinMat.BlackstoneWall, Facing, Variant);
                case CrimsonPlanks, DarkOakPlanks -> Plan.force(X, Y, Z, Pick < 0.35 ? RuinMat.CoalBlock : RuinMat.DarkOakPlanks, Facing, Variant);
                case CrimsonStem, StrippedCrimsonStem, DarkOakLog, StrippedDarkOakLog -> Plan.force(X, Y, Z, Pick < 0.3 ? RuinMat.CoalBlock : RuinMat.DarkOakLog, Facing, Variant);
                case CrimsonFence, DarkOakFence -> Plan.force(X, Y, Z, RuinMat.NetherBrickFence, Facing, Variant);
                case CrimsonSlab -> Plan.force(X, Y, Z, RuinMat.DarkOakSlab, Facing, Variant);
                case CrimsonStairs -> Plan.force(X, Y, Z, RuinMat.DarkOakStairs, Facing, Variant);
                case RedWool, NetherWartBlock -> {
                    if (Pick < 0.4) {
                        Plan.clearCell(X, Y, Z);
                    } else if (Pick < 0.55) {
                        Plan.force(X, Y, Z, RuinMat.BlackWool, Facing, Variant);
                    } else {
                        Plan.force(X, Y, Z, RuinMat.NetherWartBlock, Facing, Variant);
                    }
                }
                case RedCarpet -> {
                    if (Pick < 0.4) {
                        Plan.clearCell(X, Y, Z);
                    } else {
                        Plan.force(X, Y, Z, RuinMat.BlackCarpet, Facing, Variant);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static Map<Long, Integer> tops(RuinPlan Plan) {
        Map<Long, Integer> Top = new HashMap<>();
        for (long Key : Plan.keys()) {
            int Code = Plan.codeAt(Key);
            if (!RuinMat.byOrdinal(Code & 255).isStructural()) {
                continue;
            }
            long Column = RuinPlan.key(RuinPlan.keyX(Key), 0, RuinPlan.keyZ(Key));
            Top.merge(Column, RuinPlan.keyY(Key), Math::max);
        }
        return Top;
    }

    private static void collapse(RuinCanvas Canvas, double Amount, int Keep) {
        if (Amount <= 0) {
            return;
        }
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        Map<Long, Integer> Top = tops(Plan);
        for (long Key : Plan.keys()) {
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Y <= Keep || Plan.kept(X, Y, Z)) {
                continue;
            }
            RuinMat Mat = Plan.matAt(X, Y, Z);
            if (Mat.isAir()) {
                continue;
            }
            int Height = Top.getOrDefault(RuinPlan.key(X, 0, Z), Y);
            double Shape = Random.smooth(X, Z, 4.5, 301) * 0.75 + Random.at(X, 0, Z, 302) * 0.25;
            double Limit = Keep + (Height - Keep) * (1.0 - Amount * Shape * 1.25) + 0.5;
            if (Y > Limit) {
                Plan.clearCell(X, Y, Z);
            }
        }
    }

    private static void holes(RuinCanvas Canvas, double Amount, int Keep) {
        if (Amount <= 0) {
            return;
        }
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        Map<Long, Integer> Top = tops(Plan);
        for (long Key : Plan.keys()) {
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Y <= Keep || Plan.kept(X, Y, Z)) {
                continue;
            }
            RuinMat Mat = Plan.matAt(X, Y, Z);
            if (!Mat.isStructural()) {
                continue;
            }
            int Height = Math.max(1, Top.getOrDefault(RuinPlan.key(X, 0, Z), Y));
            double Bias = 0.35 + 0.65 * Y / (double) Height;
            if (Random.at(X, Y, Z, 401) < Amount * Bias * 0.22) {
                Plan.clearCell(X, Y, Z);
            }
        }
    }

    public static List<long[]> gravity(RuinPlan Plan) {
        Set<Long> Reached = new HashSet<>();
        ArrayDeque<Long> Queue = new ArrayDeque<>();
        for (long Key : Plan.keys()) {
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            if (!Mat.isStructural()) {
                continue;
            }
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Y <= 0 || Y == 1 && !Plan.has(X, 0, Z)) {
                Reached.add(Key);
                Queue.add(Key);
            }
        }
        while (!Queue.isEmpty()) {
            long Key = Queue.poll();
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            for (int[] Step : Around) {
                long Next = RuinPlan.key(X + Step[0], Y + Step[1], Z + Step[2]);
                if (Reached.contains(Next)) {
                    continue;
                }
                int Code = Plan.codeAt(Next);
                if (Code < 0 || !RuinMat.byOrdinal(Code & 255).isStructural()) {
                    continue;
                }
                Reached.add(Next);
                Queue.add(Next);
            }
        }
        List<long[]> Fallen = new ArrayList<>();
        for (long Key : Plan.keys()) {
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            if (Mat.isStructural() && !Reached.contains(Key)) {
                int X = RuinPlan.keyX(Key);
                int Y = RuinPlan.keyY(Key);
                int Z = RuinPlan.keyZ(Key);
                if (Plan.kept(X, Y, Z)) {
                    Plan.force(X, Y, Z, RuinMat.Air, RuinMat.South, 0);
                } else {
                    Plan.clearCell(X, Y, Z);
                }
                Fallen.add(new long[]{X, Y, Z, Mat.ordinal()});
            }
        }
        return Fallen;
    }

    public static boolean fullTop(RuinPlan Plan, int X, int Y, int Z) {
        if (Y == 0 && !Plan.has(X, 0, Z)) {
            return true;
        }
        RuinMat Mat = Plan.matAt(X, Y, Z);
        int Variant = Plan.variantAt(X, Y, Z);
        return switch (Mat.shape()) {
            case Cube, Pillar, See, Brush, Ground, Directional, Spawner -> true;
            case Slab -> Variant != 0;
            case Stairs -> (Variant & 1) != 0;
            default -> false;
        };
    }

    public static boolean soil(RuinPlan Plan, int X, int Y, int Z) {
        if (Y == 0 && !Plan.has(X, 0, Z)) {
            return true;
        }
        RuinMat Mat = Plan.matAt(X, Y, Z);
        return Mat == RuinMat.CoarseDirt || Mat == RuinMat.Dirt || Mat == RuinMat.MossBlock || Mat == RuinMat.Ground;
    }

    public static boolean supported(RuinPlan Plan, int X, int Y, int Z, RuinMat Mat, int Facing, int Variant) {
        boolean Below = fullTop(Plan, X, Y - 1, Z);
        return switch (Mat.shape()) {
            case Plant, Growth -> soil(Plan, X, Y - 1, Z);
            case Carpet -> Plan.filled(X, Y - 1, Z) || Y == 1 && !Plan.has(X, 0, Z);
            case Candle, Plate, Fire, Campfire -> Below;
            case Lantern -> (Variant & 1) != 0 ? Plan.matAt(X, Y + 1, Z) == RuinMat.Chain || fullBottom(Plan, X, Y + 1, Z) : Below;
            case Banner -> {
                int[] Back = RuinMat.step(RuinMat.opposite(Facing));
                yield Plan.solidAt(X + Back[0], Y, Z + Back[2]);
            }
            case Vine -> {
                int[] Side = RuinMat.step(Facing);
                yield Plan.solidAt(X + Side[0], Y, Z + Side[2]) || Plan.matAt(X, Y + 1, Z) == RuinMat.Vine && Plan.facingAt(X, Y + 1, Z) == Facing;
            }
            case Web -> {
                boolean Any = false;
                for (int[] Step : Around) {
                    if (Plan.solidAt(X + Step[0], Y + Step[1], Z + Step[2])) {
                        Any = true;
                    }
                }
                yield Any || Y <= 1;
            }
            case Hang -> fullBottom(Plan, X, Y + 1, Z);
            default -> true;
        };
    }

    public static boolean fullBottom(RuinPlan Plan, int X, int Y, int Z) {
        RuinMat Mat = Plan.matAt(X, Y, Z);
        int Variant = Plan.variantAt(X, Y, Z);
        return switch (Mat.shape()) {
            case Cube, Pillar, See, Brush, Directional, Spawner -> true;
            case Slab -> Variant != 1;
            case Stairs -> (Variant & 1) == 0;
            default -> false;
        };
    }

    public static int attachments(RuinPlan Plan) {
        int Removed = 0;
        for (long Key : Plan.keys()) {
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            if (!Mat.isAttachment()) {
                continue;
            }
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (!supported(Plan, X, Y, Z, Mat, (Code >> 8) & 7, (Code >> 12) & 255)) {
                Plan.force(X, Y, Z, RuinMat.Air, RuinMat.South, 0);
                Removed++;
            }
        }
        return Removed;
    }

    private static void debris(RuinCanvas Canvas, List<long[]> Fallen, double Amount) {
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        for (long[] Piece : Fallen) {
            int X = (int) Piece[0];
            int Y = (int) Piece[1];
            int Z = (int) Piece[2];
            if (Random.at(X, Y, Z, 501) > 0.3 + Amount * 0.4) {
                continue;
            }
            int Drift = Random.at(X, Y, Z, 502) < 0.5 ? 0 : 1;
            int[] Side = Sides[(int) (Random.at(X, Y, Z, 503) * 4) & 3];
            int Lx = X + Side[0] * Drift;
            int Lz = Z + Side[2] * Drift;
            int Land = Y - 1;
            while (Land > Plan.low()[1] && !Plan.filled(Lx, Land, Lz)) {
                Land--;
            }
            if (Land < 0 && !Plan.has(Lx, 0, Lz)) {
                Land = 0;
            }
            int At = Land + 1;
            if (At < 1 || At > Y || Plan.filled(Lx, At, Lz) || Plan.mustStayClear(Lx, At, Lz)) {
                continue;
            }
            RuinMat Below = Plan.matAt(Lx, Land, Lz);
            if (Land > 0 && !Below.carriesLoad()) {
                continue;
            }
            RuinMat Original = RuinMat.byOrdinal((int) Piece[3]);
            RuinMat Chunk = Original.isOpaqueCube() && Random.at(X, Y, Z, 504) < 0.5 ? Original : RuinMix.Rubble.at(Random, Lx, At, Lz);
            Plan.set(Lx, At, Lz, Chunk, RuinMat.Up, 0);
        }
    }

    private static void rubble(RuinCanvas Canvas, double Amount) {
        if (Amount <= 0) {
            return;
        }
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        Set<Long> Done = new HashSet<>();
        for (long Key : Plan.keys()) {
            int Y = RuinPlan.keyY(Key);
            if (Y < 2) {
                continue;
            }
            int Code = Plan.codeAt(Key);
            if (!RuinMat.byOrdinal(Code & 255).isOpaqueCube()) {
                continue;
            }
            int X = RuinPlan.keyX(Key);
            int Z = RuinPlan.keyZ(Key);
            for (int[] Side : Sides) {
                int Nx = X + Side[0];
                int Nz = Z + Side[2];
                long Spot = RuinPlan.key(Nx, 1, Nz);
                if (Done.contains(Spot) || Plan.filled(Nx, 1, Nz) || Plan.mustStayClear(Nx, 1, Nz)) {
                    continue;
                }
                Done.add(Spot);
                boolean Floor = Plan.solidAt(Nx, 0, Nz) || !Plan.has(Nx, 0, Nz);
                if (!Floor || Random.at(Nx, 1, Nz, 601) > Amount * 0.28) {
                    continue;
                }
                RuinMat Piece = RuinMix.Rubble.at(Random, Nx, 1, Nz);
                Plan.set(Nx, 1, Nz, Piece, RuinMat.Up, 0);
            }
        }
    }

    private static boolean openSky(RuinPlan Plan, int X, int Y, int Z) {
        int Top = Plan.high()[1];
        for (int Up = Y + 1; Up <= Top; Up++) {
            if (Plan.filled(X, Up, Z)) {
                return false;
            }
        }
        return true;
    }

    private static void growth(RuinCanvas Canvas, double Amount) {
        if (Amount <= 0) {
            return;
        }
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        List<Long> Keys = Plan.keys();
        for (long Key : Keys) {
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (!Mat.carriesLoad() || Plan.kept(X, Y, Z)) {
                continue;
            }
            boolean Exposed = !Plan.filled(X, Y + 1, Z) && !Plan.mustStayClear(X, Y + 1, Z);
            if (Exposed && Y >= 0) {
                double Roll = Random.at(X, Y, Z, 701);
                double Patch = Random.smooth(X + Y * 7, Z, 3.0, 702);
                if (Mat.isOpaqueCube() && Y >= 1 && Roll < Amount * 0.12 * Patch && !Plan.kept(X, Y, Z)) {
                    Plan.force(X, Y, Z, RuinMat.MossBlock, RuinMat.Up, 0);
                }
                double Cover = Random.at(X, Y + 1, Z, 703);
                boolean Soil = soil(Plan, X, Y, Z);
                if (Cover < Amount * (Soil ? (Y <= 0 ? 0.42 : 0.2) : 0.08) * (0.4 + Patch)) {
                    if (Soil) {
                        Plan.set(X, Y + 1, Z, RuinMat.Growth, RuinMat.South, 0);
                    } else if (Mat.isOpaqueCube() || Mat.shape() == RuinMat.Shape.Slab || Mat.shape() == RuinMat.Shape.Stairs) {
                        Plan.set(X, Y + 1, Z, RuinMat.MossCarpet, RuinMat.South, 0);
                    }
                }
            }
            if (Y < 2 || !Mat.isOpaqueCube()) {
                continue;
            }
            for (int Index = 0; Index < 4; Index++) {
                int[] Side = Sides[Index];
                int Vx = X + Side[0];
                int Vz = Z + Side[2];
                if (Plan.filled(Vx, Y, Vz) || Plan.mustStayClear(Vx, Y, Vz) || !openSky(Plan, Vx, Y, Vz)) {
                    continue;
                }
                double Roll = Random.at(Vx, Y, Vz, 704 + Index);
                if (Roll > Amount * 0.09 * (0.3 + Random.smooth(Vx, Vz + Y * 5, 4.0, 708))) {
                    continue;
                }
                int Facing = Side[0] > 0 ? RuinMat.West : Side[0] < 0 ? RuinMat.East : Side[2] > 0 ? RuinMat.North : RuinMat.South;
                int Length = 1 + (int) (Random.at(Vx, Y, Vz, 709) * Math.min(6, Y));
                for (int Down = 0; Down < Length; Down++) {
                    int Vy = Y - Down;
                    if (Vy < 1 || Plan.filled(Vx, Vy, Vz) || Plan.mustStayClear(Vx, Vy, Vz)) {
                        break;
                    }
                    Plan.set(Vx, Vy, Vz, RuinMat.Vine, Facing, 0);
                }
            }
        }
    }
}
