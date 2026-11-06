package net.rai.scp_unleashed.scp024.tune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Scp024Tune {
    public static final int Eighth = 4;
    public static final int Beat = Eighth * 2;
    public static final int Bar = Beat * 3;
    public static final int Bars = 16;
    public static final int Length = Bar * Bars;
    public static final int Anchor = 66;
    public static final int Lowest = 54;
    public static final int Highest = 78;
    public static final float Melody = 0.55f;
    public static final float Shimmer = 0.24f;
    public static final float Echo = 0.28f;
    public static final float Sparkle = 0.3f;
    public static final float Oom = 0.62f;
    public static final float Thump = 0.34f;
    public static final float Pah = 0.2f;

    public enum Voice {
        Flute,
        Bell,
        Bit,
        Chime,
        Xylophone,
        Bass,
        Drum
    }

    public record Note(int Tick, Voice Kind, int Midi, float Volume) {
        public float pitch() {
            if (Kind == Voice.Drum) {
                return 1.0f;
            }
            return (float) Math.pow(2.0, (Midi - Anchor) / 12.0);
        }
    }

    private static final int[][][] Tune = {
            {{69, 2}, {74, 2}, {77, 2}},
            {{76, 6}},
            {{75, 2}, {76, 2}, {77, 2}},
            {{69, 6}},
            {{67, 2}, {70, 2}, {74, 2}},
            {{73, 6}},
            {{74, 1}, {73, 1}, {72, 1}, {71, 1}, {70, 2}},
            {{69, 6}},
            {{77, 2}, {76, 2}, {74, 2}},
            {{73, 2}, {74, 2}, {76, 2}},
            {{77, 2}, {68, 2}, {69, 2}},
            {{74, 6}},
            {{70, 2}, {69, 2}, {67, 2}},
            {{65, 2}, {64, 2}, {62, 2}},
            {{61, 2}, {64, 2}, {69, 2}},
            {{62, 6}},
    };

    private static final int[][] Chords = {
            {62, 65, 69}, {57, 61, 67}, {62, 65, 69}, {62, 65, 69},
            {55, 67, 70}, {57, 61, 67}, {55, 67, 70}, {57, 61, 67},
            {62, 65, 69}, {57, 61, 67}, {62, 65, 69}, {62, 65, 69},
            {55, 67, 70}, {62, 65, 69}, {57, 61, 67}, {62, 65, 69},
    };

    private Scp024Tune() {
    }

    public static List<Note> notes() {
        List<Note> Score = new ArrayList<>();
        for (int Index = 0; Index < Bars; Index++) {
            int Start = Index * Bar;
            boolean Bridge = Index >= Bars / 2;
            int Offset = 0;
            for (int[] Step : Tune[Index]) {
                int Midi = Step[0];
                int Tick = Start + Offset * Eighth;
                Score.add(new Note(Tick, Voice.Flute, Midi, Melody));
                Score.add(new Note(Tick, Voice.Bell, Midi, Shimmer));
                if (Bridge) {
                    Score.add(new Note(Tick, Voice.Xylophone, Midi, Echo));
                }
                if (Step[1] == 6) {
                    Score.add(new Note(Tick + Beat, Voice.Flute, Midi, Melody * 0.45f));
                    Score.add(new Note(Tick + Beat * 2, Voice.Flute, Midi, Melody * 0.35f));
                }
                Offset += Step[1];
            }
            int[] Chord = Chords[Index];
            Score.add(new Note(Start, Voice.Bass, Chord[0], Oom));
            Score.add(new Note(Start, Voice.Drum, Anchor, Thump));
            for (int Pulse = 1; Pulse <= 2; Pulse++) {
                Score.add(new Note(Start + Beat * Pulse, Voice.Bit, Chord[1], Pah));
                Score.add(new Note(Start + Beat * Pulse, Voice.Bit, Chord[2], Pah));
            }
            if (Index % 4 == 3) {
                Score.add(new Note(Start, Voice.Chime, Math.min(Highest, Chord[2] + 5), Sparkle));
            }
        }
        Collections.sort(Score, (First, Second) -> Integer.compare(First.Tick(), Second.Tick()));
        return Score;
    }

    public static List<String> problems() {
        List<String> Found = new ArrayList<>();
        for (Note Played : notes()) {
            if (Played.Kind() != Voice.Drum && (Played.Midi() < Lowest || Played.Midi() > Highest)) {
                Found.add("note " + Played.Midi() + " at tick " + Played.Tick() + " is outside the note block range");
            }
            if (Played.Tick() < 0 || Played.Tick() >= Length) {
                Found.add("note at tick " + Played.Tick() + " is outside the loop");
            }
        }
        for (int Index = 0; Index < Bars; Index++) {
            int Eighths = 0;
            for (int[] Step : Tune[Index]) {
                Eighths += Step[1];
            }
            if (Eighths != 6) {
                Found.add("bar " + (Index + 1) + " holds " + Eighths + " eighths, not 6");
            }
        }
        return Found;
    }
}
