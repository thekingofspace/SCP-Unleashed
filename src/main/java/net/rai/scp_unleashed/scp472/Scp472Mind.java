package net.rai.scp_unleashed.scp472;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class Scp472Mind {
    public static final float Full = 100.0f;
    public static final double Reach = 24.0;
    public static final double HeardRange = 32.0;
    public static final float EdgeDrain = 0.05f;
    public static final float CoreDrain = 4.5f;
    public static final float DrainCurve = 3.0f;
    public static final float[] Thresholds = {95.0f, 80.0f, 62.0f, 45.0f, 28.0f, 12.0f};
    public static final int Stages = Thresholds.length;
    public static final int RemarkStage = 5;
    public static final int HarmStage = 6;
    public static final int VisionStage = 4;
    public static final int HarmInterval = 40;
    public static final float HarmBase = 1.0f;
    public static final float HarmExtra = 1.0f;

    private Scp472Mind() {
    }

    public static float drainPerSecond(double Distance) {
        if (Distance > Reach) {
            return 0.0f;
        }
        float Closeness = 1.0f - (float) Mth.clamp(Distance / Reach, 0.0, 1.0);
        return EdgeDrain + (CoreDrain - EdgeDrain) * (float) Math.pow(Closeness, DrainCurve);
    }

    public static int stage(float Sanity) {
        int Stage = 0;
        for (float Threshold : Thresholds) {
            if (Sanity < Threshold) {
                Stage++;
            }
        }
        return Stage;
    }

    public static float harm(float Sanity) {
        float Depth = Mth.clamp((Thresholds[HarmStage - 1] - Sanity) / Thresholds[HarmStage - 1], 0.0f, 1.0f);
        return HarmBase + HarmExtra * Depth;
    }

    public static float sanity(LivingEntity Subject) {
        Float Found = Subject.getExistingDataOrNull(Scp472Registry.Sanity.get());
        return Found == null ? Full : Found;
    }

    public static int stageOf(LivingEntity Subject) {
        return stage(sanity(Subject));
    }

    public static boolean isTouched(LivingEntity Subject) {
        return Subject.hasData(Scp472Registry.Sanity.get());
    }

    public static void store(LivingEntity Subject, float Sanity) {
        float Clamped = Mth.clamp(Sanity, 0.0f, Full);
        if (Clamped >= Full) {
            Subject.removeData(Scp472Registry.Sanity.get());
        } else {
            Subject.setData(Scp472Registry.Sanity.get(), Clamped);
        }
    }

    public static void forget(LivingEntity Subject) {
        Subject.removeData(Scp472Registry.Sanity.get());
    }
}
