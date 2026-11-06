package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.List;

public final class RuinFigures {
    public static final RuinMix Stone = RuinMix.of(RuinMat.CutRedSandstone, 40, RuinMat.SmoothRedSandstone, 34, RuinMat.RedSandstone, 26);
    public static final RuinMix Robe = RuinMix.of(RuinMat.RedNetherBricks, 46, RuinMat.RedTerracotta, 34, RuinMat.CrackedNetherBricks, 8, RuinMat.Granite, 12);
    public static final RuinMix Skin = RuinMix.of(RuinMat.SmoothRedSandstone, 70, RuinMat.CutRedSandstone, 30);
    public static final RuinMix GodBody = RuinMix.of(RuinMat.RuinBrick, 60, RuinMat.RedNetherBricks, 40);
    public static final RuinMix Dark = RuinMix.of(RuinMat.PolishedBlackstone, 40, RuinMat.BlackstoneBricks, 35, RuinMat.Blackstone, 25);

    public record Part(double[] Min, double[] Max, double[] Pivot, double Pitch, double Yaw, double Roll, RuinMix Mix) {
    }

    public record Line(double[] From, double[] To, RuinMat Mat) {
    }

    public static final class Figure {
        private final List<Part> Parts = new ArrayList<>();
        private final List<Line> Lines = new ArrayList<>();

        public Figure box(double X1, double Y1, double Z1, double X2, double Y2, double Z2, RuinMix Mix) {
            Parts.add(new Part(new double[]{X1, Y1, Z1}, new double[]{X2, Y2, Z2}, new double[]{0, 0, 0}, 0, 0, 0, Mix));
            return this;
        }

        public Figure limb(double X1, double Y1, double Z1, double X2, double Y2, double Z2, double Px, double Py, double Pz, double Pitch, double Yaw, double Roll,
                           RuinMix Mix) {
            Parts.add(new Part(new double[]{X1, Y1, Z1}, new double[]{X2, Y2, Z2}, new double[]{Px, Py, Pz}, Pitch, Yaw, Roll, Mix));
            return this;
        }

        public Figure line(double X1, double Y1, double Z1, double X2, double Y2, double Z2, RuinMat Mat) {
            Lines.add(new Line(new double[]{X1, Y1, Z1}, new double[]{X2, Y2, Z2}, Mat));
            return this;
        }

        public List<Part> parts() {
            return Parts;
        }

        public List<Line> lines() {
            return Lines;
        }
    }

    public record Pose(double Ox, double Oy, double Oz, double Yaw, double Tilt, double Roll, double PixelsPerBlock) {
    }

    private RuinFigures() {
    }

    private static double[] rotate(double[] P, double Pitch, double Yaw, double Roll) {
        double X = P[0];
        double Y = P[1];
        double Z = P[2];
        double Cr = Math.cos(Math.toRadians(Roll));
        double Sr = Math.sin(Math.toRadians(Roll));
        double X1 = X * Cr - Y * Sr;
        double Y1 = X * Sr + Y * Cr;
        double Cp = Math.cos(Math.toRadians(Pitch));
        double Sp = Math.sin(Math.toRadians(Pitch));
        double Y2 = Y1 * Cp - Z * Sp;
        double Z2 = Y1 * Sp + Z * Cp;
        double Cy = Math.cos(Math.toRadians(Yaw));
        double Sy = Math.sin(Math.toRadians(Yaw));
        double X3 = X1 * Cy + Z2 * Sy;
        double Z3 = -X1 * Sy + Z2 * Cy;
        return new double[]{X3, Y2, Z3};
    }

    private static double[] unrotate(double[] P, double Pitch, double Yaw, double Roll) {
        double Cy = Math.cos(Math.toRadians(-Yaw));
        double Sy = Math.sin(Math.toRadians(-Yaw));
        double X1 = P[0] * Cy + P[2] * Sy;
        double Z1 = -P[0] * Sy + P[2] * Cy;
        double Cp = Math.cos(Math.toRadians(-Pitch));
        double Sp = Math.sin(Math.toRadians(-Pitch));
        double Y2 = P[1] * Cp - Z1 * Sp;
        double Z2 = P[1] * Sp + Z1 * Cp;
        double Cr = Math.cos(Math.toRadians(-Roll));
        double Sr = Math.sin(Math.toRadians(-Roll));
        double X3 = X1 * Cr - Y2 * Sr;
        double Y3 = X1 * Sr + Y2 * Cr;
        return new double[]{X3, Y3, Z2};
    }

    private static double[] toPlan(Pose At, double[] Local) {
        double[] Scaled = {Local[0] / At.PixelsPerBlock(), Local[1] / At.PixelsPerBlock(), Local[2] / At.PixelsPerBlock()};
        double[] Turned = rotate(Scaled, At.Tilt(), At.Yaw(), At.Roll());
        return new double[]{Turned[0] + At.Ox(), Turned[1] + At.Oy(), Turned[2] + At.Oz()};
    }

