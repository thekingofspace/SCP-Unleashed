package net.rai.scp_unleashed.scp178.client;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp178.Scp178Gait;
import net.rai.scp_unleashed.scp178.Scp178Registry;
import net.rai.scp_unleashed.scp178.Scp178Watcher;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp178Voices {
    public static final byte FadeCue = 74;
    public static final int CalmMin = 90;
    public static final int CalmMax = 260;
    public static final int HuntMin = 40;
    public static final int HuntMax = 120;
    public static final float AmbientVolume = 1.4f;
    public static final float StepVolume = 0.5f;
    public static final float ActionVolume = 1.6f;
    private static final Map<Scp178Watcher, Voice> Voices = new WeakHashMap<>();

    private Scp178Voices() {
    }

    private static final class Voice {
        private int Timer;
        private boolean Arrived;
    }

    public static void tick(Scp178Watcher Watcher) {
        if (Watcher.isFading()) {
            return;
        }
        RandomSource Random = Watcher.getRandom();
        Voice State = Voices.computeIfAbsent(Watcher, Key -> new Voice());
        if (!State.Arrived) {
            State.Arrived = true;
            State.Timer = 30 + Random.nextInt(60);
            play(Watcher, Scp178Registry.ClickLayers, AmbientVolume * 0.8f);
        }
        Scp178Gait Walking = Watcher.gait();
        if (Walking != null && Walking.takeStep()) {
            play(Watcher, Scp178Registry.StepLayers, StepVolume);
        }
        if (--State.Timer > 0) {
            return;
        }
        boolean Hunting = Watcher.isHostile();
        State.Timer = Hunting ? HuntMin + Random.nextInt(HuntMax - HuntMin + 1) : CalmMin + Random.nextInt(CalmMax - CalmMin + 1);
        float Roll = Random.nextFloat();
        if (Hunting) {
            play(Watcher, Roll < 0.45f ? Scp178Registry.ClickLayers : Roll < 0.75f ? Scp178Registry.WhisperLayers : Scp178Registry.BreathLayers, AmbientVolume);
        } else {
            play(Watcher, Roll < 0.45f ? Scp178Registry.BreathLayers : Roll < 0.8f ? Scp178Registry.ClickLayers : Scp178Registry.WhisperLayers, AmbientVolume);
        }
    }

    public static void cue(Scp178Watcher Watcher, byte Cue) {
        if (Cue == Scp178Watcher.ActionWindup) {
            play(Watcher, Scp178Registry.WindupLayers, ActionVolume);
        } else if (Cue == Scp178Watcher.ActionLunge) {
            play(Watcher, Scp178Registry.LungeLayers, ActionVolume);
        } else if (Cue == Scp178Watcher.StrikeCue) {
            play(Watcher, Scp178Registry.StrikeLayers, ActionVolume);
        } else if (Cue == Scp178Watcher.HurtCue) {
            play(Watcher, Scp178Registry.HurtLayers, ActionVolume);
        } else if (Cue == Scp178Watcher.TurnCue) {
            play(Watcher, Scp178Registry.TurnLayers, ActionVolume);
        } else if (Cue == FadeCue) {
            play(Watcher, Scp178Registry.FadeLayers, ActionVolume);
        }
    }

    private static void play(Scp178Watcher Watcher, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        RandomSource Random = Watcher.getRandom();
        float Pitch = 0.94f + Random.nextFloat() * 0.12f;
        for (Holder<SoundEvent> Layer : Layers) {
            Watcher.level().playLocalSound(Watcher.getX(), Watcher.getEyeY(), Watcher.getZ(), Layer.value(), SoundSource.HOSTILE, Volume, Pitch, false);
        }
    }

    public static void reset() {
        Voices.clear();
    }
}
