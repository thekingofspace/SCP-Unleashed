package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class CourtDaughter extends Monster implements GeoEntity, CourtMember {
    public static final float VanillaHealth = 1000.0f;
    public static final int None = 0;
    public static final int Entrance = 1;
    public static final int Death = 2;
    public static final int Shift = 3;
    public static final int MoveA = 10;
    public static final int MoveB = 11;
    public static final int MoveC = 12;
    public static final int MoveD = 13;
    public static final int MoveE = 14;
    public static final int EntranceTicks = 70;
    public static final int DeathTicks = 80;
    public static final int SpawnBlend = 0;
    public static final int ActionBlend = 4;
    public static final int DeathBlend = 6;
    public static final int MoveBlend = 8;
    public static final int FreshTicks = 3;
    protected static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final EntityDataAccessor<Integer> ActionData = SynchedEntityData.defineId(CourtDaughter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(CourtDaughter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TwoData = SynchedEntityData.defineId(CourtDaughter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LitData = SynchedEntityData.defineId(CourtDaughter.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    protected final ServerBossEvent Bar;
    protected float Pool = 1.0f;
    protected float PoolMax = 1.0f;
    protected int Players = 1;
    protected BlockPos Dais;
    protected int ActionTime;
    protected int ActionLength;
    protected int Cooldown = 60;
    @Nullable
    protected LivingEntity ActionTarget;
    private boolean Applying;
    private boolean Ready;
    private final CourtKeeper.Watch Keeper = new CourtKeeper.Watch();
    private boolean PendingShift;
    private int SeenSerial = -1;
    private int SeenTick = -1;
    private int Retarget;

    protected CourtDaughter(EntityType<? extends Monster> Type, Level World) {
        super(Type, World);
        Bar = new ServerBossEvent(Component.translatable("boss.scp_unleashed." + courtId()), barColour(), BossEvent.BossBarOverlay.NOTCHED_10);
        Bar.setDarkenScreen(true);
        setPersistenceRequired();
        xpReward = 120;
        Dais = blockPosition();
    }

    public static AttributeSupplier.Builder attributes(double Speed) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, VanillaHealth).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 0.0).add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.FLYING_SPEED, Speed);
    }

    public abstract int index();

    public abstract String key();

    protected abstract BossEvent.BossBarColor barColour();

    protected abstract String clip(int Action);

    protected abstract void chooseMove(LivingEntity Target);

    protected abstract void moveTick(int Action, int Elapsed, @Nullable LivingEntity Target);

    protected abstract void shiftTick(int Elapsed);

    protected abstract int shiftLength();

    protected abstract void dyingTick(ServerLevel World, int Elapsed);


    protected void arenaTick(ServerLevel World) {
    }

    protected double nearRange() {
        return 4.0;
    }

    protected double farRange() {
        return 10.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, None);
        Builder.define(SerialData, 0);
        Builder.define(TwoData, false);
        Builder.define(LitData, false);
    }

    @Override
    protected void registerGoals() {
    }

    public void setup(int Count, BlockPos Centre) {
        Players = Math.max(1, Count);
        Dais = Centre.immutable();
        PoolMax = scaledPool(index(), Players);
        Pool = PoolMax;
        Ready = true;
        syncHealth();
    }

    public static float scaledPool(int Index, int Count) {
        return (float) (ScarletCourtConfig.pool(Index) * (1.0 + ScarletCourtConfig.extraPlayerShare() * Math.max(0, Count - 1)));
    }

    public float pool() {
        return Pool;
    }

    public float poolMax() {
        return PoolMax;
    }

    public void poolShare(float Share) {
        Pool = Mth.clamp(PoolMax * Share, 1.0f, PoolMax);
        syncHealth();
        checkThresholds();
    }

    public float hitCap() {
        return (float) (PoolMax * ScarletCourtConfig.hitCapShare());
    }

    public BlockPos dais() {
        return Dais;
    }

    public int players() {
        return Players;
    }

    @Override
    public String courtId() {
        return CourtIds.id(this);
    }

    public int action() {
        return entityData.get(ActionData);
    }

    public int serial() {
        return entityData.get(SerialData);
    }

    public boolean phaseTwo() {
        return entityData.get(TwoData);
    }

    protected void phaseTwo(boolean Value) {
        entityData.set(TwoData, Value);
    }

    public boolean lit() {
        return entityData.get(LitData);
    }

    protected void lit(boolean Value) {
        entityData.set(LitData, Value);
    }

    public boolean entering() {
        return action() == Entrance;
    }

    public boolean dying() {
        return action() == Death;
    }

    public void startAction(int Action, int Length) {
        entityData.set(ActionData, Action);
        entityData.set(SerialData, serial() + 1);
        ActionTime = 0;
        ActionLength = Length;
    }

    public void startMove(int Move, @Nullable LivingEntity Target) {
        ActionTarget = Target;
        if (Target != null) {
            setTarget(Target);
        }
        startAction(Move, moveLength(Move));
    }

    protected abstract int moveLength(int Move);

    public void beginEntrance() {
        startAction(Entrance, EntranceTicks);
        Bar.setVisible(true);
    }

    public void beginDying() {
        if (dying() || level().isClientSide) {
            return;
        }
        Pool = 0.0f;
        lit(false);
        startAction(Death, DeathTicks);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        Applying = true;
        try {
            super.setHealth(1.0f);
        } finally {
            Applying = false;
        }
        Bar.setProgress(0.0f);
        if (level() instanceof ServerLevel World) {
            CourtFx.sound(World, position().add(0.0, 3.0, 0.0), key() + "_death", 6.0f, 0.9f);
            CourtFx.sound(World, position().add(0.0, 3.0, 0.0), "band_snap", 3.0f, 0.6f);
            onDying(World);
        }
    }

    protected void onDying(ServerLevel World) {
    }

    protected void syncHealth() {
        Applying = true;
        try {
            super.setHealth(Pool <= 0.0f ? 1.0f : Math.max(1.0f, VanillaHealth * Pool / PoolMax));
        } finally {
            Applying = false;
        }
        updateBar();
    }

    protected void updateBar() {
        Bar.setProgress(Mth.clamp(Pool / PoolMax, 0.0f, 1.0f));
    }

    public void heal(float Amount, float Ceiling) {
        if (dying() || Pool <= 0.0f) {
            return;
        }
        Pool = Math.min(Ceiling, Pool + Amount);
        syncHealth();
    }

    @Override
    public void setHealth(float Value) {
        if (Applying || level().isClientSide || !Ready) {
            super.setHealth(Value);
            return;
        }
        float Lost = getHealth() - Value;
        if (Lost > 0.0f) {
            Pool -= Lost / VanillaHealth * PoolMax;
            if (Pool <= 0.0f) {
                beginDying();
                return;
            }
            syncHealth();
            checkThresholds();
        } else {
            super.setHealth(Value);
        }
    }

    protected void checkThresholds() {
        if (!phaseTwo() && Pool <= PoolMax * 0.5f) {
            phaseTwo(true);
            PendingShift = true;
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide) {
            return false;
        }
        if (Source.is(DamageTypes.GENERIC_KILL) || Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && Source.getEntity() == null) {
            beginDying();
            return true;
        }
        if (dying() || entering() || !Ready) {
            return false;
        }
        Entity Attacker = Source.getEntity();
        if (Attacker != null && CourtFx.allied(this, Attacker)) {
            return false;
        }
        float Scaled = Amount;
        if (Attacker == null) {
            Scaled *= (float) ScarletCourtConfig.environmentShare();
        }
        Scaled = Math.min(Scaled, hitCap());
        if (Scaled <= 0.0f) {
            return false;
        }
        return super.hurt(Source, Scaled * VanillaHealth / PoolMax);
    }

    @Override
    public void die(DamageSource Source) {
        if (!level().isClientSide && !dying()) {
            beginDying();
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide) {
            discard();
        }
    }

    public CourtKeeper.Watch keeper() {
        return Keeper;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        int Action = action();
        if (Ready && Action != Death && Action != Entrance) {
            LivingEntity Chased = getTarget();
            boolean Wants = Action == None && (getNavigation().isInProgress() || Chased != null && distanceTo(Chased) > nearRange() + 1.0);
            CourtKeeper.keep(World, this, Dais, Keeper, Wants);
        }
        if (Action != None) {
            ActionTime++;
            if (Action == Death) {
                dyingTick(World, ActionTime);
                if (ActionTime >= ActionLength) {
                    finishDeath(World);
                }
                return;
            }
            if (Action == Entrance) {
                entranceTick(World, ActionTime);
            } else if (Action == Shift) {
                shiftTick(ActionTime);
            } else {
                LivingEntity Target = ActionTarget != null && ActionTarget.isAlive() ? ActionTarget : getTarget();
                moveTick(Action, ActionTime, Target);
            }
            if (ActionTime >= ActionLength && action() == Action) {
                startActionNone();
            }
            arenaTick(World);
            return;
        }
        arenaTick(World);
        if (action() != None) {
            return;
        }
        if (PendingShift) {
            PendingShift = false;
            startAction(Shift, shiftLength());
            CourtFx.sound(World, position().add(0.0, 3.0, 0.0), key() + "_idle", 6.0f, 0.6f);
            return;
        }
        LivingEntity Target = acquire();
        steer(Target);
        if (Target != null && --Cooldown <= 0) {
            ActionTarget = Target;
            chooseMove(Target);
        }
    }

    private void startActionNone() {
        entityData.set(ActionData, None);
        ActionTime = 0;
        ActionTarget = null;
    }

    protected void entranceTick(ServerLevel World, int Elapsed) {
        if (Elapsed % 6 == 0) {
            CourtFx.burst(World, CourtFx.DeepCrimson, position().add(0.0, 1.0, 0.0), 10, 1.2, 0.02);
            BlockState Under = CourtFx.solidAt(World, blockPosition().below());
            CourtFx.crumble(World, Under, position(), 12, 1.4, 0.1);
        }
        if (Elapsed == 2) {
            CourtFx.sound(World, position(), key() + "_idle", 6.0f, 0.7f);
        }
    }

    protected void finishDeath(ServerLevel World) {
        ScarletCourt.Instance.daughterFallen(this);
        Bar.removeAllPlayers();
        discard();
    }

    @Nullable
    protected LivingEntity acquire() {
        LivingEntity Current = getTarget();
        if (Current != null && CourtFx.validTarget(Current) && Current.distanceToSqr(this) < 64.0 * 64.0 && --Retarget > 0) {
            return Current;
        }
        Retarget = 20;
        Player Best = null;
        double Closest = Double.MAX_VALUE;
        double Range = CourtArena.playerRange() + 16.0;
        for (Player Each : level().getEntitiesOfClass(Player.class, new AABB(Dais).inflate(Range, 32.0, Range), CourtFx::validTarget)) {
            double Distance = Each.distanceToSqr(this);
            if (Distance < Closest) {
                Closest = Distance;
                Best = Each;
            }
        }
        if (Best != null) {
            setTarget(Best);
            return Best;
        }
        return Current != null && CourtFx.validTarget(Current) ? Current : null;
    }

    protected void steer(@Nullable LivingEntity Target) {
        Vec3 Centre = Vec3.atBottomCenterOf(Dais);
        double FromCentre = Math.sqrt(Math.pow(getX() - Centre.x, 2) + Math.pow(getZ() - Centre.z, 2));
        if (FromCentre > CourtArena.floorRadius() - 3) {
            getNavigation().moveTo(Centre.x, Centre.y, Centre.z, 1.0);
            return;
        }
        if (Target == null) {
            getNavigation().stop();
            return;
        }
        getLookControl().setLookAt(Target, 20.0f, 20.0f);
        double Distance = distanceTo(Target);
        if (Distance > farRange()) {
            if (tickCount % 10 == 0) {
                getNavigation().moveTo(Target, 1.0);
            }
        } else if (Distance < nearRange()) {
            getNavigation().stop();
        } else if (tickCount % 20 == 0) {
            getNavigation().stop();
        }
    }

    protected void face(@Nullable LivingEntity Target) {
        if (Target != null) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            Vec3 Way = Target.position().subtract(position());
            float Yaw = (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Mth.approachDegrees(getYRot(), Yaw, 12.0f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    protected Vec3 facing() {
        return Vec3.directionFromRotation(0.0f, yBodyRot);
    }

    protected Vec3 eyes() {
        return position().add(0.0, getBbHeight() * 0.8, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ScarletCourtSounds.get(key() + "_idle");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return ScarletCourtSounds.get(key() + "_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer User) {
        super.startSeenByPlayer(User);
        Bar.addPlayer(User);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer User) {
        super.stopSeenByPlayer(User);
        Bar.removePlayer(User);
    }

    @Override
    public void remove(RemovalReason Reason) {
        Bar.removeAllPlayers();
        super.remove(Reason);
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return CourtFx.allied(this, Other) || super.isAlliedTo(Other);
    }

    @Override
    public boolean canAttack(LivingEntity Other) {
        return CourtFx.validTarget(Other) && super.canAttack(Other);
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6.0, 4.0, 6.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putFloat("Pool", Pool);
        Tag.putFloat("PoolMax", PoolMax);
        Tag.putInt("Players", Players);
        Tag.putLong("Dais", Dais.asLong());
        Tag.putBoolean("PhaseTwo", phaseTwo());
        Tag.putBoolean("Ready", Ready);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        PoolMax = Math.max(1.0f, Tag.getFloat("PoolMax"));
        Pool = Tag.getFloat("Pool");
        Players = Math.max(1, Tag.getInt("Players"));
        Dais = BlockPos.of(Tag.getLong("Dais"));
        phaseTwo(Tag.getBoolean("PhaseTwo"));
        Ready = Tag.getBoolean("Ready");
        if (hasCustomName()) {
            Bar.setName(getDisplayName());
        }
        if (Ready) {
            syncHealth();
        }
    }

    protected RawAnimation moving() {
        return WalkLoop;
    }

    protected boolean holds(int Action) {
        return Action == Death;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", MoveBlend, State -> {
            CourtDaughter Owner = State.getAnimatable();
            if (Owner.SeenTick < 0) {
                Owner.SeenTick = Owner.tickCount;
            }
            boolean Fresh = Owner.tickCount - Owner.SeenTick < FreshTicks;
            if (Owner.serial() != Owner.SeenSerial) {
                boolean First = Owner.SeenSerial < 0;
                Owner.SeenSerial = Owner.serial();
                if (!First) {
                    State.getController().forceAnimationReset();
                }
            }
            int Action = Owner.action();
            if (Action != None) {
                String Clip = Owner.clip(Action);
                if (Clip != null) {
                    State.getController().transitionLength(Fresh || Action == Entrance ? SpawnBlend : Action == Death ? DeathBlend : ActionBlend);
                    return State.setAndContinue(Owner.holds(Action) ? RawAnimation.begin().thenPlayAndHold(Clip) : RawAnimation.begin().thenPlay(Clip).thenLoop("idle"));
                }
            }
            State.getController().transitionLength(Fresh ? SpawnBlend : MoveBlend);
            if (State.isMoving()) {
                return State.setAndContinue(Owner.moving());
            }
            return State.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
