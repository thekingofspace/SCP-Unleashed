package net.rai.scp_unleashed.scp4845.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.block.Scp4845FacadeBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp4845Ambience {
    public static final int Eighth = 5;
    public static final int Bar = Eighth * 8;
    public static final float Volume = 0.12f;
    public static final float HumVolume = 0.08f;
    public static final int HumGap = 80;
    public static final int CheckGap = 20;
    public static final int CeilingReach = 6;
    public static final double Distance = 7.0;
    public static final float Detune = 0.01f;
    public static final int Pling = 0;
    public static final int Harp = 1;
    public static final int Bass = 2;
    public static final int Bell = 3;
    public static final int Flute = 4;
    public static final int Hat = 5;
    public static final int Snare = 6;
    public static final int Kick = 7;
    public static final float[] Gains = {0.45f, 0.75f, 0.85f, 0.35f, 0.8f, 0.18f, 0.3f, 0.5f};
    public static final int[][] Chords = {{15, 19, 22}, {12, 15, 19}, {8, 12, 15}, {10, 14, 17}};
    public static final int[] Roots = {3, 0, 8, 10};
    public static final int[] Progression = {0, 1, 2, 3, 0, 1, 2, 3};
    public static final int[][][] Tune = {
            {{22, 2}, {19, 1}, {22, 1}, {24, 2}, {22, 2}},
            {{19, 3}, {17, 1}, {15, 2}, {-1, 2}},
            {{20, 2}, {24, 1}, {22, 1}, {20, 2}, {19, 2}},
            {{17, 4}, {14, 2}, {-1, 2}},
            {{22, 2}, {19, 1}, {22, 1}, {24, 2}, {22, 2}},
            {{19, 2}, {22, 1}, {19, 1}, {17, 2}, {15, 2}},
            {{24, 2}, {22, 2}, {20, 2}, {17, 2}},
            {{15, 4}, {-1, 4}}};
    private static final List<Note> Score = compose();
    private static final int Length = 16 * Bar;
    private static int Tick;
    private static int Cursor;
    private static boolean Inside;
    private static int NextCheck;
    private static int NextHum;
    private static float Drift;

    public record Note(int Tick, int Voice, int Pitch, float Gain) {
    }

    private Scp4845Ambience() {
    }

    private static List<Note> compose() {
        List<Note> Found = new ArrayList<>();
        for (int Pass = 0; Pass < 2; Pass++) {
            int Lead = Pass == 0 ? Flute : Harp;
            for (int Index = 0; Index < 8; Index++) {
                int Start = (Pass * 8 + Index) * Bar;
                int Chord = Progression[Index];
                for (int Pitch : Chords[Chord]) {
                    Found.add(new Note(Start, Pling, Pitch, 0.5f));
                    Found.add(new Note(Start + 3 * Eighth, Pling, Pitch, 0.38f));
                    Found.add(new Note(Start + 6 * Eighth, Pling, Pitch, 0.3f));
                }
                Found.add(new Note(Start, Bass, Roots[Chord], 0.85f));
                Found.add(new Note(Start + 4 * Eighth, Bass, Roots[Chord] + 7 <= 24 ? Roots[Chord] + 7 : Roots[Chord], 0.6f));
                for (int Step = 0; Step < 8; Step++) {
                    Found.add(new Note(Start + Step * Eighth, Hat, 24, Step % 2 == 0 ? 0.7f : 0.45f));
                }
                Found.add(new Note(Start, Kick, 6, 0.8f));
                Found.add(new Note(Start + 4 * Eighth, Kick, 6, 0.6f));
                Found.add(new Note(Start + 2 * Eighth, Snare, 12, 0.5f));
                Found.add(new Note(Start + 6 * Eighth, Snare, 12, 0.5f));
                if (Pass == 1 && Index % 2 == 0) {
                    Found.add(new Note(Start, Bell, Math.min(24, Chords[Chord][2] + 2), 0.5f));
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

    public static List<Note> score() {
        return Score;
    }

    public static DeferredHolder<SoundEvent, SoundEvent> voice(int Voice) {
        return switch (Voice) {
            case Harp -> Scp4845Registry.MusicHarp;
            case Bass -> Scp4845Registry.MusicBass;
            case Bell -> Scp4845Registry.MusicBell;
            case Flute -> Scp4845Registry.MusicFlute;
            case Hat -> Scp4845Registry.MusicHat;
            case Snare -> Scp4845Registry.MusicSnare;
            case Kick -> Scp4845Registry.MusicKick;
            default -> Scp4845Registry.MusicPling;
        };
    }

    public static float pitchOf(int Pitch) {
        return (float) Math.pow(2.0, (Pitch - 12) / 12.0);
    }

    public static boolean floor(BlockState State) {
        if (!(State.getBlock() instanceof Scp4845FacadeBlock)) {
            return false;
        }
        Scp4845FacadeBlock.Finish Finish = State.getValue(Scp4845FacadeBlock.Surface);
        return Finish == Scp4845FacadeBlock.Finish.DiningTile || Finish == Scp4845FacadeBlock.Finish.KitchenTile || Finish == Scp4845FacadeBlock.Finish.OfficeCarpet;
    }

    public static boolean ceiling(BlockState State) {
        if (!(State.getBlock() instanceof Scp4845FacadeBlock)) {
            return false;
        }
        Scp4845FacadeBlock.Finish Finish = State.getValue(Scp4845FacadeBlock.Surface);
        return Finish == Scp4845FacadeBlock.Finish.Ceiling || Finish == Scp4845FacadeBlock.Finish.CeilingLight;
    }

    public static boolean inside(Level World, BlockPos Feet) {
        if (!floor(World.getBlockState(Feet.below()))) {
            return false;
        }
        for (int Up = 1; Up <= CeilingReach; Up++) {
            if (ceiling(World.getBlockState(Feet.above(Up)))) {
                return true;
            }
        }
        return false;
    }

    public static void stop() {
        Tick = 0;
        Cursor = 0;
        Inside = false;
    }

    private static void play(Minecraft Client, Note Next, RandomSource Random) {
        float Wobble = 1.0f + (Random.nextFloat() - 0.5f) * Detune + Mth.sin(Drift) * 0.003f;
        float Pitch = Mth.clamp(pitchOf(Next.Pitch()) * Wobble, 0.5f, 2.0f);
        float Gain = Volume * Gains[Next.Voice()] * Next.Gain();
        double Angle = Drift * 0.12;
        Client.getSoundManager().play(new SimpleSoundInstance(voice(Next.Voice()).get().getLocation(), SoundSource.MUSIC, Gain, Pitch, SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE, Math.cos(Angle) * Distance, 2.5, Math.sin(Angle) * Distance, true));
    }

    public static void tick(Minecraft Client) {
        if (Client.level == null || Client.player == null) {
            stop();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        if (--NextCheck <= 0) {
            NextCheck = CheckGap;
            boolean Now = inside(Client.level, Client.player.blockPosition());
            if (Now != Inside) {
                Tick = 0;
                Cursor = 0;
            }
            Inside = Now;
        }
        if (!Inside) {
            return;
        }
        RandomSource Random = Client.level.getRandom();
        Drift += 0.02f;
        if (--NextHum <= 0) {
            NextHum = HumGap;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp4845Registry.HumLayers) {
                Client.getSoundManager().play(new SimpleSoundInstance(Layer.get().getLocation(), SoundSource.AMBIENT, HumVolume, 1.0f, SoundInstance.createUnseededRandom(),
                        false, 0, SoundInstance.Attenuation.NONE, 0.0, 3.0, 0.0, true));
            }
        }
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
