package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp354.Scp354Emergent;
import net.rai.scp_unleashed.scp354.Scp354Swim;
import net.rai.scp_unleashed.scp354.Scp354Swimmer;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class Scp354Beast extends Monster implements GeoEntity, IEntityWithComplexSpawn, Scp354Emergent, Scp354Swimmer, ActionClock.Owner {
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final int EmergeTicks = 52;
    public static final int DripTicks = 320;
    public static final int DeathTicks = 40;
    public static final int FadeStart = 21;
    public static final int SplashTick = 22;
    public static final int ScanTicks = 10;
    public static final double SwitchShare = 0.64;
    public static final double AcquireHeight = 12.0;
    public static final int GiveUpTicks = 600;
    public static final double StillPace = 0.012;
    public static final int ActionBlend = 2;
    public static final int LoopBlend = 3;
    public static final int SwimBlend = 6;
    public static final double MaxPhaseStep = 0.11;
    public static final float LookYawLimit = 45.0f;
    public static final float LookPitchLimit = 30.0f;
    public static final float LookFollow = 0.35f;
    public static final int FlinchGap = 12;
    public static final double PhaseWrap = 64.0;
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PoolBornData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DrippingData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HaulData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation HurtPlay = RawAnimation.begin().thenPlay("hurt");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> Plays = new HashMap<>();
    private final Map<String, RawAnimation> Loops = new HashMap<>();
    protected int ActionTicks;
    private int DripLeft;
    private int HaulLeft;
    private final Scp354Swim.Course Swimming = new Scp354Swim.Course();
    private int FlinchReady;
    private int TickOrigin;
    private float RenderPartial;
    private final StrideGait Walker = new StrideGait();
    private final ActionClock Clock = new ActionClock();
    @Nullable
    private StrideGait.Shape Form;
    private double IdlePhase;
    private double IdlePhaseO;
    private float BodyYawO;
    private float LookYaw;
    private float LookYawO;
    private float LookPitch;
    private float LookPitchO;
    @Nullable
    private String LastLoop;
    private int LastBlend = LoopBlend;
    @Nullable
    private Scp354Stride Gait;
    @Nullable
    private AnimationController<Scp354Beast> BodyController;

    protected Scp354Beast(EntityType<? extends Scp354Beast> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        setPathfindingMalus(PathType.DANGER_OTHER, Scp354Swim.ShoreMalus);
    }

    protected abstract Scp354Stride[] strides();

    protected abstract double runEnter();

    protected abstract double runExit();

    protected abstract double idleSeconds();

    public abstract float renderScale();

    public abstract Scp354Voice voice();

    @Nullable
    protected abstract String clipFor(byte Action);

    protected abstract void actionTick(byte Kind, int Elapsed);

    public double acquireRange() {
        return 32.0;
    }

    public List<String> hiddenBones() {
        return List.of();
    }

    public boolean showsBone(String Bone) {
        return true;
    }

    public void engage(LivingEntity Target) {
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive() && canAct() && !(swimsOut() && Scp354Swim.swimming(this))) {
            engage(Target);
        }
    }

    @Nullable
    protected String loopClip() {
        return null;
    }

    protected boolean countsClimb() {
        return false;
    }

    protected double turnRadius() {
        return 0.6;
    }

    public boolean locksMovement() {
        return action() == ActionEmerge;
    }

    public boolean locksYaw() {
        return action() == ActionEmerge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
        Builder.define(PoolBornData, false);
        Builder.define(DrippingData, false);
        Builder.define(HaulData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp354Swim.DryNavigation(this, World);
    }

    @Override
    public boolean swimsOut() {
        return true;
    }

    @Override
    public double swimSpeed() {
        return 0.12;
    }

    @Override
    public double draft() {
        return getBbHeight() * 0.5;
    }

    @Override
    public double haulReach() {
        return 1.6;
    }

    @Override
    public int haulTicks() {
        return 14;
    }

    @Override
    public Scp354Swim.Course course() {
        return Swimming;
    }

    @Override
    public void beginHaul() {
        HaulLeft = haulTicks();
        entityData.set(HaulData, true);
    }

    @Override
    public boolean isHauling() {
        return entityData.get(HaulData);
    }

    protected void registerBaseGoals() {
        goalSelector.addGoal(0, new Scp354Swim.Escape(this, this));
        goalSelector.addGoal(0, new Scp354Swim.Buoy(this));
        goalSelector.addGoal(1, new Scp354BeastGoals.HoldStill(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new Scp354BeastGoals.Hunt(this));
    }

    public static boolean isPoolBorn(Entity Found) {
        return Found.getType().is(Scp354BeastRegistry.Spawns);
    }

    public static boolean isQuarry(LivingEntity Hunter, LivingEntity Found) {
        if (Found == Hunter || !Found.isAlive() || Found.isInvulnerable() || isPoolBorn(Found) || Found.level() != Hunter.level() || !Targeting.isTargetable(Found)) {
            return false;
        }
        if (Found instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        return ControlBodies.isPersonLike(Found) || Found instanceof AbstractVillager;
    }

    public static boolean isFairGame(LivingEntity Hunter, LivingEntity Found) {
        if (Found == Hunter || !Found.isAlive() || isPoolBorn(Found) || Found.isSpectator()) {
            return false;
        }
        return !(Found instanceof Player User) || !User.isCreative();
    }

    public List<LivingEntity> victims(AABB Area, Predicate<LivingEntity> Extra) {
        return level().getEntitiesOfClass(LivingEntity.class, Area, Found -> isFairGame(this, Found) && Extra.test(Found));
    }

    @Override
    public boolean canAttackType(EntityType<?> Type) {
        return !Type.is(Scp354BeastRegistry.Spawns) && super.canAttackType(Type);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return !isPoolBorn(Target) && super.canAttack(Target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        if (Target != null && isPoolBorn(Target)) {
            return;
        }
        super.setTarget(Target);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isPoolBornFlag() {
        return entityData.get(PoolBornData);
    }

    public boolean isDripping() {
        return entityData.get(DrippingData);
    }

    @Override
    public boolean isEmerging() {
        return action() == ActionEmerge;
    }

    public boolean canAct() {
        return !isActing() && isAlive() && !isDeadOrDying();
    }

    @Override
    public void beginEmerge() {
        setAction(ActionEmerge, EmergeTicks);
        entityData.set(PoolBornData, true);
        DripLeft = DripTicks;
        entityData.set(DrippingData, true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel World) {
            playLayers(Scp354BeastRegistry.BeastSurge, 1.4f, 0.8f);
            playLayers(voice().Emerge(), 1.6f, voice().Pitch());
            World.sendParticles(Scp354BeastRegistry.Splash.get(), getX(), getY() + 0.1, getZ(), 6, getBbWidth() * 0.35, 0.0, getBbWidth() * 0.35, 0.0);
            World.sendParticles(Scp354BeastRegistry.Drip.get(), getX(), getY() + 0.3, getZ(), 30, getBbWidth() * 0.4, 0.2, getBbWidth() * 0.4, 0.25);
        }
    }

    @Nullable
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(clockNow());
        }
        return clipOf(action());
    }

    @Nullable
    private String clipOf(byte Current) {
        if (Current == ActionNone) {
            return null;
        }
        return Current == ActionEmerge ? "emerge" : clipFor(Current);
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private double clockNow() {
        return tickCount + RenderPartial;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        Clock.begin(Current, clipOf(Current), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(LockedYawData, getYRot());
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (locksMovement()) {
            getNavigation().stop();
        }
        applyLockedYaw();
    }

    public void endAction() {
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    public int actionTicksLeft() {
        return ActionTicks;
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
    }

    public double deathElapsed() {
        return deathTime + (level().isClientSide ? RenderPartial : 0.0f);
    }

    public float lockedYaw() {
        return entityData.get(LockedYawData);
    }

    public void lockYawTo(float Yaw) {
        entityData.set(LockedYawData, Mth.wrapDegrees(Yaw));
        applyLockedYaw();
    }

    public void applyLockedYaw() {
        if (!locksYaw()) {
            return;
        }
        float Yaw = entityData.get(LockedYawData);
        setYRot(Yaw);
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
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
    public void tick() {
        super.tick();
        applyLockedYaw();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!isAlive()) {
            return;
        }
        FlinchReady = Math.max(0, FlinchReady - 1);
        if (DripLeft > 0 && --DripLeft == 0) {
            entityData.set(DrippingData, false);
        }
        if (HaulLeft > 0 && --HaulLeft == 0) {
            entityData.set(HaulData, false);
        }
        if (ActionTicks > 0) {
            byte Current = action();
            int Elapsed = (int) actionElapsed();
            if (Current == ActionEmerge) {
                emergeTick(Elapsed);
            } else {
                actionTick(Current, Elapsed);
            }
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved() && action() == Current) {
                finishAction(Current);
            }
        }
    }

    protected void finishAction(byte Kind) {
        entityData.set(ActionData, ActionNone);
    }

    protected void emergeTick(int Elapsed) {
        Scp354Swim.halt(this);
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Elapsed < 30 && Elapsed % 3 == 0) {
            World.sendParticles(Scp354BeastRegistry.Drip.get(), getX(), getY() + getBbHeight() * 0.3, getZ(), 8, getBbWidth() * 0.45, getBbHeight() * 0.2,
                    getBbWidth() * 0.45, 0.12);
        }
        if (Elapsed == 30) {
            World.sendParticles(Scp354BeastRegistry.Drip.get(), getX(), getY() + getBbHeight() * 0.6, getZ(), 40, getBbWidth() * 0.5, getBbHeight() * 0.3,
                    getBbWidth() * 0.5, 0.35);
            playLayers(Scp354BeastRegistry.BeastSplash, 0.8f, 1.3f);
        }
    }

    private void clientTick() {
        gaitTick();
        lookTick();
        Clock.settle();
        if (!isAlive() || isInvisible()) {
            return;
        }
        boolean Dripping = isDripping();
        float Rate = isEmerging() ? 1.0f : Dripping ? 0.45f : 0.03f;
        float Size = getBbWidth() * 0.5f;
        int Count = random.nextFloat() < Rate ? 1 + (isEmerging() ? random.nextInt(3) : 0) : 0;
        for (int Index = 0; Index < Count; Index++) {
            double X = getX() + (random.nextDouble() - 0.5) * 2.0 * Size;
            double Y = getY() + getBbHeight() * (0.25 + random.nextDouble() * 0.7);
            double Z = getZ() + (random.nextDouble() - 0.5) * 2.0 * Size;
            level().addParticle(Scp354BeastRegistry.Drip.get(), X, Y, Z, 0.0, 0.0, 0.0);
        }
    }

    private void lookTick() {
        LookYawO = LookYaw;
        LookPitchO = LookPitch;
        boolean Free = isAlive() && deathTime <= 0 && !isActing();
        float WantYaw = Free ? Mth.clamp(Mth.wrapDegrees(yHeadRot - yBodyRot), -LookYawLimit, LookYawLimit) : 0.0f;
        float WantPitch = Free ? Mth.clamp(Mth.wrapDegrees(getXRot()), -LookPitchLimit, LookPitchLimit) : 0.0f;
        LookYaw += (WantYaw - LookYaw) * LookFollow;
        LookPitch += (WantPitch - LookPitch) * LookFollow;
    }

    public float lookYaw(float Partial) {
        return Mth.lerp(Partial, LookYawO, LookYaw);
    }

    public float lookPitch(float Partial) {
        return Mth.lerp(Partial, LookPitchO, LookPitch);
    }

    private StrideGait.Shape shape() {
        if (Form == null) {
            Scp354Stride[] Strides = strides();
            Form = new StrideGait.Shape(StillPace, runEnter(), runExit(), Strides[0].distance(), Strides[1].distance(), MaxPhaseStep, LoopBlend);
        }
        return Form;
    }

    private void gaitTick() {
        double Dx = getX() - xo;
        double Dy = countsClimb() ? getY() - yo : 0.0;
        double Dz = getZ() - zo;
        double Turn = Math.abs(Mth.wrapDegrees(yBodyRot - BodyYawO)) * Mth.DEG_TO_RAD * turnRadius();
        BodyYawO = yBodyRot;
        Scp354Stride[] Strides = strides();
        boolean Stepped = Walker.tick(Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) + Math.min(Turn, 0.2), isAlive(), shape());
        Gait = Walker.stride() == StrideGait.Idle ? null : Strides[Walker.stride()];
        IdlePhaseO = IdlePhase;
        IdlePhase += 1.0 / (idleSeconds() * 20.0);
        if (IdlePhase > PhaseWrap) {
            IdlePhase -= PhaseWrap;
            IdlePhaseO -= PhaseWrap;
        }
        if (Gait == null || !Stepped || isSilent() || isActing() || !treads()) {
            return;
        }
        float[] Touch = Gait.touchdowns();
        for (int Limb = 0; Limb < Touch.length; Limb++) {
            if (Scp354Stride.crossed(Walker.from(), Walker.to(), Touch[Limb])) {
                footfall(Limb, Gait == Strides[1]);
            }
        }
    }

    protected boolean treads() {
        return onGround() || countsClimb();
    }

    protected void footfall(int Limb, boolean Running) {
        if (!level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 32.0)) {
            return;
        }
        float Volume = voice().StepVolume() * (Running ? 1.15f : 0.85f) * (0.88f + random.nextFloat() * 0.12f);
        float Pitch = voice().Pitch() * (0.92f + random.nextFloat() * 0.12f) * (Limb % 2 == 0 ? 1.0f : 0.96f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : voice().Step()) {
            level().playLocalSound(getX(), getY(), getZ(), Layer.get(), getSoundSource(), Volume, Pitch, false);
        }
    }

    @Nullable
    public Scp354Stride gait() {
        return Gait;
    }

    @Nullable
    public Scp354Stride strideNamed(String Clip) {
        for (Scp354Stride Stride : strides()) {
            if (Stride.clip().equals(Clip)) {
                return Stride;
            }
        }
        return null;
    }

    public double gaitCycle(float Partial) {
        return Walker.cycle(Partial);
    }

    public double idleCycle(float Partial) {
        double Value = Mth.lerp(Partial, IdlePhaseO, IdlePhase);
        return Value - Math.floor(Value);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public float fade(float Partial) {
        if (deathTime <= 0) {
            return 1.0f;
        }
        float Time = deathTime + Partial;
        return Mth.clamp(1.0f - (Time - FadeStart) / (DeathTicks - FadeStart), 0.0f, 1.0f);
    }

    public float emergeShare(float Partial) {
        if (!isEmerging()) {
            return 1.0f;
        }
        return (float) Mth.clamp(actionElapsed() / 28.0, 0.0, 1.0);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Shift = 0.94f + random.nextFloat() * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch * Shift);
        }
    }

    public boolean strike(LivingEntity Target, float Damage, double Knock, double Lift) {
        if (!Target.hurt(damageSources().mobAttack(this), Damage)) {
            return false;
        }
        if (Knock > 0.0) {
            Target.knockback(Knock, getX() - Target.getX(), getZ() - Target.getZ());
        }
        if (Lift > 0.0) {
            Target.setDeltaMovement(Target.getDeltaMovement().add(0.0, Lift, 0.0));
        }
        Target.hurtMarked = true;
        setLastHurtMob(Target);
        return true;
    }

    public double reachTo(Entity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.max(0.0, Math.sqrt(Dx * Dx + Dz * Dz) - (getBbWidth() + Target.getBbWidth()) * 0.5);
    }

    public void faceToward(Entity Target, float Step) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Yaw, Step)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    public void crackGround(ServerLevel World, Vec3 Centre, int Count, double Spread) {
        BlockPos Below = BlockPos.containing(Centre.x, Centre.y - 0.2, Centre.z);
        BlockState Ground = World.getBlockState(Below);
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Centre.x, Centre.y + 0.1, Centre.z, Count, Spread, 0.1, Spread, 0.2);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return isEmerging() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World && isAlive()) {
            World.sendParticles(Scp354BeastRegistry.Drip.get(), getX(), getY() + getBbHeight() * 0.6, getZ(), 6, getBbWidth() * 0.3, getBbHeight() * 0.2,
                    getBbWidth() * 0.3, 0.15);
            if (FlinchReady <= 0 && !isActing()) {
                FlinchReady = FlinchGap;
                triggerAnim("flinch", "hurt");
            }
        }
        return Hurt;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!isEmerging()) {
            super.knockback(Strength, X, Z);
        }
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (deathTime == SplashTick) {
            float Wide = getBbWidth() * 0.5f;
            World.sendParticles(Scp354BeastRegistry.Splash.get(), getX(), getY() + 0.05, getZ(), 10, Wide, 0.0, Wide, 0.0);
            World.sendParticles(Scp354BeastRegistry.Drip.get(), getX(), getY() + 0.4, getZ(), 50, Wide, 0.3, Wide, 0.35);
            playLayers(Scp354BeastRegistry.BeastSplash, 1.3f, 0.75f);
        }
        if (deathTime > SplashTick && deathTime % 4 == 0) {
            World.sendParticles(Scp354BeastRegistry.Splash.get(), getX(), getY() + 0.05, getZ(), 2, getBbWidth() * 0.4, 0.0, getBbWidth() * 0.4, 0.0);
        }
        if (deathTime >= DeathTicks && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel) {
            endAction();
            getNavigation().stop();
            List<DeferredHolder<SoundEvent, SoundEvent>> Layers = voice().Death();
            for (int Index = 1; Index < Layers.size(); Index++) {
                playSound(Layers.get(Index).get(), getSoundVolume(), voice().Pitch());
            }
        }
    }

    @Override
    public void playAmbientSound() {
        if (isActing() || isSilent()) {
            return;
        }
        playLayers(voice().Idle(), getSoundVolume(), voice().Pitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return voice().Idle().get(0).get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(voice().Hurt(), getSoundVolume(), voice().Pitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return voice().Hurt().get(0).get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return voice().Death().get(0).get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
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
        return super.getBoundingBoxForCulling().inflate(renderScale() * 1.2);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (!level().isClientSide || !Key.equals(ActionData) && !Key.equals(ActionStartData)) {
            return;
        }
        syncClock();
        if (Key.equals(ActionData) && BodyController != null && action() != ActionNone) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("PoolBorn", isPoolBornFlag());
        Tag.putInt("Drip", DripLeft);
        if (isEmerging()) {
            Tag.putInt("Emerge", ActionTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(PoolBornData, Tag.getBoolean("PoolBorn"));
        DripLeft = Tag.getInt("Drip");
        entityData.set(DrippingData, DripLeft > 0);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        if (Tag.getInt("Emerge") > 0) {
            setAction(ActionEmerge, Tag.getInt("Emerge"));
        }
    }

    protected RawAnimation play(String Clip) {
        return Plays.computeIfAbsent(Clip, Name -> RawAnimation.begin().thenPlayAndHold(Name));
    }

    protected RawAnimation loop(String Clip) {
        return Loops.computeIfAbsent(Clip, Name -> RawAnimation.begin().thenLoop(Name));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp354BeastController Body = new Scp354BeastController(this, "body", Event -> {
            if (deathTime > 0 || isDeadOrDying()) {
                return Event.setAndContinue(choose(Event, "death", DeathPlay));
            }
            String Clip = actionClip();
            if (Clip != null) {
                RawAnimation Wanted = choose(Event, Clip, play(Clip));
                if ("emerge".equals(Clip)) {
                    Event.getController().transitionLength(0);
                }
                return Event.setAndContinue(Wanted);
            }
            if (swimsOut() && isHauling()) {
                return Event.setAndContinue(choose(Event, "haulout", play("haulout")));
            }
            if (swimsOut() && (isInWater() || isInFluidType())) {
                return Event.setAndContinue(choose(Event, "swim", loop("swim")));
            }
            String Loop = loopClip();
            if (Loop != null) {
                return Event.setAndContinue(choose(Event, Loop, loop(Loop)));
            }
            if (Gait != null) {
                return Event.setAndContinue(choose(Event, Gait.clip(), Gait.loop()));
            }
            return Event.setAndContinue(choose(Event, "idle", IdleLoop));
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "flinch", 1, Event -> PlayState.STOP).triggerableAnim("hurt", HurtPlay));
    }

    private RawAnimation choose(AnimationState<Scp354Beast> Event, String Clip, RawAnimation Wanted) {
        if (!Clip.equals(LastLoop)) {
            LastBlend = blend(LastLoop, Clip);
            LastLoop = Clip;
        }
        Event.getController().transitionLength(LastBlend);
        return Wanted;
    }

    public int blend(@Nullable String From, String To) {
        if (To.equals(actionClip()) || "death".equals(To) || "haulout".equals(To)) {
            return actionBlend();
        }
        if (From == null || From.equals(To)) {
            return LoopBlend;
        }
        if (slowBlend(From) || "swim".equals(To)) {
            return SwimBlend;
        }
        return LoopBlend;
    }

    protected int actionBlend() {
        return ActionBlend;
    }

    protected boolean slowBlend(String Clip) {
        return "swim".equals(Clip) || "haulout".equals(Clip);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
