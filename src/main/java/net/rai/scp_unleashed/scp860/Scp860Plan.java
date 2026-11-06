package net.rai.scp_unleashed.scp860;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class Scp860Plan {
    public static final int Size = 256;
    public static final int Cells = 9;
    public static final int CellSize = 26;
    public static final int Margin = (Size - Cells * CellSize) / 2;
    public static final int StartColumn = 4;
    public static final int StartRow = Cells - 1;
    public static final int MinRoute = 30;
    public static final int MaxRoute = 42;
    public static final int MaxBranches = 6;
    public static final int MaxBranchDepth = 3;
    public static final int BranchGap = 3;
    public static final double NodeJitter = 6.0;
    public static final double EndJitter = 2.0;
    public static final double Bow = 5.0;
    public static final double SampleStep = 0.5;
    public static final double Reach = 12.0;
    public static final double ArrivalBack = 4.5;
    public static final double ExitAhead = 3.5;
    public static final int Far = 255;

    public final long Seed;
    public final double[] NodeX = new double[Cells * Cells];
    public final double[] NodeZ = new double[Cells * Cells];
    public final List<Integer> Route = new ArrayList<>();
    public final List<int[]> Edges = new ArrayList<>();
    public final Set<Integer> Kept = new HashSet<>();
    public final int Start;
    public final int Exit;
    public final int Branches;
    private final float[] SampleX;
    private final float[] SampleZ;
    private final float[] SampleRank;
    private final int SampleCount;
    private final byte[] Distance = new byte[Size * Size];
    private final int[] Nearest = new int[Size * Size];
    public final double ArrivalX;
    public final double ArrivalZ;
    public final float ArrivalYaw;
    public final int ArrivalDoorX;
    public final int ArrivalDoorZ;
    public final Direction ArrivalFacing;
    public final int ExitDoorX;
    public final int ExitDoorZ;
    public final Direction ExitFacing;
    public final double RouteLength;

    public Scp860Plan(long Seed) {
        this.Seed = Seed;
        Random Dice = new Random(Seed);
        int Count = Cells * Cells;
        int[] Parent = new int[Count];
        Arrays.fill(Parent, -1);
        List<List<Integer>> Tree = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            Tree.add(new ArrayList<>());
        }
        Start = index(StartColumn, StartRow);
        boolean[] Seen = new boolean[Count];
        Deque<Integer> Stack = new ArrayDeque<>();
        Stack.push(Start);
        Seen[Start] = true;
        while (!Stack.isEmpty()) {
            int Here = Stack.peek();
            List<Integer> Open = new ArrayList<>();
            for (int Next : neighbours(Here)) {
                if (!Seen[Next]) {
                    Open.add(Next);
                }
            }
            if (Open.isEmpty()) {
                Stack.pop();
                continue;
            }
            int Next = Open.get(Dice.nextInt(Open.size()));
            Seen[Next] = true;
            Parent[Next] = Here;
            Tree.get(Here).add(Next);
            Tree.get(Next).add(Here);
            Stack.push(Next);
        }
        int[] Depth = distances(Tree, Start, Count);
        int Best = -1;
        for (int Index = 0; Index < Count; Index++) {
            if (Depth[Index] >= MinRoute && Depth[Index] <= MaxRoute && (Best < 0 || Depth[Index] > Depth[Best]
                    || Depth[Index] == Depth[Best] && row(Index) < row(Best))) {
                Best = Index;
            }
        }
        if (Best < 0) {
            for (int Index = 0; Index < Count; Index++) {
                if (Best < 0 || Depth[Index] > Depth[Best]) {
                    Best = Index;
                }
            }
        }
        Exit = Best;
        for (int Cell = Exit; Cell != -1; Cell = Parent[Cell]) {
            Route.add(0, Cell);
        }
        Kept.addAll(Route);
        for (int Step = 0; Step + 1 < Route.size(); Step++) {
            Edges.add(new int[]{Route.get(Step), Route.get(Step + 1)});
        }
        List<int[]> Roots = new ArrayList<>();
        for (int Step = 2; Step < Route.size() - 2; Step++) {
            for (int Next : Tree.get(Route.get(Step))) {
                if (!Kept.contains(Next)) {
                    Roots.add(new int[]{Step, Next});
                }
            }
        }
        Collections.shuffle(Roots, Dice);
        List<Integer> Used = new ArrayList<>();
        int Made = 0;
        for (int[] Root : Roots) {
            if (Made >= MaxBranches) {
                break;
            }
            boolean Close = false;
            for (int Other : Used) {
                if (Math.abs(Other - Root[0]) < BranchGap) {
                    Close = true;
                }
            }
            if (Close) {
                continue;
            }
            Used.add(Root[0]);
            Made++;
            int From = Route.get(Root[0]);
            int Cell = Root[1];
            int Length = 1 + Dice.nextInt(MaxBranchDepth);
            for (int Step = 0; Step < Length; Step++) {
                Edges.add(new int[]{From, Cell});
                Kept.add(Cell);
                List<Integer> Deeper = new ArrayList<>();
                for (int Next : Tree.get(Cell)) {
                    if (Next != From && !Kept.contains(Next)) {
                        Deeper.add(Next);
                    }
                }
                if (Deeper.isEmpty()) {
                    break;
                }
                From = Cell;
                Cell = Deeper.get(Dice.nextInt(Deeper.size()));
            }
        }
        Branches = Made;
        for (int Index = 0; Index < Count; Index++) {
            double Jitter = Index == Start || Index == Exit ? EndJitter : NodeJitter;
            NodeX[Index] = Margin + column(Index) * CellSize + CellSize / 2.0 + (Dice.nextDouble() * 2.0 - 1.0) * Jitter;
            NodeZ[Index] = Margin + row(Index) * CellSize + CellSize / 2.0 + (Dice.nextDouble() * 2.0 - 1.0) * Jitter;
        }
        List<List<Integer>> KeptTree = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            KeptTree.add(new ArrayList<>());
        }
        for (int[] Edge : Edges) {
            KeptTree.get(Edge[0]).add(Edge[1]);
            KeptTree.get(Edge[1]).add(Edge[0]);
        }
        int[] Rank = distances(KeptTree, Exit, Count);
        int Previous = Route.size() >= 2 ? Route.get(Route.size() - 2) : Exit;
        double InX = NodeX[Exit] - NodeX[Previous];
        double InZ = NodeZ[Exit] - NodeZ[Previous];
        double InLength = Math.max(1.0e-6, Math.sqrt(InX * InX + InZ * InZ));
        InX /= InLength;
        InZ /= InLength;
        ExitFacing = Direction.getNearest(-InX, 0.0, -InZ);
        double DoorX = NodeX[Exit] + InX * ExitAhead;
        double DoorZ = NodeZ[Exit] + InZ * ExitAhead;
        ExitDoorX = Mth.floor(DoorX);
        ExitDoorZ = Mth.floor(DoorZ);
        List<float[]> Points = new ArrayList<>();
        double Length = 0.0;
        for (int[] Edge : Edges) {
            double Ax = NodeX[Edge[0]];
            double Az = NodeZ[Edge[0]];
            double Bx = NodeX[Edge[1]];
            double Bz = NodeZ[Edge[1]];
            double Dx = Bx - Ax;
            double Dz = Bz - Az;
            double Span = Math.max(1.0e-6, Math.sqrt(Dx * Dx + Dz * Dz));
            double Offset = (Dice.nextDouble() * 2.0 - 1.0) * Bow;
            double Cx = (Ax + Bx) / 2.0 - Dz / Span * Offset;
            double Cz = (Az + Bz) / 2.0 + Dx / Span * Offset;
            int Steps = Math.max(2, (int) Math.ceil(Span * 1.15 / SampleStep));
            double Lx = Ax;
            double Lz = Az;
            for (int Step = 0; Step <= Steps; Step++) {
                double T = Step / (double) Steps;
                double U = 1.0 - T;
                double X = U * U * Ax + 2.0 * U * T * Cx + T * T * Bx;
                double Z = U * U * Az + 2.0 * U * T * Cz + T * T * Bz;
                float Value = (float) (Rank[Edge[0]] + (Rank[Edge[1]] - Rank[Edge[0]]) * T);
                Points.add(new float[]{(float) X, (float) Z, Value});
                if (Kept.contains(Edge[0]) && Route.contains(Edge[0]) && Route.contains(Edge[1])) {
                    Length += Math.sqrt((X - Lx) * (X - Lx) + (Z - Lz) * (Z - Lz));
                }
                Lx = X;
                Lz = Z;
            }
        }
        double FrontX = DoorX - InX * 1.2;
        double FrontZ = DoorZ - InZ * 1.2;
        int Lead = (int) Math.ceil(ExitAhead / SampleStep);
        for (int Step = 0; Step <= Lead; Step++) {
            double T = Step / (double) Lead;
            Points.add(new float[]{(float) Mth.lerp(T, NodeX[Exit], FrontX), (float) Mth.lerp(T, NodeZ[Exit], FrontZ), -1.0f * (float) T});
        }
        RouteLength = Length + ExitAhead;
        int First = Route.size() >= 2 ? Route.get(1) : Exit;
        double OutX = NodeX[First] - NodeX[Start];
        double OutZ = NodeZ[First] - NodeZ[Start];
        double OutLength = Math.max(1.0e-6, Math.sqrt(OutX * OutX + OutZ * OutZ));
        OutX /= OutLength;
        OutZ /= OutLength;
        ArrivalX = NodeX[Start];
        ArrivalZ = NodeZ[Start];
        ArrivalYaw = (float) (Mth.atan2(-OutX, OutZ) * Mth.RAD_TO_DEG);
        ArrivalDoorX = Mth.floor(ArrivalX - OutX * ArrivalBack);
        ArrivalDoorZ = Mth.floor(ArrivalZ - OutZ * ArrivalBack);
        ArrivalFacing = Direction.getNearest(OutX, 0.0, OutZ);
        SampleCount = Points.size();
        SampleX = new float[SampleCount];
        SampleZ = new float[SampleCount];
        SampleRank = new float[SampleCount];
        for (int Index = 0; Index < SampleCount; Index++) {
            SampleX[Index] = Points.get(Index)[0];
            SampleZ[Index] = Points.get(Index)[1];
            SampleRank[Index] = Points.get(Index)[2];
        }
        Arrays.fill(Distance, (byte) Far);
        Arrays.fill(Nearest, -1);
        int Radius = (int) Math.ceil(Reach);
        for (int Index = 0; Index < SampleCount; Index++) {
            int Cx = Mth.floor(SampleX[Index]);
            int Cz = Mth.floor(SampleZ[Index]);
            for (int X = Cx - Radius; X <= Cx + Radius; X++) {
                if (X < 0 || X >= Size) {
                    continue;
                }
                for (int Z = Cz - Radius; Z <= Cz + Radius; Z++) {
                    if (Z < 0 || Z >= Size) {
                        continue;
                    }
                    double Dx = X + 0.5 - SampleX[Index];
                    double Dz = Z + 0.5 - SampleZ[Index];
                    double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
                    if (Gap > Reach) {
                        continue;
                    }
                    int Quarter = Math.min(Far - 1, (int) Math.round(Gap * 4.0));
                    int Slot = X * Size + Z;
                    if (Quarter < (Distance[Slot] & 0xFF)) {
                        Distance[Slot] = (byte) Quarter;
                        Nearest[Slot] = Index;
                    }
                }
            }
        }
    }

    public static int index(int Column, int Row) {
        return Row * Cells + Column;
    }

    public static int column(int Index) {
        return Index % Cells;
    }

    public static int row(int Index) {
        return Index / Cells;
    }

    private static List<Integer> neighbours(int Index) {
        List<Integer> Found = new ArrayList<>();
        int Column = column(Index);
        int Row = row(Index);
        if (Column > 0) {
            Found.add(index(Column - 1, Row));
        }
        if (Column < Cells - 1) {
            Found.add(index(Column + 1, Row));
        }
        if (Row > 0) {
            Found.add(index(Column, Row - 1));
        }
        if (Row < Cells - 1) {
            Found.add(index(Column, Row + 1));
        }
        return Found;
    }

    private static int[] distances(List<List<Integer>> Tree, int From, int Count) {
        int[] Depth = new int[Count];
        Arrays.fill(Depth, Integer.MAX_VALUE / 4);
        Deque<Integer> Queue = new ArrayDeque<>();
        Depth[From] = 0;
        Queue.add(From);
        while (!Queue.isEmpty()) {
            int Here = Queue.poll();
            for (int Next : Tree.get(Here)) {
                if (Depth[Next] > Depth[Here] + 1) {
                    Depth[Next] = Depth[Here] + 1;
                    Queue.add(Next);
                }
            }
        }
        return Depth;
    }

    public boolean inside(double X, double Z) {
        return X >= 0.0 && Z >= 0.0 && X < Size && Z < Size;
    }

    public double distance(double X, double Z) {
        int Cx = Mth.floor(X);
        int Cz = Mth.floor(Z);
        if (Cx < 0 || Cz < 0 || Cx >= Size || Cz >= Size) {
            return Far;
        }
        int Value = Distance[Cx * Size + Cz] & 0xFF;
        return Value >= Far ? Far : Value / 4.0;
    }

    public int nearest(double X, double Z) {
        int Cx = Mth.floor(X);
        int Cz = Mth.floor(Z);
        if (Cx < 0 || Cz < 0 || Cx >= Size || Cz >= Size) {
            return -1;
        }
        return Nearest[Cx * Size + Cz];
    }

    public int samples() {
        return SampleCount;
    }

    public double sampleX(int Index) {
        return SampleX[Index];
    }

    public double sampleZ(int Index) {
        return SampleZ[Index];
    }

    public double rank(int Index) {
        return SampleRank[Index];
    }

    public int routeCells() {
        return Route.size();
    }

    public int keptCells() {
        return Kept.size();
    }

    public boolean nearDoor(double X, double Z, double Radius) {
        double Ax = X - (ArrivalDoorX + 0.5);
        double Az = Z - (ArrivalDoorZ + 0.5);
        double Ex = X - (ExitDoorX + 0.5);
        double Ez = Z - (ExitDoorZ + 0.5);
        double Sx = X - ArrivalX;
        double Sz = Z - ArrivalZ;
        return Ax * Ax + Az * Az < Radius * Radius || Ex * Ex + Ez * Ez < Radius * Radius || Sx * Sx + Sz * Sz < Radius * Radius;
    }
}
