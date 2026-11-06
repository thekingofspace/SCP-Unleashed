package net.rai.scp_unleashed.scp087;

import net.minecraft.util.Mth;

public final class Scp087Darkness {
    public static final float TopFog = 16.0f;
    public static final float DeepFog = 6.0f;
    public static final float InertFog = 30.0f;
    public static final int DeepAt = 40;
    public static final int ShadowFrom = 6;
    public static final float ShadowLimit = 0.6f;

    private Scp087Darkness() {
    }

    public static float fogFar(int Flights, boolean Inert) {
        if (Inert) {
            return InertFog;
        }
        float Share = Mth.clamp(Flights / (float) DeepAt, 0.0f, 1.0f);
        return Mth.lerp(Share, TopFog, DeepFog);
    }

    public static float shadow(int Flights, boolean Inert) {
        if (Inert) {
            return 0.0f;
        }
        float Share = Mth.clamp((Flights - ShadowFrom) / (float) (DeepAt - ShadowFrom), 0.0f, 1.0f);
        return Share * ShadowLimit;
    }
}
