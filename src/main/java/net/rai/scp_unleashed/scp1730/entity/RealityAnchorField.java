package net.rai.scp_unleashed.scp1730.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;

public class RealityAnchorField extends Entity {
    public static final int HumEvery = 40;
    private static final EntityDataAccessor<Float> DataRadius = SynchedEntityData.defineId(RealityAnchorField.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DataLife = SynchedEntityData.defineId(RealityAnchorField.class, EntityDataSerializers.INT);

    public RealityAnchorField(EntityType<? extends RealityAnchorField> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    public static RealityAnchorField spawn(ServerLevel World, Vec3 At) {
        RealityAnchorField Field = new RealityAnchorField(Scp1730Registry.FieldType.get(), World);
        Field.setPos(At.x, At.y - 0.25, At.z);
        Field.entityData.set(DataRadius, (float) Scp1730Anchors.GrenadeRadius);
        Field.entityData.set(DataLife, Scp1730Anchors.GrenadeTicks);
        World.addFreshEntity(Field);
        return Field;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataRadius, (float) Scp1730Anchors.GrenadeRadius);
        Builder.define(DataLife, Scp1730Anchors.GrenadeTicks);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = radius() + 64.0;
        return Distance < Reach * Reach;
    }

    public double radius() {
        return entityData.get(DataRadius);
    }

    public int life() {
        return entityData.get(DataLife);
    }

    public float share() {
        return life() / (float) Scp1730Anchors.GrenadeTicks;
    }

    @Override
    public void tick() {
        super.tick();
        int Left = life() - 1;
        if (level().isClientSide) {
            motes();
            return;
        }
        entityData.set(DataLife, Left);
        if (Left <= 0) {
            discard();
            return;
        }
        if (Left % HumEvery == 0) {
            level().playSound(null, getX(), getY(), getZ(), Scp1730Registry.AnchorHum.get(), SoundSource.AMBIENT, 0.8f, 0.9f + Left / (float) Scp1730Anchors.GrenadeTicks * 0.2f);
        }
    }

    private void motes() {
        double Radius = radius();
        int Count = 6;
        for (int Index = 0; Index < Count; Index++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Lift = (random.nextDouble() * 2.0 - 1.0) * Radius * 0.95;
            double Ring = Math.sqrt(Math.max(0.0, Radius * Radius - Lift * Lift));
            level().addParticle(Scp1730Registry.AnchorParticle.get(), getX() + Math.cos(Angle) * Ring, getY() + Lift, getZ() + Math.sin(Angle) * Ring, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level().addParticle(Scp1730Registry.AnchorParticle.get(), getX() + (random.nextDouble() - 0.5) * Radius, getY() + (random.nextDouble() - 0.5) * Radius,
                    getZ() + (random.nextDouble() - 0.5) * Radius, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        entityData.set(DataLife, Tag.getInt("Life"));
        entityData.set(DataRadius, Tag.contains("Radius") ? Tag.getFloat("Radius") : (float) Scp1730Anchors.GrenadeRadius);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putInt("Life", life());
        Tag.putFloat("Radius", (float) radius());
    }
}
