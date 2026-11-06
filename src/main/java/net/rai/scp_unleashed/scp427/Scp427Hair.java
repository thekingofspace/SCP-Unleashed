package net.rai.scp_unleashed.scp427;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class Scp427Hair {
    public static final int Chains = 8;
    public static final int Segments = 3;
    public static final double RootLimit = 0.5;
    public static final double BendLimit = 0.3;
    public static final double Stiffness = 0.12;
    public static final double Damping = 0.24;
    public static final double Follow = 0.3;
    public static final double FollowDamping = 0.32;
    public static final double DragGain = 2.6;
    public static final double AccelGain = 3.5;
    public static final double TurnGain = 1.4;
    public static final double SwayBase = 0.05;
    public static final double HurtKick = 0.25;
    private final double[][] Angles = new double[Chains][Segments * 2];
    private final double[][] Before = new double[Chains][Segments * 2];
    private final double[][] Speeds = new double[Chains][Segments * 2];
    private final double[] Phases = new double[Chains];
    private final RandomSource Random = RandomSource.create();
    private double LastX;
    private double LastZ;
    private double SpeedForward;
    private double SpeedSide;
    private boolean Primed;
    private int LastHurt;
    private int Time;

    public Scp427Hair() {
        for (int Chain = 0; Chain < Chains; Chain++) {
            Phases[Chain] = Random.nextDouble() * Math.PI * 2.0;
        }
    }

    public void tick(Scp427Flesh Owner) {
        Time++;
        double Theta = Math.toRadians(Owner.yBodyRot);
        double WorldX = Primed ? Owner.getX() - LastX : 0.0;
        double WorldZ = Primed ? Owner.getZ() - LastZ : 0.0;
        LastX = Owner.getX();
        LastZ = Owner.getZ();
        Primed = true;
        double Forward = -Math.sin(Theta) * WorldX + Math.cos(Theta) * WorldZ;
        double Side = Math.cos(Theta) * WorldX + Math.sin(Theta) * WorldZ;
        double AccelForward = Forward - SpeedForward;
        double AccelSide = Side - SpeedSide;
        SpeedForward = Forward;
        SpeedSide = Side;
        double YawRate = Math.toRadians(Mth.wrapDegrees(Owner.yBodyRot - Owner.yBodyRotO));
        boolean Dead = Owner.isDeadOrDying();
        boolean Hurt = Owner.hurtTime > LastHurt && !Dead;
        LastHurt = Owner.hurtTime;
        for (int Chain = 0; Chain < Chains; Chain++) {
            double[] Angle = Angles[Chain];
            double[] Speed = Speeds[Chain];
            System.arraycopy(Angle, 0, Before[Chain], 0, Angle.length);
            double TargetX = -Forward * DragGain;
            double TargetZ = Side * DragGain - YawRate * TurnGain;
            if (!Dead) {
                TargetX += SwayBase * Math.sin(Time * 0.06 + Phases[Chain]);
                TargetZ += SwayBase * Math.sin(Time * 0.045 + Phases[Chain] * 1.7);
            }
            Speed[0] += (TargetX - Angle[0]) * Stiffness + AccelForward * AccelGain;
            Speed[1] += (TargetZ - Angle[1]) * Stiffness - AccelSide * AccelGain;
            if (Hurt) {
                for (int Part = 0; Part < Segments * 2; Part++) {
                    Speed[Part] += (Random.nextDouble() - 0.5) * 2.0 * HurtKick;
                }
            }
            Speed[0] *= 1.0 - Damping;
            Speed[1] *= 1.0 - Damping;
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
                Speed[Here] += (Previous[Up] - Angle[Here]) * Follow;
                Speed[Here + 1] += (Previous[Up + 1] - Angle[Here + 1]) * Follow;
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

    public float local(int Chain, int Part, int Axis, float Partial) {
        double Now = Angles[Chain][Part * 2 + Axis];
        double Then = Before[Chain][Part * 2 + Axis];
        double Value = Then + (Now - Then) * Partial;
        if (Part == 0) {
            return (float) Value;
        }
        double ParentNow = Angles[Chain][(Part - 1) * 2 + Axis];
        double ParentThen = Before[Chain][(Part - 1) * 2 + Axis];
        return (float) (Value - (ParentThen + (ParentNow - ParentThen) * Partial));
    }
}
