package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
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

public class Scp093Torso extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float RenderScale = 2.2f;
    public static final float Width = 4.4f;
    public static final float Height = 8.4f;
    public static final float EyeHeight = 6.85f;
    public static final double Health = 100.0;
    public static final double Armour = 6.0;
    public static final double KnockbackResistance = 1.0;
    public static final double Speed = 0.27;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.55;
    public static final double FollowRange = 48.0;
    public static final double WastesFollowBonus = 56.0;
    public static final double StepHeight = 2.6;
    public static final float ClimbDrive = 0.01f;
    public static final double SwipeDamage = 9.0;
    public static final double SlamDamage = 13.0;
    public static final double SwipeReach = 3.6;
    public static final double SwipeSlack = 0.4;
    public static final float SwipeArc = 40.0f;
    public static final double SwipePush = 0.8;
    public static final double SlamForward = 3.6;
    public static final double SlamRadius = 3.0;
    public static final double SlamBelow = 1.5;
    public static final double SlamAbove = 4.0;
    public static final double EvenHeight = 5.0;
    public static final int SwipeTicks = 18;
    public static final int SwipeHitTick = 10;
    public static final int SwipeCooldown = 24;
    public static final int SlamTicks = 30;
    public static final int SlamHitTick = 18;
    public static final int SlamCooldown = 90;
    public static final float SlamChance = 0.38f;
    public static final int SlamSlowTicks = 60;
    public static final int RiseTicks = 60;
    public static final int DeathTicks = 30;
    public static final int DeathImpactTick = 22;
    public static final int ImpactWindow = 4;
    public static final int DeathMark = -2;
    public static final int AmbientInterval = 150;
    public static final float Voice = 3.0f;
    public static final double CullSide = 4.8;
    public static final double CullUp = 4.6;
    public static final double CullDown = 6.7;
    public static final double PlantForward = 3.40;
    public static final double PlantSide = 2.48;
    public static final int RoomReach = 12;
    public static final int RoomStep = 2;
    public static final int RoomRise = 4;
    public static final float AimTolerance = 25.0f;
    public static final float BodyTurn = 4.0f;
    public static final float BodyFollow = 6.0f;
    public static final float BodyEase = 0.5f;
    public static final float TurnEase = 0.35f;
    public static final float TurnFloor = 0.6f;
    public static final float IdleTurn = 1.5f;
    public static final int HeadTurn = 7;
    public static final int HeadReach = 50;
    public static final double KeepShare = 1.5;
    public static final int LoseTicks = 400;
    public static final int ForgetTicks = 600;
    public static final double SenseRange = 8.0;
    public static final double[] SightLevels = {4.0, 1.5};
    public static final double SightLimit = 128.0;
    public static final byte ActionNone = 0;
    public static final byte ActionSwipeLeft = 1;
    public static final byte ActionSlam = 2;
    public static final byte ActionRise = 3;
    public static final byte ActionSwipeRight = 4;
    public static final Scp093Gait.Profile CrawlProfile = new Scp093Gait.Profile(55.0, 0.1580, 0.25, 1.6, new double[]{0.02, 0.52}, new float[]{1.0f, 0.92f});
    public static final ResourceLocation WastesRange = Scp093Registry.location("scp093_wastes_range");
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp093Torso.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp093Torso.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp093Torso.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ClimbingData = SynchedEntityData.defineId(Scp093Torso.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation SwipeLeftPlay = RawAnimation.begin().thenPlayAndHold("swipe_left");
    private static final RawAnimation SwipeRightPlay = RawAnimation.begin().thenPlayAndHold("swipe_right");
    private static final RawAnimation SlamPlay = RawAnimation.begin().thenPlayAndHold("slam");
    private static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionTicks;
    @Nullable
    private LivingEntity StrikeTarget;
    private int SwipeReady;
    private int SlamReady;
    private float RenderPartial;
    private int TickOrigin;
    private int ImpactSeen = Integer.MIN_VALUE;
    @Nullable
    private Vec3 LastSeen;
    private int UnseenTicks;
    private int AwayTicks;
    @Nullable
    private LivingEntity Tracked;
    @Nullable
    private Scp093Gait Gait;

    public Scp093Torso(EntityType<? extends Scp093Torso> Type, Level World) {
        super(Type, World);
        xpReward = 30;
        moveControl = new Scp093TorsoMove(this);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new Scp093TorsoTurning(this);
    }

    @Override
    public int getMaxHeadYRot() {
        return HeadReach;
    }

    @Override
    public int getHeadRotSpeed() {
        return HeadTurn;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, SwipeDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.STEP_HEIGHT, StepHeight).add(Attributes.ATTACK_KNOCKBACK, 1.6).add(Attributes.SAFE_FALL_DISTANCE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(HuntingData, false);
        Builder.define(ClimbingData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp093TorsoNavigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp093TorsoGoals.HoldStill(this));
        goalSelector.addGoal(2, new Scp093TorsoGoals.Engage(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp093TorsoGoals.Hunt<>(this, Player.class, Scp093Torso::isQuarry));
        targetSelector.addGoal(2, new Scp093TorsoGoals.Hunt<>(this, LivingEntity.class, Found -> !(Found instanceof Player) && ControlBodies.isPersonLike(Found) && isQuarry(Found)));
        targetSelector.addGoal(3, new Scp093TorsoGoals.Retaliate(this));
    }

    public static boolean isQuarry(LivingEntity Found) {
        if (Found instanceof Scp093Torso || !Found.isAlive() || Found.isSpectator() || !Targeting.isTargetable(Found)) {
            return false;
        }
        return !(Found instanceof Player User) || !User.isCreative();
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isRising() {
        return action() == ActionRise;
    }

    public boolean isHunting() {
        return entityData.get(HuntingData);
    }

    public boolean isClimbing() {
        return entityData.get(ClimbingData);
    }

    public void faceForward() {
        yBodyRot = getYRot();
        yBodyRotO = yBodyRot;
        yHeadRot = getYRot();
        yHeadRotO = yHeadRot;
    }

    public double followRange() {
        return getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    @Override
    public boolean hasLineOfSight(Entity Seen) {
        if (super.hasLineOfSight(Seen)) {
            return true;
        }
        if (Seen.level() != level()) {
            return false;
        }
        Vec3 Far = new Vec3(Seen.getX(), Seen.getEyeY(), Seen.getZ());
        for (double Lift : SightLevels) {
            Vec3 Near = new Vec3(getX(), getY() + Lift, getZ());
            if (Near.distanceTo(Far) <= SightLimit && level().clip(new ClipContext(Near, Far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    public boolean senses(LivingEntity Target) {
        return Scp093Wastes.inside(this) || distanceToSqr(Target) < SenseRange * SenseRange || getSensing().hasLineOfSight(Target);
    }

    public boolean keeps(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || Target.level() != level() || !isQuarry(Target) || !canAttack(Target)) {
            return false;
        }
        double Keep = followRange() * KeepShare;
        if (distanceToSqr(Target) > Keep * Keep || Tracked == Target && AwayTicks > LoseTicks) {
            return false;
        }
        return Tracked != Target || Scp093Wastes.inside(this) || UnseenTicks <= ForgetTicks;
    }

    @Nullable
    public Vec3 lastSeen() {
        return LastSeen;
    }

    private void track() {
        LivingEntity Target = getTarget();
        if (Target == null || !Target.isAlive()) {
            Tracked = null;
            LastSeen = null;
            UnseenTicks = 0;
            AwayTicks = 0;
            return;
        }
        if (Target != Tracked) {
            Tracked = Target;
            LastSeen = Target.position();
            UnseenTicks = 0;
            AwayTicks = 0;
        }
        if (senses(Target)) {
            LastSeen = Target.position();
            UnseenTicks = 0;
        } else {
            UnseenTicks++;
        }
        AwayTicks = distanceTo(Target) > followRange() ? AwayTicks + 1 : 0;
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public void jumpFromGround() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity Passenger) {
        return false;
    }

    public boolean locksMovement() {
        return isActing() || isDeadOrDying();
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        getNavigation().stop();
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionSwipeLeft -> "swipe_left";
            case ActionSwipeRight -> "swipe_right";
            case ActionSlam -> "slam";
            case ActionRise -> "rise";
            default -> null;
        };
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public double deathElapsed() {
        return deathTime + (isDeadOrDying() ? RenderPartial : 0.0f);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public boolean canSwipe() {
        return SwipeReady <= 0 && !isActing();
    }

    public boolean canSlam() {
        return SlamReady <= 0 && !isActing();
    }

    public double reachTo(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz) - Target.getBbWidth() * 0.5 - getBbWidth() * 0.25;
    }

    public float yawTo(double X, double Z) {
        return (float) (Mth.atan2(Z - getZ(), X - getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public float facingError(LivingEntity Target) {
        return Math.abs(Mth.wrapDegrees(yawTo(Target.getX(), Target.getZ()) - getYRot()));
    }

    public static float turnStep(float Error, float Limit) {
        float Step = Error * TurnEase;
        if (Math.abs(Step) < TurnFloor) {
            Step = Math.signum(Error) * Math.min(Math.abs(Error), TurnFloor);
        }
        return Mth.clamp(Step, -Limit, Limit);
    }

    public void turnToward(double X, double Z) {
        setYRot(getYRot() + turnStep(Mth.wrapDegrees(yawTo(X, Z) - getYRot()), BodyTurn));
    }

    public double sideOf(LivingEntity Target) {
        double Yaw = getYRot() * Mth.DEG_TO_RAD;
        return (Target.getX() - getX()) * Math.cos(Yaw) + (Target.getZ() - getZ()) * Math.sin(Yaw);
    }

    public void beginSwipe(LivingEntity Target) {
        StrikeTarget = Target;
        SwipeReady = SwipeCooldown;
        setAction(sideOf(Target) >= 0.0 ? ActionSwipeLeft : ActionSwipeRight, SwipeTicks);
    }

    public void beginSlam(LivingEntity Target) {
        StrikeTarget = Target;
        SlamReady = SlamCooldown;
        SwipeReady = Math.max(SwipeReady, SwipeCooldown);
        setAction(ActionSlam, SlamTicks);
        playLayers(Scp093Registry.AmbientLayers, Voice * 0.8f, 0.65f);
    }

    public void beginRise() {
        setAction(ActionRise, RiseTicks);
        setPersistenceRequired();
        playLayers(Scp093Registry.RiseLayers, Voice * 0.8f, 0.8f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp093Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
                impacts(Current);
            }
            return;
        }
        boolean Climb = horizontalCollision && isAlive() && !locksMovement() && zza > ClimbDrive;
        if (entityData.get(ClimbingData) != Climb) {
            entityData.set(ClimbingData, Climb);
        }
        if (!isAlive()) {
            return;
        }
        SwipeReady = Math.max(0, SwipeReady - 1);
        SlamReady = Math.max(0, SlamReady - 1);
        if (tickCount % 40 == 0) {
            reach();
        }
        track();
        boolean Hunting = getTarget() != null && getTarget().isAlive();
        if (entityData.get(HuntingData) != Hunting) {
            entityData.set(HuntingData, Hunting);
        }
        if (ActionTicks > 0) {
            actionTick(action(), (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                entityData.set(ActionData, ActionNone);
                StrikeTarget = null;
            }
        }
    }

    private void impacts(Scp093Gait Current) {
        int Mark;
        Vec3 Spot;
        float Weight;
        if (isDeadOrDying()) {
            if (deathTime < DeathImpactTick || deathTime >= DeathImpactTick + ImpactWindow) {
                return;
            }
            Mark = DeathMark;
            Spot = position();
            Weight = 1.1f;
        } else {
            double Elapsed = actionElapsed();
            if (action() != ActionSlam || Elapsed < SlamHitTick || Elapsed >= SlamHitTick + ImpactWindow) {
                return;
            }
            Mark = entityData.get(ActionStartData);
            Spot = position().add(Vec3.directionFromRotation(0.0f, yBodyRot).scale(SlamForward));
            Weight = 1.0f;
        }
        if (Mark == ImpactSeen) {
            return;
        }
        ImpactSeen = Mark;
        Current.impact(this, Spot, Weight);
    }

    public Vec3 plantSpot(int Index) {
        double Yaw = yBodyRot * Mth.DEG_TO_RAD;
        double Forward = PlantForward;
        double Across = Index == 0 ? PlantSide : -PlantSide;
        return new Vec3(getX() - Math.sin(Yaw) * Forward + Math.cos(Yaw) * Across, getY(), getZ() + Math.cos(Yaw) * Forward + Math.sin(Yaw) * Across);
    }

    private void reach() {
        AttributeInstance Range = getAttribute(Attributes.FOLLOW_RANGE);
        if (Range == null) {
            return;
        }
        boolean Inside = Scp093Wastes.inside(this);
        if (Inside && !Range.hasModifier(WastesRange)) {
            Range.addOrUpdateTransientModifier(new AttributeModifier(WastesRange, WastesFollowBonus, AttributeModifier.Operation.ADD_VALUE));
        } else if (!Inside && Range.hasModifier(WastesRange)) {
            Range.removeModifier(WastesRange);
        }
    }

    private void actionTick(byte Kind, int Elapsed) {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionRise) {
            if (Elapsed % 6 == 0 && level() instanceof ServerLevel World) {
                BlockState Ground = World.getBlockState(blockPosition().below());
                if (!Ground.isAir()) {
                    World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), getY() + 0.1, getZ(), 30, 1.6, 0.1, 1.6, 0.08);
                    World.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, Ground), getX(), getY() + 0.1, getZ(), 12, 1.8, 0.1, 1.8, 0.2);
                }
            }
            return;
        }
        if (Target != null && Target.isAlive() && Elapsed < (Kind == ActionSlam ? SlamHitTick : SwipeHitTick)) {
            getLookControl().setLookAt(Target, HeadTurn, getMaxHeadXRot());
            turnToward(Target.getX(), Target.getZ());
        }
        if ((Kind == ActionSwipeLeft || Kind == ActionSwipeRight) && Elapsed == SwipeHitTick) {
            swipeHit(Target);
        } else if (Kind == ActionSlam && Elapsed == SlamHitTick) {
            slamHit();
        }
    }

    private void swipeHit(@Nullable LivingEntity Target) {
        playLayers(Scp093Registry.SwipeLayers, Voice * 0.7f, 0.85f + random.nextFloat() * 0.12f);
        if (Target == null || !Target.isAlive() || reachTo(Target) > SwipeReach + SwipeSlack || facingError(Target) > SwipeArc || !getSensing().hasLineOfSight(Target)) {
            return;
        }
        if (doHurtTarget(Target)) {
            Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0).normalize().scale(SwipePush);
            Target.push(Push.x, 0.3, Push.z);
            Target.hurtMarked = true;
        }
    }

    private void slamHit() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        playLayers(Scp093Registry.SlamLayers, Voice, 0.75f + random.nextFloat() * 0.1f);
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        Vec3 Centre = position().add(Facing.scale(SlamForward));
        BlockState Ground = World.getBlockState(BlockPos.containing(Centre).below());
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Centre.x, Centre.y + 0.1, Centre.z, 70, 2.2, 0.1, 2.2, 0.2);
        }
        World.sendParticles(ParticleTypes.POOF, Centre.x, Centre.y + 0.2, Centre.z, 14, 1.8, 0.1, 1.8, 0.03);
        DamageSource Source = damageSources().source(Scp093Registry.TorsoDamage, this);
        AABB Area = new AABB(Centre.x - SlamRadius, Centre.y - SlamBelow, Centre.z - SlamRadius, Centre.x + SlamRadius, Centre.y + SlamAbove, Centre.z + SlamRadius);
        for (LivingEntity Struck : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != this && !(Found instanceof Scp093Torso) && Found.isAlive())) {
            double Dx = Struck.getX() - Centre.x;
            double Dz = Struck.getZ() - Centre.z;
            if (Dx * Dx + Dz * Dz > SlamRadius * SlamRadius || Struck instanceof Player User && (User.isCreative() || User.isSpectator())) {
                continue;
            }
            if (Struck.hurt(Source, (float) SlamDamage)) {
                Struck.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SlamSlowTicks, 1), this);
                Struck.push(0.0, 0.5, 0.0);
                Struck.hurtMarked = true;
            }
        }
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    @Override
    public void playAmbientSound() {
        playLayers(Scp093Registry.AmbientLayers, getSoundVolume(), getVoicePitch());
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp093Registry.TorsoAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp093Registry.TorsoHurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(Scp093Registry.HurtLayers, getSoundVolume(), getVoicePitch());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp093Registry.TorsoDeath.get();
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (!level().isClientSide) {
            playSound(Scp093Registry.TorsoDeathCollapse.get(), Voice * 0.8f, 0.75f);
        }
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    protected float getSoundVolume() {
        return Voice;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Box = getBoundingBox();
        return new AABB(Box.minX - CullSide, Box.minY - CullDown, Box.minZ - CullSide, Box.maxX + CullSide, Box.maxY + CullUp, Box.maxZ + CullSide);
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING)) {
            return true;
        }
        return isRising() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        if (Reason == MobSpawnType.SPAWN_EGG || Reason == MobSpawnType.COMMAND) {
            setPersistenceRequired();
        }
        faceForward();
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return !isPersistenceRequired() && super.removeWhenFarAway(Distance);
    }

    @Nullable
    public Scp093Gait gait() {
        if (!level().isClientSide) {
            return null;
        }
        if (Gait == null) {
            Gait = new Scp093Gait(CrawlProfile);
        }
        return Gait;
    }

    public boolean isCrawling() {
        Scp093Gait Current = gait();
        return Current != null && Current.striding();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp093Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    public static boolean roomAt(ServerLevel World, Vec3 Spot) {
        AABB Box = Scp093Registry.Torso.get().getDimensions().makeBoundingBox(Spot);
        if (!World.noCollision(Box) || World.containsAnyLiquid(Box)) {
            return false;
        }
        BlockPos Below = BlockPos.containing(Spot.x, Spot.y - 0.5, Spot.z);
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    public static Vec3 roomNear(ServerLevel World, Vec3 Spot) {
        if (roomAt(World, Spot)) {
            return Spot;
        }
        BlockPos Base = BlockPos.containing(Spot);
        for (int Ring = RoomStep; Ring <= RoomReach; Ring += RoomStep) {
            for (int Dx = -Ring; Dx <= Ring; Dx += RoomStep) {
                for (int Dz = -Ring; Dz <= Ring; Dz += RoomStep) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Ring) {
                        continue;
                    }
                    for (int Lift = 0; Lift <= RoomRise * 2; Lift++) {
                        int Dy = (Lift + 1) / 2 * (Lift % 2 == 0 ? -1 : 1);
                        Vec3 Try = new Vec3(Base.getX() + Dx + 0.5, Base.getY() + Dy, Base.getZ() + Dz + 0.5);
                        if (World.hasChunkAt(BlockPos.containing(Try)) && roomAt(World, Try)) {
                            return Try;
                        }
                    }
                }
            }
        }
        return Spot;
    }

    public static Scp093Torso riseFrom(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp093Torso Risen = Scp093Registry.Torso.get().create(World);
        if (Risen == null) {
            return null;
        }
        Vec3 Room = roomNear(World, Spot);
        Risen.moveTo(Room.x, Room.y, Room.z, Yaw, 0.0f);
        Risen.setYBodyRot(Yaw);
        Risen.setYHeadRot(Yaw);
        Risen.finalizeSpawn(World, World.getCurrentDifficultyAt(Risen.blockPosition()), MobSpawnType.CONVERSION, null);
        if (!World.addFreshEntity(Risen)) {
            return null;
        }
        Risen.beginRise();
        return Risen;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("SlamCooldown", SlamReady);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        SlamReady = Tag.getInt("SlamCooldown");
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        faceForward();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp093GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp093GaitController.blendFor(Current));
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            if (Current == ActionRise) {
                return Event.setAndContinue(RisePlay);
            }
            if (Current == ActionSwipeLeft) {
                return Event.setAndContinue(SwipeLeftPlay);
            }
            if (Current == ActionSwipeRight) {
                return Event.setAndContinue(SwipeRightPlay);
            }
            if (Current == ActionSlam) {
                return Event.setAndContinue(SlamPlay);
            }
            if (isCrawling()) {
                return Event.setAndContinue(CrawlLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
