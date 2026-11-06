package net.rai.scp_unleashed.plumbing;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class Washing {
    public static final int RinseParticles = 3;

    private Washing() {
    }

    public static void applyDefaults(EntityWashedEvent Event) {
        Entity Target = Event.getEntity();
        ServerLevel World = Event.getLevel();
        if (Event.shouldExtinguish() && Target.isOnFire()) {
            Target.clearFire();
            World.playSound(null, Target.getX(), Target.getY(), Target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6f, 1.6f + World.getRandom().nextFloat() * 0.3f);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
        }
        if (Event.shouldRinse() && Target instanceof LivingEntity) {
            rinse(World, Target, Math.max(1, Event.getStrength()));
        }
    }

    public static void rinse(ServerLevel World, Entity Target, int Strength) {
        AABB Box = Target.getBoundingBox();
        double Width = Box.getXsize() * 0.35;
        double Depth = Box.getZsize() * 0.35;
        double Middle = Box.minY + Box.getYsize() * 0.45;
        int Count = RinseParticles * Strength;
        World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.COARSE_DIRT.defaultBlockState()),
                Target.getX(), Middle, Target.getZ(), Count, Width, Box.getYsize() * 0.3, Depth, 0.0);
        World.sendParticles(ParticleTypes.SPLASH, Target.getX(), Box.maxY, Target.getZ(), Count + 2, Width, 0.05, Depth, 0.1);
    }
}
