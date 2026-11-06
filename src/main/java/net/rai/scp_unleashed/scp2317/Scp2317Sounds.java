package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp2317Sounds {
    private Scp2317Sounds() {
    }

    public static void play(ServerLevel World, Vec3 Spot, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void play(ServerLevel World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        play(World, Vec3.atCenterOf(Pos), Layers, SoundSource.BLOCKS, Volume, Pitch);
    }
}
