package net.rai.scp_unleashed.scp3008.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp3008.Scp3008Hours;
import net.rai.scp_unleashed.scp3008.network.Scp3008FadePayload;

public final class Scp3008ClientState {
    public static final int[] OffSteps = Scp3008Floodlights.Steps;
    public static final float[] OffLevels = {0.72f, 0.5f, 0.32f, 0.18f, 0.07f, 0.0f};
    public static final int[] OnSteps = Scp3008Floodlights.Steps;
    public static final float[] OnLevels = {0.06f, 0.16f, 0.3f, 0.48f, 0.72f, 1.0f};
    public static final int FadeHold = 80;
    private static float Lit = 1.0f;
    private static float LastLit = 1.0f;
    private static boolean Known;
    private static boolean Night;
    private static int SwitchAge = -1;
    private static boolean Switching;
    private static boolean ToNight;
    private static int Ticks;
    private static int FadePhase = -1;
    private static int FadeAge;
    private static int FadeTicks = 1;
    private static float FadeLevel;
    private static float LastFadeLevel;

    private Scp3008ClientState() {
    }

    public static boolean inside(Minecraft Client) {
        return Client.level != null && Scp3008Hours.inStore(Client.level);
    }

    public static float brightness(float Partial) {
        return Mth.lerp(Partial, LastLit, Lit);
    }

    public static float brightness() {
        return Lit;
    }

    public static boolean night() {
        return Night;
    }

    public static int ticks() {
        return Ticks;
    }

    public static float fadeLevel(float Partial) {
        return Mth.clamp(Mth.lerp(Partial, LastFadeLevel, FadeLevel), 0.0f, 1.0f);
    }

    public static void fade(int Phase, int Duration) {
        FadePhase = Phase;
        FadeAge = 0;
        FadeTicks = Math.max(1, Duration);
        if (Phase == Scp3008FadePayload.Cancel) {
            FadePhase = -1;
            FadeLevel = 0.0f;
            LastFadeLevel = 0.0f;
        }
    }

    private static void tickFade() {
        LastFadeLevel = FadeLevel;
        if (FadePhase < 0) {
            FadeLevel = Math.max(0.0f, FadeLevel - 0.1f);
            return;
        }
        FadeAge++;
        if (FadePhase == Scp3008FadePayload.FadeIn) {
            FadeLevel = Math.min(1.0f, FadeAge / (float) FadeTicks);
            if (FadeAge > FadeTicks + FadeHold) {
                fade(Scp3008FadePayload.FadeOut, 20);
            }
        } else {
            float Share = Math.min(1.0f, FadeAge / (float) FadeTicks);
            FadeLevel = 1.0f - Share * Share * (3.0f - 2.0f * Share);
            if (FadeAge >= FadeTicks) {
                FadePhase = -1;
                FadeLevel = 0.0f;
            }
        }
    }

    public static void tick(Minecraft Client) {
        tickFade();
        LastLit = Lit;
        if (!inside(Client)) {
            Known = false;
            Switching = false;
            Lit = 1.0f;
            LastLit = 1.0f;
            return;
        }
        Ticks++;
        ClientLevel World = Client.level;
        boolean Now = Scp3008Hours.night(World);
        if (!Known) {
            Known = true;
            Night = Now;
            Lit = Now ? 0.0f : 1.0f;
            LastLit = Lit;
            Switching = false;
            return;
        }
        if (Now != Night) {
            Night = Now;
            Switching = true;
            ToNight = Now;
            SwitchAge = 0;
            Scp3008Floodlights.begin(Client, Now);
        }
        if (Switching) {
            int[] Steps = ToNight ? OffSteps : OnSteps;
            float[] Levels = ToNight ? OffLevels : OnLevels;
            for (int Index = 0; Index < Steps.length; Index++) {
                if (SwitchAge == Steps[Index]) {
                    Lit = Levels[Index];
                    LastLit = Lit;
                    Scp3008Floodlights.bank(Client, ToNight, Index);
                }
            }
            SwitchAge++;
            if (SwitchAge > Steps[Steps.length - 1]) {
                Switching = false;
                Lit = ToNight ? 0.0f : 1.0f;
            }
        }
    }

    public static void reset() {
        Known = false;
        Switching = false;
        Lit = 1.0f;
        LastLit = 1.0f;
        FadePhase = -1;
        FadeLevel = 0.0f;
        LastFadeLevel = 0.0f;
    }
}
