package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.tune.Scp024Tune;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Scp024Music {
    public static final SoundSource Channel = SoundSource.MUSIC;
    public static final float Level = 0.4f;
    public static final int FadeIn = 40;
    public static final int FadeOut = 30;
    private static final RandomSource Dice = RandomSource.create();
    private static final Map<Scp024Tune.Voice, Supplier<SoundEvent>> Voices = new EnumMap<>(Scp024Tune.Voice.class);
    private static List<List<Scp024Tune.Note>> Timeline;
    private static boolean Playing;
    private static float Volume;
    private static float Target;
    private static int Position;

    static {
        Voices.put(Scp024Tune.Voice.Flute, Scp024Registry.MusicFlute::get);
        Voices.put(Scp024Tune.Voice.Bell, Scp024Registry.MusicBell::get);
        Voices.put(Scp024Tune.Voice.Bit, Scp024Registry.MusicBit::get);
        Voices.put(Scp024Tune.Voice.Chime, Scp024Registry.MusicChime::get);
        Voices.put(Scp024Tune.Voice.Xylophone, Scp024Registry.MusicXylophone::get);
        Voices.put(Scp024Tune.Voice.Bass, Scp024Registry.MusicBass::get);
        Voices.put(Scp024Tune.Voice.Drum, Scp024Registry.MusicDrum::get);
    }

    private Scp024Music() {
    }

    private static List<List<Scp024Tune.Note>> timeline() {
        if (Timeline == null) {
            List<List<Scp024Tune.Note>> Built = new ArrayList<>();
            for (int Tick = 0; Tick < Scp024Tune.Length; Tick++) {
                Built.add(new ArrayList<>());
            }
            for (Scp024Tune.Note Played : Scp024Tune.notes()) {
                Built.get(Played.Tick()).add(Played);
            }
            Timeline = Built;
        }
        return Timeline;
    }

    public static void start() {
        Target = 1.0f;
        if (!Playing) {
            Playing = true;
            Position = 0;
            Volume = 0.0f;
        }
    }

    public static void fade() {
        Target = 0.0f;
    }

    public static boolean active() {
        return Playing;
    }

    public static void stop(Minecraft Client) {
        Playing = false;
        Volume = 0.0f;
        Target = 0.0f;
        Position = 0;
        for (Supplier<SoundEvent> Sound : Voices.values()) {
            Client.getSoundManager().stop(Sound.get().getLocation(), Channel);
        }
    }

    public static void tick(Minecraft Client) {
        if (!Playing || Client.isPaused()) {
            return;
        }
        if (Volume < Target) {
            Volume = Math.min(Target, Volume + 1.0f / FadeIn);
        } else if (Volume > Target) {
            Volume = Math.max(Target, Volume - 1.0f / FadeOut);
        }
        if (Volume <= 0.0f && Target <= 0.0f) {
            Playing = false;
            Position = 0;
            return;
        }
        for (Scp024Tune.Note Played : timeline().get(Position)) {
            SoundEvent Sound = Voices.get(Played.Kind()).get();
            Client.getSoundManager().play(new SimpleSoundInstance(Sound.getLocation(), Channel, Played.Volume() * Level * Volume, Played.pitch(), Dice, false, 0,
                    SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
        }
        Position = (Position + 1) % Scp024Tune.Length;
    }
}
