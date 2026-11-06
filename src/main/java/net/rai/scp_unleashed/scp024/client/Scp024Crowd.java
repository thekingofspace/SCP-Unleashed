package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public final class Scp024Crowd {
    public static final int MaxQueued = 600;
    public static final float CrowdVolume = 1.0f;
    public static final float MusicVolume = 0.8f;
    private static final List<Hit> Queue = new ArrayList<>();
    private static final RandomSource Dice = RandomSource.create();
    private static int Clock;

    private record Hit(int At, Supplier<SoundEvent> Sound, float Volume, float Pitch, SoundSource Source) {
    }

    private Scp024Crowd() {
    }

    private static void at(int Delay, Supplier<SoundEvent> Sound, float Volume, float Pitch, SoundSource Source) {
        if (Queue.size() < MaxQueued) {
            Queue.add(new Hit(Clock + Math.max(0, Delay), Sound, Volume, Mth.clamp(Pitch, 0.5f, 2.0f), Source));
        }
    }

    private static float jitter(float Low, float High) {
        return Low + Dice.nextFloat() * (High - Low);
    }

    public static void applause(int Delay, int Length, float Peak) {
        List<Supplier<SoundEvent>> Hands = List.of(Scp024Registry.ClapScaffold::get, Scp024Registry.ClapBamboo::get, Scp024Registry.ClapFrame::get, Scp024Registry.ClapBook::get);
        for (int Tick = 0; Tick < Length; Tick++) {
            float Rise = Math.min(1.0f, Tick / 6.0f);
            float Fall = Tick < Length * 0.55f ? 1.0f : (Length - Tick) / (Length * 0.45f);
            float Level = Peak * Rise * Fall;
            int Count = (int) Level + (Dice.nextFloat() < Level - (int) Level ? 1 : 0);
            for (int Clap = 0; Clap < Count; Clap++) {
                at(Delay + Tick, Hands.get(Dice.nextInt(Hands.size())), jitter(0.10f, 0.22f) * CrowdVolume, jitter(0.85f, 1.25f), SoundSource.AMBIENT);
            }
        }
    }

    public static void cheer(int Delay, int Voices) {
        List<Supplier<SoundEvent>> Throats = List.of(Scp024Registry.CheerVillager::get, Scp024Registry.CheerPillager::get, Scp024Registry.CheerPiglin::get,
                Scp024Registry.CheerWitch::get, Scp024Registry.CheerVindicator::get);
        for (int Voice = 0; Voice < Voices; Voice++) {
            at(Delay + Dice.nextInt(9), Throats.get(Voice % Throats.size()), jitter(0.22f, 0.36f) * CrowdVolume, jitter(0.88f, 1.28f), SoundSource.AMBIENT);
        }
    }

    public static void ooh(int Delay) {
        List<Supplier<SoundEvent>> Throats = List.of(Scp024Registry.OohVillager::get, Scp024Registry.OohWitch::get);
        for (int Voice = 0; Voice < 5; Voice++) {
            at(Delay + Dice.nextInt(4), Throats.get(Voice % Throats.size()), jitter(0.26f, 0.4f) * CrowdVolume, jitter(0.86f, 1.1f), SoundSource.AMBIENT);
        }
    }

    public static void laugh(int Delay) {
        List<Supplier<SoundEvent>> Throats = List.of(Scp024Registry.LaughWitch::get, Scp024Registry.LaughPiglin::get, Scp024Registry.LaughVillager::get);
        for (int Voice = 0; Voice < 5; Voice++) {
            at(Delay + Dice.nextInt(11), Throats.get(Voice % Throats.size()), jitter(0.24f, 0.38f) * CrowdVolume, jitter(0.95f, 1.3f), SoundSource.AMBIENT);
        }
    }

    public static void drumroll(int Delay, int Length) {
        for (int Tick = 0; Tick < Length; Tick++) {
            float Swell = 0.14f + 0.4f * Tick / (float) Length;
            at(Delay + Tick, Scp024Registry.DrumSnare::get, Swell * MusicVolume, jitter(0.95f, 1.05f), SoundSource.RECORDS);
        }
        at(Delay + Length, Scp024Registry.DrumKick::get, 0.8f * MusicVolume, 1.0f, SoundSource.RECORDS);
        at(Delay + Length, Scp024Registry.DrumCrash::get, 0.55f * MusicVolume, 1.0f, SoundSource.RECORDS);
    }

    private static void note(int Delay, Supplier<SoundEvent> Sound, float Pitch, float Volume) {
        at(Delay, Sound, Volume * MusicVolume, Pitch, SoundSource.RECORDS);
    }

    public static void fanfare(int Delay) {
        float[] Pitches = {0.707f, 0.891f, 1.059f, 1.414f};
        int[] Times = {0, 3, 6, 10};
        for (int Index = 0; Index < Pitches.length; Index++) {
            note(Delay + Times[Index], Scp024Registry.NoteBit::get, Pitches[Index], 0.45f);
            note(Delay + Times[Index], Scp024Registry.NotePling::get, Pitches[Index], 0.3f);
        }
        note(Delay + 10, Scp024Registry.NoteBell::get, 0.707f, 0.4f);
        note(Delay + 10, Scp024Registry.NoteBell::get, 1.059f, 0.3f);
    }

    public static void sting(int Delay) {
        float[] Pitches = {0.749f, 0.707f, 0.667f, 0.63f, 0.63f, 0.625f};
        int[] Times = {0, 8, 16, 24, 28, 32};
        for (int Index = 0; Index < Pitches.length; Index++) {
            note(Delay + Times[Index], Scp024Registry.NoteBuzzer::get, Pitches[Index], Index < 3 ? 0.55f : 0.45f);
        }
    }

    public static void play(Scp024Cue Cue) {
        Minecraft Client = Minecraft.getInstance();
        switch (Cue) {
            case Applause -> applause(0, 46, 3.5f);
            case Cheer -> {
                cheer(0, 7);
                applause(2, 40, 3.0f);
            }
            case Ooh -> ooh(0);
            case Laugh -> laugh(0);
            case Drumroll -> drumroll(0, 34);
            case Fanfare -> fanfare(0);
            case Sting -> sting(0);
            case Buzzer -> {
                note(0, Scp024Registry.NoteBuzzer::get, 0.5f, 0.7f);
                note(0, Scp024Registry.NoteBass::get, 0.5f, 0.6f);
                note(3, Scp024Registry.NoteBuzzer::get, 0.5f, 0.7f);
                note(6, Scp024Registry.NoteBuzzer::get, 0.5f, 0.7f);
            }
            case Ding -> {
                note(0, Scp024Registry.NotePling::get, 1.26f, 0.5f);
                note(0, Scp024Registry.NoteBell::get, 1.26f, 0.35f);
                note(3, Scp024Registry.NotePling::get, 1.68f, 0.5f);
                note(3, Scp024Registry.NoteChime::get, 1.68f, 0.35f);
            }
            case Tick -> {
                note(0, Scp024Registry.NoteCowbell::get, 1.19f, 0.45f);
                note(0, Scp024Registry.NoteXylophone::get, 1.19f, 0.3f);
            }
            case Go -> {
                note(0, Scp024Registry.NoteBell::get, 1.0f, 0.5f);
                note(0, Scp024Registry.NotePling::get, 1.5f, 0.45f);
                note(0, Scp024Registry.NoteBit::get, 2.0f, 0.3f);
                applause(1, 30, 3.0f);
            }
            case Whistle -> {
                for (int Step = 0; Step < 14; Step++) {
                    note(Step, Scp024Registry.NoteFlute::get, 2.0f - Step * 0.09f, 0.35f);
                }
            }
            case Welcome -> {
                fanfare(0);
                cheer(4, 8);
                applause(4, 70, 4.5f);
            }
            case Reveal -> {
                note(0, Scp024Registry.NoteBell::get, 1.0f, 0.5f);
                note(0, Scp024Registry.NoteChime::get, 1.5f, 0.4f);
                cheer(0, 8);
                applause(1, 60, 4.5f);
                confetti(Client);
            }
            case Finale -> {
                fanfare(0);
                cheer(2, 9);
                cheer(14, 7);
                applause(2, 100, 5.0f);
                confetti(Client);
            }
            default -> {
            }
        }
    }

    private static void confetti(Minecraft Client) {
        if (Client.player == null || Client.level == null) {
            return;
        }
        for (int Piece = 0; Piece < 60; Piece++) {
            double X = Client.player.getX() + (Dice.nextDouble() - 0.5) * 6.0;
            double Y = Client.player.getY() + 2.5 + Dice.nextDouble() * 1.5;
            double Z = Client.player.getZ() + (Dice.nextDouble() - 0.5) * 6.0;
            Client.level.addParticle(Scp024Registry.Confetti.get(), X, Y, Z, 0.0, 0.0, 0.0);
        }
        Client.level.addParticle(ParticleTypes.FIREWORK, Client.player.getX(), Client.player.getY() + 3.0, Client.player.getZ(), 0.0, 0.05, 0.0);
    }

    public static void tick(Minecraft Client) {
        Clock++;
        if (Queue.isEmpty()) {
            return;
        }
        if (Client.player == null) {
            Queue.clear();
            return;
        }
        Iterator<Hit> Each = Queue.iterator();
        while (Each.hasNext()) {
            Hit Due = Each.next();
            if (Due.At() > Clock) {
                continue;
            }
            Each.remove();
            Client.getSoundManager().play(new SimpleSoundInstance(Due.Sound().get().getLocation(), Due.Source(), Due.Volume(), Due.Pitch(), Dice, false, 0,
                    SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
        }
    }

    public static void reset() {
        Queue.clear();
    }

    public static boolean active() {
        return !Queue.isEmpty();
    }
}
