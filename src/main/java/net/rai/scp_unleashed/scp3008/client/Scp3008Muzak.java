package net.rai.scp_unleashed.scp3008.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp3008Muzak {
    public static final int Eighth = 6;
    public static final int Bar = Eighth * 8;
    public static final float Volume = 0.14f;
    public static final float Detune = 0.012f;
    public static final int EchoDelay = 3;
    public static final float EchoShare = 0.3f;
    public static final double Distance = 9.0;
    public static final int Pling = 0;
    public static final int Harp = 1;
    public static final int Bass = 2;
    public static final int Bell = 3;
    public static final int Flute = 4;
    public static final int Chime = 5;
    public static final float[] Gains = {0.55f, 0.7f, 0.9f, 0.35f, 0.75f, 0.3f};
    public static final int[][] Chords = {{13, 17, 20}, {10, 13, 17}, {6, 10, 13}, {8, 12, 15}, {3, 6, 10}, {5, 8, 12}};
    public static final int[] Roots = {1, 10, 6, 8, 3, 5};
    public static final int[] Fifths = {8, 5, 13, 15, 10, 12};
    public static final int G = 0;
    public static final int Em = 1;
    public static final int C = 2;
    public static final int D = 3;
    public static final int Am = 4;
    public static final int Bm = 5;
    public static final int[] VerseChords = {G, Em, C, D, G, Em, Am, D};
    public static final int[] BridgeChords = {C, D, Bm, Em, Am, D, G, G};
    public static final int[][][] VerseTune = {
            {{17, 2}, {20, 1}, {18, 1}, {17, 2}, {13, 2}},
            {{22, 3}, {20, 1}, {17, 2}, {13, 2}},
            {{18, 2}, {22, 2}, {20, 1}, {18, 1}, {17, 2}},
            {{15, 3}, {17, 1}, {15, 2}, {-1, 2}},
            {{17, 2}, {20, 1}, {22, 1}, {20, 2}, {17, 2}},
            {{13, 3}, {15, 1}, {17, 2}, {22, 2}},
            {{18, 2}, {17, 1}, {15, 1}, {22, 2}, {18, 2}},
            {{20, 4}, {15, 2}, {-1, 2}}};
    public static final int[][][] BridgeTune = {
            {{22, 2}, {22, 1}, {20, 1}, {18, 2}, {22, 2}},
            {{24, 3}, {22, 1}, {20, 2}, {15, 2}},
            {{20, 2}, {17, 2}, {20, 1}, {24, 1}, {20, 2}},
            {{22, 4}, {17, 2}, {13, 2}},
            {{15, 2}, {18, 1}, {22, 1}, {20, 2}, {18, 2}},
            {{17, 3}, {18, 1}, {15, 2}, {20, 2}},
            {{13, 6}, {17, 2}},
            {{13, 4}, {-1, 4}}};
    private static final List<Note> Score = compose();
    private static final int Length = 32 * Bar;
    private static int Tick;
    private static int Cursor;
    private static float Drift;

    public record Note(int Tick, int Voice, int Pitch, float Gain) {
    }

    private Scp3008Muzak() {
    }

    public static List<Note> score() {
        return Score;
    }

    public static int length() {
        return Length;
    }

    private static List<Note> compose() {
        List<Note> Found = new ArrayList<>();
        for (int Section = 0; Section < 4; Section++) {
            boolean Verse = Section % 2 == 0;
            int[] Chart = Verse ? VerseChords : BridgeChords;
            int[][][] Tune = Verse ? VerseTune : BridgeTune;
            int Lead = Section == 2 ? Harp : Flute;
            for (int Index = 0; Index < 8; Index++) {
                int Start = (Section * 8 + Index) * Bar;
                int[] Chord = Chords[Chart[Index]];
                for (int Pitch : Chord) {
                    Found.add(new Note(Start, Pling, Pitch, 0.5f));
                    Found.add(new Note(Start + 24, Pling, Pitch, 0.42f));
                }
                Found.add(new Note(Start + 18, Pling, Chord[1], 0.32f));
                Found.add(new Note(Start + 18, Pling, Chord[2], 0.32f));
                Found.add(new Note(Start + 42, Pling, Chord[2], 0.28f));
                Found.add(new Note(Start, Bass, Roots[Chart[Index]], 0.8f));
                Found.add(new Note(Start + 24, Bass, Fifths[Chart[Index]], 0.6f));
                if (Section == 2 && Index % 2 == 0) {
                    Found.add(new Note(Start, Chime, Math.min(24, Chord[0] + 12), 0.6f));
                }
                if (Section == 3 && Index == 7) {
                    Found.add(new Note(Start, Bell, 13, 0.8f));
                    Found.add(new Note(Start + 12, Bell, 17, 0.6f));
                    Found.add(new Note(Start + 24, Bell, 20, 0.5f));
                }
                int Offset = 0;
                for (int[] Step : Tune[Index]) {
                    if (Step[0] >= 0) {
                        Found.add(new Note(Start + Offset * Eighth, Lead, Step[0], 0.9f));
                    }
                    Offset += Step[1];
                }
            }
        }
        Found.sort(Comparator.comparingInt(Note::Tick));
        return List.copyOf(Found);
    }

    public static DeferredHolder<SoundEvent, SoundEvent> voice(int Voice) {
        return switch (Voice) {
            case Harp -> Scp3008Registry.MuzakHarp;
            case Bass -> Scp3008Registry.MuzakBass;
            case Bell -> Scp3008Registry.MuzakBell;
            case Flute -> Scp3008Registry.MuzakFlute;
            case Chime -> Scp3008Registry.MuzakChime;
            default -> Scp3008Registry.MuzakPling;
        };
    }

    public static float pitchOf(int Pitch) {
        return (float) Math.pow(2.0, (Pitch - 12) / 12.0);
    }

    public static void stop() {
        Tick = 0;
        Cursor = 0;
    }

    private static void play(Minecraft Client, Note Next, RandomSource Random) {
        float Wobble = 1.0f + (Random.nextFloat() - 0.5f) * Detune + Mth.sin(Drift) * 0.004f;
        float Pitch = Mth.clamp(pitchOf(Next.Pitch()) * Wobble, 0.5f, 2.0f);
        float Gain = Volume * Gains[Next.Voice()] * Next.Gain();
        double Angle = Drift * 0.15;
        double X = Math.cos(Angle) * Distance;
        double Z = Math.sin(Angle) * Distance;
        SoundEvent Sound = voice(Next.Voice()).get();
        Client.getSoundManager().play(new SimpleSoundInstance(Sound.getLocation(), SoundSource.MUSIC, Gain, Pitch, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, X, 3.0, Z, true));
        if (Next.Voice() == Flute || Next.Voice() == Harp || Next.Voice() == Chime) {
            Client.getSoundManager().playDelayed(new SimpleSoundInstance(Sound.getLocation(), SoundSource.MUSIC, Gain * EchoShare, Pitch * 0.995f,
                    SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, -X, 5.0, -Z, true), EchoDelay);
        }
    }

    public static void tick(Minecraft Client) {
        if (!Scp3008ClientState.inside(Client) || Client.level == null) {
            stop();
            return;
        }
        if (Scp3008ClientState.brightness() < 0.99f || Scp3008ClientState.night()) {
            stop();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Drift += 0.02f;
        RandomSource Random = Client.level.getRandom();
        while (Cursor < Score.size() && Score.get(Cursor).Tick() <= Tick) {
            Note Next = Score.get(Cursor++);
            if (Next.Tick() == Tick) {
                play(Client, Next, Random);
            }
        }
        Tick++;
        if (Tick >= Length) {
            Tick = 0;
            Cursor = 0;
        }
    }
}
