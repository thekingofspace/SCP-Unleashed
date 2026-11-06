package net.rai.scp_unleashed.scp2845;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp2845Sounds {
    private Scp2845Sounds() {
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static void play(ServerLevel World, Vec3 At, DeferredHolder<SoundEvent, SoundEvent> Layer, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
    }
}
