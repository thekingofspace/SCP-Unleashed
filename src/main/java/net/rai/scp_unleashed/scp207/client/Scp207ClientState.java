package net.rai.scp_unleashed.scp207.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp207.network.Scp207TollPayload;

public final class Scp207ClientState {
    public static final float BlurStart = 0.12f;
    public static final float BlurFull = 0.95f;
    public static final float GreyStart = 0.04f;
    public static final float GreyFull = 1.0f;
    public static final float GreyDepth = 0.94f;
    public static final float Ease = 0.08f;
    private static float Toll;
    private static float Rate;
    private static float Shown;
    private static float LastShown;
    private static int Clock;

    private Scp207ClientState() {
    }

    public static void apply(Scp207TollPayload Payload) {
        Toll = Mth.clamp(Payload.Toll(), 0.0f, 1.0f);
        Rate = Payload.Rate();
    }

    public static void tick(Minecraft Client) {
        if (Client.player == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        Toll = Mth.clamp(Toll + Rate, 0.0f, 1.0f);
        LastShown = Shown;
        Shown += (Toll - Shown) * Ease;
        if (Math.abs(Toll - Shown) < 1.0e-4f) {
            Shown = Toll;
        }
    }

    public static float toll(float Partial) {
        if (Minecraft.getInstance().player == null) {
            return 0.0f;
        }
        return Mth.lerp(Partial, LastShown, Shown);
    }

    public static int clock() {
        return Clock;
    }

    public static float blur(float Partial) {
        return smooth((toll(Partial) - BlurStart) / (BlurFull - BlurStart));
    }

    public static float grey(float Partial) {
        return GreyDepth * smooth((toll(Partial) - GreyStart) / (GreyFull - GreyStart));
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static void reset() {
        Toll = 0.0f;
        Rate = 0.0f;
        Shown = 0.0f;
        LastShown = 0.0f;
        Clock = 0;
    }
}
