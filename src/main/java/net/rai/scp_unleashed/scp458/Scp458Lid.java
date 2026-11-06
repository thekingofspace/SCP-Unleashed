package net.rai.scp_unleashed.scp458;

import net.minecraft.util.Mth;

public final class Scp458Lid {
    public static final float OpenAngle = 106.0f;
    public static final float Overshoot = 9.0f;
    public static final float SwingTicks = 9.0f;
    public static final float SettleTicks = 8.0f;
    public static final float FallTicks = 8.0f;
    public static final float BounceTicks = 6.0f;
    public static final float Bounce = 5.0f;
    public static final float OpenTicks = SwingTicks + SettleTicks;
    public static final float CloseTicks = FallTicks + BounceTicks;
    public static final float HingeY = 3.0f / 16.0f;
    public static final float HingeZ = 16.0f / 16.0f;
    public static final float PizzaShown = 2.5f;
    public static final float HingeShare = 0.5f;

    private Scp458Lid() {
    }

    public static float duration(boolean Opening) {
        return Opening ? OpenTicks : CloseTicks;
    }

    public static float angle(boolean Opening, float From, float Ticks) {
        if (Opening) {
            if (Ticks < SwingTicks) {
                float Progress = Math.max(0.0f, Ticks) / SwingTicks;
                return From + (OpenAngle + Overshoot - From) * swing(Progress);
            }
            float Settle = Math.min(1.0f, (Ticks - SwingTicks) / SettleTicks);
            float Remaining = 1.0f - Settle;
            return OpenAngle + Overshoot * Remaining * Remaining * Mth.cos(Settle * Mth.PI * 1.5f);
        }
        if (Ticks < FallTicks) {
            float Progress = Math.max(0.0f, Ticks) / FallTicks;
            return From * (1.0f - Progress * Progress);
        }
        float Hop = Math.min(1.0f, (Ticks - FallTicks) / BounceTicks);
        return Bounce * Mth.sin(Hop * Mth.PI) * (1.0f - Hop * 0.35f);
    }

    public static float swing(float Progress) {
        float Clamped = Mth.clamp(Progress, 0.0f, 1.0f);
        return Clamped < 0.5f ? 2.0f * Clamped * Clamped : 1.0f - (float) Math.pow(-2.0f * Clamped + 2.0f, 2.0f) / 2.0f;
    }

    public static float settled(boolean Open) {
        return Open ? OpenAngle : 0.0f;
    }
}
