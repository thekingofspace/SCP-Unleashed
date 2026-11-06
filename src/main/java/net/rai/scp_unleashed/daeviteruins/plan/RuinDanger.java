package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class RuinDanger {
    public static final String TrapPlate = "trap_plate";
    public static final String TrapChest = "trap_chest";
    public static final String TrapTread = "trap_tread";
    public static final String TrapDispenser = "trap_dispenser";
    public static final String TrapFall = "trap_fall";
    public static final int Husk = 0;
    public static final int Skeleton = 1;
    public static final int Stray = 2;
    public static final int Vindicator = 3;
    public static final int WitherSkeleton = 4;
    public static final int Evoker = 5;
    public static final int Pillager = 6;
    public static final int Blaze = 7;
    public static final int Arrows = 0;
    public static final int FireCharges = 1;
    public static final int SlowArrows = 2;
    public static final int PoisonArrows = 3;
    public static final int PitMagma = 0;
    public static final int PitWebs = 1;
    public static final int PitBones = 2;

    private RuinDanger() {
    }

    public static int guardianCount(int Tier) {
        return switch (Tier) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 6;
            default -> 9;
        };
    }

    public static int spawnerCount(int Tier) {
        return Tier >= 5 ? 2 : Tier == 4 ? 1 : 0;
    }

    public static int guardianKind(RuinRandom Random, int Tier) {
        double Roll = Random.nextDouble();
        return switch (Tier) {
            case 1, 2 -> Roll < 0.6 ? Husk : Skeleton;
            case 3 -> Roll < 0.38 ? Husk : Roll < 0.68 ? Skeleton : Roll < 0.85 ? Stray : Vindicator;
            case 4 -> Roll < 0.24 ? Husk : Roll < 0.48 ? Skeleton : Roll < 0.62 ? Stray : Roll < 0.9 ? Vindicator : Pillager;
            default -> Roll < 0.18 ? Skeleton : Roll < 0.3 ? Husk : Roll < 0.58 ? Vindicator : Roll < 0.88 ? WitherSkeleton : Pillager;
        };
    }

    public static int spawnerKind(RuinRandom Random, int Tier) {
        double Roll = Random.nextDouble();
        if (Tier >= 5) {
            return Roll < 0.4 ? Blaze : Roll < 0.7 ? Skeleton : Husk;
        }
        return Roll < 0.55 ? Skeleton : Husk;
    }

    public static int dartAmmo(RuinRandom Random, int Tier) {
        double Roll = Random.nextDouble();
        return switch (Tier) {
            case 1, 2 -> Arrows;
            case 3 -> Roll < 0.55 ? Arrows : FireCharges;
            case 4 -> Roll < 0.3 ? Arrows : Roll < 0.65 ? FireCharges : SlowArrows;
            default -> Roll < 0.45 ? FireCharges : Roll < 0.75 ? PoisonArrows : SlowArrows;
        };
    }

    public static boolean dartTrap(RuinCanvas Canvas, int X, int Y, int Z, boolean AlongX, int Reach, int Ammo, boolean Head) {
        RuinPlan Plan = Canvas.plan();
        int Group = Canvas.nextGroup();
        int Height = Head ? Y + 1 : Y;
        List<int[]> Found = new ArrayList<>();
        if (!RuinDecay.fullTop(Plan, X, Y - 1, Z)) {
            return false;
        }
        for (int Sign : new int[]{-1, 1}) {
            int Nx = AlongX ? X + Sign : X;
            int Nz = AlongX ? Z : Z + Sign;
            if (Plan.filled(Nx, Height, Nz) && Plan.matAt(Nx, Height, Nz).isStructural()) {
                continue;
            }
            for (int Distance = 2; Distance <= Reach; Distance++) {
                int Wx = AlongX ? X + Sign * Distance : X;
                int Wz = AlongX ? Z : Z + Sign * Distance;
                if (Plan.filled(Wx, Height, Wz)) {
                    if (Plan.matAt(Wx, Height, Wz).carriesLoad() && Plan.matAt(Wx, Height, Wz) != RuinMat.Dispenser) {
                        Found.add(new int[]{Wx, Height, Wz, AlongX ? (Sign < 0 ? RuinMat.East : RuinMat.West) : (Sign < 0 ? RuinMat.South : RuinMat.North)});
                    }
                    break;
                }
            }
        }
        if (Found.isEmpty()) {
            return false;
        }
        RuinMat Plate = darkFloor(Plan.matAt(X, Y - 1, Z)) ? RuinMat.BlackstonePlate : RuinMat.StonePlate;
        Plan.set(X, Y, Z, Plate, RuinMat.South, 0);
        Plan.keep(X, Y, Z);
        Canvas.protect(X, Y - 1, Z);
        Plan.mark(TrapPlate, X, Y, Z, 0, Group);
        for (int[] Spot : Found) {
            Plan.force(Spot[0], Spot[1], Spot[2], RuinMat.Dispenser, Spot[3], Ammo);
            Plan.keep(Spot[0], Spot[1], Spot[2]);
            Canvas.protect(Spot[0], Spot[1] - 1, Spot[2]);
            Plan.mark(TrapDispenser, Spot[0], Spot[1], Spot[2], Spot[3], Group);
            int[] Step = RuinMat.step(Spot[3]);
            int Cx = Spot[0] + Step[0];
            int Cz = Spot[2] + Step[2];
            while (Cx != X || Cz != Z) {
                Plan.keepClear(Cx, Spot[1], Cz);
                Cx += Step[0];
                Cz += Step[2];
            }
        }
        Plan.keepClear(X, Y + 1, Z);
        return true;
    }

    public static void prune(RuinPlan Plan) {
        Set<Integer> Armed = new HashSet<>();
        for (int[] Spot : Plan.marks(TrapDispenser)) {
            if (Plan.matAt(Spot[0], Spot[1], Spot[2]) == RuinMat.Dispenser) {
                Armed.add(Spot[4]);
            }
        }
        Set<Integer> Triggered = new HashSet<>();
        for (int[] Spot : Plan.marks(TrapPlate)) {
            if (Plan.matAt(Spot[0], Spot[1], Spot[2]).shape() == RuinMat.Shape.Plate) {
                Triggered.add(Spot[4]);
            }
        }
        for (int[] Spot : Plan.marks(TrapChest)) {
            if (Plan.matAt(Spot[0], Spot[1], Spot[2]) == RuinMat.TrappedChest) {
                Triggered.add(Spot[4]);
            }
        }
        filter(Plan, TrapDispenser, Spot -> Plan.matAt(Spot[0], Spot[1], Spot[2]) == RuinMat.Dispenser && Triggered.contains(Spot[4]));
        filter(Plan, TrapPlate, Spot -> Plan.matAt(Spot[0], Spot[1], Spot[2]).shape() == RuinMat.Shape.Plate && Armed.contains(Spot[4]));
        filter(Plan, TrapChest, Spot -> Plan.matAt(Spot[0], Spot[1], Spot[2]) == RuinMat.TrappedChest && Armed.contains(Spot[4]));
        filter(Plan, TrapTread, Spot -> Plan.filled(Spot[0], Spot[1], Spot[2]) && !Plan.filled(Spot[0], Spot[1] - 1, Spot[2]));
    }

    private static void filter(RuinPlan Plan, String Label, Predicate<int[]> Wanted) {
        List<int[]> Kept = new ArrayList<>();
        for (int[] Spot : Plan.marks(Label)) {
            if (Wanted.test(Spot)) {
                Kept.add(Spot);
            }
        }
        Plan.dropMarks(Label);
        for (int[] Spot : Kept) {
            Plan.mark(Label, Spot[0], Spot[1], Spot[2], Spot[3], Spot[4]);
        }
    }

    private static boolean darkFloor(RuinMat Mat) {
        return Mat == RuinMat.PolishedBlackstone || Mat == RuinMat.BlackstoneBricks || Mat == RuinMat.CrackedBlackstoneBricks || Mat == RuinMat.Blackstone
                || Mat == RuinMat.SmoothBasalt || Mat == RuinMat.ChiseledBlackstone || Mat == RuinMat.GildedBlackstone || Mat == RuinMat.Basalt || Mat == RuinMat.PolishedBasalt;
    }

    public static void collapse(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int FloorY, int Depth, int Bottom, RuinMix Floor) {
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        int Group = Canvas.nextGroup();
        int Lx = Math.min(X1, X2);
        int Hx = Math.max(X1, X2);
        int Lz = Math.min(Z1, Z2);
        int Hz = Math.max(Z1, Z2);
        for (int X = Lx - 1; X <= Hx + 1; X++) {
            for (int Z = Lz - 1; Z <= Hz + 1; Z++) {
                boolean Inside = X >= Lx && X <= Hx && Z >= Lz && Z <= Hz;
                for (int Y = FloorY - Depth; Y < FloorY; Y++) {
                    if (Inside) {
                        Plan.force(X, Y, Z, RuinMat.Air, RuinMat.South, 0);
                        Plan.keep(X, Y, Z);
                    } else if (!Plan.kept(X, Y, Z)) {
                        Plan.force(X, Y, Z, RuinMix.DarkBrick.at(Random, X, Y, Z), RuinMat.Up, 0);
                        Plan.keep(X, Y, Z);
                    }
                }
                int BottomY = FloorY - Depth - 1;
                if (!Inside) {
                    if (!Plan.kept(X, BottomY, Z)) {
                        Plan.force(X, BottomY, Z, RuinMix.DarkBrick.at(Random, X, BottomY, Z), RuinMat.Up, 0);
                        Plan.keep(X, BottomY, Z);
                    }
                    continue;
                }
                RuinMat Base = switch (Bottom) {
                    case PitMagma -> Random.at(X, BottomY, Z, 5) < 0.75 ? RuinMat.Magma : RuinMat.Blackstone;
                    case PitBones -> Random.at(X, BottomY, Z, 5) < 0.4 ? RuinMat.BoneBlock : RuinMat.Gravel;
                    default -> RuinMat.Gravel;
                };
                Plan.force(X, BottomY, Z, Base, RuinMat.Up, 0);
                Plan.keep(X, BottomY, Z);
                if (Bottom == PitWebs && Random.at(X, BottomY + 1, Z, 6) < 0.55) {
                    Plan.force(X, BottomY + 1, Z, RuinMat.Cobweb, RuinMat.South, 0);
                }
                RuinMat Top = Floor.at(Random, X, FloorY, Z);
                if (Top.shape() != RuinMat.Shape.Cube) {
                    Top = RuinMat.CrackedBlackstoneBricks;
                }
                Plan.force(X, FloorY, Z, Top, RuinMat.Up, 0);
                Plan.keep(X, FloorY, Z);
                Plan.mark(TrapTread, X, FloorY, Z, 0, Group);
                Plan.mark(TrapFall, X, FloorY, Z, 0, Group);
                Plan.keepClear(X, FloorY + 1, Z);
            }
        }
    }

    public static void ventFloor(RuinCanvas Canvas, int Cx, int Y, int Cz, double Radius, double Density, int Salt) {
        RuinPlan Plan = Canvas.plan();
        RuinRandom Random = Canvas.random();
        int Reach = (int) Math.ceil(Radius);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (Dx * Dx + Dz * Dz > Radius * Radius) {
                    continue;
                }
                int X = Cx + Dx;
                int Z = Cz + Dz;
                if (Plan.filled(X, Y, Z) && Plan.matAt(X, Y, Z).isOpaqueCube() && !Plan.kept(X, Y, Z) && Random.at(X, Y, Z, Salt) < Density) {
                    Plan.set(X, Y, Z, RuinMat.Magma, RuinMat.South, 0);
                }
            }
        }
    }

    public static List<int[]> scatter(RuinCanvas Canvas, List<int[]> Candidates, int Count) {
        RuinRandom Random = Canvas.random();
        List<int[]> Pool = new ArrayList<>(Candidates);
        List<int[]> Chosen = new ArrayList<>();
        while (!Pool.isEmpty() && Chosen.size() < Count) {
            int[] Pick = Pool.remove(Random.nextInt(Pool.size()));
            boolean Apart = true;
            for (int[] Other : Chosen) {
                if (Math.abs(Other[0] - Pick[0]) + Math.abs(Other[2] - Pick[2]) < 3) {
                    Apart = false;
                    break;
                }
            }
            if (Apart) {
                Chosen.add(Pick);
            }
        }
        return Chosen;
    }

    public static void guardians(RuinCanvas Canvas, List<int[]> Candidates, int Count) {
        List<int[]> Valid = new ArrayList<>();
        for (int[] Spot : Candidates) {
            if (RuinDecay.standable(Canvas.plan(), Spot[0], Spot[1], Spot[2]) && !Canvas.plan().has(Spot[0], Spot[1], Spot[2])) {
                Valid.add(Spot);
            }
        }
        if (Valid.isEmpty()) {
            Valid = Candidates;
        }
        for (int[] Spot : scatter(Canvas, Valid, Count)) {
            Canvas.guardian(Spot[0], Spot[1], Spot[2], Spot.length > 3 ? Spot[3] : RuinMat.South, guardianKind(Canvas.random(), Canvas.tier()));
        }
    }

    public static List<int[]> floorSpots(RuinCanvas Canvas, int X1, int Y, int Z1, int X2, int Z2) {
        RuinPlan Plan = Canvas.plan();
        List<int[]> Spots = new ArrayList<>();
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                if (!Plan.filled(X, Y, Z) && !Plan.filled(X, Y + 1, Z) && (Y <= 1 && !Plan.has(X, Y - 1, Z) || Plan.solidAt(X, Y - 1, Z))) {
                    Spots.add(new int[]{X, Y, Z});
                }
            }
        }
        return Spots;
    }
}