    private static double[] toLocal(Pose At, double X, double Y, double Z) {
        double[] Back = unrotate(new double[]{X - At.Ox(), Y - At.Oy(), Z - At.Oz()}, At.Tilt(), At.Yaw(), At.Roll());
        return new double[]{Back[0] * At.PixelsPerBlock(), Back[1] * At.PixelsPerBlock(), Back[2] * At.PixelsPerBlock()};
    }

    private static double[][] corners(Part Piece) {
        double[][] Out = new double[8][];
        int Index = 0;
        for (int A = 0; A < 2; A++) {
            for (int B = 0; B < 2; B++) {
                for (int C = 0; C < 2; C++) {
                    double[] Corner = {A == 0 ? Piece.Min()[0] : Piece.Max()[0], B == 0 ? Piece.Min()[1] : Piece.Max()[1], C == 0 ? Piece.Min()[2] : Piece.Max()[2]};
                    double[] Moved = rotate(new double[]{Corner[0] - Piece.Pivot()[0], Corner[1] - Piece.Pivot()[1], Corner[2] - Piece.Pivot()[2]}, Piece.Pitch(),
                            Piece.Yaw(), Piece.Roll());
                    Out[Index++] = new double[]{Moved[0] + Piece.Pivot()[0], Moved[1] + Piece.Pivot()[1], Moved[2] + Piece.Pivot()[2]};
                }
            }
        }
        return Out;
    }

    private static boolean inside(Part Piece, double[] Local) {
        double[] Back = unrotate(new double[]{Local[0] - Piece.Pivot()[0], Local[1] - Piece.Pivot()[1], Local[2] - Piece.Pivot()[2]}, Piece.Pitch(), Piece.Yaw(),
                Piece.Roll());
        double X = Back[0] + Piece.Pivot()[0];
        double Y = Back[1] + Piece.Pivot()[1];
        double Z = Back[2] + Piece.Pivot()[2];
        return X >= Piece.Min()[0] && X < Piece.Max()[0] && Y >= Piece.Min()[1] && Y < Piece.Max()[1] && Z >= Piece.Min()[2] && Z < Piece.Max()[2];
    }

    public static int draw(RuinCanvas Canvas, Figure Shape, Pose At, double MinY) {
        return draw(Canvas, Shape, At, MinY, Double.MAX_VALUE);
    }

