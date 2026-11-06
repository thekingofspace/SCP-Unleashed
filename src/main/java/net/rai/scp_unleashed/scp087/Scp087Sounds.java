package net.rai.scp_unleashed.scp087;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public final class Scp087Sounds {
    private static final List<Pending> Queue = new ArrayList<>();

    private record Pending(ServerLevel World, long At, Vec3 Pos, Supplier<SoundEvent> Sound, SoundSource Source, float Volume, float Pitch) {
    }

    private Scp087Sounds() {
    }

    public static void play(ServerLevel World, Vec3 Pos, Supplier<SoundEvent> Sound, SoundSource Source, float Volume, float Pitch) {
        World.playSound(null, Pos.x, Pos.y, Pos.z, Sound.get(), Source, Volume, Pitch);
    }

    public static void later(ServerLevel World, int Delay, Vec3 Pos, Supplier<SoundEvent> Sound, SoundSource Source, float Volume, float Pitch) {
        if (Delay <= 0) {
            play(World, Pos, Sound, Source, Volume, Pitch);
            return;
        }
        Queue.add(new Pending(World, World.getGameTime() + Delay, Pos, Sound, Source, Volume, Pitch));
    }

    public static void door(ServerLevel World, Vec3 Pos, float Pitch) {
        play(World, Pos, Scp087Registry.DoorOpen, SoundSource.BLOCKS, 1.0f, Pitch);
        play(World, Pos, Scp087Registry.DoorLow, SoundSource.BLOCKS, 0.8f, Pitch);
        later(World, 14, Pos, Scp087Registry.DoorShut, SoundSource.BLOCKS, 0.9f, Pitch);
    }

    public static void laugh(ServerLevel World, Vec3 Pos, float Volume) {
        float Pitch = 0.9f + World.getRandom().nextFloat() * 0.15f;
        play(World, Pos, Scp087Registry.Laugh, SoundSource.HOSTILE, Volume, Pitch);
        later(World, 5, Pos.add(0.0, -4.0, 0.0), Scp087Registry.LaughEcho, SoundSource.HOSTILE, Volume * 0.7f, Pitch * 0.92f);
        later(World, 11, Pos.add(0.0, -8.0, 0.0), Scp087Registry.LaughEcho, SoundSource.HOSTILE, Volume * 0.45f, Pitch * 0.85f);
    }

    public static void tick() {
        if (Queue.isEmpty()) {
            return;
        }
        Iterator<Pending> Cursor = Queue.iterator();
        while (Cursor.hasNext()) {
            Pending Entry = Cursor.next();
            if (Entry.World().getGameTime() >= Entry.At()) {
                Cursor.remove();
                play(Entry.World(), Entry.Pos(), Entry.Sound(), Entry.Source(), Entry.Volume(), Entry.Pitch());
            }
        }
    }

    public static void clear() {
        Queue.clear();
    }
}
