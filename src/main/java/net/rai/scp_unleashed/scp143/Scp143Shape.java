package net.rai.scp_unleashed.scp143;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public final class Scp143Shape {
    public static final int Log = 0;
    public static final int Wood = 1;
    public static final int Leaves = 2;
    public static final int AxisY = 0;
    public static final int AxisX = 1;
    public static final int AxisZ = 2;
    public static final int MaxLeafDistance = 6;

    private Scp143Shape() {
    }

    public record Cell(int X, int Y, int Z, int Kind, int Axis, int Distance) {
    }

    private record Limb(double[] Start, double[] Control, double[] End, double StartRadius, double EndRadius) {
    }

    private static final class Plan {
        private final Map<Long, int[]> Cells = new HashMap<>();
        private final List<double[]> Clusters = new ArrayList<>();
        private final long Seed;

        private Plan(long Seed) {
            this.Seed = Seed;
        }

        private static long key(int X, int Y, int Z) {
            return ((long) (X + 512) << 40) | ((long) (Y + 512) << 20) | (long) (Z + 512);
        }

        private void wood(int X, int Y, int Z, int Axis) {
            long Key = key(X, Y, Z);
            int[] Found = Cells.get(Key);
            if (Found != null && Found[3] != Leaves) {
                if (Found[4] != Axis) {
                    Found[3] = Wood;
                }
                return;
            }
            Cells.put(Key, new int[]{X, Y, Z, Log, Axis});
        }

        private void leaf(int X, int Y, int Z) {
            long Key = key(X, Y, Z);
            if (!Cells.containsKey(Key)) {
                Cells.put(Key, new int[]{X, Y, Z, Leaves, AxisY});
            }
        }

        private boolean has(int X, int Y, int Z) {
            return Cells.containsKey(key(X, Y, Z));
        }

        private int kindAt(int X, int Y, int Z) {
            int[] Found = Cells.get(key(X, Y, Z));
            return Found == null ? -1 : Found[3];
        }
    }

    public static List<Cell> grand(long Seed) {
        SplittableRandom Random = new SplittableRandom(Seed);
        Plan Shape = new Plan(Seed);
        double Lean = Random.nextDouble() * Math.PI * 2.0;
        double[] Base = {1.0, 0.0, 1.0};
        double[] Waist = {1.0 + Math.cos(Lean) * 0.4, 2.5, 1.0 + Math.sin(Lean) * 0.4};
        double[] Fork = {1.0 + Math.cos(Lean) * 1.1, 5.0, 1.0 + Math.sin(Lean) * 1.1};
        trunk(Shape, Base, Waist, 1.05, 0.95);
        trunk(Shape, Waist, Fork, 0.8, 0.6);
        roots(Shape, Random);
        int LimbCount = 6;
        double Turn = Random.nextDouble() * Math.PI * 2.0;
        double Crown = Fork[1] + 1.6;
        List<Limb> Mains = new ArrayList<>();
        for (int Index = 0; Index < LimbCount; Index++) {
            double Angle = Turn + Index * Math.PI * 2.0 / LimbCount + (Random.nextDouble() - 0.5) * 0.35;
            double Reach = 6.6 + Random.nextDouble() * 1.4;
            double Height = Crown + (Index % 2 == 0 ? -0.4 : 0.4) + Random.nextDouble() * 0.5;
            double Along = 0.3 + (Index % 3) * 0.25;
            double[] Start = {Waist[0] + (Fork[0] - Waist[0]) * Along, Waist[1] + (Fork[1] - Waist[1]) * Along + 0.8, Waist[2] + (Fork[2] - Waist[2]) * Along};
            double[] End = {1.0 + Math.cos(Angle) * Reach, Height, 1.0 + Math.sin(Angle) * Reach};
            double[] Control = {Start[0] + (End[0] - Start[0]) * 0.45, Height + 2.6, Start[2] + (End[2] - Start[2]) * 0.45};
            Limb Main = new Limb(Start, Control, End, 0.75, 0.5);
            limb(Shape, Main);
            Mains.add(Main);
            Shape.Clusters.add(new double[]{End[0], End[1] + 1.0, End[2], 3.1 + Random.nextDouble() * 0.5, 2.1 + Random.nextDouble() * 0.4, 1.2});
        }
        for (int Index = 0; Index < LimbCount; Index += 2) {
            Limb Main = Mains.get(Index);
            double[] From = bezier(Main, 0.4);
            double[] Next = Mains.get((Index + 1) % LimbCount).End();
            double[] To = {(From[0] * 0.45 + Next[0] * 0.2 + 1.0 * 0.35), Crown + 3.4 + Random.nextDouble() * 0.6, (From[2] * 0.45 + Next[2] * 0.2 + 1.0 * 0.35)};
            double[] Mid = {(From[0] + To[0]) * 0.5, To[1] + 0.2, (From[2] + To[2]) * 0.5};
            limb(Shape, new Limb(From, Mid, To, 0.5, 0.5));
            Shape.Clusters.add(new double[]{To[0], To[1] + 0.8, To[2], 2.6 + Random.nextDouble() * 0.4, 2.0 + Random.nextDouble() * 0.4, 1.0});
        }
        double[] Leader = {1.0 + (Random.nextDouble() - 0.5) * 0.8, Crown + 5.2, 1.0 + (Random.nextDouble() - 0.5) * 0.8};
        limb(Shape, new Limb(new double[]{Fork[0], Fork[1] - 0.5, Fork[2]}, new double[]{Fork[0], Crown + 2.6, Fork[2]}, Leader, 0.6, 0.5));
        Shape.Clusters.add(new double[]{Leader[0], Leader[1] + 0.5, Leader[2], 2.8, 2.2, 1.2});
        canopy(Shape, new SplittableRandom(Seed ^ 0x5DEECE66DL), 0.3);
        curtains(Shape, new SplittableRandom(Seed ^ 0x2545F4914F6CDD1DL), 0.1, 2);
        return finish(Shape);
    }

    public static List<Cell> young(long Seed) {
        SplittableRandom Random = new SplittableRandom(Seed);
        Plan Shape = new Plan(Seed);
        double Lean = Random.nextDouble() * Math.PI * 2.0;
        int Height = 4 + Random.nextInt(2);
        double[] Base = {0.5, 0.0, 0.5};
        double[] Fork = {0.5 + Math.cos(Lean) * 0.4, Height, 0.5 + Math.sin(Lean) * 0.4};
        trunk(Shape, Base, Fork, 0.5, 0.5);
        int LimbCount = 3;
        double Turn = Random.nextDouble() * Math.PI * 2.0;
        for (int Index = 0; Index < LimbCount; Index++) {
            double Angle = Turn + Index * Math.PI * 2.0 / LimbCount + (Random.nextDouble() - 0.5) * 0.6;
            double Reach = 2.3 + Random.nextDouble() * 1.0;
            double Rise = 1.6 + Random.nextDouble() * 0.9;
            double[] Start = {Fork[0], Fork[1] - 0.5, Fork[2]};
            double[] End = {Fork[0] + Math.cos(Angle) * Reach, Fork[1] + Rise, Fork[2] + Math.sin(Angle) * Reach};
            double[] Control = {Fork[0] + Math.cos(Angle) * Reach * 0.3, Fork[1] + Rise, Fork[2] + Math.sin(Angle) * Reach * 0.3};
            limb(Shape, new Limb(Start, Control, End, 0.5, 0.5));
            Shape.Clusters.add(new double[]{End[0], End[1] + 0.7, End[2], 2.4 + Random.nextDouble() * 0.4, 1.7 + Random.nextDouble() * 0.3, 1.0});
        }
        Shape.Clusters.add(new double[]{Fork[0], Fork[1] + 2.2, Fork[2], 2.3, 1.8, 1.0});
        canopy(Shape, new SplittableRandom(Seed ^ 0x5DEECE66DL), 0.35);
        curtains(Shape, new SplittableRandom(Seed ^ 0x2545F4914F6CDD1DL), 0.22, 2);
        return finish(Shape);
    }

    public static int[] bounds(List<Cell> Cells) {
        int[] Box = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (Cell Each : Cells) {
            Box[0] = Math.min(Box[0], Each.X());
            Box[1] = Math.min(Box[1], Each.Y());
            Box[2] = Math.min(Box[2], Each.Z());
            Box[3] = Math.max(Box[3], Each.X());
            Box[4] = Math.max(Box[4], Each.Y());
            Box[5] = Math.max(Box[5], Each.Z());
        }
        return Box;
    }

    private static void trunk(Plan Shape, double[] Base, double[] Top, double BaseRadius, double TopRadius) {
        int Steps = 48;
        for (int Step = 0; Step <= Steps; Step++) {
            double T = (double) Step / Steps;
            double X = Base[0] + (Top[0] - Base[0]) * T * T;
            double Y = Base[1] + (Top[1] - Base[1]) * T;
            double Z = Base[2] + (Top[2] - Base[2]) * T * T;
            double Radius = BaseRadius + (TopRadius - BaseRadius) * T;
            disc(Shape, X, Y, Z, Radius, AxisY);
        }
    }

    private static void roots(Plan Shape, SplittableRandom Random) {
        int[][] Sides = {{-1, 0, -1, 0}, {2, 1, 1, 0}, {1, -1, 0, -1}, {0, 2, 0, 1}};
        int Skip = Random.nextInt(Sides.length);
        for (int Index = 0; Index < Sides.length; Index++) {
            if (Index == Skip) {
                continue;
            }
            int[] Side = Sides[Index];
            int Axis = Side[2] != 0 ? AxisX : AxisZ;
            Shape.wood(Side[0], 0, Side[1], Axis);
            if (Random.nextDouble() < 0.6) {
                Shape.wood(Side[0] + Side[2], -1, Side[1] + Side[3], Axis);
            }
        }
    }

    private static void disc(Plan Shape, double X, double Y, double Z, double Radius, int Axis) {
        int Cy = (int) Math.floor(Y);
        int Reach = (int) Math.ceil(Radius) + 1;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                int Bx = (int) Math.floor(X) + Dx;
                int Bz = (int) Math.floor(Z) + Dz;
                double Ox = Bx + 0.5 - X;
                double Oz = Bz + 0.5 - Z;
                if (Ox * Ox + Oz * Oz <= Radius * Radius) {
                    Shape.wood(Bx, Cy, Bz, Axis);
                }
            }
        }
    }

    private static double[] bezier(Limb Path, double T) {
        double U = 1.0 - T;
        double[] Point = new double[3];
        for (int Axis = 0; Axis < 3; Axis++) {
            Point[Axis] = U * U * Path.Start()[Axis] + 2.0 * U * T * Path.Control()[Axis] + T * T * Path.End()[Axis];
        }
        return Point;
    }

    private static void limb(Plan Shape, Limb Path) {
        int Steps = 64;
        double[] Previous = bezier(Path, 0.0);
        for (int Step = 1; Step <= Steps; Step++) {
            double T = (double) Step / Steps;
            double[] Point = bezier(Path, T);
            double Dx = Point[0] - Previous[0];
            double Dy = Point[1] - Previous[1];
            double Dz = Point[2] - Previous[2];
            int Axis = Math.abs(Dy) >= Math.abs(Dx) && Math.abs(Dy) >= Math.abs(Dz) ? AxisY : Math.abs(Dx) >= Math.abs(Dz) ? AxisX : AxisZ;
            double Radius = Path.StartRadius() + (Path.EndRadius() - Path.StartRadius()) * T;
            if (Radius <= 0.55) {
                Shape.wood((int) Math.floor(Point[0]), (int) Math.floor(Point[1]), (int) Math.floor(Point[2]), Axis);
            } else {
                ball(Shape, Point, Radius, Axis);
            }
            Previous = Point;
        }
    }

    private static void ball(Plan Shape, double[] Centre, double Radius, int Axis) {
        int Reach = (int) Math.ceil(Radius) + 1;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dy = -Reach; Dy <= Reach; Dy++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    int Bx = (int) Math.floor(Centre[0]) + Dx;
                    int By = (int) Math.floor(Centre[1]) + Dy;
                    int Bz = (int) Math.floor(Centre[2]) + Dz;
                    double Ox = Bx + 0.5 - Centre[0];
                    double Oy = By + 0.5 - Centre[1];
                    double Oz = Bz + 0.5 - Centre[2];
                    if (Ox * Ox + Oy * Oy + Oz * Oz <= Radius * Radius) {
                        Shape.wood(Bx, By, Bz, Axis);
                    }
                }
            }
        }
    }

    private static double hash(int X, int Y, int Z, long Seed) {
        long Value = X * 0x9E3779B97F4A7C15L ^ Y * 0xC2B2AE3D27D4EB4FL ^ Z * 0x165667B19E3779F9L ^ Seed;
        Value = (Value ^ (Value >>> 31)) * 0x94D049BB133111EBL;
        Value = Value ^ (Value >>> 29);
        return (Value >>> 11) * 0x1.0p-53;
    }

    private static double noise(double X, double Y, double Z, long Seed) {
        int X0 = (int) Math.floor(X);
        int Y0 = (int) Math.floor(Y);
        int Z0 = (int) Math.floor(Z);
        double Fx = smooth(X - X0);
        double Fy = smooth(Y - Y0);
        double Fz = smooth(Z - Z0);
        double Total = 0.0;
        for (int Corner = 0; Corner < 8; Corner++) {
            int Cx = Corner & 1;
            int Cy = (Corner >> 1) & 1;
            int Cz = (Corner >> 2) & 1;
            double Weight = (Cx == 1 ? Fx : 1.0 - Fx) * (Cy == 1 ? Fy : 1.0 - Fy) * (Cz == 1 ? Fz : 1.0 - Fz);
            Total += Weight * hash(X0 + Cx, Y0 + Cy, Z0 + Cz, Seed);
        }
        return Total;
    }

    private static double smooth(double Value) {
        return Value * Value * (3.0 - 2.0 * Value);
    }

    private static void canopy(Plan Shape, SplittableRandom Random, double Roughness) {
        List<double[]> Blobs = new ArrayList<>();
        for (double[] Cluster : Shape.Clusters) {
            Blobs.add(new double[]{Cluster[0], Cluster[1], Cluster[2], Cluster[3] * 0.82, Cluster[4], Cluster[5]});
            int Extra = 3 + Random.nextInt(2);
            double Spin = Random.nextDouble() * Math.PI * 2.0;
            for (int Index = 0; Index < Extra; Index++) {
                double Angle = Spin + Index * Math.PI * 2.0 / Extra + (Random.nextDouble() - 0.5) * 0.8;
                double Out = Cluster[3] * (0.35 + Random.nextDouble() * 0.2);
                double Size = 0.55 + Random.nextDouble() * 0.15;
                Blobs.add(new double[]{Cluster[0] + Math.cos(Angle) * Out, Cluster[1] + (Random.nextDouble() - 0.6) * Cluster[4] * 0.7, Cluster[2] + Math.sin(Angle) * Out,
                        Cluster[3] * Size, Cluster[4] * (Size + 0.15), Cluster[5] * Size});
            }
        }
        for (double[] Cluster : Blobs) {
            double Cx = Cluster[0];
            double Cy = Cluster[1];
            double Cz = Cluster[2];
            double Wide = Cluster[3];
            double Tall = Cluster[4];
            double Under = Cluster[5];
            int Reach = (int) Math.ceil(Wide) + 2;
            int Up = (int) Math.ceil(Tall) + 2;
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dy = -Up; Dy <= Up; Dy++) {
                    for (int Dz = -Reach; Dz <= Reach; Dz++) {
                        int Bx = (int) Math.floor(Cx) + Dx;
                        int By = (int) Math.floor(Cy) + Dy;
                        int Bz = (int) Math.floor(Cz) + Dz;
                        double Ox = (Bx + 0.5 - Cx) / Wide;
                        double Oy = By + 0.5 - Cy;
                        Oy = Oy >= 0.0 ? Oy / Tall : Oy / Under;
                        double Oz = (Bz + 0.5 - Cz) / Wide;
                        double Squeeze = 1.0 + 0.45 * Math.max(0.0, Oy);
                        double Reached = (Ox * Ox + Oz * Oz) * Squeeze * Squeeze + Oy * Oy;
                        double Wobble = (noise(Bx * 0.45, By * 0.6, Bz * 0.45, Shape.Seed) - 0.5) * 2.0 * Roughness;
                        if (Reached <= 1.0 + Wobble) {
                            Shape.leaf(Bx, By, Bz);
                        }
                    }
                }
            }
        }
    }

    private static void curtains(Plan Shape, SplittableRandom Random, double Chance, int MaxLength) {
        int[] Box = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int[] Each : Shape.Cells.values()) {
            Box[0] = Math.min(Box[0], Each[0]);
            Box[1] = Math.min(Box[1], Each[2]);
            Box[2] = Math.max(Box[2], Each[0]);
            Box[3] = Math.max(Box[3], Each[2]);
        }
        double Mx = (Box[0] + Box[2]) * 0.5 + 0.5;
        double Mz = (Box[1] + Box[3]) * 0.5 + 0.5;
        double Span = Math.max(Box[2] - Box[0], Box[3] - Box[1]) * 0.5;
        List<int[]> Rim = new ArrayList<>();
        for (int[] Each : Shape.Cells.values()) {
            if (Each[3] != Leaves || Shape.has(Each[0], Each[1] - 1, Each[2])) {
                continue;
            }
            double Ox = Each[0] + 0.5 - Mx;
            double Oz = Each[2] + 0.5 - Mz;
            if (Math.sqrt(Ox * Ox + Oz * Oz) >= Span * 0.55) {
                Rim.add(Each);
            }
        }
        Rim.sort((A, B) -> A[0] != B[0] ? Integer.compare(A[0], B[0]) : A[1] != B[1] ? Integer.compare(A[1], B[1]) : Integer.compare(A[2], B[2]));
        for (int[] Each : Rim) {
            if (Random.nextDouble() >= Chance) {
                continue;
            }
            int Length = Random.nextDouble() < 0.7 ? 1 : 1 + Random.nextInt(MaxLength);
            for (int Drop = 1; Drop <= Length; Drop++) {
                if (Shape.has(Each[0], Each[1] - Drop, Each[2])) {
                    break;
                }
                Shape.leaf(Each[0], Each[1] - Drop, Each[2]);
            }
        }
    }

    private static List<Cell> finish(Plan Shape) {
        for (int[] Each : Shape.Cells.values()) {
            if (Each[3] == Leaves) {
                continue;
            }
            int Axes = 0;
            for (int[] Step : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                int Near = Shape.kindAt(Each[0] + Step[0], Each[1] + Step[1], Each[2] + Step[2]);
                if (Near == Log || Near == Wood) {
                    int Along = Step[1] != 0 ? AxisY : Step[0] != 0 ? AxisX : AxisZ;
                    Axes |= 1 << Along;
                }
            }
            if (Integer.bitCount(Axes) >= 2 && Each[4] != AxisY) {
                Each[3] = Wood;
            }
        }
        Map<Long, Integer> Distance = new HashMap<>();
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        for (int[] Each : Shape.Cells.values()) {
            if (Each[3] != Leaves) {
                Distance.put(Plan.key(Each[0], Each[1], Each[2]), 0);
                Queue.add(Each);
            }
        }
        while (!Queue.isEmpty()) {
            int[] Each = Queue.poll();
            int Here = Distance.get(Plan.key(Each[0], Each[1], Each[2]));
            if (Here >= MaxLeafDistance) {
                continue;
            }
            for (int[] Step : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                int Nx = Each[0] + Step[0];
                int Ny = Each[1] + Step[1];
                int Nz = Each[2] + Step[2];
                long Key = Plan.key(Nx, Ny, Nz);
                int[] Near = Shape.Cells.get(Key);
                if (Near != null && Near[3] == Leaves && !Distance.containsKey(Key)) {
                    Distance.put(Key, Here + 1);
                    Queue.add(Near);
                }
            }
        }
        List<Cell> Result = new ArrayList<>();
        List<int[]> Sorted = new ArrayList<>(Shape.Cells.values());
        Sorted.sort((A, B) -> A[1] != B[1] ? Integer.compare(A[1], B[1]) : A[0] != B[0] ? Integer.compare(A[0], B[0]) : Integer.compare(A[2], B[2]));
        for (int[] Each : Sorted) {
            Integer Found = Distance.get(Plan.key(Each[0], Each[1], Each[2]));
            if (Found == null) {
                continue;
            }
            Result.add(new Cell(Each[0], Each[1], Each[2], Each[3], Each[4], Found));
        }
        return Result;
    }
}
