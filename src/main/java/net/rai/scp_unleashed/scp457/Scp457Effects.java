package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.function.Consumer;

public final class Scp457Effects {
    public static final int EmitterTimeout = 20;
    public static final double FlameFloor = 0.2;
    public static final String[] TrackedBones = {"crown", "head", "body", "waist", "right_arm", "left_arm", "right_forearm", "left_forearm", "right_leg", "left_leg",
            "right_shin", "left_shin"};
    public static final float LoudVolume = 1.4f;
    private static Consumer<Scp457> ClientTicker = Burning -> {
    };

    private Scp457Effects() {
    }

    private static void play(Level World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static void steamBurst(ServerLevel World, Scp457 Burning) {
        double Middle = Burning.getY() + Burning.getBbHeight() * 0.55;
        World.sendParticles(Scp457Registry.Steam.get(), Burning.getX(), Middle, Burning.getZ(), 46, 0.45, 0.8, 0.45, 0.04);
        World.sendParticles(ParticleTypes.CLOUD, Burning.getX(), Middle, Burning.getZ(), 18, 0.4, 0.7, 0.4, 0.03);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Burning.getX(), Middle, Burning.getZ(), 10, 0.35, 0.6, 0.35, 0.02);
        play(World, Burning.position(), Scp457Registry.DouseLayers, LoudVolume, 1.0f);
    }

    public static void hiss(ServerLevel World, Scp457 Burning) {
        World.sendParticles(Scp457Registry.Steam.get(), Burning.getX(), Burning.getY() + Burning.getBbHeight() * 0.7, Burning.getZ(), 8, 0.35, 0.5, 0.35, 0.02);
        World.playSound(null, Burning.getX(), Burning.getY(), Burning.getZ(), Scp457Registry.Hiss.get(), SoundSource.HOSTILE, 0.9f, 0.9f + World.getRandom().nextFloat() * 0.2f);
    }

    public static void swipe(ServerLevel World, Scp457 Burning) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Burning.getYRot());
        Vec3 Front = Burning.position().add(Facing.scale(0.9)).add(0.0, Burning.getBbHeight() * 0.55, 0.0);
        World.sendParticles(ParticleTypes.FLAME, Front.x, Front.y, Front.z, 10, 0.35, 0.3, 0.35, 0.03);
        play(World, Front, Scp457Registry.SwipeLayers, 1.0f, 0.9f + World.getRandom().nextFloat() * 0.2f);
    }

    public static void ignite(ServerLevel World, Entity Target) {
        World.sendParticles(ParticleTypes.FLAME, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), 14, 0.3, Target.getBbHeight() * 0.3, 0.3, 0.02);
        World.playSound(null, Target.getX(), Target.getY(), Target.getZ(), Scp457Registry.Ignite.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    public static void ignition(ServerLevel World, Scp457 Burning) {
        World.sendParticles(ParticleTypes.FLAME, Burning.getX(), Burning.getY() + 1.3, Burning.getZ(), 60, 0.35, 0.8, 0.35, 0.05);
        World.sendParticles(Scp457Registry.Ember.get(), Burning.getX(), Burning.getY() + 1.4, Burning.getZ(), 30, 0.45, 0.6, 0.45, 0.05);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Burning.getX(), Burning.getY() + 1.5, Burning.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        play(World, Burning.position(), Scp457Registry.RiseLayers, LoudVolume, 1.0f);
    }

    public static void fizzle(ServerLevel World, Vec3 At) {
        World.sendParticles(Scp457Registry.Steam.get(), At.x, At.y + 0.6, At.z, 20, 0.4, 0.5, 0.4, 0.03);
        World.playSound(null, At.x, At.y, At.z, Scp457Registry.Hiss.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
    }

    public static void stifled(ServerLevel World, BlockPos Pos) {
        World.sendParticles(ParticleTypes.SMOKE, Pos.getX() + 0.5, Pos.getY() + 0.3, Pos.getZ() + 0.5, 8, 0.25, 0.08, 0.25, 0.01);
        World.sendParticles(Scp457Registry.Ember.get(), Pos.getX() + 0.5, Pos.getY() + 0.25, Pos.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.02);
    }

    public static void collapsing(ServerLevel World, Scp457 Burning, int Time) {
        boolean Wet = Burning.deathKind() == Scp457.DeathWater || Burning.deathKind() == Scp457.DeathFrost;
        double Middle = Burning.getY() + Burning.getBbHeight() * Math.max(0.15, 0.6 - Time / (float) Scp457.CollapseTicks * 0.45);
        if (Time == 1 && !Wet) {
            World.sendParticles(ParticleTypes.FLAME, Burning.getX(), Middle, Burning.getZ(), 30, 0.4, 0.7, 0.4, 0.05);
        }
        if (Time == Scp457.CollapseTicks / 2) {
            play(World, Burning.position(), Scp457Registry.CollapseLayers, 1.2f, 1.0f);
        }
        Scp457Cold.collapsing(World, Burning, Time, Middle);
        if (Time % 3 == 0) {
            World.sendParticles(Scp457Registry.Ash.get(), Burning.getX(), Middle, Burning.getZ(), 5, 0.4, 0.4, 0.4, 0.01);
            if (Wet) {
                World.sendParticles(Scp457Registry.Steam.get(), Burning.getX(), Middle, Burning.getZ(), 4, 0.35, 0.4, 0.35, 0.02);
            }
        }
    }

    public static void ashes(ServerLevel World, Scp457 Burning) {
        World.sendParticles(Scp457Registry.Ash.get(), Burning.getX(), Burning.getY() + 0.3, Burning.getZ(), 40, 0.5, 0.25, 0.5, 0.02);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Burning.getX(), Burning.getY() + 0.4, Burning.getZ(), 10, 0.4, 0.2, 0.4, 0.01);
        Scp457Cold.ashes(World, Burning);
        if (Burning.deathKind() == Scp457.DeathWater || Burning.deathKind() == Scp457.DeathFrost) {
            World.sendParticles(Scp457Registry.Steam.get(), Burning.getX(), Burning.getY() + 0.4, Burning.getZ(), 12, 0.4, 0.2, 0.4, 0.02);
        }
    }

    public static void setClientTicker(Consumer<Scp457> Ticker) {
        ClientTicker = Ticker;
    }

    public static void clientTick(Scp457 Burning) {
        ClientTicker.accept(Burning);
    }
}
