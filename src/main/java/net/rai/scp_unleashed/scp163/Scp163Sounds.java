package net.rai.scp_unleashed.scp163;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class Scp163Sounds {
    private Scp163Sounds() {
    }

    private static void play(ServerLevel World, Vec3 At, Holder<SoundEvent> Sound, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Sound.value(), SoundSource.BLOCKS, Volume, Pitch);
    }

    private static float spread(RandomSource Random, float Base, float Range) {
        return Base + (Random.nextFloat() - 0.5f) * Range;
    }

    public static void impact(ServerLevel World, Vec3 At, RandomSource Random) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp163Registry.CrashLayers) {
            play(World, At, Layer, 1.0f, spread(Random, 1.0f, 0.06f));
        }
    }

    public static void debris(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp163Registry.Debris, spread(Random, 0.8f, 0.3f), spread(Random, 1.0f, 0.25f));
    }

    public static void hullTick(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp163Registry.HullTick, spread(Random, 0.6f, 0.2f), spread(Random, 1.1f, 0.3f));
    }
}
