package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class Scp4666Cues {
    public static final int[][] Tannenbaum = {{8, 2}, {13, 3}, {13, 1}, {13, 4}, {15, 2}, {16, 3}, {16, 1}, {16, 4}, {16, 2}, {15, 2}, {16, 2}, {18, 4}, {11, 4}, {15, 2}, {13, 6}};
    public static final int[][] DeckTheHalls = {{20, 3}, {18, 1}, {16, 2}, {15, 2}, {13, 2}, {15, 2}, {16, 2}, {13, 2}, {15, 1}, {16, 1}, {18, 1}, {15, 1}, {16, 3}, {15, 1}, {13, 2}, {12, 2}, {13, 6}};
    public static final int[][] JingleBells = {{9, 2}, {9, 2}, {9, 4}, {9, 2}, {9, 2}, {9, 4}, {9, 2}, {12, 2}, {5, 3}, {7, 1}, {9, 8}, {10, 2}, {10, 2}, {10, 3}, {10, 1}, {10, 2}, {9, 2}, {9, 2}, {9, 1}, {9, 1}, {9, 2}, {7, 2}, {7, 2}, {9, 2}, {7, 4}, {12, 4}};
    public static final int[][][] Carols = {Tannenbaum, DeckTheHalls, JingleBells};
    private static final Map<MinecraftServer, List<Cue>> Queues = new ConcurrentHashMap<>();

    private record Cue(long Due, Consumer<ServerLevel> Action) {
    }

    private Scp4666Cues() {
    }

    public static void after(ServerLevel World, int Ticks, Consumer<ServerLevel> Action) {
        Queues.computeIfAbsent(World.getServer(), Key -> new ArrayList<>()).add(new Cue(World.getServer().getTickCount() + Math.max(0, Ticks), Action));
    }

    public static void tick(ServerLevel World) {
        List<Cue> Queue = Queues.get(World.getServer());
        if (Queue == null || Queue.isEmpty()) {
            return;
        }
        long Now = World.getServer().getTickCount();
        List<Cue> Ready = new ArrayList<>();
        Iterator<Cue> Walk = Queue.iterator();
        while (Walk.hasNext()) {
            Cue Next = Walk.next();
            if (Next.Due() <= Now) {
                Ready.add(Next);
                Walk.remove();
            }
        }
        for (Cue Next : Ready) {
            Next.Action().accept(World);
        }
    }

    public static int pending(MinecraftServer Server) {
        List<Cue> Queue = Queues.get(Server);
        return Queue == null ? 0 : Queue.size();
    }

    public static void clear(MinecraftServer Server) {
        Queues.remove(Server);
    }

    public static float pitchOf(int Note) {
        return (float) Math.pow(2.0, (Note - 12) / 12.0);
    }

    public static int tune(ServerLevel World, Vec3 Spot, int[][] Melody, int TicksPerBeat, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, int Shift, float Detune) {
        int Cursor = 0;
        for (int[] Note : Melody) {
            int Pitch = Math.max(0, Math.min(24, Note[0] + Shift));
            int Index = Cursor;
            after(World, Index, Level -> {
                float Wobble = 1.0f + (Level.getRandom().nextFloat() - 0.5f) * Detune;
                for (int Layer = 0; Layer < Layers.size(); Layer++) {
                    Holder<SoundEvent> Sound = Layers.get(Layer);
                    Level.playSound(null, Spot.x, Spot.y, Spot.z, Sound.value(), SoundSource.AMBIENT, Volume * (Layer == 0 ? 1.0f : 0.55f),
                            Math.max(0.5f, Math.min(2.0f, pitchOf(Pitch) * Wobble)));
                }
            });
            Cursor += Note[1] * TicksPerBeat;
        }
        return Cursor;
    }

    public static void sound(ServerLevel World, Vec3 Spot, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (int Layer = 0; Layer < Layers.size(); Layer++) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Layers.get(Layer).get(), SoundSource.HOSTILE, Volume * (Layer == 0 ? 1.0f : 0.7f), Pitch);
        }
    }
}
