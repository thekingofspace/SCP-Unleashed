package net.rai.scp_unleashed.scp066;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp066Tunes {
    public static final int Beat = 4;
    public static final float Harp = 0.55f;
    public static final float Bass = 0.4f;
    public static final float Bell = 0.45f;
    public static final float Chime = 0.38f;
    public static final float Flute = 0.5f;
    public static final float Xylophone = 0.5f;

    public static final List<Score> Melodies = List.of(
            score("hello",
                    beat(0, Scp066Registry.TuneHarp, 1, Harp), beat(1, Scp066Registry.TuneHarp, 5, Harp), beat(2, Scp066Registry.TuneHarp, 8, Harp),
                    beat(3, Scp066Registry.TuneHarp, 13, Harp), beat(4, Scp066Registry.TuneHarp, 12, Harp), beat(5, Scp066Registry.TuneHarp, 8, Harp),
                    beat(6, Scp066Registry.TuneHarp, 10, Harp), beat(7, Scp066Registry.TuneHarp, 12, Harp), beat(8, Scp066Registry.TuneHarp, 13, Harp),
                    beat(8, Scp066Registry.TuneChime, 13, Chime)),
            score("lullaby",
                    beat(0, Scp066Registry.TuneBell, 8, Bell), beat(0, Scp066Registry.TuneHarp, 1, Bass), beat(2, Scp066Registry.TuneBell, 13, Bell),
                    beat(3, Scp066Registry.TuneBell, 12, Bell), beat(4, Scp066Registry.TuneBell, 10, Bell), beat(4, Scp066Registry.TuneHarp, 6, Bass),
                    beat(6, Scp066Registry.TuneBell, 8, Bell), beat(8, Scp066Registry.TuneBell, 5, Bell), beat(8, Scp066Registry.TuneHarp, 1, Bass),
                    beat(8, Scp066Registry.TuneHarp, 8, Bass)),
            score("hop",
                    beat(0, Scp066Registry.TuneXylophone, 8, Xylophone), beat(0, Scp066Registry.TuneHarp, 1, Bass), beat(1, Scp066Registry.TuneXylophone, 13, Xylophone),
                    beat(2, Scp066Registry.TuneXylophone, 8, Xylophone), beat(3, Scp066Registry.TuneXylophone, 13, Xylophone),
                    beat(4, Scp066Registry.TuneXylophone, 10, Xylophone), beat(4, Scp066Registry.TuneHarp, 8, Bass), beat(5, Scp066Registry.TuneXylophone, 15, Xylophone),
                    beat(6, Scp066Registry.TuneXylophone, 12, Xylophone), beat(7, Scp066Registry.TuneXylophone, 17, Xylophone),
                    beat(8, Scp066Registry.TuneXylophone, 13, Xylophone), beat(8, Scp066Registry.TuneHarp, 1, Bass)),
            score("sigh",
                    beat(0, Scp066Registry.TuneFlute, 13, Flute), beat(0, Scp066Registry.TuneHarp, 1, Bass), beat(3, Scp066Registry.TuneFlute, 12, Flute),
                    beat(4, Scp066Registry.TuneFlute, 10, Flute), beat(6, Scp066Registry.TuneFlute, 8, Flute), beat(6, Scp066Registry.TuneHarp, 6, Bass),
                    beat(9, Scp066Registry.TuneFlute, 10, Flute), beat(10, Scp066Registry.TuneFlute, 13, Flute), beat(10, Scp066Registry.TuneHarp, 1, Bass),
                    beat(10, Scp066Registry.TuneHarp, 8, Bass)),
            score("cascade",
                    tick(0, Scp066Registry.TuneChime, 24, Chime), tick(0, Scp066Registry.TuneHarp, 1, Bass), tick(0, Scp066Registry.TuneHarp, 5, Bass),
                    tick(2, Scp066Registry.TuneChime, 20, Chime), tick(4, Scp066Registry.TuneChime, 17, Chime), tick(6, Scp066Registry.TuneChime, 13, Chime),
                    tick(8, Scp066Registry.TuneChime, 8, Chime), tick(10, Scp066Registry.TuneChime, 5, Chime), tick(12, Scp066Registry.TuneChime, 1, Chime),
                    tick(12, Scp066Registry.TuneHarp, 8, Bass), tick(12, Scp066Registry.TuneHarp, 13, Bass)),
            score("echo",
                    beat(0, Scp066Registry.TuneHarp, 8, Harp), beat(1, Scp066Registry.TuneHarp, 10, Harp), beat(2, Scp066Registry.TuneHarp, 12, Harp),
                    beat(3, Scp066Registry.TuneHarp, 13, Harp), beat(5, Scp066Registry.TuneBell, 13, Bell), beat(6, Scp066Registry.TuneBell, 12, Bell),
                    beat(7, Scp066Registry.TuneBell, 10, Bell), beat(8, Scp066Registry.TuneBell, 8, Bell), beat(8, Scp066Registry.TuneHarp, 1, Bass)),
            score("thirds",
                    beat(0, Scp066Registry.TuneHarp, 5, Harp), beat(0, Scp066Registry.TuneHarp, 8, Bass), beat(1, Scp066Registry.TuneHarp, 6, Harp),
                    beat(1, Scp066Registry.TuneHarp, 10, Bass), beat(2, Scp066Registry.TuneHarp, 8, Harp), beat(2, Scp066Registry.TuneHarp, 12, Bass),
                    beat(3, Scp066Registry.TuneHarp, 10, Harp), beat(3, Scp066Registry.TuneHarp, 13, Bass), beat(4, Scp066Registry.TuneHarp, 8, Harp),
                    beat(4, Scp066Registry.TuneHarp, 12, Bass), beat(6, Scp066Registry.TuneHarp, 5, Harp), beat(6, Scp066Registry.TuneHarp, 8, Bass),
                    beat(6, Scp066Registry.TuneBell, 13, Bell)));

    public static final Score Treat = score("treat",
            tick(0, Scp066Registry.TuneChime, 13, 0.45f), tick(2, Scp066Registry.TuneChime, 17, 0.45f), tick(4, Scp066Registry.TuneChime, 20, 0.45f),
            tick(6, Scp066Registry.TuneChime, 24, 0.45f), tick(6, Scp066Registry.TuneBell, 13, 0.3f));
    public static final Score NotYet = score("not_yet",
            tick(0, Scp066Registry.TuneHarp, 8, 0.3f), tick(3, Scp066Registry.TuneHarp, 5, 0.3f));
    public static final Score Ouch = score("ouch",
            tick(0, Scp066Registry.TuneXylophone, 10, 0.4f), tick(2, Scp066Registry.TuneXylophone, 6, 0.4f));
    public static final Score Snarl = score("snarl",
            tick(0, Scp066Registry.StabBass, 0, 0.6f), tick(0, Scp066Registry.StabHarp, 1, 0.4f), tick(0, Scp066Registry.StabHarp, 2, 0.4f));
    public static final Score Hunt = score("hunt",
            tick(0, Scp066Registry.StabBass, 1, 0.55f), tick(0, Scp066Registry.StabDidgeridoo, 1, 0.35f), tick(6, Scp066Registry.StabBass, 2, 0.55f),
            tick(12, Scp066Registry.StabBass, 1, 0.55f), tick(16, Scp066Registry.StabBass, 2, 0.6f), tick(20, Scp066Registry.StabHarp, 13, 0.3f),
            tick(20, Scp066Registry.StabHarp, 14, 0.3f));
    public static final Score Unravel = score("unravel",
            tick(0, Scp066Registry.StabHarp, 13, 0.5f), tick(4, Scp066Registry.StabHarp, 10, 0.5f), tick(8, Scp066Registry.StabHarp, 6, 0.5f),
            tick(12, Scp066Registry.StabHarp, 1, 0.5f), tick(16, Scp066Registry.StabBass, 0, 0.6f));
    public static final Score Change = change();

    private Scp066Tunes() {
    }

    public record Note(int Tick, DeferredHolder<SoundEvent, SoundEvent> Sound, int Semitone, float Volume) {
        public float pitch() {
            return pitchOf(Semitone);
        }
    }

    public record Score(String Name, List<Note> Notes, int Length) {
    }

    public static float pitchOf(int Semitone) {
        return (float) Math.pow(2.0, (Semitone - 12) / 12.0);
    }

    private static Note beat(int Step, DeferredHolder<SoundEvent, SoundEvent> Sound, int Semitone, float Volume) {
        return new Note(Step * Beat, Sound, Semitone, Volume);
    }

    private static Note tick(int Tick, DeferredHolder<SoundEvent, SoundEvent> Sound, int Semitone, float Volume) {
        return new Note(Tick, Sound, Semitone, Volume);
    }

    private static Score score(String Name, Note... Notes) {
        List<Note> Sorted = new ArrayList<>(List.of(Notes));
        Sorted.sort(Comparator.comparingInt(Note::Tick));
        return new Score(Name, List.copyOf(Sorted), Sorted.get(Sorted.size() - 1).Tick() + 1);
    }

    private static Score change() {
        List<Note> Notes = new ArrayList<>();
        Notes.add(tick(0, Scp066Registry.StabHarp, 13, 0.6f));
        Notes.add(tick(0, Scp066Registry.StabHarp, 14, 0.6f));
        Notes.add(tick(0, Scp066Registry.StabBell, 12, 0.4f));
        Notes.add(tick(0, Scp066Registry.StabBell, 13, 0.4f));
        Notes.add(tick(4, Scp066Registry.StabHarp, 10, 0.55f));
        Notes.add(tick(4, Scp066Registry.StabHarp, 11, 0.55f));
        Notes.add(tick(8, Scp066Registry.StabHarp, 6, 0.55f));
        Notes.add(tick(8, Scp066Registry.StabHarp, 7, 0.55f));
        Notes.add(tick(12, Scp066Registry.StabHarp, 1, 0.55f));
        Notes.add(tick(12, Scp066Registry.StabHarp, 2, 0.55f));
        Notes.add(tick(12, Scp066Registry.StabBass, 1, 0.6f));
        Notes.add(tick(16, Scp066Registry.StabDidgeridoo, 0, 0.5f));
        int[] Cluster = {0, 6, 1, 7, 11, 17, 12, 18};
        for (int Tick = Scp066Entity.GreyTicks; Tick < Scp066Entity.ShakeEnd; Tick += 3) {
            float Rise = (Tick - Scp066Entity.GreyTicks) / (float) (Scp066Entity.ShakeEnd - Scp066Entity.GreyTicks);
            int Index = (Tick - Scp066Entity.GreyTicks) / 3;
            int Root = Cluster[Index % Cluster.length];
            Notes.add(tick(Tick, Scp066Registry.StabXylophone, Root, 0.3f + 0.35f * Rise));
            Notes.add(tick(Tick, Scp066Registry.StabXylophone, Root + 1, 0.3f + 0.35f * Rise));
            if (Index % 2 == 0) {
                Notes.add(tick(Tick, Scp066Registry.StabBass, Index % 4 == 0 ? 1 : 2, 0.4f + 0.35f * Rise));
            }
            if (Index % 4 == 0) {
                Notes.add(tick(Tick, Scp066Registry.StabDidgeridoo, 0, 0.45f + 0.2f * Rise));
            }
        }
        for (int Eye = 0; Eye < Scp066Entity.EyeCount; Eye++) {
            int Tick = Scp066Entity.ShakeEnd + (Eye * 5) / 2;
            Notes.add(tick(Tick, Scp066Registry.StabBell, 16 + Eye, 0.4f));
            Notes.add(tick(Tick, Scp066Registry.StabBell, 17 + Eye, 0.4f));
            Notes.add(tick(Tick, Scp066Registry.StabPling, 10 + Eye, 0.32f));
        }
        int Burst = Scp066Entity.EyesEnd;
        Notes.add(tick(Burst + 1, Scp066Registry.StabAnvil, 0, 0.35f));
        Notes.add(tick(Burst, Scp066Registry.StabBass, 0, 0.55f));
        Notes.add(tick(Burst, Scp066Registry.StabDidgeridoo, 0, 0.5f));
        Notes.add(tick(Burst, Scp066Registry.StabHarp, 6, 0.45f));
        Notes.add(tick(Burst, Scp066Registry.StabHarp, 12, 0.45f));
        Notes.add(tick(Burst, Scp066Registry.StabHarp, 13, 0.45f));
        Notes.add(tick(Burst, Scp066Registry.StabDrum, 0, 0.55f));
        Notes.add(tick(Burst + 1, Scp066Registry.StabBell, 6, 0.35f));
        Notes.add(tick(Burst + 1, Scp066Registry.StabBell, 12, 0.35f));
        for (int Step = 0; Step < 5; Step++) {
            Notes.add(tick(Burst + 5 + Step * 2, Scp066Registry.StabPling, 6 + Step * 3, 0.42f));
        }
        Notes.sort(Comparator.comparingInt(Note::Tick));
        return new Score("change", List.copyOf(Notes), Scp066Entity.ChangeTicks);
    }

    public static void play(ServerLevel World, Entity Source, Note Played, boolean Sparkle) {
        World.playSound(null, Source.getX(), Source.getY() + Scp066Entity.Middle, Source.getZ(), Played.Sound().get(), SoundSource.NEUTRAL, Played.Volume(), Played.pitch());
        if (Sparkle) {
            World.sendParticles(ParticleTypes.NOTE, Source.getX(), Source.getY() + Scp066Entity.Crown + 0.2, Source.getZ(), 0,
                    Played.Semitone() / 24.0, 0.0, 0.0, 1.0);
        }
    }

    public static final class Playback {
        private final Score Playing;
        private int Tick;

        public Playback(Score Playing, int Tick) {
            this.Playing = Playing;
            this.Tick = Tick;
        }

        public Score score() {
            return Playing;
        }

        public int position() {
            return Tick;
        }

        public boolean advance(ServerLevel World, Entity Source, boolean Sparkle) {
            for (Note Played : Playing.Notes()) {
                if (Played.Tick() == Tick) {
                    play(World, Source, Played, Sparkle);
                }
            }
            Tick++;
            return Tick < Playing.Length();
        }
    }
}
