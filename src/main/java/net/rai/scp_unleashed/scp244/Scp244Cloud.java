package net.rai.scp_unleashed.scp244;

import net.minecraft.util.Mth;

public final class Scp244Cloud {
    public static final float Radius = 16.0f;
    public static final float SpreadPerTick = 0.12f;
    public static final float FrontSoft = 2.5f;
    public static final float MaxFront = 26.0f;
    public static final float RisePerTick = 1.0f / 40.0f;
    public static final int LingerTicks = 300;
    public static final float FadePerTick = 1.0f / 400.0f;
    public static final float EdgeDensity = 0.35f;
    public static final float Falloff = 0.8f;

    private Scp244Cloud() {
    }

    public static float shape(float Distance) {
        float Share = Mth.clamp(Distance / Radius, 0.0f, 1.0f);
        return 1.0f - (1.0f - EdgeDensity) * (float) Math.pow(Share, Falloff);
    }

    public static float reach(float Distance, float Front) {
        return Mth.clamp((Front - Distance) / FrontSoft, 0.0f, 1.0f);
    }

    public static float density(float Distance, float Front, float Intensity) {
        if (Intensity <= 0.0f || Front <= 0.0f) {
            return 0.0f;
        }
        return Intensity * shape(Distance) * reach(Distance, Front);
    }

    public record State(float Front, float Intensity, int ClosedTicks) {
        public static final State Still = new State(0.0f, 0.0f, 0);

        public State advance(boolean Open) {
            if (Open) {
                return new State(Math.min(MaxFront, Front + SpreadPerTick), Math.min(1.0f, Intensity + RisePerTick), 0);
            }
            if (Intensity <= 0.0f) {
                return Front == 0.0f && ClosedTicks == 0 ? this : Still;
            }
            int Closed = ClosedTicks + 1;
            float Left = Closed > LingerTicks ? Math.max(0.0f, Intensity - FadePerTick) : Intensity;
            return Left <= 0.0f ? Still : new State(Front, Left, Closed);
        }

        public boolean active() {
            return Intensity > 0.0f;
        }
    }
}
