package net.rai.scp_unleashed.scp106;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp106Portal extends Entity {
    public static final int GrowTicks = 16;
    public static final int FadeTicks = 30;
    public static final float WallWidth = 1.5f;
    public static final float WallHeight = 2.75f;
    public static final float CeilingSize = 2.0f;
    public static final double Lift = 0.01;
    private static final EntityDataAccessor<Byte> FaceData = SynchedEntityData.defineId(Scp106Portal.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> WidthData = SynchedEntityData.defineId(Scp106Portal.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HeightData = SynchedEntityData.defineId(Scp106Portal.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LifeData = SynchedEntityData.defineId(Scp106Portal.class, EntityDataSerializers.INT);

    public Scp106Portal(EntityType<? extends Scp106Portal> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    @Nullable
    public static Scp106Portal place(ServerLevel World, Vec3 Anchor, Direction Face, float Width, float Height, int Life) {
        Scp106Portal Portal = Scp106Registry.Portal.get().create(World);
        if (Portal == null) {
            return null;
        }
        Vec3 Out = Vec3.atLowerCornerOf(Face.getNormal()).scale(Lift);
        Portal.setPos(Anchor.x + Out.x, Anchor.y + Out.y, Anchor.z + Out.z);
        Portal.entityData.set(FaceData, (byte) Face.get3DDataValue());
        Portal.entityData.set(WidthData, Width);
        Portal.entityData.set(HeightData, Height);
        Portal.entityData.set(LifeData, Life);
        return World.addFreshEntity(Portal) ? Portal : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(FaceData, (byte) Direction.NORTH.get3DDataValue());
        Builder.define(WidthData, WallWidth);
        Builder.define(HeightData, WallHeight);
        Builder.define(LifeData, 60);
    }

    public Direction face() {
        return Direction.from3DDataValue(entityData.get(FaceData));
    }

    public float portalWidth() {
        return entityData.get(WidthData);
    }

    public float portalHeight() {
        return entityData.get(HeightData);
    }

    public int life() {
        return entityData.get(LifeData);
    }

    public float spread(float Partial) {
        float Age = tickCount + Partial;
        float Share = Mth.clamp(Age / GrowTicks, 0.0f, 1.0f);
        return 0.15f + 0.85f * (1.0f - (1.0f - Share) * (1.0f - Share));
    }

    public float fade(float Partial) {
        float Left = life() - (tickCount + Partial);
        return Mth.clamp(Left / FadeTicks, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            drip();
            return;
        }
        if (tickCount >= life()) {
            discard();
        }
    }

    private void drip() {
        RandomSource Random = getRandom();
        if (Random.nextFloat() > 0.45f * fade(0.0f)) {
            return;
        }
        Direction Face = face();
        double Spread = spread(0.0f);
        if (Face == Direction.DOWN) {
            double Reach = portalWidth() * 0.4 * Spread;
            level().addParticle(Scp106Registry.Goo.get(), getX() + (Random.nextDouble() - 0.5) * 2.0 * Reach, getY() - 0.05, getZ() + (Random.nextDouble() - 0.5) * 2.0 * Reach,
                    0.0, -0.05, 0.0);
            return;
        }
        Vec3 Across = Vec3.atLowerCornerOf(Face.getClockWise().getNormal());
        double Side = (Random.nextDouble() - 0.5) * portalWidth() * 0.8 * Spread;
        double Height = Random.nextDouble() * portalHeight() * Spread;
        Vec3 Out = Vec3.atLowerCornerOf(Face.getNormal()).scale(0.06);
        level().addParticle(Scp106Registry.Goo.get(), getX() + Across.x * Side + Out.x, getY() + Height, getZ() + Across.z * Side + Out.z, 0.0, -0.02, 0.0);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double Reach = Math.max(portalWidth(), portalHeight());
        return new AABB(getX() - Reach, getY() - Reach, getZ() - Reach, getX() + Reach, getY() + Reach, getZ() + Reach);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
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
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
