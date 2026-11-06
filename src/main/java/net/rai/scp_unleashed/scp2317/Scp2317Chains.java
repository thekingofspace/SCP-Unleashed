package net.rai.scp_unleashed.scp2317;

import net.minecraft.world.phys.Vec3;

public final class Scp2317Chains {
    public static final float ModelScale = 2.5f;
    public static final String[] Order = {"flank_a", "flank_b", "waist_a", "waist_b", "neck", "wrist_b", "wrist_a"};
    public static final double[][] Attach = {
            {-0.8281, 3.1348, 0.5714},
            {1.8281, 3.1348, 0.5714},
            {-0.9062, 2.6402, -0.9818},
            {1.9062, 2.6402, -0.9818},
            {0.5000, 6.3332, -3.9095},
            {7.5052, 4.3403, -1.7790},
            {-6.5052, 4.3403, -1.7790},
    };
    public static final int[] Pillar = {4, 3, 5, 2, 0, 1, 6};
    public static final double[] Lift = {2.4, 2.4, 1.6, 1.6, 2.0, 2.6, 2.6};
    public static final double PillarHalf = 1.5;
    public static final double FaceGap = 0.06;

    private Scp2317Chains() {
    }

    public static int count() {
        return Order.length;
    }

    public static Vec3 attach(int Index) {
        double[] Point = Attach[Index];
        return new Vec3(Point[0], Point[1], Point[2]);
    }

    public static Vec3 anchor(int Index) {
        int[] Centre = Scp2317Plan.pillarCentre(Pillar[Index], Scp2317Plan.CavePillarRadius);
        double Cx = Centre[0] + 0.5;
        double Cz = Centre[1] + 0.5;
        double Dx = 0.5 - Cx;
        double Dz = 0.5 - Cz;
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        double Ux = Dx / Length;
        double Uz = Dz / Length;
        double Inset = PillarHalf / Math.max(Math.abs(Ux), Math.abs(Uz)) + FaceGap;
        return new Vec3(Cx + Ux * Inset, attach(Index).y + Lift[Index], Cz + Uz * Inset);
    }

    public static int openEyes(int Broken) {
        return Math.min(12, (int) Math.ceil(Broken * 12.0 / count()));
    }
}
