package net.rai.scp_unleashed.scp2521;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp2521Tendrils extends Entity {
    public static final float Width = 0.9f;
    public static final float Height = 1.8f;
    public static final int GrowTicks = 36;
    public static final int DragTicks = 24;
    private static final EntityDataAccessor<Long> StartData = SynchedEntityData.defineId(Scp2521Tendrils.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DragData = SynchedEntityData.defineId(Scp2521Tendrils.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> ReachData = SynchedEntityData.defineId(Scp2521Tendrils.class, EntityDataSerializers.FLOAT);
    @Nullable
    private UUID Owner;

    public Scp2521Tendrils(EntityType<? extends Scp2521Tendrils> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StartData, 0L);
        Builder.define(DragData, 0L);
        Builder.define(ReachData, 1.8f);
    }

    public void begin(UUID Id, float Reach) {
        Owner = Id;
        entityData.set(StartData, level().getGameTime());
        entityData.set(ReachData, Reach);
    }

    public void drag() {
        entityData.set(DragData, level().getGameTime());
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public float reach() {
        return entityData.get(ReachData);
    }

    public float growth(float Partial) {
        float Seconds = level().getGameTime() - entityData.get(StartData) + Partial;
        return Scp2521Motion.ease(Seconds / GrowTicks);
    }

    public float dragged(float Partial) {
        long Drag = entityData.get(DragData);
        if (Drag <= 0L) {
            return 0.0f;
        }
        return Scp2521Motion.ease((level().getGameTime() - Drag + Partial) / DragTicks);
    }

    public float age(float Partial) {
        return (level().getGameTime() - entityData.get(StartData) + Partial) / 20.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !Scp2521Trips.wants(((ServerLevel) level()).getServer(), this)) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition Transition) {
        return null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        Owner = Saved.hasUUID("Scp2521Owner") ? Saved.getUUID("Scp2521Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        if (Owner != null) {
            Saved.putUUID("Scp2521Owner", Owner);
        }
    }
}
