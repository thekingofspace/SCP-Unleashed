package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class Scp016Spawn extends Monster implements GeoEntity {
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionStrike = 2;
    public static final int Lifespan = 12000;
    public static final int CollapseWarning = 200;
    public static final int CheckTicks = 20;
    public static final int EmergeTicks = 30;
    public static final double AlertRange = 16.0;
    public static final double Follow = 32.0;
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp016Spawn.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> Collapsing = SynchedEntityData.defineId(Scp016Spawn.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> HostData = SynchedEntityData.defineId(Scp016Spawn.class, EntityDataSerializers.BYTE);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private long BornAt = -1L;
    protected int ActionTicks;
    @Nullable
    private Scp016Gait Gait;

    protected Scp016Spawn(EntityType<? extends Scp016Spawn> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
    }

    public abstract String variant();

    protected abstract Scp016Gait.Profile gaitProfile();

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(Collapsing, false);
        Builder.define(HostData, (byte) Scp016Host.Unset);
    }

    public int host() {
        return Scp016Host.clamp(entityData.get(HostData));
    }

    public void setHost(int Code) {
        entityData.set(HostData, (byte) Scp016Host.clamp(Code));
    }

    public boolean hasDeathAnimation() {
        return false;
    }

    public float bodyPitch(float Partial) {
        return 0.0f;
    }

    public float bodyRoll(float Partial) {
        return 0.0f;
    }

    protected void registerKinGoals() {
        registerKinGoals(true, true);
    }

    protected void registerKinGoals(boolean Floats, boolean HuntsPlayers) {
        if (Floats) {
            goalSelector.addGoal(0, new FloatGoal(this));
        }
        goalSelector.addGoal(1, new Scp016Goals.HoldStill(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (HuntsPlayers) {
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::wantsQuarry));
        }
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, 10, true, false, this::wantsQuarry));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    public boolean wantsQuarry(LivingEntity Target) {
        return isQuarry(Target);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean locksMovement() {
        return isActing();
    }

    public boolean isEmerging() {
        return action() == ActionEmerge;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    public void beginEmerging() {
        setAction(ActionEmerge, EmergeTicks);
    }

    public boolean isCollapsing() {
        return entityData.get(Collapsing);
    }

    public int getRemaining() {
        if (BornAt < 0L) {
            return Lifespan;
        }
        return (int) Math.max(0L, Lifespan - (level().getGameTime() - BornAt));
    }

    @Nullable
    public Scp016Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp016Gait(gaitProfile());
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp016Gait Current = gait();
        return Current != null && Current.striding();
    }

    public void setGaitPartial(float Partial) {
        Scp016Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    public static boolean isKin(@Nullable Entity Other) {
        return Other instanceof Scp016Spawn || Other instanceof LivingEntity Living && Scp016Affliction.isAfflicted(Living);
    }

    public static boolean isQuarry(LivingEntity Target) {
        return Target.isAlive() && !isKin(Target) && Targeting.isTargetable(Target);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setCanPickUpLoot(false);
        if (entityData.get(HostData) == Scp016Host.Unset) {
            setHost(Scp016Host.random(random));
        }
        BornAt = World.getLevel().getGameTime();
        return Result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp016Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (isAlive() && ActionTicks > 0) {
            byte Current = action();
            actionTick(Current, ActionTicks);
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive()) {
                entityData.set(ActionData, ActionNone);
                actionFinished(Current);
            }
        }
    }

    protected void actionTick(byte Kind, int Remaining) {
    }

    protected void actionFinished(byte Kind) {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        long Now = level().getGameTime();
        if (BornAt < 0L) {
            BornAt = Now;
        }
        if ((tickCount + getId()) % CheckTicks != 0) {
            return;
        }
        long Age = Now - BornAt;
        if (Age >= Lifespan) {
            Scp016Events.collapse(this);
            return;
        }
        if (Age >= Lifespan - CollapseWarning && !isCollapsing()) {
            entityData.set(Collapsing, true);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Landed = super.doHurtTarget(Target);
        if (Landed && !level().isClientSide && Target instanceof LivingEntity Victim) {
            Scp016Affliction.afflict(Victim);
        }
        return Landed;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && Source.getEntity() instanceof LivingEntity Attacker && Attacker.isAlive() && !(Attacker instanceof Scp016Spawn)) {
            if (!(Attacker instanceof Player User) || !User.isCreative() && !User.isSpectator()) {
                alertKin(Attacker);
            }
        }
        return Hurt;
    }

    protected void alertKin(LivingEntity Attacker) {
        for (Scp016Spawn Kin : level().getEntitiesOfClass(Scp016Spawn.class, getBoundingBox().inflate(AlertRange))) {
            if (Kin != this && Kin.getTarget() == null && Kin.isAlive() && !Kin.isEmerging()) {
                Kin.setTarget(Attacker);
            }
        }
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp016Spawn || super.isAlliedTo(Other);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return !(Target instanceof Scp016Spawn) && super.canAttack(Target);
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
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putLong("BornAt", BornAt);
        Tag.putByte("Action", action());
        Tag.putInt("ActionTicks", ActionTicks);
        Tag.putByte("Host", (byte) host());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        BornAt = Tag.contains("BornAt") ? Tag.getLong("BornAt") : -1L;
        setHost(Tag.contains("Host") ? Tag.getByte("Host") : Scp016Host.random(random));
        int Ticks = Tag.getInt("ActionTicks");
        byte Saved = Tag.getByte("Action");
        entityData.set(ActionData, Ticks > 0 && Saved != ActionStrike ? Saved : ActionNone);
        ActionTicks = Ticks > 0 && Saved != ActionStrike ? Ticks : 0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