    public static int draw(RuinCanvas Canvas, Figure Shape, Pose At, double MinY, double MaxY) {
        double[] Low = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] High = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Part Piece : Shape.parts()) {
            for (double[] Corner : corners(Piece)) {
                double[] Placed = toPlan(At, Corner);
                for (int Axis = 0; Axis < 3; Axis++) {
                    Low[Axis] = Math.min(Low[Axis], Placed[Axis]);
                    High[Axis] = Math.max(High[Axis], Placed[Axis]);
                }
            }
        }
        int Count = 0;
        RuinRandom Random = Canvas.random();
        for (int X = (int) Math.floor(Low[0]) - 1; X <= (int) Math.ceil(High[0]) + 1; X++) {
            for (int Y = (int) Math.floor(Low[1]) - 1; Y <= (int) Math.ceil(High[1]) + 1; Y++) {
                if (Y < MinY || Y > MaxY) {
                    continue;
                }
                for (int Z = (int) Math.floor(Low[2]) - 1; Z <= (int) Math.ceil(High[2]) + 1; Z++) {
                    double[] Local = toLocal(At, X + 0.5, Y + 0.5, Z + 0.5);
                    for (Part Piece : Shape.parts()) {
                        if (inside(Piece, Local)) {
                            Canvas.put(X, Y, Z, Piece.Mix().at(Random, X, Y, Z));
                            Count++;
                            break;
                        }
                    }
                }
            }
        }
        for (Line Stroke : Shape.lines()) {
            double[] A = toPlan(At, Stroke.From());
            double[] B = toPlan(At, Stroke.To());
            double Length = Math.max(Math.abs(B[0] - A[0]), Math.max(Math.abs(B[1] - A[1]), Math.abs(B[2] - A[2])));
            int Steps = Math.max(1, (int) Math.ceil(Length * 2));
            for (int Step = 0; Step <= Steps; Step++) {
                double T = Step / (double) Steps;
                int X = (int) Math.floor(A[0] + (B[0] - A[0]) * T);
                int Y = (int) Math.floor(A[1] + (B[1] - A[1]) * T);
                int Z = (int) Math.floor(A[2] + (B[2] - A[2]) * T);
                if (Y < MinY || Y > MaxY) {
                    continue;
                }
                if (!Canvas.filled(X, Y, Z) || Canvas.at(X, Y, Z).shape() == RuinMat.Shape.Fence) {
                    Canvas.put(X, Y, Z, Stroke.Mat());
                    Count++;
                }
            }
        }
        return Count;
    }

    public static Figure knight(boolean Spear, double ArmRaise) {
        Figure Shape = new Figure();
        Shape.box(-4, 0, -2, 0, 12, 2, Robe).box(0, 0, -2, 4, 12, 2, Robe);
        Shape.box(-4.5, 12, -2.5, 4.5, 24, 2.5, Stone);
        Shape.box(-4.5, 18, -2.6, 4.5, 20, 2.6, RuinMix.Brass);
        Shape.box(-4, 24, -4, 4, 32, 4, Skin);
        Shape.box(-4.5, 29, -4.5, 4.5, 33, 4.5, Dark);
        Shape.limb(-8.5, 12, -2.5, -4.5, 24, 2.5, -6.5, 22, 0, -ArmRaise, 0, 0, Stone);
        Shape.limb(4.5, 12, -2.5, 8.5, 24, 2.5, 6.5, 22, 0, 0, 0, 0, Stone);
        Shape.box(8.5, 8, -4, 10.5, 22, 4, RuinMix.Brass);
        if (Spear) {
            double Reach = Math.sin(Math.toRadians(ArmRaise)) * 10;
            Shape.line(-6.5, -2, Reach - 2, -6.5, 44, Reach + 1, RuinMat.DarkOakFence);
            Shape.line(-6.5, 44, Reach + 1, -6.5, 48, Reach + 1.5, RuinMat.LightningRod);
        }
        return Shape;
    }

    public static Figure overseer(double ArmRaise) {
        Figure Shape = new Figure();
        Shape.box(-6, 0, -4, 6, 6, 4, Robe);
        Shape.box(-5, 6, -3, 5, 16, 3, Robe);
        Shape.box(-4.5, 16, -2.5, 4.5, 24, 2.5, Robe);
        Shape.box(-5, 13, -3.1, 5, 15, 3.1, RuinMix.Brass);
        Shape.box(-4, 24, -4, 4, 32, 4, Skin);
        Shape.box(-5, 25, -5, 5, 34, 2, Robe);
        Shape.limb(-8.5, 12, -2.5, -4.5, 24, 2.5, -6.5, 22, 0, -ArmRaise, 0, -8, Robe);
        Shape.limb(4.5, 12, -2.5, 8.5, 24, 2.5, 6.5, 22, 0, -ArmRaise, 0, 8, Robe);
        Shape.line(9, -1, 3, 9, 38, 3, RuinMat.Chain);
        return Shape;
    }

    public static Figure god(boolean Seated) {
        Figure Shape = new Figure();
        Shape.box(-2.8, 33, 3.4, -1.0, 34.8, 4.4, RuinMix.Gold);
        Shape.box(1.0, 33, 3.4, 2.8, 34.8, 4.4, RuinMix.Gold);
        Shape.box(-4.6, 37, -4.6, 4.6, 39.5, 4.6, RuinMix.Gold);
        Shape.box(-4, 30, -4, 4, 38, 4, Skin);
        Shape.box(-6.6, 27, -3.6, 6.6, 29, 3.6, RuinMix.Gold);
        Shape.box(-6.4, 14, -3.4, 6.4, 16, 3.4, RuinMix.Brass);
        Shape.box(-6, 12, -3, 6, 30, 3, GodBody);
        if (Seated) {
            Shape.box(-8.5, 0, -6, 8.5, 1.5, 8, RuinMix.Gold);
            Shape.box(-8, 1.5, -6, 8, 7, 7.5, Robe);
            Shape.box(-8, 7, -1, 8, 12, 7.5, Robe);
        } else {
            Shape.box(-7.6, 0, -4.6, 7.6, 2, 4.6, RuinMix.Gold);
            Shape.box(-7, 2, -4, 7, 12, 4, Robe);
        }
        double[][] Arms = {{27, 40, 0, 15}, {22, 4, -28, 14}, {17, -34, 0, 13}};
        for (double[] Arm : Arms) {
            double Y = Arm[0];
            double Length = Arm[3];
            Shape.limb(6 + Length, Y - 2.5, -2.5, 6 + Length + 2.5, Y + 2.5, 2.5, 6, Y, 0, 0, Arm[2], Arm[1], RuinMix.Gold);
            Shape.limb(-6 - Length - 2.5, Y - 2.5, -2.5, -6 - Length, Y + 2.5, 2.5, -6, Y, 0, 0, -Arm[2], -Arm[1], RuinMix.Gold);
            Shape.limb(6, Y - 2, -2, 6 + Length, Y + 2, 2, 6, Y, 0, 0, Arm[2], Arm[1], Skin);
            Shape.limb(-6 - Length, Y - 2, -2, -6, Y + 2, 2, -6, Y, 0, 0, -Arm[2], -Arm[1], Skin);
        }
        Shape.line(-3.6, 39.5, 0, -3.6, 43, 0, RuinMat.LightningRod);
        Shape.line(3.6, 39.5, 0, 3.6, 43, 0, RuinMat.LightningRod);
        Shape.line(0, 39.5, -3.6, 0, 43.5, -3.6, RuinMat.LightningRod);
        Shape.line(0, 39.5, 3.6, 0, 44.5, 3.6, RuinMat.LightningRod);
        return Shape;
    }

    public static Figure king(boolean Sword) {
        Figure Shape = new Figure();
        Shape.box(-5, 0, -3, 5, 10, 3, Robe);
        Shape.box(-4.5, 10, -2.5, 4.5, 24, 2.5, Robe);
        Shape.box(-4.6, 14, -2.6, 4.6, 16, 2.6, RuinMix.Gold);
        Shape.box(-4, 24, -4, 4, 32, 4, Skin);
        Shape.box(-4.5, 31, -4.5, 4.5, 33, 4.5, RuinMix.Gold);
        Shape.line(-3.5, 33, -3.5, -3.5, 36, -3.5, RuinMat.LightningRod);
        Shape.line(3.5, 33, 3.5, 3.5, 36, 3.5, RuinMat.LightningRod);
        Shape.line(0, 33, 4, 0, 37, 4, RuinMat.LightningRod);
        Shape.limb(-8.5, 12, -2.5, -4.5, 24, 2.5, -6.5, 22, 0, -20, 0, 0, Robe);
        Shape.limb(4.5, 12, -2.5, 8.5, 24, 2.5, 6.5, 22, 0, -20, 0, 0, Robe);
        if (Sword) {
            Shape.line(0, 2, 7, 0, 16, 7, RuinMat.BlackstoneWall);
        }
        return Shape;
    }

    public static Figure legs() {
        Figure Shape = new Figure();
        Shape.box(-4, 0, -2, 0, 12, 2, Robe).box(0, 0, -2, 4, 12, 2, Robe);
        Shape.box(-4.6, 0, -2.6, -0.4, 3, 2.6, RuinMix.Brass).box(0.4, 0, -2.6, 4.6, 3, 2.6, RuinMix.Brass);
        return Shape;
    }

    public static Figure upper(boolean Spear, boolean Head) {
        Figure Shape = new Figure();
        Shape.box(-4.5, 12, -2.5, 4.5, 24, 2.5, Stone);
        Shape.box(-4.5, 18, -2.6, 4.5, 20, 2.6, RuinMix.Brass);
        if (Head) {
            Shape.box(-4, 24, -4, 4, 32, 4, Skin);
            Shape.box(-4.5, 29, -4.5, 4.5, 33, 4.5, RuinMix.Gold);
        }
        Shape.box(-8.5, 12, -2.5, -4.5, 24, 2.5, Stone);
        Shape.box(4.5, 12, -2.5, 8.5, 24, 2.5, Stone);
        if (Spear) {
            Shape.line(-6.5, 4, 0, -6.5, 44, 0, RuinMat.DarkOakLog);
        }
        return Shape;
    }

    public static Figure headAlone() {
        Figure Shape = new Figure();
        Shape.box(-3.2, -0.2, 3.2, -1.0, 1.8, 4.4, Dark);
        Shape.box(1.0, -0.2, 3.2, 3.2, 1.8, 4.4, Dark);
        Shape.box(-2.0, -3.0, 3.2, 2.0, -1.8, 4.4, Dark);
        Shape.box(-4, -4, -4, 4, 4, 4, Skin);
        Shape.box(-4.5, 3, -4.5, 4.5, 5, 4.5, RuinMix.Gold);
        return Shape;
    }

    public static Figure head() {
        Figure Shape = new Figure();
        Shape.box(-4, 24, -4, 4, 32, 4, Skin);
        Shape.box(-4.5, 29, -4.5, 4.5, 33, 4.5, RuinMix.Gold);
        return Shape;
    }

    public static Figure needle(double Height, double Base) {
        Figure Shape = new Figure();
        Shape.box(-Base, 0, -Base, Base, Height * 0.8, Base, Stone);
        Shape.box(-Base * 0.7, Height * 0.8, -Base * 0.7, Base * 0.7, Height * 0.95, Base * 0.7, Stone);
        Shape.box(-Base * 0.35, Height * 0.95, -Base * 0.35, Base * 0.35, Height, Base * 0.35, RuinMix.Gold);
        return Shape;
    }
}
