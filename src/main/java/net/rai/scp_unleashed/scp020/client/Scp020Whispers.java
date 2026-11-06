package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp020.Scp020Registry;

public final class Scp020Whispers {
    public static final int SlowWhisper = 640;
    public static final int FastWhisper = 100;
    public static final int FirstWhisper = 60;
    public static final int VoicesFrom = 35;
    public static final int BreathFrom = 70;
    public static final float QuietVolume = 0.35f;
    public static final float LoudVolume = 0.85f;
    public static final float LowPitch = 0.78f;
    public static final float HighPitch = 0.96f;
    public static final float NearDistance = 1.2f;
    public static final float FarDistance = 2.6f;
    private static int Countdown;
    private static boolean Active;

    private Scp020Whispers() {
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        int Intensity = Scp020ClientState.intensity();
        if (Intensity <= 0) {
            Active = false;
            return;
        }
        RandomSource Random = User.getRandom();
        if (!Active) {
            Active = true;
            Countdown = FirstWhisper + Random.nextInt(FirstWhisper);
        }
        if (--Countdown > 0) {
            return;
        }
        float Progress = Scp020ClientState.strength();
        int Interval = Math.round(Mth.lerp(Progress, SlowWhisper, FastWhisper));
        Countdown = Interval + Random.nextInt(Interval / 2 + 1);
        whisper(Client, Random, Intensity, Progress);
    }

    private static void whisper(Minecraft Client, RandomSource Random, int Intensity, float Progress) {
        float Angle = Random.nextFloat() * Mth.TWO_PI;
        float Distance = Mth.lerp(Random.nextFloat(), NearDistance, FarDistance);
        double X = Mth.cos(Angle) * Distance;
        double Z = Mth.sin(Angle) * Distance;
        double Y = (Random.nextFloat() - 0.3f) * 0.6f;
        float Pitch = Mth.lerp(Random.nextFloat(), LowPitch, HighPitch);
        float Volume = Mth.lerp(Progress, QuietVolume, LoudVolume);
        play(Client, Random, Scp020Registry.Whisper, Volume, Pitch, X, Y, Z);
        if (Intensity >= VoicesFrom) {
            play(Client, Random, Scp020Registry.WhisperVoices, Volume * 0.7f, Pitch * 0.92f, X, Y, Z);
        }
        if (Intensity >= BreathFrom) {
            play(Client, Random, Scp020Registry.WhisperBreath, Volume * 0.6f, Pitch, -X, Y, -Z);
        }
    }

    private static void play(Minecraft Client, RandomSource Random, DeferredHolder<SoundEvent, SoundEvent> Layer, float Volume, float Pitch, double X, double Y, double Z) {
        Client.getSoundManager().play(new SimpleSoundInstance(Layer.get().getLocation(), SoundSource.MASTER, Volume, Pitch, Random, false, 0,
                SoundInstance.Attenuation.LINEAR, X, Y, Z, true));
    }

    public static void reset() {
        Countdown = 0;
        Active = false;
    }
}
