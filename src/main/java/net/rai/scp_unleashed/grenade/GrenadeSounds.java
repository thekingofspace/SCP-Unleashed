package net.rai.scp_unleashed.grenade;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GrenadeSounds {
    private GrenadeSounds() {
    }

    public static void play(Level World, Vec3 At, Holder<SoundEvent> Sound, SoundSource Source, float Volume, float Pitch) {
        RandomSource Random = World.getRandom();
        float Jitter = 0.96F + Random.nextFloat() * 0.08F;
        World.playSound(null, At.x, At.y, At.z, Sound, Source, Volume, Pitch * Jitter);
    }

    public static void pinPull(Level World, Entity User) {
        Vec3 At = User.getEyePosition();
        play(World, At, GrenadeRegistry.PinPull, SoundSource.PLAYERS, 0.8F, 1.0F);
        play(World, At, GrenadeRegistry.PinClick, SoundSource.PLAYERS, 0.6F, 1.0F);
    }

    public static void thrown(Level World, Entity User, float Charge) {
        Vec3 At = User.getEyePosition();
        play(World, At, GrenadeRegistry.Throw, SoundSource.PLAYERS, 0.5F + 0.4F * Charge, 1.0F - 0.1F * Charge);
        play(World, At, GrenadeRegistry.Spoon, SoundSource.PLAYERS, 0.55F, 1.0F);
    }

    public static void clink(Level World, Vec3 At, double Speed) {
        float Strength = (float) Mth.clamp(Speed / 0.8, 0.15, 1.0);
        play(World, At, GrenadeRegistry.Bounce, SoundSource.BLOCKS, 0.35F + 0.65F * Strength, 0.9F + 0.2F * (1.0F - Strength));
        play(World, At, GrenadeRegistry.BounceRing, SoundSource.BLOCKS, 0.2F + 0.45F * Strength, 1.0F);
    }

    public static void thud(Level World, Vec3 At, double Speed) {
        float Strength = (float) Mth.clamp(Speed / 0.8, 0.2, 1.0);
        play(World, At, GrenadeRegistry.Bounce, SoundSource.BLOCKS, 0.3F + 0.4F * Strength, 0.75F);
    }

    public static void fizzle(Level World, Vec3 At) {
        play(World, At, GrenadeRegistry.Fizzle, SoundSource.BLOCKS, 0.8F, 1.0F);
    }
}
