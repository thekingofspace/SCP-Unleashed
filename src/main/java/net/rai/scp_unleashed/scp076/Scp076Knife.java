package net.rai.scp_unleashed.scp076;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class Scp076Knife extends ThrowableProjectile {
    public static final double Gravity = 0.02;
    public static final int StuckLife = 50;
    public static final int FlightLife = 120;
    public static final float SpinPerTick = 42.0f;
    protected static final EntityDataAccessor<Boolean> StuckData = SynchedEntityData.defineId(Scp076Knife.class, EntityDataSerializers.BOOLEAN);

    private float Damage = Scp076Abel.KnifeDamage;
    private int StuckTicks;

    public Scp076Knife(EntityType<? extends Scp076Knife> Type, Level World) {
        super(Type, World);
    }

    public Scp076Knife(Level World, LivingEntity Thrower, Vec3 From) {
        super(Scp076Registry.Knife.get(), From.x, From.y, From.z, World);
        setOwner(Thrower);
    }

    public void setDamage(float Value) {
        Damage = Value;
    }

    public boolean isStuck() {
        return entityData.get(StuckData);
    }

    public int stuckTicks() {
        return StuckTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StuckData, false);
    }

    @Override
    protected double getDefaultGravity() {
        return Gravity;
    }

    @Override
    public void tick() {
        if (isStuck()) {
            xo = getX();
            yo = getY();
            zo = getZ();
            StuckTicks++;
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && StuckTicks >= StuckLife) {
                crumble();
            }
            return;
        }
        super.tick();
        if (!level().isClientSide && tickCount > FlightLife) {
            crumble();
        }
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(Scp076DoorBlockEntity.Ash, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return super.canHitEntity(Target) && !(Target instanceof Scp076Abel) && !(Target instanceof Scp076Knife);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Entity Target = Result.getEntity();
        Entity Thrower = getOwner();
        DamageSource Source = damageSources().source(Scp076Registry.KnifeDamage, this, Thrower);
        if (Target.hurt(Source, Damage)) {
            Vec3 Motion = getDeltaMovement().multiply(1.0, 0.0, 1.0);
            if (Target instanceof LivingEntity Body && Motion.lengthSqr() > 1.0e-4) {
                Body.knockback(0.25, -Motion.x, -Motion.z);
            }
            World.playSound(null, getX(), getY(), getZ(), Scp076Registry.KnifeHit.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        }
        crumble();
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (level().isClientSide) {
            return;
        }
        Vec3 Motion = getDeltaMovement();
        Vec3 Hit = Result.getLocation();
        Vec3 Back = Motion.lengthSqr() > 1.0e-6 ? Motion.normalize().scale(0.12) : Vec3.ZERO;
        setPos(Hit.x - Back.x, Hit.y - Back.y, Hit.z - Back.z);
        double Flat = Motion.horizontalDistance();
        setYRot((float) (Mth.atan2(Motion.x, Motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(Motion.y, Flat) * Mth.RAD_TO_DEG));
        setDeltaMovement(Vec3.ZERO);
        entityData.set(StuckData, true);
        playSound(Scp076Registry.KnifeLand.get(), 1.0f, 0.9f + random.nextFloat() * 0.2f);
    }

    private void crumble() {
        if (level() instanceof ServerLevel World && !isRemoved()) {
            World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), getY(), getZ(), 10, 0.1, 0.1, 0.1, 0.02);
            World.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.0);
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
