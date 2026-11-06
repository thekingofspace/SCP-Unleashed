package net.rai.scp_unleashed.scp330;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class Scp330Barrier {
    public static final float Capacity = 10.0f;
    public static final int Duration = 600;
    public static final float Spent = 0.001f;
    public static final int Shards = 24;

    private Scp330Barrier() {
    }

    public static void raise(LivingEntity Target) {
        Target.setData(Scp330Registry.Shell, Capacity);
        Target.addEffect(new MobEffectInstance(Scp330Registry.Barrier, Duration, 0, false, false, true));
        if (Target.level() instanceof ServerLevel World) {
            Scp330Taking.play(World, Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0), Scp330Registry.UpLayers, 0.8f, 1.0f);
        }
        Scp330Marks.track(Target);
    }

    public static float pool(LivingEntity Target) {
        if (!Target.hasEffect(Scp330Registry.Barrier)) {
            return 0.0f;
        }
        Float Left = Target.getExistingDataOrNull(Scp330Registry.Shell);
        return Left == null ? 0.0f : Left;
    }

    public static boolean ignores(DamageSource Source) {
        return Source.is(Scp330Registry.GreedDamage) || Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static float absorb(LivingEntity Target, DamageSource Source, float Amount) {
        if (Amount <= 0.0f || ignores(Source)) {
            return Amount;
        }
        float Pool = pool(Target);
        if (Pool <= 0.0f) {
            return Amount;
        }
        float Taken = Math.min(Pool, Amount);
        float Left = Pool - Taken;
        if (Left <= Spent) {
            shatter(Target);
        } else {
            Target.setData(Scp330Registry.Shell, Left);
            if (Target.level() instanceof ServerLevel World) {
                Scp330Taking.play(World, centre(Target), Scp330Registry.ChimeLayers, 0.8f, 0.9f + 0.4f * Left / Capacity);
            }
        }
        return Amount - Taken;
    }

    public static void shatter(LivingEntity Target) {
        Target.removeData(Scp330Registry.Shell);
        Target.removeEffect(Scp330Registry.Barrier);
        if (Target.level() instanceof ServerLevel World) {
            Vec3 At = centre(Target);
            Scp330Taking.play(World, At, Scp330Registry.ShatterLayers, 1.0f, 1.0f);
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                    At.x, At.y, At.z, Shards, Target.getBbWidth() * 0.7, Target.getBbHeight() * 0.45, Target.getBbWidth() * 0.7, 0.1);
        }
        Scp330Marks.update(Target);
    }

    public static void clear(LivingEntity Target) {
        if (Target.hasData(Scp330Registry.Shell)) {
            Target.removeData(Scp330Registry.Shell);
        }
        Scp330Marks.update(Target);
    }

    private static Vec3 centre(LivingEntity Target) {
        return Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0);
    }
}
