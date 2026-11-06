package net.rai.scp_unleashed.scp439.client;

import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.function.Function;

public final class Scp439HivePose {
    public static final float SeatDrop = -9.75f;
    public static final float VillagerSeatDrop = -8.2f;
    public static final float MaskShown = 4.6f;
    public static final String[] HumanBones = {"right_leg", "left_leg", "waist", "head", "right_arm", "left_arm"};
    public static final float[][] HumanAngles = {{-90f, 14f, 0f}, {-91f, -16f, 0f}, {22f, 3f, 4f}, {-22f, 9f, 5f}, {-10f, 0f, 24f}, {-8f, 0f, -26f}};
    public static final String[] VillagerBones = {"right_leg", "left_leg", "waist", "skirt", "head", "arms"};
    public static final float[][] VillagerAngles = {{-90f, 14f, 0f}, {-91f, -16f, 0f}, {22f, 3f, 4f}, {-116f, 0f, 0f}, {-22f, 9f, 5f}, {4f, 0f, 0f}};
    public static final float JawRest = 16f;
    public static final float JawStrain = 16f;
    public static final float JawHurt = 14f;
    public static final float JawHeave = 18f;
    public static final float JawDeath = 24f;
    public static final String[] Settled = {"root", "waist", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg", "skirt", "arms"};

    private Scp439HivePose() {
    }

    public static float[] shakes(float Seconds) {
        float First = (float) (Math.sin(Seconds * 13.1) * (0.5 + 0.5 * Math.sin(Seconds * 1.7)));
        float Second = (float) (Math.sin(Seconds * 10.3 + 1.1) * (0.5 + 0.5 * Math.sin(Seconds * 2.3 + 0.4)));
        float Third = (float) (Math.sin(Seconds * 15.7 + 2.0) * (0.5 + 0.5 * Math.sin(Seconds * 1.1 + 2.2)));
        return new float[]{First, Second, Third};
    }

    public static float strain(float Seconds) {
        return (float) (0.5 + 0.5 * Math.sin(Seconds * 1.05 - 1.5708));
    }

    public static float jaw(float Seconds, float Hurt, float Heave, float Death) {
        return JawRest + JawStrain * strain(Seconds) + 1.2f * shakes(Seconds)[2] + JawHurt * Hurt + JawHeave * Heave + JawDeath * Death;
    }

    public static void apply(Function<String, GeoBone> Lookup, boolean Rural, float Seconds, float Hurt, float Heave, float Death) {
        for (String Name : Settled) {
            GeoBone Found = Lookup.apply(Name);
            if (Found != null) {
                Found.setPosX(0.0f);
                Found.setPosY(0.0f);
                Found.setPosZ(0.0f);
                Found.setScaleX(1.0f);
                Found.setScaleY(1.0f);
                Found.setScaleZ(1.0f);
            }
        }
        String[] Bones = Rural ? VillagerBones : HumanBones;
        float[][] Angles = Rural ? VillagerAngles : HumanAngles;
        float[] Shake = shakes(Seconds);
        for (int Index = 0; Index < Bones.length; Index++) {
            float[] Angle = Angles[Index];
            GeoBone Found = Lookup.apply(Bones[Index]);
            if (Bones[Index].equals("head")) {
                turn(Found, Angle[0] + 1.2f * Shake[0] - 8.0f * Hurt - 9.0f * Heave + 12.0f * Death, Angle[1] + 1.0f * Shake[1], Angle[2] + 0.8f * Shake[2] + 4.0f * Hurt);
            } else if (Bones[Index].equals("waist")) {
                turn(Found, Angle[0] + 0.2f * Shake[1], Angle[1], Angle[2] + 0.15f * Shake[0]);
            } else {
                turn(Found, Angle[0], Angle[1], Angle[2]);
            }
        }
        GeoBone Root = Lookup.apply("root");
        if (Root != null) {
            Root.setPosY((Rural ? VillagerSeatDrop : SeatDrop) - 2.0f * Death);
            Root.setScaleY(1.0f - 0.55f * Death);
            Root.setScaleX(1.0f + 0.12f * Death);
            Root.setScaleZ(1.0f + 0.12f * Death);
        }
    }

    private static void turn(GeoBone Found, float X, float Y, float Z) {
        if (Found == null) {
            return;
        }
        Found.setRotX(Found.getInitialSnapshot().getRotX() - X * Mth.DEG_TO_RAD);
        Found.setRotY(Found.getInitialSnapshot().getRotY() - Y * Mth.DEG_TO_RAD);
        Found.setRotZ(Found.getInitialSnapshot().getRotZ() + Z * Mth.DEG_TO_RAD);
    }
}
