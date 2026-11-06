package net.rai.scp_unleashed.scp001;

import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import org.jetbrains.annotations.Nullable;

public class Scp001Portal extends Entity {
    public static final float Width = 1.2f;
    public static final float Height = 3.0f;
    public static final int OpenTicks = 8;
    public static final int CloseTicks = 12;
    public static final int Colour = ApocalypticRiftEntity.Colour;

    private static final EntityDataAccessor<Integer> LifeData = SynchedEntityData.defineId(Scp001Portal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AgeData = SynchedEntityData.defineId(Scp001Portal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SeedData = SynchedEntityData.defineId(Scp001Portal.class, EntityDataSerializers.INT);

    private int ClientAge;

    public Scp001Portal(EntityType<? extends Scp001Portal> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    public static Scp001Portal open(ServerLevel World, Vec3 Bottom, float Yaw, int Life) {
        Scp001Portal Portal = Scp001Registry.Portal.get().create(World);
        if (Portal == null) {
            return null;
        }
        Portal.moveTo(Bottom.x, Bottom.y, Bottom.z, Yaw, 0.0f);
        Portal.entityData.set(LifeData, Math.max(OpenTicks + CloseTicks, Life));
        Portal.entityData.set(SeedData, World.random.nextInt(4096));
        if (!World.addFreshEntity(Portal)) {
            return null;
        }
        float Pitch = 0.9f + World.random.nextFloat() * 0.15f;
        World.playSound(null, Bottom.x, Bottom.y + 1.2, Bottom.z, Scp001Registry.PortalOpen.get(), SoundSource.HOSTILE, 1.6f, Pitch);
        World.playSound(null, Bottom.x, Bottom.y + 1.2, Bottom.z, Scp001Registry.PortalHum.get(), SoundSource.HOSTILE, 1.2f, Pitch);
        World.playSound(null, Bottom.x, Bottom.y + 1.2, Bottom.z, Scp001Registry.PortalGlass.get(), SoundSource.HOSTILE, 1.1f, Pitch * 0.9f);
        World.sendParticles(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 1.4f), Bottom.x, Bottom.y + 1.3, Bottom.z, 1, 0.0, 0.0, 0.0, 0.0);
        return Portal;
    }

    public void close() {
        int Age = entityData.get(AgeData);
        int Life = entityData.get(LifeData);
        if (Life - Age > CloseTicks) {
            entityData.set(LifeData, Age + CloseTicks);
        }
    }

    public int getSeed() {
        return entityData.get(SeedData);
    }

    public float openness(float Partial) {
        int Life = entityData.get(LifeData);
        float Age = (level().isClientSide ? ClientAge : entityData.get(AgeData)) + Partial;
        float Rise = Mth.clamp(Age / OpenTicks, 0.0f, 1.0f);
        float Fall = Mth.clamp((Life - Age) / CloseTicks, 0.0f, 1.0f);
        return Rise * Rise * (3.0f - 2.0f * Rise) * Fall * Fall * (3.0f - 2.0f * Fall);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(LifeData, 60);
        Builder.define(AgeData, 0);
        Builder.define(SeedData, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ClientAge = Math.max(ClientAge + 1, entityData.get(AgeData));
            float Open = openness(0.0f);
            if (Open > 0.2f && random.nextInt(2) == 0) {
                double Lift = 0.3 + random.nextDouble() * 2.4;
                level().addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 1.0f), getX() + (random.nextDouble() - 0.5) * 0.8, getY() + Lift,
                        getZ() + (random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.08, 0.02, (random.nextDouble() - 0.5) * 0.08);
            }
            return;
        }
        int Age = entityData.get(AgeData) + 1;
        entityData.set(AgeData, Age);
        if (level() instanceof ServerLevel World && Age % 3 == 0) {
            World.sendParticles(new DustParticleOptions(Scp001Prototype.Gold, 1.2f), getX(), getY() + 1.3, getZ(), 3, 0.25, 0.8, 0.25, 0.0);
        }
        if (Age == entityData.get(LifeData) - CloseTicks && level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY() + 1.2, getZ(), Scp001Registry.PortalClose.get(), SoundSource.HOSTILE, 1.3f, 1.0f);
        }
        if (Age >= entityData.get(LifeData)) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.0, 1.0, 2.0);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = 96.0 * getViewScale();
        return Distance < Reach * Reach;
    }
}
