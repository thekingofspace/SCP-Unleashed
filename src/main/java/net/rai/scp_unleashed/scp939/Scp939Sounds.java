package net.rai.scp_unleashed.scp939;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp939Sounds {
    private Scp939Sounds() {
    }

    public static void play(ServerLevel World, Vec3 At, SoundEvent Event, SoundSource Source, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Event, Source, Volume, Pitch);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        play(World, At, Layers, SoundSource.HOSTILE, Volume, Pitch);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            play(World, At, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void play(ServerLevel World, BlockPos At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        play(World, Vec3.atCenterOf(At), Layers, SoundSource.BLOCKS, Volume, Pitch);
    }
}
