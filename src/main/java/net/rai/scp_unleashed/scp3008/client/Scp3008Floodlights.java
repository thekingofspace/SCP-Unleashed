package net.rai.scp_unleashed.scp3008.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;

import java.util.List;

public final class Scp3008Floodlights {
    public static final int Banks = 6;
    public static final int[] Steps = {0, 5, 10, 16, 23, 31};
    public static final float NearDistance = 5.0f;
    public static final float BankSpacing = 13.0f;
    public static final float BankFalloff = 0.7f;
    public static final float BankDarken = 0.035f;
    public static final float EchoBase = 0.28f;
    public static final float EchoGrowth = 0.07f;
    public static final int[] EchoDelays = {3, 7};
    public static final float[] EchoShares = {1.0f, 0.45f};
    public static final float[] EchoPitch = {0.985f, 0.97f};
    public static final List<Layer> Hit = List.of(
            new Layer(Scp3008Registry.FloodClunk, 1.0f, 0.52f, 0),
            new Layer(Scp3008Registry.FloodRelay, 0.75f, 0.62f, 0),
            new Layer(Scp3008Registry.FloodThud, 0.55f, 0.5f, 0),
            new Layer(Scp3008Registry.FloodRing, 0.22f, 0.75f, 1));
    public static final int[] Flickers = {3, 5, 9};
    private static float SweepStart;

    public record Layer(DeferredHolder<SoundEvent, SoundEvent> Sound, float Gain, float Pitch, int Delay) {
    }

    private Scp3008Floodlights() {
    }

    private static void play(Minecraft Client, DeferredHolder<SoundEvent, SoundEvent> Sound, float Volume, float Pitch, double X, double Y, double Z, int Delay) {
        SimpleSoundInstance Made = new SimpleSoundInstance(Sound.get().getLocation(), SoundSource.AMBIENT, Volume, Mth.clamp(Pitch, 0.5f, 2.0f),
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, X, Y, Z, true);
        if (Delay <= 0) {
            Client.getSoundManager().play(Made);
        } else {
            Client.getSoundManager().playDelayed(Made, Delay);
        }
    }

    public static int bankFor(boolean ToNight, int Index) {
        int Step = Mth.clamp(Index, 0, Banks - 1);
        return ToNight ? Step : Banks - 1 - Step;
    }

    public static void begin(Minecraft Client, boolean ToNight) {
        SweepStart = Client.level == null ? 0.0f : Client.level.getRandom().nextFloat() * Mth.TWO_PI;
        if (ToNight) {
            play(Client, Scp3008Registry.PowerDown, 0.55f, 0.55f, 0.0, 3.0, 0.0, 2);
            play(Client, Scp3008Registry.FloodHum, 0.16f, 0.7f, 0.0, 4.0, 0.0, 0);
        } else {
            play(Client, Scp3008Registry.PowerUp, 0.5f, 0.6f, 0.0, 3.0, 0.0, 0);
            play(Client, Scp3008Registry.FloodHum, 0.18f, 0.75f, 0.0, 4.0, 0.0, Steps[Banks - 1] + 6);
        }
    }

    public static void bank(Minecraft Client, boolean ToNight, int Index) {
        int Bank = bankFor(ToNight, Index);
        float Angle = SweepStart + Bank * 0.9f;
        float Distance = NearDistance + Bank * BankSpacing;
        double X = Math.cos(Angle) * Distance;
        double Z = Math.sin(Angle) * Distance;
        double Y = 6.0 + Bank * 3.0;
        float Volume = (float) Math.pow(BankFalloff, Bank);
        float Darken = 1.0f - Bank * BankDarken;
        float Echo = EchoBase + Bank * EchoGrowth;
        for (Layer Next : Hit) {
            float Pitch = Next.Pitch() * Darken;
            play(Client, Next.Sound(), Volume * Next.Gain(), Pitch, X, Y, Z, Next.Delay());
            for (int Repeat = 0; Repeat < EchoDelays.length; Repeat++) {
                double Side = Repeat == 0 ? -1.4 : 1.8;
                play(Client, Next.Sound(), Volume * Next.Gain() * Echo * EchoShares[Repeat], Pitch * EchoPitch[Repeat], X * Side, Y + 8.0, -Z * Side,
                        Next.Delay() + EchoDelays[Repeat] + Bank);
            }
        }
        if (!ToNight && Bank <= 1) {
            for (int Flicker = 0; Flicker < Flickers.length; Flicker++) {
                float Pitch = 1.45f + Flicker * 0.17f;
                play(Client, Scp3008Registry.FloodTick, 0.32f - Flicker * 0.07f, Pitch, X * 0.5, Y + 2.0, Z * 0.5, Flickers[Flicker] + Bank * 2);
            }
        }
    }
}
