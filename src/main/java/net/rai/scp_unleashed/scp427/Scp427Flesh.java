package net.rai.scp_unleashed.scp427;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.control.ControlRefusing;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Scp427Flesh extends Monster implements GeoEntity, IEntityWithComplexSpawn, ControlRefusing {
    public static final float Width = 1.4f;
    public static final float Height = 2.5f;
    public static final float EyeHeight = 2.0f;
    public static final EntityDimensions CrawlSize = EntityDimensions.scalable(1.4f, 1.9f).withEyeHeight(1.45f);
    public static final double MaxHealth = 240.0;
    public static final double Armour = 10.0;
    public static final double Toughness = 4.0;
    public static final double SlamDamage = 20.0;
    public static final double MoveSpeed = 0.4;
    public static final double Steadiness = 0.9;
    public static final double HuntRange = 48.0;
    public static final double SenseRange = 6.0;
    public static final double WalkSpeed = 0.6;
    public static final double RunSpeed = 1.75;
    public static final double CrawlSpeed = 0.55;
    public static final double RunAbove = 5.0;
    public static final double LungeMin = 4.5;
    public static final double LungeMax = 11.0;
    public static final int LungeTicks = 24;
    public static final int LungeLaunch = 6;
    public static final int LungeCooldown = 80;
    public static final int LungeChance = 4;
    public static final float LungeDamage = 18.0f;
    public static final double LungeForward = 1.15;
    public static final double LungeLift = 0.42;
    public static final double SlamReach = 3.4;
    public static final int SlamTicks = 24;
    public static final int SlamImpact = 11;
    public static final int SlamCooldown = 16;
    public static final float SplashDamage = 10.0f;
    public static final double SplashRadius = 2.6;
    public static final int RoarTicks = 40;
    public static final int RoarCooldown = 600;
    public static final int RegenInterval = 30;
    public static final float RegenAmount = 1.0f;
    public static final int RetargetInterval = 10;
    public static final int LostLimit = 160;
    public static final int RepathInterval = 6;
    public static final int DeathTicks = 40;
    public static final int StandDelay = 12;
    public static final byte ActionNone = 0;
    public static final byte ActionLunge = 1;
    public static final byte ActionSlam = 2;
    public static final byte ActionRoar = 3;

    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp427Flesh.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp427Flesh.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp427Flesh.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation LungePlay = RawAnimation.begin().thenPlayAndHold("lunge");
    private static final RawAnimation SlamPlay = RawAnimation.begin().thenPlayAndHold("slam");
    private static final RawAnimation RoarPlay = RawAnimation.begin().thenPlayAndHold("roar");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation HurtPlay = RawAnimation.begin().thenPlay("hurt");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private Scp427Gait Gait;
    @Nullable
    private Scp427Hair Hair;
    private int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    private int StrikeTimer;
    private int LungeTimer = LungeCooldown / 2;
    private int RoarTimer;
    private boolean LungeHit;
    private int RetargetTimer;
    private int RepathTimer;
    private int LowTicks;
    private int Lost;
    private Vec3 LastSeen = Vec3.ZERO;
    private boolean Emerging;

    public Scp427Flesh(EntityType<? extends Scp427Flesh> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed).add(Attributes.ATTACK_DAMAGE, SlamDamage).add(Attributes.FOLLOW_RANGE, HuntRange)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness).add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.SAFE_FALL_DISTANCE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(HuntingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp427Navigation(this, World);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setCanPickUpLoot(false);
        return Group;
    }

    public void emerge() {
        Emerging = true;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean busy() {
        return action() != ActionNone;
    }

    public boolean isHunting() {
        return entityData.get(HuntingData);
    }

    public boolean isCrawling() {
        return getPose() == Pose.CROUCHING;
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionLunge -> "lunge";
            case ActionSlam -> "slam";
            case ActionRoar -> "roar";
            default -> null;
        };
    }

    public String stateKey() {
        if (isDeadOrDying()) {
            return "collapsing";
        }
        return switch (action()) {
            case ActionLunge -> "lunging";
            case ActionSlam -> "slamming";
            case ActionRoar -> "roaring";
            default -> isHunting() ? "hunting" : "prowling";
        };
    }

    private void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp427Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            hair().tick(this);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (StrikeTimer > 0) {
            StrikeTimer--;
        }
        if (LungeTimer > 0) {
            LungeTimer--;
        }
        if (RoarTimer > 0) {
            RoarTimer--;
        }
        if (tickCount % RegenInterval == 0 && getHealth() < getMaxHealth()) {
            heal(RegenAmount);
        }
        posture(World);
        if (Emerging) {
            Emerging = false;
            roar(World);
        }
        if (ActionTicks > 0) {
            tickAction(World);
            return;
        }
        if (--RetargetTimer <= 0) {
            RetargetTimer = RetargetInterval;
            retarget(World);
        }
        LivingEntity Target = getTarget();
        boolean Hunting = Target != null;
        if (entityData.get(HuntingData) != Hunting) {
            entityData.set(HuntingData, Hunting);
        }
        setAggressive(Hunting);
        if (Target == null) {
            prowl();
        } else {
            hunt(World, Target);
        }
    }

    private void tickAction(ServerLevel World) {
        byte Current = action();
        int Elapsed = (int) actionElapsed();
        LivingEntity Target = getTarget();
        if (Current != ActionLunge || Elapsed < LungeLaunch) {
            getNavigation().stop();
        }
        if (Target != null) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
        }
        if (Current == ActionLunge) {
            if (Elapsed < LungeLaunch) {
                setDeltaMovement(getDeltaMovement().multiply(0.3, 1.0, 0.3));
                if (Target != null) {
                    faceToward(Target);
                }
            } else if (Elapsed == LungeLaunch) {
                launch(World, Target);
            } else if (!LungeHit && Target != null && Target.isAlive() && getBoundingBox().inflate(0.6).intersects(Target.getBoundingBox())) {
                LungeHit = true;
                Target.hurt(damageSources().source(Scp427Registry.Maul, this), LungeDamage);
                Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (Push.lengthSqr() > 1.0E-4) {
                    Push = Push.normalize();
                    Target.knockback(0.8, -Push.x, -Push.z);
                }
            }
        } else if (Current == ActionSlam) {
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            if (Elapsed == SlamImpact) {
                impact(World);
            }
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
        }
        if (--ActionTicks > 0) {
            return;
        }
        setAction(ActionNone, 0);
    }

    private void faceToward(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    private void launch(ServerLevel World, @Nullable LivingEntity Target) {
        Vec3 Toward = Target != null ? Target.position().subtract(position()).multiply(1.0, 0.0, 1.0) : Vec3.directionFromRotation(0.0f, getYRot());
        if (Toward.lengthSqr() < 1.0E-4) {
            Toward = Vec3.directionFromRotation(0.0f, getYRot());
        }
        Toward = Toward.normalize();
        double Reach = Target != null ? Math.min(1.0, distanceTo(Target) / LungeMax + 0.35) : 0.7;
        setDeltaMovement(Toward.x * LungeForward * Reach, LungeLift, Toward.z * LungeForward * Reach);
        hasImpulse = true;
        Scp427Registry.play(World, getEyePosition(), Scp427Registry.LungeLayers, SoundSource.HOSTILE, 1.4f, 1.0f);
    }

    private void impact(ServerLevel World) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Point = position().add(Facing.scale(1.6));
        Scp427Registry.play(World, Point, Scp427Registry.ImpactLayers, SoundSource.HOSTILE, 1.5f, 1.0f);
        BlockState Below = World.getBlockState(BlockPos.containing(Point).below());
        if (!Below.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Below), Point.x, Point.y + 0.1, Point.z, 30, 1.0, 0.1, 1.0, 0.15);
        }
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive() && distanceTo(Target) <= SlamReach + 0.8) {
            Target.hurt(damageSources().source(Scp427Registry.Maul, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            Target.push(0.0, 0.45, 0.0);
            Target.hurtMarked = true;
        }
        List<LivingEntity> Near = World.getEntitiesOfClass(LivingEntity.class, new AABB(Point, Point).inflate(SplashRadius, 1.5, SplashRadius),
                Other -> Other != this && Other != Target && Scp427Prey.prey(this, Other));
        for (LivingEntity Other : Near) {
            Other.hurt(damageSources().source(Scp427Registry.Maul, this), SplashDamage);
            Other.push(0.0, 0.3, 0.0);
            Other.hurtMarked = true;
        }
    }

    private void roar(ServerLevel World) {
        setAction(ActionRoar, RoarTicks);
        RoarTimer = RoarCooldown;
        Scp427Registry.play(World, getEyePosition(), Scp427Registry.RoarLayers, SoundSource.HOSTILE, 2.4f, 1.0f);
    }

    private void retarget(ServerLevel World) {
        LivingEntity Current = getTarget();
        LivingEntity Best = Scp427Prey.closest(World, this, HuntRange, SenseRange);
        if (Current != null && Scp427Prey.prey(this, Current) && distanceTo(Current) <= HuntRange) {
            boolean Sees = hasLineOfSight(Current);
            if (Sees) {
                LastSeen = Current.position();
                Lost = 0;
            } else {
                Lost += RetargetInterval;
            }
            boolean Switch = Best != null && Best != Current && (!Sees || Scp427Prey.clearlyCloser(this, Current, Best));
            if (!Switch && Lost < LostLimit) {
                return;
            }
        }
        Lost = 0;
        if (Best != Current) {
            setTarget(Best);
            RepathTimer = 0;
            if (Best != null) {
                LastSeen = Best.position();
                if (Current == null && RoarTimer <= 0) {
                    roar(World);
                }
            }
        }
    }

    private void hunt(ServerLevel World, LivingEntity Target) {
        double Distance = distanceTo(Target);
        boolean Sees = hasLineOfSight(Target);
        if (Sees) {
            LastSeen = Target.position();
        }
        getLookControl().setLookAt(Target, 30.0f, 30.0f);
        if (Distance <= SlamReach && Sees && StrikeTimer <= 0) {
            StrikeTimer = SlamCooldown + SlamTicks;
            setAction(ActionSlam, SlamTicks);
            Scp427Registry.play(World, getEyePosition(), List.of(Scp427Registry.Slam), SoundSource.HOSTILE, 1.4f, 1.0f);
            return;
        }
        if (Distance >= LungeMin && Distance <= LungeMax && Sees && onGround() && !isCrawling() && LungeTimer <= 0 && random.nextInt(LungeChance) == 0) {
            LungeTimer = LungeCooldown;
            LungeHit = false;
            faceToward(Target);
            setAction(ActionLunge, LungeTicks);
            return;
        }
        if (--RepathTimer > 0 && !getNavigation().isDone()) {
            return;
        }
        RepathTimer = RepathInterval;
        double Speed = isCrawling() ? CrawlSpeed : Distance > RunAbove ? RunSpeed : WalkSpeed;
        Vec3 Goal = Sees ? Target.position() : LastSeen;
        if (Distance > 1.8 || !Sees) {
            if (!getNavigation().moveTo(Goal.x, Goal.y, Goal.z, Speed)) {
                getNavigation().stop();
            }
        }
    }

    private void prowl() {
        if (getNavigation().isDone() && random.nextInt(60) == 0) {
            Vec3 Spot = position().add((random.nextDouble() - 0.5) * 18.0, 0.0, (random.nextDouble() - 0.5) * 18.0);
            getNavigation().moveTo(Spot.x, getY(), Spot.z, isCrawling() ? CrawlSpeed : WalkSpeed);
        }
    }

    private void posture(ServerLevel World) {
        if (isDeadOrDying()) {
            return;
        }
        boolean Low = cramped(World, position()) || pathLow(World);
        if (Low) {
            LowTicks = StandDelay;
            if (getPose() != Pose.CROUCHING) {
                setPose(Pose.CROUCHING);
            }
            return;
        }
        if (getPose() == Pose.CROUCHING && --LowTicks <= 0) {
            setPose(Pose.STANDING);
        }
    }

    private boolean cramped(ServerLevel World, Vec3 At) {
        AABB Standing = EntityDimensions.scalable(Width, Height).makeBoundingBox(At).deflate(0.02, 0.0, 0.02);
        return !World.noBlockCollision(this, Standing);
    }

    private boolean pathLow(ServerLevel World) {
        Path Current = getNavigation().getPath();
        if (Current == null || Current.isDone()) {
            return false;
        }
        int Next = Current.getNextNodeIndex();
        for (int Index = Next; Index < Math.min(Current.getNodeCount(), Next + 3); Index++) {
            Node Step = Current.getNode(Index);
            if (Index > Next && Step.distanceToSqr(blockPosition()) > 9.0f) {
                break;
            }
            if (cramped(World, Vec3.atBottomCenterOf(new BlockPos(Step.x, Step.y, Step.z)))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Current) {
        return Current == Pose.CROUCHING ? CrawlSize : super.getDefaultDimensions(Current);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide) {
            if (isAlive()) {
                triggerAnim("hurt", "hurt");
            }
            if (isAlive() && Source.getEntity() instanceof LivingEntity Attacker && Scp427Prey.prey(this, Attacker)) {
                LivingEntity Current = getTarget();
                if (Current == null || Current != Attacker && distanceTo(Attacker) < distanceTo(Current)) {
                    setTarget(Attacker);
                    LastSeen = Attacker.position();
                }
            }
        }
        return Hurt;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource Source) {
        boolean Before = dead;
        super.die(Source);
        if (!Before && dead && level() instanceof ServerLevel World) {
            Scp427Registry.play(World, getEyePosition(), Scp427Registry.DeathLayers, SoundSource.HOSTILE, 1.6f, 1.0f);
        }
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
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
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp427.no_link");
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp427Registry.Idle.get();
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        playSound(Scp427Registry.IdleCreak.get(), 0.6f, getVoicePitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp427Registry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        super.playHurtSound(Source);
        playSound(Scp427Registry.HurtCreak.get(), 0.7f, getVoicePitch());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Nullable
    public Scp427Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp427Gait();
        }
        return Gait;
    }

    public Scp427Hair hair() {
        if (Hair == null) {
            Hair = new Scp427Hair();
        }
        return Hair;
    }

    public boolean isStriding() {
        Scp427Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp427Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp427Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("LungeTimer", LungeTimer);
        Saved.putInt("RoarTimer", RoarTimer);
        Saved.putBoolean("Emerging", Emerging);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        LungeTimer = Saved.getInt("LungeTimer");
        RoarTimer = Saved.getInt("RoarTimer");
        Emerging = Saved.getBoolean("Emerging");
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp427GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp427GaitController.blendFor(Current));
            if (deathTime > 0 || isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            if (Current == ActionLunge) {
                return Event.setAndContinue(LungePlay);
            }
            if (Current == ActionSlam) {
                return Event.setAndContinue(SlamPlay);
            }
            if (Current == ActionRoar) {
                return Event.setAndContinue(RoarPlay);
            }
            if (isCrawling()) {
                return Event.setAndContinue(CrawlLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "hurt", 1, Event -> PlayState.STOP).triggerableAnim("hurt", HurtPlay));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
