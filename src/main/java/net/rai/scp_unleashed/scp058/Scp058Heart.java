package net.rai.scp_unleashed.scp058;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class Scp058Heart extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.95f;
    public static final float Height = 1.2f;
    public static final float EyeHeight = 0.75f;
    public static final double Health = 150.0;
    public static final double Armour = 12.0;
    public static final double Toughness = 4.0;
    public static final double KnockbackResistance = 0.75;
    public static final double Speed = 0.42;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.5;
    public static final double FollowRange = 64.0;
    public static final double AcquireRange = 32.0;
    public static final double AcquireHeight = 8.0;
    public static final int ScanTicks = 10;
    public static final double SwitchShare = 0.64;
    public static final double StingDamage = 13.0;
    public static final double LashDamage = 6.0;
    public static final double LashReach = 4.0;
    public static final double StingReach = 1.6;
    public static final float LashPull = 1.15f;
    public static final int PoisonTicks = 160;
    public static final int PoisonLevel = 1;
    public static final int DarknessTicks = 100;
    public static final double LeapMin = 5.0;
    public static final double LeapMax = 12.0;
    public static final double LeapSpeed = 1.05;
    public static final double LeapLift = 0.48;
    public static final int LashTicks = 12;
    public static final int LashHitTick = 6;
    public static final int LashCooldown = 28;
    public static final int StingTicks = 14;
    public static final int StingHitTick = 8;
    public static final int StingCooldown = 34;
    public static final int LeapTicks = 14;
    public static final int LeapLaunchTick = 3;
    public static final int LeapCooldown = 60;
    public static final int EmergeTicks = 40;
    public static final int ShriekTick = 26;
    public static final int HeraldDelay = 50;
    public static final double HeraldRange = 24.0;
    public static final int RegenInterval = 40;
    public static final int CalmBeat = 24;
    public static final int HuntBeat = 12;
    public static final double BeatHearing = 20.0;
    public static final float CalmPitch = 0.85f;
    public static final float HuntPitch = 1.05f;
    public static final float PulseLub = 0.07f;
    public static final float PulseDub = 0.04f;
    public static final float PulseAcross = 0.4f;
    public static final float LubTicks = 2.4f;
    public static final float DubLead = 0.2f;
    public static final float DubGap = 2.0f;
    public static final float DubTicks = 2.0f;
    public static final int WhisperInterval = 320;
    public static final float WhisperChance = 0.35f;
    public static final int GiveUpTicks = 1200;
    public static final int PinTicks = 40;
    public static final double PinDrift = 0.03;
    public static final double PinRise = 0.02;
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionLash = 2;
    public static final byte ActionSting = 3;
    public static final byte ActionLeap = 4;
    public static final DustParticleOptions Ichor = new DustParticleOptions(new Vector3f(0.36f, 0.02f, 0.06f), 1.4f);
    public static final Scp058Gait.Shape HeartGait = new Scp058Gait.Shape(
            new Scp058Gait.Profile(9.0, 0.0694, 0.5, 2.6, new double[]{0.992, 0.492}, new float[]{1.0f, 1.0f}),
            new Scp058Gait.Profile(5.0, 0.2778, 0.6, 2.0, new double[]{0.99, 0.512}, new float[]{1.0f, 1.0f}),
            0.2, 0.15, 1, Scp058Registry.Step, Scp058Registry.StepClick, 0.42f, 0.3f, 1.0f, 24.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp058Heart.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp058Heart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp058Heart.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ClimbingData = SynchedEntityData.defineId(Scp058Heart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp058Heart.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation ClimbLoop = RawAnimation.begin().thenLoop("climb");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation LashPlay = RawAnimation.begin().thenPlayAndHold("lash");
    private static final RawAnimation StingPlay = RawAnimation.begin().thenPlayAndHold("sting");
    private static final RawAnimation LeapPlay = RawAnimation.begin().thenPlayAndHold("leap");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionTicks;
    @Nullable
    private LivingEntity StrikeTarget;
    private int LashReady;
    private int StingReady;
    private int LeapReady;
    @Nullable
    private UUID Herald;
    private int HeraldTicks = -1;
    private float RenderPartial;
    private int TickOrigin;
    @Nullable
    private Scp058Gait Gait;
    private double HeartClock;
    private double HeartClockO;
    private int Pinned;
    private Vec3 PinSpot = Vec3.ZERO;

    public Scp058Heart(EntityType<? extends Scp058Heart> Type, Level World) {
        super(Type, World);
        xpReward = 40;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, StingDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.ATTACK_KNOCKBACK, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
        Builder.define(ClimbingData, false);
        Builder.define(HuntingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp058Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp058Goals.Leap(this));
        goalSelector.addGoal(3, new Scp058Goals.Strike(this, ChaseModifier));
        goalSelector.addGoal(5, new Scp058Goals.Prowl(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Scp058Heart.class));
        targetSelector.addGoal(2, new Scp058Goals.Hunt(this));
    }

    public static boolean isQuarry(LivingEntity Found) {
        if (Found instanceof Scp058Heart || !Found.isAlive() || Found.isSpectator() || !Targeting.isTargetable(Found)) {
            return false;
        }
        return !(Found instanceof Player User) || !User.isCreative();
    }

    public static boolean isPrey(LivingEntity Found) {
        if (!isQuarry(Found)) {
            return false;
        }
        return Found instanceof Player || Found instanceof AbstractVillager || Found instanceof Animal || Found.getType().is(Scp058Registry.PersonLike);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp058Navigation(this, World);
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

    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionLash || Current == ActionSting;
    }

    public boolean locksYaw() {
        return action() == ActionEmerge;
    }

    public boolean isClimbing() {
        return entityData.get(ClimbingData);
    }

    public boolean isHunting() {
        return entityData.get(HuntingData);
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionEmerge -> "emerge";
            case ActionLash -> "lash";
            case ActionSting -> "sting";
            case ActionLeap -> "leap";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(LockedYawData, getYRot());
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind == ActionEmerge || Kind == ActionLash || Kind == ActionSting) {
            getNavigation().stop();
        }
        applyLockedYaw();
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
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

    public void beginEmerging(@Nullable UUID Witness) {
        setAction(ActionEmerge, EmergeTicks);
        Herald = Witness;
        HeraldTicks = HeraldDelay;
    }

    public boolean canLash() {
        return LashReady <= 0 && !isActing();
    }

    public boolean canSting() {
        return StingReady <= 0 && !isActing();
    }

    public boolean canLeap() {
        return LeapReady <= 0 && !isActing() && onGround() && !isClimbing();
    }

    public void beginLash(LivingEntity Target) {
        StrikeTarget = Target;
        LashReady = LashCooldown;
        setAction(ActionLash, LashTicks);
        playLayers(Scp058Registry.LashLayers.subList(0, 1), 1.0f, 0.95f + random.nextFloat() * 0.1f);
    }

    public void beginSting(LivingEntity Target) {
        StrikeTarget = Target;
        StingReady = StingCooldown;
        setAction(ActionSting, StingTicks);
        playSound(Scp058Registry.LeapHiss.get(), 0.7f, 1.2f + random.nextFloat() * 0.1f);
    }

    public void beginLeap(LivingEntity Target) {
        StrikeTarget = Target;
        LeapReady = LeapCooldown + random.nextInt(20);
        setAction(ActionLeap, LeapTicks);
    }

    private void letGo() {
        Pinned = 0;
        if (getNavigation() instanceof Scp058Navigation Route) {
            Route.giveUp();
        } else {
            getNavigation().stop();
        }
    }

    public void prowl() {
        for (WrappedGoal Entry : goalSelector.getAvailableGoals()) {
            if (Entry.getGoal() instanceof Scp058Goals.Prowl Wander) {
                Wander.trigger();
            }
        }
    }

    public double reachTo(LivingEntity Target) {
        return Math.sqrt(distanceToSqr(Target)) - Target.getBbWidth() * 0.5;
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
        boolean Climbing = horizontalCollision && !locksMovement() && !(getNavigation() instanceof Scp058Navigation Route && Route.resting());
        if (Climbing && !onGround()) {
            Vec3 Spot = position();
            double DriftX = Spot.x - PinSpot.x;
            double DriftZ = Spot.z - PinSpot.z;
            boolean Moving = DriftX * DriftX + DriftZ * DriftZ > PinDrift * PinDrift || Spot.y - PinSpot.y > PinRise;
            PinSpot = Spot;
            Pinned = Moving ? 0 : Pinned + 1;
            if (Pinned >= PinTicks) {
                letGo();
                Climbing = false;
            }
        } else {
            Pinned = 0;
            PinSpot = position();
        }
        if (entityData.get(ClimbingData) != Climbing) {
            entityData.set(ClimbingData, Climbing);
        }
        boolean Hunting = getTarget() != null && getTarget().isAlive();
        if (entityData.get(HuntingData) != Hunting) {
            entityData.set(HuntingData, Hunting);
        }
        LashReady = Math.max(0, LashReady - 1);
        StingReady = Math.max(0, StingReady - 1);
        LeapReady = Math.max(0, LeapReady - 1);
        if (ActionTicks > 0) {
            byte Current = action();
            actionTick(Current, (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                entityData.set(ActionData, ActionNone);
                StrikeTarget = null;
            }
        }
        if (HeraldTicks > 0 && --HeraldTicks == 0) {
            herald();
        }
        if (!Hunting && tickCount % RegenInterval == 0 && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }
    }

    private void clientTick() {
        Scp058Gait Current = gait();
        if (Current != null) {
            Current.tick(this);
        }
        if (!isAlive()) {
            HeartClockO = HeartClock;
            return;
        }
        HeartClockO = HeartClock;
        HeartClock += 1.0;
        int Period = isHunting() ? HuntBeat : CalmBeat;
        if (HeartClock >= Period) {
            HeartClock -= Period;
            HeartClockO -= Period;
            thump();
        }
    }

    private void thump() {
        if (isSilent() || !level().hasNearbyAlivePlayer(getX(), getY(), getZ(), BeatHearing)) {
            return;
        }
        float Pitch = beatPitch();
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp058Registry.HeartbeatLayers) {
            level().playLocalSound(getX(), getY() + getBbHeight() * 0.5, getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch, false);
        }
    }

    public float beatPitch() {
        return isHunting() ? HuntPitch : CalmPitch;
    }

    public float pulse(float Partial) {
        float Ticks = (float) (HeartClockO + (HeartClock - HeartClockO) * Partial);
        return 1.0f + PulseLub * bump(Ticks, 0.0f, LubTicks) + PulseDub * bump(Ticks, DubLead + DubGap / beatPitch(), DubTicks);
    }

    public static float pulseAcross(float Scale) {
        return 1.0f + (Scale - 1.0f) * PulseAcross;
    }

    private static float bump(float Ticks, float Start, float Span) {
        if (Ticks < Start || Ticks > Start + Span) {
            return 0.0f;
        }
        return Mth.sin((float) Math.PI * (Ticks - Start) / Span);
    }

    public int heartRate() {
        return 1200 / (isHunting() ? HuntBeat : CalmBeat);
    }

    private void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionEmerge) {
            emergeTick(Elapsed);
            return;
        }
        if (Target != null && Target.isAlive()) {
            getLookControl().setLookAt(Target, 60.0f, 60.0f);
            if (Kind != ActionLeap) {
                float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
                setYRot(Mth.approachDegrees(getYRot(), Yaw, 30.0f));
                yBodyRot = getYRot();
            }
        }
        if (Kind == ActionLash) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == LashHitTick) {
                lashHit(Target);
            }
        } else if (Kind == ActionSting) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == StingHitTick) {
                stingHit(Target);
            }
        } else if (Kind == ActionLeap) {
            if (Elapsed < LeapLaunchTick) {
                setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            } else if (Elapsed == LeapLaunchTick) {
                launch(Target);
            }
        }
    }

    private void emergeTick(int Elapsed) {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Elapsed < 24 && Elapsed % 4 == 0) {
            World.sendParticles(Scp058Registry.Mote.get(), getX(), getY() + 0.5, getZ(), 6, 0.35, 0.3, 0.35, 0.08);
        }
        if (Elapsed == ShriekTick) {
            playSound(Scp058Registry.EmergeShriek.get(), 1.6f, 0.95f + random.nextFloat() * 0.1f);
            World.sendParticles(Ichor, getX(), getY() + 0.7, getZ(), 18, 0.8, 0.4, 0.8, 0.0);
        }
    }

    private void lashHit(@Nullable LivingEntity Target) {
        playSound(Scp058Registry.LashCrack.get(), 1.1f, 0.9f + random.nextFloat() * 0.2f);
        if (Target == null || !Target.isAlive() || reachTo(Target) > LashReach || !hasLineOfSight(Target)) {
            return;
        }
        if (Target.hurt(damageSources().mobAttack(this), (float) LashDamage)) {
            Target.knockback(LashPull, Target.getX() - getX(), Target.getZ() - getZ());
            Target.hurtMarked = true;
            setLastHurtMob(Target);
        }
    }

    private void stingHit(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || reachTo(Target) > StingReach || !hasLineOfSight(Target)) {
            return;
        }
        if (doHurtTarget(Target)) {
            playSound(Scp058Registry.StingPierce.get(), 1.0f, 0.9f + random.nextFloat() * 0.15f);
            Target.addEffect(new MobEffectInstance(MobEffects.POISON, PoisonTicks, PoisonLevel), this);
            Target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DarknessTicks, 0), this);
        }
    }

    private void launch(@Nullable LivingEntity Target) {
        if (Target == null || !onGround()) {
            return;
        }
        Vec3 Toward = new Vec3(Target.getX() - getX(), 0.0, Target.getZ() - getZ());
        double Distance = Toward.length();
        if (Distance < 1.0e-3) {
            return;
        }
        double Push = Math.min(LeapSpeed, 0.2 + Distance * 0.09);
        Vec3 Velocity = Toward.scale(Push / Distance);
        setDeltaMovement(Velocity.x, LeapLift + Math.max(0.0, Target.getY() - getY()) * 0.06, Velocity.z);
        hasImpulse = true;
        playLayers(Scp058Registry.LeapLayers, 1.0f, 0.95f + random.nextFloat() * 0.1f);
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    private void herald() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Component Line = Component.translatable("message.scp_unleashed.scp058.arrival").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        for (ServerPlayer Viewer : World.players()) {
            boolean Witness = Herald != null && Herald.equals(Viewer.getUUID());
            if (Witness || Viewer.distanceToSqr(this) < HeraldRange * HeraldRange) {
                Viewer.displayClientMessage(Line, true);
            }
        }
        Herald = null;
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState State, Vec3 Multiplier) {
        if (!State.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(State, Multiplier);
        }
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World) {
            World.sendParticles(Scp058Registry.Mote.get(), getX(), getY() + getBbHeight() * 0.55, getZ(), 4, 0.25, 0.25, 0.25, 0.06);
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp058Registry.Mote.get(), getX(), getY() + 0.6, getZ(), 30, 0.4, 0.4, 0.4, 0.1);
            World.sendParticles(Ichor, getX(), getY() + 0.6, getZ(), 20, 0.5, 0.4, 0.5, 0.0);
            playSound(Scp058Registry.DeathThump.get(), 1.4f, 0.8f);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return WhisperInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isActing() || random.nextFloat() > WhisperChance) {
            return;
        }
        playLayers(Scp058Registry.WhisperLayers, 0.9f, 0.85f + random.nextFloat() * 0.15f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp058Registry.Whisper.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp058Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp058Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(isActing() ? 3.5 : 1.5);
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Nullable
    public Scp058Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp058Gait(HeartGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp058Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp058Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp058Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Herald != null && HeraldTicks > 0) {
            Tag.putUUID("Herald", Herald);
            Tag.putInt("HeraldTicks", HeraldTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Herald = Tag.hasUUID("Herald") ? Tag.getUUID("Herald") : null;
        HeraldTicks = Tag.contains("HeraldTicks") ? Tag.getInt("HeraldTicks") : -1;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp058GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp058GaitController.blendFor(Current));
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionLash) {
                return Event.setAndContinue(LashPlay);
            }
            if (Current == ActionSting) {
                return Event.setAndContinue(StingPlay);
            }
            if (Current == ActionLeap) {
                return Event.setAndContinue(LeapPlay);
            }
            if (isClimbing()) {
                return Event.setAndContinue(ClimbLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
