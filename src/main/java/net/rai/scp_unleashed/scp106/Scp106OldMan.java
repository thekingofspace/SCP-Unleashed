package net.rai.scp_unleashed.scp106;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.advmaterials.MagnetizedDoorBlock;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Scp106OldMan extends Monster implements GeoEntity {
    public static final float Width = 0.7f;
    public static final float Height = 2.2f;
    public static final float EyeHeight = 2.0f;
    public static final double MaxHealth = 250.0;
    public static final double Armour = 12.0;
    public static final double Toughness = 4.0;
    public static final double Follow = 48.0;
    public static final float DamageFactor = 0.4f;
    public static final double Pace = 0.055;
    public static final double ChaseWarp = 24.0;
    public static final int ChaseCooldownMin = 400;
    public static final int ChaseCooldownMax = 800;
    public static final double ReturnRange = 128.0;
    public static final double VerticalSpeed = 0.07;
    public static final double Gravity = 0.06;
    public static final double MaxFall = 0.6;
    public static final double StepRise = 0.14;
    public static final double SenseRange = 40.0;
    public static final double KeepRange = 50.0;
    public static final double TouchReach = 0.35;
    public static final double GrabReach = 0.9;
    public static final int GrabTicks = 12;
    public static final int GrabContact = 6;
    public static final float TouchDamage = 4.0f;
    public static final double WarpMin = 20.0;
    public static final double WarpMax = 96.0;
    public static final int WarpCooldownMin = 1200;
    public static final int WarpCooldownMax = 2400;
    public static final int FirstWarpDelay = 400;
    public static final int FailedWarpDelay = 100;
    public static final int SinkTicks = 36;
    public static final int RiseTicks = 40;
    public static final int HiddenMin = 20;
    public static final int HiddenMax = 50;
    public static final double SinkDepth = 2.6;
    public static final int LureSinkTicks = 16;
    public static final int LureHiddenTicks = 8;
    public static final int LureRiseTicks = 24;
    public static final int BreathMin = 90;
    public static final int BreathMax = 170;
    public static final int LaughHuntMin = 240;
    public static final int LaughHuntMax = 600;
    public static final int LaughIdleMin = 500;
    public static final int LaughIdleMax = 1200;
    public static final int ContainCheckEvery = 100;
    public static final int ContainReach = 12;
    public static final int BreachGrace = 12000;
    public static final float BreachChance = 1.0f / 360.0f;
    public static final int BreachTicks = 600;
    public static final int BreachCooldown = 6000;
    public static final double BreachWarnRange = 32.0;
    public static final int SatedCapture = 400;
    public static final int SatedLure = 1800;
    public static final int RetreatMin = 2400;
    public static final int RetreatMax = 4800;
    public static final int RegenDelay = 60;
    public static final int RegenEvery = 10;
    public static final float RegenAmount = 2.0f;
    public static final float RegenRested = 6.0f;
    public static final int EchoFirst = 6;
    public static final int EchoSecond = 13;
    public static final float EchoFirstVolume = 0.5f;
    public static final float EchoSecondVolume = 0.28f;
    public static final int WanderMin = 160;
    public static final int WanderMax = 320;
    public static final int WanderRadius = 10;
    public static final int TargetEvery = 10;
    public static final int RegroupTicks = 200;
    public static final int RegroupGap = 100;
    public static final double RedirectDrift = 10.0;
    public static final int EmergeAttempts = 24;
    public static final byte ActionNone = 0;
    public static final byte ActionSink = 1;
    public static final byte ActionRise = 2;
    public static final byte ActionGrab = 3;
    public static final byte ActionCorrode = 4;
    public static final byte ActionHidden = 5;
    public static final byte ActionStepOut = 6;
    public static final byte ActionDescend = 7;
    public static final int StepOutTicks = 44;
    public static final int StepOutHold = 6;
    public static final int StepOutMove = 32;
    public static final double StepOutDistance = 1.1;
    public static final int DescendLower = 32;
    public static final int DescendDrop = 8;
    public static final int DescendTicks = 56;
    public static final double DescendStart = 0.1;
    public static final double DescendHang = 2.3;
    public static final int PortalLinger = 30;
    public static final byte ModeWander = 0;
    public static final byte ModeHunt = 1;
    public static final byte ModeSated = 2;
    public static final byte ModeContained = 3;
    public static final byte ModeBreaching = 4;
    public static final byte ModeEmerging = 5;
    public static final byte LaughEvent = 60;
    public static final byte BreathEvent = 61;
    public static final String[] ActionClips = {"", "sink", "rise", "grab", "corrode", "idle", "emerge_wall", "emerge_ceiling"};
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation SinkPlay = RawAnimation.begin().thenPlayAndHold("sink");
    public static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    public static final RawAnimation GrabPlay = RawAnimation.begin().thenPlayAndHold("grab");
    public static final RawAnimation CorrodeLoop = RawAnimation.begin().thenLoop("corrode");
    public static final RawAnimation LaughPlay = RawAnimation.begin().thenPlayAndHold("laugh");
    public static final RawAnimation BreathePlay = RawAnimation.begin().thenPlayAndHold("breathe");
    public static final RawAnimation QuietLoop = RawAnimation.begin().thenLoop("quiet");
    public static final RawAnimation StepOutPlay = RawAnimation.begin().thenPlayAndHold("emerge_wall");
    public static final RawAnimation DescendPlay = RawAnimation.begin().thenPlayAndHold("emerge_ceiling");
    public static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    public static final int LaughTicks = 60;
    public static final int BreatheTicks = 52;
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp106OldMan.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp106OldMan.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(Scp106OldMan.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionStart;
    private int ActionLength;
    private Vec3 Steer = Vec3.ZERO;
    private double Lift = Double.NaN;
    private double FallSpeed;
    @Nullable
    private LivingEntity Target;
    @Nullable
    private UUID LureVictim;
    @Nullable
    private Vec3 WarpDest;
    private double SurfaceY = Double.NaN;
    private boolean Retreating;
    private boolean Lured;
    private int WarpCooldown = FirstWarpDelay;
    private int ContainedTicks;
    private int BreachWait;
    private int Sated;
    private int BreathTimer = BreathMin;
    private int LaughTimer = LaughIdleMin;
    private int WanderTimer;
    @Nullable
    private Vec3 WanderGoal;
    private int LastHurt = -10000;
    private boolean InsideWall;
    private final List<BlockPos> BreachBlocks = new ArrayList<>();
    @Nullable
    private Vec3 BreachStand;
    @Nullable
    private Direction BreachFacing;
    private boolean BreachDoor;
    private int LocalActionStart;
    private int LocalLaugh = -1000;
    private int LocalBreath = -1000;
    private final List<Echo> Echoes = new ArrayList<>();
    private float RenderPartial;
    @Nullable
    private Scp106Gait Gait;
    @Nullable
    private Vec3 ArrivalStart;
    @Nullable
    private Vec3 ArrivalEnd;
    private double ArrivalCeiling = Double.NaN;
    private boolean Chasing;
    private boolean Erased;
    private int RetreatTicks = RetreatMin;
    @Nullable
    private UUID LastTarget;
    private int Regroup;
    private int LastWarp = -10000;
    private boolean Commanded;
    @Nullable
    private Scp106Arrival.Style ForcedStyle;

    public Scp106OldMan(EntityType<? extends Scp106OldMan> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.ATTACK_DAMAGE, TouchDamage)
                .add(Attributes.SAFE_FALL_DISTANCE, 1024.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ModeData, ModeWander);
        Builder.define(SerialData, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(ActionData) || Key.equals(SerialData)) {
            LocalActionStart = tickCount;
        }
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    private void setMode(byte Value) {
        if (mode() != Value) {
            entityData.set(ModeData, Value);
        }
    }

    private void startAction(byte Kind, int Length) {
        ActionStart = tickCount;
        ActionLength = Length;
        entityData.set(ActionData, Kind);
        entityData.set(SerialData, entityData.get(SerialData) + 1);
    }

    private int elapsed() {
        return tickCount - ActionStart;
    }

    @Nullable
    public String actionClip() {
        byte Kind = action();
        return Kind > ActionNone && Kind < ActionClips.length ? ActionClips[Kind] : null;
    }

    public double actionElapsed() {
        return (tickCount - LocalActionStart + RenderPartial) / 20.0;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        if (Gait != null) {
            Gait.setPartial(Partial);
        }
    }

    @Nullable
    public Scp106Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp106Gait();
        }
        return Gait;
    }

    public boolean laughing() {
        return tickCount - LocalLaugh < LaughTicks;
    }

    public boolean breathing() {
        return tickCount - LocalBreath < BreatheTicks;
    }

    public double voiceElapsed() {
        int Start = laughing() ? LocalLaugh : LocalBreath;
        return (tickCount - Start + RenderPartial) / 20.0;
    }

    public boolean hidden() {
        return action() == ActionHidden;
    }

    public boolean isContained() {
        return ContainedTicks > 0;
    }

    public int containedTicks() {
        return ContainedTicks;
    }

    public int warpCooldown() {
        return WarpCooldown;
    }

    public int satedTicks() {
        return Sated;
    }

    public boolean breaching() {
        return !BreachBlocks.isEmpty();
    }

    @Nullable
    public LivingEntity target() {
        return Target;
    }

    public static boolean huntable(Level World, LivingEntity Candidate) {
        return Scp106Targeting.valid(World, Candidate);
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        if (level().isClientSide) {
            tickClient();
        }
    }

    private void tickClient() {
        Scp106Gait Walking = gait();
        if (Walking != null) {
            Walking.tick(this);
        }
        if (hidden() || isInvisible()) {
            return;
        }
        RandomSource Random = getRandom();
        boolean Inside = Scp106Phasing.solid(level(), BlockPos.containing(getX(), getY() + 1.0, getZ()));
        float Chance = Inside ? 0.6f : 0.14f;
        if (Random.nextFloat() < Chance) {
            AABB Box = getBoundingBox();
            double X = Box.minX + Random.nextDouble() * (Box.maxX - Box.minX);
            double Y = Box.minY + 0.3 + Random.nextDouble() * (Box.maxY - Box.minY - 0.5);
            double Z = Box.minZ + Random.nextDouble() * (Box.maxZ - Box.minZ);
            level().addParticle(Scp106Registry.Goo.get(), X, Y, Z, 0.0, -0.02, 0.0);
        }
        byte Kind = action();
        if (Kind == ActionDescend && Random.nextFloat() < 0.6f) {
            level().addParticle(Scp106Registry.Goo.get(), getX() + (Random.nextDouble() - 0.5) * 1.2, getY() + 0.4 + Random.nextDouble() * 1.8,
                    getZ() + (Random.nextDouble() - 0.5) * 1.2, 0.0, -0.08, 0.0);
        }
        if (Kind == ActionStepOut && Random.nextFloat() < 0.6f) {
            level().addParticle(Scp106Registry.Goo.get(), getX() + (Random.nextDouble() - 0.5) * 1.0, getY() + 0.2 + Random.nextDouble() * 2.0,
                    getZ() + (Random.nextDouble() - 0.5) * 1.0, 0.0, -0.03, 0.0);
        }
        if ((Kind == ActionSink || Kind == ActionRise) && Random.nextFloat() < 0.5f) {
            level().addParticle(Scp106Registry.Goo.get(), getX() + (Random.nextDouble() - 0.5) * 1.4, getY() + SinkDepth * 0.2 + Random.nextDouble() * 0.6,
                    getZ() + (Random.nextDouble() - 0.5) * 1.4, (Random.nextDouble() - 0.5) * 0.06, 0.08, (Random.nextDouble() - 0.5) * 0.06);
        }
    }

    @Override
    public void handleEntityEvent(byte Event) {
        if (Event == LaughEvent) {
            LocalLaugh = tickCount;
            return;
        }
        if (Event == BreathEvent) {
            LocalBreath = tickCount;
            return;
        }
        super.handleEntityEvent(Event);
    }

    @Override
    protected void customServerAiStep() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Steer = Vec3.ZERO;
        Lift = Double.NaN;
        if (WarpCooldown > 0) {
            WarpCooldown--;
        }
        if (BreachWait > 0) {
            BreachWait--;
        }
        if (Sated > 0) {
            Sated--;
        }
        switch (action()) {
            case ActionSink -> tickSink(World);
            case ActionHidden -> tickHidden(World);
            case ActionRise -> tickRise(World);
            case ActionStepOut -> tickStepOut(World);
            case ActionDescend -> tickDescend(World);
            case ActionGrab -> tickGrab(World);
            case ActionCorrode -> tickCorrode(World);
            default -> tickFree(World);
        }
        if (!hidden()) {
            tickVoice(World);
            if (tickCount % ContainCheckEvery == 0) {
                checkContainment(World);
            }
        }
        if (tickCount % RegenEvery == 0 && getHealth() < getMaxHealth()) {
            heal(tickCount - LastHurt > RegenDelay ? RegenRested : RegenAmount);
        }
        tickEchoes(World);
        updateMode();
    }

    private void updateMode() {
        byte Kind = action();
        if (arriving() || Kind == ActionSink || Kind == ActionHidden) {
            setMode(ModeEmerging);
        } else if (breaching()) {
            setMode(ModeBreaching);
        } else if (Sated > 0) {
            setMode(ModeSated);
        } else if (isContained()) {
            setMode(ModeContained);
        } else if (Target != null) {
            setMode(ModeHunt);
        } else {
            setMode(ModeWander);
        }
    }

    private void tickFree(ServerLevel World) {
        if (breaching()) {
            tickBreachApproach(World);
            return;
        }
        if (Sated > 0) {
            Target = null;
            wander(World);
            return;
        }
        boolean Lost = Target != null && !keep(Target);
        boolean Vanished = Lost && !Scp106Targeting.valid(World, Target);
        if (Lost || tickCount % TargetEvery == 0) {
            Target = findTarget(World, Lost ? null : Target);
        }
        if (Vanished && Target == null) {
            Regroup = RegroupTicks;
        }
        if (Target != null) {
            Regroup = 0;
            hunt(World, Target);
            return;
        }
        if (Regroup > 0) {
            Regroup--;
            if (tickCount % 20 == 0 && tickCount - LastWarp >= RegroupGap && tryWarp(World, null, true)) {
                Regroup = 0;
                return;
            }
        } else if (tickCount % 20 == 0 && tryWarp(World, null, false)) {
            return;
        }
        wander(World);
    }

    private boolean keep(LivingEntity Candidate) {
        return Scp106Targeting.valid(level(), Candidate) && Candidate.distanceToSqr(getEyePosition()) <= KeepRange * KeepRange;
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel World, @Nullable LivingEntity Current) {
        return Scp106Targeting.choose(World, getEyePosition(), SenseRange, KeepRange, 0.0, Current, Scp106Targeting.Stickiness, true, false);
    }

    private void hunt(ServerLevel World, LivingEntity Quarry) {
        getLookControl().setLookAt(Quarry, 30.0f, 30.0f);
        if (getBoundingBox().inflate(TouchReach).intersects(Quarry.getBoundingBox())) {
            startAction(ActionGrab, GrabTicks);
            World.playSound(null, getX(), getEyeY(), getZ(), Scp106Registry.Touch.get(), SoundSource.HOSTILE, 1.0f, 0.9f + getRandom().nextFloat() * 0.15f);
            return;
        }
        LastTarget = Quarry.getUUID();
        double Distance = Quarry.distanceTo(this);
        if (Distance > ChaseWarp && tickCount % 20 == 0 && tryWarp(World, Quarry, false)) {
            return;
        }
        steerTowards(Quarry.position(), Pace, true);
    }

    private void steerTowards(Vec3 Goal, double Speed, boolean ThreeD) {
        double Dx = Goal.x - getX();
        double Dz = Goal.z - getZ();
        double Dy = Goal.y - getY();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Flat > 0.25) {
            double Scale = Math.min(Speed, Flat) / Flat;
            Steer = new Vec3(Dx * Scale, 0.0, Dz * Scale);
            float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Mth.approachDegrees(getYRot(), Yaw, 9.0f));
            yBodyRot = getYRot();
        }
        if (ThreeD && Math.abs(Dy) > 1.1 && (Dy < 0.0 || againstCeiling())) {
            Lift = Math.signum(Dy) * Math.min(VerticalSpeed, Math.abs(Dy));
        }
    }

    private boolean againstCeiling() {
        for (int Dy = 0; Dy <= 3; Dy++) {
            if (Scp106Phasing.solid(level(), BlockPos.containing(getX(), getY() + 0.5 + Dy, getZ()))) {
                return true;
            }
        }
        return false;
    }

    private void wander(ServerLevel World) {
        if (WanderTimer > 0) {
            WanderTimer--;
        }
        if (WanderTimer <= 0) {
            RandomSource Random = getRandom();
            WanderTimer = WanderMin + Random.nextInt(WanderMax - WanderMin + 1);
            if (Random.nextFloat() < 0.35f) {
                WanderGoal = null;
            } else {
                double Angle = Random.nextDouble() * Mth.TWO_PI;
                double Radius = 3.0 + Random.nextDouble() * (WanderRadius - 3.0);
                WanderGoal = new Vec3(getX() + Math.cos(Angle) * Radius, getY(), getZ() + Math.sin(Angle) * Radius);
            }
        }
        if (WanderGoal != null) {
            if (WanderGoal.distanceToSqr(getX(), WanderGoal.y, getZ()) < 0.8) {
                WanderGoal = null;
            } else {
                steerTowards(WanderGoal, Pace, false);
                getLookControl().setLookAt(WanderGoal.x, getEyeY(), WanderGoal.z);
            }
        }
    }

    private boolean canWarp(ServerLevel World) {
        return WarpCooldown <= 0 && Sated <= 0 && !breaching() && !anchored();
    }

    public boolean anchored() {
        return Scp106Phasing.standingOnMagnet(level(), getBoundingBox());
    }

    private boolean tryWarp(ServerLevel World, @Nullable LivingEntity Quarry, boolean Regrouping) {
        if (Regrouping ? Sated > 0 || breaching() || anchored() : !canWarp(World)) {
            return false;
        }
        LivingEntity Chosen = Quarry;
        if (Chosen == null) {
            Chosen = Scp106Targeting.choose(World, position(), WarpMax, WarpMax, WarpMin, null, 1.0, false, true);
        }
        if (Chosen == null || !Scp106Targeting.valid(World, Chosen) || Chosen.distanceTo(this) > WarpMax) {
            return false;
        }
        Vec3 Spot = emergence(World, Chosen);
        if (Spot == null) {
            WarpCooldown = FailedWarpDelay;
            return false;
        }
        Target = Chosen;
        LastTarget = Chosen.getUUID();
        Chasing = Quarry != null || Regrouping;
        beginWarp(World, Spot, SinkTicks);
        return true;
    }

    @Nullable
    public Vec3 emergence(ServerLevel World, LivingEntity Quarry) {
        Vec3 Found = emergenceNear(World, Quarry, getRandom());
        return Found != null && net.rai.scp_unleashed.scp001gate.Scp001GateZones.forbidden(World, Found) ? null : Found;
    }

    @Nullable
    public static Vec3 emergenceNear(ServerLevel World, LivingEntity Quarry, RandomSource Random) {
        BlockPos Base = Quarry.blockPosition();
        Vec3 Eye = Quarry.getEyePosition();
        for (int Attempt = 0; Attempt < EmergeAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = 2.5 + Random.nextDouble() * 2.5;
            BlockPos Column = Base.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            for (int Dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos Feet = Column.above(Dy);
                if (!World.isLoaded(Feet) || !openFor(World, Feet)) {
                    continue;
                }
                Vec3 Spot = Vec3.atBottomCenterOf(Feet);
                if (Scp106Phasing.lineHeld(World, Spot.add(0.0, 1.2, 0.0), Eye)) {
                    continue;
                }
                return Spot;
            }
        }
        return null;
    }

    public static boolean openFor(ServerLevel World, BlockPos Feet) {
        BlockPos Below = Feet.below();
        BlockState Floor = World.getBlockState(Below);
        if (!Floor.isFaceSturdy(World, Below, Direction.UP) || Scp106Phasing.isMagnetized(Floor) || Floor.getBlock() instanceof MagnetizedDoorBlock) {
            return false;
        }
        for (int Dy = 0; Dy < 3; Dy++) {
            BlockPos Cell = Feet.above(Dy);
            if (Scp106Phasing.solid(World, Cell) || !World.getFluidState(Cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void beginWarp(ServerLevel World, Vec3 Spot, int Ticks) {
        WarpDest = Spot;
        LastWarp = tickCount;
        cancelBreach(World);
        SurfaceY = getY();
        startAction(ActionSink, Ticks);
        Scp106Effects.pool(World, position());
        Scp106Effects.layered(World, this, Scp106Registry.Sink, Scp106Registry.BreathWet, 1.0f, 1.0f);
    }

    private void tickSink(ServerLevel World) {
        Steer = Vec3.ZERO;
        if (elapsed() < ActionLength) {
            return;
        }
        setInvisible(true);
        if (Retreating) {
            startAction(ActionHidden, RetreatTicks);
            return;
        }
        redirect(World);
        if (WarpDest != null) {
            SurfaceY = WarpDest.y;
            moveTo(WarpDest.x, WarpDest.y - SinkDepth, WarpDest.z, getYRot(), getXRot());
        }
        startAction(ActionHidden, Lured ? LureHiddenTicks : HiddenMin + getRandom().nextInt(HiddenMax - HiddenMin + 1));
    }

    private void redirect(ServerLevel World) {
        if (Lured || Retreating || Commanded || WarpDest == null) {
            return;
        }
        boolean Valid = Scp106Targeting.valid(World, Target);
        if (Valid && Target.distanceToSqr(WarpDest) <= RedirectDrift * RedirectDrift) {
            return;
        }
        LivingEntity Next = Valid ? Target : Scp106Targeting.choose(World, WarpDest, WarpMax, WarpMax, 0.0, null, 1.0, false, true);
        if (Next == null) {
            Next = Scp106Targeting.choose(World, WarpDest, SenseRange, SenseRange, 0.0, null, 1.0, false, false);
        }
        Target = Next;
        if (Next == null) {
            return;
        }
        LastTarget = Next.getUUID();
        Vec3 Spot = emergence(World, Next);
        if (Spot != null) {
            WarpDest = Spot;
        }
    }

    private void tickHidden(ServerLevel World) {
        Steer = Vec3.ZERO;
        if (elapsed() < ActionLength) {
            return;
        }
        if (Retreating) {
            Retreating = false;
            setHealth(getMaxHealth());
            LivingEntity Quarry = returnQuarry(World);
            Vec3 Spot = Quarry != null ? emergence(World, Quarry) : null;
            if (Spot != null) {
                SurfaceY = Spot.y;
                moveTo(Spot.x, Spot.y - SinkDepth, Spot.z, getYRot(), getXRot());
                Target = Quarry;
            }
            Scp106ReturnLedger.get(World.getServer()).complete(getUUID());
        } else if (WarpDest != null) {
            Vec3 Was = WarpDest;
            redirect(World);
            if (WarpDest != Was) {
                SurfaceY = WarpDest.y;
                moveTo(WarpDest.x, WarpDest.y - SinkDepth, WarpDest.z, getYRot(), getXRot());
            }
        }
        if (Double.isNaN(SurfaceY)) {
            SurfaceY = getY() + SinkDepth;
        }
        if (Target != null) {
            float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Yaw);
            yBodyRot = Yaw;
            yHeadRot = Yaw;
        }
        setInvisible(false);
        Vec3 Surface = new Vec3(getX(), SurfaceY, getZ());
        Scp106Arrival.Plan Plan = Lured ? Scp106Arrival.floor(Surface) : Scp106Arrival.choose(World, Surface, getRandom(), ForcedStyle);
        ForcedStyle = null;
        switch (Plan.Kind()) {
            case Wall -> startStepOut(World, Plan);
            case Ceiling -> startDescend(World, Plan);
            default -> {
                startAction(ActionRise, Lured ? LureRiseTicks : RiseTicks);
                Scp106Effects.pool(World, Surface);
                World.playSound(null, Surface.x, Surface.y, Surface.z, Scp106Registry.Rise.get(), SoundSource.HOSTILE, 1.2f, 0.85f + getRandom().nextFloat() * 0.1f);
            }
        }
        laugh(World);
    }

    @Nullable
    private LivingEntity returnQuarry(ServerLevel World) {
        LivingEntity Last = LastTarget != null && World.getEntity(LastTarget) instanceof LivingEntity Found ? Found : null;
        LivingEntity Best = Scp106Targeting.choose(World, position(), ReturnRange, ReturnRange, 0.0, Last, Double.POSITIVE_INFINITY, false, true);
        return Best != null ? Best : Scp106Targeting.choose(World, position(), SenseRange, SenseRange, 0.0, Last, Double.POSITIVE_INFINITY, false, false);
    }

    private void startStepOut(ServerLevel World, Scp106Arrival.Plan Plan) {
        Direction Out = Plan.Out();
        Vec3 End = Plan.End();
        Vec3 Step = Vec3.atLowerCornerOf(Out.getNormal());
        Vec3 Start = End.subtract(Step.scale(StepOutDistance));
        float Yaw = Out.toYRot();
        moveTo(Start.x, Start.y, Start.z, Yaw, 0.0f);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
        ArrivalStart = Start;
        ArrivalEnd = End;
        SurfaceY = End.y;
        startAction(ActionStepOut, StepOutTicks);
        Vec3 Face = End.subtract(Step.scale(0.5));
        Scp106Portal.place(World, Face, Out, Scp106Portal.WallWidth, Scp106Portal.WallHeight, StepOutTicks + PortalLinger);
        Vec3 Mouth = Face.add(0.0, 1.2, 0.0);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp106Registry.Corrode.get(), SoundSource.HOSTILE, 1.1f, 0.8f + getRandom().nextFloat() * 0.1f);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp106Registry.Phase.get(), SoundSource.HOSTILE, 1.0f, 0.75f);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp106Registry.Rise.get(), SoundSource.HOSTILE, 1.0f, 0.9f);
        Scp106Effects.splash(World, Mouth, 18, 0.6);
    }

    private void startDescend(ServerLevel World, Scp106Arrival.Plan Plan) {
        Vec3 End = Plan.End();
        double Ceiling = Plan.CeilingY();
        moveTo(End.x, Ceiling + DescendStart, End.z, getYRot(), 0.0f);
        ArrivalStart = new Vec3(End.x, Ceiling + DescendStart, End.z);
        ArrivalEnd = End;
        ArrivalCeiling = Ceiling;
        SurfaceY = End.y;
        startAction(ActionDescend, DescendTicks);
        Vec3 Face = new Vec3(End.x, Ceiling, End.z);
        Scp106Portal.place(World, Face, Direction.DOWN, Scp106Portal.CeilingSize, Scp106Portal.CeilingSize, DescendLower + DescendDrop + PortalLinger);
        World.playSound(null, Face.x, Face.y, Face.z, Scp106Registry.Rise.get(), SoundSource.HOSTILE, 1.2f, 0.85f + getRandom().nextFloat() * 0.1f);
        World.playSound(null, Face.x, Face.y, Face.z, Scp106Registry.Corrode.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
        Scp106Effects.splash(World, Face.subtract(0.0, 0.3, 0.0), 18, 0.6);
    }

    private void tickStepOut(ServerLevel World) {
        Steer = Vec3.ZERO;
        int Elapsed = elapsed();
        if (Elapsed == StepOutHold + 8 || Elapsed == StepOutHold + 24) {
            World.playSound(null, getX(), getY(), getZ(), Scp106Registry.Step.get(), SoundSource.HOSTILE, 0.8f, 0.9f);
            World.playSound(null, getX(), getY(), getZ(), Scp106Registry.StepWet.get(), SoundSource.HOSTILE, 0.6f, 0.9f);
        }
        if (Elapsed < ActionLength) {
            return;
        }
        finishArrival(World);
    }

    private void tickDescend(ServerLevel World) {
        Steer = Vec3.ZERO;
        int Elapsed = elapsed();
        if (Elapsed == DescendLower + DescendDrop) {
            World.playSound(null, getX(), getY(), getZ(), Scp106Registry.Sink.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
            World.playSound(null, getX(), getY(), getZ(), Scp106Registry.Step.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
            Scp106Effects.pool(World, position());
        }
        if (Elapsed < ActionLength) {
            return;
        }
        finishArrival(World);
    }

    private Vec3 scriptedPosition(int Elapsed) {
        if (ArrivalStart == null || ArrivalEnd == null) {
            return position();
        }
        if (action() == ActionStepOut) {
            double Share = Mth.clamp((Elapsed - StepOutHold) / (double) StepOutMove, 0.0, 1.0);
            return ArrivalStart.add(ArrivalEnd.subtract(ArrivalStart).scale(Share));
        }
        double Hang = ArrivalCeiling - DescendHang;
        double Y;
        if (Elapsed <= DescendLower) {
            Y = ArrivalStart.y - (ArrivalStart.y - Hang) * Elapsed / DescendLower;
        } else if (Elapsed <= DescendLower + DescendDrop) {
            double Share = (Elapsed - DescendLower) / (double) DescendDrop;
            Y = Hang - (Hang - ArrivalEnd.y) * Share * Share;
        } else {
            Y = ArrivalEnd.y;
        }
        return new Vec3(ArrivalEnd.x, Math.max(Y, ArrivalEnd.y), ArrivalEnd.z);
    }

    private void finishArrival(ServerLevel World) {
        if (ArrivalEnd != null) {
            setPos(ArrivalEnd.x, ArrivalEnd.y, ArrivalEnd.z);
        } else if (!Double.isNaN(SurfaceY) && getY() < SurfaceY - 0.05) {
            setPos(getX(), SurfaceY, getZ());
        }
        ArrivalStart = null;
        ArrivalEnd = null;
        ArrivalCeiling = Double.NaN;
        SurfaceY = Double.NaN;
        WarpDest = null;
        Commanded = false;
        WarpCooldown = Chasing ? ChaseCooldownMin + getRandom().nextInt(ChaseCooldownMax - ChaseCooldownMin + 1)
                : WarpCooldownMin + getRandom().nextInt(WarpCooldownMax - WarpCooldownMin + 1);
        Chasing = false;
        startAction(ActionNone, 0);
        if (Lured && LureVictim != null && World.getEntity(LureVictim) instanceof LivingEntity Victim && Victim.isAlive() && Targeting.isTargetable(Victim)) {
            Target = Victim;
            if (getBoundingBox().inflate(GrabReach + 0.6).intersects(Victim.getBoundingBox())) {
                startAction(ActionGrab, GrabTicks);
                World.playSound(null, getX(), getEyeY(), getZ(), Scp106Registry.Touch.get(), SoundSource.HOSTILE, 1.0f, 0.85f);
            }
        }
    }

    public boolean arriving() {
        byte Kind = action();
        return Kind == ActionRise || Kind == ActionStepOut || Kind == ActionDescend;
    }

    public void forceStyle(@Nullable Scp106Arrival.Style Style) {
        ForcedStyle = Style;
    }

    public void hurryReturn() {
        if (action() == ActionHidden && Retreating) {
            ActionLength = Math.min(ActionLength, elapsed());
        }
    }

    public void huntAfterReturn(LivingEntity Quarry) {
        Target = Quarry;
        LastTarget = Quarry.getUUID();
    }

    public void erase() {
        Erased = true;
        if (level() instanceof ServerLevel World) {
            Scp106ReturnLedger.get(World.getServer()).complete(getUUID());
        }
        discard();
    }

    private void tickRise(ServerLevel World) {
        Steer = Vec3.ZERO;
        if (elapsed() < ActionLength) {
            return;
        }
        finishArrival(World);
    }

    private void tickGrab(ServerLevel World) {
        if (Target != null) {
            getLookControl().setLookAt(Target, 40.0f, 40.0f);
        }
        if (elapsed() == GrabContact && Target != null) {
            double Reach = Lured ? GrabReach + 0.8 : GrabReach;
            if (Target.isAlive() && Targeting.isTargetable(Target) && Target.level() == World && getBoundingBox().inflate(Reach).intersects(Target.getBoundingBox())) {
                capture(World, Target);
            }
        }
        if (elapsed() >= ActionLength) {
            startAction(ActionNone, 0);
        }
    }

    private void capture(ServerLevel World, LivingEntity Victim) {
        boolean Lure = Lured && LureVictim != null && LureVictim.equals(Victim.getUUID());
        Scp106Effects.splash(World, Victim.position().add(0.0, 1.0, 0.0), 18, 0.4);
        if (Lure) {
            Victim.hurt(damageSources().source(Scp106Registry.Taken, this), Float.MAX_VALUE);
            Lured = false;
            LureVictim = null;
            Sated = SatedLure;
            Target = null;
            laugh(World);
            return;
        }
        Victim.hurt(damageSources().source(Scp106Registry.Corrosion, this), TouchDamage);
        boolean Gone = !Victim.isAlive() || Victim.isDeadOrDying() || Victim instanceof ServerPlayer User && ControlManager.hasPendingDeath(User);
        if (!Gone) {
            Scp106Pocket.queueTake(Victim);
        }
        Sated = SatedCapture;
        Target = null;
        laugh(World);
    }

    public boolean lure(ServerLevel World, Vec3 Spot, LivingEntity Victim) {
        if (World != level()) {
            return false;
        }
        if (Retreating) {
            Retreating = false;
            setHealth(Math.max(getHealth(), getMaxHealth() * 0.5f));
        }
        cancelBreach(World);
        Lured = true;
        LureVictim = Victim.getUUID();
        Target = Victim;
        Sated = 0;
        if (hidden()) {
            setInvisible(false);
        }
        beginWarp(World, Spot, LureSinkTicks);
        return true;
    }

    public void forceWarp(ServerLevel World, LivingEntity Quarry) {
        Vec3 Spot = null;
        if (ForcedStyle != null && ForcedStyle != Scp106Arrival.Style.Floor) {
            for (int Attempt = 0; Attempt < EmergeAttempts && Spot == null; Attempt++) {
                Vec3 Candidate = emergence(World, Quarry);
                if (Candidate != null && Scp106Arrival.choose(World, Candidate, getRandom(), ForcedStyle).Kind() == ForcedStyle) {
                    Spot = Candidate;
                }
            }
        }
        if (Spot == null) {
            Spot = emergence(World, Quarry);
        }
        if (Spot == null) {
            Spot = Quarry.position();
        }
        Target = Quarry;
        LastTarget = Quarry.getUUID();
        Sated = 0;
        Commanded = true;
        beginWarp(World, Spot, SinkTicks);
    }

    private void tickVoice(ServerLevel World) {
        RandomSource Random = getRandom();
        if (--BreathTimer <= 0) {
            BreathTimer = BreathMin + Random.nextInt(BreathMax - BreathMin + 1);
            breathe(World, 1.0f);
        }
        if (--LaughTimer <= 0) {
            laugh(World);
        }
    }

    public void laugh(ServerLevel World) {
        RandomSource Random = getRandom();
        boolean Hunting = Target != null;
        LaughTimer = Hunting ? LaughHuntMin + Random.nextInt(LaughHuntMax - LaughHuntMin + 1) : LaughIdleMin + Random.nextInt(LaughIdleMax - LaughIdleMin + 1);
        float Pitch = 0.97f + Random.nextFloat() * 0.06f;
        speak(World, Scp106Registry.Laugh, 1.0f, Pitch, true);
        speak(World, Scp106Registry.LaughLow, 0.9f, Pitch, true);
        World.broadcastEntityEvent(this, LaughEvent);
    }

    public void breathe(ServerLevel World, float Volume) {
        float Pitch = 0.97f + getRandom().nextFloat() * 0.06f;
        speak(World, Scp106Registry.Breath, Volume, Pitch, false);
        speak(World, Scp106Registry.BreathWet, Volume * 0.75f, Pitch, false);
        speak(World, Scp106Registry.BreathLow, Volume * 0.85f, Pitch, false);
        World.broadcastEntityEvent(this, BreathEvent);
    }

    private void speak(ServerLevel World, Holder<SoundEvent> Sound, float Volume, float Pitch, boolean Echoing) {
        long Seed = World.getRandom().nextLong();
        Scp106Effects.voice(World, this, Sound, Volume, Pitch, Seed);
        if (Echoing) {
            Echoes.add(new Echo(tickCount + EchoFirst, Sound, Volume * EchoFirstVolume, Pitch, Seed));
            Echoes.add(new Echo(tickCount + EchoSecond, Sound, Volume * EchoSecondVolume, Pitch, Seed));
        }
    }

    private void tickEchoes(ServerLevel World) {
        for (int Index = Echoes.size() - 1; Index >= 0; Index--) {
            Echo Pending = Echoes.get(Index);
            if (tickCount >= Pending.Due()) {
                Echoes.remove(Index);
                Scp106Effects.voice(World, this, Pending.Sound(), Pending.Volume(), Pending.Pitch(), Pending.Seed());
            }
        }
    }

    private record Echo(int Due, Holder<SoundEvent> Sound, float Volume, float Pitch, long Seed) {
    }

    private void checkContainment(ServerLevel World) {
        byte Kind = action();
        if (Kind != ActionNone && Kind != ActionCorrode) {
            return;
        }
        int Sealed = Scp106Phasing.sealedSides(World, position(), getY(), ContainReach);
        boolean Enclosed = Sealed >= 6 || Sealed == 5 && !Scp106Phasing.rayHeld(World, new Vec3(getX(), getY() + 1.5, getZ()), Direction.UP, ContainReach);
        if (!Enclosed) {
            ContainedTicks = 0;
            if (breaching() && Kind != ActionCorrode) {
                cancelBreach(World);
            }
            return;
        }
        ContainedTicks += ContainCheckEvery;
        if (!breaching() && ContainedTicks >= BreachGrace && BreachWait <= 0 && Sated <= 0 && getRandom().nextFloat() < BreachChance) {
            startBreach(World);
        }
    }

    public boolean startBreach(ServerLevel World) {
        cancelBreach(World);
        BlockPos Feet = blockPosition();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        BlockPos Door = null;
        double DoorDistance = Double.MAX_VALUE;
        for (int Dx = -ContainReach; Dx <= ContainReach; Dx++) {
            for (int Dy = -2; Dy <= 3; Dy++) {
                for (int Dz = -ContainReach; Dz <= ContainReach; Dz++) {
                    Cursor.set(Feet.getX() + Dx, Feet.getY() + Dy, Feet.getZ() + Dz);
                    BlockState State = World.getBlockState(Cursor);
                    if (State.getBlock() instanceof MagnetizedDoorBlock && !State.getValue(MagnetizedDoorBlock.Open)) {
                        double Distance = Cursor.distSqr(Feet);
                        if (Distance < DoorDistance) {
                            DoorDistance = Distance;
                            Door = Cursor.immutable();
                        }
                    }
                }
            }
        }
        if (Door != null) {
            BlockState DoorState = World.getBlockState(Door);
            Direction Front = DoorState.getValue(MagnetizedDoorBlock.Facing);
            Vec3 Centre = Vec3.atCenterOf(Door);
            Direction Toward = Centre.subtract(position()).dot(Vec3.atLowerCornerOf(Front.getNormal())) > 0 ? Front.getOpposite() : Front;
            BreachBlocks.add(Door);
            BreachDoor = true;
            BreachFacing = Toward.getOpposite();
            BreachStand = Vec3.atBottomCenterOf(Door.relative(Toward)).with(Direction.Axis.Y, getY());
        } else {
            List<Direction> Sides = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
            LivingEntity Nearest = Scp106Targeting.choose(World, position(), 64.0, 64.0, 0.0, null, 1.0, false, true);
            if (Nearest != null) {
                Vec3 Toward = Nearest.position().subtract(position());
                Direction Best = Direction.getNearest(Toward.x, 0.0, Toward.z);
                Sides.remove(Best);
                Sides.add(0, Best);
            } else {
                Collections.shuffle(Sides, new Random(getRandom().nextLong()));
            }
            for (Direction Side : Sides) {
                BlockPos Wall = Scp106Phasing.firstHeld(World, new Vec3(getX(), getY() + 0.5, getZ()), Side, ContainReach);
                if (Wall == null) {
                    continue;
                }
                List<BlockPos> Column = new ArrayList<>();
                for (int Dy = 0; Dy < 3; Dy++) {
                    BlockPos Cell = Wall.above(Dy);
                    if (Scp106Phasing.isMagnetized(World.getBlockState(Cell))) {
                        Column.add(Cell);
                    }
                }
                if (Column.isEmpty()) {
                    continue;
                }
                BreachBlocks.addAll(Column);
                BreachDoor = false;
                BreachFacing = Side;
                BreachStand = Vec3.atBottomCenterOf(Wall.relative(Side.getOpposite())).with(Direction.Axis.Y, getY());
                break;
            }
        }
        if (BreachBlocks.isEmpty()) {
            return false;
        }
        for (ServerPlayer Listener : World.players()) {
            if (Listener.distanceToSqr(this) <= BreachWarnRange * BreachWarnRange) {
                Listener.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.breach_warning").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
            }
        }
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp106Registry.Corrode.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
        DiscordNotices.containment(World, blockPosition(), Scp106Registry.DocumentId, true);
        return true;
    }

    private void tickBreachApproach(ServerLevel World) {
        if (BreachStand == null || BreachFacing == null) {
            cancelBreach(World);
            return;
        }
        double Flat = BreachStand.distanceToSqr(getX(), BreachStand.y, getZ());
        if (Flat > 0.5) {
            steerTowards(BreachStand, Pace, false);
            return;
        }
        float Yaw = BreachFacing.toYRot();
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
        startAction(ActionCorrode, BreachTicks);
    }

    private void tickCorrode(ServerLevel World) {
        if (!breaching()) {
            startAction(ActionNone, 0);
            return;
        }
        if (BreachFacing != null) {
            float Yaw = BreachFacing.toYRot();
            setYRot(Yaw);
            yBodyRot = Yaw;
            yHeadRot = Yaw;
        }
        int Elapsed = elapsed();
        int Stage = Mth.clamp(Elapsed * 10 / Math.max(1, ActionLength), 0, 9);
        RandomSource Random = getRandom();
        for (int Index = 0; Index < BreachBlocks.size(); Index++) {
            BlockPos Pos = BreachBlocks.get(Index);
            if (Elapsed % 10 == 0) {
                World.destroyBlockProgress(breakerId(Index), Pos, Stage);
            }
            if (Random.nextFloat() < 0.25f) {
                Vec3 Face = Vec3.atCenterOf(Pos).add(BreachFacing == null ? Vec3.ZERO : Vec3.atLowerCornerOf(BreachFacing.getOpposite().getNormal()).scale(0.52));
                Scp106Effects.splash(World, Face, 2, 0.25);
            }
        }
        if (Elapsed % 40 == 0) {
            World.playSound(null, getX(), getY() + 1.0, getZ(), Scp106Registry.Corrode.get(), SoundSource.HOSTILE, 1.1f, 0.75f + Stage * 0.03f);
        }
        if (Elapsed % 120 == 60) {
            breathe(World, 1.15f);
        }
        if (Elapsed < ActionLength) {
            return;
        }
        for (int Index = 0; Index < BreachBlocks.size(); Index++) {
            BlockPos Pos = BreachBlocks.get(Index);
            World.destroyBlockProgress(breakerId(Index), Pos, -1);
            BlockState State = World.getBlockState(Pos);
            if (BreachDoor && State.getBlock() instanceof MagnetizedDoorBlock) {
                World.destroyBlock(Pos, false);
            } else if (Scp106Phasing.isMagnetized(State)) {
                World.levelEvent(2001, Pos, Block.getId(State));
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), 3);
            }
            Scp106Effects.splash(World, Vec3.atCenterOf(Pos), 10, 0.4);
        }
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp106Registry.Breach.get(), SoundSource.HOSTILE, 1.4f, 0.8f);
        laugh(World);
        BreachBlocks.clear();
        BreachStand = null;
        BreachFacing = null;
        ContainedTicks = 0;
        BreachWait = BreachCooldown;
        startAction(ActionNone, 0);
    }

    private int breakerId(int Index) {
        return -1000 - getId() * 4 - Index;
    }

    private void cancelBreach(ServerLevel World) {
        for (int Index = 0; Index < BreachBlocks.size(); Index++) {
            World.destroyBlockProgress(breakerId(Index), BreachBlocks.get(Index), -1);
        }
        BreachBlocks.clear();
        BreachStand = null;
        BreachFacing = null;
        if (action() == ActionCorrode) {
            startAction(ActionNone, 0);
        }
    }

    @Override
    public void travel(Vec3 Input) {
        if (!isControlledByLocalInstance()) {
            calculateEntityAnimation(false);
            return;
        }
        byte Kind = action();
        boolean Grounded = false;
        Vec3 Motion;
        if (Kind == ActionSink) {
            Motion = new Vec3(0.0, -SinkDepth / Math.max(1, ActionLength), 0.0);
        } else if (Kind == ActionStepOut || Kind == ActionDescend) {
            Motion = scriptedPosition(elapsed() + 1).subtract(position());
        } else if (Kind == ActionRise) {
            Motion = new Vec3(0.0, SinkDepth / Math.max(1, ActionLength), 0.0);
        } else if (Kind == ActionHidden) {
            Motion = Vec3.ZERO;
        } else {
            double Vertical;
            if (!Double.isNaN(Lift)) {
                Vertical = Lift;
                FallSpeed = 0.0;
            } else {
                Vertical = groundFollow();
                Grounded = FallSpeed == 0.0;
            }
            if (getY() + Vertical < level().getMinBuildHeight() + 1) {
                Vertical = Math.max(0.0, level().getMinBuildHeight() + 1 - getY());
            }
            Motion = Scp106Phasing.collide(level(), getBoundingBox(), new Vec3(Steer.x, Vertical, Steer.z));
            if (Double.isNaN(Lift) && Vertical < 0.0 && Motion.y > Vertical + 1.0E-4) {
                FallSpeed = 0.0;
                Grounded = true;
            }
        }
        move(MoverType.SELF, Motion);
        setDeltaMovement(Motion);
        setOnGround(Grounded || Kind == ActionCorrode || Kind == ActionGrab || Kind == ActionStepOut);
        trackWalls();
        calculateEntityAnimation(false);
    }

    private double groundFollow() {
        BlockPos Feet = BlockPos.containing(getX(), getY() + 0.01, getZ());
        boolean FeetSolid = Scp106Phasing.solid(level(), Feet);
        if (FeetSolid) {
            if (!Scp106Phasing.solid(level(), Feet.above())) {
                FallSpeed = 0.0;
                return Math.min(StepRise, Math.max(0.0, Scp106Phasing.topOf(level(), Feet) - getY()));
            }
            FallSpeed = 0.0;
            return 0.0;
        }
        BlockPos Below = BlockPos.containing(getX(), getY() - 0.05, getZ());
        if (Scp106Phasing.solid(level(), Below)) {
            double Top = Scp106Phasing.topOf(level(), Below);
            if (Top >= getY() - 0.06) {
                FallSpeed = 0.0;
                return Top - getY();
            }
        }
        FallSpeed = Math.max(FallSpeed - Gravity, -MaxFall);
        double Next = getY() + FallSpeed;
        BlockPos Landing = BlockPos.containing(getX(), Next, getZ());
        if (Scp106Phasing.solid(level(), Landing)) {
            double Top = Scp106Phasing.topOf(level(), Landing);
            if (Top > Next && Top <= getY()) {
                double Step = Top - getY();
                FallSpeed = 0.0;
                return Step;
            }
        }
        return FallSpeed;
    }

    private void trackWalls() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        boolean Inside = Scp106Phasing.solid(World, BlockPos.containing(getX(), getY() + 1.0, getZ()));
        if (Inside && !InsideWall) {
            World.playSound(null, getX(), getY() + 1.0, getZ(), Scp106Registry.Phase.get(), SoundSource.HOSTILE, 0.8f, 0.85f + getRandom().nextFloat() * 0.2f);
            Scp106Effects.splash(World, position().add(0.0, 1.0, 0.0), 8, 0.35);
        }
        InsideWall = Inside;
        if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 && tickCount % 10 == 0) {
            Scp106Effects.decal(World, BlockPos.containing(getX(), getY() - 0.05, getZ()), getRandom());
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (hidden()) {
            return Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(Source, Amount);
        }
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypeTags.IS_FREEZING)) {
            return false;
        }
        float Reduced = Amount * DamageFactor;
        LastHurt = tickCount;
        if (Reduced >= getHealth() - 1.0f && level() instanceof ServerLevel World) {
            startRetreat(World);
            return true;
        }
        return super.hurt(Source, Reduced);
    }

    private void startRetreat(ServerLevel World) {
        if (Retreating) {
            return;
        }
        if (Target != null) {
            LastTarget = Target.getUUID();
        }
        Retreating = true;
        Lured = false;
        LureVictim = null;
        Target = null;
        Chasing = false;
        cancelBreach(World);
        setHealth(1.0f);
        deathTime = 0;
        WarpDest = null;
        RetreatTicks = RetreatMin + getRandom().nextInt(RetreatMax - RetreatMin + 1);
        boolean Hidden = action() == ActionHidden;
        Vec3 Floor = Hidden ? new Vec3(getX(), Double.isNaN(SurfaceY) ? getY() + SinkDepth : SurfaceY, getZ()) : ArrivalEnd != null ? ArrivalEnd : position();
        ArrivalStart = null;
        ArrivalEnd = null;
        ArrivalCeiling = Double.NaN;
        if (Hidden) {
            startAction(ActionHidden, RetreatTicks);
        } else {
            setPos(Floor.x, Floor.y, Floor.z);
            SurfaceY = Floor.y;
            setInvisible(false);
            startAction(ActionSink, SinkTicks);
        }
        Scp106ReturnLedger.get(World.getServer()).schedule(getUUID(), World.dimension(), Floor,
                Scp106ReturnLedger.now(World.getServer()) + (Hidden ? 0 : SinkTicks) + RetreatTicks, LastTarget);
        Scp106Effects.pool(World, Floor);
        Scp106Effects.splash(World, Floor.add(0.0, 1.0, 0.0), 20, 0.5);
        Scp106Effects.layered(World, this, Scp106Registry.Sink, Scp106Registry.LaughLow, 1.2f, 1.0f);
        laugh(World);
    }

    public boolean retreating() {
        return Retreating;
    }

    private static boolean erases(DamageSource Source) {
        return Source.is(DamageTypes.GENERIC_KILL);
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && !erases(Source)) {
            if (Source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                loseToVoid(World);
                return;
            }
            startRetreat(World);
            setHealth(Math.max(1.0f, getHealth()));
            return;
        }
        if (level() instanceof ServerLevel World) {
            Erased = true;
            Scp106ReturnLedger.get(World.getServer()).complete(getUUID());
        }
        super.die(Source);
    }

    @Override
    protected void tickDeath() {
        if (level() instanceof ServerLevel World && !Erased) {
            startRetreat(World);
            setHealth(Math.max(1.0f, getHealth()));
            deathTime = 0;
            return;
        }
        super.tickDeath();
    }

    @Override
    protected void onBelowWorld() {
        if (level() instanceof ServerLevel World) {
            loseToVoid(World);
            return;
        }
        super.onBelowWorld();
    }

    private void loseToVoid(ServerLevel World) {
        if (isRemoved()) {
            return;
        }
        Vec3 Anchor = ArrivalEnd != null ? ArrivalEnd : position();
        int Away = RetreatMin + getRandom().nextInt(RetreatMax - RetreatMin + 1);
        Scp106ReturnLedger.get(World.getServer()).schedule(getUUID(), World.dimension(), Anchor, Scp106ReturnLedger.now(World.getServer()) + Away,
                Target != null ? Target.getUUID() : LastTarget);
        Erased = true;
        discard();
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (level() instanceof ServerLevel World && Reason.shouldDestroy() && !Erased) {
            Scp106ReturnLedger Ledger = Scp106ReturnLedger.get(World.getServer());
            if (!Ledger.pending(getUUID())) {
                int Away = RetreatMin + getRandom().nextInt(RetreatMax - RetreatMin + 1);
                Ledger.schedule(getUUID(), World.dimension(), position(), Scp106ReturnLedger.now(World.getServer()) + Away, Target != null ? Target.getUUID() : LastTarget);
            }
        }
        super.remove(Reason);
    }

    public void emergeAt(ServerLevel World, Vec3 Spot, float Yaw) {
        SurfaceY = Spot.y;
        moveTo(Spot.x, Spot.y - SinkDepth, Spot.z, Yaw, 0.0f);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
        setInvisible(true);
        startAction(ActionHidden, 4);
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean isPickable() {
        return !hidden() && super.isPickable();
    }

    @Override
    public boolean startRiding(Entity Vehicle, boolean Force) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean AllowPassengers) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
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
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp106Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp106Registry.Hurt.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.2f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("WarpCooldown", WarpCooldown);
        Tag.putInt("Contained", ContainedTicks);
        Tag.putInt("BreachWait", BreachWait);
        Tag.putInt("Sated", Sated);
        Tag.putBoolean("Retreating", Retreating);
        byte Kind = action();
        if (Kind == ActionSink || Kind == ActionHidden || arriving()) {
            Tag.putBoolean("Underground", true);
            Tag.putDouble("SurfaceY", Double.isNaN(SurfaceY) ? getY() : SurfaceY);
            Tag.putInt("HiddenLeft", Kind == ActionHidden ? Math.max(1, ActionLength - elapsed()) : Retreating ? RetreatTicks : 20);
            if (ArrivalEnd != null) {
                Tag.putDouble("ArrivalX", ArrivalEnd.x);
                Tag.putDouble("ArrivalZ", ArrivalEnd.z);
            }
        }
        if (LastTarget != null) {
            Tag.putUUID("LastTarget", LastTarget);
        }
        Tag.putInt("RetreatTicks", RetreatTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        WarpCooldown = Tag.getInt("WarpCooldown");
        ContainedTicks = Tag.getInt("Contained");
        BreachWait = Tag.getInt("BreachWait");
        Sated = Tag.getInt("Sated");
        Retreating = Tag.getBoolean("Retreating");
        LastTarget = Tag.hasUUID("LastTarget") ? Tag.getUUID("LastTarget") : null;
        RetreatTicks = Tag.contains("RetreatTicks") ? Math.max(1, Tag.getInt("RetreatTicks")) : RetreatMin;
        if (Tag.getBoolean("Underground")) {
            double Surface = Tag.getDouble("SurfaceY");
            SurfaceY = Surface;
            double X = Tag.contains("ArrivalX") ? Tag.getDouble("ArrivalX") : getX();
            double Z = Tag.contains("ArrivalZ") ? Tag.getDouble("ArrivalZ") : getZ();
            setPos(X, Surface - SinkDepth, Z);
            setInvisible(true);
            startAction(ActionHidden, Math.max(1, Tag.getInt("HiddenLeft")));
        } else {
            setInvisible(false);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp106Controller(this, "body", 4, Event -> {
            Event.getController().transitionLength(Scp106Controller.blendFor(action()));
            return Event.setAndContinue(bodyAnimation());
        }));
        Controllers.add(new Scp106VoiceController(this, "voice", 3, Event -> Event.setAndContinue(voiceAnimation())));
    }

    private RawAnimation bodyAnimation() {
        if (isDeadOrDying()) {
            return DeathPlay;
        }
        return switch (action()) {
            case ActionSink -> SinkPlay;
            case ActionRise -> RisePlay;
            case ActionStepOut -> StepOutPlay;
            case ActionDescend -> DescendPlay;
            case ActionGrab -> GrabPlay;
            case ActionCorrode -> CorrodeLoop;
            default -> {
                Scp106Gait Walking = gait();
                yield Walking != null && Walking.striding() ? WalkLoop : IdleLoop;
            }
        };
    }

    private RawAnimation voiceAnimation() {
        if (laughing()) {
            return LaughPlay;
        }
        if (breathing()) {
            return BreathePlay;
        }
        return QuietLoop;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
