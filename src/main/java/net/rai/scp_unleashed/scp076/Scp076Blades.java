package net.rai.scp_unleashed.scp076;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp076Blades extends Entity implements TraceableEntity {
    public static final int WarnTicks = 16;
    public static final int RiseTicks = 3;
    public static final int HoldTicks = 12;
    public static final int SinkTicks = 8;
    public static final double HitRadius = 0.55;
    public static final double HitHeight = 1.5;
    public static final double Toss = 0.35;
    protected static final EntityDataAccessor<Integer> DelayData = SynchedEntityData.defineId(Scp076Blades.class, EntityDataSerializers.INT);

    private float Damage = Scp076Abel.BladeDamage;
    @Nullable
    private UUID OwnerId;
    @Nullable
    private LivingEntity OwnerCache;

    public Scp076Blades(EntityType<? extends Scp076Blades> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    public Scp076Blades(Level World, double X, double Y, double Z, float Yaw, int Delay, @Nullable LivingEntity Owner, float Damage) {
        this(Scp076Registry.Blades.get(), World);
        setPos(X, Y, Z);
        setYRot(Yaw);
        entityData.set(DelayData, Delay);
        this.Damage = Damage;
        if (Owner != null) {
            OwnerId = Owner.getUUID();
            OwnerCache = Owner;
        }
    }

    public int delay() {
        return entityData.get(DelayData);
    }

    public int lifetime() {
        return delay() + WarnTicks + RiseTicks + HoldTicks + SinkTicks;
    }

    public float rise(float Partial) {
        double Time = tickCount + Partial - delay() - WarnTicks;
        if (Time <= 0.0) {
            return 0.0f;
        }
        if (Time < RiseTicks) {
            return (float) (Time / RiseTicks);
        }
        Time -= RiseTicks;
        if (Time < HoldTicks) {
            return 1.0f;
        }
        Time -= HoldTicks;
        return (float) Math.max(0.0, 1.0 - Time / SinkTicks);
    }

    public boolean warning() {
        int Time = tickCount - delay();
        return Time >= 0 && Time < WarnTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DelayData, 0);
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        if (OwnerCache == null && OwnerId != null && level() instanceof ServerLevel World && World.getEntity(OwnerId) instanceof LivingEntity Found) {
            OwnerCache = Found;
        }
        return OwnerCache;
    }

    @Override
    public void tick() {
        super.tick();
        int Time = tickCount - delay();
        if (level().isClientSide) {
            if (Time >= 0 && Time < WarnTicks) {
                warnParticles(Time);
            }
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Time == 0) {
            World.playSound(null, getX(), getY(), getZ(), Scp076Registry.BladesWarn.get(), SoundSource.HOSTILE, 1.0f, 0.85f + random.nextFloat() * 0.2f);
            World.playSound(null, getX(), getY(), getZ(), Scp076Registry.BladesWarnLow.get(), SoundSource.HOSTILE, 0.9f, 0.7f);
        }
        if (Time == WarnTicks) {
            erupt(World);
        }
        if (Time == WarnTicks + RiseTicks + HoldTicks) {
            World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), getY() + 0.6, getZ(), 12, 0.25, 0.4, 0.25, 0.02);
        }
        if (tickCount >= lifetime()) {
            World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), getY() + 0.2, getZ(), 8, 0.2, 0.1, 0.2, 0.01);
            discard();
        }
    }

    private void warnParticles(int Time) {
        BlockState Ground = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (Time % 2 == 0 && !Ground.isAir()) {
            for (int Index = 0; Index < 3; Index++) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX() + (random.nextDouble() - 0.5) * 0.9, getY() + 0.05,
                        getZ() + (random.nextDouble() - 0.5) * 0.9, 0.0, 0.12, 0.0);
            }
        }
        level().addParticle(Scp076DoorBlockEntity.Ash, getX() + (random.nextDouble() - 0.5) * 0.8, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.02, 0.0);
        if (Time > WarnTicks / 2) {
            level().addParticle(Scp076DoorBlockEntity.Ember, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.04, 0.0);
        }
    }

    private void erupt(ServerLevel World) {
        World.playSound(null, getX(), getY(), getZ(), Scp076Registry.BladesErupt.get(), SoundSource.HOSTILE, 1.1f, 0.8f + random.nextFloat() * 0.25f);
        World.playSound(null, getX(), getY(), getZ(), Scp076Registry.BladesRing.get(), SoundSource.HOSTILE, 0.8f, 0.7f + random.nextFloat() * 0.2f);
        BlockState Ground = World.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), getY() + 0.1, getZ(), 18, 0.3, 0.1, 0.3, 0.15);
        }
        LivingEntity Owner = getOwner();
        DamageSource Source = damageSources().source(Scp076Registry.BladesDamage, this, Owner);
        AABB Reach = new AABB(getX() - HitRadius, getY(), getZ() - HitRadius, getX() + HitRadius, getY() + HitHeight, getZ() + HitRadius);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, Reach, LivingEntity::isAlive)) {
            if (Body instanceof Scp076Abel || Body == Owner || Body instanceof Player User && (User.isCreative() || User.isSpectator())) {
                continue;
            }
            if (Body.hurt(Source, Damage)) {
                Vec3 Motion = Body.getDeltaMovement();
                Body.setDeltaMovement(Motion.x * 0.4, Math.max(Motion.y, Toss), Motion.z * 0.4);
                Body.hurtMarked = true;
            }
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
    protected void readAdditionalSaveData(CompoundTag Saved) {
        Damage = Saved.getFloat("Damage");
        OwnerId = Saved.hasUUID("Owner") ? Saved.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        Saved.putFloat("Damage", Damage);
        if (OwnerId != null) {
            Saved.putUUID("Owner", OwnerId);
        }
    }
}
