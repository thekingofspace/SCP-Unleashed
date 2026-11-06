package net.rai.scp_unleashed.rift;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

public class RiftEntity extends Entity implements Extractable {
    public static final int OpenTicks = 50;
    public static final int CloseTicks = 100;
    public static final int CollapseTicks = 18;
    public static final int DecayDelay = 20;
    public static final float DecayPerTick = 0.015f;
    public static final int Lifetime = 6000;
    public static final int LifetimeSpread = 1200;
    public static final float WidthRatio = 0.42f;

    private static final EntityDataAccessor<Integer> DataTier = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DataExtraction = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DataCollapse = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DataBirth = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DataLifetime = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DataSeed = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);

    private long LastExtracted;
    private float ShownExtraction;
    private float ShownExtractionOld;

    public RiftEntity(EntityType<? extends RiftEntity> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        if (!World.isClientSide) {
            entityData.set(DataBirth, World.getGameTime());
            entityData.set(DataLifetime, Lifetime - LifetimeSpread + World.random.nextInt(LifetimeSpread * 2 + 1));
            entityData.set(DataSeed, World.random.nextInt(4096));
        }
        refreshDimensions();
    }

    public static float heightOf(FragmentRarity Rarity) {
        return 1.2f + 0.35f * (Rarity.getTier() - 1);
    }

    public static AABB boxAt(Vec3 Bottom, FragmentRarity Rarity) {
        float Height = heightOf(Rarity);
        double Half = Height * WidthRatio * 0.5;
        return new AABB(Bottom.x - Half, Bottom.y, Bottom.z - Half, Bottom.x + Half, Bottom.y + Height, Bottom.z + Half);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataTier, FragmentRarity.Faint.getTier());
        Builder.define(DataExtraction, 0.0f);
        Builder.define(DataCollapse, 0);
        Builder.define(DataBirth, 0L);
        Builder.define(DataLifetime, Lifetime);
        Builder.define(DataSeed, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (DataTier.equals(Key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose Stance) {
        float Height = heightOf(getRarity());
        return EntityDimensions.fixed(Height * WidthRatio, Height);
    }

    public void setRarity(FragmentRarity Rarity) {
        entityData.set(DataTier, Rarity.getTier());
        refreshDimensions();
    }

    @Override
    public FragmentRarity getRarity() {
        return FragmentRarity.byTier(entityData.get(DataTier));
    }

    public float getRiftHeight() {
        return heightOf(getRarity());
    }

    public Vec3 getCenter(float PartialTick) {
        return getPosition(PartialTick).add(0.0, getRiftHeight() * 0.5, 0.0);
    }

    public int getSeed() {
        return entityData.get(DataSeed);
    }

    public long getBirth() {
        return entityData.get(DataBirth);
    }

    public int getLifetime() {
        return entityData.get(DataLifetime);
    }

    public boolean isCollapsing() {
        return entityData.get(DataCollapse) > 0;
    }

    public float getCollapse(float PartialTick) {
        int Ticks = entityData.get(DataCollapse);
        if (Ticks <= 0) {
            return 0.0f;
        }
        return Mth.clamp((Ticks - 1 + PartialTick) / CollapseTicks, 0.0f, 1.0f);
    }

    public float getOpenness(float PartialTick) {
        float Age = (float) (level().getGameTime() - getBirth()) + PartialTick;
        float Opening = Mth.clamp(Age / OpenTicks, 0.0f, 1.0f);
        float Closing = Mth.clamp((getLifetime() - Age) / CloseTicks, 0.0f, 1.0f);
        return Opening * Opening * (3.0f - 2.0f * Opening) * Closing * Closing * (3.0f - 2.0f * Closing);
    }

    public boolean isClosing() {
        return level().getGameTime() - getBirth() > getLifetime() - CloseTicks;
    }

    public float getShownExtraction(float PartialTick) {
        return Mth.lerp(PartialTick, ShownExtractionOld, ShownExtraction);
    }

    @Override
    public float getExtraction() {
        return entityData.get(DataExtraction);
    }

    @Override
    public void setExtraction(float Progress) {
        if (level().isClientSide || !canBeExtracted()) {
            return;
        }
        LastExtracted = level().getGameTime();
        float Clamped = Mth.clamp(Progress, 0.0f, 1.0f);
        if (Math.abs(Clamped - getExtraction()) >= 0.002f || Clamped == 0.0f || Clamped == 1.0f) {
            entityData.set(DataExtraction, Clamped);
        }
    }

    @Override
    public boolean canBeExtracted() {
        return isAlive() && !isCollapsing() && !isClosing();
    }

    @Override
    public void collapse() {
        if (level().isClientSide || isCollapsing() || isRemoved()) {
            return;
        }
        entityData.set(DataCollapse, 1);
        float Pitch = 1.12f - 0.07f * getRarity().getTier() + (random.nextFloat() - 0.5f) * 0.06f;
        level().playSound(null, getX(), getY() + getRiftHeight() * 0.5, getZ(), RiftRegistry.RiftCollapse.get(), SoundSource.AMBIENT, 1.6f, Pitch);
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            ShownExtractionOld = ShownExtraction;
            ShownExtraction += (getExtraction() - ShownExtraction) * 0.35f;
            return;
        }
        long Now = level().getGameTime();
        int Collapse = entityData.get(DataCollapse);
        if (Collapse > 0) {
            if (Collapse >= CollapseTicks) {
                discard();
            } else {
                entityData.set(DataCollapse, Collapse + 1);
            }
            return;
        }
        if (!RiftFormation.allowedIn(level())) {
            entityData.set(DataCollapse, 1);
            return;
        }
        long Birth = getBirth();
        if (Birth <= 0L || Birth > Now + 20L) {
            entityData.set(DataBirth, Now);
            Birth = Now;
        }
        if (Now - Birth >= getLifetime()) {
            discard();
            return;
        }
        float Progress = getExtraction();
        if (Progress > 0.0f && Now - LastExtracted > DecayDelay) {
            entityData.set(DataExtraction, Math.max(0.0f, Progress - DecayPerTick));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        String Name = Tag.getString("Rarity");
        for (FragmentRarity Rarity : FragmentRarity.values()) {
            if (Rarity.getSerializedName().equals(Name)) {
                setRarity(Rarity);
            }
        }
        if (Tag.contains("Birth")) {
            entityData.set(DataBirth, Tag.getLong("Birth"));
        }
        if (Tag.contains("Lifetime")) {
            int Saved = Mth.clamp(Tag.getInt("Lifetime"), CloseTicks + OpenTicks, Lifetime + LifetimeSpread);
            long Age = level().getGameTime() - getBirth();
            if (getBirth() > 0L && Age > Saved - CloseTicks) {
                Saved = (int) Math.min(Integer.MAX_VALUE, Age + CloseTicks);
            }
            entityData.set(DataLifetime, Saved);
        }
        if (Tag.contains("Seed")) {
            entityData.set(DataSeed, Tag.getInt("Seed") & 4095);
        }
        entityData.set(DataCollapse, Math.max(0, Tag.getInt("Collapse")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putString("Rarity", getRarity().getSerializedName());
        Tag.putLong("Birth", getBirth());
        Tag.putInt("Lifetime", getLifetime());
        Tag.putInt("Seed", getSeed());
        if (isCollapsing()) {
            Tag.putInt("Collapse", entityData.get(DataCollapse));
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double Half = getRiftHeight() * 0.5;
        return getBoundingBox().inflate(Half * 1.7, Half * 1.05, Half * 1.7);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = 160.0 * getViewScale();
        return Distance < Reach * Reach;
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
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
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }
}
