package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Scp860Watcher extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.95f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.85f;
    public static final double MaxHealth = 40.0;
    public static final double Armor = 2.0;
    public static final double AttackDamage = 5.0;
    public static final double FollowRange = 40.0;
    public static final double Speed = 0.3;
    public static final double StepHeight = 1.0;
    public static final double KnockbackResistance = 0.3;
    public static final double StalkModifier = 0.55;
    public static final double RoamModifier = 0.62;
    public static final double HuntModifier = 1.55;
    public static final double WalkDistance = 1.5;
    public static final double RunDistance = 2.6;
    public static final double[] WalkContacts = {0.0, 0.25, 0.5, 0.75};
    public static final double[] RunContacts = {0.0, 0.1, 0.5, 0.6};
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.24, 0.19, WalkDistance, RunDistance, 0.6, 1);
    public static final int AttackCooldown = 18;
    public static final int PounceCooldown = 60;
    public static final double PounceMin = 3.0;
    public static final double PounceMax = 6.0;
    public static final double PouncePower = 0.9;
    public static final double PounceLift = 0.42;
    public static final double WatchFar = 30.0;
    public static final double WatchNear = 18.0;
    public static final double RestlessFar = 18.0;
    public static final double RestlessNear = 12.0;
    public static final double CloseFar = 12.0;
    public static final double CloseNear = 8.0;
    public static final double KeepOffPath = 8.0;
    public static final int RepathTicks = 30;
    public static final double FollowReach = 14.0;
    public static final int MaxFollowers = 2;
    public static final int FollowAnger = 200;
    public static final int RetaliateTicks = 200;
    public static final double Territory = 12.0;
    public static final double KeepAway = 8.0;
    public static final double TooClose = 3.5;
    public static final int CrowdTicks = 20;
    public static final int CrowdAnger = 100;
    public static final double WildSight = 16.0;
    public static final double AngerReach = 32.0;
    public static final int AmbientInterval = 260;
    public static final float StepVolume = 0.06f;
    public static final float RoarPitch = 0.95f;
    public static final double CullSpread = 2.6;
    public static final double CullRise = 1.5;
    public static final int DeathTicks = 34;
    public static final byte Calm = 0;
    public static final byte Watching = 1;
    public static final byte Restless = 2;
    public static final byte Hunting = 3;
    public static final byte Wary = 4;
    private static final EntityDataAccessor<Byte> PostureData = SynchedEntityData.defineId(Scp860Watcher.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> StalkData = SynchedEntityData.defineId(Scp860Watcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ForestData = SynchedEntityData.defineId(Scp860Watcher.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WatchLoop = RawAnimation.begin().thenLoop("watch");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation StalkLoop = RawAnimation.begin().thenLoop("stalk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation DeathHold = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation PounceOnce = RawAnimation.begin().thenPlay("pounce");
    private static final RawAnimation GrowlOnce = RawAnimation.begin().thenPlay("growl");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private int Generation = -1;
    @Nullable
    private UUID Watched;
    @Nullable
    private UUID HuntTarget;
    @Nullable
    private UUID AngerTarget;
    private int AngerTicks;
    private int RetaliateLeft;
    @Nullable
    private UUID RetaliateTarget;
    @Nullable
    private BlockPos Home;
    private int AttackWait;
    private int PounceWait;
    private int Windup;
    private long RoaredAt = -1L;
    private int RepathWait;
    private int Crowd;
    private float RenderPartial;

    public Scp860Watcher(EntityType<? extends Scp860Watcher> Type, Level World) {
        super(Type, World);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ARMOR, Armor).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.STEP_HEIGHT, StepHeight);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PostureData, Calm);
        Builder.define(StalkData, false);
        Builder.define(ForestData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        if (Home == null) {
            Home = blockPosition();
        }
        if (Generation < 0 && Scp860Forest.strict(World.getLevel())) {
            bindToForest(Scp860State.get(World.getLevel().getServer()).generation());
        }
        return Result;
    }

    public boolean forestBound() {
        return entityData.get(ForestData);
    }

    public void bindToForest(int Value) {
        Generation = Value;
        entityData.set(ForestData, true);
    }

    public byte posture() {
        return entityData.get(PostureData);
    }

    public void setPosture(byte Value) {
        if (posture() != Value) {
            entityData.set(PostureData, Value);
        }
    }

    public boolean isStalking() {
        return entityData.get(StalkData);
    }

    @Nullable
    public UUID watched() {
        return Watched;
    }

    @Nullable
    public UUID huntTarget() {
        return HuntTarget;
    }

    @Nullable
    public UUID angerTarget() {
        return AngerTicks > 0 ? AngerTarget : null;
    }

    public int angerTicks() {
        return AngerTicks;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public boolean isHunting(Player User) {
        return User.getUUID().equals(HuntTarget) || AngerTicks > 0 && User.getUUID().equals(AngerTarget);
    }

    public void watch(Player User) {
        Watched = User.getUUID();
    }

    public void hunt(Player User) {
        hunt(User, 0, false);
    }

    public void hunt(Player User, int Delay, boolean Leader) {
        HuntTarget = User.getUUID();
        Watched = User.getUUID();
        Windup = Math.max(Windup, Delay);
        setPosture(Hunting);
        if (Leader) {
            roar();
        }
    }

    public void roar() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Scp860Sounds.hostile(World, position(), Scp860Registry.RoarLayers, 1.0f, RoarPitch + random.nextFloat() * 0.08f);
        RoaredAt = World.getGameTime();
        triggerAnim("action", "growl");
    }

    public long roaredAt() {
        return RoaredAt;
    }

    public int windup() {
        return Windup;
    }

    public void calm() {
        HuntTarget = null;
        setTarget(null);
        RepathWait = 0;
        Windup = 0;
        setPosture(Watching);
    }

    public void growl(boolean Near) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Scp860Sounds.hostile(World, position(), Scp860Registry.GrowlLayers, Near ? 1.0f : 0.75f, 0.9f + random.nextFloat() * 0.2f);
        if (Near) {
            triggerAnim("action", "growl");
        }
    }

    public void released(ServerLevel World) {
        entityData.set(ForestData, false);
        Generation = -1;
        HuntTarget = null;
        Watched = null;
        AngerTicks = 0;
        Home = blockPosition();
        setPosture(Wary);
        setPersistenceRequired();
    }

    public void followedOut(Player User) {
        entityData.set(ForestData, false);
        Generation = -1;
        HuntTarget = null;
        Watched = null;
        Home = blockPosition();
        AngerTarget = User.getUUID();
        AngerTicks = FollowAnger;
        setPosture(Hunting);
        setPersistenceRequired();
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean isStriding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public boolean isRunning() {
        return Stride.stride() == StrideGait.Run;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public static int contactsCrossed(double From, double To, double[] Contacts) {
        int Crossed = 0;
        for (double Contact : Contacts) {
            if (Math.floor(To - Contact) > Math.floor(From - Contact)) {
                Crossed++;
            }
        }
        return Crossed;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive(), Gait);
            if (Moving && isAlive() && !isSilent()) {
                int Steps = contactsCrossed(Stride.from(), Stride.to(), isRunning() ? RunContacts : WalkContacts);
                for (int Index = 0; Index < Steps; Index++) {
                    float Volume = StepVolume * (isRunning() ? 1.6f : isStalking() ? 0.5f : 1.0f);
                    level().playLocalSound(getX(), getY(), getZ(), Scp860Registry.Step.get(), SoundSource.HOSTILE, Volume, 0.9f + random.nextFloat() * 0.2f, false);
                    level().playLocalSound(getX(), getY(), getZ(), Scp860Registry.StepPad.get(), SoundSource.HOSTILE, Volume * 0.7f, 0.9f + random.nextFloat() * 0.2f, false);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (AttackWait > 0) {
            AttackWait--;
        }
        if (PounceWait > 0) {
            PounceWait--;
        }
        if (RepathWait > 0) {
            RepathWait--;
        }
        if (forestBound()) {
            forestStep(World);
        } else {
            wildStep(World);
        }
    }

    @Nullable
    private ServerPlayer player(ServerLevel World, @Nullable UUID Id) {
        if (Id == null) {
            return null;
        }
        Player Found = World.getPlayerByUUID(Id);
        return Found instanceof ServerPlayer User && User.isAlive() && !User.isSpectator() ? User : null;
    }

    private void forestStep(ServerLevel World) {
        if (tickCount % 40 == 0) {
            if (Generation != Scp860State.get(World.getServer()).generation() || despawnable(World)) {
                discard();
                return;
            }
        }
        ServerPlayer Prey = player(World, HuntTarget);
        if (HuntTarget != null && (Prey == null || !Scp860Forest.present(Prey))) {
            calm();
            Prey = null;
        }
        if (Prey != null) {
            chase(World, Prey);
            return;
        }
        ServerPlayer Seen = player(World, Watched);
        if (Seen == null || !Scp860Forest.present(Seen)) {
            List<ServerPlayer> Near = Scp860Director.sortedNear(World, position(), 64.0);
            Seen = Near.isEmpty() ? null : Near.get(0);
            Watched = Seen == null ? null : Seen.getUUID();
        }
        if (Seen == null) {
            setPosture(Calm);
            return;
        }
        float Patience = Scp860Director.patience(Seen);
        double Near = Patience >= Scp860Director.NearAt ? CloseNear : Patience >= Scp860Director.GrowlAt ? RestlessNear : WatchNear;
        double Far = Patience >= Scp860Director.NearAt ? CloseFar : Patience >= Scp860Director.GrowlAt ? RestlessFar : WatchFar;
        setPosture(Patience >= Scp860Director.GrowlAt ? Restless : Watching);
        getLookControl().setLookAt(Seen, 30.0f, 30.0f);
        double Gap = distanceTo(Seen);
        boolean OnPath = Scp860Forest.distance(World.getServer(), getX(), getZ()) < KeepOffPath;
        boolean Settled = getNavigation().isDone();
        if (RepathWait <= 0 && (Gap < Near || Gap > Far || OnPath || Settled && random.nextInt(40) == 0)) {
            RepathWait = RepathTicks;
            Vec3 Spot = watchSpot(World, Seen, Near, Far);
            if (Spot != null) {
                boolean Hurry = Gap > Far + 8.0 || OnPath;
                entityData.set(StalkData, !Hurry);
                getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Hurry ? RoamModifier : StalkModifier);
            }
        }
    }

    @Nullable
    private Vec3 watchSpot(ServerLevel World, Player Seen, double Near, double Far) {
        double Base = Math.atan2(getZ() - Seen.getZ(), getX() - Seen.getX());
        for (int Try = 0; Try < 10; Try++) {
            double Angle = Base + (random.nextDouble() - 0.5) * (Try < 5 ? 1.6 : 4.0);
            double Reach = Near + random.nextDouble() * (Far - Near);
            Vec3 Spot = new Vec3(Seen.getX() + Math.cos(Angle) * Reach, Scp860Forest.Ground + 1.0, Seen.getZ() + Math.sin(Angle) * Reach);
            if (Scp860Forest.distance(World.getServer(), Spot.x, Spot.z) < KeepOffPath + 0.5 || !Scp860Forest.nearRegion(Spot.x, Spot.z)) {
                continue;
            }
            BlockPos Feet = Scp860Director.groundAt(World, Spot);
            if (Feet != null) {
                return Vec3.atBottomCenterOf(Feet);
            }
        }
        return null;
    }

    private boolean despawnable(ServerLevel World) {
        for (ServerPlayer User : Scp860Forest.participants(World)) {
            if (distanceTo(User) <= Scp860Director.DespawnReach) {
                return false;
            }
        }
        return !Scp860Director.seen(World, position());
    }

    private void chase(ServerLevel World, LivingEntity Prey) {
        setPosture(Hunting);
        entityData.set(StalkData, false);
        getLookControl().setLookAt(Prey, 40.0f, 40.0f);
        if (Windup > 0) {
            Windup--;
            getNavigation().stop();
            return;
        }
        setTarget(Prey);
        double Gap = distanceTo(Prey);
        if (PounceWait <= 0 && onGround() && Gap >= PounceMin && Gap <= PounceMax) {
            pounce(World, Prey);
        }
        if (tickCount % 4 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(Prey, HuntModifier);
        }
        if (AttackWait <= 0 && isWithinMeleeAttackRange(Prey)) {
            AttackWait = AttackCooldown;
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(Prey);
        }
    }

    public void pounce(ServerLevel World, LivingEntity Prey) {
        PounceWait = PounceCooldown;
        Vec3 Way = Prey.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Way.lengthSqr() > 1.0e-4) {
            Way = Way.normalize();
        }
        setDeltaMovement(Way.x * PouncePower, PounceLift, Way.z * PouncePower);
        hasImpulse = true;
        triggerAnim("action", "pounce");
        Scp860Sounds.hostile(World, position(), Scp860Registry.PounceLayers, 1.0f, 0.95f + random.nextFloat() * 0.1f);
    }

    private void wildStep(ServerLevel World) {
        if (Home == null) {
            Home = blockPosition();
        }
        if (AngerTicks > 0) {
            AngerTicks--;
            ServerPlayer Prey = player(World, AngerTarget);
            if (Prey != null && !Prey.isCreative() && distanceTo(Prey) <= AngerReach) {
                chase(World, Prey);
                return;
            }
            AngerTicks = 0;
        }
        if (RetaliateLeft > 0) {
            RetaliateLeft--;
            Entity Found = RetaliateTarget == null ? null : World.getEntity(RetaliateTarget);
            if (Found instanceof LivingEntity Prey && Prey.isAlive() && !(Prey instanceof Player User && (User.isCreative() || User.isSpectator()))
                    && distanceTo(Prey) <= AngerReach) {
                chase(World, Prey);
                return;
            }
            RetaliateLeft = 0;
        }
        setTarget(null);
        setPosture(Wary);
        Player Closest = World.getNearestPlayer(this, WildSight);
        if (Closest != null && (Closest.isCreative() || Closest.isSpectator())) {
            Closest = null;
        }
        if (Closest != null) {
            getLookControl().setLookAt(Closest, 30.0f, 30.0f);
            double Gap = distanceTo(Closest);
            if (Gap < TooClose) {
                Crowd++;
                if (Crowd >= CrowdTicks) {
                    Crowd = 0;
                    RetaliateTarget = Closest.getUUID();
                    RetaliateLeft = CrowdAnger;
                    growl(true);
                }
            } else {
                Crowd = Math.max(0, Crowd - 1);
            }
            if (Gap < KeepAway && RepathWait <= 0) {
                RepathWait = RepathTicks / 2;
                Vec3 Away = position().subtract(Closest.position()).multiply(1.0, 0.0, 1.0);
                Away = Away.lengthSqr() < 1.0e-4 ? new Vec3(1.0, 0.0, 0.0) : Away.normalize();
                Vec3 Goal = position().add(Away.scale(5.0));
                Vec3 Centre = Vec3.atBottomCenterOf(Home);
                if (Goal.distanceTo(Centre) > Territory) {
                    Goal = Centre.add(Goal.subtract(Centre).normalize().scale(Territory));
                }
                entityData.set(StalkData, false);
                getNavigation().moveTo(Goal.x, Goal.y, Goal.z, RoamModifier);
            }
            return;
        }
        Crowd = 0;
        if (distanceToSqr(Vec3.atBottomCenterOf(Home)) > (Territory + 4.0) * (Territory + 4.0) && RepathWait <= 0) {
            RepathWait = RepathTicks;
            entityData.set(StalkData, false);
            getNavigation().moveTo(Home.getX() + 0.5, Home.getY(), Home.getZ() + 0.5, RoamModifier);
        } else if (getNavigation().isDone() && random.nextInt(160) == 0) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = 2.0 + random.nextDouble() * 6.0;
            entityData.set(StalkData, true);
            getNavigation().moveTo(Home.getX() + 0.5 + Math.cos(Angle) * Reach, Home.getY(), Home.getZ() + 0.5 + Math.sin(Angle) * Reach, StalkModifier);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hit = super.hurt(Source, Amount);
        if (Hit && !level().isClientSide && Source.getEntity() instanceof LivingEntity Attacker && Attacker != this) {
            if (forestBound()) {
                if (Attacker instanceof Player User && !User.isCreative()) {
                    hunt(User);
                }
            } else {
                RetaliateTarget = Attacker.getUUID();
                RetaliateLeft = RetaliateTicks;
            }
            if (level() instanceof ServerLevel World) {
                World.playSound(null, getX(), getY(), getZ(), Scp860Registry.HurtCrack.get(), SoundSource.HOSTILE, 0.8f, 0.9f + random.nextFloat() * 0.2f);
            }
        }
        return Hit;
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (level() instanceof ServerLevel World) {
            triggerAnim("action", "attack");
            Scp860Sounds.hostile(World, Target.position(), Scp860Registry.AttackLayers, 0.9f, 0.9f + random.nextFloat() * 0.2f);
        }
        return Hit;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY(), getZ(), Scp860Registry.DeathCrack.get(), SoundSource.HOSTILE, 0.9f, 0.9f);
        }
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
        return getBoundingBox().inflate(CullSpread, 0.0, CullSpread).expandTowards(0.0, CullRise, 0.0);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp860Registry.Voice.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World && posture() == Hunting && Windup <= 0) {
            Scp860Sounds.hostile(World, position(), Scp860Registry.VoiceLayers, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp860Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp860Registry.Death.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Forest", forestBound());
        Tag.putInt("Generation", Generation);
        if (Watched != null) {
            Tag.putUUID("Watched", Watched);
        }
        if (HuntTarget != null) {
            Tag.putUUID("HuntTarget", HuntTarget);
        }
        if (AngerTarget != null) {
            Tag.putUUID("AngerTarget", AngerTarget);
        }
        Tag.putInt("AngerTicks", AngerTicks);
        Tag.putInt("Retaliate", RetaliateLeft);
        if (RetaliateTarget != null) {
            Tag.putUUID("RetaliateTarget", RetaliateTarget);
        }
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(ForestData, Tag.getBoolean("Forest"));
        Generation = Tag.contains("Generation") ? Tag.getInt("Generation") : -1;
        Watched = Tag.hasUUID("Watched") ? Tag.getUUID("Watched") : null;
        HuntTarget = Tag.hasUUID("HuntTarget") ? Tag.getUUID("HuntTarget") : null;
        AngerTarget = Tag.hasUUID("AngerTarget") ? Tag.getUUID("AngerTarget") : null;
        AngerTicks = Tag.getInt("AngerTicks");
        RetaliateLeft = Tag.getInt("Retaliate");
        RetaliateTarget = Tag.hasUUID("RetaliateTarget") ? Tag.getUUID("RetaliateTarget") : null;
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
    }

    public static List<Scp860Watcher> chasers(ServerPlayer User) {
        List<Scp860Watcher> Found = new ArrayList<>(User.serverLevel().getEntitiesOfClass(Scp860Watcher.class, User.getBoundingBox().inflate(FollowReach),
                Watcher -> Watcher.isAlive() && Watcher.forestBound() && User.getUUID().equals(Watcher.huntTarget()) && Watcher.distanceTo(User) <= FollowReach));
        Found.sort(Comparator.comparingDouble(Watcher -> Watcher.distanceToSqr(User)));
        return Found.size() > MaxFollowers ? new ArrayList<>(Found.subList(0, MaxFollowers)) : Found;
    }

    @Nullable
    public static Scp860Watcher followOut(Scp860Watcher Chaser, ServerPlayer User, Scp860Trip.Spot Back, int Index) {
        double Angle = Back.Yaw() * Mth.DEG_TO_RAD + (Index == 0 ? 2.4 : -2.4);
        Vec3 Wanted = Back.Pos().add(-Math.sin(Angle) * 2.5, 0.0, Math.cos(Angle) * 2.5);
        Vec3 Spot = GatewayTravel.safeSpot(Back.World(), Wanted, Scp860Registry.Watcher.get());
        float Yaw = (float) (Mth.atan2(User.getX() - Spot.x, Spot.z - User.getZ()) * Mth.RAD_TO_DEG);
        Entity Moved = Chaser.changeDimension(new DimensionTransition(Back.World(), Spot, Vec3.ZERO, Yaw, 0.0f, DimensionTransition.DO_NOTHING));
        if (Moved instanceof Scp860Watcher Watcher) {
            Watcher.followedOut(User);
            return Watcher;
        }
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp860GaitController(this, "body", 4, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathHold);
            }
            if (isStriding()) {
                if (isRunning()) {
                    return Event.setAndContinue(RunLoop);
                }
                return Event.setAndContinue(isStalking() && posture() != Hunting ? StalkLoop : WalkLoop);
            }
            byte Pose = posture();
            return Event.setAndContinue(Pose == Watching || Pose == Restless || Pose == Wary ? WatchLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("attack", AttackOnce)
                .triggerableAnim("pounce", PounceOnce).triggerableAnim("growl", GrowlOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
