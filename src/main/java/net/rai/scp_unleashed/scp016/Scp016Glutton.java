package net.rai.scp_unleashed.scp016;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class Scp016Glutton extends Scp016Spawn {
    public static final byte ActionGrab = 3;
    public static final double Health = 56.0;
    public static final double Speed = 0.18;
    public static final double Damage = 8.0;
    public static final double Armour = 4.0;
    public static final double Steadiness = 1.0;
    public static final double BaseScale = 1.06;
    public static final int GrowthCap = 4;
    public static final double ScaleStep = 0.05;
    public static final double HealthStep = 6.0;
    public static final double DamageStep = 1.0;
    public static final float GulpHeal = 12.0f;
    public static final int GrabTicks = 32;
    public static final int ContactAt = 22;
    public static final int GulpAt = 8;
    public static final double GrabReach = 2.2;
    public static final double DropDamage = 10.0;
    public static final int DigestTicks = 200;
    public static final double HuntRange = 20.0;
    public static final double PeopleRange = 10.0;
    public static final double FoodWidth = 1.5;
    public static final double FoodHeight = 2.2;
    public static final int SwipeTicks = 20;
    public static final int SwipeAt = 11;
    public static final double SwipeReach = 3.0;
    public static final double SwipeKnock = 0.8;
    public static final Vec3 HandOut = new Vec3(-11.65, 4.77, -20.66);
    public static final Vec3 HandBack = new Vec3(-19.42, 17.0, -17.0);
    public static final Vec3 Mouth = new Vec3(0.0, 19.0, -10.0);
    public static final double PullFrom = 0.5;
    public static final double PullTo = 1.05;
    public static final double GulpTime = 1.2;
    public static final ResourceLocation GrowthId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp016_glutton_growth");
    public static final Scp016Gait.Profile GluttonGait = new Scp016Gait.Profile(24.0, 0.04, 0.5, 3.0, 4,
            new double[]{0.988, 0.544}, new float[]{1.0f, 0.85f},
            Scp016Registry.GluttonStep, Scp016Registry.GluttonThud, null, null, 0.5f, 0.45f, 0.0f, 0.0f, 0.8f, 0.0f);
    private static final EntityDataAccessor<Integer> GrowthData = SynchedEntityData.defineId(Scp016Glutton.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation GrabPlay = RawAnimation.begin().thenPlayAndHold("grab");
    private static final RawAnimation SwipePlay = RawAnimation.begin().thenPlayAndHold("swipe");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    @Nullable
    private LivingEntity Prey;
    @Nullable
    private LivingEntity SwipeTarget;
    private boolean Holding;
    private int Digest;

    public Scp016Glutton(EntityType<? extends Scp016Glutton> Type, Level World) {
        super(Type, World);
        xpReward = 16;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.KNOCKBACK_RESISTANCE, Steadiness).add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.SCALE, BaseScale)
                .add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "glutton";
    }

    @Override
    public boolean hasDeathAnimation() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(GrowthData, 0);
    }

    @Override
    protected void registerGoals() {
        registerKinGoals();
        targetSelector.addGoal(5, new Scp016Goals.Forage(this));
        goalSelector.addGoal(2, new Scp016Goals.Maul(this, 1.05));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    }

    @Override
    public boolean wantsQuarry(LivingEntity Target) {
        return isQuarry(Target) && distanceTo(Target) <= PeopleRange;
    }

    public int growth() {
        return entityData.get(GrowthData);
    }

    public boolean hungry() {
        return Digest <= 0;
    }

    public void setGrowth(int Value) {
        int Clamped = Mth.clamp(Value, 0, GrowthCap);
        entityData.set(GrowthData, Clamped);
        modifier(Attributes.SCALE, Clamped * ScaleStep);
        modifier(Attributes.MAX_HEALTH, Clamped * HealthStep);
        modifier(Attributes.ATTACK_DAMAGE, Clamped * DamageStep);
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    private void modifier(Holder<Attribute> Stat, double Amount) {
        AttributeInstance Instance = getAttribute(Stat);
        if (Instance == null) {
            return;
        }
        Instance.removeModifier(GrowthId);
        if (Amount != 0.0) {
            Instance.addTransientModifier(new AttributeModifier(GrowthId, Amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public void setDigest(int Ticks) {
        Digest = Ticks;
    }

    public boolean isFood(LivingEntity Found) {
        if (!(Found instanceof Mob) || Found instanceof Scp016Spawn || !Found.isAlive() || !Targeting.isTargetable(Found) || isKin(Found) || Found instanceof Player || Found instanceof AbstractVillager
                || ControlBodies.isPersonLike(Found) || Found instanceof AbstractGolem || Found.hasCustomName() || Found.isPassenger() || Found.isVehicle()) {
            return false;
        }
        if (Found instanceof OwnableEntity Pet && Pet.getOwnerUUID() != null) {
            return false;
        }
        if (Found.getType().is(Tags.EntityTypes.BOSSES) || !"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(Found.getType()).getNamespace())) {
            return false;
        }
        if (Found.getBbWidth() > FoodWidth * getScale() || Found.getBbHeight() > FoodHeight * getScale()) {
            return false;
        }
        return Found instanceof Animal || Found instanceof Enemy;
    }

    public double reach(LivingEntity Target, double Base) {
        return Base * getScale() + Target.getBbWidth() * 0.5 + getBbWidth() * 0.5;
    }

    public void beginGrab(LivingEntity Target) {
        Prey = Target;
        Holding = false;
        setAction(ActionGrab, GrabTicks);
        playSound(Scp016Registry.GluttonGrab.get(), 1.0f, 0.75f + random.nextFloat() * 0.1f);
    }

    public void beginSwipe(LivingEntity Target) {
        SwipeTarget = Target;
        setAction(ActionStrike, SwipeTicks);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && Digest > 0) {
            Digest--;
        }
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind == ActionGrab) {
            grabTick(Remaining);
        } else if (Kind == ActionStrike) {
            if (SwipeTarget != null && SwipeTarget.isAlive() && Remaining > SwipeAt) {
                getLookControl().setLookAt(SwipeTarget, 30.0f, 30.0f);
                face(SwipeTarget);
            }
            if (Remaining == SwipeAt) {
                swipe();
            }
        }
    }

    private void grabTick(int Remaining) {
        LivingEntity Held = Prey;
        if (Held == null || !Held.isAlive() || Held.isRemoved() || Held.level() != level()) {
            release();
            return;
        }
        if (!Holding) {
            face(Held);
            getLookControl().setLookAt(Held, 30.0f, 30.0f);
            if (Remaining == ContactAt) {
                if (distanceTo(Held) <= reach(Held, GrabReach) + 0.6) {
                    Holding = true;
                    Held.stopRiding();
                } else {
                    release();
                    return;
                }
            } else {
                return;
            }
        }
        double Time = (GrabTicks - Remaining) / 20.0;
        Vec3 Local;
        if (Time < PullTo) {
            Local = HandOut.lerp(HandBack, Mth.clamp((Time - PullFrom) / (PullTo - PullFrom), 0.0, 1.0));
        } else {
            Local = HandBack.lerp(Mouth, Mth.clamp((Time - PullTo) / (GulpTime - PullTo), 0.0, 1.0));
        }
        Vec3 Point = worldPoint(Local);
        if (Held.position().distanceTo(Point) > 4.0 * getScale()) {
            release();
            return;
        }
        Held.setPos(Point.x, Point.y - Held.getBbHeight() * 0.5, Point.z);
        Held.setDeltaMovement(Vec3.ZERO);
        Held.fallDistance = 0.0f;
        if (Held instanceof Mob Victim) {
            Victim.getNavigation().stop();
        }
        if (Remaining == GulpAt) {
            gulp(Held);
        }
    }

    public Vec3 worldPoint(Vec3 Local) {
        float Yaw = yBodyRot * Mth.DEG_TO_RAD;
        double Scale = getScale() / 16.0;
        Vec3 Forward = new Vec3(-Mth.sin(Yaw), 0.0, Mth.cos(Yaw));
        Vec3 Left = new Vec3(Mth.cos(Yaw), 0.0, Mth.sin(Yaw));
        return position().add(Left.scale(Local.x * Scale)).add(0.0, Local.y * Scale, 0.0).add(Forward.scale(-Local.z * Scale));
    }

    private void gulp(LivingEntity Held) {
        if (level() instanceof ServerLevel World) {
            Vec3 Centre = Held.getBoundingBox().getCenter();
            World.sendParticles(Scp016Registry.Fleck.get(), Centre.x, Centre.y, Centre.z, 36, Held.getBbWidth() * 0.4, Held.getBbHeight() * 0.4, Held.getBbWidth() * 0.4, 0.1);
            World.sendParticles(Scp016Events.SplitDust, Centre.x, Centre.y, Centre.z, 24, Held.getBbWidth() * 0.4, Held.getBbHeight() * 0.4, Held.getBbWidth() * 0.4, 0.0);
            World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.REDSTONE)), Centre.x, Centre.y, Centre.z, 10, 0.2, 0.2, 0.2, 0.06);
            sounds(Scp016Registry.GulpLayers);
        }
        Held.discard();
        Prey = null;
        Holding = false;
        setGrowth(growth() + 1);
        heal(GulpHeal);
        Digest = DigestTicks;
    }

    private void release() {
        Prey = null;
        Holding = false;
        if (action() == ActionGrab) {
            setAction(ActionNone, 0);
        }
    }

    private void swipe() {
        sounds(Scp016Registry.SwipeLayers);
        LivingEntity Target = SwipeTarget;
        if (Target == null || !Target.isAlive() || distanceTo(Target) > reach(Target, SwipeReach) || !getSensing().hasLineOfSight(Target)) {
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        if (Target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (Push.lengthSqr() > 1.0E-6) {
                Push = Push.normalize();
                Target.knockback(SwipeKnock, -Push.x, -Push.z);
            }
            Scp016Affliction.afflict(Target);
            setLastHurtMob(Target);
        }
    }

    private void face(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        float Facing = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Facing);
        setYBodyRot(Facing);
        setYHeadRot(Facing);
    }

    private void sounds(List<DeferredHolder<SoundEvent, SoundEvent>> Layers) {
        float Pitch = 0.9f + random.nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionGrab) {
            Prey = null;
            Holding = false;
        } else if (Kind == ActionStrike) {
            SwipeTarget = null;
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && Holding && Amount >= DropDamage && !level().isClientSide) {
            release();
        }
        return Hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.GluttonAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.GluttonHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.GluttonDeath.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.3f;
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return GluttonGait;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Growth", growth());
        Tag.putInt("Digest", Digest);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setGrowth(Tag.getInt("Growth"));
        Digest = Tag.getInt("Digest");
        if (action() != ActionEmerge) {
            setAction(ActionNone, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 4, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionGrab) {
                return Event.setAndContinue(GrabPlay);
            }
            if (Current == ActionStrike) {
                return Event.setAndContinue(SwipePlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(isCollapsing() ? ShudderLoop : IdleLoop);
        }));
    }
}
