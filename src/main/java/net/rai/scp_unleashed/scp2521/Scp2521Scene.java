package net.rai.scp_unleashed.scp2521;

import net.minecraft.util.Mth;

public final class Scp2521Scene {
    public static final int BindAt = 40;
    public static final int GrabAt = 90;
    public static final int GlideTicks = 16;
    public static final int CarryAt = 122;
    public static final int VoidAt = 142;
    public static final int StareTicks = 220;
    public static final int FallAt = VoidAt + StareTicks;
    public static final int FallTicks = 40;
    public static final int HomeAt = FallAt + FallTicks;
    public static final int HomeFade = 40;
    public static final int Cooldown = 600;
    public static final int BarsIn = 20;
    public static final int DarkFrom = CarryAt + 2;
    public static final int VoidFade = 24;

    private Scp2521Scene() {
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static float darkness(float Ticks) {
        if (Ticks < DarkFrom) {
            return 0.0f;
        }
        if (Ticks < VoidAt) {
            return smooth((Ticks - DarkFrom) / (VoidAt - DarkFrom - 2));
        }
        if (Ticks < VoidAt + VoidFade) {
            return 1.0f - smooth((Ticks - VoidAt) / VoidFade);
        }
        if (Ticks < FallAt + 14) {
            return 0.0f;
        }
        if (Ticks < HomeAt) {
            return smooth((Ticks - FallAt - 14) / (HomeAt - FallAt - 18));
        }
        return 1.0f - smooth((Ticks - HomeAt) / HomeFade);
    }

    public static float bars(float Ticks) {
        float In = smooth(Ticks / BarsIn);
        float Out = 1.0f - smooth((Ticks - HomeAt - 8) / (HomeFade - 8));
        return Math.min(In, Out);
    }
}
