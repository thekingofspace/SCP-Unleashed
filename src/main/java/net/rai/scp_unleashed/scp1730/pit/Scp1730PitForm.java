package net.rai.scp_unleashed.scp1730.pit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import net.rai.scp_unleashed.scp1730.network.Scp1730ShakePayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730PitForm extends Monster implements GeoEntity {
    public static final float Width = 6.5f;
    public static final float Height = 9.0f;
    public static final float EyeHeight = 6.5f;
    public static final double Depth = 1.0;
    public static final double Submerged = 12.0;
    public static final double MouthForward = 3.1;
    public static final double MouthUp = 5.0;
    public static final double Inset = 2.5;
    public static final double RoomReach = 10.0;
    public static final double ArenaReach = 26.0;
    public static final double VirtualHalf = 6.0;
    public static final float MaxHealth = 640.0f;
    public static final float NormalShare = 0.12f;
    public static final float AnchoredShare = 0.35f;
    public static final float WeaknessMax = 100.0f;
    public static final float WeaknessGain = 0.6f;
    public static final float WeaknessDecay = 0.08f;
    public static final float RecoverWeakness = 45.0f;
    public static final float RecoverHeal = 0.08f;
    public static final int EmergeTicks = 100;
    public static final int DazeTicks = 240;
    public static final int RecoverTicks = 50;
    public static final int SinkTicks = 60;
    public static final int DeathTicks = 200;
    public static final int LonelyTicks = 200;
    public static final int TongueTelegraph = 26;
    public static final int TongueHold = 70;
    public static final double TongueSpeed = 1.4;
    public static final double TongueReach = 13.0;
    public static final double TongueGrab = 1.1;
    public static final double TonguePull = 0.3;
    public static final int StruggleFree = 18;
    public static final float TongueBite = 6.0f;
    public static final int BiteTelegraph = 18;
    public static final int BiteRecover = 14;
    public static final double BiteRadius = 3.6;
    public static final float BiteDamage = 9.0f;
    public static final int SpitTelegraph = 20;
    public static final int SpitRecover = 12;
    public static final int LeechCap = 6;
    public static final int LeechesPerSpit = 3;
    public static final int SlamLength = 54;
    public static final int PullTelegraph = 50;
    public static final int PullTicks = 50;
    public static final double PullRadius = 9.0;
    public static final double PullStrength = 0.05;
    public static final int PullRest = 900;
    public static final byte StateDormant = 0;
    public static final byte StateEmerging = 1;
    public static final byte StateActive = 2;
    public static final byte StateDazed = 3;
    public static final byte StateRecovering = 4;
    public static final byte StateSinking = 5;
    public static final byte ActionNone = 0;
    public static final byte ActionTongue = 1;
    public static final byte ActionBite = 2;
    public static final byte ActionSpit = 3;
    public static final byte ActionSlam = 4;
    public static final byte ActionPull = 5;
    public static final int EyeCount = 10;
    private static final EntityDataAccessor<Byte> DataState = SynchedEntityData.defineId(Scp1730PitForm.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DataAction = SynchedEntityData.defineId(Scp1730PitForm.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DataWeakness = SynchedEntityData.defineId(Scp1730PitForm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DataAnchored = SynchedEntityData.defineId(Scp1730PitForm.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation DormantHold = RawAnimation.begin().thenPlayAndHold("dormant");
    private static final RawAnimation EmergeOnce = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ThrashLoop = RawAnimation.begin().thenLoop("thrash");
    private static final RawAnimation DazedLoop = RawAnimation.begin().thenPlay("slump").thenLoop("dazed");
    private static final RawAnimation RecoverOnce = RawAnimation.begin().thenPlayAndHold("recover");
    private static final RawAnimation SinkOnce = RawAnimation.begin().thenPlayAndHold("sink");
    private static final RawAnimation DeathHold = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation TongueOnce = RawAnimation.begin().thenPlay("tongue");
    private static final RawAnimation BiteOnce = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation SpitOnce = RawAnimation.begin().thenPlay("spit");
    private static final RawAnimation SlamOnce = RawAnimation.begin().thenPlay("slam");
    private static final RawAnimation PullOnce = RawAnimation.begin().thenPlay("pull");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent Bar = new ServerBossEvent(Component.translatable("entity.scp_unleashed.scp1730_3"), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean HasHome;
    private double HomeX;
    private double HomeZ;
    private double HalfX = VirtualHalf;
    private double HalfZ = VirtualHalf;
    private int RimY;
    private float Weakness;
    private int StateTicks;
    private int ActionTicks;
    private int Cooldown = 40;
    private int PullWait = 400;
    private int Lonely;
    private int Grabbed = -1;
    private int Struggle;
    private double LastGap = -1.0;
    private double TongueOut;
    private Vec3 TongueAim = Vec3.ZERO;
    private int TongueId = -1;
    private boolean Finished;
    public int ClientStateTicks;
    private byte ClientState = -1;

    public Scp1730PitForm(EntityType<? extends Scp1730PitForm> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, 0.0).add(Attributes.FOLLOW_RANGE, ArenaReach)
                .add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_DAMAGE, BiteDamage);
    }

    public static Scp1730PitForm spawnIn(ServerLevel World, double MinX, double MinZ, double MaxX, double MaxZ, int Rim) {
        Scp1730PitForm Made = Scp1730PitRegistry.PitType.get().create(World);
        if (Made == null) {
            return null;
        }
        Made.setHome((MinX + MaxX) / 2.0, (MinZ + MaxZ) / 2.0, (MaxX - MinX) / 2.0, (MaxZ - MinZ) / 2.0, Rim);
        Made.moveTo(Made.HomeX, Rim - Depth - Submerged, Made.HomeZ, World.random.nextFloat() * 360.0f, 0.0f);
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Made.HomeX, Rim, Made.HomeZ)), MobSpawnType.EVENT, null);
        World.addFreshEntity(Made);
        return Made;
    }

    public void setHome(double X, double Z, double Hx, double Hz, int Rim) {
        HasHome = true;
        HomeX = X;
        HomeZ = Z;
        HalfX = Math.max(Inset + 0.5, Hx);
        HalfZ = Math.max(Inset + 0.5, Hz);
        RimY = Rim;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Data);
        if (!HasHome) {
            setHome(getX(), getZ(), VirtualHalf, VirtualHalf, (int) Math.floor(getY()));
            setPos(getX(), RimY - Depth - Submerged, getZ());
        }
        return Result;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataState, StateDormant);
        Builder.define(DataAction, ActionNone);
        Builder.define(DataWeakness, 0);
        Builder.define(DataAnchored, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (DataState.equals(Key) && level().isClientSide && ClientState != state()) {
            ClientState = state();
            ClientStateTicks = 0;
        }
    }

    public byte state() {
        return entityData.get(DataState);
    }

    public byte action() {
        return entityData.get(DataAction);
    }

    public int weakness() {
        return entityData.get(DataWeakness);
    }

    public float weaknessExact() {
        return Weakness;
    }

    public boolean anchored() {
        return entityData.get(DataAnchored);
    }

    public boolean dazed() {
        return state() == StateDazed && !isDeadOrDying();
    }

    public boolean emerged() {
        byte Now = state();
        return Now == StateActive || Now == StateDazed || Now == StateRecovering;
    }

    public int rimY() {
        return RimY;
    }

    public Vec3 home() {
        return new Vec3(HomeX, RimY, HomeZ);
    }

    public int stage() {
        int Weak = dazed() ? 100 : weakness();
        return Weak >= 66 ? 2 : Weak >= 33 ? 1 : 0;
    }

    public int closedEyes() {
        byte Now = state();
        if (isDeadOrDying() || Now == StateDazed || Now == StateDormant) {
            return EyeCount;
        }
        if (Now == StateEmerging) {
            return Mth.clamp(EyeCount - (ClientStateTicks - 30) / 6, 0, EyeCount);
        }
        return Mth.clamp((int) (weakness() / WeaknessMax * (EyeCount - 1)), 0, EyeCount - 1);
    }

    public void setWeakness(float Value) {
        Weakness = Mth.clamp(Value, 0.0f, WeaknessMax);
        entityData.set(DataWeakness, (int) Weakness);
    }

    private void setState(byte Next) {
        entityData.set(DataState, Next);
        StateTicks = 0;
        if (Next != StateActive) {
            finishAction();
        }
    }

    public void forceState(byte Next) {
        setState(Next);
    }

    public int stateTicks() {
        return StateTicks;
    }

    public void shortenState(int Left) {
        byte Now = state();
        int Length = Now == StateDazed ? DazeTicks : Now == StateEmerging ? EmergeTicks : Now == StateRecovering ? RecoverTicks : StateTicks + Left;
        StateTicks = Math.max(StateTicks, Length - Left);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer User) {
        super.startSeenByPlayer(User);
        if (emerged()) {
            Bar.addPlayer(User);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer User) {
        super.stopSeenByPlayer(User);
        Bar.removePlayer(User);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return state() != StateDormant && state() != StateSinking && !isRemoved();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
    }

    public static float shareFor(boolean Dazed, boolean Anchored) {
        return Dazed ? 1.0f : Anchored ? AnchoredShare : NormalShare;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        byte Now = state();
        if (isDeadOrDying() || Now == StateDormant || Now == StateEmerging || Now == StateSinking) {
            return false;
        }
        if (!level().isClientSide && Source.getEntity() instanceof Player && Grabbed >= 0) {
            releaseGrab(true);
        }
        float Share = shareFor(Now == StateDazed, anchored());
        if (Share < 1.0f && !level().isClientSide && Source.getEntity() != null && random.nextInt(3) == 0) {
            level().playSound(null, getX(), getY() + MouthUp, getZ(), Scp1730PitRegistry.Ward.get(), SoundSource.HOSTILE, 1.4f, 0.8f + random.nextFloat() * 0.3f);
        }
        return super.hurt(Source, Amount * Share);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return emerged() ? Scp1730PitRegistry.Idle.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp1730PitRegistry.Hurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 2.5f;
    }

    public Vec3 facing() {
        return Vec3.directionFromRotation(0.0f, yBodyRot);
    }

    public Vec3 mouth() {
        Vec3 Forward = facing();
        double Lift = MouthUp;
        double Reach = MouthForward;
        if (state() == StateDazed) {
            Lift = 2.2;
            Reach = 4.4;
        }
        return position().add(Forward.scale(Reach)).add(0.0, Lift, 0.0);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        releaseGrab(false);
        dropTongue();
        entityData.set(DataAction, ActionNone);
        Finished = true;
        if (level() instanceof ServerLevel World) {
            cue(World, Scp1730PitRegistry.Death.get(), Scp1730PitRegistry.RoarLow.get(), 3.0f, 0.7f);
            shake(World, 0.5f, 30);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            Vec3 Mouth = mouth();
            if (deathTime % 4 == 0) {
                World.sendParticles(ParticleTypes.SQUID_INK, Mouth.x, Mouth.y, Mouth.z, 6, 1.2, 1.2, 1.2, 0.04);
            }
            if (deathTime > 40) {
                setPos(getX(), getY() - 0.075, getZ());
                if (deathTime % 6 == 0) {
                    rimDust(World, 6);
                }
            }
            if (deathTime == 60 || deathTime == 120) {
                cue(World, Scp1730PitRegistry.RoarLow.get(), Scp1730PitRegistry.Sink.get(), 2.6f, 0.55f);
                shake(World, 0.35f, 20);
            }
            if (deathTime >= DeathTicks && !isRemoved()) {
                World.playSound(null, HomeX, RimY - 10.0, HomeZ, Scp1730PitRegistry.RoarLow.get(), SoundSource.HOSTILE, 3.0f, 0.4f);
                Bar.removeAllPlayers();
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        Bar.removeAllPlayers();
        dropTongue();
        super.remove(Reason);
    }

    public void captured(ServerLevel World) {
        Finished = true;
        releaseGrab(false);
        dropTongue();
        Bar.removeAllPlayers();
    }

    public boolean finished() {
        return Finished;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ClientStateTicks++;
            clientEffects();
        }
    }

    private void clientEffects() {
        if (state() == StateEmerging && random.nextInt(2) == 0) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = random.nextDouble() * Math.max(HalfX, 3.0);
            level().addParticle(ParticleTypes.SQUID_INK, getX() + Math.cos(Angle) * Reach, getY() + Depth + random.nextDouble() * 1.5, getZ() + Math.sin(Angle) * Reach,
                    0.0, 0.08, 0.0);
        }
        if (action() == ActionPull && random.nextInt(1) == 0) {
            for (int Index = 0; Index < 3; Index++) {
                double Angle = random.nextDouble() * Math.PI * 2.0;
                double Reach = 6.0 + random.nextDouble() * 5.0;
                double Px = getX() + Math.cos(Angle) * Reach;
                double Pz = getZ() + Math.sin(Angle) * Reach;
                level().addParticle(ParticleTypes.ASH, Px, getY() + Depth + 0.5 + random.nextDouble() * 2.0, Pz, (getX() - Px) * 0.08, -0.02, (getZ() - Pz) * 0.08);
            }
        }
        if (anchored() && random.nextInt(3) == 0) {
            Vec3 Mouth = mouth();
            level().addParticle(Scp1730Registry.AnchorParticle.get(), Mouth.x + random.nextGaussian(), Mouth.y + random.nextGaussian(), Mouth.z + random.nextGaussian(),
                    0.0, 0.02, 0.0);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isDeadOrDying()) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (!HasHome) {
            setHome(getX(), getZ(), VirtualHalf, VirtualHalf, (int) Math.floor(getY() + Depth));
        }
        Bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 5 == 0) {
            boolean Now = emerged() && Scp1730Anchors.anchored(this);
            if (Now != anchored()) {
                entityData.set(DataAnchored, Now);
                if (Now) {
                    cue(World, Scp1730PitRegistry.Thrash.get(), Scp1730PitRegistry.Hurt.get(), 2.4f, 0.7f);
                }
            }
        }
        StateTicks++;
        switch (state()) {
            case StateDormant -> tickDormant(World);
            case StateEmerging -> tickEmerging(World);
            case StateActive -> tickActive(World);
            case StateDazed -> tickDazed(World);
            case StateRecovering -> tickRecovering(World);
            case StateSinking -> tickSinking(World);
            default -> setState(StateDormant);
        }
    }

    @Override
    protected void customServerAiStep() {
    }

    private boolean valid(Player Each) {
        return Each.isAlive() && !Each.isSpectator() && !Each.isCreative();
    }

    public boolean inRoom(Entity Each) {
        double Dx = Math.abs(Each.getX() - HomeX) - HalfX;
        double Dz = Math.abs(Each.getZ() - HomeZ) - HalfZ;
        return Dx < RoomReach && Dz < RoomReach && Each.getY() > RimY - 3.0 && Each.getY() < RimY + 9.0;
    }

    @Nullable
    private Player pickTarget(ServerLevel World) {
        Player Best = null;
        double BestGap = ArenaReach * ArenaReach;
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(HomeX, RimY, HomeZ, HomeX, RimY, HomeZ).inflate(ArenaReach), this::valid)) {
            if (!inRoom(Each)) {
                continue;
            }
            double Gap = Each.distanceToSqr(mouth());
            if (Gap < BestGap) {
                Best = Each;
                BestGap = Gap;
            }
        }
        return Best;
    }

    private boolean anyoneInRoom(ServerLevel World) {
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(HomeX, RimY, HomeZ, HomeX, RimY, HomeZ).inflate(ArenaReach), Each -> Each.isAlive() && !Each.isSpectator())) {
            if (inRoom(Each)) {
                return true;
            }
        }
        return false;
    }

    private void tickDormant(ServerLevel World) {
        setPos(HomeX, RimY - Depth - Submerged, HomeZ);
        if (StateTicks % 10 == 0 && anyoneInRoom(World)) {
            beginEmerge(World);
        }
    }

    public void beginEmerge(ServerLevel World) {
        setState(StateEmerging);
        cue(World, Scp1730PitRegistry.Rumble.get(), Scp1730PitRegistry.Sink.get(), 3.0f, 0.6f);
        shake(World, 0.3f, 40);
        Player Target = pickTarget(World);
        if (Target != null) {
            faceToward(Target.position(), 360.0f);
        }
    }

    private void tickEmerging(ServerLevel World) {
        double Share = Mth.clamp((StateTicks - 20) / (double) (EmergeTicks - 20), 0.0, 1.0);
        double Eased = Share * Share * (3.0 - 2.0 * Share);
        setPos(HomeX, RimY - Depth - Submerged * (1.0 - Eased), HomeZ);
        if (StateTicks % 8 == 0) {
            rimDust(World, 8);
        }
        if (StateTicks == 30 || StateTicks == 55) {
            Player Target = pickTarget(World);
            if (Target != null) {
                Scp1730PitWhip.writhe(World, this, rimPoint(Target.position(), -0.4), 50 + random.nextInt(20));
            }
            World.playSound(null, HomeX, RimY, HomeZ, Scp1730PitRegistry.WhipRise.get(), SoundSource.HOSTILE, 2.0f, 0.7f);
        }
        if (StateTicks == EmergeTicks - 20) {
            cue(World, Scp1730PitRegistry.Roar.get(), Scp1730PitRegistry.RoarLow.get(), 3.0f, 0.8f);
            shake(World, 0.4f, 24);
        }
        if (StateTicks >= EmergeTicks) {
            setState(StateActive);
            Cooldown = 30;
            for (ServerPlayer User : World.players()) {
                if (User.distanceToSqr(this) < 96.0 * 96.0) {
                    Bar.addPlayer(User);
                }
            }
        }
    }

    private void tickActive(ServerLevel World) {
        boolean Anchored0 = anchored();
        if (Anchored0) {
            setWeakness(Weakness + WeaknessGain);
        } else if (Weakness > 0.0f) {
            setWeakness(Weakness - WeaknessDecay);
        }
        if (Weakness >= WeaknessMax) {
            daze(World);
            return;
        }
        Player Target = pickTarget(World);
        if (Target == null && (StateTicks % 20 != 0 || !anyoneInRoom(World))) {
            Lonely += StateTicks % 20 == 0 ? 20 : 0;
            if (Lonely > LonelyTicks && action() == ActionNone) {
                setState(StateSinking);
                World.playSound(null, getX(), getY() + MouthUp, getZ(), Scp1730PitRegistry.Sink.get(), SoundSource.HOSTILE, 2.0f, 0.7f);
            }
        } else if (Target != null || StateTicks % 20 == 0) {
            Lonely = 0;
        }
        if (PullWait > 0) {
            PullWait--;
        }
        tickAction(World, Target, Anchored0);
        glide(World, Target, Anchored0);
    }

    private void glide(ServerLevel World, @Nullable Player Target, boolean Anchored0) {
        double WantX = HomeX;
        double WantZ = HomeZ;
        byte Doing = action();
        if (Target != null) {
            double Lean = Doing == ActionBite ? 1.0 : Doing == ActionPull ? 0.0 : 0.45;
            Vec3 Edge = clampInside(Target.position());
            WantX = Mth.lerp(Lean, HomeX, Edge.x);
            WantZ = Mth.lerp(Lean, HomeZ, Edge.z);
            if (Doing != ActionTongue || ActionTicks < TongueTelegraph - 6) {
                faceToward(Target.position(), Anchored0 ? 3.0f : 7.0f);
            }
        }
        double Speed = Anchored0 ? 0.04 : 0.12;
        double Dx = Mth.clamp(WantX - getX(), -Speed, Speed);
        double Dz = Mth.clamp(WantZ - getZ(), -Speed, Speed);
        double WantY = RimY - Depth - (Doing == ActionPull ? 1.2 : 0.0) + (Anchored0 ? Math.sin(tickCount * 0.6) * 0.15 : 0.0);
        double Dy = Mth.clamp(WantY - getY(), -0.12, 0.12);
        setPos(getX() + Dx, getY() + Dy, getZ() + Dz);
    }

    public Vec3 clampInside(Vec3 Point) {
        return new Vec3(Mth.clamp(Point.x, HomeX - HalfX + Inset, HomeX + HalfX - Inset), Point.y, Mth.clamp(Point.z, HomeZ - HalfZ + Inset, HomeZ + HalfZ - Inset));
    }

    public Vec3 rimPoint(Vec3 Toward, double Outside) {
        double Dx = Toward.x - HomeX;
        double Dz = Toward.z - HomeZ;
        double Sx = Math.abs(Dx) < 1.0e-4 ? 1.0e9 : HalfX / Math.abs(Dx);
        double Sz = Math.abs(Dz) < 1.0e-4 ? 1.0e9 : HalfZ / Math.abs(Dz);
        double Share = Math.min(Sx, Sz);
        if (Share > 1.0e8) {
            return new Vec3(HomeX + HalfX + Outside, RimY, HomeZ);
        }
        Vec3 Edge = new Vec3(HomeX + Dx * Share, RimY, HomeZ + Dz * Share);
        Vec3 Out = new Vec3(Dx, 0.0, Dz).normalize();
        return Edge.add(Out.scale(Outside));
    }

    private void faceToward(Vec3 Point, float Step) {
        double Dx = Point.x - getX();
        double Dz = Point.z - getZ();
        float Want = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.approachDegrees(getYRot(), Want, Step);
        setYRot(Turned);
        yBodyRot = Turned;
        yHeadRot = Turned;
    }

    private void tickAction(ServerLevel World, @Nullable Player Target, boolean Anchored0) {
        byte Doing = action();
        float Slow = Anchored0 ? 1.5f : 1.0f;
        if (Doing == ActionNone) {
            if (Cooldown > 0) {
                Cooldown--;
                return;
            }
            if (Target == null) {
                return;
            }
            begin(World, choose(World, Target));
            return;
        }
        ActionTicks++;
        switch (Doing) {
            case ActionTongue -> tickTongue(World, Target, Slow);
            case ActionBite -> {
                int Hit = (int) (BiteTelegraph * Slow);
                if (ActionTicks == Hit) {
                    bite(World);
                }
                if (ActionTicks >= Hit + BiteRecover) {
                    finishAction();
                }
            }
            case ActionSpit -> {
                int Hit = (int) (SpitTelegraph * Slow);
                if (ActionTicks == Hit) {
                    spit(World, Target);
                }
                if (ActionTicks >= Hit + SpitRecover) {
                    finishAction();
                }
            }
            case ActionSlam -> {
                if (ActionTicks == 6 && Target != null) {
                    slam(World, Target);
                }
                if (ActionTicks >= SlamLength * Slow) {
                    finishAction();
                }
            }
            case ActionPull -> tickPull(World, Slow);
            default -> finishAction();
        }
    }

    private byte choose(ServerLevel World, Player Target) {
        double Gap = Target.position().distanceTo(mouth());
        double Edge = Target.position().distanceTo(rimPoint(Target.position(), 0.0));
        float Roll = random.nextFloat();
        if (PullWait <= 0 && Roll < 0.18f && Edge < PullRadius) {
            return ActionPull;
        }
        Roll = random.nextFloat();
        if (Gap < BiteRadius + 2.5 && Edge < 3.5) {
            return Roll < 0.5f ? ActionBite : Roll < 0.75f ? ActionTongue : ActionSlam;
        }
        if (Gap < TongueReach) {
            return Roll < 0.38f ? ActionTongue : Roll < 0.72f ? ActionSlam : ActionSpit;
        }
        return Roll < 0.6f ? ActionSlam : ActionSpit;
    }

    private void begin(ServerLevel World, byte Chosen) {
        if (Chosen == ActionSpit && leechCount(World) >= LeechCap) {
            Chosen = ActionSlam;
        }
        entityData.set(DataAction, Chosen);
        ActionTicks = 0;
        Vec3 Mouth = mouth();
        switch (Chosen) {
            case ActionTongue -> {
                triggerAnim("action", "tongue");
                World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730PitRegistry.Hiss.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
            }
            case ActionBite -> {
                triggerAnim("action", "bite");
                cue(World, Scp1730PitRegistry.Roar.get(), Scp1730PitRegistry.Hiss.get(), 2.2f, 1.1f);
            }
            case ActionSpit -> {
                triggerAnim("action", "spit");
                World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730PitRegistry.Spit.get(), SoundSource.HOSTILE, 2.0f, 0.6f);
            }
            case ActionSlam -> {
                triggerAnim("action", "slam");
                World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730PitRegistry.RoarLow.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            }
            case ActionPull -> {
                triggerAnim("action", "pull");
                PullWait = PullRest;
                World.playSound(null, HomeX, RimY, HomeZ, Scp1730PitRegistry.Inhale.get(), SoundSource.HOSTILE, 3.0f, 0.7f);
                shake(World, 0.25f, PullTelegraph + PullTicks);
            }
            default -> {
            }
        }
    }

    private void finishAction() {
        if (action() == ActionNone && Grabbed < 0 && TongueId < 0) {
            return;
        }
        releaseGrab(false);
        dropTongue();
        entityData.set(DataAction, ActionNone);
        ActionTicks = 0;
        Cooldown = (anchored() ? 70 : 34) + random.nextInt(24);
    }

    private void tickTongue(ServerLevel World, @Nullable Player Target, float Slow) {
        int Launch = (int) (TongueTelegraph * Slow);
        Vec3 Mouth = mouth();
        if (ActionTicks < Launch) {
            if (Target != null && ActionTicks < Launch - 6) {
                TongueAim = Target.position().add(0.0, 1.0, 0.0);
            }
            if (ActionTicks % 4 == 0) {
                World.sendParticles(new DustParticleOptions(new Vector3f(0.75f, 0.08f, 0.1f), 1.4f), Mouth.x, Mouth.y, Mouth.z, 4, 0.4, 0.4, 0.4, 0.0);
            }
            return;
        }
        if (ActionTicks == Launch) {
            TongueOut = 0.0;
            Grabbed = -1;
            Struggle = 0;
            LastGap = -1.0;
            Scp1730PitTongue Made = Scp1730PitTongue.spawn(World, this);
            TongueId = Made == null ? -1 : Made.getId();
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730PitRegistry.Lash.get(), SoundSource.HOSTILE, 2.2f, 0.9f);
        }
        Vec3 Way = TongueAim.subtract(Mouth);
        double Reach = Math.min(TongueReach, Way.length() + 2.0);
        Vec3 Direction = Way.lengthSqr() < 1.0e-6 ? facing() : Way.normalize();
        Entity Held = Grabbed >= 0 ? World.getEntity(Grabbed) : null;
        if (Held instanceof Player Victim && Victim.isAlive()) {
            holdTongue(World, Victim, Mouth);
        } else {
            if (Grabbed >= 0) {
                releaseGrab(false);
            }
            boolean Out = ActionTicks - Launch < Reach / TongueSpeed + 4;
            TongueOut = Out ? Math.min(Reach, TongueOut + TongueSpeed) : Math.max(0.0, TongueOut - TongueSpeed * 1.5);
            Vec3 Tip = Mouth.add(Direction.scale(TongueOut));
            if (Out) {
                for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(Mouth, Tip).inflate(TongueGrab + 1.0), this::valid)) {
                    Vec3 Body = Each.position().add(0.0, Each.getBbHeight() * 0.5, 0.0);
                    if (segmentGap(Body, Mouth, Tip) < TongueGrab + Each.getBbWidth() * 0.5) {
                        Grabbed = Each.getId();
                        World.playSound(null, Each.getX(), Each.getY(), Each.getZ(), Scp1730PitRegistry.Lash.get(), SoundSource.HOSTILE, 1.6f, 1.3f);
                        break;
                    }
                }
            } else if (TongueOut <= 0.0) {
                finishAction();
                return;
            }
            updateTongue(Mouth, Tip);
        }
        if (ActionTicks - Launch > TongueHold) {
            finishAction();
        }
    }

    private void holdTongue(ServerLevel World, Player Victim, Vec3 Mouth) {
        Vec3 Drop = rimPoint(Victim.position(), 1.2);
        Vec3 Toward = new Vec3(Drop.x - Victim.getX(), 0.0, Drop.z - Victim.getZ());
        double Gap = Toward.length();
        if (anchored()) {
            releaseGrab(true);
            return;
        }
        if (LastGap >= 0.0 && Gap > LastGap - 0.05) {
            Struggle++;
        } else {
            Struggle = Math.max(0, Struggle - 1);
        }
        LastGap = Gap;
        if (Struggle >= StruggleFree) {
            releaseGrab(true);
            return;
        }
        if (Gap < 1.4) {
            Victim.hurt(source(World), TongueBite);
            World.playSound(null, Victim.getX(), Victim.getY(), Victim.getZ(), Scp1730PitRegistry.Bite.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            Vec3 Away = new Vec3(Victim.getX() - HomeX, 0.0, Victim.getZ() - HomeZ).normalize();
            Victim.setDeltaMovement(Away.x * 1.1, 0.45, Away.z * 1.1);
            Victim.hurtMarked = true;
            releaseGrab(false);
            return;
        }
        Vec3 Pull = Toward.normalize().scale(TonguePull);
        Victim.setDeltaMovement(Victim.getDeltaMovement().multiply(0.5, 1.0, 0.5).add(Pull.x, 0.02, Pull.z));
        Victim.hurtMarked = true;
        Victim.fallDistance = 0.0f;
        TongueOut = Victim.position().add(0.0, 1.0, 0.0).distanceTo(Mouth);
        updateTongue(Mouth, Victim.position().add(0.0, 1.0, 0.0));
    }

    private void updateTongue(Vec3 Mouth, Vec3 Tip) {
        if (TongueId >= 0 && level().getEntity(TongueId) instanceof Scp1730PitTongue Tongue) {
            Tongue.follow(Mouth, Tip);
        }
    }

    private void dropTongue() {
        if (TongueId >= 0 && level().getEntity(TongueId) instanceof Scp1730PitTongue Tongue) {
            Tongue.discard();
        }
        TongueId = -1;
        TongueOut = 0.0;
    }

    public int grabbedId() {
        return Grabbed;
    }

    public void releaseGrab(boolean Recoil) {
        if (Grabbed < 0) {
            return;
        }
        Grabbed = -1;
        Struggle = 0;
        LastGap = -1.0;
        if (Recoil && level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY() + MouthUp, getZ(), Scp1730PitRegistry.Hurt.get(), SoundSource.HOSTILE, 2.0f, 1.2f);
            ActionTicks = Math.max(ActionTicks, (int) (TongueTelegraph * 1.5) + TongueHold - 10);
        }
    }

    public static double segmentGap(Vec3 Point, Vec3 Start, Vec3 End) {
        Vec3 Line = End.subtract(Start);
        double Length = Line.lengthSqr();
        double Share = Length < 1.0e-9 ? 0.0 : Mth.clamp(Point.subtract(Start).dot(Line) / Length, 0.0, 1.0);
        return Point.distanceTo(Start.add(Line.scale(Share)));
    }

    public DamageSource source(ServerLevel World) {
        return new DamageSource(World.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730PitRegistry.PitDamage), this);
    }

    private void bite(ServerLevel World) {
        Vec3 Mouth = mouth();
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730PitRegistry.Bite.get(), SoundSource.HOSTILE, 2.6f, 0.8f);
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(Mouth, Mouth).inflate(BiteRadius + 2.0), this::valid)) {
            Vec3 Body = Each.position().add(0.0, Each.getBbHeight() * 0.5, 0.0);
            if (Body.distanceTo(Mouth) - Each.getBbWidth() * 0.5 > BiteRadius) {
                continue;
            }
            if (Each.hurt(source(World), BiteDamage)) {
                Vec3 Away = new Vec3(Each.getX() - HomeX, 0.0, Each.getZ() - HomeZ).normalize();
                Each.setDeltaMovement(Away.x * 0.9, 0.35, Away.z * 0.9);
                Each.hurtMarked = true;
            }
        }
        World.sendParticles(ParticleTypes.CRIT, Mouth.x, Mouth.y, Mouth.z, 12, 0.8, 0.6, 0.8, 0.2);
    }

    public int leechCount(ServerLevel World) {
        return World.getEntitiesOfClass(Scp1730Leech.class, new AABB(HomeX, RimY, HomeZ, HomeX, RimY, HomeZ).inflate(32.0), Scp1730Leech::isAlive).size();
    }

    private void spit(ServerLevel World, @Nullable Player Target) {
        int Room = LeechCap - leechCount(World);
        int Count = Math.min(LeechesPerSpit, Room);
        Vec3 Mouth = mouth();
        World.sendParticles(ParticleTypes.SQUID_INK, Mouth.x, Mouth.y, Mouth.z, 20, 0.6, 0.6, 0.6, 0.12);
        for (int Index = 0; Index < Count; Index++) {
            Scp1730Leech Leech = Scp1730Registry.LeechType.get().create(World);
            if (Leech == null) {
                continue;
            }
            Vec3 Aim = Target != null ? Target.position() : rimPoint(Mouth.add(facing().scale(4.0)), 2.0);
            Vec3 Land = Aim.add((random.nextDouble() - 0.5) * 4.0, 0.0, (random.nextDouble() - 0.5) * 4.0);
            Vec3 Safe = rimPoint(Land, Math.max(1.5, Land.distanceTo(new Vec3(HomeX, Land.y, HomeZ)) - Math.max(HalfX, HalfZ)));
            Vec3 Way = Safe.subtract(Mouth);
            double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
            double Time = Math.max(10.0, Flat / 0.6);
            Leech.moveTo(Mouth.x, Mouth.y, Mouth.z, random.nextFloat() * 360.0f, 0.0f);
            Leech.setDeltaMovement(Way.x / Time, (Way.y + 0.04 * Time * Time * 0.5) / Time, Way.z / Time);
            Leech.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Mouth)), MobSpawnType.MOB_SUMMONED, null);
            World.addFreshEntity(Leech);
        }
    }

    private void slam(ServerLevel World, Player Target) {
        Scp1730PitWhip.strike(World, this, Target.position(), rimPoint(Target.position(), -0.4));
        Vec3 Side = rimPoint(Target.position().add(facing().cross(new Vec3(0.0, 1.0, 0.0)).scale(5.0 * (random.nextBoolean() ? 1.0 : -1.0))), -0.4);
        Scp1730PitWhip.strike(World, this, Target.position().add((random.nextDouble() - 0.5) * 6.0, 0.0, (random.nextDouble() - 0.5) * 6.0), Side);
    }

    private void tickPull(ServerLevel World, float Slow) {
        int Start = (int) (PullTelegraph * Slow);
        if (ActionTicks >= Start && ActionTicks < Start + PullTicks) {
            for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(HomeX, RimY, HomeZ, HomeX, RimY, HomeZ).inflate(HalfX + PullRadius, 6.0, HalfZ + PullRadius),
                    this::valid)) {
                Vec3 Edge = rimPoint(Each.position(), 0.0);
                if (Each.position().distanceTo(new Vec3(Edge.x, Each.getY(), Edge.z)) > PullRadius || Each.getY() < RimY - 0.5) {
                    continue;
                }
                Vec3 Toward = new Vec3(HomeX - Each.getX(), 0.0, HomeZ - Each.getZ()).normalize().scale(PullStrength);
                Each.setDeltaMovement(Each.getDeltaMovement().add(Toward));
                Each.hurtMarked = true;
            }
            if (ActionTicks % 5 == 0) {
                rimDust(World, 10);
            }
        }
        if (ActionTicks >= Start + PullTicks + 10) {
            finishAction();
        }
    }

    public void daze(ServerLevel World) {
        setWeakness(WeaknessMax);
        setState(StateDazed);
        cue(World, Scp1730PitRegistry.Daze.get(), Scp1730PitRegistry.RoarLow.get(), 3.0f, 0.6f);
        shake(World, 0.45f, 20);
    }

    private void tickDazed(ServerLevel World) {
        Player Near = World.getNearestPlayer(HomeX, RimY, HomeZ, ArenaReach, Each -> Each instanceof Player User && valid(User));
        if (StateTicks < 24 && Near != null) {
            Vec3 Edge = clampInside(Near.position());
            Vec3 Rim = rimPoint(Near.position(), 0.0);
            Vec3 Want = new Vec3(Mth.lerp(0.7, Edge.x, Rim.x), 0.0, Mth.lerp(0.7, Edge.z, Rim.z));
            setPos(getX() + Mth.clamp(Want.x - getX(), -0.3, 0.3), Mth.lerp(0.15, getY(), RimY - Depth + 0.2), getZ() + Mth.clamp(Want.z - getZ(), -0.3, 0.3));
            faceToward(Near.position(), 12.0f);
        }
        if (StateTicks % 20 == 0) {
            Vec3 Mouth = mouth();
            World.sendParticles(ParticleTypes.SQUID_INK, Mouth.x, Mouth.y, Mouth.z, 4, 0.6, 0.3, 0.6, 0.01);
        }
        if (StateTicks >= DazeTicks) {
            setState(StateRecovering);
            cue(World, Scp1730PitRegistry.Roar.get(), Scp1730PitRegistry.Thrash.get(), 2.6f, 0.75f);
        }
    }

    private void tickRecovering(ServerLevel World) {
        Vec3 Back = clampInside(new Vec3(Mth.lerp(0.5, getX(), HomeX), 0.0, Mth.lerp(0.5, getZ(), HomeZ)));
        setPos(getX() + Mth.clamp(Back.x - getX(), -0.1, 0.1), Mth.lerp(0.1, getY(), RimY - Depth), getZ() + Mth.clamp(Back.z - getZ(), -0.1, 0.1));
        if (StateTicks >= RecoverTicks) {
            setWeakness(RecoverWeakness);
            heal(getMaxHealth() * RecoverHeal);
            setState(StateActive);
            Cooldown = 20;
        }
    }

    private void tickSinking(ServerLevel World) {
        double Share = Math.min(1.0, StateTicks / (double) SinkTicks);
        setPos(getX() + (HomeX - getX()) * 0.1, RimY - Depth - Submerged * Share, getZ() + (HomeZ - getZ()) * 0.1);
        if (StateTicks >= SinkTicks) {
            Bar.removeAllPlayers();
            setWeakness(0.0f);
            heal(getMaxHealth() * 0.5f);
            setState(StateDormant);
        } else if (StateTicks > 10 && anyoneInRoom(World) && pickTarget(World) != null) {
            setState(StateActive);
        }
    }

    private void rimDust(ServerLevel World, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Edge = rimPoint(new Vec3(HomeX + random.nextGaussian() * 5.0, RimY, HomeZ + random.nextGaussian() * 5.0), 0.2);
            BlockState Ground = World.getBlockState(BlockPos.containing(Edge.x, RimY - 0.5, Edge.z));
            if (!Ground.isAir()) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Edge.x, RimY + 0.1, Edge.z, 3, 0.3, 0.1, 0.3, 0.05);
            }
        }
    }

    public void cue(ServerLevel World, SoundEvent First, SoundEvent Second, float Volume, float Pitch) {
        Vec3 Mouth = emerged() ? mouth() : new Vec3(HomeX, RimY, HomeZ);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, First, SoundSource.HOSTILE, Volume, Pitch);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Second, SoundSource.HOSTILE, Volume * 0.8f, Pitch * 0.9f);
    }

    public void shake(ServerLevel World, float Strength, int Ticks) {
        for (ServerPlayer User : World.players()) {
            double Gap = User.distanceToSqr(HomeX, RimY, HomeZ);
            if (Gap < 28.0 * 28.0) {
                PacketDistributor.sendToPlayer(User, new Scp1730ShakePayload((float) (Strength * (1.0 - Math.sqrt(Gap) / 28.0)), Ticks));
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("HasHome", HasHome);
        Tag.putDouble("HomeX", HomeX);
        Tag.putDouble("HomeZ", HomeZ);
        Tag.putDouble("HalfX", HalfX);
        Tag.putDouble("HalfZ", HalfZ);
        Tag.putInt("RimY", RimY);
        Tag.putFloat("Weakness", Weakness);
        Tag.putByte("PitState", state() == StateEmerging || state() == StateSinking ? StateDormant : state() == StateRecovering ? StateActive : state());
        Tag.putInt("StateTicks", StateTicks);
        Tag.putInt("PullWait", PullWait);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        HasHome = Tag.getBoolean("HasHome");
        HomeX = Tag.getDouble("HomeX");
        HomeZ = Tag.getDouble("HomeZ");
        HalfX = Tag.contains("HalfX") ? Tag.getDouble("HalfX") : VirtualHalf;
        HalfZ = Tag.contains("HalfZ") ? Tag.getDouble("HalfZ") : VirtualHalf;
        RimY = Tag.getInt("RimY");
        setWeakness(Tag.getFloat("Weakness"));
        entityData.set(DataState, Tag.getByte("PitState"));
        StateTicks = Tag.getInt("StateTicks");
        PullWait = Tag.getInt("PullWait");
        if (hasCustomName()) {
            Bar.setName(getDisplayName());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 6, Animated -> {
            if (isDeadOrDying()) {
                return Animated.setAndContinue(DeathHold);
            }
            return switch (state()) {
                case StateDormant -> Animated.setAndContinue(DormantHold);
                case StateEmerging -> Animated.setAndContinue(EmergeOnce);
                case StateDazed -> Animated.setAndContinue(DazedLoop);
                case StateRecovering -> Animated.setAndContinue(RecoverOnce);
                case StateSinking -> Animated.setAndContinue(SinkOnce);
                default -> Animated.setAndContinue(anchored() ? ThrashLoop : IdleLoop);
            };
        }));
        Controllers.add(new AnimationController<>(this, "action", 4, Animated -> Animated.getController().isPlayingTriggeredAnimation() && !isDeadOrDying() && state() == StateActive
                ? PlayState.CONTINUE : PlayState.STOP).receiveTriggeredAnimations().triggerableAnim("tongue", TongueOnce).triggerableAnim("bite", BiteOnce)
                .triggerableAnim("spit", SpitOnce).triggerableAnim("slam", SlamOnce).triggerableAnim("pull", PullOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
