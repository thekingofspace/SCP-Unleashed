package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.rai.scp_unleashed.daevite.DaeviteCollapse;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;
import net.rai.scp_unleashed.daevite.DaeviteTargets;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public class DaeviteBolt extends ThrowableProjectile {
    public static final int Life = 70;
    public static final int Homing = 22;
    public static final double TurnRate = 0.12;

    private float Damage = DaeviteOverseer.BoltDamage;
    private int TargetId = -1;

    public DaeviteBolt(EntityType<? extends DaeviteBolt> Type, Level World) {
        super(Type, World);
    }

    public DaeviteBolt(Level World, LivingEntity Caster, Vec3 From, @Nullable LivingEntity Target, float Damage) {
        super(DaeviteRegistry.Bolt.get(), From.x, From.y, From.z, World);
        setOwner(Caster);
        this.Damage = Damage;
        TargetId = Target != null ? Target.getId() : -1;
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
        if (!level().isClientSide && tickCount < Homing && TargetId >= 0) {
            Entity Found = level().getEntity(TargetId);
            if (Found instanceof LivingEntity Target && Target.isAlive() && !Scp268.isNeglected(Target)) {
                Vec3 Motion = getDeltaMovement();
                double Pace = Motion.length();
                Vec3 Want = Target.position().add(0.0, Target.getBbHeight() * 0.55, 0.0).subtract(position());
                if (Pace > 1.0e-3 && Want.lengthSqr() > 1.0e-3) {
                    Vec3 Turned = Motion.normalize().add(Want.normalize().subtract(Motion.normalize()).scale(TurnRate)).normalize().scale(Pace);
                    setDeltaMovement(Turned);
                    hasImpulse = true;
                }
            }
        }
        super.tick();
        if (level().isClientSide) {
            level().addParticle(DaeviteCollapse.Beam, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }
        } else if (tickCount > Life) {
            fizzle();
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return super.canHitEntity(Target) && !DaeviteTargets.isAlly(Target) && !(Target instanceof DaeviteBolt);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Entity Target = Result.getEntity();
        DamageSource Source = damageSources().source(DaeviteRegistry.BoltDamage, this, getOwner());
        if (Target.hurt(Source, Damage)) {
            Vec3 Motion = getDeltaMovement().multiply(1.0, 0.0, 1.0);
            if (Target instanceof LivingEntity Body && Motion.lengthSqr() > 1.0e-4) {
                Body.knockback(0.35, -Motion.x, -Motion.z);
            }
        }
        World.playSound(null, getX(), getY(), getZ(), DaeviteRegistry.BoltHit.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        World.sendParticles(DaeviteCollapse.Cinder, getX(), getY(), getZ(), 14, 0.2, 0.2, 0.2, 0.08);
        World.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.04);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), DaeviteRegistry.BoltHit.get(), SoundSource.HOSTILE, 0.7f, 1.2f);
            fizzle();
        }
    }

    private void fizzle() {
        if (level() instanceof ServerLevel World && !isRemoved()) {
            World.sendParticles(DaeviteCollapse.Cinder, getX(), getY(), getZ(), 10, 0.15, 0.15, 0.15, 0.05);
            World.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.01);
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 64.0 * 64.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putFloat("Damage", Damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Damage = Saved.getFloat("Damage");
    }
}
