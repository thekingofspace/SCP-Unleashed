package net.rai.scp_unleashed.scp2521.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp2521.Scp2521Motion;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationProcessor;

public final class Scp2521Sway {
    public static final float[] Omega = {Mth.TWO_PI / 4.0f, Mth.TWO_PI / 5.3f, Mth.TWO_PI / 6.7f};
    public static final float[] AmplitudeA = {7.0f, 6.0f, 3.5f};
    public static final float[] AmplitudeB = {4.0f, 3.0f, 2.5f};
    public static final float TwitchRate = 0.43f;
    public static final int TwitchSharp = 24;
    public static final float TwitchSize = 16.0f;
    public static final float ChainLag = 0.9f;
    public static final float BreathPeriod = 4.5f;
    public static final float ChestBreath = 0.025f;
    public static final float BellyBreath = 0.02f;
    public static final int Bundle = 2;
    public static final float GazeCoil = 0.9f;
    public static final float WalkDrift = 0.35f;
    public static final float WalkUncoil = 0.5f;
    public static final float WalkTrail = -0.35f;
    public static final float TiltDegrees = 20.0f;
    public static final float NodDegrees = 8.0f;
    public static final float LeanDegrees = 6.0f;

    private Scp2521Sway() {
    }

    public static void apply(AnimationProcessor<?> Processor, float Clock, Scp2521Motion.Sway Current) {
        float[] Rotation = new float[3];
        for (int Index = 0; Index < Scp2521SwayTable.Bones.length; Index++) {
            GeoBone Found = Processor.getBone(Scp2521SwayTable.Bones[Index]);
            if (Found == null) {
                continue;
            }
            int Kind = Scp2521SwayTable.Kinds[Index];
            int Link = Scp2521SwayTable.Indices[Index];
            float Weight = (Link + 1.0f) / Scp2521SwayTable.Counts[Index];
            float Phase = Scp2521SwayTable.Phases[Index];
            float Lag = Phase - Link * ChainLag;
            float First = AmplitudeA[Kind] * Current.Amount() * (0.4f + Weight) * Mth.sin(Clock * Omega[Kind] + Lag);
            float Second = AmplitudeB[Kind] * Current.Amount() * (0.3f + Weight) * Mth.sin(Clock * Omega[Kind] * 2.0f + Phase * 1.7f - Link * ChainLag);
            if (Kind != Bundle) {
                float Pulse = Math.max(0.0f, Mth.sin(Clock * TwitchRate + Scp2521SwayTable.Clusters[Index] * 2.1f));
                First += (float) Math.pow(Pulse, TwitchSharp) * TwitchSize * Weight;
            }
            float Bend = Current.Coil() * (8.0f + 10.0f * Weight);
            First += Bend * Scp2521SwayTable.Signs[Index];
            Second += Bend * 0.6f;
            First += Current.Flare() * -18.0f * (Link == 0 ? 1.0f : 0.4f);
            First += Current.Trail() * 22.0f * (Link == 0 ? 1.0f : 0.5f);
            Rotation[0] = 0.0f;
            Rotation[1] = 0.0f;
            Rotation[2] = 0.0f;
            Rotation[Scp2521SwayTable.AxesA[Index]] += First;
            Rotation[Scp2521SwayTable.AxesB[Index]] += Second;
            BoneSnapshot Rest = Found.getInitialSnapshot();
            Found.setRotX(Rest.getRotX() - Rotation[0] * Mth.DEG_TO_RAD);
            Found.setRotY(Rest.getRotY() - Rotation[1] * Mth.DEG_TO_RAD);
            Found.setRotZ(Rest.getRotZ() + Rotation[2] * Mth.DEG_TO_RAD);
        }
        breathe(Processor.getBone("chest"), Mth.sin(Clock / BreathPeriod * Mth.TWO_PI) * ChestBreath);
        breathe(Processor.getBone("belly"), Mth.sin(Clock / BreathPeriod * Mth.TWO_PI + 0.6f) * BellyBreath);
    }

    public static void attend(AnimationProcessor<?> Processor, float Gaze, float Lean, float Side) {
        GeoBone Look = Processor.getBone("look");
        if (Look != null) {
            BoneSnapshot Rest = Look.getInitialSnapshot();
            Look.setRotZ(Rest.getRotZ() + Gaze * TiltDegrees * Side * Mth.DEG_TO_RAD);
            Look.setRotX(Rest.getRotX() - Gaze * NodDegrees * Mth.DEG_TO_RAD);
        }
        GeoBone Body = Processor.getBone("body");
        if (Body != null) {
            BoneSnapshot Rest = Body.getInitialSnapshot();
            Body.setRotX(Rest.getRotX() - Lean * LeanDegrees * Mth.DEG_TO_RAD);
        }
    }

    private static void breathe(GeoBone Found, float Swell) {
        if (Found == null) {
            return;
        }
        BoneSnapshot Rest = Found.getInitialSnapshot();
        Found.setScaleX(Rest.getScaleX() * (1.0f + Swell));
        Found.setScaleZ(Rest.getScaleZ() * (1.0f + Swell));
    }
}
