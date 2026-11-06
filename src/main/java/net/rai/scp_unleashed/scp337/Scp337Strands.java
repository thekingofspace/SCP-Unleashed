package net.rai.scp_unleashed.scp337;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.List;

public final class Scp337Strands {
    public static final double RootLimit = 0.45;
    public static final double BendLimit = 0.3;
    public static final double RootStiffness = 0.1;
    public static final double RootDamping = 0.22;
    public static final double FollowStiffness = 0.28;
    public static final double FollowDamping = 0.3;
    public static final double AccelGain = 12.0;
    public static final double DragGain = 14.0;
    public static final double TurnDrag = 1.6;
    public static final double SpinGain = 2.5;
    public static final double SwayBase = 0.08;
    public static final double SwayAgitated = 0.22;
    public static final double JitterChance = 0.18;
    public static final double JitterKick = 0.12;
    public static final double HurtKick = 0.3;
    public static final double DeathStiffness = 0.05;
    public static final double FlinchDecay = 0.86;
    public static final double FlinchDegrees = 7.0;
    public static final double AgitationRise = 0.12;
    public static final double AgitationFall = 0.03;

    private Table Bound;
    private double[][] Angles;
    private double[][] Before;
    private double[][] Speeds;
    private double[][] Phases;
    private final RandomSource Random = RandomSource.create();
    private int Time;
    private double LastX;
    private double LastZ;
    private double SpeedX;
    private double SpeedZ;
    private boolean Primed;
    private int LastHurt;
    private double Agitation;
    private double Flinch;
    private double FlinchBefore;

    public boolean bound() {
        return Bound != null;
    }

    public Table table() {
        return Bound;
    }

    public void bind(Table Value) {
        if (Bound == Value) {
            return;
        }
        Bound = Value;
        int Count = Value.Names.length;
        Angles = new double[Count][];
        Before = new double[Count][];
        Speeds = new double[Count][];
        Phases = new double[Count][2];
        for (int Chain = 0; Chain < Count; Chain++) {
            int Size = Value.Names[Chain].length * 2;
            Angles[Chain] = new double[Size];
            Before[Chain] = new double[Size];
            Speeds[Chain] = new double[Size];
            Phases[Chain][0] = Random.nextDouble() * Math.PI * 2.0;
            Phases[Chain][1] = Random.nextDouble() * Math.PI * 2.0;
        }
    }

