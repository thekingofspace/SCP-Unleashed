package net.rai.scp_unleashed.scp066.client;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp066.Scp066Registry;

import java.util.List;

public final class Scp066Chord {
    public static final float LoudGain = 0.63f;
    public static final float NormalGain = 0.2f;
    public static final int Spacing = 5;

    public static final List<Layer> Layers = List.of(
            layer(Scp066Registry.ChordDrum, 0, 0.8f, 0),
            layer(Scp066Registry.ChordBass, 6, 0.8f, 0),
            layer(Scp066Registry.ChordBass, 18, 0.55f, 0),
            layer(Scp066Registry.ChordDidgeridoo, 6, 0.6f, 0),
            layer(Scp066Registry.ChordHarp, 6, 0.6f, 0),
            layer(Scp066Registry.ChordHarp, 9, 0.6f, 0),
            layer(Scp066Registry.ChordHarp, 12, 0.6f, 0),
            layer(Scp066Registry.ChordHarp, 15, 0.6f, 0),
            layer(Scp066Registry.ChordGuitar, 9, 0.5f, 0),
            layer(Scp066Registry.ChordGuitar, 15, 0.5f, 0),
            layer(Scp066Registry.ChordPling, 12, 0.5f, 0),
            layer(Scp066Registry.ChordPling, 18, 0.5f, 0),
            layer(Scp066Registry.ChordBell, 6, 0.6f, 1),
            layer(Scp066Registry.ChordBell, 12, 0.6f, 1),
            layer(Scp066Registry.ChordAnvil, 0, 0.45f, 1),
            layer(Scp066Registry.ChordBell, 15, 0.45f, 3),
            layer(Scp066Registry.ChordHarp, 18, 0.45f, 3));

    private Scp066Chord() {
    }

    public record Layer(DeferredHolder<SoundEvent, SoundEvent> Sound, int Semitone, float Volume, int Delay) {
    }

    private static Layer layer(DeferredHolder<SoundEvent, SoundEvent> Sound, int Semitone, float Volume, int Delay) {
        return new Layer(Sound, Semitone, Volume, Delay);
    }
}
