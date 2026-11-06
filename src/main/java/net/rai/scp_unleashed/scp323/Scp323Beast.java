package net.rai.scp_unleashed.scp323;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.UUID;

public class Scp323Beast extends Monster implements GeoEntity, IEntityWithComplexSpawn, ControlRefusing {
    public static final float Width = 0.9f;
    public static final float Height = 2.9f;
    public static final float EyeHeight = 2.55f;
    public static final EntityDimensions CrawlSize = EntityDimensions.scalable(0.9f, 1.9f).withEyeHeight(1.45f);
    public static final double MaxHealth = 180.0;
    public static final double Armour = 10.0;
    public static final double Toughness = 4.0;
    public static final double ClawDamage = 14.0;
    public static final double MoveSpeed = 0.3;
    public static final double Steadiness = 0.85;
    public static final double HuntRange = 40.0;
    public static final double SenseRange = 4.0;
    public static final double StalkSpeed = 0.7;
    public static final double SprintSpeed = 1.35;
    public static final double CrawlSpeed = 0.85;
    public static final double SwipeReach = 3.2;
    public static final int SwipeTicks = 14;
    public static final int SwipeImpact = 7;
    public static final int SwipeCooldown = 8;
    public static final int MorphTicks = 40;
    public static final int RetractTicks = 40;
    public static final int HowlTicks = 36;
    public static final int RetargetInterval = 10;
    public static final int LostLimit = 120;
    public static final int RepathInterval = 8;
    public static final int CycleInterval = 10;
    public static final int OrphanGrace = 60;
    public static final int DeathTicks = 30;
    public static final int StandDelay = 12;
    public static final int CryInterval = 260;
    public static final byte ActionNone = 0;
    public static final byte ActionMorph = 1;
    public static final byte ActionRetract = 2;
    public static final byte ActionHowl = 3;

    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp323Beast.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp323Beast.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp323Beast.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation StalkLoop = RawAnimation.begin().thenLoop("stalk");
    private static final RawAnimation SprintLoop = RawAnimation.begin().thenLoop("sprint");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation MorphPlay = RawAnimation.begin().thenPlayAndHold("morph");
    private static final RawAnimation RetractPlay = RawAnimation.begin().thenPlayAndHold("retract");
    private static final RawAnimation HowlPlay = RawAnimation.begin().thenPlayAndHold("howl");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SwipeLeft = RawAnimation.begin().thenPlay("swipe_left");
    private static final RawAnimation SwipeRight = RawAnimation.begin().thenPlay("swipe_right");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private Scp323Gait Gait;
    private int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private UUID HostPlayer;
    @Nullable
    private UUID HostBody;
    @Nullable
    private CompoundTag Stored;
    private String HostName = "";
    private int Kills;
    private boolean First = true;
    private boolean Started;
    private boolean SeenNight;
    private boolean DawnPassed;
    private long NightPhase = Long.MIN_VALUE;
    private boolean Released;
    private boolean PendingRetract;
    private int OrphanTicks;
    private int StrikeTimer;
    private int ImpactIn;
    private boolean LeftSwipe;
    private int RetargetTimer;
    private int RepathTimer;
    private int LowTicks;
    private int CryTimer = CryInterval;
    private Vec3 LastSeen = Vec3.ZERO;
    private int LostTicks;

