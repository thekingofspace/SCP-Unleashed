package net.rai.scp_unleashed.scp420j.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp420j.network.Scp420jFloatPayload;

public final class Scp420jClientState {
    public static final float FadeIn = 1.0f / 30.0f;
    public static final float FadeOut = 1.0f / 70.0f;
    private static int Session;
    private static int Elapsed;
    private static double BaseY;
    private static boolean Active;
    private static boolean Descending;
    private static float Shown;
    private static float LastShown;
    private static int Clock;

    private Scp420jClientState() {
    }

    public static void apply(Scp420jFloatPayload Payload) {
        if (Payload.cleared()) {
            Session = 0;
            Active = false;
            Descending = false;
            return;
        }
        Session = Payload.Session();
        Elapsed = Payload.Elapsed();
        BaseY = Payload.BaseY();
        Descending = Payload.Descending();
        Active = true;
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
        if (floating()) {
            Elapsed++;
        }
        LastShown = Shown;
        float Target = floating() ? 1.0f : 0.0f;
        Shown = Target > Shown ? Math.min(Target, Shown + FadeIn) : Math.max(Target, Shown - FadeOut);
    }

    public static boolean floating() {
        return Active && !Descending;
    }

    public static boolean descending() {
        return Active && Descending;
    }

    public static boolean idle() {
        return !Active && Shown <= 0.0f && LastShown <= 0.0f;
    }

    public static int session() {
        return Session;
    }

    public static int elapsed() {
        return Elapsed;
    }

    public static double baseY() {
        return BaseY;
    }

    public static int clock() {
        return Clock;
    }

    public static float shown(float Partial) {
        float Value = Mth.lerp(Partial, LastShown, Shown);
        return Value * Value * (3.0f - 2.0f * Value);
    }

    public static void reset() {
        Session = 0;
        Elapsed = 0;
        BaseY = 0.0;
        Active = false;
        Descending = false;
        Shown = 0.0f;
        LastShown = 0.0f;
        Clock = 0;
    }
}
