package net.rai.scp_unleashed.transferpod;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.stasis.StasisEffects;
import net.rai.scp_unleashed.stasis.StasisRegistry;

public final class TransferPodEffects {
    public static final FragmentRarity Grade = FragmentRarity.Abyssal;
    public static final float PitchDrop = 0.12f;
    public static final int Sparks = 26;
    public static final int Smoke = 14;

    private TransferPodEffects() {
    }

    public static float pitch(ServerLevel World) {
        return StasisEffects.pitch(Grade, World) - PitchDrop;
    }

    public static void release(ServerLevel World, Vec3 At, TransferPodContent Held) {
        float Pitch = pitch(World);
        play(World, At, StasisRegistry.Release.get(), 1.0f, Pitch);
        play(World, At, StasisRegistry.ReleaseHiss.get(), 0.8f, Pitch);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : TransferPodRegistry.UnsealLayers) {
            play(World, At, Layer.get(), 0.9f, 1.0f);
        }
        Vec3 Centre = At.add(0.0, 0.3, 0.0);
        int Colour = 0xFF000000 | Held.rgb();
        World.sendParticles(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 0.6f), Centre.x, Centre.y, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 0.9f), Centre.x, Centre.y, Centre.z, Sparks, 0.35, 0.2, 0.35, 0.08);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y, Centre.z, Smoke, 0.3, 0.1, 0.3, 0.02);
    }

    public static void breakOut(ServerLevel World, Vec3 At, TransferPodContent Held) {
        float Pitch = pitch(World);
        play(World, At, StasisRegistry.Breakout.get(), 1.2f, Pitch);
        play(World, At, StasisRegistry.BreakoutShards.get(), 1.0f, Pitch);
        play(World, At, TransferPodRegistry.Vent.get(), 1.0f, 0.9f);
        Vec3 Centre = At.add(0.0, 0.3, 0.0);
        int Colour = 0xFF000000 | Held.rgb();
        World.sendParticles(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 0.9f), Centre.x, Centre.y, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), Centre.x, Centre.y, Centre.z, 24, 0.3, 0.3, 0.3, 0.15);
        World.sendParticles(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 1.1f), Centre.x, Centre.y, Centre.z, Sparks, 0.4, 0.3, 0.4, 0.12);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y, Centre.z, Smoke, 0.35, 0.2, 0.35, 0.03);
    }

    public static void deny(ServerLevel World, Vec3 At) {
        StasisEffects.deny(World, At, Grade);
    }

    private static void play(ServerLevel World, Vec3 At, SoundEvent Event, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Event, SoundSource.PLAYERS, Volume, Pitch);
    }
}
