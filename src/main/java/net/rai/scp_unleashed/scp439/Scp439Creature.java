package net.rai.scp_unleashed.scp439;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class Scp439Creature extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn, ActionClock.Owner {
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final double StillPace = 0.008;
    public static final double MaxPhaseStep = 0.45;
    public static final double PhaseWrap = 64.0;
    public static final int ActionBlend = 2;
    public static final int LoopBlend = 3;
    public static final int FlinchGap = 12;
    public static final int ColonyCheck = 20;
    public static final int SeenEvery = 100;
    public static final int RestlessTicks = 40;
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp439Creature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp439Creature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp439Creature.class, EntityDataSerializers.FLOAT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation HurtPlay = RawAnimation.begin().thenPlay("hurt");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> Plays = new HashMap<>();
    protected int ActionTicks;
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
    @Nullable
    private Scp439Stride Gait;
    @Nullable
    private AnimationController<Scp439Creature> BodyController;
    @Nullable
    protected UUID HiveId;
    @Nullable
    protected BlockPos Home;
    private boolean Collapsing;
    private int Restless;

    protected Scp439Creature(EntityType<? extends Scp439Creature> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
    }

    protected abstract Scp439Stride[] strides();

    protected abstract double runEnter();

    protected abstract double runExit();

    protected abstract double idleSeconds();

    public abstract float renderScale();

    public abstract String modelName();

    @Nullable
    protected abstract String clipFor(byte Action);

    protected abstract void actionTick(byte Kind, int Elapsed);

    protected abstract int emergeTicks();

    protected abstract int deathTicks();

    protected abstract byte memberKind();

    protected abstract List<DeferredHolder<SoundEvent, SoundEvent>> idleSounds();

    protected abstract List<DeferredHolder<SoundEvent, SoundEvent>> hurtSounds();

    protected abstract List<DeferredHolder<SoundEvent, SoundEvent>> deathSounds();

    protected abstract List<DeferredHolder<SoundEvent, SoundEvent>> stepSounds();

    protected float voicePitch() {
        return 1.0f;
    }

    protected float stepVolume() {
        return 0.5f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
    }

    public static boolean isColony(@Nullable Entity Found) {
        return Found instanceof Scp439Creature || Found instanceof Scp439Hive;
    }

    @Nullable
    public UUID hiveId() {
        return HiveId;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void bindHive(Scp439Hive Hive) {
        HiveId = Hive.getUUID();
        Home = Hive.blockPosition();
        if (level() instanceof ServerLevel World && memberKind() >= 0) {
            Scp439Colonies.get(World).seeMember(getUUID(), HiveId, memberKind(), World.getGameTime());
        }
    }

    public void setHome(BlockPos Pos) {
        Home = Pos.immutable();
    }

    @Nullable
    public Scp439Hive hive() {
        if (HiveId == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(HiveId) instanceof Scp439Hive Found && Found.isAlive() ? Found : null;
    }

    public Vec3 homeCentre() {
        return Home != null ? Vec3.atBottomCenterOf(Home) : position();
    }

    public double fromHome() {
        return Home == null ? 0.0 : Math.sqrt(distanceToSqr(homeCentre()));
    }

    public void collapse() {
        if (Collapsing || !isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        Collapsing = true;
        World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 14, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.05);
        playLayers(Scp439Registry.Crumble, 0.9f, 1.0f);
        hurt(damageSources().source(Scp439Registry.Collapse), Float.MAX_VALUE);
    }

    public boolean isCollapsing() {
        return Collapsing;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isEmerging() {
        return action() == ActionEmerge;
    }

    public boolean canAct() {
        return !isActing() && isAlive() && !isDeadOrDying();
    }

    public boolean locksMovement() {
        return isActing();
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
    }

    public void endAction() {
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    public void beginEmerge() {
        setAction(ActionEmerge, emergeTicks());
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.2, getZ(), 12, getBbWidth() * 0.4, 0.1, getBbWidth() * 0.4, 0.04);
        }
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
    }

    public float lockedYaw() {
        return entityData.get(LockedYawData);
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
        if (locksMovement() && isActing()) {
            float Yaw = lockedYaw();
            setYRot(Yaw);
            yBodyRot = Yaw;
            yHeadRot = Yaw;
        }
        if (level().isClientSide) {
            gaitTick();
            Clock.settle();
            return;
        }
        if (!isAlive()) {
            return;
        }
        FlinchReady = Math.max(0, FlinchReady - 1);
        dropLostTarget();
        if (ActionTicks > 0) {
            byte Current = action();
            int Elapsed = (int) actionElapsed();
            actionTick(Current, Elapsed);
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved() && action() == Current) {
                finishAction(Current);
            }
        }
        if (tickCount % ColonyCheck == 0 && level() instanceof ServerLevel World) {
            colonyTick(World);
        }
    }

    private void dropLostTarget() {
        LivingEntity Target = getTarget();
        if (Target != null && (!Target.isAlive() || Target.isRemoved() || Target.level() != level())) {
            setTarget(null);
            Restless = RestlessTicks;
        } else if (Restless > 0) {
            Restless--;
        }
    }

    public boolean restless() {
        return Restless > 0;
    }

    protected void colonyTick(ServerLevel World) {
        if (HiveId == null) {
            return;
        }
        if (!Scp439Colonies.hiveAlive(World.getServer(), HiveId)) {
            collapse();
            return;
        }
        Scp439Colonies Colonies = Scp439Colonies.get(World);
        Colonies.hivePos(HiveId).ifPresent(Pos -> Home = Pos);
        if (memberKind() >= 0 && tickCount % SeenEvery < ColonyCheck) {
            Colonies.seeMember(getUUID(), HiveId, memberKind(), World.getGameTime());
        }
    }

    protected void finishAction(byte Kind) {
        entityData.set(ActionData, ActionNone);
    }

    private StrideGait.Shape shape() {
        if (Form == null) {
            Scp439Stride[] Strides = strides();
            Form = new StrideGait.Shape(StillPace, runEnter(), runExit(), Strides[0].distance(), Strides[1].distance(), MaxPhaseStep, LoopBlend);
        }
        return Form;
    }

    private void gaitTick() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Turn = Math.abs(Mth.wrapDegrees(yBodyRot - BodyYawO)) * Mth.DEG_TO_RAD * getBbWidth() * 0.4;
        BodyYawO = yBodyRot;
        Scp439Stride[] Strides = strides();
        boolean Stepped = Walker.tick(Math.sqrt(Dx * Dx + Dz * Dz) + Math.min(Turn, 0.1), isAlive(), shape());
        Gait = Walker.stride() == StrideGait.Idle ? null : Strides[Walker.stride()];
        IdlePhaseO = IdlePhase;
        IdlePhase += 1.0 / (idleSeconds() * 20.0);
        if (IdlePhase > PhaseWrap) {
            IdlePhase -= PhaseWrap;
            IdlePhaseO -= PhaseWrap;
        }
        if (Gait == null || !Stepped || isSilent() || isActing() || !onGround()) {
            return;
        }
        float[] Touch = Gait.touchdowns();
        for (int Limb = 0; Limb < Touch.length; Limb++) {
            if (Scp439Stride.crossed(Walker.from(), Walker.to(), Touch[Limb]) && Limb % 2 == 0) {
                footfall(Limb, Gait == Strides[1]);
            }
        }
    }

    protected void footfall(int Limb, boolean Running) {
        if (!level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 24.0)) {
            return;
        }
        float Volume = stepVolume() * (Running ? 1.1f : 0.85f) * (0.88f + random.nextFloat() * 0.12f);
        float Pitch = voicePitch() * (0.92f + random.nextFloat() * 0.12f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : stepSounds()) {
            level().playLocalSound(getX(), getY(), getZ(), Layer.get(), getSoundSource(), Volume, Pitch, false);
        }
    }

    @Nullable
    public Scp439Stride gait() {
        return Gait;
    }

    @Nullable
    public Scp439Stride strideNamed(String Clip) {
        for (Scp439Stride Stride : strides()) {
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

    public float fade(float Partial) {
        if (deathTime <= 0) {
            return 1.0f;
        }
        float Start = deathTicks() * 0.6f;
        return Mth.clamp(1.0f - (deathTime + Partial - Start) / (deathTicks() - Start), 0.0f, 1.0f);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Shift = 0.94f + random.nextFloat() * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch * Shift);
        }
    }

    public boolean strike(LivingEntity Target, float Damage) {
        if (!Target.hurt(damageSources().mobAttack(this), Damage)) {
            return false;
        }
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
        setYRot(Mth.approachDegrees(getYRot(), Yaw, Step));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public boolean canAttackType(EntityType<?> Type) {
        return Type != Scp439Registry.Queen.get() && Type != Scp439Registry.Worker.get() && Type != Scp439Registry.Brute.get() && Type != Scp439Registry.Hive.get()
                && super.canAttackType(Type);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return !isColony(Target) && super.canAttack(Target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        if (Target != null && isColony(Target)) {
            return;
        }
        super.setTarget(Target);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return isEmerging() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.getEntity() != null && isColony(Source.getEntity()) && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive() && level() instanceof ServerLevel World) {
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + getBbHeight() * 0.6, getZ(), 3, getBbWidth() * 0.3, getBbHeight() * 0.2, getBbWidth() * 0.3, 0.03);
            if (FlinchReady <= 0 && !isActing()) {
                FlinchReady = FlinchGap;
                triggerAnim("flinch", "hurt");
            }
        }
        return Hurt;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!isEmerging()) {
            super.knockback(Strength, X, Z);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World && deathTime == deathTicks() / 2) {
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + getBbHeight() * 0.4, getZ(), 10, getBbWidth() * 0.4, getBbHeight() * 0.2, getBbWidth() * 0.4, 0.04);
        }
        if (deathTime >= deathTicks() && !level().isClientSide() && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel World) {
            endAction();
            getNavigation().stop();
            forgetSelf(World);
        }
    }

    protected void forgetSelf(ServerLevel World) {
        Scp439Colonies.get(World).removeMember(getUUID());
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (Reason.shouldDestroy() && level() instanceof ServerLevel World) {
            forgetSelf(World);
        }
        super.remove(Reason);
    }

    @Override
    public void playAmbientSound() {
        if (isActing() || isSilent()) {
            return;
        }
        playLayers(idleSounds(), getSoundVolume(), voicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return idleSounds().get(0).get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(hurtSounds(), getSoundVolume(), voicePitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return hurtSounds().get(0).get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return deathSounds().get(0).get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
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
        return super.getBoundingBoxForCulling().inflate(1.0);
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
        if (HiveId != null) {
            Tag.putUUID("Hive", HiveId);
        }
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
        if (isEmerging()) {
            Tag.putInt("Emerge", ActionTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        HiveId = Tag.hasUUID("Hive") ? Tag.getUUID("Hive") : null;
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        if (Tag.getInt("Emerge") > 0) {
            setAction(ActionEmerge, Tag.getInt("Emerge"));
        }
    }

    protected RawAnimation play(String Clip) {
        return Plays.computeIfAbsent(Clip, Name -> RawAnimation.begin().thenPlayAndHold(Name));
    }

    protected void extraControllers(AnimatableManager.ControllerRegistrar Controllers) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp439Controller Body = new Scp439Controller(this, "body", Event -> {
            if (deathTime > 0 || isDeadOrDying()) {
                Event.getController().transitionLength(ActionBlend);
                return Event.setAndContinue(DeathPlay);
            }
            String Clip = actionClip();
            if (Clip != null) {
                Event.getController().transitionLength("emerge".equals(Clip) ? 0 : ActionBlend);
                return Event.setAndContinue(play(Clip));
            }
            Event.getController().transitionLength(LoopBlend);
            if (Gait != null) {
                return Event.setAndContinue(Gait.loop());
            }
            return Event.setAndContinue(IdleLoop);
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "flinch", 1, Event -> PlayState.STOP).triggerableAnim("hurt", HurtPlay));
        extraControllers(Controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
