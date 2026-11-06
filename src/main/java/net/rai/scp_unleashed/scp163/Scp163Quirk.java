package net.rai.scp_unleashed.scp163;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public enum Scp163Quirk {
    Clap((byte) 4, "clap", 32, 3, true, new int[]{9, 14, 19, 24}, () -> Scp163Registry.ClapLayers, 0.5f, 1.1f),
    Drum((byte) 5, "drum", 32, 2, false, new int[]{7, 11, 15, 19, 23, 27}, () -> Scp163Registry.DrumLayers, 0.4f, 1.0f),
    Ripple((byte) 6, "ripple", 28, 2, false, new int[]{3}, () -> Scp163Registry.RippleLayers, 0.5f, 1.0f),
    Stretch((byte) 7, "stretch", 48, 1, false, new int[]{4, 26}, () -> Scp163Registry.StretchLayers, 0.5f, 1.0f),
    Look((byte) 8, "look", 48, 3, false, new int[]{12}, () -> Scp163Registry.ChitterLayers, 0.4f, 1.2f),
    Preen((byte) 9, "preen", 40, 2, false, new int[]{7, 20, 33}, () -> Scp163Registry.PreenLayers, 0.45f, 1.1f),
    Bob((byte) 10, "bob", 24, 3, true, new int[]{6, 18}, () -> Scp163Registry.BobLayers, 0.35f, 1.2f),
    LookUp((byte) 11, "lookup", 48, 0, false, new int[]{14}, () -> Scp163Registry.ChitterLayers, 0.45f, 0.95f),
    Tap((byte) 12, "tap", 36, 0, false, new int[]{10, 15, 20, 25}, () -> Scp163Registry.TapLayers, 0.55f, 1.0f);

    private final byte Action;
    private final String Clip;
    private final int Ticks;
    private final int Weight;
    private final boolean Cheerful;
    private final int[] Beats;
    private final Supplier<List<DeferredHolder<SoundEvent, SoundEvent>>> Sound;
    private final float Volume;
    private final float Pitch;

    Scp163Quirk(byte Action, String Clip, int Ticks, int Weight, boolean Cheerful, int[] Beats, Supplier<List<DeferredHolder<SoundEvent, SoundEvent>>> Sound,
                float Volume, float Pitch) {
        this.Action = Action;
        this.Clip = Clip;
        this.Ticks = Ticks;
        this.Weight = Weight;
        this.Cheerful = Cheerful;
        this.Beats = Beats;
        this.Sound = Sound;
        this.Volume = Volume;
        this.Pitch = Pitch;
    }

    public byte action() {
        return Action;
    }

    public String clip() {
        return Clip;
    }

    public int ticks() {
        return Ticks;
    }

    public boolean isIdle() {
        return Weight > 0;
    }

    public boolean onBeat(int Elapsed) {
        for (int Beat : Beats) {
            if (Beat == Elapsed) {
                return true;
            }
        }
        return false;
    }

    public List<DeferredHolder<SoundEvent, SoundEvent>> sound() {
        return Sound.get();
    }

    public float volume() {
        return Volume;
    }

    public float pitch() {
        return Pitch;
    }

    @Nullable
    public static Scp163Quirk byAction(byte Action) {
        for (Scp163Quirk Quirk : values()) {
            if (Quirk.Action == Action) {
                return Quirk;
            }
        }
        return null;
    }

    @Nullable
    public static Scp163Quirk pick(RandomSource Random, Scp163Mood Mood, @Nullable Scp163Quirk Last) {
        int Total = 0;
        for (Scp163Quirk Quirk : values()) {
            if (allowed(Quirk, Mood, Last)) {
                Total += Quirk.Weight;
            }
        }
        if (Total <= 0) {
            return null;
        }
        int Roll = Random.nextInt(Total);
        for (Scp163Quirk Quirk : values()) {
            if (!allowed(Quirk, Mood, Last)) {
                continue;
            }
            Roll -= Quirk.Weight;
            if (Roll < 0) {
                return Quirk;
            }
        }
        return null;
    }

    private static boolean allowed(Scp163Quirk Quirk, Scp163Mood Mood, @Nullable Scp163Quirk Last) {
        return Quirk.isIdle() && Quirk != Last && (!Quirk.Cheerful || Mood == Scp163Mood.Calm);
    }
}
