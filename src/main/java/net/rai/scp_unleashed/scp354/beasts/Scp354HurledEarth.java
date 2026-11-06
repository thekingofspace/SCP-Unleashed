package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Scp354HurledEarth extends ThrowableProjectile {
    public static final float HitDamage = 10.0f;
    public static final float SplashDamage = 5.0f;
    public static final double SplashRadius = 1.8;
    public static final double Gravity = 0.05;
    public static final double LaunchSpeed = 1.15;
    public static final int MaxLife = 120;
    private static final EntityDataAccessor<BlockState> HeldData = SynchedEntityData.defineId(Scp354HurledEarth.class, EntityDataSerializers.BLOCK_STATE);

    public Scp354HurledEarth(EntityType<? extends Scp354HurledEarth> Type, Level World) {
        super(Type, World);
    }

    public Scp354HurledEarth(Level World, LivingEntity Thrower, BlockState Held) {
        super(Scp354BeastRegistry.HurledEarth.get(), Thrower, World);
        entityData.set(HeldData, Held);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(HeldData, Blocks.COARSE_DIRT.defaultBlockState());
    }

    public BlockState held() {
        return entityData.get(HeldData);
    }

    @Override
    protected double getDefaultGravity() {
        return Gravity;
    }

    public void launchAt(Vec3 Aim) {
        Vec3 Delta = Aim.subtract(position());
        double Flat = Math.sqrt(Delta.x * Delta.x + Delta.z * Delta.z);
        double Ticks = Math.max(6.0, Flat / LaunchSpeed);
        double Rise = (Delta.y + 0.5 * Gravity * Ticks * Ticks) / Ticks;
        double Drag = 1.0 + Ticks * 0.005;
        setDeltaMovement(Delta.x / Ticks * Drag, Rise * Drag, Delta.z / Ticks * Drag);
        hasImpulse = true;
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return super.canHitEntity(Target) && !Target.getType().is(Scp354BeastRegistry.Spawns);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > MaxLife) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        super.onHitEntity(Result);
        Entity Target = Result.getEntity();
        Entity Thrower = getOwner();
        if (Target.hurt(damageSources().mobProjectile(this, Thrower instanceof LivingEntity Living ? Living : null), HitDamage) && Target instanceof LivingEntity Living) {
            Vec3 Push = getDeltaMovement().multiply(1.0, 0.0, 1.0);
            if (Push.lengthSqr() > 1.0e-4) {
                Living.knockback(0.9, -Push.x, -Push.z);
            }
        }
    }

    @Override
    protected void onHit(HitResult Result) {
        super.onHit(Result);
        if (!(level() instanceof ServerLevel World) || isRemoved()) {
            return;
        }
        Entity Thrower = getOwner();
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, held()), getX(), getY() + 0.2, getZ(), 40, 0.5, 0.3, 0.5, 0.2);
        AABB Area = getBoundingBox().inflate(SplashRadius);
        Entity Struck = Result instanceof EntityHitResult Hit ? Hit.getEntity() : null;
        for (LivingEntity Victim : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != Thrower && Found != Struck && Found.isAlive()
                && !Found.getType().is(Scp354BeastRegistry.Spawns) && Found.distanceToSqr(this) <= SplashRadius * SplashRadius)) {
            Victim.hurt(damageSources().mobProjectile(this, Thrower instanceof LivingEntity Living ? Living : null), SplashDamage);
        }
        for (var Layer : Scp354BeastRegistry.BruteClodHit) {
            World.playSound(null, getX(), getY(), getZ(), Layer.get(), getSoundSource(), 1.6f, 0.8f + random.nextFloat() * 0.15f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.put("Held", NbtUtils.writeBlockState(held()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Held")) {
            entityData.set(HeldData, NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), Tag.getCompound("Held")));
        }
    }
}
