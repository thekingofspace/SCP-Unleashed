package net.rai.scp_unleashed.daeviteruins.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;

public class DaeviteFlare extends ThrowableProjectile {
    public static final int Fuse = 14;
    public static final double Radius = 10.0;
    public static final int Glow = 160;
    public static final int Daze = 40;

    public DaeviteFlare(EntityType<? extends DaeviteFlare> Type, Level World) {
        super(Type, World);
    }

    public DaeviteFlare(Level World, LivingEntity Caster, Vec3 From) {
        super(DaeviteRuinsRegistry.Flare.get(), From.x, From.y, From.z, World);
        setOwner(Caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }
        if (tickCount >= Fuse || isInWaterOrBubble()) {
            burst();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        if (!level().isClientSide) {
            burst();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (!level().isClientSide) {
            burst();
        }
    }

    private void burst() {
        if (!(level() instanceof ServerLevel World) || isRemoved()) {
            return;
        }
        boolean Wet = isInWaterOrBubble();
        double Reach = Wet ? Radius * 0.4 : Radius;
        AABB Area = getBoundingBox().inflate(Reach);
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found instanceof Enemy && Found.isAlive())) {
            if (Target.distanceToSqr(this) > Reach * Reach) {
                continue;
            }
            Target.addEffect(new MobEffectInstance(MobEffects.GLOWING, Wet ? Glow / 3 : Glow, 0), getOwner());
            Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Daze, 0), getOwner());
        }
        World.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 40, 0.6, 0.6, 0.6, 0.12);
        World.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 8, 0.4, 0.4, 0.4, 0.0);
        World.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 20, 0.5, 0.5, 0.5, 0.03);
        World.playSound(null, getX(), getY(), getZ(), DaeviteRuinsRegistry.FlareBurst.get(), SoundSource.PLAYERS, 1.2f, 0.9f + random.nextFloat() * 0.2f);
        discard();
    }
}
