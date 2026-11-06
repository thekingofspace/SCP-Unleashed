package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp140.api.DaeviteGodSlainEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

public class DaeviteGod extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float GodWidth = 2.2f;
    public static final float GodHeight = 4.8f;
    public static final float GodEye = 4.1f;
    public static final float KingWidth = 1.4f;
    public static final float KingHeight = 3.45f;
    public static final float KingEye = 3.0f;
    public static final String KingNameKey = "entity.scp_unleashed.daevite_god.king";
    public static final double MaxHealth = 800.0;
    public static final float TransformShare = 0.5f;
    public static final double Armour = 12.0;
    public static final double Toughness = 4.0;
    public static final double FollowRange = 64.0;
    public static final double KingSpeed = 0.32;
    public static final double WalkModifier = 1.0;
    public static final double RunModifier = 1.3;
    public static final float HitCap = 30.0f;
    public static final int Experience = 400;
    public static final double HoverHeight = 1.8;
    public static final double FlySpeed = 0.16;
    public static final double ClimbSpeed = 0.14;
    public static final double OrbitRate = 0.012;
    public static final double CloseRange = 5.5;
    public static final double PreferredRange = 10.0;
    public static final float LightningDamage = 9.0f;
    public static final float SpikesDamage = 10.0f;
    public static final int LightningTelegraph = 25;
    public static final int SpikeTelegraph = 22;
    public static final int SpiritTelegraph = 20;
    public static final float LightningRadius = 1.5f;
    public static final float SpikeRadius = 1.25f;
    public static final float SpiritRadius = 1.0f;
    public static final int CastGap = 46;
    public static final int CastJitter = 20;
    public static final int SpiritCooldown = 320;
    public static final int SpiritCap = 6;
    public static final float CleaveDamage = 18.0f;
    public static final float SweepDamage = 12.0f;
    public static final float ThrustDamage = 14.0f;
    public static final float SlamDamage = 16.0f;
    public static final float ShockDamage = 8.0f;
    public static final double CleaveReach = 4.8;
    public static final double SweepReach = 5.2;
    public static final double MeleeStart = 4.2;
    public static final double ThrustMin = 5.0;
    public static final double ThrustMax = 10.0;
    public static final double LeapMin = 7.0;
    public static final double LeapMax = 18.0;
    public static final double DashSpeed = 1.15;
    public static final double SlamRadius = 3.0;
    public static final double ShockReach = 7.8;
    public static final int ShockTicks = 8;
    public static final int MeleeCooldown = 16;
    public static final int ThrustCooldown = 60;
    public static final int LeapCooldown = 140;
    public static final int KingSpellCooldown = 240;
    public static final int BlinkCooldown = 200;
    public static final int ComboSpeed = 125;
    public static final int RecoverDelay = 1200;
    public static final int RecoverQuiet = 200;
    public static final int RecoverEvery = 40;
    public static final float RecoverAmount = 1.0f;
    public static final int EntranceTicks = 100;
    public static final int EntranceDescent = 60;
    public static final double EntranceRise = 7.0;
    public static final int TransformTicks = 80;
    public static final int TransformCollapse = 39;
    public static final int TransformSwap = 45;
    public static final int DeathTicks = 60;
    public static final int LostSightLimit = 120;
    public static final int FarLimit = 60;
    public static final double FarRange = 28.0;
    public static final int UnreachableLimit = 40;
    public static final int LedgerEvery = 20;
    public static final byte PhaseEntrance = 0;
    public static final byte PhaseActive = 1;
    public static final byte PhaseTransform = 2;
    public static final byte ActionNone = 0;
    public static final byte ActionCastLow = 1;
    public static final byte ActionCastMid = 2;
    public static final byte ActionCastHigh = 3;
    public static final byte ActionCastAll = 4;
    public static final byte ActionSummon = 5;
    public static final byte ActionGodBlink = 6;
    public static final byte ActionCleave = 10;
    public static final byte ActionSweep = 11;
    public static final byte ActionThrust = 12;
    public static final byte ActionLeap = 13;
    public static final byte ActionSlam = 14;
    public static final byte ActionKingCast = 15;
    public static final byte ActionKingBlink = 16;
    public static final int CastLowTicks = 20;
    public static final int CastLowRelease = 9;
    public static final int CastMidTicks = 20;
    public static final int CastMidRelease = 9;
    public static final int CastHighTicks = 24;
    public static final int CastHighRelease = 11;
    public static final int CastAllTicks = 30;
    public static final int CastAllRelease = 14;
    public static final int SummonTicks = 32;
    public static final int SummonRelease = 16;
    public static final int BlinkTicks = 20;
    public static final int BlinkVanish = 10;
    public static final int CleaveTicks = 26;
    public static final int CleaveHit = 14;
    public static final int SweepTicks = 22;
    public static final int SweepHit = 11;
    public static final int ThrustTicks = 24;
    public static final int ThrustDash = 10;
    public static final int ThrustDashEnd = 15;
    public static final int LeapTicks = 18;
    public static final int LeapLaunch = 10;
    public static final int LeapLimit = 70;
    public static final int SlamTicks = 16;
    public static final int KingCastTicks = 24;
    public static final int KingCastRelease = 12;
    public static final int ReformTicks = 35;
    public static final byte PatternUnderLightning = 1;
    public static final byte PatternVolley = 2;
    public static final byte PatternLineSpikes = 3;
    public static final byte PatternRingSpikes = 4;
    public static final byte PatternSelfRing = 5;
    public static final byte PatternUnderSpikes = 6;
    public static final byte PatternSpirits = 7;
    public static final byte PatternKingThorns = 8;
    protected static final EntityDataAccessor<Byte> StageData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> PhaseStartData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> ActionSpeedData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Optional<BlockPos>> HomeData = SynchedEntityData.defineId(DaeviteGod.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final RawAnimation GodFloat = RawAnimation.begin().thenLoop("god_float");
    private static final RawAnimation GodDrift = RawAnimation.begin().thenLoop("god_drift");
    private static final RawAnimation GodFloatTransform = RawAnimation.begin().thenPlayAndHold("god_float_transform");
    private static final RawAnimation GodFloatDeath = RawAnimation.begin().thenPlayAndHold("god_float_death");
    private static final RawAnimation GodFloatBlink = RawAnimation.begin().thenPlayAndHold("god_float_blink");
    private static final RawAnimation GodFloatEntrance = RawAnimation.begin().thenPlayAndHold("god_float_entrance");
    private static final RawAnimation GodArmsIdle = RawAnimation.begin().thenLoop("god_arms_idle");
    private static final RawAnimation GodEntrance = RawAnimation.begin().thenPlayAndHold("god_entrance");
    private static final RawAnimation GodTransform = RawAnimation.begin().thenPlayAndHold("god_transform");
    private static final RawAnimation GodDeath = RawAnimation.begin().thenPlayAndHold("god_death");
    private static final RawAnimation KingIdle = RawAnimation.begin().thenLoop("king_idle");
    private static final RawAnimation KingWalk = RawAnimation.begin().thenLoop("king_walk");
    private static final RawAnimation KingRun = RawAnimation.begin().thenLoop("king_run");
    private static final RawAnimation KingReform = RawAnimation.begin().thenPlayAndHold("king_reform");
    private static final RawAnimation KingDeath = RawAnimation.begin().thenPlayAndHold("king_death");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private DaeviteGodGait Gait;
    @Nullable
    private DaeviteGodController BodyController;
    @Nullable
    private DaeviteGodController FloatController;
    private boolean Claimed;
    private boolean SlainFired;
    private byte Pattern;
    private boolean ActionFired;
    private int CastWait = 30;
    private int SpiritWait = 120;
    private int BlinkWait = 60;
    private int MeleeWait;
    private int ThrustWait = 30;
    private int LeapWait = 60;
    private int SpellWait = 100;
    private int ComboLeft;
    private double OrbitSign = 1.0;
    private int OrbitTimer;
    private int LostSight;
    private int FarTicks;
    private int Unreachable;
    private int StuckTicks;
    private Vec3 StuckFrom = Vec3.ZERO;
    private int IdleTicks;
    private int LastHurt = -100000;
    private int LastSeen;
    private int TrackedId = -1;
    @Nullable
    private Vec3 TrackedPos;
    private Vec3 TrackedVelocity = Vec3.ZERO;
    @Nullable
    private Vec3 BlinkTarget;
    private boolean Airborne;
    private boolean HeavyStrike;
    private final Set<Integer> Struck = new HashSet<>();
    private int ShockTimer = -1;
    private Vec3 ShockCentre = Vec3.ZERO;
    private double EntranceFrom;
    private double EntranceTo;
    private boolean Recovering;

    public DaeviteGod(EntityType<? extends DaeviteGod> Type, Level World) {
        super(Type, World);
        xpReward = Experience;
        setPersistenceRequired();
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, KingSpeed)
                .add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.ATTACK_DAMAGE, CleaveDamage)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.SAFE_FALL_DISTANCE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(StageData, (byte) 1);
        Builder.define(PhaseData, PhaseActive);
        Builder.define(PhaseStartData, 0);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(ActionSpeedData, 100);
        Builder.define(SerialData, 0);
        Builder.define(HomeData, Optional.empty());
    }

    @Override
    protected void registerGoals() {
    }

    public byte stage() {
        return entityData.get(StageData);
    }

    public boolean king() {
        return stage() >= 2;
    }

    @Override
    protected Component getTypeName() {
        return king() ? Component.translatable(KingNameKey) : super.getTypeName();
    }

    public byte phase() {
        return entityData.get(PhaseData);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean entering() {
        return phase() == PhaseEntrance;
    }

    public boolean transforming() {
        return phase() == PhaseTransform;
    }

    public boolean busy() {
        return phase() != PhaseActive;
    }

    public boolean recovering() {
        return Recovering;
    }

    @Nullable
    public BlockPos home() {
        return entityData.get(HomeData).orElse(null);
    }

    private void setHome(@Nullable BlockPos Value) {
        entityData.set(HomeData, Optional.ofNullable(Value == null ? null : Value.immutable()));
    }

    public float stageCap() {
        return king() ? (float) (MaxHealth * TransformShare) : (float) MaxHealth;
    }

    private int now() {
        return tickCount;
    }

    public double phaseElapsed() {
        int Start = entityData.get(PhaseStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public double actionSpeed() {
        return entityData.get(ActionSpeedData) / 100.0;
    }

    public double clipTicks() {
        return actionElapsed() * actionSpeed();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        DaeviteGodGait Current = gait();
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
    protected EntityDimensions getDefaultDimensions(Pose Current) {
        return king() ? EntityDimensions.scalable(KingWidth, KingHeight).withEyeHeight(KingEye) : EntityDimensions.scalable(GodWidth, GodHeight).withEyeHeight(GodEye);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (StageData.equals(Accessor)) {
            refreshDimensions();
            if (BodyController != null) {
                BodyController.forceAnimationReset();
            }
            if (FloatController != null) {
                FloatController.forceAnimationReset();
            }
        }
        if (level().isClientSide && (SerialData.equals(Accessor) || PhaseData.equals(Accessor)) && BodyController != null) {
            BodyController.forceAnimationReset();
            if (FloatController != null) {
                FloatController.forceAnimationReset();
            }
        }
    }

    private void setStage(byte Value) {
        entityData.set(StageData, Value);
        setNoGravity(Value < 2);
        refreshDimensions();
    }

    private void setPhase(byte Value) {
        entityData.set(PhaseStartData, tickCount);
        entityData.set(PhaseData, Value);
        endAction();
        getNavigation().stop();
    }

    private void startAction(byte Kind, int Speed) {
        ActionFired = false;
        Struck.clear();
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionSpeedData, Speed);
        entityData.set(ActionData, Kind);
        entityData.set(SerialData, entityData.get(SerialData) + 1);
        getNavigation().stop();
    }

    private void endAction() {
        if (action() != ActionNone) {
            entityData.set(ActionData, ActionNone);
            entityData.set(ActionSpeedData, 100);
        }
        Airborne = false;
    }

    public void beginEntrance(Vec3 Altar) {
        EntranceTo = Altar.y + HoverHeight;
        EntranceFrom = EntranceTo + EntranceRise;
        moveTo(Altar.x, EntranceFrom, Altar.z, getYRot(), 0.0f);
        setHome(BlockPos.containing(Altar));
        setPhase(PhaseEntrance);
    }

    public void markClaimed() {
        Claimed = true;
    }

    public void adopt(int Stage, float Health) {
        if (Stage >= 2) {
            setStage((byte) 2);
        }
        if (Health > 0.0f) {
            setHealth(Math.min(Health, getMaxHealth()));
        }
    }

    public void recall(ServerLevel World, Vec3 Altar) {
        if (World != level()) {
            return;
        }
        setTarget(null);
        if (king()) {
            teleportTo(Altar.x, Altar.y, Altar.z);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), Altar.x, Altar.y + 1.5, Altar.z, 60, 0.8, 1.2, 0.8, 0.08);
            playLayers(DaeviteGodRegistry.RoarLayers, 1.0f, 0.85f);
            setHome(BlockPos.containing(Altar));
            return;
        }
        beginEntrance(Altar);
    }

    public void forceTransform() {
        if (king() || !isAlive()) {
            return;
        }
        if (getHealth() > stageCap() * TransformShare) {
            setHealth((float) (MaxHealth * TransformShare));
        }
        beginTransform();
    }

    public void banish(ServerLevel World) {
        vanishSpirits(World);
        DaeviteGodLedger.get(World.getServer()).forget();
        World.sendParticles(DaeviteGodRegistry.Ash.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 80, 0.8, getBbHeight() * 0.35, 0.8, 0.04);
        World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 60, 0.8, getBbHeight() * 0.35, 0.8, 0.06);
        playLayers(DaeviteGodRegistry.BlinkLayers, 1.0f, 0.7f);
        discard();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel World) {
            reconcile(World);
        }
    }

    private void reconcile(ServerLevel World) {
        DaeviteGodLedger Ledger = DaeviteGodLedger.get(World.getServer());
        if (Ledger.owns(this)) {
            Claimed = true;
            return;
        }
        if (Ledger.banished(getUUID())) {
            discard();
            return;
        }
        if (!Ledger.alive()) {
            Ledger.claim(this);
            Claimed = true;
            if (home() == null) {
                setHome(blockPosition());
            }
            return;
        }
        DaeviteGod Other = Ledger.live(World.getServer());
        if (Other != null && Other != this) {
            if (!Claimed) {
                Other.recall(World, position());
            }
            discard();
            return;
        }
        if (!Claimed) {
            adopt(Ledger.stage(), Ledger.health());
            Ledger.claim(this);
            Claimed = true;
            if (home() == null) {
                setHome(blockPosition());
            }
            return;
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (tickCount % LedgerEvery == 0) {
            DaeviteGodLedger Ledger = DaeviteGodLedger.get(World.getServer());
            if (Ledger.supersedes(this)) {
                vanishSpirits(World);
                discard();
                return;
            }
            Ledger.update(this);
        }
        tickShock(World);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        switch (phase()) {
            case PhaseEntrance -> tickEntrance(World);
            case PhaseTransform -> tickTransform(World);
            default -> tickActive(World);
        }
    }

    private void tickEntrance(ServerLevel World) {
        int Time = (int) phaseElapsed();
        double Share = Mth.clamp(Time / (double) EntranceDescent, 0.0, 1.0);
        double Eased = Share * Share * (3.0 - 2.0 * Share);
        double Y = EntranceFrom + (EntranceTo - EntranceFrom) * Eased;
        setDeltaMovement(Vec3.ZERO);
        setPos(getX(), Y, getZ());
        if (Time == 0) {
            playLayers(DaeviteGodRegistry.EntranceLayers, 1.0f, 0.9f);
        }
        if (Time % 20 == 0 && Time <= 60) {
            float Pitch = 0.62f + Time / 60.0f * 0.3f;
            playLayers(DaeviteGodRegistry.ChantLayers, 1.0f, Pitch);
        }
        if (Time == 50) {
            playSound(DaeviteGodRegistry.Rumble.get(), 1.0f, 0.75f);
        }
        if (Time % 2 == 0 && Time < 70) {
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 2.0, getZ(), 6, 1.2, 3.0, 1.2, 0.05);
        }
        if (Time == 80) {
            playLayers(DaeviteGodRegistry.RoarLayers, 1.0f, 0.8f);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 2.5, getZ(), 120, 2.5, 1.5, 2.5, 0.2);
            for (Player Viewer : World.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6.0), Found -> !Found.isSpectator())) {
                Vec3 Away = Viewer.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-3) {
                    Away = Away.normalize().scale(0.8);
                    Viewer.setDeltaMovement(Viewer.getDeltaMovement().add(Away.x, 0.3, Away.z));
                    Viewer.hurtMarked = true;
                }
            }
        }
        if (Time >= EntranceTicks) {
            setPhase(PhaseActive);
            CastWait = 20;
        }
    }

    private void beginTransform() {
        setDeltaMovement(Vec3.ZERO);
        setPhase(PhaseTransform);
        playLayers(DaeviteGodRegistry.TransformLayers, 1.0f, 0.85f);
    }

    private void tickTransform(ServerLevel World) {
        int Time = (int) phaseElapsed();
        if (Time < TransformCollapse) {
            setDeltaMovement(0.0, king() ? 0.0 : 0.08, 0.0);
            if (Time % 2 == 0) {
                World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 2.4, getZ(), 8, 1.4, 2.0, 1.4, 0.08);
            }
            if (Time % 6 == 0) {
                World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 4, 1.0, 0.2, 1.0, 0.02);
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (Time == TransformCollapse) {
            World.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2.4, getZ(), 2, 0.0, 0.0, 0.0, 0.0);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 2.4, getZ(), 160, 2.2, 2.2, 2.2, 0.35);
            World.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.4, getZ(), 40, 1.2, 1.6, 1.2, 0.15);
            playSound(DaeviteGodRegistry.ReformBell.get(), 1.0f, 0.6f);
            for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7.0), Found -> DaeviteGodTargets.canHurt(this, Found))) {
                Vec3 Away = Body.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-3) {
                    Away = Away.normalize().scale(1.1);
                    Body.setDeltaMovement(Body.getDeltaMovement().add(Away.x, 0.45, Away.z));
                    Body.hurtMarked = true;
                }
            }
        }
        if (Time == TransformSwap && !king()) {
            OptionalDouble Ground = DaeviteGodTargets.groundAt(World, getX(), getZ(), getY(), 40);
            setStage((byte) 2);
            if (Ground.isPresent()) {
                setPos(getX(), Ground.getAsDouble(), getZ());
            }
            setHealth(Math.min(getHealth(), stageCap()));
            playLayers(DaeviteGodRegistry.ReformLayers, 1.0f, 0.8f);
        }
        if (Time == TransformSwap + 27) {
            playLayers(DaeviteGodRegistry.RoarLayers, 1.0f, 0.75f);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 2.0, getZ(), 70, 1.5, 1.0, 1.5, 0.15);
        }
        if (Time >= TransformTicks) {
            setPhase(PhaseActive);
            MeleeWait = 10;
            LeapWait = 40;
        }
    }

    private void tickActive(ServerLevel World) {
        scan(World);
        LivingEntity Target = getTarget();
        track(Target);
        setAggressive(Target != null);
        tickCooldowns();
        tickRecovery();
        if (!king() && getHealth() <= MaxHealth * TransformShare && action() != ActionGodBlink) {
            beginTransform();
            return;
        }
        if (king()) {
            tickKing(World, Target);
        } else {
            tickGod(World, Target);
        }
    }

    private void tickCooldowns() {
        CastWait = Math.max(0, CastWait - 1);
        SpiritWait = Math.max(0, SpiritWait - 1);
        BlinkWait = Math.max(0, BlinkWait - 1);
        MeleeWait = Math.max(0, MeleeWait - 1);
        ThrustWait = Math.max(0, ThrustWait - 1);
        LeapWait = Math.max(0, LeapWait - 1);
        SpellWait = Math.max(0, SpellWait - 1);
    }

    private void tickRecovery() {
        if (getTarget() != null) {
            IdleTicks = 0;
            Recovering = false;
            return;
        }
        IdleTicks++;
        Recovering = IdleTicks > RecoverDelay && tickCount - LastHurt > RecoverQuiet && getHealth() < stageCap();
        if (Recovering && tickCount % RecoverEvery == 0) {
            setHealth(Math.min(stageCap(), getHealth() + RecoverAmount));
        }
    }

    private void scan(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null) {
            if (!DaeviteGodTargets.isPrey(this, Current) || Current.level() != level() || distanceToSqr(Current) > Mth.square(FollowRange * 1.5)) {
                setTarget(null);
            } else if (hasLineOfSight(Current)) {
                LastSeen = tickCount;
            }
        }
        if (getTarget() != null || (tickCount + getId()) % 10 != 0) {
            return;
        }
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        AABB Reach = getBoundingBox().inflate(FollowRange, FollowRange * 0.6, FollowRange);
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, Reach, Body -> DaeviteGodTargets.isPrey(this, Body))) {
            double Gap = distanceToSqr(Found);
            if (Gap < BestGap && (Gap < 24.0 * 24.0 || hasLineOfSight(Found))) {
                BestGap = Gap;
                Best = Found;
            }
        }
        if (Best != null) {
            setTarget(Best);
            LastSeen = tickCount;
        }
    }

    private void track(@Nullable LivingEntity Target) {
        if (Target == null) {
            TrackedId = -1;
            TrackedPos = null;
            TrackedVelocity = Vec3.ZERO;
            return;
        }
        Vec3 Now = Target.position();
        if (Target.getId() == TrackedId && TrackedPos != null) {
            Vec3 Step = Now.subtract(TrackedPos).multiply(1.0, 0.0, 1.0);
            if (Step.lengthSqr() > 1.0) {
                Step = Vec3.ZERO;
            }
            TrackedVelocity = TrackedVelocity.scale(0.6).add(Step.scale(0.4));
        } else {
            TrackedVelocity = Vec3.ZERO;
        }
        TrackedId = Target.getId();
        TrackedPos = Now;
    }

    private Vec3 predict(LivingEntity Target, double Ticks) {
        Vec3 Lead = TrackedVelocity.scale(Ticks);
        if (Lead.length() > 4.0) {
            Lead = Lead.normalize().scale(4.0);
        }
        return Target.position().add(Lead);
    }

    private void tickGod(ServerLevel World, @Nullable LivingEntity Target) {
        byte Current = action();
        if (Current != ActionNone) {
            tickGodAction(World, Target, Current);
            float Drift = Current == ActionGodBlink ? 0.0f : 0.35f;
            hover(World, Target, Drift);
            return;
        }
        hover(World, Target, 1.0f);
        if (Target == null) {
            return;
        }
        faceTarget(Target, 8.0f);
        double Gap = horizontalGap(Target);
        boolean Sees = hasLineOfSight(Target);
        LostSight = Sees ? 0 : LostSight + 1;
        FarTicks = Gap > FarRange ? FarTicks + 1 : 0;
        if (BlinkWait <= 0 && (LostSight > LostSightLimit || FarTicks > FarLimit || StuckTicks > 80)) {
            Vec3 Spot = blinkSpot(World, Target);
            if (Spot != null) {
                BlinkTarget = Spot;
                startAction(ActionGodBlink, 100);
                playLayers(DaeviteGodRegistry.BlinkLayers, 1.0f, 0.8f);
                BlinkWait = BlinkCooldown;
                LostSight = 0;
                FarTicks = 0;
                StuckTicks = 0;
                return;
            }
        }
        if (CastWait <= 0) {
            castSpell(World, Target, Gap);
        }
    }

    private double horizontalGap(Entity Other) {
        return Math.sqrt(Mth.square(Other.getX() - getX()) + Mth.square(Other.getZ() - getZ()));
    }

    private void faceTarget(Entity Target, float Rate) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), Yaw, Rate));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        getLookControl().setLookAt(Target, Rate * 2.0f, 30.0f);
    }

    private void hover(ServerLevel World, @Nullable LivingEntity Target, float Drive) {
        Vec3 Want;
        if (Target != null) {
            Vec3 Offset = position().subtract(Target.position()).multiply(1.0, 0.0, 1.0);
            if (Offset.lengthSqr() < 1.0e-3) {
                Offset = new Vec3(1.0, 0.0, 0.0);
            }
            if (++OrbitTimer > 180 + random.nextInt(120)) {
                OrbitTimer = 0;
                OrbitSign = -OrbitSign;
            }
            Vec3 Direction = Offset.normalize().yRot((float) (OrbitSign * OrbitRate * 6.0));
            double Gap = Offset.length();
            double Preferred = Gap < CloseRange ? PreferredRange : Mth.clamp(Gap, 8.0, 12.0);
            Want = Target.position().add(Direction.scale(Preferred));
        } else if (home() != null && home().distSqr(blockPosition()) > 64.0) {
            Want = Vec3.atBottomCenterOf(home());
        } else {
            Want = position();
        }
        OptionalDouble Ground = DaeviteGodTargets.groundAt(World, Want.x, Want.z, Math.max(getY(), Target != null ? Target.getY() : getY()), 24);
        double Floor = Ground.isPresent() ? Ground.getAsDouble() : (Target != null ? Target.getY() : getY() - HoverHeight);
        double Altitude = Floor + HoverHeight;
        OptionalDouble Under = DaeviteGodTargets.groundAt(World, getX(), getZ(), getY(), 24);
        if (Under.isPresent()) {
            Altitude = Math.max(Altitude, Under.getAsDouble() + HoverHeight);
        }
        Vec3 Delta = new Vec3(Want.x - getX(), Altitude - getY(), Want.z - getZ());
        Vec3 Flat = Delta.multiply(1.0, 0.0, 1.0);
        double Speed = FlySpeed * Drive;
        Vec3 Horizontal = Flat.lengthSqr() > Speed * Speed ? Flat.normalize().scale(Speed) : Flat;
        double Vertical = Mth.clamp(Delta.y * 0.2, -ClimbSpeed, ClimbSpeed);
        Vec3 Wanted = new Vec3(Horizontal.x, Vertical, Horizontal.z);
        setDeltaMovement(getDeltaMovement().scale(0.75).add(Wanted.scale(0.25)));
        hasImpulse = true;
        if (tickCount % 40 == 0) {
            if (Flat.length() > 3.0 && position().distanceTo(StuckFrom) < 0.5) {
                StuckTicks += 40;
            } else {
                StuckTicks = 0;
            }
            StuckFrom = position();
        }
    }

    @Nullable
    private Vec3 blinkSpot(ServerLevel World, LivingEntity Target) {
        for (int Attempt = 0; Attempt < 12; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = 6.0 + random.nextDouble() * 4.0;
            double X = Target.getX() + Math.cos(Angle) * Reach;
            double Z = Target.getZ() + Math.sin(Angle) * Reach;
            OptionalDouble Ground = DaeviteGodTargets.groundAt(World, X, Z, Target.getY() + 2.0, 10);
            if (Ground.isEmpty()) {
                continue;
            }
            double Y = Ground.getAsDouble() + (king() ? 0.0 : HoverHeight);
            AABB Box = getDimensions(getPose()).makeBoundingBox(X, Y, Z);
            if (World.noCollision(this, Box) && !World.containsAnyLiquid(Box)) {
                return new Vec3(X, Y, Z);
            }
        }
        return null;
    }

    private void tickGodAction(ServerLevel World, @Nullable LivingEntity Target, byte Current) {
        int Time = (int) actionElapsed();
        if (Target != null) {
            faceTarget(Target, 10.0f);
        }
        if (Current == ActionGodBlink) {
            tickBlink(World, Time);
            return;
        }
        int Release = switch (Current) {
            case ActionCastLow -> CastLowRelease;
            case ActionCastMid -> CastMidRelease;
            case ActionCastHigh -> CastHighRelease;
            case ActionCastAll -> CastAllRelease;
            default -> SummonRelease;
        };
        int Length = switch (Current) {
            case ActionCastLow -> CastLowTicks;
            case ActionCastMid -> CastMidTicks;
            case ActionCastHigh -> CastHighTicks;
            case ActionCastAll -> CastAllTicks;
            default -> SummonTicks;
        };
        if (Time == 1) {
            playLayers(DaeviteGodRegistry.CastLayers, 1.0f, 0.85f + random.nextFloat() * 0.15f);
        }
        if (Time < Release && Time % 2 == 0) {
            handGlow(World, Current);
        }
        if (Time >= Release && !ActionFired) {
            ActionFired = true;
            if (Target != null && DaeviteGodTargets.isPrey(this, Target)) {
                release(World, Target, Pattern);
            }
        }
        if (Time >= Length) {
            endAction();
            float Pace = getHealth() < MaxHealth * 0.7 ? 0.8f : 1.0f;
            CastWait = (int) ((CastGap + random.nextInt(CastJitter + 1)) * Pace);
        }
    }

    private void tickBlink(ServerLevel World, int Time) {
        setDeltaMovement(Vec3.ZERO);
        if (Time % 2 == 0 && Time <= BlinkVanish) {
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 10, getBbWidth() * 0.4, getBbHeight() * 0.35, getBbWidth() * 0.4, 0.05);
        }
        if (Time == BlinkVanish && BlinkTarget != null) {
            World.sendParticles(DaeviteGodRegistry.Ash.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 40, getBbWidth() * 0.4, getBbHeight() * 0.35, getBbWidth() * 0.4, 0.03);
            teleportTo(BlinkTarget.x, BlinkTarget.y, BlinkTarget.z);
            playLayers(DaeviteGodRegistry.BlinkLayers, 1.0f, 1.05f);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 50, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.1);
            BlinkTarget = null;
        }
        if (Time >= BlinkTicks) {
            endAction();
            CastWait = Math.min(CastWait, 10);
            MeleeWait = Math.min(MeleeWait, 6);
        }
    }

    private void handGlow(ServerLevel World, byte Current) {
        int Pair = switch (Current) {
            case ActionCastLow -> 3;
            case ActionCastMid -> 2;
            case ActionCastHigh -> 1;
            default -> 0;
        };
        for (int Side = -1; Side <= 1; Side += 2) {
            for (int Index = 1; Index <= 3; Index++) {
                if (Pair != 0 && Pair != Index) {
                    continue;
                }
                Vec3 Hand = handPoint(Index, Side);
                World.sendParticles(DaeviteGodRegistry.Ember.get(), Hand.x, Hand.y, Hand.z, 2, 0.12, 0.12, 0.12, 0.02);
            }
        }
    }

    public Vec3 handPoint(int Pair, int Side) {
        double Out = switch (Pair) {
            case 1 -> 1.9;
            case 2 -> 2.3;
            default -> 1.5;
        };
        double Up = switch (Pair) {
            case 1 -> 5.0;
            case 2 -> 3.2;
            default -> 1.6;
        };
        Vec3 Right = Vec3.directionFromRotation(0.0f, getYRot() + 90.0f * Side);
        Vec3 Ahead = Vec3.directionFromRotation(0.0f, getYRot());
        return position().add(Right.scale(Out)).add(Ahead.scale(0.6)).add(0.0, Up, 0.0);
    }

    private void castSpell(ServerLevel World, LivingEntity Target, double Gap) {
        long Spirits = spiritsNear(World);
        boolean Elevated = Target.getY() - groundUnder(World, Target) > 2.5 || !Target.onGround() && Target.getDeltaMovement().y > -0.1 && Gap < 6.0;
        int Total = 0;
        int[] Weights = new int[9];
        if (Gap < CloseRange) {
            Weights[PatternSelfRing] = 5;
            Weights[PatternUnderSpikes] = 2;
            Weights[PatternUnderLightning] = 1;
        } else {
            Weights[PatternUnderLightning] = 3;
            Weights[PatternVolley] = 2;
            Weights[PatternLineSpikes] = 2;
            Weights[PatternRingSpikes] = 2;
            Weights[PatternUnderSpikes] = 1;
        }
        if (Elevated) {
            Weights[PatternUnderLightning] += 3;
            Weights[PatternVolley] += 2;
        }
        if (SpiritWait <= 0 && Spirits < SpiritCap) {
            Weights[PatternSpirits] = 3;
        }
        for (int Weight : Weights) {
            Total += Weight;
        }
        int Roll = random.nextInt(Math.max(1, Total));
        byte Chosen = PatternUnderLightning;
        for (int Index = 0; Index < Weights.length; Index++) {
            Roll -= Weights[Index];
            if (Roll < 0 && Weights[Index] > 0) {
                Chosen = (byte) Index;
                break;
            }
        }
        Pattern = Chosen;
        byte Kind = switch (Chosen) {
            case PatternVolley, PatternUnderLightning -> ActionCastHigh;
            case PatternLineSpikes, PatternSelfRing -> ActionCastLow;
            case PatternRingSpikes -> ActionCastAll;
            case PatternUnderSpikes -> ActionCastMid;
            default -> ActionSummon;
        };
        if (Chosen == PatternSpirits) {
            SpiritWait = SpiritCooldown;
        }
        startAction(Kind, 100);
    }

    private double groundUnder(ServerLevel World, Entity Body) {
        OptionalDouble Ground = DaeviteGodTargets.groundAt(World, Body.getX(), Body.getZ(), Body.getY(), 16);
        return Ground.isPresent() ? Ground.getAsDouble() : Body.getY();
    }

    private Vec3 surface(ServerLevel World, Vec3 Point, double Around) {
        OptionalDouble Ground = DaeviteGodTargets.groundAt(World, Point.x, Point.z, Around, 12);
        return new Vec3(Point.x, Ground.isPresent() ? Ground.getAsDouble() : Around, Point.z);
    }

    private void release(ServerLevel World, LivingEntity Target, byte Chosen) {
        boolean Hard = World.getDifficulty() == Difficulty.HARD;
        double Around = Target.getY();
        Vec3 Aim = predict(Target, 12.0);
        switch (Chosen) {
            case PatternUnderLightning -> {
                DaeviteGodSigil.cast(World, surface(World, Aim, Around), DaeviteGodSigil.KindLightning, 0, LightningTelegraph, LightningRadius, LightningDamage, this);
                if (Hard) {
                    for (int Index = 0; Index < 2; Index++) {
                        double Angle = random.nextDouble() * Math.PI * 2.0;
                        Vec3 Spot = Aim.add(Math.cos(Angle) * 3.2, 0.0, Math.sin(Angle) * 3.2);
                        DaeviteGodSigil.cast(World, surface(World, Spot, Around), DaeviteGodSigil.KindLightning, 6 + Index * 6, LightningTelegraph, LightningRadius, LightningDamage, this);
                    }
                }
            }
            case PatternVolley -> {
                Vec3[] Spots = {Target.position(), predict(Target, 18.0), Target.position().add(random.nextGaussian() * 2.5, 0.0, random.nextGaussian() * 2.5)};
                for (int Index = 0; Index < Spots.length; Index++) {
                    DaeviteGodSigil.cast(World, surface(World, Spots[Index], Around), DaeviteGodSigil.KindLightning, Index * 7, LightningTelegraph, LightningRadius, LightningDamage, this);
                }
            }
            case PatternLineSpikes -> {
                Vec3 From = position().multiply(1.0, 0.0, 1.0);
                Vec3 Line = Aim.multiply(1.0, 0.0, 1.0).subtract(From);
                double Length = Math.max(4.0, Line.length() + 4.0);
                Vec3 Step = Line.lengthSqr() > 1.0e-3 ? Line.normalize() : Vec3.directionFromRotation(0.0f, getYRot());
                int Count = (int) Math.min(12, Math.ceil(Length / 1.8));
                for (int Index = 1; Index <= Count; Index++) {
                    Vec3 Spot = From.add(Step.scale(Index * 1.8)).add(0.0, Around, 0.0);
                    DaeviteGodSigil.cast(World, surface(World, Spot, Around), DaeviteGodSigil.KindSpikes, Index * 3, SpikeTelegraph, SpikeRadius, SpikesDamage, this);
                }
            }
            case PatternRingSpikes -> {
                double Turn = random.nextDouble() * Math.PI * 2.0;
                int Count = Hard ? 6 : 5;
                for (int Index = 0; Index < Count; Index++) {
                    double Angle = Turn + Index * Math.PI * 2.0 / Count;
                    Vec3 Spot = Target.position().add(Math.cos(Angle) * 3.2, 0.0, Math.sin(Angle) * 3.2);
                    DaeviteGodSigil.cast(World, surface(World, Spot, Around), DaeviteGodSigil.KindSpikes, 0, SpikeTelegraph + 2, SpikeRadius, SpikesDamage, this);
                }
                DaeviteGodSigil.cast(World, surface(World, Target.position(), Around), DaeviteGodSigil.KindSpikes, 10, SpikeTelegraph, SpikeRadius, SpikesDamage, this);
            }
            case PatternSelfRing -> {
                Vec3 Centre = surface(World, position(), getY());
                for (int Index = 0; Index < 8; Index++) {
                    double Angle = Index * Math.PI / 4.0;
                    Vec3 Spot = Centre.add(Math.cos(Angle) * 3.4, 0.0, Math.sin(Angle) * 3.4);
                    DaeviteGodSigil.cast(World, surface(World, Spot, Centre.y + 1.0), DaeviteGodSigil.KindSpikes, Index % 2 * 4, SpikeTelegraph, SpikeRadius, SpikesDamage, this);
                }
                DaeviteGodSigil.cast(World, surface(World, Target.position(), Around), DaeviteGodSigil.KindSpikes, 8, SpikeTelegraph, SpikeRadius, SpikesDamage, this);
            }
            case PatternUnderSpikes -> {
                DaeviteGodSigil.cast(World, surface(World, Aim, Around), DaeviteGodSigil.KindSpikes, 0, SpikeTelegraph, SpikeRadius + 0.3f, SpikesDamage, this);
                Vec3 Side = Vec3.directionFromRotation(0.0f, getYRot() + 90.0f);
                for (int Flank = -1; Flank <= 1; Flank += 2) {
                    DaeviteGodSigil.cast(World, surface(World, Aim.add(Side.scale(Flank * 2.4)), Around), DaeviteGodSigil.KindSpikes, 5, SpikeTelegraph, SpikeRadius, SpikesDamage, this);
                }
            }
            case PatternKingThorns -> {
                DaeviteGodSigil.cast(World, surface(World, Aim, Around), DaeviteGodSigil.KindSpikes, 0, SpikeTelegraph, SpikeRadius + 0.3f, SpikesDamage, this);
                for (int Index = 0; Index < 3; Index++) {
                    double Angle = random.nextDouble() * Math.PI * 2.0;
                    Vec3 Spot = Aim.add(Math.cos(Angle) * 2.6, 0.0, Math.sin(Angle) * 2.6);
                    DaeviteGodSigil.cast(World, surface(World, Spot, Around), Index == 0 ? DaeviteGodSigil.KindLightning : DaeviteGodSigil.KindSpikes, 4 + Index * 4,
                            Index == 0 ? LightningTelegraph : SpikeTelegraph, Index == 0 ? LightningRadius : SpikeRadius, Index == 0 ? LightningDamage : SpikesDamage, this);
                }
            }
            default -> {
                int Count = 2 + random.nextInt(Hard ? 3 : 2);
                double Turn = random.nextDouble() * Math.PI * 2.0;
                Vec3 Centre = horizontalGap(Target) > 16.0 ? position() : Target.position();
                for (int Index = 0; Index < Count; Index++) {
                    double Angle = Turn + Index * Math.PI * 2.0 / Count;
                    Vec3 Spot = Centre.add(Math.cos(Angle) * 3.5, 0.0, Math.sin(Angle) * 3.5);
                    DaeviteGodSigil.cast(World, surface(World, Spot, Around), DaeviteGodSigil.KindSpirits, Index * 4, SpiritTelegraph, SpiritRadius, 0.0f, this);
                }
            }
        }
    }

    private long spiritsNear(ServerLevel World) {
        return World.getEntitiesOfClass(DaeviteGodSpirit.class, getBoundingBox().inflate(64.0), Spirit -> getUUID().equals(Spirit.ownerId()) && Spirit.isAlive()).size();
    }

    public void vanishSpirits(ServerLevel World) {
        for (DaeviteGodSpirit Spirit : World.getEntitiesOfClass(DaeviteGodSpirit.class, getBoundingBox().inflate(160.0), Found -> Found.isAlive())) {
            if (Spirit.ownerId() == null || getUUID().equals(Spirit.ownerId())) {
                Spirit.fade();
            }
        }
    }

    private void tickKing(ServerLevel World, @Nullable LivingEntity Target) {
        byte Current = action();
        if (Current != ActionNone) {
            tickKingAction(World, Target, Current);
            return;
        }
        if (Target == null) {
            BlockPos Anchor = home();
            if (Anchor != null && Anchor.distSqr(blockPosition()) > 144.0 && tickCount % 20 == 0) {
                getNavigation().moveTo(Anchor.getX() + 0.5, Anchor.getY(), Anchor.getZ() + 0.5, WalkModifier);
            }
            return;
        }
        faceTarget(Target, 18.0f);
        double Gap = horizontalGap(Target);
        double Rise = Target.getY() - getY();
        boolean Sees = hasLineOfSight(Target);
        LostSight = Sees ? 0 : LostSight + 1;
        Path Route = getNavigation().getPath();
        boolean Stalled = Gap > 3.5 && (Route == null || getNavigation().isStuck() || Route.isDone() && Gap > 4.5 || Rise > 3.0 && Gap < 6.0);
        Unreachable = Stalled ? Unreachable + 1 : Math.max(0, Unreachable - 2);
        if (Gap <= MeleeStart && Math.abs(Rise) < 3.0 && Sees && MeleeWait <= 0) {
            ComboLeft = getHealth() < MaxHealth * 0.25 ? 2 : 1 + random.nextInt(2);
            startMelee(Gap, 100);
            return;
        }
        if (Gap >= ThrustMin && Gap <= ThrustMax && Math.abs(Rise) < 1.6 && Sees && ThrustWait <= 0 && random.nextFloat() < 0.4f) {
            startAction(ActionThrust, 100);
            ThrustWait = ThrustCooldown;
            playSound(DaeviteGodRegistry.SwingHeavy.get(), 1.0f, 0.7f);
            return;
        }
        if (Gap >= LeapMin && Gap <= LeapMax && Rise < 10.0 && (Sees || Unreachable > UnreachableLimit) && LeapWait <= 0) {
            startAction(ActionLeap, 100);
            LeapWait = LeapCooldown + random.nextInt(40);
            return;
        }
        if (Unreachable > UnreachableLimit && SpellWait <= 0) {
            Pattern = PatternKingThorns;
            startAction(ActionKingCast, 100);
            SpellWait = KingSpellCooldown;
            playLayers(DaeviteGodRegistry.CastLayers, 1.0f, 0.7f);
            return;
        }
        if ((Unreachable > UnreachableLimit * 3 || Gap > FarRange || LostSight > LostSightLimit * 2) && BlinkWait <= 0) {
            Vec3 Spot = blinkSpot(World, Target);
            if (Spot != null) {
                BlinkTarget = Spot;
                startAction(ActionKingBlink, 100);
                playLayers(DaeviteGodRegistry.BlinkLayers, 1.0f, 0.7f);
                BlinkWait = BlinkCooldown;
                Unreachable = 0;
                LostSight = 0;
                return;
            }
        }
        if (tickCount % 8 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(Target, Gap > 8.0 ? RunModifier : WalkModifier);
        }
    }

    private void startMelee(double Gap, int Speed) {
        byte Kind;
        float Roll = random.nextFloat();
        if (Gap < 2.6) {
            Kind = Roll < 0.6f ? ActionSweep : ActionCleave;
        } else {
            Kind = Roll < 0.5f ? ActionCleave : Roll < 0.85f ? ActionSweep : ActionThrust;
        }
        if (Kind == ActionThrust) {
            ThrustWait = ThrustCooldown;
        }
        startAction(Kind, Speed);
        playLayers(DaeviteGodRegistry.SwingLayers, 1.0f, Kind == ActionCleave ? 0.65f : 0.8f);
    }

    private void tickKingAction(ServerLevel World, @Nullable LivingEntity Target, byte Current) {
        double Clip = clipTicks();
        int Time = (int) actionElapsed();
        if (Current == ActionKingBlink) {
            tickBlink(World, Time);
            return;
        }
        if (Target != null && Current != ActionLeap && Current != ActionSlam && !(Current == ActionThrust && Clip >= ThrustDash)) {
            faceTarget(Target, Current == ActionSweep ? 12.0f : 8.0f);
        }
        switch (Current) {
            case ActionCleave -> {
                holdStill();
                if (Clip >= CleaveHit && !ActionFired) {
                    ActionFired = true;
                    cleave(World);
                }
                if (Clip >= CleaveTicks) {
                    finishMelee(Target);
                }
            }
            case ActionSweep -> {
                holdStill();
                if (Clip >= SweepHit && !ActionFired) {
                    ActionFired = true;
                    sweep(World);
                }
                if (Clip >= SweepTicks) {
                    finishMelee(Target);
                }
            }
            case ActionThrust -> {
                if (Clip < ThrustDash) {
                    holdStill();
                } else if (Clip < ThrustDashEnd) {
                    if (!ActionFired) {
                        ActionFired = true;
                        playSound(DaeviteGodRegistry.Thrust.get(), 1.0f, 0.8f);
                    }
                    Vec3 Ahead = Vec3.directionFromRotation(0.0f, getYRot());
                    setDeltaMovement(Ahead.x * DashSpeed, getDeltaMovement().y, Ahead.z * DashSpeed);
                    hasImpulse = true;
                    thrustHits(World);
                } else {
                    setDeltaMovement(getDeltaMovement().multiply(0.4, 1.0, 0.4));
                }
                if (Clip >= ThrustTicks) {
                    finishMelee(Target);
                }
            }
            case ActionLeap -> tickLeap(World, Target, Time);
            case ActionSlam -> {
                holdStill();
                if (Time >= SlamTicks) {
                    endAction();
                    MeleeWait = 10;
                }
            }
            case ActionKingCast -> {
                holdStill();
                if (Time >= KingCastRelease && !ActionFired) {
                    ActionFired = true;
                    playLayers(DaeviteGodRegistry.CastLayers, 1.0f, 0.75f);
                    if (Target != null) {
                        release(World, Target, PatternKingThorns);
                    }
                }
                if (Time >= KingCastTicks) {
                    endAction();
                }
            }
            default -> endAction();
        }
    }

    private void holdStill() {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.3, 1.0, 0.3));
    }

    private void finishMelee(@Nullable LivingEntity Target) {
        endAction();
        if (ComboLeft > 0 && Target != null && DaeviteGodTargets.isPrey(this, Target) && horizontalGap(Target) <= MeleeStart + 0.8) {
            ComboLeft--;
            startMelee(horizontalGap(Target), ComboSpeed);
            return;
        }
        ComboLeft = 0;
        MeleeWait = MeleeCooldown + random.nextInt(8);
    }

    private List<LivingEntity> victims(ServerLevel World, double Reach) {
        return World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Reach, 2.0, Reach), Body -> DaeviteGodTargets.canHurt(this, Body));
    }

    private boolean inArc(Entity Body, double Reach, double Degrees) {
        Vec3 Toward = Body.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        double Distance = Toward.length() - Body.getBbWidth() * 0.5;
        if (Distance > Reach) {
            return false;
        }
        if (Toward.lengthSqr() < 0.25) {
            return true;
        }
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        return Toward.normalize().dot(Facing) >= Math.cos(Math.toRadians(Degrees));
    }

    private boolean blade(LivingEntity Body, float Amount, boolean Heavy) {
        DamageSource Source = damageSources().source(DaeviteGodRegistry.BladeDamage, this, this);
        HeavyStrike = Heavy;
        try {
            return Body.hurt(Source, Amount);
        } finally {
            HeavyStrike = false;
        }
    }

    private void cleave(ServerLevel World) {
        playLayers(DaeviteGodRegistry.ImpactLayers, 1.0f, 0.75f + random.nextFloat() * 0.1f);
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        for (int Step = 1; Step <= 5; Step++) {
            Vec3 Spot = position().add(Facing.scale(Step * 0.9));
            BlockState Ground = World.getBlockState(BlockPos.containing(Spot.x, getY() - 0.5, Spot.z));
            if (!Ground.isAir()) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Spot.x, getY() + 0.1, Spot.z, 8, 0.3, 0.1, 0.3, 0.15);
            }
            World.sendParticles(DaeviteGodRegistry.Ember.get(), Spot.x, getY() + 0.2, Spot.z, 3, 0.2, 0.1, 0.2, 0.04);
        }
        for (LivingEntity Body : victims(World, CleaveReach)) {
            if (inArc(Body, CleaveReach, 40.0) && blade(Body, CleaveDamage, true)) {
                Body.knockback(0.5, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
                setLastHurtMob(Body);
            }
        }
    }

    private void sweep(ServerLevel World) {
        playLayers(DaeviteGodRegistry.ImpactLayers, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        World.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Facing.x * 2.0, getY() + 1.6, getZ() + Facing.z * 2.0, 3, 1.2, 0.2, 1.2, 0.0);
        for (LivingEntity Body : victims(World, SweepReach)) {
            if (inArc(Body, SweepReach, 85.0) && blade(Body, SweepDamage, false)) {
                Vec3 Away = Body.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-3) {
                    Body.knockback(1.2, -Away.x, -Away.z);
                }
                setLastHurtMob(Body);
            }
        }
    }

    private void thrustHits(ServerLevel World) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Tip = position().add(Facing.scale(2.6)).add(0.0, 1.6, 0.0);
        AABB Reach = new AABB(Tip, Tip).inflate(1.6, 1.4, 1.6);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, Reach, Found -> DaeviteGodTargets.canHurt(this, Found))) {
            if (Struck.add(Body.getId()) && blade(Body, ThrustDamage, false)) {
                Body.knockback(0.9, -Facing.x, -Facing.z);
                playSound(DaeviteGodRegistry.ImpactRing.get(), 1.0f, 1.1f);
                setLastHurtMob(Body);
            }
        }
    }

    private void tickLeap(ServerLevel World, @Nullable LivingEntity Target, int Time) {
        if (Time < LeapLaunch) {
            holdStill();
            if (Target != null) {
                faceTarget(Target, 20.0f);
            }
            return;
        }
        if (Time == LeapLaunch) {
            Vec3 Goal = Target != null ? predict(Target, 14.0) : position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(8.0));
            launch(Goal);
            playSound(DaeviteGodRegistry.Leap.get(), 1.0f, 0.75f);
            World.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 12, 0.6, 0.1, 0.6, 0.05);
            Airborne = true;
            return;
        }
        if (Airborne && onGround() && Time > LeapLaunch + 3) {
            Airborne = false;
            land(World);
            return;
        }
        if (Time > LeapLimit) {
            endAction();
        }
    }

    private void launch(Vec3 Goal) {
        Vec3 Flat = Goal.subtract(position()).multiply(1.0, 0.0, 1.0);
        double Distance = Flat.length();
        int Flight = Mth.clamp((int) Math.round(Distance / 0.75), 12, 24);
        double Drag = 0.91;
        double Horizontal = Distance * (1.0 - Drag) / (1.0 - Math.pow(Drag, Flight));
        double Gravity = getAttributeValue(Attributes.GRAVITY);
        double Rise = Goal.y - getY();
        double Best = 0.9;
        double BestError = Double.MAX_VALUE;
        for (double Lift = 0.4; Lift <= 2.0; Lift += 0.02) {
            double Y = 0.0;
            double Speed = Lift;
            for (int Tick = 0; Tick < Flight; Tick++) {
                Y += Speed;
                Speed = (Speed - Gravity) * 0.98;
            }
            double Error = Math.abs(Y - Rise);
            if (Error < BestError) {
                BestError = Error;
                Best = Lift;
            }
        }
        Vec3 Direction = Distance > 1.0e-3 ? Flat.scale(1.0 / Distance) : Vec3.directionFromRotation(0.0f, getYRot());
        setDeltaMovement(Direction.x * Horizontal, Best, Direction.z * Horizontal);
        hasImpulse = true;
    }

    private void land(ServerLevel World) {
        startAction(ActionSlam, 100);
        playLayers(DaeviteGodRegistry.SlamLayers, 1.0f, 0.7f + random.nextFloat() * 0.1f);
        BlockState Ground = World.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), getY() + 0.1, getZ(), 60, 1.8, 0.2, 1.8, 0.3);
        }
        World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 0.3, getZ(), 40, 1.5, 0.3, 1.5, 0.12);
        for (LivingEntity Body : victims(World, SlamRadius + 1.0)) {
            double Distance = Math.sqrt(Mth.square(Body.getX() - getX()) + Mth.square(Body.getZ() - getZ()));
            if (Distance <= SlamRadius + Body.getBbWidth() * 0.5 && Math.abs(Body.getY() - getY()) < 2.5 && blade(Body, SlamDamage, true)) {
                Body.setDeltaMovement(Body.getDeltaMovement().add(0.0, 0.6, 0.0));
                Body.hurtMarked = true;
                Struck.add(Body.getId());
            }
        }
        ShockTimer = 0;
        ShockCentre = position();
    }

    private void tickShock(ServerLevel World) {
        if (ShockTimer < 0) {
            return;
        }
        ShockTimer++;
        double Radius = SlamRadius + (ShockReach - SlamRadius) * ShockTimer / ShockTicks;
        if (ShockTimer == 2) {
            World.playSound(null, ShockCentre.x, ShockCentre.y, ShockCentre.z, DaeviteGodRegistry.Shockwave.get(), SoundSource.HOSTILE, 1.0f, 0.75f);
        }
        int Points = (int) (Radius * 6.0);
        for (int Index = 0; Index < Points; Index++) {
            double Angle = Index * Math.PI * 2.0 / Points;
            double X = ShockCentre.x + Math.cos(Angle) * Radius;
            double Z = ShockCentre.z + Math.sin(Angle) * Radius;
            BlockState Ground = World.getBlockState(BlockPos.containing(X, ShockCentre.y - 0.5, Z));
            if (!Ground.isAir() && Index % 2 == 0) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), X, ShockCentre.y + 0.1, Z, 1, 0.1, 0.05, 0.1, 0.1);
            }
            if (Index % 3 == 0) {
                World.sendParticles(DaeviteGodRegistry.Ember.get(), X, ShockCentre.y + 0.15, Z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
        DamageSource Source = damageSources().source(DaeviteGodRegistry.ShockwaveDamage, this, this);
        AABB Reach = new AABB(ShockCentre, ShockCentre).inflate(Radius + 1.0, 1.5, Radius + 1.0);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, Reach, Found -> DaeviteGodTargets.canHurt(this, Found))) {
            double Distance = Math.sqrt(Mth.square(Body.getX() - ShockCentre.x) + Mth.square(Body.getZ() - ShockCentre.z));
            if (Math.abs(Distance - Radius) > 0.8 || !Body.onGround() || Math.abs(Body.getY() - ShockCentre.y) > 1.2 || Struck.contains(Body.getId())) {
                continue;
            }
            Struck.add(Body.getId());
            if (Body.hurt(Source, ShockDamage)) {
                Vec3 Away = Body.position().subtract(ShockCentre).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-3) {
                    Away = Away.normalize().scale(0.9);
                    Body.setDeltaMovement(Body.getDeltaMovement().add(Away.x, 0.35, Away.z));
                    Body.hurtMarked = true;
                }
            }
        }
        if (ShockTimer >= ShockTicks) {
            ShockTimer = -1;
        }
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    private void clientTick() {
        DaeviteGodGait Current = gait();
        if (Current != null) {
            Current.tick(this);
        }
        if (!isAlive()) {
            return;
        }
        if (!king()) {
            if (tickCount % 2 == 0) {
                level().addParticle(DaeviteGodRegistry.Ember.get(), getRandomX(0.5), getY() + random.nextDouble() * 0.3, getRandomZ(0.5), 0.0, -0.03, 0.0);
            }
            if (tickCount % 5 == 0) {
                level().addParticle(DaeviteGodRegistry.Ember.get(), getRandomX(0.9), getY() + 1.0 + random.nextDouble() * 3.4, getRandomZ(0.9), 0.0, 0.02, 0.0);
            }
        } else if (tickCount % 4 == 0) {
            level().addParticle(DaeviteGodRegistry.Ember.get(), getRandomX(0.6), getY() + 0.5 + random.nextDouble() * 2.6, getRandomZ(0.6), 0.0, 0.02, 0.0);
        }
        if (entering() && tickCount % 2 == 0) {
            for (int Index = 0; Index < 3; Index++) {
                level().addParticle(DaeviteGodRegistry.Ember.get(), getX() + (random.nextDouble() - 0.5) * 2.4, getY() + random.nextDouble() * 12.0, getZ() + (random.nextDouble() - 0.5) * 2.4,
                        0.0, 0.08, 0.0);
            }
        }
    }

    @Nullable
    public DaeviteGodGait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new DaeviteGodGait();
        }
        return Gait;
    }

    public float skyDarkness(float Partial) {
        if (isDeadOrDying()) {
            return Mth.clamp(0.6f * (1.0f - (deathTime + Partial) / DeathTicks), 0.0f, 0.6f);
        }
        double Elapsed = phaseElapsed();
        return switch (phase()) {
            case PhaseEntrance -> (float) Mth.clamp(Elapsed / 40.0, 0.0, 1.0) * 0.85f;
            case PhaseTransform -> 0.85f;
            default -> 0.45f;
        };
    }

    public float pillar(float Partial) {
        if (!entering()) {
            return 0.0f;
        }
        double Elapsed = phaseElapsed();
        double In = Mth.clamp(Elapsed / 6.0, 0.0, 1.0);
        double Out = Mth.clamp((EntranceTicks - 20 - Elapsed) / 20.0, 0.0, 1.0);
        return (float) (In * Out);
    }

    public float collapseLight(float Partial) {
        if (!transforming()) {
            return 0.0f;
        }
        double Time = phaseElapsed();
        if (Time < TransformCollapse - 8) {
            return (float) Mth.clamp((Time - (TransformCollapse - 26)) / 18.0, 0.0, 1.0) * 0.4f;
        }
        if (Time < TransformSwap + 6) {
            return 1.0f;
        }
        return (float) Mth.clamp(1.0 - (Time - TransformSwap - 6) / 20.0, 0.0, 1.0);
    }

    public float deathProgress(float Partial) {
        return isDeadOrDying() ? Mth.clamp((deathTime + Partial) / DeathTicks, 0.0f, 1.0f) : 0.0f;
    }

    public static boolean absolute(DamageSource Source) {
        return Source.is(DamageTypes.GENERIC_KILL) || Source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (absolute(Source)) {
            return false;
        }
        if (busy() || action() == ActionGodBlink && actionElapsed() >= BlinkVanish - 2 && actionElapsed() <= BlinkVanish + 2) {
            return true;
        }
        if (DaeviteGodTargets.isAlly(Source.getEntity()) || DaeviteGodTargets.isAlly(Source.getDirectEntity())) {
            return true;
        }
        if (Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypeTags.IS_FREEZING) || Source.is(DamageTypeTags.IS_LIGHTNING)
                || Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING) || Source.is(DamageTypes.CACTUS) || Source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        float Capped = absolute(Source) ? Amount : Math.min(Amount, HitCap);
        boolean Hurt = super.hurt(Source, Capped);
        if (Hurt && !level().isClientSide) {
            LastHurt = tickCount;
            IdleTicks = 0;
            Entity Attacker = Source.getEntity();
            if (isAlive() && Attacker instanceof LivingEntity Body && Body != this && !DaeviteGodTargets.isAlly(Body)) {
                LivingEntity Current = getTarget();
                if (Current == null || DaeviteGodTargets.isPrey(this, Body) && distanceToSqr(Body) < distanceToSqr(Current) * 0.5 || !DaeviteGodTargets.isPrey(this, Current)) {
                    setTarget(Body);
                }
            }
        }
        return Hurt;
    }

    @Override
    protected void actuallyHurt(DamageSource Source, float Amount) {
        super.actuallyHurt(Source, Amount);
        if (!king() && !absolute(Source)) {
            float Floor = (float) (MaxHealth * TransformShare);
            if (getHealth() < Floor) {
                setHealth(Floor);
            }
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (!dead || SlainFired || !(level() instanceof ServerLevel World)) {
            return;
        }
        endAction();
        setDeltaMovement(Vec3.ZERO);
        playLayers(DaeviteGodRegistry.DeathLayers, 1.0f, king() ? 0.8f : 0.65f);
        fallen(World, Source.getEntity() instanceof LivingEntity Found ? Found : getKillCredit());
    }

    private void fallen(ServerLevel World, @Nullable LivingEntity Slayer) {
        SlainFired = true;
        vanishSpirits(World);
        DaeviteGodLedger.get(World.getServer()).slain(this);
        DaeviteGodSlainEvent.fire(World, blockPosition(), Slayer);
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (Reason == RemovalReason.KILLED && !SlainFired && level() instanceof ServerLevel World) {
            fallen(World, getKillCredit());
        }
        super.remove(Reason);
    }

    @Override
    protected void tickDeath() {
        if (!SlainFired && !dead && level() instanceof ServerLevel) {
            die(damageSources().generic());
        }
        deathTime++;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        AABB Box = getBoundingBox();
        double Share = Math.min(1.0, deathTime / (double) DeathTicks);
        double Top = Box.maxY - (Box.maxY - Box.minY) * Share * 0.85;
        World.sendParticles(DaeviteGodRegistry.Ash.get(), getX(), (Box.minY + Top) * 0.5, getZ(), 4 + deathTime / 5, getBbWidth() * 0.35, (Top - Box.minY) * 0.3, getBbWidth() * 0.35, 0.02);
        World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), Top, getZ(), 3, getBbWidth() * 0.3, 0.2, getBbWidth() * 0.3, 0.05);
        if (deathTime % 4 == 0) {
            World.sendParticles(ParticleTypes.END_ROD, getX(), Box.minY + 0.5, getZ(), 2, 0.2, 0.2, 0.2, 0.12);
        }
        if (deathTime == 20) {
            playSound(DaeviteGodRegistry.Crumble.get(), 1.0f, 0.75f);
        }
        if (deathTime >= DeathTicks && !isRemoved()) {
            World.sendParticles(DaeviteGodRegistry.Ash.get(), getX(), Box.minY + 0.4, getZ(), 70, getBbWidth() * 0.4, 0.3, getBbWidth() * 0.4, 0.05);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), Box.minY + 0.5, getZ(), 50, 0.4, 2.5, 0.4, 0.12);
            playSound(DaeviteGodRegistry.ReformBell.get(), 1.0f, 0.5f);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean canDisableShield() {
        return HeavyStrike || super.canDisableShield();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return Effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(Effect);
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
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
    public void push(double X, double Y, double Z) {
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(FluidType Kind) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void thunderHit(ServerLevel World, LightningBolt Bolt) {
    }

    @Override
    public boolean startRiding(Entity Vehicle, boolean Force) {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean Passengers) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void checkDespawn() {
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
    public boolean isAlliedTo(Entity Other) {
        return DaeviteGodTargets.isAlly(Other) || super.isAlliedTo(Other);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return busy() ? null : (king() ? DaeviteGodRegistry.KingAmbient : DaeviteGodRegistry.Ambient).get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DaeviteGodRegistry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(DaeviteGodRegistry.HurtLayers, 1.0f, king() ? 0.95f : 0.75f + random.nextFloat() * 0.1f);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0f;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (entering()) {
            return super.getBoundingBoxForCulling().inflate(3.0, 64.0, 3.0);
        }
        return super.getBoundingBoxForCulling().inflate(king() ? 3.0 : 2.5, 1.5, king() ? 3.0 : 2.5);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = entering() ? 256.0 : 128.0;
        return Distance < Range * Range;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putByte("Stage", stage());
        Saved.putBoolean("Claimed", Claimed);
        Saved.putBoolean("Slain", SlainFired);
        BlockPos Anchor = home();
        if (Anchor != null) {
            Saved.put("Home", NbtUtils.writeBlockPos(Anchor));
        }
        Saved.putInt("IdleTicks", IdleTicks);
        if (transforming() && !king()) {
            Saved.putBoolean("Transforming", true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        byte Stage = Saved.getByte("Stage");
        boolean Pending = Saved.getBoolean("Transforming");
        setStage(Stage >= 2 || Pending ? (byte) 2 : (byte) 1);
        if (Pending) {
            setHealth(Math.min(getHealth(), stageCap()));
        }
        Claimed = Saved.getBoolean("Claimed");
        SlainFired = Saved.getBoolean("Slain");
        setHome(NbtUtils.readBlockPos(Saved, "Home").orElse(null));
        IdleTicks = Saved.getInt("IdleTicks");
        entityData.set(PhaseData, PhaseActive);
        entityData.set(ActionData, ActionNone);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        FloatController = new DaeviteGodController(this, "float", 4, Event -> {
            if (king()) {
                return PlayState.STOP;
            }
            if (isDeadOrDying()) {
                return Event.setAndContinue(GodFloatDeath);
            }
            if (transforming()) {
                return Event.setAndContinue(GodFloatTransform);
            }
            if (entering()) {
                return Event.setAndContinue(GodFloatEntrance);
            }
            if (action() == ActionGodBlink) {
                return Event.setAndContinue(GodFloatBlink);
            }
            Vec3 Motion = getDeltaMovement();
            return Event.setAndContinue(Motion.x * Motion.x + Motion.z * Motion.z > 0.006 ? GodDrift : GodFloat);
        });
        BodyController = new DaeviteGodController(this, "body", 3, Event -> {
            if (king()) {
                if (isDeadOrDying()) {
                    return Event.setAndContinue(KingDeath);
                }
                if (transforming()) {
                    return Event.setAndContinue(KingReform);
                }
                String Clip = actionClip();
                if (Clip != null) {
                    return Event.setAndContinue(DaeviteGodController.play(Clip));
                }
                DaeviteGodGait Current = gait();
                if (Current != null && Current.striding()) {
                    return Event.setAndContinue(Current.running() ? KingRun : KingWalk);
                }
                return Event.setAndContinue(KingIdle);
            }
            if (isDeadOrDying()) {
                return Event.setAndContinue(GodDeath);
            }
            if (transforming()) {
                return Event.setAndContinue(GodTransform);
            }
            if (entering()) {
                return Event.setAndContinue(GodEntrance);
            }
            String Clip = actionClip();
            if (Clip != null) {
                return Event.setAndContinue(DaeviteGodController.play(Clip));
            }
            return Event.setAndContinue(GodArmsIdle);
        });
        Controllers.add(FloatController);
        Controllers.add(BodyController);
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionCastLow -> "god_cast_low";
            case ActionCastMid -> "god_cast_mid";
            case ActionCastHigh -> "god_cast_high";
            case ActionCastAll -> "god_cast_all";
            case ActionSummon -> "god_summon";
            case ActionGodBlink -> "god_blink";
            case ActionCleave -> "king_cleave";
            case ActionSweep -> "king_sweep";
            case ActionThrust -> "king_thrust";
            case ActionLeap -> "king_leap";
            case ActionSlam -> "king_slam";
            case ActionKingCast -> "king_cast";
            case ActionKingBlink -> "king_blink";
            default -> null;
        };
    }

    @Nullable
    public String phaseClip() {
        if (isDeadOrDying()) {
            return king() ? "king_death" : "god_death";
        }
        if (transforming()) {
            return king() ? "king_reform" : "god_transform";
        }
        if (entering()) {
            return "god_entrance";
        }
        return null;
    }

    public double phaseClipTicks() {
        if (isDeadOrDying()) {
            return deathTime + RenderPartial;
        }
        if (transforming() && king()) {
            return Math.max(0.0, phaseElapsed() - TransformSwap);
        }
        return phaseElapsed();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
