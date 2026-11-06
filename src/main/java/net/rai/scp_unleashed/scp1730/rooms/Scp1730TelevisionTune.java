package net.rai.scp_unleashed.scp1730.rooms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Scp1730TelevisionTune {
    public static final int Pling = 0;
    public static final int Bit = 1;
    public static final int Bell = 2;
    public static final int Xylophone = 3;
    public static final int IronXylophone = 4;
    public static final int Harp = 5;
    public static final int Banjo = 6;
    public static final int Eighth = 4;
    public static final int[][] Melody = {
            {10, 1, 11, 1, 12, 1, 13, 1}, {22, 2, 18, 1, 13, 1}, {15, 1, 14, 1, 15, 1, 18, 1}, {13, 2, -1, 2},
            {10, 1, 11, 1, 12, 1, 13, 1}, {20, 2, 17, 1, 13, 1}, {15, 1, 17, 1, 18, 1, 20, 1}, {18, 2, 13, 1, 6, 1},
            {18, 1, 13, 1, 18, 1, 22, 1}, {23, 2, 22, 1, 20, 1}, {17, 1, 13, 1, 17, 1, 20, 1}, {22, 2, 20, 1, 18, 1},
            {15, 1, 18, 1, 22, 1, 18, 1}, {20, 2, 15, 1, 17, 1}, {18, 1, 20, 1, 17, 1, 13, 1}, {18, 2, -1, 2}};
    public static final int[][] Chords = {{6, 10, 13}, {11, 11, 15}, {1, 5, 8}, {3, 11, 15}};
    public static final int[] Harmony = {0, 0, 3, 2, 0, 2, 3, 0, 0, 1, 2, 0, 3, 2, 2, 0};
    private static volatile List<Note> Score;

    public record Note(int Tick, int Instrument, float Semitone, float Volume) {
    }

    private Scp1730TelevisionTune() {
    }

    public static List<Note> score() {
        List<Note> Found = Score;
        if (Found == null) {
            Found = compose();
            Score = Found;
        }
        return Found;
    }

    public static float pitch(float Semitone) {
        return (float) Math.pow(2.0, (Semitone - 12.0) / 12.0);
    }

    static List<Note> compose() {
        int Music = Scp1730TelevisionBlockEntity.ShowTicks - Scp1730TelevisionBlockEntity.WindTicks;
        int End = Scp1730TelevisionBlockEntity.ShowTicks;
        Random Wobble = new Random(1730L);
        List<Note> Made = new ArrayList<>();
        double Time = 0.0;
        int Bar = 0;
        boolean Winding = false;
        double Step = Eighth;
        while (Time < End - 14) {
            int[] Line = Melody[Bar % Melody.length];
            int[] Chord = Chords[Harmony[Bar % Harmony.length]];
            int Slot = 0;
            for (int Index = 0; Index + 1 < Line.length; Index += 2) {
                int Pitch = Line[Index];
                int Length = Line[Index + 1];
                for (int Sub = 0; Sub < Length; Sub++) {
                    if (Time >= Music) {
                        Winding = true;
                    }
                    double Share = Winding ? Math.min(1.0, (Time - Music) / (double) (End - Music)) : 0.0;
                    float Sour = (float) (-2.2 * Share);
                    float Loud = (float) (1.0 - 0.6 * Share);
                    float Warble = (float) (Math.sin(Time * 0.37) * 0.12 + Wobble.nextFloat() * 0.06 - 0.03);
                    int Tick = (int) Math.round(Time);
                    if (Sub == 0 && Pitch >= 0) {
                        Made.add(new Note(Tick, Pling, Pitch + Warble + Sour, 0.75f * Loud));
                        Made.add(new Note(Tick, Bit, Pitch + Warble + Sour + 0.12f, 0.32f * Loud));
                        if (Bar % 16 >= 8 && Slot == 0) {
                            Made.add(new Note(Tick, Bell, Pitch + Sour, 0.35f * Loud));
                        }
                        if (Bar % 4 == 3 && Slot == 0) {
                            Made.add(new Note(Tick, Xylophone, Pitch + Sour, 0.4f * Loud));
                        }
                    }
                    if (Slot == 0) {
                        Made.add(new Note(Tick, Banjo, Chord[0] + Sour + Warble, 0.7f * Loud));
                    } else if (Slot == 2) {
                        Made.add(new Note(Tick, IronXylophone, Chord[1] + Sour, 0.42f * Loud));
                        Made.add(new Note(Tick, IronXylophone, Chord[2] + Sour + 0.08f, 0.42f * Loud));
                    } else if (Slot == 1 || Slot == 3) {
                        Made.add(new Note(Tick, Harp, Chord[1] + 12 + Sour, 0.18f * Loud));
                    }
                    if (Winding) {
                        Step += 0.45;
                    }
                    Time += Step;
                    Slot++;
                }
            }
            Bar++;
        }
        int Last = End - 10;
        Made.add(new Note(Last, Banjo, 1.0f, 0.6f));
        Made.add(new Note(Last, Banjo, 7.0f, 0.55f));
        Made.add(new Note(Last, Bit, 0.0f, 0.35f));
        Made.add(new Note(Last + 2, IronXylophone, 6.5f, 0.3f));
        Made.sort((First, Second) -> Integer.compare(First.Tick(), Second.Tick()));
        return Collections.unmodifiableList(Made);
    }
}