    public void tick(Scp337Mass Mass) {
        if (Bound == null) {
            return;
        }
        Time++;
        double Theta = Math.toRadians(180.0 - Mass.yBodyRot);
        double Cos = Math.cos(Theta);
        double Sin = Math.sin(Theta);
        double WorldX = Primed ? Mass.getX() - LastX : 0.0;
        double WorldZ = Primed ? Mass.getZ() - LastZ : 0.0;
        LastX = Mass.getX();
        LastZ = Mass.getZ();
        Primed = true;
        double LocalX = Cos * WorldX - Sin * WorldZ;
        double LocalZ = Sin * WorldX + Cos * WorldZ;
        double AccelX = LocalX - SpeedX;
        double AccelZ = LocalZ - SpeedZ;
        SpeedX = LocalX;
        SpeedZ = LocalZ;
        double YawRate = -Math.toRadians(Mth.wrapDegrees(Mass.yBodyRot - Mass.yBodyRotO));
        boolean Dead = Mass.isDeadOrDying();
        boolean Hurt = Mass.hurtTime > LastHurt && !Dead;
        LastHurt = Mass.hurtTime;
        double Goal = Mass.isWrithing() && !Dead ? 1.0 : 0.0;
        Agitation += (Goal - Agitation) * (Goal > Agitation ? AgitationRise : AgitationFall);
        FlinchBefore = Flinch;
        Flinch *= FlinchDecay;
        if (Hurt) {
            Flinch = 1.0;
        }
        double Stiffness = Dead ? DeathStiffness : RootStiffness;
        for (int Chain = 0; Chain < Angles.length; Chain++) {
            double[] Angle = Angles[Chain];
            double[] Speed = Speeds[Chain];
            System.arraycopy(Angle, 0, Before[Chain], 0, Angle.length);
            double Rx = Bound.RootX[Chain];
            double Rz = Bound.RootZ[Chain];
            double SpinX = YawRate * Rz;
            double SpinZ = -YawRate * Rx;
            double TargetX = -SpeedX * DragGain - SpinX * TurnDrag + Rx * YawRate * YawRate * SpinGain;
            double TargetZ = -SpeedZ * DragGain - SpinZ * TurnDrag + Rz * YawRate * YawRate * SpinGain;
            if (!Dead) {
                double Sway = SwayBase + SwayAgitated * Agitation;
                TargetX += Sway * Math.sin(Time * 0.05 + Phases[Chain][0]);
                TargetZ += Sway * Math.sin(Time * 0.037 + Phases[Chain][1]);
            }
            Speed[0] += (TargetX - Angle[0]) * Stiffness - AccelX * AccelGain;
            Speed[1] += (TargetZ - Angle[1]) * Stiffness - AccelZ * AccelGain;
            if (!Dead && Agitation > 0.0 && Random.nextDouble() < JitterChance * Agitation) {
                Speed[0] += (Random.nextDouble() - 0.5) * 2.0 * JitterKick * Agitation;
                Speed[1] += (Random.nextDouble() - 0.5) * 2.0 * JitterKick * Agitation;
            }
            int Segments = Angle.length / 2;
            if (Hurt) {
                for (int Part = 0; Part < Segments; Part++) {
                    Speed[Part * 2] += (Random.nextDouble() - 0.5) * 2.0 * HurtKick;
                    Speed[Part * 2 + 1] += (Random.nextDouble() - 0.5) * 2.0 * HurtKick;
                }
            }
            Speed[0] *= 1.0 - RootDamping;
            Speed[1] *= 1.0 - RootDamping;
            Angle[0] += Speed[0];
            Angle[1] += Speed[1];
            double Size = Math.sqrt(Angle[0] * Angle[0] + Angle[1] * Angle[1]);
            if (Size > RootLimit) {
                Angle[0] *= RootLimit / Size;
                Angle[1] *= RootLimit / Size;
            }
            double[] Previous = Before[Chain];
            for (int Part = 1; Part < Segments; Part++) {
                int Here = Part * 2;
                int Up = Here - 2;
                Speed[Here] += (Previous[Up] - Angle[Here]) * FollowStiffness;
                Speed[Here + 1] += (Previous[Up + 1] - Angle[Here + 1]) * FollowStiffness;
                Speed[Here] *= 1.0 - FollowDamping;
                Speed[Here + 1] *= 1.0 - FollowDamping;
                Angle[Here] += Speed[Here];
                Angle[Here + 1] += Speed[Here + 1];
                double BendX = Angle[Here] - Angle[Up];
                double BendZ = Angle[Here + 1] - Angle[Up + 1];
                double Bend = Math.sqrt(BendX * BendX + BendZ * BendZ);
                if (Bend > BendLimit) {
                    Angle[Here] = Angle[Up] + BendX * BendLimit / Bend;
                    Angle[Here + 1] = Angle[Up + 1] + BendZ * BendLimit / Bend;
                }
            }
        }
    }

    public double flinch(float Partial) {
        return (FlinchBefore + (Flinch - FlinchBefore) * Partial) * Math.toRadians(FlinchDegrees);
    }

    public double agitation() {
        return Agitation;
    }

    public void pose(int Chain, float Partial, double[][] Out) {
        Table Data = Bound;
        int Segments = Data.Names[Chain].length;
        double[] Now = Angles[Chain];
        double[] Then = Before[Chain];
        double[] Previous = identity();
        for (int Part = 0; Part < Segments; Part++) {
            double PushX = Then[Part * 2] + (Now[Part * 2] - Then[Part * 2]) * Partial;
            double PushZ = Then[Part * 2 + 1] + (Now[Part * 2 + 1] - Then[Part * 2 + 1]) * Partial;
            double[] Bent = multiply(deflection(Data.Heading[Chain], PushX, PushZ), Data.World[Chain][Part]);
            double[] Local = multiply(transpose(Previous), Bent);
            euler(Local, Out[Part]);
            Previous = Bent;
        }
    }

