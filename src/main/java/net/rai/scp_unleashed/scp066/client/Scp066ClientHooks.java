package net.rai.scp_unleashed.scp066.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.scp066.Scp066Tunes;
import net.rai.scp_unleashed.scp066.network.Scp066ChordPayload;

public final class Scp066ClientHooks {
    private static long LastChord = -1000L;

    private Scp066ClientHooks() {
    }

    public static boolean loud() {
        return ScpClientConfig.Scp066LoudMusic.get();
    }

    public static void chord(Scp066ChordPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        long Now = Client.level.getGameTime();
        if (Now >= LastChord && Now - LastChord < Scp066Chord.Spacing) {
            return;
        }
        LastChord = Now;
        float Gain = loud() ? Scp066Chord.LoudGain : Scp066Chord.NormalGain;
        RandomSource Random = SoundInstance.createUnseededRandom();
        for (Scp066Chord.Layer Entry : Scp066Chord.Layers) {
            float Pitch = Scp066Tunes.pitchOf(Entry.Semitone() - Payload.Voicing());
            SimpleSoundInstance Sound = new SimpleSoundInstance(Entry.Sound().get(), SoundSource.HOSTILE, Entry.Volume() * Gain, Pitch, Random,
                    Payload.X(), Payload.Y(), Payload.Z());
            if (Entry.Delay() > 0) {
                Client.getSoundManager().playDelayed(Sound, Entry.Delay());
            } else {
                Client.getSoundManager().play(Sound);
            }
        }
    }
}
