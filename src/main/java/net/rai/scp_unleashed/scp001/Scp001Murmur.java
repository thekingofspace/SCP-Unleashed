package net.rai.scp_unleashed.scp001;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scp001Murmur {
    public static final int Window = 4;
    public static final int WindowCap = 6;
    public static final int TickCap = 3;
    public static final float Height = 1.2f;

    private final List<Pending> Queue = new ArrayList<>();
    private final ArrayDeque<Long> Recent = new ArrayDeque<>();

    private record Pending(long Due, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch, boolean Forced) {
    }

    public boolean play(Scp001Prototype Creature, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch, boolean Forced) {
        Level World = Creature.level();
        long Now = World.getGameTime();
        while (!Recent.isEmpty() && (Recent.peekFirst() <= Now - Window || Recent.peekFirst() > Now)) {
            Recent.pollFirst();
        }
        int SameTick = 0;
        for (long Stamp : Recent) {
            if (Stamp == Now) {
                SameTick++;
            }
        }
        if (!Forced && (Recent.size() + Layers.size() > WindowCap || SameTick + Layers.size() > TickCap)) {
            return false;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            if (World instanceof ServerLevel Server) {
                Server.playSound(null, Creature.getX(), Creature.getY() + Height, Creature.getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
            } else {
                World.playLocalSound(Creature.getX(), Creature.getY() + Height, Creature.getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch, false);
            }
            Recent.addLast(Now);
        }
        return true;
    }

    public void schedule(Scp001Prototype Creature, int Delay, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch, boolean Forced) {
        Queue.add(new Pending(Creature.level().getGameTime() + Delay, Layers, Volume, Pitch, Forced));
    }

    public void stutter(Scp001Prototype Creature, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, int Count, float Volume, float Pitch, float Step, RandomSource Random) {
        int Delay = 0;
        for (int Index = 0; Index < Count; Index++) {
            float Shift = (0.93f + Random.nextFloat() * 0.14f) * (1.0f + Step * Index);
            schedule(Creature, Delay, Layers, Volume * (Index == 0 ? 1.0f : 0.8f), Pitch * Shift, false);
            Delay += 1 + Random.nextInt(2);
        }
    }

    public void tick(Scp001Prototype Creature, boolean Hushed) {
        long Now = Creature.level().getGameTime();
        Iterator<Pending> Each = Queue.iterator();
        List<Pending> Due = new ArrayList<>();
        while (Each.hasNext()) {
            Pending Next = Each.next();
            if (Next.Due() <= Now) {
                Each.remove();
                Due.add(Next);
            }
        }
        for (Pending Next : Due) {
            if (!Hushed || Next.Forced()) {
                play(Creature, Next.Layers(), Next.Volume(), Next.Pitch(), Next.Forced());
            }
        }
    }

    public void clear() {
        Queue.clear();
    }
}
