package net.rai.scp_unleashed.scp001;

import net.minecraft.util.RandomSource;

public final class Scp001Jitter {
    public static final String[] Bones = {"head", "neck_upper", "neck", "chest", "waist"};
    public static final float[] Reach = {5.0f, 2.5f, 1.5f, 0.6f, 0.5f};
    public static final String[] Arms = {"arm_left", "arm_right"};
    public static final int[] ArmLimbs = {Scp001Gait.ArmLeft, Scp001Gait.ArmRight};
    public static final float ArmReach = 4.0f;
    public static final float BladeReach = 7.0f;
    public static final int Fingers = 4;
    public static final int Quiet = 0;
    public static final int Snapped = 1;
    public static final int Released = 2;
    public static final int Burst = 3;

    private final float[][] Offsets = new float[Bones.length][3];
    private final float[][] ArmOffsets = new float[Arms.length][3];
    private final float[] BladeOffsets = new float[Fingers * 2];
    private final float[] Snap = new float[3];
    private final String[] Blades = new String[Fingers * 2];
    private int SnapLeft;
    private int SecondLeft;
    private boolean Tilted;
    private int Timer;
    private int Sign = 1;

    public Scp001Jitter() {
        for (int Index = 0; Index < Fingers * 2; Index++) {
            Blades[Index] = "claw_" + (Index < Fingers ? "left" : "right") + "_" + (Index % Fingers) + "_2";
        }
    }

    public int tick(RandomSource Random, boolean Tremble, Scp001Gait Gait, double Phase, int SnapOdds) {
        int Outcome = Quiet;
        if (Tremble) {
            Sign = -Sign;
            for (int Index = 0; Index < Bones.length; Index++) {
                float Scale = Index == 0 ? 1.0f : Index == 1 ? 0.5f : 0.15f;
                Offsets[Index][0] = Sign * 4.0f * Scale;
                Offsets[Index][1] = -Sign * 6.0f * Scale;
                Offsets[Index][2] = Sign * 5.0f * Scale;
            }
            for (int Index = 0; Index < BladeOffsets.length; Index++) {
                BladeOffsets[Index] = Sign * BladeReach * 0.8f;
            }
            Timer = 0;
        } else if (--Timer <= 0) {
            Timer = 2 + Random.nextInt(4);
            for (int Index = 0; Index < Bones.length; Index++) {
                for (int Axis = 0; Axis < 3; Axis++) {
                    Offsets[Index][Axis] = (Random.nextFloat() * 2.0f - 1.0f) * Reach[Index];
                }
            }
            for (int Index = 0; Index < BladeOffsets.length; Index++) {
                BladeOffsets[Index] = Random.nextInt(3) == 0 ? (Random.nextFloat() * 2.0f - 1.0f) * BladeReach : 0.0f;
            }
        }
        for (int Index = 0; Index < Arms.length; Index++) {
            boolean Swinging = Gait.swinging(ArmLimbs[Index], Phase);
            for (int Axis = 0; Axis < 3; Axis++) {
                ArmOffsets[Index][Axis] = Swinging && Timer % 2 == 0 ? (Random.nextFloat() * 2.0f - 1.0f) * ArmReach : Swinging ? ArmOffsets[Index][Axis] : 0.0f;
            }
        }
        if (SnapLeft > 0) {
            if (--SnapLeft == 0) {
                if (SecondLeft > 0) {
                    Snap[1] = -Snap[1] * 1.15f;
                    SnapLeft = SecondLeft;
                    SecondLeft = 0;
                    return Snapped;
                }
                Snap[0] = Snap[1] = Snap[2] = 0.0f;
                Outcome = Tilted ? Released : Quiet;
                Tilted = false;
            }
        } else if (!Tremble && SnapOdds > 0 && Random.nextInt(SnapOdds) == 0) {
            int Kind = Random.nextInt(5);
            float Turn = (25.0f + Random.nextFloat() * 25.0f) * (Random.nextBoolean() ? 1.0f : -1.0f);
            if (Kind <= 1) {
                Tilted = true;
                Snap[0] = (Random.nextFloat() - 0.5f) * 12.0f;
                Snap[1] = Turn * 0.3f;
                Snap[2] = (60.0f + Random.nextFloat() * 50.0f) * (Random.nextBoolean() ? 1.0f : -1.0f);
                SnapLeft = 3 + Random.nextInt(5);
                Outcome = Snapped;
            } else if (Kind == 2) {
                Snap[0] = (Random.nextFloat() - 0.5f) * 8.0f;
                Snap[1] = Turn;
                Snap[2] = Turn * 0.3f;
                SnapLeft = 1;
                SecondLeft = 1 + Random.nextInt(2);
                Outcome = Burst;
            } else {
                Snap[0] = (Random.nextFloat() - 0.5f) * 14.0f;
                Snap[1] = Turn;
                Snap[2] = (Random.nextFloat() - 0.5f) * 30.0f;
                SnapLeft = 2 + Random.nextInt(3);
                Outcome = Snapped;
            }
        }
        return Outcome;
    }

    public float[][] offsets() {
        return Offsets;
    }

    public float[][] armOffsets() {
        return ArmOffsets;
    }

    public float[] bladeOffsets() {
        return BladeOffsets;
    }

    public String[] blades() {
        return Blades;
    }

    public float[] snap() {
        return Snap;
    }

    public void calm() {
        SnapLeft = 0;
        SecondLeft = 0;
        Tilted = false;
        Snap[0] = Snap[1] = Snap[2] = 0.0f;
    }
}
