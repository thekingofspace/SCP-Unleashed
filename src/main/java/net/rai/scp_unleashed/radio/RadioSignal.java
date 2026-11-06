package net.rai.scp_unleashed.radio;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class RadioSignal {
    public static final SoundSource Source = SoundSource.VOICE;

    private RadioSignal() {
    }

    public static void playTo(ServerPlayer Listener, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Listener.playNotifySound(Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void playTo(List<RadioRelay.Hearer> Hearers, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (RadioRelay.Hearer Each : Hearers) {
            playTo(Each.Listener(), Layers, Volume * (0.6f + 0.4f * (float) Each.Strength()), Pitch);
        }
    }
}
