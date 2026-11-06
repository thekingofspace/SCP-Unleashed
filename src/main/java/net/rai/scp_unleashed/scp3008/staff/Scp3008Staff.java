package net.rai.scp_unleashed.scp3008.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.scp3008.Scp3008Hours;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class Scp3008Staff extends PathfinderMob implements GeoEntity {
    public static final int ActivityIdle = 0;
    public static final int ActivityTidy = 1;
    public static final int ActivityStare = 2;
    public static final int ActivityPost = 3;
    public static final double Speed = 0.25;
    public static final double FollowRange = 48.0;
    public static final double StepHeight = 1.0;
    public static final double WanderModifier = 0.62;
    public static final double ProwlModifier = 0.9;
    public static final double ProvokedModifier = 1.0;
    public static final double BaseReach = 0.83;
    public static final double WindLeeway = 0.6;
    public static final double CullReach = 1.4;
    public static final double CullRise = 0.4;
    public static final int AttackCooldown = 24;
    public static final int WindTicks = 6;
    public static final int ProvokeTicks = 200;
    public static final int ShoveLimit = 16;
    public static final int ShoveStare = 70;
    public static final int WitnessStare = 80;
    public static final double WitnessRange = 12.0;
    public static final int AnnounceTicks = 40;
    public static final int AlertSpread = 30;
    public static final int AmbientInterval = 260;
    public static final int ShieldCooldown = 60;
    public static final float ShieldChance = 0.5f;
    public static final float ThornChance = 0.35f;
    public static final float ThornDamage = 2.0f;
    public static final double ThornReach = 4.0;
    public static final float StepVolume = 0.3f;
    public static final float RunVolume = 0.55f;
    public static final float SqueakChance = 0.22f;
    public static final float RattleChance = 0.35f;
    public static final int Experience = 8;
    public static final byte ShieldBreakEvent = 30;
    public static final double[] Contacts = {0.0, 0.5};
    private static final EntityDataAccessor<Integer> VariantData = SynchedEntityData.defineId(Scp3008Staff.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ActivityData = SynchedEntityData.defineId(Scp3008Staff.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HostileData = SynchedEntityData.defineId(Scp3008Staff.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ProwlLoop = RawAnimation.begin().thenLoop("prowl");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation StareLoop = RawAnimation.begin().thenLoop("stare");
    private static final RawAnimation TidyLoop = RawAnimation.begin().thenLoop("tidy");
    private static final RawAnimation LurkLoop = RawAnimation.begin().thenLoop("lurk");
    private static final RawAnimation SwipeOnce = RawAnimation.begin().thenPlay("swipe");
    private static final RawAnimation AnnounceOnce = RawAnimation.begin().thenPlay("announce");
    private static final RawAnimation AlertOnce = RawAnimation.begin().thenPlay("alert");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    @Nullable
    private Scp3008StaffVariant Kind;
    private boolean Chosen;
    private boolean StoreBorn;
    private boolean Settled;
    private boolean WasHostile;
    private int AlertWait;
    private int AttackWait;
    private int Winding;
    @Nullable
    private LivingEntity WindTarget;
    private int Announcing;
    private int Provoked;
    @Nullable
    private UUID Provoker;
    private int Shove;
    private int StareTicks;
    @Nullable
    private UUID StareFocus;
    @Nullable
    private Boolean ClockOverride;
    private float RenderPartial;

    public Scp3008Staff(EntityType<? extends Scp3008Staff> Type, Level World) {
        super(Type, World);
        xpReward = Experience;
        Kind = Scp3008StaffVariant.byId(entityData.get(VariantData));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Scp3008StaffVariant.Lump.Health).add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.ATTACK_DAMAGE, Scp3008StaffVariant.Lump.Damage).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ARMOR, Scp3008StaffVariant.Lump.Armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, Scp3008StaffVariant.Lump.Knockback).add(Attributes.STEP_HEIGHT, StepHeight);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(VariantData, Scp3008StaffVariant.Lump.ordinal());
        Builder.define(ActivityData, ActivityIdle);
        Builder.define(HostileData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp3008StaffGoals.Announce(this));
        goalSelector.addGoal(2, new Scp3008StaffGoals.Strike(this));
        goalSelector.addGoal(3, new Scp3008StaffGoals.Stare(this));
        goalSelector.addGoal(4, new Scp3008StaffGoals.Tidy(this, false));
        goalSelector.addGoal(5, new Scp3008StaffGoals.Tidy(this, true));
        goalSelector.addGoal(6, new Scp3008StaffGoals.Wander(this));
        goalSelector.addGoal(7, new Scp3008StaffGoals.Calm<>(this, new LookAtPlayerGoal(this, Player.class, Scp3008StaffGoals.GlanceRange, Scp3008StaffGoals.GlanceChance)));
        goalSelector.addGoal(8, new Scp3008StaffGoals.Calm<>(this, new RandomLookAroundGoal(this)));
        targetSelector.addGoal(1, new Scp3008StaffGoals.Hunt(this));
        targetSelector.addGoal(2, new Scp3008StaffGoals.Retaliate(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        if (!Chosen) {
            setVariant(Scp3008StaffVariant.random(random));
        }
        setHealth(getMaxHealth());
        return Result;
    }

    public Scp3008StaffVariant variant() {
        return Kind != null ? Kind : Scp3008StaffVariant.byId(entityData.get(VariantData));
    }

    public void setVariant(Scp3008StaffVariant Picked) {
        Chosen = true;
        Kind = Picked;
        entityData.set(VariantData, Picked.ordinal());
        base(Attributes.MAX_HEALTH, Picked.Health);
        base(Attributes.ATTACK_DAMAGE, Picked.Damage);
        base(Attributes.ARMOR, Picked.Armor);
        base(Attributes.KNOCKBACK_RESISTANCE, Picked.Knockback);
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        refreshDimensions();
    }

    private void base(Holder<Attribute> Key, double Value) {
        AttributeInstance Found = getAttribute(Key);
        if (Found != null) {
            Found.setBaseValue(Value);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (VariantData.equals(Key)) {
            Kind = Scp3008StaffVariant.byId(entityData.get(VariantData));
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose Stance) {
        if (Kind == null) {
            return super.getDefaultDimensions(Stance);
        }
        return Kind.Size;
    }

    public int activity() {
        return entityData.get(ActivityData);
    }

    public void setActivity(int Activity) {
        if (activity() != Activity) {
            entityData.set(ActivityData, Activity);
        }
    }

    public boolean isHostile() {
        return entityData.get(HostileData);
    }

    public boolean night() {
        return ClockOverride != null ? ClockOverride : Scp3008Hours.night(level());
    }

    public boolean hostile() {
        return night() && level().getDifficulty() != Difficulty.PEACEFUL;
    }

    public void setClockOverride(@Nullable Boolean Night) {
        ClockOverride = Night;
    }

    public boolean storeBorn() {
        return StoreBorn;
    }

    public void setStoreBorn(boolean Value) {
        StoreBorn = Value;
    }

    @Nullable
    public UUID provoker() {
        return Provoked > 0 ? Provoker : null;
    }

    public boolean provokedBy(@Nullable LivingEntity Other) {
        return Other != null && Provoked > 0 && Other.getUUID().equals(Provoker);
    }

    public boolean engaged() {
        return hostile() || provokedBy(getTarget());
    }

    public double chaseModifier() {
        return hostile() ? variant().HuntModifier : ProvokedModifier;
    }

    public boolean announcing() {
        return Announcing > 0;
    }

    public int stareTicks() {
        return StareTicks;
    }

    @Nullable
    public LivingEntity stareFocus() {
        if (StareTicks <= 0 || StareFocus == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(StareFocus) instanceof LivingEntity Living ? Living : null;
    }

    public void stareAt(LivingEntity Focus, int Ticks) {
        StareFocus = Focus.getUUID();
        StareTicks = Ticks;
    }

    public void clearStare() {
        StareTicks = 0;
        StareFocus = null;
    }

    public void provoke(LivingEntity Attacker) {
        Provoker = Attacker.getUUID();
        Provoked = ProvokeTicks;
        setTarget(Attacker);
        if (level() instanceof ServerLevel World) {
            for (Scp3008Staff Other : World.getEntitiesOfClass(Scp3008Staff.class, getBoundingBox().inflate(WitnessRange), Each -> Each != this && Each.isAlive())) {
                Other.stareAt(Attacker, WitnessStare);
            }
        }
    }

    public void announce() {
        Announcing = AnnounceTicks;
        getNavigation().stop();
        triggerAnim("action", "announce");
    }

    public float voicePitch() {
        float Size = switch (variant()) {
            case Stout -> 1.12f;
            case Lanky, Stalk -> 0.86f;
            case Hunch -> 0.8f;
            default -> 0.95f;
        };
        return Size * (0.94f + random.nextFloat() * 0.12f);
    }

    public void play(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, Vec3 At, float Volume, float Pitch) {
        if (!(level() instanceof ServerLevel World) || isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        boolean Now = hostile();
        if (Now != isHostile()) {
            entityData.set(HostileData, Now);
        }
        if (!Settled) {
            Settled = true;
            WasHostile = Now;
        }
        if (Now && !WasHostile) {
            AlertWait = 1 + random.nextInt(AlertSpread);
            clearStare();
            setActivity(ActivityIdle);
        } else if (!Now && WasHostile) {
            calm();
        }
        WasHostile = Now;
        if (AlertWait > 0 && --AlertWait == 0) {
            triggerAnim("action", "alert");
            play(Scp3008StaffRegistry.AlertLayers, getEyePosition(), 1.0f, voicePitch());
        }
        if (AttackWait > 0) {
            AttackWait--;
        }
        if (Announcing > 0) {
            Announcing--;
        }
        if (Provoked > 0 && --Provoked == 0) {
            if (!Now && getTarget() != null && getTarget().getUUID().equals(Provoker)) {
                setTarget(null);
            }
            Provoker = null;
        }
        if (Shove > 0) {
            Shove--;
        }
        if (StareTicks > 0 && --StareTicks == 0) {
            StareFocus = null;
        }
        if (Winding > 0 && --Winding == 0) {
            land();
        }
    }

    private void calm() {
        LivingEntity Current = getTarget();
        if (Current != null && !provokedBy(Current)) {
            setTarget(null);
        }
        getNavigation().stop();
        setAggressive(false);
        Winding = 0;
        WindTarget = null;
    }

    public boolean canSwing() {
        return AttackWait <= 0 && Winding <= 0 && Announcing <= 0;
    }

    public boolean winding() {
        return Winding > 0;
    }

    public void swingAt(LivingEntity Target) {
        AttackWait = AttackCooldown;
        Winding = WindTicks;
        WindTarget = Target;
        triggerAnim("action", "swipe");
        play(Scp3008StaffRegistry.SwingLayers, position().add(0.0, getBbHeight() * 0.6, 0.0), 0.9f, voicePitch());
    }

    private void land() {
        LivingEntity Target = WindTarget;
        WindTarget = null;
        if (Target == null || !Target.isAlive() || !Scp3008StaffGoals.isPrey(Target) || !engaged() && !provokedBy(Target)) {
            return;
        }
        if (getAttackBoundingBox().inflate(WindLeeway, 0.0, WindLeeway).intersects(Target.getHitbox())) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(Target);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CullReach, CullRise, CullReach);
    }

    @Override
    protected AABB getAttackBoundingBox() {
        double Reach = BaseReach + variant().Reach;
        return getBoundingBox().inflate(Reach, 0.0, Reach);
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Blocking = Target instanceof Player User && User.isBlocking();
        boolean Hit = super.doHurtTarget(Target);
        if (level() instanceof ServerLevel World) {
            play(Scp3008StaffRegistry.HitLayers, Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0), 0.9f, 0.9f + random.nextFloat() * 0.15f);
            if (Blocking && variant().ShieldBreaker && Target instanceof Player User && random.nextFloat() < ShieldChance) {
                Item Held = User.getUseItem().getItem();
                User.getCooldowns().addCooldown(Held, ShieldCooldown);
                User.stopUsingItem();
                World.broadcastEntityEvent(User, ShieldBreakEvent);
            }
        }
        return Hit;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel && isAlive()) {
            Entity Attacker = Source.getEntity();
            if (Attacker instanceof LivingEntity Living && Living != this) {
                if (!hostile() && Scp3008StaffGoals.isPrey(Living)) {
                    provoke(Living);
                }
                if (variant().Thorns && Source.getDirectEntity() == Attacker && distanceToSqr(Attacker) < ThornReach * ThornReach && random.nextFloat() < ThornChance) {
                    Living.hurt(damageSources().thorns(this), ThornDamage);
                }
            }
        }
        return Hurt;
    }

    @Override
    public void push(Entity Other) {
        super.push(Other);
        if (level().isClientSide || hostile() || !(Other instanceof Player User) || !Scp3008StaffGoals.isPrey(User)) {
            return;
        }
        Shove += 2;
        if (Shove > ShoveLimit) {
            Shove = 0;
            stareAt(User, ShoveStare);
            getNavigation().stop();
            play(Scp3008StaffRegistry.HumLayers, getEyePosition(), 0.9f, voicePitch() * 0.8f);
        }
    }

    public int shove() {
        return Shove;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            stepSounds();
        }
    }

    private void stepSounds() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive(), variant().Gait);
        if (!Moving || isSilent()) {
            return;
        }
        boolean Running = Stride.stride() == StrideGait.Run;
        for (double Contact : Contacts) {
            if (Math.floor(Stride.to() - Contact) > Math.floor(Stride.from() - Contact)) {
                float Pitch = (Running ? 0.92f : 1.0f) * (0.94f + random.nextFloat() * 0.12f);
                if (Running) {
                    local(Scp3008StaffRegistry.Run.get(), RunVolume, Pitch);
                    local(Scp3008StaffRegistry.RunClatter.get(), RunVolume * 0.6f, Pitch);
                    if (random.nextFloat() < RattleChance) {
                        local(Scp3008StaffRegistry.RunRattle.get(), RunVolume * 0.4f, Pitch);
                    }
                } else {
                    local(Scp3008StaffRegistry.Step.get(), StepVolume, Pitch);
                    local(Scp3008StaffRegistry.StepShuffle.get(), StepVolume * 0.9f, Pitch);
                    if (random.nextFloat() < SqueakChance) {
                        local(Scp3008StaffRegistry.StepSqueak.get(), StepVolume * 0.6f, Pitch);
                    }
                }
            }
        }
    }

    private void local(SoundEvent Sound, float Volume, float Pitch) {
        level().playLocalSound(getX(), getY(), getZ(), Sound, SoundSource.HOSTILE, Volume, Pitch, false);
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isHostile() ? null : Scp3008StaffRegistry.Hum.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isHostile()) {
            return;
        }
        play(Scp3008StaffRegistry.HumLayers, getEyePosition(), 0.7f, voicePitch());
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp3008StaffRegistry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        super.playHurtSound(Source);
        play(List.of(Scp3008StaffRegistry.HurtCloth), position(), 0.7f, voicePitch());
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp3008StaffRegistry.Death.get();
    }

    @Override
    public float getVoicePitch() {
        return voicePitch();
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        play(List.of(Scp3008StaffRegistry.DeathSigh), getEyePosition(), 0.8f, voicePitch());
    }

    @Override
    public void checkDespawn() {
        if (!StoreBorn) {
            return;
        }
        Player Nearest = level().getNearestPlayer(this, -1.0);
        if (Nearest == null || Nearest.distanceToSqr(this) > Scp3008StaffSpawner.DespawnDistance * Scp3008StaffSpawner.DespawnDistance) {
            discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return !StoreBorn && super.shouldBeSaved();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Variant", variant().ordinal());
        Tag.putBoolean("StoreBorn", StoreBorn);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Variant")) {
            float Health = getHealth();
            setVariant(Scp3008StaffVariant.byId(Tag.getInt("Variant")));
            setHealth(Math.min(Health, getMaxHealth()));
        }
        StoreBorn = Tag.getBoolean("StoreBorn");
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean striding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public RawAnimation animation() {
        if (striding()) {
            if (Stride.stride() == StrideGait.Run) {
                return RunLoop;
            }
            return isHostile() ? ProwlLoop : WalkLoop;
        }
        if (isHostile()) {
            return LurkLoop;
        }
        return switch (activity()) {
            case ActivityTidy -> TidyLoop;
            case ActivityStare -> StareLoop;
            default -> IdleLoop;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp3008StaffGaitController(this, "body", Scp3008StaffGaitController.Blend, Event -> Event.setAndContinue(animation())));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("swipe", SwipeOnce).triggerableAnim("announce", AnnounceOnce)
                .triggerableAnim("alert", AlertOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
