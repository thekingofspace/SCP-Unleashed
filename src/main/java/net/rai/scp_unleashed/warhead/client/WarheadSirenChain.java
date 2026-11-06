package net.rai.scp_unleashed.warhead.client;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public final class WarheadSirenChain {
    public static final float Margin = 0.1f;
    public static final float Tail = 0.05f;
    public static final float SecondsPerTick = 0.05f;

    private final DeferredHolder<SoundEvent, SoundEvent> Event;
    private final float BasePitch;
    private final float SteadyStart;
    private final float SteadyEnd;
    private final float Crossfade;
    private final float Gain;
    private final List<Voice> Voices = new ArrayList<>();
    private boolean Fresh = true;

    private static final class Voice {
        private final WarheadSirenSound Sound;
        private float Position;
        private float Weight;

        private Voice(WarheadSirenSound Sound) {
            this.Sound = Sound;
        }
    }

    public WarheadSirenChain(DeferredHolder<SoundEvent, SoundEvent> Event, float BasePitch, float SteadyStart, float SteadyEnd, float Crossfade, float Gain) {
        this.Event = Event;
        this.BasePitch = BasePitch;
        this.SteadyStart = SteadyStart;
        this.SteadyEnd = SteadyEnd;
        this.Crossfade = Crossfade;
        this.Gain = Gain;
    }

    public float spacing() {
        return SteadyEnd - Crossfade - SteadyStart - Margin;
    }

    public float rate(float Pitch) {
        return Mth.clamp(Pitch * BasePitch, 0.5f, 2.0f);
    }

    public int voices() {
        return Voices.size();
    }

    private static float rise(float Progress) {
        return Mth.sin(Mth.HALF_PI * Mth.clamp(Progress, 0.0f, 1.0f));
    }

    private static float fall(float Progress) {
        return Mth.cos(Mth.HALF_PI * Mth.clamp(Progress, 0.0f, 1.0f));
    }

    public void tick(SoundManager Manager, boolean Running, float Pitch, float Volume) {
        if (Running && (Voices.isEmpty() || Voices.getLast().Position >= spacing())) {
            WarheadSirenSound Sound = new WarheadSirenSound(Event.get(), Pitch, 0.0f);
            Manager.play(Sound);
            Voices.add(new Voice(Sound));
        }
        for (Voice Item : Voices) {
            Item.Weight = 0.0f;
        }
        if (!Voices.isEmpty()) {
            Voice Current = Voices.getFirst();
            float Handoff = (Current.Position - (SteadyEnd - Crossfade)) / Crossfade;
            if (Fresh) {
                Current.Weight = rise((Current.Position - SteadyStart) / Crossfade);
                if (Current.Position >= SteadyStart + Crossfade) {
                    Fresh = false;
                }
            } else {
                Current.Weight = fall(Handoff);
            }
            if (Handoff > 0.0f && Voices.size() > 1) {
                Voices.get(1).Weight = rise(Handoff);
                Fresh = false;
            }
        }
        float Step = rate(Pitch) * SecondsPerTick;
        for (Voice Item : Voices) {
            Item.Sound.drive(Pitch, Item.Weight * Volume * Gain);
            Item.Position += Step;
        }
        Voices.removeIf(Item -> {
            if (Item.Position >= SteadyEnd + Tail) {
                Item.Sound.finish();
                return true;
            }
            return false;
        });
    }

    public void stop() {
        for (Voice Item : Voices) {
            Item.Sound.finish();
        }
        Voices.clear();
        Fresh = true;
    }
}
