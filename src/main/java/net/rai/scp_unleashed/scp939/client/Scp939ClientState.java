package net.rai.scp_unleashed.scp939.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp939.Scp939Breath;
import net.rai.scp_unleashed.scp939.network.Scp939BreathPayload;

public final class Scp939ClientState {
    public static final float Follow = 0.12f;
    public static final float LowFloor = 0.15f;
    private static float Exposure;
    private static float Breath;
    private static float Haze;
    private static boolean Masked;
    private static float Shown;
    private static float ShownOld;

    private Scp939ClientState() {
    }

    public static void apply(Scp939BreathPayload Payload) {
        Exposure = Payload.Exposure();
        Breath = Payload.Breath();
        Haze = Payload.Haze();
        Masked = Payload.Masked();
    }

    public static float exposure() {
        return Exposure / Scp939Breath.MaxExposure;
    }

    public static float haze() {
        return Haze;
    }

    public static boolean masked() {
        return Masked;
    }

    public static float target() {
        if (Masked || Breath <= 0.0f) {
            return 0.0f;
        }
        return Mth.clamp(Breath * (LowFloor + (1.0f - LowFloor) * exposure()), 0.0f, 1.0f);
    }

    public static void tick(Minecraft Client) {
        ShownOld = Shown;
        if (Client.player == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        float Wanted = target() * Client.options.screenEffectScale().get().floatValue();
        Shown += (Wanted - Shown) * Follow;
        if (Wanted <= 0.0f && Shown < 0.003f) {
            Shown = 0.0f;
        }
    }

    public static float glitch(float Partial) {
        return Mth.lerp(Partial, ShownOld, Shown);
    }

    public static void reset() {
        Exposure = 0.0f;
        Breath = 0.0f;
        Haze = 0.0f;
        Masked = false;
        Shown = 0.0f;
        ShownOld = 0.0f;
    }
}
