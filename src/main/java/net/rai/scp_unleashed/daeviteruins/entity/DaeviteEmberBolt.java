package net.rai.scp_unleashed.daeviteruins.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;

public class DaeviteEmberBolt extends ThrowableProjectile {
    public static final float Damage = 3.0f;
    public static final int Burn = 40;
    public static final int Life = 30;
    public static final double Wobble = 0.045;

    public DaeviteEmberBolt(EntityType<? extends DaeviteEmberBolt> Type, Level World) {
        super(Type, World);
    }

    public DaeviteEmberBolt(Level World, LivingEntity Caster, Vec3 From) {
        super(DaeviteRuinsRegistry.EmberBolt.get(), From.x, From.y, From.z, World);
        setOwner(Caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0, 0.01, 0.0);
            }
            if (random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX() + random.nextGaussian() * 0.1, getY(), getZ() + random.nextGaussian() * 0.1, 0.0, -0.02, 0.0);
            }
            return;
        }
        Vec3 Motion = getDeltaMovement();
        setDeltaMovement(Motion.add(random.nextGaussian() * Wobble, random.nextGaussian() * Wobble, random.nextGaussian() * Wobble).scale(0.985));
        if (isInWaterOrBubble() || tickCount > Life) {
            snuff();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Entity Target = Result.getEntity();
        DamageSource Source = damageSources().source(DaeviteRuinsRegistry.EmberDamage, this, getOwner());
        if (Target.hurt(Source, Damage)) {
            Target.igniteForTicks(Burn);
        }
        World.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.02);
        World.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (!level().isClientSide) {
            snuff();
        }
    }

    private void snuff() {
        if (level() instanceof ServerLevel World && !isRemoved()) {
            World.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.01);
            World.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), 3, 0.08, 0.08, 0.08, 0.01);
            World.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3f, 1.8f);
            discard();
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
