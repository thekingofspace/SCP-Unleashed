package net.rai.scp_unleashed.scp224.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp224.network.Scp224AgePayload;

public final class Scp224ClientState {
    public static final float Ease = 0.08f;
    public static final float Snap = 0.0005f;

    private static float Target;
    private static boolean Forced;
    private static float Shown;
    private static float ShownOld;

    private Scp224ClientState() {
    }

    public static void apply(Scp224AgePayload Payload) {
        Target = Mth.clamp(Payload.Progress(), 0.0f, 1.0f);
        Forced = Payload.Forced();
    }

    public static boolean isForced() {
        return Forced;
    }

    public static float target() {
        return Target;
    }

    public static void tick() {
        ShownOld = Shown;
        float Gap = Target - Shown;
        Shown = Math.abs(Gap) < Snap ? Target : Shown + Gap * Ease;
    }

    public static float shown(float Partial) {
        return Mth.clamp(Mth.lerp(Partial, ShownOld, Shown), 0.0f, 1.0f);
    }

    public static boolean isActive() {
        return Shown > 0.0f || ShownOld > 0.0f || Target > 0.0f;
    }

    public static void reset() {
        Target = 0.0f;
        Forced = false;
        Shown = 0.0f;
        ShownOld = 0.0f;
    }
}
