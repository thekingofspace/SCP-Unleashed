package net.rai.scp_unleashed.scp465;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Scp465Lid {
    public static final float OpenAngle = 112.0f;
    public static final float Overshoot = 10.0f;
    public static final float PopTicks = 3.0f;
    public static final float PopAngle = 9.0f;
    public static final float SwingTicks = 8.0f;
    public static final float SettleTicks = 9.0f;
    public static final float FallTicks = 7.0f;
    public static final float BounceTicks = 6.0f;
    public static final float Bounce = 6.0f;
    public static final float OpenTicks = PopTicks + SwingTicks + SettleTicks;
    public static final float CloseTicks = FallTicks + BounceTicks;
    public static final float HingeY = 11.0f / 16.0f;
    public static final float HingeZ = 14.0f / 16.0f;

    private Scp465Lid() {
    }

    public static float duration(boolean Opening) {
        return Opening ? OpenTicks : CloseTicks;
    }

    public static float angle(boolean Opening, float From, float Ticks) {
        if (Opening) {
            if (Ticks < PopTicks) {
                float Progress = Math.max(0.0f, Ticks) / PopTicks;
                return From + (Math.max(From, PopAngle) - From) * Mth.sin(Progress * Mth.HALF_PI);
            }
            float Start = Math.max(From, PopAngle);
            if (Ticks < PopTicks + SwingTicks) {
                float Progress = (Ticks - PopTicks) / SwingTicks;
                return Start + (OpenAngle + Overshoot - Start) * swing(Progress);
            }
            float Settle = Math.min(1.0f, (Ticks - PopTicks - SwingTicks) / SettleTicks);
            float Remaining = 1.0f - Settle;
            return OpenAngle + Overshoot * Remaining * Remaining * Mth.cos(Settle * Mth.PI * 1.5f);
        }
        if (Ticks < FallTicks) {
            float Progress = Math.max(0.0f, Ticks) / FallTicks;
            return From * (1.0f - Progress * Progress);
        }
        float Hop = Math.min(1.0f, (Ticks - FallTicks) / BounceTicks);
        return Bounce * Mth.sin(Hop * Mth.PI) * (1.0f - Hop * 0.4f);
    }

    public static float swing(float Progress) {
        float Clamped = Mth.clamp(Progress, 0.0f, 1.0f);
        return Clamped < 0.5f ? 2.0f * Clamped * Clamped : 1.0f - (float) Math.pow(-2.0f * Clamped + 2.0f, 2.0f) / 2.0f;
    }

    public static float settled(boolean Open) {
        return Open ? OpenAngle : 0.0f;
    }

    public static float turn(int Quarter) {
        return 180.0f - Math.floorMod(Quarter, 4) * 90.0f;
    }

    public static Matrix4f transform(int Quarter, float Angle) {
        return new Matrix4f().translate(0.5f, 0.0f, 0.5f).rotateY((float) Math.toRadians(turn(Quarter))).translate(-0.5f, 0.0f, -0.5f)
                .translate(0.0f, HingeY, HingeZ).rotateX((float) Math.toRadians(Angle)).translate(0.0f, -HingeY, -HingeZ);
    }

    public static Matrix4f transform(Direction Facing, float Angle) {
        return transform(Facing.get2DDataValue(), Angle);
    }

    public static Vector3f place(Direction Facing, float Angle, float X, float Y, float Z) {
        return transform(Facing, Angle).transformPosition(new Vector3f(X, Y, Z));
    }
}
