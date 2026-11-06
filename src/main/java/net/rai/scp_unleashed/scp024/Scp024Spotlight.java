package net.rai.scp_unleashed.scp024;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Scp024Spotlight extends Entity {
    private static final EntityDataAccessor<Float> TargetX = SynchedEntityData.defineId(Scp024Spotlight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TargetY = SynchedEntityData.defineId(Scp024Spotlight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TargetZ = SynchedEntityData.defineId(Scp024Spotlight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> Colour = SynchedEntityData.defineId(Scp024Spotlight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> Width = SynchedEntityData.defineId(Scp024Spotlight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> Power = SynchedEntityData.defineId(Scp024Spotlight.class, EntityDataSerializers.FLOAT);
    private Vec3 Previous = Vec3.ZERO;
    private Vec3 Current = Vec3.ZERO;
    private boolean Primed;

    public Scp024Spotlight(EntityType<? extends Scp024Spotlight> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(TargetX, 0.0f);
        Builder.define(TargetY, 0.0f);
        Builder.define(TargetZ, 0.0f);
        Builder.define(Colour, 0xFFF2D8);
        Builder.define(Width, 1.2f);
        Builder.define(Power, 1.0f);
    }

    public void aim(Vec3 Spot) {
        entityData.set(TargetX, (float) Spot.x);
        entityData.set(TargetY, (float) Spot.y);
        entityData.set(TargetZ, (float) Spot.z);
    }

    public Vec3 target() {
        return new Vec3(entityData.get(TargetX), entityData.get(TargetY), entityData.get(TargetZ));
    }

    public void tint(int Rgb) {
        entityData.set(Colour, Rgb & 0xFFFFFF);
    }

    public int colour() {
        return entityData.get(Colour);
    }

    public void spread(float Radius) {
        entityData.set(Width, Radius);
    }

    public float width() {
        return entityData.get(Width);
    }

    public void power(float Level) {
        entityData.set(Power, Level);
    }

    public float power() {
        return entityData.get(Power);
    }

    public Vec3 target(float PartialTick) {
        return Previous.lerp(Current, PartialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 Latest = target();
            if (!Primed) {
                Previous = Latest;
                Current = Latest;
                Primed = true;
            } else {
                Previous = Current;
                Current = Current.lerp(Latest, 0.5);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 Spot = target();
        double Reach = width() + 1.0;
        return new AABB(position(), Spot).inflate(Reach);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }
}
