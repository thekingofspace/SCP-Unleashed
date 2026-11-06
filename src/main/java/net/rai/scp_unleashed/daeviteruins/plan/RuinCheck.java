package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RuinCheck {
    private static final int[][] Around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private RuinCheck() {
    }

    public static List<String> problems(RuinPlan Plan) {
        List<String> Found = new ArrayList<>();
        Set<Long> Reached = new HashSet<>();
        ArrayDeque<Long> Queue = new ArrayDeque<>();
        for (long Key : Plan.keys()) {
            RuinMat Mat = RuinMat.byOrdinal(Plan.codeAt(Key) & 255);
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Mat.isStructural() && (Y <= 0 || Y == 1 && !Plan.has(X, 0, Z))) {
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
                int Code = Plan.codeAt(Next);
                if (Code >= 0 && !Reached.contains(Next) && RuinMat.byOrdinal(Code & 255).isStructural()) {
                    Reached.add(Next);
                    Queue.add(Next);
                }
            }
        }
        int Floating = 0;
        int Loose = 0;
        String FirstFloating = "";
        String FirstLoose = "";
        for (long Key : Plan.keys()) {
            int Code = Plan.codeAt(Key);
            RuinMat Mat = RuinMat.byOrdinal(Code & 255);
            int X = RuinPlan.keyX(Key);
            int Y = RuinPlan.keyY(Key);
            int Z = RuinPlan.keyZ(Key);
            if (Mat.isStructural() && !Reached.contains(Key)) {
                if (Floating++ == 0) {
                    FirstFloating = Mat + "@" + X + "," + Y + "," + Z;
                }
            }
            if (Mat.isAttachment() && !RuinDecay.supported(Plan, X, Y, Z, Mat, (Code >> 8) & 7, (Code >> 12) & 255)) {
                if (Loose++ == 0) {
                    FirstLoose = Mat + "@" + X + "," + Y + "," + Z;
                }
            }
        }
        if (Floating > 0) {
            Found.add("floating " + Floating + " first " + FirstFloating);
        }
        if (Loose > 0) {
            Found.add("unsupported " + Loose + " first " + FirstLoose);
        }
        for (int[] Spot : Plan.marks(RuinPlan.Guardian)) {
            if (Plan.filled(Spot[0], Spot[1], Spot[2]) || Plan.filled(Spot[0], Spot[1] + 1, Spot[2])) {
                Found.add("guardian blocked @" + Spot[0] + "," + Spot[1] + "," + Spot[2]);
            }
            if (!RuinDecay.fullTop(Plan, Spot[0], Spot[1] - 1, Spot[2]) && Plan.matAt(Spot[0], Spot[1] - 1, Spot[2]).shape() != RuinMat.Shape.Slab
                    && Plan.matAt(Spot[0], Spot[1] - 1, Spot[2]).shape() != RuinMat.Shape.Stairs) {
                Found.add("guardian unfloored @" + Spot[0] + "," + Spot[1] + "," + Spot[2]);
            }
        }
        for (int[] Spot : Plan.marks(RuinDanger.TrapPlate)) {
            RuinMat Mat = Plan.matAt(Spot[0], Spot[1], Spot[2]);
            if (Mat.shape() != RuinMat.Shape.Plate) {
                Found.add("plate missing @" + Spot[0] + "," + Spot[1] + "," + Spot[2]);
            }
        }
        for (int[] Spot : Plan.marks(RuinDanger.TrapDispenser)) {
            if (Plan.matAt(Spot[0], Spot[1], Spot[2]) != RuinMat.Dispenser) {
                Found.add("dispenser missing @" + Spot[0] + "," + Spot[1] + "," + Spot[2]);
                continue;
            }
            int[] Step = RuinMat.step(Spot[3]);
            int X = Spot[0] + Step[0];
            int Z = Spot[2] + Step[2];
            boolean Clear = true;
            for (int Distance = 0; Distance < 12; Distance++) {
                RuinMat There = Plan.matAt(X, Spot[1], Z);
                boolean HitPlate = false;
                for (int[] Plate : Plan.marks(RuinDanger.TrapPlate)) {
                    if (Plate[4] == Spot[4] && Plate[0] == X && Plate[2] == Z) {
                        HitPlate = true;
                    }
                }
                if (HitPlate) {
                    break;
                }
                if (Plan.filled(X, Spot[1], Z) && There.isStructural()) {
                    Clear = false;
                    break;
                }
                X += Step[0];
                Z += Step[2];
            }
            if (!Clear) {
                Found.add("dispenser blocked @" + Spot[0] + "," + Spot[1] + "," + Spot[2]);
            }
        }
        for (int[] Spot : Plan.marks(RuinDanger.TrapTread)) {
            if (!Plan.filled(Spot[0], Spot[1], Spot[2]) || Plan.filled(Spot[0], Spot[1] - 1, Spot[2])) {
                Found.add("tread broken @" + Spot[0] + "," + Spot[1] + "," + Spot[2]);
            }
        }
        for (long Key : Plan.keys()) {
            RuinMat Mat = RuinMat.byOrdinal(Plan.codeAt(Key) & 255);
            if (Mat == RuinMat.Chest || Mat == RuinMat.TrappedChest || Mat == RuinMat.Spawner) {
                int X = RuinPlan.keyX(Key);
                int Y = RuinPlan.keyY(Key);
                int Z = RuinPlan.keyZ(Key);
                if (!RuinDecay.fullTop(Plan, X, Y - 1, Z) && !(Y == 1 && !Plan.has(X, 0, Z))) {
                    Found.add(Mat + " unfloored @" + X + "," + Y + "," + Z);
                }
                if ((Mat == RuinMat.Chest || Mat == RuinMat.TrappedChest) && Plan.filled(X, Y + 1, Z) && Plan.matAt(X, Y + 1, Z).isOpaqueCube()) {
                    Found.add(Mat + " lid blocked @" + X + "," + Y + "," + Z);
                }
            }
        }
        return Found;
    }

    public static int[] counts(RuinPlan Plan) {
        int Chests = 0;
        int Spawners = 0;
        int Brushes = 0;
        int Pots = 0;
        for (long Key : Plan.keys()) {
            RuinMat Mat = RuinMat.byOrdinal(Plan.codeAt(Key) & 255);
            if (Mat == RuinMat.Chest || Mat == RuinMat.TrappedChest || Mat == RuinMat.Barrel) {
                Chests++;
            } else if (Mat == RuinMat.Spawner) {
                Spawners++;
            } else if (Mat.shape() == RuinMat.Shape.Brush) {
                Brushes++;
            } else if (Mat == RuinMat.DecoratedPot) {
                Pots++;
            }
        }
        Set<Integer> Groups = new HashSet<>();
        for (String Label : new String[]{RuinDanger.TrapPlate, RuinDanger.TrapTread, RuinDanger.TrapChest}) {
            for (int[] Spot : Plan.marks(Label)) {
                Groups.add(Spot[4]);
            }
        }
        return new int[]{Plan.marks(RuinPlan.Guardian).size(), Spawners, Groups.size(), Chests, Pots, Brushes};
    }
}
