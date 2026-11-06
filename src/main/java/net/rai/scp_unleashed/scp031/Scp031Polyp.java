package net.rai.scp_unleashed.scp031;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

public class Scp031Polyp extends Mob implements GeoEntity {
    public static final int Stages = 4;
    public static final float[] Widths = {0.25f, 0.4f, 0.55f, 0.7f};
    public static final float[] Heights = {0.2f, 0.4f, 0.55f, 0.78f};
    public static final float MaxHealth = 2.0f;
    public static final int MinGrowth = 1800;
    public static final int GrowthSpread = 1200;
    public static final int SyncInterval = 20;
    private static final EntityDataAccessor<Integer> StageData = SynchedEntityData.defineId(Scp031Polyp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ProgressData = SynchedEntityData.defineId(Scp031Polyp.class, EntityDataSerializers.INT);
    private static final RawAnimation PulseLoop = RawAnimation.begin().thenLoop("pulse");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Growth;
    private int GrowthTarget;

    public Scp031Polyp(EntityType<? extends Scp031Polyp> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(StageData, 0);
        Builder.define(ProgressData, 0);
    }

    public int stage() {
        return Mth.clamp(entityData.get(StageData), 0, Stages - 1);
    }

    public int progressPercent() {
        return Mth.clamp(entityData.get(ProgressData), 0, 100);
    }

    private int targetGrowth() {
        if (GrowthTarget <= 0) {
            GrowthTarget = MinGrowth + random.nextInt(GrowthSpread + 1);
        }
        return GrowthTarget;
    }

    private int stageFor(int Ticks) {
        return Mth.clamp(Ticks * Stages / Math.max(1, targetGrowth()), 0, Stages - 1);
    }

    public void setGrowth(int Ticks) {
        Growth = Mth.clamp(Ticks, 0, targetGrowth());
        entityData.set(StageData, stageFor(Growth));
        entityData.set(ProgressData, Growth * 100 / targetGrowth());
    }

    public void setStage(int Stage) {
        setGrowth(Mth.clamp(Stage, 0, Stages - 1) * targetGrowth() / Stages + 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        Growth++;
        int Stage = stageFor(Growth);
        if (Stage != stage()) {
            entityData.set(StageData, Stage);
            World.playSound(null, getX(), getY() + 0.2, getZ(), Scp031Registry.PolypGrow.get(), SoundSource.HOSTILE, 0.7f, 0.9f + random.nextFloat() * 0.2f);
            puff(World, 4 + Stage * 2);
        }
        if (Growth % SyncInterval == 0) {
            entityData.set(ProgressData, Growth * 100 / targetGrowth());
            if (random.nextInt(6) == 0) {
                World.playSound(null, getX(), getY() + 0.2, getZ(), Scp031Registry.PolypSquelch.get(), SoundSource.HOSTILE, 0.35f, 0.9f + random.nextFloat() * 0.3f);
            }
        }
        if (Growth >= targetGrowth()) {
            mature(World);
        }
    }

    private void puff(ServerLevel World, int Count) {
        float Scale = Widths[stage()];
        for (int Index = 0; Index < Count; Index++) {
            World.sendParticles(new DustParticleOptions(Index % 2 == 0 ? Scp031Mimic.Rose : Scp031Mimic.Pink, 0.8f), getX(), getY() + Heights[stage()] * 0.6, getZ(), 1,
                    Scale * 0.4, 0.1, Scale * 0.4, 0.0);
        }
    }

    private void mature(ServerLevel World) {
        if (Scp031Census.get(World).full(World.getGameTime())) {
            World.playSound(null, getX(), getY() + 0.2, getZ(), Scp031Registry.PolypWither.get(), SoundSource.HOSTILE, 0.8f, 0.8f + random.nextFloat() * 0.15f);
            puff(World, 8);
            discard();
            return;
        }
        Optional<Scp031Mimic> Risen = Scp031Mimic.spawnAt(World, position(), getYRot());
        if (Risen.isEmpty()) {
            Growth = targetGrowth() - 200;
            return;
        }
        World.playSound(null, getX(), getY() + 0.5, getZ(), Scp031Registry.PolypRise.get(), SoundSource.HOSTILE, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        World.playSound(null, getX(), getY() + 0.5, getZ(), Scp031Registry.PolypRiseLow.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        puff(World, 14);
        discard();
    }

    public static Optional<Scp031Polyp> sprout(ServerLevel World, Vec3 Where) {
        Scp031Polyp Polyp = Scp031Registry.Polyp.get().create(World);
        if (Polyp == null) {
            return Optional.empty();
        }
        Polyp.moveTo(Where.x, Where.y, Where.z, World.random.nextFloat() * 360.0f, 0.0f);
        Polyp.setGrowth(0);
        if (!World.addFreshEntity(Polyp)) {
            return Optional.empty();
        }
        World.playSound(null, Where.x, Where.y + 0.2, Where.z, Scp031Registry.PolypSprout.get(), SoundSource.HOSTILE, 0.8f, 0.95f + World.random.nextFloat() * 0.1f);
        Polyp.puff(World, 6);
        return Optional.of(Polyp);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (StageData.equals(Key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose Stance) {
        int Stage = stage();
        return EntityDimensions.scalable(Widths[Stage], Heights[Stage]);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Growth", Growth);
        Tag.putInt("GrowthTarget", targetGrowth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        GrowthTarget = Tag.getInt("GrowthTarget");
        setGrowth(Tag.getInt("Growth"));
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp031Registry.PolypSquelch.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp031Registry.PolypBurst.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 4, Event -> Event.setAndContinue(PulseLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
