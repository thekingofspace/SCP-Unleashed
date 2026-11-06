package net.rai.scp_unleashed.scp280.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp280.Scp280Shade;

public final class Scp280ShadeEyes {
    public static final int PeekLength = 26;
    public static final float SquintOpen = 0.35f;
    public static final float RecoilOpen = 0.12f;
    public static final int DeathTicks = 20;

    private Scp280ShadeEyes() {
    }

    public static float order(int Index) {
        return (Scp280ShadeGeoModel.hash(Index * 29 + 11) % 1000) / 1000.0f;
    }

    public static float baseOpenness(Scp280Shade Shade, float Partial) {
        if (Shade.isDeadOrDying()) {
            return 1.0f;
        }
        if (Shade.isContained()) {
            return 1.0f;
        }
        byte Action = Shade.action();
        if (Action == Scp280Shade.ActionRecoil) {
            return RecoilOpen;
        }
        if (Shade.isPinned() || Shade.isHuddled()) {
            return SquintOpen;
        }
        return 1.0f;
    }

    public static float eyeFactor(Scp280Shade Shade, int Index, float Time) {
        if (Shade.isDeadOrDying()) {
            float Gone = (Shade.deathTime + (Time - Shade.tickCount)) / DeathTicks;
            return Mth.clamp((order(Index) - Gone * 1.3f) * 4.0f + 1.0f, 0.0f, 1.0f);
        }
        if (Shade.isContained()) {
            int Period = 180 + Scp280ShadeGeoModel.hash(Index * 7 + 3) % 260;
            float Phase = (Time + Scp280ShadeGeoModel.hash(Index * 9 + 4) % Period) % Period;
            if (Phase < PeekLength) {
                return Mth.clamp(Math.min(Phase, PeekLength - Phase) / 5.0f, 0.0f, 0.7f);
            }
            return 0.0f;
        }
        if (Shade.action() == Scp280Shade.ActionEmerge) {
            float Progress = Shade.actionAge(Time - Shade.tickCount) / (float) Scp280Shade.EmergeTicks;
            return Mth.clamp((Progress * 1.5f - order(Index)) * 5.0f, 0.0f, 1.0f);
        }
        return 1.0f;
    }

    public static float glow(Scp280Shade Shade, float Partial) {
        if (Shade.isContained()) {
            return 0.75f;
        }
        if (Shade.isDeadOrDying()) {
            return Mth.clamp(1.0f - (Shade.deathTime + Partial) / DeathTicks, 0.0f, 1.0f);
        }
        return 1.0f;
    }
}
