package net.rai.scp_unleashed.scp902;

import net.minecraft.util.Mth;

public final class Scp902Scene {
    public static final int DepartTicks = 30;
    public static final int ArriveFade = 60;
    public static final int ArriveFadeLimit = 60;
    public static final int TextAt = 740;
    public static final int TextIn = 40;
    public static final int TextHold = 100;
    public static final int TextOut = 40;
    public static final int FinishAt = 940;
    public static final int FinishFade = 60;
    public static final int ReturnAt = FinishAt + FinishFade;
    public static final int HomeFade = 60;
    public static final int TotalTicks = DepartTicks + ReturnAt + HomeFade;

    private Scp902Scene() {
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static float text(float SceneTicks) {
        float Local = SceneTicks - TextAt;
        if (Local < 0.0f || Local > TextIn + TextHold + TextOut) {
            return 0.0f;
        }
        if (Local < TextIn) {
            return smooth(Local / TextIn);
        }
        if (Local < TextIn + TextHold) {
            return 1.0f;
        }
        return 1.0f - smooth((Local - TextIn - TextHold) / TextOut);
    }

    public static float arrivalWhite(float SceneTicks) {
        return 1.0f - smooth(SceneTicks / ArriveFade);
    }

    public static float finishWhite(float SceneTicks) {
        return smooth((SceneTicks - FinishAt) / FinishFade);
    }

    public static float sceneWhite(float SceneTicks) {
        return Math.max(arrivalWhite(SceneTicks), finishWhite(SceneTicks));
    }

    public static float departWhite(float Ticks) {
        return smooth(Ticks / DepartTicks);
    }

    public static float homeWhite(float Ticks) {
        return 1.0f - smooth(Ticks / HomeFade);
    }

    public static float bars(float SceneTicks) {
        float In = smooth(SceneTicks / ArriveFade);
        float Out = 1.0f - smooth((SceneTicks - ReturnAt) / 20.0f);
        return Math.min(In, Out);
    }
}
