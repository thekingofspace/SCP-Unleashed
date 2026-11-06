package net.rai.scp_unleashed.z0004;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Z0004Pirate extends Monster implements GeoEntity {
    public static final String VariantKey = "Variant";
    public static final int Cutlass = 0;
    public static final int Hook = 1;
    public static final int Pistol = 2;
    public static final int Blunderbuss = 3;
    public static final int Variants = 4;
    public static final String[] VariantNames = {"cutlass", "hook", "pistol", "blunderbuss"};
    public static final double[] VariantHealth = {28.0, 32.0, 22.0, 24.0};
    public static final double[] VariantDamage = {5.0, 6.0, 3.0, 3.0};
    public static final double[] VariantArmor = {2.0, 3.0, 1.0, 2.0};
    public static final float Width = 0.6f;
    public static final float Height = 1.9f;
    public static final float EyeHeight = 1.7f;
    public static final double Speed = 0.25;
    public static final double FollowRange = 32.0;
    public static final float SwimShare = 0.12f;
    public static final float WaterDrag = 0.9f;
    public static final double CrewWalkDistance = 0.4536;
    public static final double CrewRunDistance = 1.25;
    public static final double CaptainWalkDistance = 0.5081;
    public static final double CaptainRunDistance = 1.4;
    public static final StrideGait.Shape CrewGait = new StrideGait.Shape(0.004, 0.13, 0.11, CrewWalkDistance, CrewRunDistance, 0.6, Z0004PirateController.Blend);
    public static final StrideGait.Shape CaptainGait = new StrideGait.Shape(0.004, 0.14, 0.12, CaptainWalkDistance, CaptainRunDistance, 0.6, Z0004PirateController.Blend);
    public static final int SwimEnter = 4;
    public static final int SwimLeave = 6;
    public static final int ChestCheck = 40;
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    public static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    public static final RawAnimation SlashOnce = RawAnimation.begin().thenPlay("slash");
    public static final RawAnimation ShootOnce = RawAnimation.begin().thenPlay("shoot");
    public static final RawAnimation CommandOnce = RawAnimation.begin().thenPlay("command");
    private static final EntityDataAccessor<Integer> VariantData = SynchedEntityData.defineId(Z0004Pirate.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    @Nullable
    private BlockPos Home;
    @Nullable
    private UUID FightId;
    private int ShotsFired;
    private float RenderPartial;
    private int WetFor;
    private int DryFor;
    private boolean SwimShown;

    public Z0004Pirate(EntityType<? extends Z0004Pirate> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.WATER, 0.0f);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0f);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 10, SwimShare, 1.0f, false);
        lookControl = new SmoothSwimmingLookControl(this, 10);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, VariantHealth[0]).add(Attributes.ATTACK_DAMAGE, VariantDamage[0]).add(Attributes.ARMOR, VariantArmor[0])
                .add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(VariantData, Cutlass);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Z0004PirateGoals.Fight(this));
        goalSelector.addGoal(5, new Z0004PirateGoals.Home(this));
        goalSelector.addGoal(6, new Z0004PirateGoals.Wander(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Z0004Pirate.class, Z0004Ship.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, Z0004Fight::targetable));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                Other -> !(Other instanceof Player) && Z0004Fight.targetable(Other)));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new AmphibiousPathNavigation(this, World);
    }

    public int variant() {
        return Mth.clamp(entityData.get(VariantData), 0, Variants - 1);
    }

    public boolean captain() {
        return false;
    }

    public boolean melee() {
        int Kind = variant();
        return captain() || Kind == Cutlass || Kind == Hook;
    }

    public boolean gunner() {
        int Kind = variant();
        return captain() || Kind == Pistol || Kind == Blunderbuss;
    }

    public void setVariant(int Value) {
        int Kind = Mth.clamp(Value, 0, Variants - 1);
        entityData.set(VariantData, Kind);
        if (!captain()) {
            base(Attributes.MAX_HEALTH, VariantHealth[Kind]);
            base(Attributes.ATTACK_DAMAGE, VariantDamage[Kind]);
            base(Attributes.ARMOR, VariantArmor[Kind]);
            setHealth(getMaxHealth());
        }
    }

    protected void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> Attribute, double Value) {
        AttributeInstance Instance = getAttribute(Attribute);
        if (Instance != null) {
            Instance.setBaseValue(Value);
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Data);
        if (Reason != MobSpawnType.SPAWN_EGG) {
            setVariant(variant());
        }
        return Result;
    }

    public void enlist(@Nullable BlockPos Chest, @Nullable UUID Fight) {
        Home = Chest == null ? null : Chest.immutable();
        FightId = Fight;
        if (Fight != null) {
            setPersistenceRequired();
        }
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    @Nullable
    public UUID fightId() {
        return FightId;
    }

    public int shotsFired() {
        return ShotsFired;
    }

    public void countShot() {
        ShotsFired++;
    }

    public void retreat() {
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 0.8, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
            World.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1.0, getZ(), 20, 0.5, 0.4, 0.5, 0.1);
            World.playSound(null, getX(), getY(), getZ(), Z0004Registry.Dive.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && onGround() && !isInWater(), captain() ? CaptainGait : CrewGait);
            if (isInWater()) {
                WetFor++;
                DryFor = 0;
            } else {
                DryFor++;
                WetFor = 0;
            }
            if (!SwimShown && WetFor >= SwimEnter && !onGround()) {
                SwimShown = true;
            } else if (SwimShown && (DryFor >= SwimLeave || onGround() && !isUnderWater())) {
                SwimShown = false;
            }
            if (isUnderWater() && random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.BUBBLE, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0.0, 0.05, 0.0);
            }
        } else if (FightId != null && tickCount % ChestCheck == 0 && level() instanceof ServerLevel World && Home != null && World.isLoaded(Home)) {
            Z0004ChestBlockEntity Chest = Z0004Fight.chestAt(World, Home);
            if (Chest == null || !Chest.tracks(FightId)) {
                retreat();
            }
        }
    }

    @Override
    public void travel(Vec3 Input) {
        if (isControlledByLocalInstance() && isInWater()) {
            Vec3 Push = Input.lengthSqr() > 1.0e-6 ? Input.normalize() : Vec3.ZERO;
            moveRelative(getSpeed(), Push);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(WaterDrag));
        } else {
            super.travel(Input);
        }
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean swimShown() {
        return SwimShown;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public RawAnimation bodyAnimation() {
        if (SwimShown) {
            return SwimLoop;
        }
        if (Stride.stride() == StrideGait.Run) {
            return RunLoop;
        }
        if (Stride.stride() == StrideGait.Walk) {
            return WalkLoop;
        }
        return IdleLoop;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return FightId == null && super.removeWhenFarAway(Distance);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return FightId != null || super.requiresCustomPersistence();
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return Z0004Fight.targetable(Target) && super.canAttack(Target);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Z0004Fight.fleet(Source.getEntity()) || Z0004Fight.fleet(Source.getDirectEntity()) || Source.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        if (Source.getEntity() instanceof Player User && FightId != null && Home != null && level() instanceof ServerLevel World) {
            Z0004ChestBlockEntity Chest = Z0004Fight.chestAt(World, Home);
            if (Chest != null && Chest.tracks(FightId)) {
                Chest.join(User.getUUID());
            }
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        boolean First = !dead;
        super.die(Source);
        if (First && FightId != null && Home != null && level() instanceof ServerLevel World) {
            Z0004ChestBlockEntity Chest = Z0004Fight.chestAt(World, Home);
            if (Chest != null) {
                Chest.crewDown(getUUID());
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            playSound(Z0004Registry.Swing.get(), 0.9f, 0.9f + random.nextFloat() * 0.2f);
        }
        return Hit;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Z0004Registry.Rattle.get();
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        playSound(Z0004Registry.RattleCoral.get(), 0.6f, getVoicePitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Z0004Registry.CrewHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Z0004Registry.CrewDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
        playSound(Z0004Registry.CrewStep.get(), 0.4f, 1.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt(VariantKey, variant());
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
        if (FightId != null) {
            Tag.putUUID("Fight", FightId);
        }
        Tag.putInt("Shots", ShotsFired);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains(VariantKey)) {
            float Health = getHealth();
            setVariant(Tag.getInt(VariantKey));
            if (Tag.contains("Health")) {
                setHealth(Math.min(Health, getMaxHealth()));
            }
        }
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        FightId = Tag.hasUUID("Fight") ? Tag.getUUID("Fight") : null;
        ShotsFired = Tag.getInt("Shots");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Z0004PirateController(this));
        Controllers.add(new AnimationController<>(this, "action", 2, State -> PlayState.STOP).triggerableAnim("slash", SlashOnce).triggerableAnim("shoot", ShootOnce)
                .triggerableAnim("command", CommandOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