    public static double[] identity() {
        return new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
    }

    public static double[] multiply(double[] First, double[] Second) {
        double[] Result = new double[9];
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 3; Column++) {
                Result[Row * 3 + Column] = First[Row * 3] * Second[Column] + First[Row * 3 + 1] * Second[3 + Column] + First[Row * 3 + 2] * Second[6 + Column];
            }
        }
        return Result;
    }

    public static double[] transpose(double[] Matrix) {
        return new double[]{Matrix[0], Matrix[3], Matrix[6], Matrix[1], Matrix[4], Matrix[7], Matrix[2], Matrix[5], Matrix[8]};
    }

    public static double[] axisAngle(double X, double Y, double Z, double Angle) {
        double Cos = Math.cos(Angle);
        double Sin = Math.sin(Angle);
        double Other = 1.0 - Cos;
        return new double[]{
                Cos + X * X * Other, X * Y * Other - Z * Sin, X * Z * Other + Y * Sin,
                Y * X * Other + Z * Sin, Cos + Y * Y * Other, Y * Z * Other - X * Sin,
                Z * X * Other - Y * Sin, Z * Y * Other + X * Sin, Cos + Z * Z * Other};
    }

    public static double[] deflection(double[] Heading, double PushX, double PushZ) {
        double Size = Math.sqrt(PushX * PushX + PushZ * PushZ);
        if (Size < 1.0e-6) {
            return identity();
        }
        double Tx = PushX / Size;
        double Tz = PushZ / Size;
        double Ax = Heading[1] * Tz;
        double Ay = Heading[2] * Tx - Heading[0] * Tz;
        double Az = -Heading[1] * Tx;
        double Length = Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
        if (Length < 1.0e-6) {
            return identity();
        }
        return axisAngle(Ax / Length, Ay / Length, Az / Length, Size);
    }

    public static double[] fromEuler(double Rx, double Ry, double Rz) {
        double[] X = axisAngle(1.0, 0.0, 0.0, Rx);
        double[] Y = axisAngle(0.0, 1.0, 0.0, Ry);
        double[] Z = axisAngle(0.0, 0.0, 1.0, Rz);
        return multiply(Z, multiply(Y, X));
    }

    public static void euler(double[] Matrix, double[] Out) {
        double Ry = Math.asin(Mth.clamp(-Matrix[6], -1.0, 1.0));
        double Rx;
        double Rz;
        if (Math.abs(Math.cos(Ry)) > 1.0e-6) {
            Rx = Math.atan2(Matrix[7], Matrix[8]);
            Rz = Math.atan2(Matrix[3], Matrix[0]);
        } else {
            Rx = Math.atan2(-Matrix[5], Matrix[4]);
            Rz = 0.0;
        }
        Out[0] = Rx;
        Out[1] = Ry;
        Out[2] = Rz;
    }

    public static final class Table {
        public final String[][] Names;
        public final double[][][] World;
        public final double[][] Heading;
        public final double[] RootX;
        public final double[] RootZ;

        public Table(List<String[]> ChainNames, List<double[][]> Rests, List<double[]> Roots, List<Boolean> Rising) {
            int Count = ChainNames.size();
            Names = ChainNames.toArray(new String[0][]);
            World = new double[Count][][];
            Heading = new double[Count][];
            RootX = new double[Count];
            RootZ = new double[Count];
            for (int Chain = 0; Chain < Count; Chain++) {
                double[][] Rest = Rests.get(Chain);
                World[Chain] = new double[Rest.length][];
                double[] Current = identity();
                for (int Part = 0; Part < Rest.length; Part++) {
                    Current = multiply(Current, fromEuler(Rest[Part][0], Rest[Part][1], Rest[Part][2]));
                    World[Chain][Part] = Current;
                }
                double Sign = Rising.get(Chain) ? 1.0 : -1.0;
                double[] First = World[Chain][0];
                Heading[Chain] = new double[]{First[1] * Sign, First[4] * Sign, First[7] * Sign};
                RootX[Chain] = Roots.get(Chain)[0] / 16.0;
                RootZ[Chain] = Roots.get(Chain)[1] / 16.0;
            }
        }
    }
}
