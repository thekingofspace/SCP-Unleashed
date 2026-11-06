package net.rai.scp_unleashed.scp001scarlet.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class ScarletCultist extends Monster implements GeoEntity {
    public static final double SlotRadius = 3.2;
    public static final double ProvokeRange = 7.0;
    public static final int CalmDelay = 100;
    public static final float EffigyChance = 0.35f;
    private static final EntityDataAccessor<Boolean> LeaderData = SynchedEntityData.defineId(ScarletCultist.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KneelData = SynchedEntityData.defineId(ScarletCultist.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RitualLoop = RawAnimation.begin().thenLoop("ritual");
    private static final RawAnimation StaffLoop = RawAnimation.begin().thenLoop("ritual_staff");
    private static final RawAnimation StrikeOnce = RawAnimation.begin().thenPlay("attack");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos Circle;
    private float SlotAngle;
    private int Calm;

    public ScarletCultist(EntityType<? extends Monster> Type, Level World) {
        super(Type, World);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(LeaderData, false);
        Builder.define(KneelData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(2, new KneelGoal(this));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return Circle == null && super.canUse();
            }
        });
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ScarletCultist.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, Seen -> Seen.distanceToSqr(this) < ProvokeRange * ProvokeRange));
    }

    public boolean leader() {
        return entityData.get(LeaderData);
    }

    public void leader(boolean Value) {
        entityData.set(LeaderData, Value);
    }

    public boolean kneeling() {
        return entityData.get(KneelData);
    }

    public void kneeling(boolean Value) {
        entityData.set(KneelData, Value);
    }

    public void assign(BlockPos Centre, float Angle) {
        Circle = Centre.immutable();
        SlotAngle = Angle;
        Calm = CalmDelay;
    }

    @Nullable
    public BlockPos circle() {
        return Circle;
    }

    public Vec3 slot() {
        if (Circle == null) {
            return position();
        }
        return new Vec3(Circle.getX() + 0.5 + Math.sin(SlotAngle) * SlotRadius, Circle.getY(), Circle.getZ() + 0.5 - Math.cos(SlotAngle) * SlotRadius);
    }

    public boolean atSlot() {
        Vec3 Spot = slot();
        double Dx = getX() - Spot.x;
        double Dz = getZ() - Spot.z;
        return Dx * Dx + Dz * Dz < 0.8 && Math.abs(getY() - Spot.y) < 1.5;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (kneeling() && random.nextInt(6) == 0) {
                double Hood = random.nextBoolean() ? 1.3 : 0.1;
                level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 0.8, getY() + Hood, getZ() + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.012, 0.0);
            }
            return;
        }
        if (getTarget() != null) {
            Calm = 0;
            if (kneeling()) {
                kneeling(false);
            }
        } else if (Calm < CalmDelay) {
            Calm++;
        }
    }

    public boolean calm() {
        return Calm >= CalmDelay && getTarget() == null;
    }

    @Override
    public boolean doHurtTarget(Entity Victim) {
        boolean Hit = super.doHurtTarget(Victim);
        if (level() instanceof ServerLevel) {
            triggerAnim("action", "attack");
        }
        return Hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp001ScarletRegistry.CultistAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp001ScarletRegistry.CultistHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp001ScarletRegistry.CultistDeath.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return kneeling() ? 120 : 240;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean Recent) {
        super.dropCustomDeathLoot(World, Source, Recent);
        if (leader() && Recent && random.nextFloat() < EffigyChance) {
            spawnAtLocation(new ItemStack(Scp001ScarletRegistry.Effigy.get()));
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return Circle == null && super.removeWhenFarAway(Distance);
    }

    public void vanish() {
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 18, 0.3, 0.6, 0.3, 0.02);
            World.sendParticles(net.rai.scp_unleashed.scp001scarlet.throne.ScarletGlyphBlock.Ember, getX(), getY() + 1.0, getZ(), 14, 0.35, 0.7, 0.35, 0.0);
            World.playSound(null, getX(), getY(), getZ(), Scp001ScarletRegistry.Vanish.get(), SoundSource.HOSTILE, 0.9f, 0.8f + random.nextFloat() * 0.2f);
            World.playSound(null, getX(), getY(), getZ(), Scp001ScarletRegistry.VanishHiss.get(), SoundSource.HOSTILE, 0.6f, 0.7f);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Leader", leader());
        if (Circle != null) {
            Tag.putLong("Circle", Circle.asLong());
            Tag.putFloat("Slot", SlotAngle);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        leader(Tag.getBoolean("Leader"));
        if (Tag.contains("Circle")) {
            Circle = BlockPos.of(Tag.getLong("Circle"));
            SlotAngle = Tag.getFloat("Slot");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 5, Event -> {
            if (kneeling()) {
                return Event.setAndContinue(leader() ? StaffLoop : RitualLoop);
            }
            if (Event.isMoving()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("attack", StrikeOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    private static final class KneelGoal extends Goal {
        private final ScarletCultist Owner;
        private int Repath;

        private KneelGoal(ScarletCultist Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.Circle != null && Owner.calm();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.Circle != null && Owner.getTarget() == null;
        }

        @Override
        public void stop() {
            Owner.kneeling(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos Centre = Owner.Circle;
            if (Centre == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Centre.getX() + 0.5, Centre.getY() + 0.5, Centre.getZ() + 0.5);
            if (Owner.atSlot()) {
                Owner.getNavigation().stop();
                if (!Owner.kneeling()) {
                    Owner.kneeling(true);
                }
                float Yaw = (float) (Mth.atan2(Centre.getZ() + 0.5 - Owner.getZ(), Centre.getX() + 0.5 - Owner.getX()) * Mth.RAD_TO_DEG) - 90.0f;
                Owner.setYRot(Yaw);
                Owner.yBodyRot = Yaw;
                Owner.yHeadRot = Yaw;
                return;
            }
            Owner.kneeling(false);
            if (--Repath <= 0) {
                Repath = 20;
                Vec3 Spot = Owner.slot();
                Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, 0.9);
            }
        }
    }
}
