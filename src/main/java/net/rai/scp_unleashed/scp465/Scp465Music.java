package net.rai.scp_unleashed.scp465;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public final class Scp465Music {
    public static final double Bpm = 125.0;
    public static final double BeatTicks = 1200.0 / Bpm;
    public static final int StepsPerBeat = 4;
    public static final double StepTicks = BeatTicks / StepsPerBeat;
    public static final int StepsPerBar = 16;
    public static final double BarTicks = StepTicks * StepsPerBar;
    public static final int Bars = 32;
    public static final int Steps = StepsPerBar * Bars;
    public static final int IntroBars = 4;
    public static final int PhraseBars = 8;
    public static final int HookBar = 8;
    public static final int BreakBar = 16;
    public static final int BuildBar = 20;
    public static final int DropBar = 24;
    public static final int GapStep = 12;
    public static final int[] Roots = {3, 11, 6, 1};
    public static final int[][] ArpTones = {{3, 6, 10, 15}, {3, 6, 11, 15}, {1, 6, 10, 13}, {1, 5, 8, 13}};
    public static final int[][] StabTones = {{6, 10, 15}, {6, 11, 15}, {6, 10, 13}, {5, 8, 13}};
    public static final int[] ArpOrder = {0, 1, 2, 3, 2, 1, 2, 3, 0, 1, 2, 3, 2, 3, 2, 1};
    public static final int[][] Hook = {
            {22, -1, -1, 18, -1, -1, 15, -1, -1, -1, 18, -1, 22, -1, -1, -1},
            {23, -1, -1, 18, -1, -1, 15, -1, -1, -1, 18, -1, 23, -1, -1, -1},
            {22, -1, -1, 18, -1, -1, 13, -1, -1, -1, 18, -1, 22, -1, -1, -1},
            {20, -1, -1, 17, -1, -1, 13, -1, -1, -1, 17, -1, 20, -1, 22, -1}};
    public static final int[] BassShift = {-1, -1, 0, 0, -1, -1, 12, 0, -1, -1, 0, 0, -1, -1, 7, 12};
    public static final float[] BassLevel = {0.0f, 0.0f, 0.8f, 0.4f, 0.0f, 0.0f, 0.7f, 0.4f, 0.0f, 0.0f, 0.8f, 0.4f, 0.0f, 0.0f, 0.7f, 0.45f};

    public enum Section {
        Intro,
        Groove,
        Hook,
        Break,
        Build,
        Drop
    }

    public enum Instrument {
        Kick,
        KickSub,
        Clap,
        ClapBody,
        Hat,
        HatOpen,
        Bass,
        BassAttack,
        Arp,
        Lead,
        LeadShine,
        Stab,
        Crash;

        public DeferredHolder<SoundEvent, SoundEvent> sound() {
            return switch (this) {
                case Kick -> Scp465Registry.MusicKick;
                case KickSub -> Scp465Registry.MusicKickSub;
                case Clap -> Scp465Registry.MusicClap;
                case ClapBody -> Scp465Registry.MusicClapBody;
                case Hat -> Scp465Registry.MusicHat;
                case HatOpen -> Scp465Registry.MusicHatOpen;
                case Bass -> Scp465Registry.MusicBass;
                case BassAttack -> Scp465Registry.MusicBassAttack;
                case Arp -> Scp465Registry.MusicArp;
                case Lead -> Scp465Registry.MusicLead;
                case LeadShine -> Scp465Registry.MusicLeadShine;
                case Stab -> Scp465Registry.MusicStab;
                case Crash -> Scp465Registry.MusicCrash;
            };
        }
    }

    public record Note(Instrument Voice, float Pitch, float Volume) {
    }

    private Scp465Music() {
    }

    public static float pitch(int Semitone) {
        return (float) Math.pow(2.0, (Semitone - 12) / 12.0);
    }

    public static int stepAt(double TicksSinceStart) {
        return (int) Math.floor(TicksSinceStart / StepTicks);
    }

    public static int barOf(int Step) {
        return Math.floorMod(Step, Steps) / StepsPerBar;
    }

    public static Section section(int Step) {
        if (Step >= 0 && Step < IntroBars * StepsPerBar) {
            return Section.Intro;
        }
        int Bar = barOf(Step);
        if (Bar < HookBar) {
            return Section.Groove;
        }
        if (Bar < BreakBar) {
            return Section.Hook;
        }
        if (Bar < BuildBar) {
            return Section.Break;
        }
        if (Bar < DropBar) {
            return Section.Build;
        }
        return Section.Drop;
    }

    public static boolean kicking(int Step) {
        int Position = Math.floorMod(Step, StepsPerBar);
        if (Position % StepsPerBeat != 0) {
            return false;
        }
        return switch (section(Step)) {
            case Break -> false;
            case Build -> barOf(Step) < DropBar - 1 || Position < GapStep;
            default -> true;
        };
    }

    public static List<Note> notes(int Step) {
        List<Note> Found = new ArrayList<>();
        notes(Step, Found);
        return Found;
    }

    public static void notes(int Step, List<Note> Out) {
        if (Step < 0) {
            return;
        }
        int Bar = barOf(Step);
        int Position = Math.floorMod(Step, StepsPerBar);
        int Chord = Bar % Roots.length;
        Section Stage = section(Step);
        boolean PhraseEnd = (Bar + 1) % PhraseBars == 0;
        if (kicking(Step)) {
            Out.add(new Note(Instrument.Kick, 0.86f, 1.0f));
            if (Stage != Section.Build || Bar >= DropBar - 2) {
                Out.add(new Note(Instrument.KickSub, pitch(Roots[Chord]), Stage == Section.Drop ? 0.55f : 0.45f));
            }
        }
        if (Position == 0 && Step > 0 && (Bar == 0 || Bar == HookBar || Bar == BreakBar || Bar == DropBar)) {
            Out.add(new Note(Instrument.Crash, 0.62f, Bar == DropBar ? 0.9f : 0.7f));
        }
        switch (Stage) {
            case Intro -> intro(Bar, Position, Chord, Out);
            case Groove, Hook, Drop -> groove(Stage, Bar, Position, Chord, PhraseEnd, Out);
            case Break -> breakdown(Bar, Position, Chord, Out);
            case Build -> build(Bar, Position, Chord, Out);
        }
    }

    private static void drums(int Position, boolean Fill, float Lift, List<Note> Out) {
        if (Position == 4 || Position == 12) {
            Out.add(new Note(Instrument.Clap, 1.0f, 0.72f));
            Out.add(new Note(Instrument.ClapBody, 0.72f, 0.35f));
        } else if (Fill && Position >= 13) {
            Out.add(new Note(Instrument.Clap, 0.9f + (Position - 12) * 0.08f, 0.32f + (Position - 12) * 0.08f));
        }
        if (Position % StepsPerBeat == 2) {
            Out.add(new Note(Instrument.HatOpen, 1.25f, 0.38f * Lift));
        } else {
            Out.add(new Note(Instrument.Hat, Position % StepsPerBeat == 0 ? 1.7f : 1.85f, (Position % StepsPerBeat == 0 ? 0.2f : 0.13f) * Lift));
        }
    }

    private static void bass(int Position, int Chord, float Lift, List<Note> Out) {
        int Shift = BassShift[Position];
        if (Shift < 0) {
            return;
        }
        float Played = pitch(Roots[Chord] + Shift);
        Out.add(new Note(Instrument.Bass, Played, BassLevel[Position] * Lift));
        if (BassLevel[Position] >= 0.7f) {
            Out.add(new Note(Instrument.BassAttack, Played, 0.3f * Lift));
        }
    }

    private static void arp(int Position, int Chord, float Volume, List<Note> Out) {
        Out.add(new Note(Instrument.Arp, pitch(ArpTones[Chord][ArpOrder[Position]]), Volume));
    }

    private static void hook(Instrument Voice, int Position, int Chord, float Volume, float Shine, List<Note> Out) {
        int Tone = Hook[Chord][Position];
        if (Tone < 0) {
            return;
        }
        Out.add(new Note(Voice, pitch(Tone), Volume));
        Out.add(new Note(Instrument.LeadShine, pitch(Tone), Shine));
    }

    private static void stabs(int Chord, float Volume, List<Note> Out) {
        for (int Tone : StabTones[Chord]) {
            Out.add(new Note(Instrument.Stab, pitch(Tone), Volume));
        }
    }

    private static void intro(int Bar, int Position, int Chord, List<Note> Out) {
        if (Position % 2 == 0 || Bar >= 1) {
            if (Position % StepsPerBeat == 2) {
                Out.add(new Note(Instrument.HatOpen, 1.25f, 0.3f));
            } else if (Bar >= 1) {
                Out.add(new Note(Instrument.Hat, 1.8f, 0.12f));
            }
        }
        if (Bar >= 2) {
            bass(Position, Chord, 0.8f, Out);
            if (Position == 4 || Position == 12) {
                Out.add(new Note(Instrument.Clap, 1.0f, 0.6f));
            }
        }
        if (Bar == IntroBars - 1) {
            arp(Position, Chord, 0.1f + Position * 0.01f, Out);
            if (Position >= 13) {
                Out.add(new Note(Instrument.Clap, 0.9f + (Position - 12) * 0.08f, 0.3f + (Position - 12) * 0.08f));
            }
        }
    }

    private static void groove(Section Stage, int Bar, int Position, int Chord, boolean PhraseEnd, List<Note> Out) {
        boolean Dropping = Stage == Section.Drop;
        float Lift = Dropping ? 1.1f : 1.0f;
        drums(Position, PhraseEnd, Lift, Out);
        bass(Position, Chord, Dropping ? 1.1f : 1.0f, Out);
        arp(Position, Chord, Stage == Section.Groove ? 0.22f : 0.28f, Out);
        if (Stage == Section.Groove) {
            return;
        }
        hook(Instrument.Lead, Position, Chord, Dropping ? 0.62f : 0.5f, Dropping ? 0.2f : 0.12f, Out);
        if (Dropping ? Position % StepsPerBeat == 2 : Position == 6 || Position == 14) {
            stabs(Chord, Dropping ? 0.2f : 0.15f, Out);
        }
    }

    private static void breakdown(int Bar, int Position, int Chord, List<Note> Out) {
        if (Position == 0 || Position == 8) {
            Out.add(new Note(Instrument.Bass, pitch(Roots[Chord]), Position == 0 ? 0.6f : 0.35f));
            stabs(Chord, Position == 0 ? 0.28f : 0.16f, Out);
        }
        if (Position % 2 == 0) {
            arp(Position, Chord, 0.2f, Out);
        }
        hook(Instrument.Stab, Position, Chord, 0.34f, 0.1f, Out);
        if (Bar == BuildBar - 1 && Position >= 12) {
            Out.add(new Note(Instrument.Hat, 1.6f + (Position - 12) * 0.1f, 0.12f + (Position - 12) * 0.04f));
        }
    }

    private static void build(int Bar, int Position, int Chord, List<Note> Out) {
        int Into = Bar - BuildBar;
        if (Into == DropBar - BuildBar - 1 && Position >= GapStep) {
            return;
        }
        int Every = Into == 0 ? 4 : Into == 1 ? 2 : 1;
        float Rise = (Into * StepsPerBar + Position) / (float) (4 * StepsPerBar);
        if (Position % Every == 0) {
            Out.add(new Note(Instrument.Clap, 0.8f + Rise * 1.1f, 0.3f + Rise * 0.45f));
        }
        if (Into < 3 && Position % (Into == 2 ? 2 : StepsPerBeat) == 0) {
            Out.add(new Note(Instrument.Bass, pitch(Roots[Chord]), 0.55f + Rise * 0.2f));
        }
        if (Into >= 2 && Position % StepsPerBeat == 2) {
            Out.add(new Note(Instrument.HatOpen, 1.25f + Rise * 0.4f, 0.25f + Rise * 0.15f));
        }
        arp(Position, Chord, 0.18f + Rise * 0.2f, Out);
    }
}
