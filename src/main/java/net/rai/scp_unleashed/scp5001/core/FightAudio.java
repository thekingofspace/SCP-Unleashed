package net.rai.scp_unleashed.scp5001.core;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class FightAudio {
    public static final float Range = 480.0f;
    public static final float Wobble = 0.04f;
    public static final float Swell = 0.12f;
    public static final int Horizon = 400;
    private static final List<Pending> Waiting = new ArrayList<>();
    private static final Map<String, Long> Last = new HashMap<>();
    @Nullable
    public static Consumer<Played> Tap;

    public record Layer(Holder<SoundEvent> Event, float Pitch, float Volume, int Delay) {
    }

    public record Played(long Tick, String Event, double X, double Y, double Z, float Volume, float Pitch) {
    }

    private record Pending(long Due, Vec3 At, Holder<SoundEvent> Event, float Pitch, float Volume, SoundSource Source) {
    }

    private FightAudio() {
    }

    public static SoundEvent event(net.minecraft.resources.ResourceLocation Location) {
        return SoundEvent.createFixedRangeEvent(Location, Range);
    }

    public static Layer layer(Holder<SoundEvent> Event) {
        return new Layer(Event, 1.0f, 1.0f, 0);
    }

    public static Layer layer(Holder<SoundEvent> Event, int Delay) {
        return new Layer(Event, 1.0f, 1.0f, Delay);
    }

    public static Layer layer(Holder<SoundEvent> Event, float Pitch, int Delay) {
        return new Layer(Event, Pitch, 1.0f, Delay);
    }

    public static Layer layer(Holder<SoundEvent> Event, float Pitch, float Volume, int Delay) {
        return new Layer(Event, Pitch, Volume, Delay);
    }

    public static boolean ready(ServerLevel World, String Key, int Cooldown) {
        Long Before = Last.get(Key);
        long Now = World.getGameTime();
        return Before == null || Now - Before >= Cooldown || Now < Before;
    }

    public static boolean cue(ServerLevel World, Vec3 At, String Key, int Cooldown, SoundSource Source, Layer... Layers) {
        if (!ready(World, Key, Cooldown)) {
            return false;
        }
        long Now = World.getGameTime();
        Last.put(Key, Now);
        for (Layer Each : Layers) {
            float Pitch = Each.Pitch() * (1.0f + (World.random.nextFloat() * 2.0f - 1.0f) * Wobble);
            float Volume = Math.min(1.0f, Each.Volume() * (1.0f - World.random.nextFloat() * Swell));
            if (Each.Delay() <= 0) {
                emit(World, At, Each.Event(), Pitch, Volume, Source);
            } else {
                Waiting.add(new Pending(Now + Each.Delay(), At, Each.Event(), Pitch, Volume, Source));
            }
        }
        return true;
    }

    public static boolean cue(ServerLevel World, Vec3 At, String Key, int Cooldown, Layer... Layers) {
        return cue(World, At, Key, Cooldown, SoundSource.AMBIENT, Layers);
    }

    public static void tick(ServerLevel World) {
        if (Waiting.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        List<Pending> Due = new ArrayList<>();
        Iterator<Pending> Each = Waiting.iterator();
        while (Each.hasNext()) {
            Pending Next = Each.next();
            if (Next.Due() <= Now || Next.Due() - Now > Horizon) {
                Each.remove();
                Due.add(Next);
            }
        }
        for (Pending Next : Due) {
            emit(World, Next.At(), Next.Event(), Next.Pitch(), Next.Volume(), Next.Source());
        }
    }

    public static void reset() {
        Waiting.clear();
        Last.clear();
    }

    private static void emit(ServerLevel World, Vec3 At, Holder<SoundEvent> Event, float Pitch, float Volume, SoundSource Source) {
        World.playSound(null, At.x, At.y, At.z, Event, Source, Volume, Pitch);
        Consumer<Played> Listener = Tap;
        if (Listener != null) {
            String Name = Event.unwrapKey().map(Key -> Key.location().getPath()).orElse("unknown");
            Listener.accept(new Played(World.getGameTime(), Name, At.x, At.y, At.z, Volume, Pitch));
        }
    }
}