    public Scp323Beast(EntityType<? extends Scp323Beast> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed).add(Attributes.ATTACK_DAMAGE, ClawDamage).add(Attributes.FOLLOW_RANGE, HuntRange)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness).add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.SAFE_FALL_DISTANCE, 6.0);
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
        return new Scp323Navigation(this, World);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setCanPickUpLoot(false);
        return Group;
    }

    public void prepare(boolean FirstEpisode, int KillsSoFar, boolean AtNight, long Phase) {
        First = FirstEpisode;
        Kills = KillsSoFar;
        Started = true;
        SeenNight = AtNight;
        NightPhase = AtNight ? Phase : Long.MIN_VALUE;
        DawnPassed = false;
    }

    public void bindPlayer(ServerPlayer User, UUID Body) {
        HostPlayer = User.getUUID();
        HostBody = Body;
        HostName = User.getGameProfile().getName();
        Stored = null;
    }

    public void bindBody(UUID Body, CompoundTag Saved, String Name) {
        HostPlayer = null;
        HostBody = Body;
        Stored = Saved.copy();
        HostName = Name;
    }

    public void adoptBody(LivingEntity Body) {
        CompoundTag Saved = Scp323Hosts.save(Body);
        if (Saved == null) {
            return;
        }
        HostPlayer = null;
        HostBody = Body.getUUID();
        Stored = Saved;
        HostName = Body.getName().getString();
    }

    public void detachPlayer() {
        HostPlayer = null;
    }

    @Nullable
    public UUID hostPlayer() {
        return HostPlayer;
    }

    @Nullable
    public UUID hostBody() {
        return HostBody;
    }

    @Nullable
    public CompoundTag storedBody() {
        return Stored == null ? null : Stored.copy();
    }

    public String hostName() {
        return HostName;
    }

    public boolean awaitsBody() {
        return HostPlayer == null && Stored == null && HostBody != null;
    }

    public int kills() {
        return Kills;
    }

    public boolean firstEpisode() {
        return First;
    }

    public boolean release() {
        if (Released) {
            return false;
        }
        Released = true;
        return true;
    }

    public boolean isReleased() {
        return Released;
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
            case ActionMorph -> "morph";
            case ActionRetract -> "retract";
            case ActionHowl -> "howl";
            default -> null;
        };
    }

    public String stateKey() {
        return switch (action()) {
            case ActionMorph -> "emerging";
            case ActionRetract -> "receding";
            case ActionHowl -> "howling";
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

    public void beginMorph() {
        setAction(ActionMorph, MorphTicks);
    }

    public void beginRetract() {
        if (action() == ActionRetract || !(level() instanceof ServerLevel World)) {
            return;
        }
        setAction(ActionRetract, RetractTicks);
        setTarget(null);
        Scp323Hosts.play(World, position().add(0.0, 1.5, 0.0), Scp323Registry.RetractLayers, 1.0f, 1.0f);
    }

    private void beginHowl(ServerLevel World) {
        setAction(ActionHowl, HowlTicks);
        Scp323Hosts.play(World, getEyePosition(), Scp323Registry.HowlLayers, 2.2f, 1.0f);
    }

    public void recordKill(LivingEntity Victim) {
        if (!(level() instanceof ServerLevel World) || Released) {
            return;
        }
        Kills++;
        if (First && Kills >= Scp323Hosts.KillQuota) {
            PendingRetract = true;
        }
        if (action() == ActionNone) {
            beginHowl(World);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp323Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive() || Released) {
            return;
        }
        if (!Started) {
            Started = true;
            long Time = Scp323Night.time(World);
            SeenNight = Scp323Night.isNight(Time);
            NightPhase = SeenNight ? Scp323Night.nightId(Time) : Long.MIN_VALUE;
        }
        observeNight(World);
        if (StrikeTimer > 0) {
            StrikeTimer--;
        }
        if (ImpactIn > 0 && --ImpactIn == 0) {
            impact(World);
        }
        posture(World);
        if (!checkHost(World)) {
            return;
        }
        if (ActionTicks > 0) {
            tickAction(World);
            return;
        }
        if (PendingRetract || tickCount % CycleInterval == 0 && episodeOver(World)) {
            PendingRetract = false;
            beginRetract();
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
            prowl(World);
        } else {
            hunt(World, Target);
        }
        if (--CryTimer <= 0) {
            CryTimer = CryInterval + random.nextInt(CryInterval);
            Scp323Hosts.play(World, getEyePosition(), Scp323Registry.CryLayers, 1.6f, 1.0f);
        }
    }

    private boolean checkHost(ServerLevel World) {
        if (HostPlayer == null) {
            OrphanTicks = 0;
            return true;
        }
        ServerPlayer User = Scp323Hosts.player(World.getServer(), HostPlayer);
        if (User != null && User.isAlive() && getUUID().equals(Scp323Bond.of(User).Creature())) {
            OrphanTicks = 0;
            return true;
        }
        if (++OrphanTicks < OrphanGrace) {
            getNavigation().stop();
            return false;
        }
        Released = true;
        Scp323Hosts.retracted(World, position());
        discard();
        return false;
    }

    private void observeNight(ServerLevel World) {
        long Time = Scp323Night.time(World);
        if (Scp323Night.isNight(Time)) {
            long Phase = Scp323Night.nightId(Time);
            if (!SeenNight || NightPhase == Long.MIN_VALUE) {
                SeenNight = true;
                NightPhase = Phase;
            } else if (NightPhase != Phase) {
                DawnPassed = true;
            }
        } else if (SeenNight) {
            DawnPassed = true;
        }
    }

    public boolean episodeOver(ServerLevel World) {
        observeNight(World);
        if (First && Kills >= Scp323Hosts.KillQuota) {
            return true;
        }
        return DawnPassed;
    }

    private void tickAction(ServerLevel World) {
        byte Current = action();
        int Elapsed = (int) actionElapsed();
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
        if (Current == ActionMorph && Elapsed % 8 == 4) {
            Vec3 Middle = position().add(0.0, 1.2, 0.0);
            World.sendParticles(ParticleTypes.WHITE_ASH, Middle.x, Middle.y, Middle.z, 8, 0.4, 0.8, 0.4, 0.02);
            World.playSound(null, Middle.x, Middle.y, Middle.z, Scp323Registry.MorphClick.get(), SoundSource.HOSTILE, 0.8f, 0.8f + random.nextFloat() * 0.3f);
        }
        if (Current == ActionRetract && Elapsed % 8 == 4) {
            Vec3 Middle = position().add(0.0, 1.0, 0.0);
            World.sendParticles(ParticleTypes.WHITE_ASH, Middle.x, Middle.y, Middle.z, 8, 0.4, 0.6, 0.4, 0.02);
            World.playSound(null, Middle.x, Middle.y, Middle.z, Scp323Registry.RetractClick.get(), SoundSource.HOSTILE, 0.8f, 0.9f + random.nextFloat() * 0.3f);
        }
        LivingEntity Target = getTarget();
        if (Current == ActionHowl && Target != null) {
            getLookControl().setLookAt(Target, 20.0f, 20.0f);
        }
        if (--ActionTicks > 0) {
            return;
        }
        setAction(ActionNone, 0);
        if (Current == ActionRetract) {
            finishRetract(World);
        } else if (Current == ActionHowl && PendingRetract) {
            PendingRetract = false;
            beginRetract();
        }
    }

    public void finishRetract(ServerLevel World) {
        if (Released) {
            return;
        }
        Released = true;
        Vec3 At = position();
        float Yaw = getYRot();
        ServerPlayer User = Scp323Hosts.player(World.getServer(), HostPlayer);
        Scp323Hosts.retracted(World, At);
        if (User != null && getUUID().equals(Scp323Bond.of(User).Creature())) {
            Scp323Watch.stop(User);
            if (User.level() != World || User.distanceToSqr(At) > 0.01) {
                User.teleportTo(World, At.x, At.y, At.z, Yaw, 0.0f);
            }
            User.setYHeadRot(Yaw);
            Scp323Bond.set(User, Scp323Bond.of(User).settle(First));
        } else if (Stored != null) {
            CompoundTag Attachments = Stored.getCompound(Scp323Bond.NeoAttachmentsKey);
            Scp323Bond Bond = Scp323Bond.fromAttachments(Attachments);
            if (Bond.Stage() <= 0) {
                Bond = Scp323Bond.fresh();
            }
            LivingEntity Body = Scp323Hosts.restore(World, Stored, At, Yaw, Bond.settle(First));
            if (Body == null) {
                Scp323Hosts.dropSkull(World, At, Yaw, 0);
            }
        } else {
            Scp323Hosts.dropSkull(World, At, Yaw, 0);
        }
        discard();
    }

    public void vanish() {
        Released = true;
        discard();
    }

    private void retarget(ServerLevel World) {
        LivingEntity Current = getTarget();
        LivingEntity Best = Scp323Prey.closest(World, this, HuntRange, SenseRange);
        if (Current != null && Scp323Prey.prey(this, Current) && distanceTo(Current) <= HuntRange) {
            boolean Sees = hasLineOfSight(Current);
            if (Sees) {
                LastSeen = Current.position();
                LostTicks = 0;
            } else {
                LostTicks += RetargetInterval;
            }
            boolean Switch = Best != null && Best != Current && (!Sees || Scp323Prey.clearlyCloser(this, Current, Best));
            if (!Switch && LostTicks < LostLimit) {
                return;
            }
        }
        LostTicks = 0;
        if (Best != Current) {
            setTarget(Best);
            RepathTimer = 0;
            if (Best != null) {
                LastSeen = Best.position();
                Scp323Hosts.play(World, getEyePosition(), Scp323Registry.CryLayers, 1.4f, 1.1f);
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
        if (Distance <= SwipeReach && Sees && StrikeTimer <= 0) {
            swipe(World, Target);
        }
        if (--RepathTimer > 0 && !getNavigation().isDone()) {
            return;
        }
        RepathTimer = RepathInterval;
        double Speed = isCrawling() ? CrawlSpeed : Sees && Distance > 2.0 ? SprintSpeed : StalkSpeed;
        if (Sees) {
            if (Distance > 1.6 && !getNavigation().moveTo(Target, Speed)) {
                getNavigation().stop();
            }
        } else if (!getNavigation().moveTo(LastSeen.x, LastSeen.y, LastSeen.z, Speed)) {
            getNavigation().stop();
        }
    }

    private void swipe(ServerLevel World, LivingEntity Target) {
        StrikeTimer = SwipeCooldown + SwipeTicks;
        ImpactIn = SwipeImpact;
        LeftSwipe = !LeftSwipe;
        triggerAnim("attack", LeftSwipe ? "swipe_left" : "swipe_right");
        Scp323Hosts.play(World, getEyePosition(), Scp323Registry.SwipeLayers, 1.0f, 1.0f);
    }

    private void impact(ServerLevel World) {
        LivingEntity Target = getTarget();
        if (Target == null || !Target.isAlive() || distanceTo(Target) > SwipeReach + 0.6 || !hasLineOfSight(Target)) {
            return;
        }
        Target.hurt(damageSources().source(Scp323Registry.Claw, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Push.lengthSqr() > 1.0E-4) {
            Push = Push.normalize();
            Target.knockback(0.5, -Push.x, -Push.z);
        }
    }

    private void prowl(ServerLevel World) {
        if (getNavigation().isDone() && random.nextInt(50) == 0) {
            Vec3 Spot = position().add((random.nextDouble() - 0.5) * 16.0, 0.0, (random.nextDouble() - 0.5) * 16.0);
            getNavigation().moveTo(Spot.x, getY(), Spot.z, isCrawling() ? CrawlSpeed : StalkSpeed);
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
        if (Hurt && !level().isClientSide && isAlive() && Source.getEntity() instanceof LivingEntity Attacker && Scp323Prey.prey(this, Attacker)) {
            LivingEntity Current = getTarget();
            if (Current == null || Current != Attacker && distanceTo(Attacker) < distanceTo(Current)) {
                setTarget(Attacker);
                LastSeen = Attacker.position();
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Before = dead;
        super.die(Source);
        if (!Before && dead && !level().isClientSide) {
            Scp323Hosts.beastDied(this, Source);
        }
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
    public void push(Entity Other) {
        if (HostPlayer != null && HostPlayer.equals(Other.getUUID())) {
            return;
        }
        super.push(Other);
    }

    @Override
    protected void doPush(Entity Other) {
        if (HostPlayer != null && HostPlayer.equals(Other.getUUID())) {
            return;
        }
        super.doPush(Other);
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
        return Component.translatable(Scp323Possession.Prefix + "no_link");
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 180;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp323Registry.Idle.get();
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        playSound(Scp323Registry.IdleCreak.get(), 0.7f, getVoicePitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp323Registry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        super.playHurtSound(Source);
        playSound(Scp323Registry.HurtCreak.get(), 0.8f, getVoicePitch());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp323Registry.Death.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Nullable
    public Scp323Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp323Gait();
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp323Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp323Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp323Gait Current = gait();
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
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (HostPlayer != null) {
            Tag.putUUID("HostPlayer", HostPlayer);
        }
        if (HostBody != null) {
            Tag.putUUID("HostBody", HostBody);
        }
        if (Stored != null) {
            Tag.put("HostData", Stored.copy());
        }
        Tag.putString("HostName", HostName);
        Tag.putInt("Kills", Kills);
        Tag.putBoolean("FirstEpisode", First);
        Tag.putBoolean("Started", Started);
        Tag.putBoolean("SeenNight", SeenNight);
        Tag.putBoolean("DawnPassed", DawnPassed);
        if (NightPhase != Long.MIN_VALUE) {
            Tag.putLong("NightPhase", NightPhase);
        }
        Tag.putBoolean("PendingRetract", PendingRetract || action() == ActionRetract);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        HostPlayer = Tag.hasUUID("HostPlayer") ? Tag.getUUID("HostPlayer") : null;
        HostBody = Tag.hasUUID("HostBody") ? Tag.getUUID("HostBody") : null;
        Stored = Tag.contains("HostData") ? Tag.getCompound("HostData").copy() : null;
        HostName = Tag.getString("HostName");
        Kills = Tag.getInt("Kills");
        First = !Tag.contains("FirstEpisode") || Tag.getBoolean("FirstEpisode");
        Started = Tag.contains("Started");
        SeenNight = Tag.getBoolean("SeenNight") || Tag.getBoolean("StartedAtNight");
        DawnPassed = Tag.getBoolean("DawnPassed");
        NightPhase = Tag.contains("NightPhase") ? Tag.getLong("NightPhase") : Long.MIN_VALUE;
        PendingRetract = Tag.getBoolean("PendingRetract");
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp323GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp323GaitController.blendFor(Current));
            if (deathTime > 0 || isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            if (Current == ActionMorph) {
                return Event.setAndContinue(MorphPlay);
            }
            if (Current == ActionRetract) {
                return Event.setAndContinue(RetractPlay);
            }
            if (Current == ActionHowl) {
                return Event.setAndContinue(HowlPlay);
            }
            if (isCrawling()) {
                return Event.setAndContinue(CrawlLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? SprintLoop : StalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "attack", 1, Event -> PlayState.STOP).triggerableAnim("swipe_left", SwipeLeft).triggerableAnim("swipe_right", SwipeRight));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
