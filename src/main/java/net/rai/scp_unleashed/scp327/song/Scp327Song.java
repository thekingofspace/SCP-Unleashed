package net.rai.scp_unleashed.scp327.song;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp327Song {
    public static final int Eighth = 12;
    public static final int Beat = Eighth * 2;
    public static final int Bar = Beat * 3;
    public static final int Bars = 16;
    public static final int Length = Bar * Bars;
    public static final int BedTicks = Bar * 8;
    public static final float LowestFactor = 0.94f;
    public static final float HighestFactor = 1.26f;
    public static final float TeenFactor = 1.19f;
    public static final float MinPitch = 0.5f;
    public static final float MaxPitch = 2.0f;
    public static final float BedPitch = 0.95139f;
    public static final float Lead = 0.75f;
    public static final float Swell = 0.55f;
    public static final float Grace = 0.5f;
    public static final float Echo = 0.16f;
    public static final float Pluck = 0.2f;
    public static final float Sparkle = 0.2f;
    public static final float Toll = 0.1f;
    public static final float Glimmer = 0.7f;
    public static final float Wash = 0.85f;
    public static final float SustainSeconds = 0.75f;
    public static final float EighthSeconds = Eighth / 20.0f;
    public static final int RollStep = 3;
    public static final int EchoLow = 67;
    public static final int EchoHigh = 86;

    public enum Voice {
        Hum(70.86f, true),
        Breath(77.7f, true),
        Flute(78.0f, false),
        Chime(90.0f, false),
        Bell(90.0f, false),
        Harp(66.0f, false),
        Shimmer(0.0f, false),
        Bed(0.0f, false);

        public final float Anchor;
        public final boolean Sung;

        Voice(float Anchor, boolean Sung) {
            this.Anchor = Anchor;
            this.Sung = Sung;
        }
    }

    public record Note(int Tick, Voice Kind, float Midi, float Volume, float Pitch) {
        public static Note tuned(int Tick, Voice Kind, float Midi, float Volume) {
            return new Note(Tick, Kind, Midi, Volume, (float) Math.pow(2.0, (Midi - Kind.Anchor) / 12.0));
        }

        public static Note raw(int Tick, Voice Kind, float Pitch, float Volume) {
            return new Note(Tick, Kind, 0.0f, Volume, Pitch);
        }
    }

    private static final int[][][] Melody = {
            {{69, 4}, {74, 2}},
            {{72, 2}, {69, 4}},
            {{67, 2}, {65, 2}, {67, 2}},
            {{69, 6}},
            {{69, 4}, {74, 2}},
            {{76, 2}, {77, 4}},
            {{76, 2}, {74, 2}, {73, 2}},
            {{74, 6}},
            {{77, 4}, {75, 2}},
            {{74, 2}, {72, 2}, {70, 2}},
            {{69, 4}, {70, 1}, {69, 1}},
            {{67, 6}},
            {{65, 4}, {67, 2}},
            {{69, 2}, {74, 4}},
            {{73, 2}, {70, 2}, {64, 2}},
            {{62, 6}},
    };

    private static final int[][] Chords = {
            {62, 65, 69}, {65, 69, 72}, {60, 64, 67}, {62, 65, 69},
            {62, 65, 69}, {58, 62, 65}, {57, 61, 64}, {62, 65, 69},
            {63, 67, 70}, {58, 62, 65}, {62, 65, 69}, {62, 67, 70},
            {62, 65, 69}, {62, 65, 69}, {57, 61, 64}, {62, 65, 69},
    };

    private static final int[][] Sparkles = {
            {3, 86, 81}, {7, 89, 86}, {8, 87, 82}, {11, 86, 82}, {12, 81, 86}, {15, 86, 81},
    };

    private static final boolean[] Echoes = {
            false, false, false, false, true, true, true, true, false, true, true, true, false, true, true, false,
    };

    private Scp327Song() {
    }

    public static List<Note> notes() {
        List<Note> Score = new ArrayList<>();
        for (int Index = 0; Index < Bars; Index++) {
            int Start = Index * Bar;
            int Offset = 0;
            for (int[] Step : Melody[Index]) {
                int Midi = Step[0];
                int Eighths = Step[1];
                int Tick = Start + Offset * Eighth;
                if (Eighths == 1) {
                    Score.add(Note.tuned(Tick, Voice.Breath, Midi, Grace));
                } else {
                    Score.add(Note.tuned(Tick, Voice.Hum, Midi, Lead));
                }
                if (Eighths > 1) {
                    Note Sung = Note.tuned(Tick, Voice.Hum, Midi, Lead);
                    int Pace = Math.max(1, (int) Math.floor(SustainSeconds / Sung.Pitch() / EighthSeconds));
                    for (int Held = Pace; Held < Eighths; Held += Pace) {
                        Score.add(Note.tuned(Tick + Held * Eighth, Voice.Hum, Midi, Lead * Swell));
                    }
                }
                if (Echoes[Index] && Eighths >= 2) {
                    Score.add(Note.tuned(Tick + Beat, Voice.Flute, echoOctave(Midi), Echo));
                }
                Offset += Eighths;
            }
            int[] Chord = Chords[Index];
            for (int Tone = 0; Tone < Chord.length; Tone++) {
                Score.add(Note.tuned(Start + Tone * RollStep, Voice.Harp, Chord[Tone], Pluck * (Tone == 0 ? 1.0f : 0.8f)));
            }
            if (Index % 2 == 1) {
                Score.add(Note.tuned(Start + Beat * 2, Voice.Harp, Chord[2], Pluck * 0.6f));
            }
            if (Index % 4 == 0) {
                Score.add(Note.tuned(Start, Voice.Bell, Chord[0] + 24, Toll));
                Score.add(Note.raw(Start + Eighth, Voice.Shimmer, 0.8f, Glimmer));
            }
            if (Index % 8 == 0) {
                Score.add(Note.raw(Start, Voice.Bed, BedPitch, Wash));
            }
        }
        for (int[] Spark : Sparkles) {
            int Tick = Spark[0] * Bar + Beat * 2;
            Score.add(Note.tuned(Tick, Voice.Chime, Spark[1], Sparkle));
            Score.add(Note.tuned(Tick + Eighth, Voice.Chime, Spark[2], Sparkle * 0.6f));
        }
        Score.sort(Comparator.comparingInt(Note::Tick));
        return Score;
    }

    public static List<Note> hum() {
        List<Note> Score = new ArrayList<>();
        for (Note Played : notes()) {
            if (Played.Kind().Sung || Played.Kind() == Voice.Shimmer) {
                Score.add(Played);
            }
        }
        return Score;
    }

    private static int echoOctave(int Midi) {
        int Shifted = Midi;
        while (Shifted < EchoLow) {
            Shifted += 12;
        }
        while (Shifted + 12 <= EchoHigh && Shifted < 74) {
            Shifted += 12;
        }
        return Shifted;
    }

    public static List<String> problems() {
        List<String> Found = new ArrayList<>();
        for (Note Played : notes()) {
            float Low = Played.Kind() == Voice.Bed ? Played.Pitch() : Played.Pitch() * LowestFactor;
            float High = Played.Kind() == Voice.Bed ? Played.Pitch() : Played.Pitch() * HighestFactor;
            if (Low < MinPitch || High > MaxPitch) {
                Found.add(Played.Kind().name() + " note " + Played.Midi() + " at tick " + Played.Tick() + " plays at " + Low + ".." + High);
            }
            if (Played.Tick() < 0 || Played.Tick() >= Length) {
                Found.add("note at tick " + Played.Tick() + " is outside the loop");
            }
        }
        for (int Index = 0; Index < Bars; Index++) {
            int Eighths = 0;
            for (int[] Step : Melody[Index]) {
                Eighths += Step[1];
            }
            if (Eighths != 6) {
                Found.add("bar " + (Index + 1) + " holds " + Eighths + " eighths, not 6");
            }
        }
        if (Chords.length != Bars || Echoes.length != Bars) {
            Found.add("chord or echo table does not match the bar count");
        }
        return Found;
    }
}
