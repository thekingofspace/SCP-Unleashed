package net.rai.scp_unleashed.scp553;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Scp553Butterfly extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.45f;
    public static final float Height = 0.35f;
    public static final float EyeHeight = 0.2f;
    public static final double Health = 4.0;
    public static final double Cruise = 0.22;
    public static final double FollowRange = 24.0;
    public static final float WingDamage = 2.0f;
    public static final double DriftModifier = 0.45;
    public static final double OrbitModifier = 1.1;
    public static final double DiveModifier = 2.6;
    public static final double RecoverModifier = 1.4;
    public static final double ScatterModifier = 1.6;
    public static final double Drag = 0.9;
    public static final double Jitter = 0.018;
    public static final double GlideSink = 0.006;
    public static final double HoverLift = 0.012;
    public static final double Clearance = 0.6;
    public static final double CeilingDrift = 5.0;
    public static final int GlideMin = 12;
    public static final int GlideMax = 30;
    public static final int FlapMin = 24;
    public static final int FlapMax = 60;
    public static final int CensusInterval = 20;
    public static final double RallyRadius = 16.0;
    public static final int BreedCooldown = 12000;
    public static final int FirstBreedMin = 2400;
    public static final int FirstBreedMax = 6000;
    public static final int RetryCooldown = 400;
    public static final int HomeRange = 20;
    public static final int SpookTicks = 50;
    public static final int SpitTicks = 14;
    public static final int SpitLaunch = 7;
    public static final int FlutterSoundMin = 40;
    public static final int FlutterSoundMax = 90;
    public static final byte ActionNone = 0;
    public static final byte ActionSpit = 1;
    public static final byte ActionDive = 2;
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp553Butterfly.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp553Butterfly.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AgitatedData = SynchedEntityData.defineId(Scp553Butterfly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PerchedData = SynchedEntityData.defineId(Scp553Butterfly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GlideData = SynchedEntityData.defineId(Scp553Butterfly.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation FlutterLoop = RawAnimation.begin().thenLoop("flutter");
    private static final RawAnimation GlideLoop = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation PerchLoop = RawAnimation.begin().thenLoop("perch");
    private static final RawAnimation DiveLoop = RawAnimation.begin().thenLoop("dive");
    private static final RawAnimation SpitPlay = RawAnimation.begin().thenPlayAndHold("spit");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos Home;
    private int Cooldown = FirstBreedMin;
    private int Population = 1;
    private int ActionTicks;
    private int GlideClock = FlapMin;
    private int Spooked;
    private int SoundClock = FlutterSoundMin;
    private boolean Landing;
    @Nullable
    private Vec3 SpookedFrom;
    private float RenderPartial;
    private int TickOrigin;

    public Scp553Butterfly(EntityType<? extends Scp553Butterfly> Type, Level World) {
        super(Type, World);
        moveControl = new Scp553Flight(this);
        setNoGravity(true);
        xpReward = 1;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Cruise).add(Attributes.FLYING_SPEED, Cruise)
                .add(Attributes.ATTACK_DAMAGE, WingDamage).add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(AgitatedData, false);
        Builder.define(PerchedData, false);
        Builder.define(GlideData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp553Goals.Swarm(this));
        goalSelector.addGoal(2, new Scp553Goals.Scatter(this));
        goalSelector.addGoal(3, new Scp553Goals.Breed(this));
        goalSelector.addGoal(4, new Scp553Goals.Perch(this));
        goalSelector.addGoal(5, new Scp553Goals.Drift(this));
        targetSelector.addGoal(1, new Scp553Goals.Seek(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        FlyingPathNavigation Navigation = new FlyingPathNavigation(this, World);
        Navigation.setCanOpenDoors(false);
        Navigation.setCanFloat(true);
        Navigation.setCanPassDoors(true);
        return Navigation;
    }

    public static boolean isQuarry(LivingEntity Found) {
        if (Found instanceof Scp553Butterfly || !Found.isAlive() || Found.isSpectator() || !Targeting.isTargetable(Found)) {
            return false;
        }
        if (Found instanceof Player User) {
            return !User.isCreative() && User.level().getDifficulty() != Difficulty.PEACEFUL;
        }
        return ControlBodies.isPersonLike(Found);
    }

    public double cruise() {
        return getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public void travel(Vec3 Input) {
        if (isControlledByLocalInstance()) {
            if (isInWater() || isInLava()) {
                moveRelative(0.02f, Input);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.8).add(0.0, 0.03, 0.0));
            } else {
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(Drag));
            }
        }
        calculateEntityAnimation(false);
    }

    public void flutter(boolean Moving) {
        if (!isAlive() || isPerched() || level().isClientSide) {
            return;
        }
        RandomSource Random = getRandom();
        Vec3 Motion = getDeltaMovement();
        boolean Gliding = isGliding();
        if (!isDiving()) {
            double Shake = Gliding ? Jitter * 0.3 : Jitter;
            Motion = Motion.add((Random.nextDouble() - 0.5) * Shake, (Random.nextDouble() - 0.5) * Shake, (Random.nextDouble() - 0.5) * Shake);
            if (Gliding) {
                Motion = Motion.add(0.0, -GlideSink, 0.0);
            }
            double Ground = groundGap();
            if (Ground < Clearance && !landing()) {
                Motion = Motion.add(0.0, HoverLift * 2.0, 0.0);
            } else if (!Moving && getTarget() == null && Ground > CeilingDrift) {
                Motion = Motion.add(0.0, -HoverLift * 0.5, 0.0);
            }
        }
        setDeltaMovement(Motion);
    }

    public boolean landing() {
        return Landing;
    }

    public void setLanding(boolean Value) {
        Landing = Value;
    }

    public double groundGap() {
        BlockPos Pos = blockPosition();
        for (int Depth = 0; Depth <= 6; Depth++) {
            BlockPos Below = Pos.below(Depth);
            BlockState State = level().getBlockState(Below);
            if (!State.getFluidState().isEmpty()) {
                return getY() - Below.getY() - State.getFluidState().getHeight(level(), Below);
            }
            if (!State.getCollisionShape(level(), Below).isEmpty()) {
                return getY() - (Below.getY() + State.getCollisionShape(level(), Below).max(Direction.Axis.Y));
            }
        }
        return 7.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (ActionTicks > 0) {
            ActionTicks--;
            if (ActionTicks <= 0 && action() == ActionSpit) {
                entityData.set(ActionData, ActionNone);
            }
        }
        if (Cooldown > 0) {
            Cooldown--;
        }
        if (Spooked > 0) {
            Spooked--;
        }
        if ((tickCount + getId()) % CensusInterval == 0) {
            census(World);
        }
        if (isPerched()) {
            setDeltaMovement(Vec3.ZERO);
            setGliding(false);
        } else {
            glideTick();
        }
        if (--SoundClock <= 0) {
            SoundClock = FlutterSoundMin + getRandom().nextInt(FlutterSoundMax - FlutterSoundMin + 1);
            if (!isPerched()) {
                playLayers(Scp553Registry.FlutterLayers, 0.25f, 1.0f + getRandom().nextFloat() * 0.15f);
            }
        }
    }

    private void glideTick() {
        if (--GlideClock > 0) {
            return;
        }
        boolean Calm = !isAgitated() && getTarget() == null && action() == ActionNone;
        if (isGliding() || !Calm || groundGap() < Clearance * 2.0) {
            setGliding(false);
            GlideClock = FlapMin + getRandom().nextInt(FlapMax - FlapMin + 1);
        } else {
            setGliding(true);
            GlideClock = GlideMin + getRandom().nextInt(GlideMax - GlideMin + 1);
        }
    }

    public void census(ServerLevel World) {
        Population = Scp553Colony.population(World, position());
        boolean Swarming = Scp553Colony.swarming(Population);
        boolean Was = isAgitated();
        if (Swarming == Was) {
            return;
        }
        entityData.set(AgitatedData, Swarming);
        if (Swarming) {
            setPerched(false);
            setGliding(false);
            if (getId() % 3 == 0) {
                playLayers(Scp553Registry.SwarmLayers, 0.7f, 0.9f + getRandom().nextFloat() * 0.2f);
            }
        } else {
            setTarget(null);
            if (action() == ActionDive) {
                clearAction();
            }
        }
    }

    public int population() {
        return Population;
    }

    public boolean isAgitated() {
        return entityData.get(AgitatedData);
    }

    public boolean isPerched() {
        return entityData.get(PerchedData);
    }

    public void setPerched(boolean Value) {
        if (entityData.get(PerchedData) != Value) {
            entityData.set(PerchedData, Value);
        }
    }

    public boolean isGliding() {
        return entityData.get(GlideData);
    }

    public void setGliding(boolean Value) {
        if (entityData.get(GlideData) != Value) {
            entityData.set(GlideData, Value);
        }
    }

    public void rally(LivingEntity Target) {
        if (!isAgitated()) {
            return;
        }
        for (Scp553Butterfly Other : level().getEntitiesOfClass(Scp553Butterfly.class, getBoundingBox().inflate(RallyRadius), Found -> Found != this && Found.isAlive())) {
            if (Other.isAgitated() && Other.getTarget() == null && isQuarry(Target)) {
                Other.setTarget(Target);
            }
        }
    }

    public boolean facesQuarry() {
        LivingEntity Target = getTarget();
        return Target != null && Target.isAlive() && isAgitated() && distanceToSqr(Target) < 16.0;
    }

    public boolean touching(LivingEntity Target) {
        return getBoundingBox().inflate(0.25).intersects(Target.getBoundingBox());
    }

    public boolean strike(LivingEntity Target) {
        if (!(level() instanceof ServerLevel World) || !isQuarry(Target)) {
            return false;
        }
        boolean Landed = Target.hurt(damageSources().source(Scp553Registry.WingDamage, this), WingDamage);
        Vec3 Contact = Target.getBoundingBox().getCenter().add(position().subtract(Target.position()).normalize().scale(Target.getBbWidth() * 0.5));
        World.sendParticles(Scp553Registry.Glint.get(), Contact.x, Contact.y, Contact.z, Landed ? 6 : 2, 0.1, 0.1, 0.1, 0.03);
        playLayers(Scp553Registry.SliceLayers, Landed ? 0.8f : 0.4f, 1.1f + getRandom().nextFloat() * 0.2f);
        if (Landed) {
            setLastHurtMob(Target);
        }
        return Landed;
    }

    public int breedCooldown() {
        return Cooldown;
    }

    public void setBreedCooldown(int Ticks) {
        Cooldown = Math.max(0, Ticks);
    }

    public boolean readyToBreed() {
        return Cooldown <= 0 && getTarget() == null && !isDiving();
    }

    public void bred(boolean Seeded) {
        Cooldown = Seeded ? BreedCooldown : RetryCooldown;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setHome(BlockPos Pos) {
        Home = Pos.immutable();
    }

    public void newborn(BlockPos Nest) {
        Home = Nest.immutable();
        Cooldown = BreedCooldown;
    }

    public void arrived(BlockPos Cave, RandomSource Random) {
        Home = Cave.immutable();
        Cooldown = FirstBreedMin + Random.nextInt(FirstBreedMax - FirstBreedMin + 1);
    }

    public boolean spooked() {
        return Spooked > 0;
    }

    @Nullable
    public Vec3 spookedFrom() {
        return SpookedFrom;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isDiving() {
        return action() == ActionDive;
    }

    public boolean isSpitting() {
        return action() == ActionSpit;
    }

    @Nullable
    public String actionClip() {
        return action() == ActionSpit ? "spit" : null;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
    }

    public void clearAction() {
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    public int actionAge() {
        return Math.max(0, tickCount - entityData.get(ActionStartData));
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (!Hurt || !isAlive() || !(level() instanceof ServerLevel World)) {
            return Hurt;
        }
        setPerched(false);
        World.sendParticles(Scp553Registry.Glint.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 4, 0.12, 0.12, 0.12, 0.03);
        Entity Attacker = Source.getEntity();
        if (isAgitated() && Attacker instanceof LivingEntity Living && isQuarry(Living)) {
            setTarget(Living);
            rally(Living);
        } else if (!isAgitated()) {
            Spooked = SpookTicks;
            SpookedFrom = Attacker != null ? Attacker.position() : Source.getSourcePosition();
        }
        return true;
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp553Registry.Glint.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 14, 0.2, 0.2, 0.2, 0.06);
            playSound(Scp553Registry.DeathShatter.get(), 0.8f, 1.1f + getRandom().nextFloat() * 0.2f);
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        super.remove(Reason);
        if (level() instanceof ServerLevel World && (Reason.shouldDestroy() || Reason == RemovalReason.CHANGED_DIMENSION)) {
            Scp553World.get(World.getServer()).leave(getUUID());
        }
    }

    public void enlist(ServerLevel World) {
        Scp553World.get(World.getServer()).join(getUUID());
        if (Home == null) {
            Home = blockPosition();
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        if (Home == null) {
            Home = blockPosition();
        }
        if (Reason != MobSpawnType.BREEDING) {
            Cooldown = FirstBreedMin + getRandom().nextInt(FirstBreedMax - FirstBreedMin + 1);
        }
        return Result;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Dy, boolean Grounded, BlockState State, BlockPos Pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isPerched();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public void playAmbientSound() {
        playLayers(Scp553Registry.CallLayers, isAgitated() ? 0.7f : 0.45f, (isAgitated() ? 1.15f : 1.0f) + getRandom().nextFloat() * 0.1f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp553Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp553Registry.Death.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }

    @Override
    public SoundSource getSoundSource() {
        return isAgitated() ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
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
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.6);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Home != null) {
            Saved.put("Home", NbtUtils.writeBlockPos(Home));
        }
        Saved.putInt("BreedCooldown", Cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Home = NbtUtils.readBlockPos(Saved, "Home").orElse(null);
        Cooldown = Saved.contains("BreedCooldown") ? Saved.getInt("BreedCooldown") : FirstBreedMin;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp553WingController(this, "wings", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp553WingController.blendFor(Current));
            if (Current == ActionSpit) {
                return Event.setAndContinue(SpitPlay);
            }
            if (Current == ActionDive) {
                return Event.setAndContinue(DiveLoop);
            }
            if (isPerched()) {
                return Event.setAndContinue(PerchLoop);
            }
            return Event.setAndContinue(isGliding() ? GlideLoop : FlutterLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
