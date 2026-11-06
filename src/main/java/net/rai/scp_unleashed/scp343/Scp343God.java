package net.rai.scp_unleashed.scp343;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.furnishing.FurnishingSeat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp343God extends DClassLike implements ControlRefusing {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.62f;
    public static final double MaxHealth = 20.0;
    public static final double MoveSpeed = 0.22;
    public static final double StrollSpeed = 0.6;
    public static final double HomeSpeed = 0.65;
    public static final double LeaveSpeed = 0.7;
    public static final int StrollInterval = 60;
    public static final int HomeRadius = 32;
    public static final int HomeDrift = 80;
    public static final int ThroneHome = 10;
    public static final float LookRange = 10.0f;
    public static final float LookChance = 0.06f;
    public static final float Glance = 0.5f;
    public static final int MaxHeadTurn = 25;
    public static final int SeatedHeadTurn = 15;
    public static final byte PhaseNone = 0;
    public static final byte PhaseEmerging = 1;
    public static final byte PhaseDeparting = 2;
    public static final int PhaseTicks = 30;
    public static final int ContainCheckEvery = 100;
    public static final int ContainGrace = 900;
    public static final int LeaveTimeout = 300;
    public static final int AttendTicks = 70;
    public static final long LineCooldown = 40L;
    public static final int SeekTicks = 1200;
    public static final double GreetDistance = 3.0;
    public static final int ThroneCheckEvery = 40;
    public static final long FirstSitDelay = 60L;
    public static final long SitRollEvery = 200L;
    public static final float SitChance = 0.25f;
    public static final long SitShortest = 600L;
    public static final long SitLongest = 1800L;
    public static final long RestShortest = 1200L;
    public static final long RestLongest = 2400L;
    public static final int NodShortest = 300;
    public static final int NodSpread = 400;
    public static final double NodRange = 6.0;
    public static final double PartingRange = 16.0;
    public static final int VoidMargin = 24;
    public static final RawAnimation NodPlay = RawAnimation.begin().thenPlay("nod");
    public static final RawAnimation TalkPlay = RawAnimation.begin().thenPlay("talk");
    protected static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(Scp343God.class, EntityDataSerializers.BYTE);

    private Vec3 PhaseFrom = Vec3.ZERO;
    private Vec3 PhaseTo = Vec3.ZERO;
    private long PhaseStart;
    private int LocalPhaseStart;
    private boolean DepartOnLoad;
    private boolean Leaving;
    private boolean Forced;
    @Nullable
    private BlockPos LeaveStand;
    @Nullable
    private BlockPos LeaveWall;
    private int LeaveTicks;
    private int ContainedTicks;
    @Nullable
    private UUID Listener;
    private int AttendLeft;
    private final Map<UUID, Long> LineReady = new HashMap<>();
    private int LastLine = -1;
    @Nullable
    private UUID Seeker;
    private int SeekLeft;
    private int NodReady;
    @Nullable
    private BlockPos ThronePos;
    private boolean FirstSit = true;
    private long SitReady;
    private long SitUntil;
    private long NextRoll;

    public Scp343God(EntityType<? extends Scp343God> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PhaseData, PhaseNone);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp343Goals.StepAway(this));
        goalSelector.addGoal(2, new Scp343Goals.Attend(this));
        goalSelector.addGoal(2, new Scp343Goals.Approach(this));
        goalSelector.addGoal(3, new Scp343Goals.Sit(this));
        goalSelector.addGoal(4, new Scp343Goals.Homeward(this, HomeSpeed));
        goalSelector.addGoal(5, new Scp343Goals.Wander(this, StrollSpeed, StrollInterval));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LookRange, LookChance));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        super.registerControllers(Controllers);
        Controllers.add(new AnimationController<>(this, "gesture", 4, Event -> PlayState.STOP).triggerableAnim("nod", NodPlay).triggerableAnim("talk", TalkPlay));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        restrictTo(blockPosition(), HomeRadius);
        return Result;
    }

    public byte phase() {
        return entityData.get(PhaseData);
    }

    private void setPhase(byte Kind) {
        entityData.set(PhaseData, Kind);
    }

    public boolean isPhasing() {
        return phase() != PhaseNone;
    }

    public boolean isDeparting() {
        return phase() == PhaseDeparting;
    }

    public boolean isEmerging() {
        return phase() == PhaseEmerging;
    }

    public boolean isSeated() {
        return getVehicle() instanceof FurnishingSeat;
    }

    public boolean isLeaving() {
        return Leaving;
    }

    public boolean isForced() {
        return Forced;
    }

    @Nullable
    public BlockPos leaveStand() {
        return LeaveStand;
    }

    @Nullable
    public BlockPos leaveWall() {
        return LeaveWall;
    }

    public int leaveTicks() {
        return LeaveTicks;
    }

    public void countLeave() {
        LeaveTicks++;
    }

    public int containedTicks() {
        return ContainedTicks;
    }

    public void setContainedTicks(int Ticks) {
        ContainedTicks = Ticks;
    }

    @Nullable
    public BlockPos thronePos() {
        return ThronePos;
    }

    public long sitUntil() {
        return SitUntil;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (PhaseData.equals(Accessor)) {
            LocalPhaseStart = tickCount;
        }
    }

    public float fade(float Partial) {
        byte Kind = phase();
        if (Kind == PhaseNone) {
            return 1.0f;
        }
        float Progress = Mth.clamp((tickCount - LocalPhaseStart + Partial) / PhaseTicks, 0.0f, 1.0f);
        return Kind == PhaseEmerging ? 0.15f + 0.85f * Progress : 1.0f - Progress;
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    @Override
    public int getMaxHeadYRot() {
        return isSeated() ? SeatedHeadTurn : MaxHeadTurn;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp343.refuses");
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (Source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            if (level() instanceof ServerLevel && isAlive()) {
                Scp343Away.depart(this);
            }
            return false;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    protected void onBelowWorld() {
        if (level() instanceof ServerLevel && isAlive()) {
            Scp343Away.depart(this);
            return;
        }
        super.onBelowWorld();
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return Effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(Effect);
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
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return Vehicle instanceof FurnishingSeat && super.canRide(Vehicle);
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
    protected boolean shouldDropLoot() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp343Registry.Hum.get();
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World && !isPhasing()) {
            Scp343Speech.play(World, this, Scp343Registry.HumLayers, 0.5f, 0.95f + getRandom().nextFloat() * 0.1f);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 360;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp343Registry.StepIn.get();
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND || User.isSpectator() || isPhasing()) {
            return InteractionResult.PASS;
        }
        ItemStack Held = User.getItemInHand(Hand);
        if (Held.is(Items.NAME_TAG) || Held.is(Items.LEAD)) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Speaker) {
            listen(Speaker);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void listen(ServerPlayer Speaker) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Listener = Speaker.getUUID();
        AttendLeft = AttendTicks;
        getNavigation().stop();
        getLookControl().setLookAt(Speaker, 60.0f, 40.0f);
        if (!isSeated()) {
            float Yaw = (float) (Mth.atan2(Speaker.getZ() - getZ(), Speaker.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Yaw);
            setYBodyRot(Yaw);
            setYHeadRot(Yaw);
        }
        long Now = World.getGameTime();
        Long Ready = LineReady.get(Speaker.getUUID());
        if (Ready != null && Now < Ready) {
            return;
        }
        LineReady.put(Speaker.getUUID(), Now + LineCooldown);
        if (LineReady.size() > 32) {
            LineReady.values().removeIf(Until -> Until < Now);
        }
        LastLine = Scp343Speech.pick(getRandom(), Scp343Speech.Lines, LastLine);
        Scp343Speech.say(Speaker, this, Scp343Speech.line(LastLine));
        Scp343Speech.chord(World, this);
        triggerAnim("gesture", "talk");
        List<Scp343Blessings.Gift> Given = Scp343Blessings.grant(Speaker);
        if (!Given.isEmpty()) {
            Scp343Speech.play(World, Speaker, Scp343Registry.BlessLayers, 0.8f, 1.0f);
            Scp343Effects.blessing(World, Speaker);
            Speaker.displayClientMessage(Component.translatable("message.scp_unleashed.scp343.blessed").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        }
    }

    @Nullable
    public Player listener() {
        if (Listener == null || AttendLeft <= 0) {
            return null;
        }
        Player Found = level().getPlayerByUUID(Listener);
        return Found != null && Found.isAlive() && !Found.isSpectator() && Found.distanceToSqr(this) < 64.0 ? Found : null;
    }

    public boolean isAttending() {
        return listener() != null;
    }

    public void seek(@Nullable UUID Target) {
        Seeker = Target;
        SeekLeft = Target == null ? 0 : SeekTicks;
    }

    @Nullable
    public Player seeker() {
        if (Seeker == null || SeekLeft <= 0) {
            return null;
        }
        Player Found = level().getPlayerByUUID(Seeker);
        return Found != null && Found.isAlive() && !Found.isSpectator() && Targeting.isTargetable(Found) ? Found : null;
    }

    public void greet(ServerPlayer Target) {
        Seeker = null;
        SeekLeft = 0;
        getNavigation().stop();
        getLookControl().setLookAt(Target, 60.0f, 40.0f);
        if (level() instanceof ServerLevel World) {
            Scp343Speech.say(Target, this, Scp343Speech.greeting(getRandom().nextInt(Scp343Speech.Greetings)));
            Scp343Speech.chord(World, this);
            triggerAnim("gesture", "nod");
            NodReady = tickCount + NodShortest;
        }
    }

    public boolean wantsToSit(long Now) {
        if (ThronePos == null || Leaving || isSeated() || isPhasing() || isAttending() || seeker() != null || Now < SitReady || Now < NextRoll) {
            return false;
        }
        NextRoll = Now + SitRollEvery;
        return FirstSit || getRandom().nextFloat() < SitChance;
    }

    public void sitDelayed(long Now) {
        SitReady = Now + SitRollEvery * 3;
    }

    public boolean sitOn(ServerLevel World, BlockPos Throne) {
        if (!Scp343Thrones.formed(World, Throne) || Scp343Thrones.occupied(World, Throne)) {
            return false;
        }
        for (FurnishingSeat Stale : World.getEntitiesOfClass(FurnishingSeat.class, new AABB(Throne))) {
            if (!Stale.isVehicle()) {
                Stale.discard();
            }
        }
        float Yaw = Scp343Thrones.facing(World, Throne).toYRot();
        FurnishingSeat Seat = new FurnishingSeat(World, Scp343Thrones.seat(World, Throne), Yaw);
        if (!World.addFreshEntity(Seat)) {
            return false;
        }
        if (!startRiding(Seat, true)) {
            Seat.discard();
            return false;
        }
        getNavigation().stop();
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        long Now = World.getGameTime();
        SitUntil = Now + SitShortest + getRandom().nextInt((int) (SitLongest - SitShortest));
        FirstSit = false;
        return true;
    }

    public void standUp(long Now) {
        if (isSeated()) {
            stopRiding();
        }
        SitReady = Now + RestShortest + getRandom().nextInt((int) (RestLongest - RestShortest));
    }

    public void startLeaving(@Nullable BlockPos Stand, @Nullable BlockPos Wall, boolean Force) {
        if (Leaving || isPhasing()) {
            return;
        }
        Leaving = true;
        Forced = Force;
        LeaveStand = Stand;
        LeaveWall = Wall;
        LeaveTicks = 0;
        if (isSeated()) {
            stopRiding();
        }
        if (level() instanceof ServerLevel World) {
            Component Parting = Scp343Speech.parting(getRandom().nextInt(Scp343Speech.Partings));
            for (ServerPlayer Nearby : World.players()) {
                if (Nearby.distanceToSqr(this) <= PartingRange * PartingRange && !Nearby.isSpectator()) {
                    Scp343Speech.say(Nearby, this, Parting);
                }
            }
        }
    }

    public void cancelLeaving() {
        Leaving = false;
        Forced = false;
        LeaveStand = null;
        LeaveWall = null;
        LeaveTicks = 0;
        ContainedTicks = 0;
    }

    public void beginDeparture(BlockPos Wall) {
        if (!(level() instanceof ServerLevel World) || isPhasing()) {
            return;
        }
        if (isSeated()) {
            stopRiding();
        }
        getNavigation().stop();
        PhaseFrom = position();
        PhaseTo = Wall.equals(blockPosition()) ? position() : Vec3.atBottomCenterOf(Wall);
        if (!PhaseTo.equals(PhaseFrom)) {
            float Yaw = (float) (Mth.atan2(PhaseTo.z - PhaseFrom.z, PhaseTo.x - PhaseFrom.x) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Yaw);
            setYBodyRot(Yaw);
            setYHeadRot(Yaw);
        }
        PhaseStart = World.getGameTime();
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPhase(PhaseDeparting);
        Scp343Speech.play(World, this, Scp343Registry.StepInLayers, 0.9f, 1.0f);
        Scp343Effects.wallGlow(World, PhaseTo, 18);
    }

    public void prepareEmergence(Scp343Walls.Emergence Spot) {
        PhaseFrom = Spot.from();
        PhaseTo = Spot.to();
        PhaseStart = level().getGameTime();
        float Yaw = Spot.hasWall() ? Spot.yaw() : getRandom().nextFloat() * 360.0f;
        moveTo(PhaseFrom.x, PhaseFrom.y, PhaseFrom.z, Yaw, 0.0f);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPhase(PhaseEmerging);
        cancelLeaving();
        DepartOnLoad = false;
    }

    public void announceEmergence(ServerLevel World) {
        Scp343Speech.play(World, this, Scp343Registry.StepOutLayers, 0.9f, 1.0f);
        Scp343Effects.wallGlow(World, PhaseFrom, 18);
    }

    public void prepareAway() {
        if (isSeated()) {
            stopRiding();
        }
        setPhase(PhaseNone);
        noPhysics = false;
        setNoGravity(false);
        cancelLeaving();
        Listener = null;
        AttendLeft = 0;
        Seeker = null;
        SeekLeft = 0;
        ThronePos = null;
        FirstSit = true;
        SitReady = 0L;
        NextRoll = 0L;
        DepartOnLoad = false;
    }

    private void tickPhase(ServerLevel World) {
        long Elapsed = Math.max(0L, World.getGameTime() - PhaseStart);
        float Progress = Mth.clamp((float) Elapsed / PhaseTicks, 0.0f, 1.0f);
        Vec3 At = PhaseFrom.lerp(PhaseTo, Progress);
        setPos(At.x, At.y, At.z);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        if (Elapsed % 3L == 0L) {
            Scp343Effects.wallGlow(World, isEmerging() ? PhaseFrom : PhaseTo, 3);
        }
        if (Elapsed < PhaseTicks) {
            return;
        }
        if (isDeparting()) {
            Scp343Away.depart(this);
            return;
        }
        setPhase(PhaseNone);
        noPhysics = false;
        setNoGravity(false);
        setPos(PhaseTo.x, PhaseTo.y, PhaseTo.z);
        restrictTo(blockPosition(), HomeRadius);
    }

    private void tickThrone(ServerLevel World) {
        if ((tickCount + getId()) % ThroneCheckEvery != 0) {
            return;
        }
        BlockPos Found = Scp343Thrones.nearest(World, position(), Scp343Thrones.Radius);
        if (Found != null) {
            if (ThronePos == null) {
                SitReady = Math.max(SitReady, World.getGameTime() + FirstSitDelay);
                NextRoll = 0L;
            }
            ThronePos = Found;
            restrictTo(Found, ThroneHome);
            ContainedTicks = 0;
            if (Leaving && !Forced) {
                cancelLeaving();
            }
            return;
        }
        if (ThronePos != null) {
            ThronePos = null;
            FirstSit = true;
            restrictTo(blockPosition(), HomeRadius);
        }
    }

    private void tickSeat(ServerLevel World) {
        Entity Vehicle = getVehicle();
        if (Vehicle == null) {
            return;
        }
        if (!(Vehicle instanceof FurnishingSeat Seat)) {
            stopRiding();
            return;
        }
        long Now = World.getGameTime();
        BlockPos Under = Seat.blockPosition();
        if (Now >= SitUntil || !Scp343Thrones.formed(World, Under) || Leaving) {
            standUp(Now);
            return;
        }
        setYBodyRot(Seat.getYRot());
        setYRot(Seat.getYRot());
    }

    private void tickContainment(ServerLevel World) {
        if ((tickCount + getId()) % ContainCheckEvery != 0 || isSeated() || Forced) {
            return;
        }
        if (ThronePos != null) {
            ContainedTicks = 0;
            return;
        }
        Scp343Enclosure.Survey Found = Scp343Enclosure.survey(World, blockPosition());
        if (!Found.Enclosed()) {
            if (Leaving) {
                cancelLeaving();
            }
            ContainedTicks = 0;
            return;
        }
        if (isAttending() || seeker() != null) {
            return;
        }
        ContainedTicks += ContainCheckEvery;
        if (!Leaving && ContainedTicks >= ContainGrace) {
            startLeaving(Found.Stand(), Found.Wall(), false);
        }
    }

    public boolean checkContainmentNow() {
        if (!(level() instanceof ServerLevel World) || isPhasing()) {
            return false;
        }
        Scp343Enclosure.Survey Found = Scp343Enclosure.survey(World, blockPosition());
        if (ThronePos != null || !Found.Enclosed()) {
            ContainedTicks = 0;
            return false;
        }
        ContainedTicks = Math.max(ContainedTicks + ContainCheckEvery, ContainGrace);
        if (!Leaving) {
            startLeaving(Found.Stand(), Found.Wall(), false);
        }
        return true;
    }

    public void leaveNow(boolean Force) {
        if (!(level() instanceof ServerLevel World) || isPhasing()) {
            return;
        }
        Scp343Enclosure.Survey Found = Scp343Enclosure.survey(World, blockPosition());
        BlockPos Stand = Found.Stand();
        BlockPos Wall = Found.Wall();
        if (Wall == null) {
            Scp343Walls.Emergence Near = Scp343Walls.near(World, blockPosition(), 1, 8, getRandom(), true, null, 0.0).orElse(null);
            if (Near != null) {
                Stand = Near.Stand();
                Wall = Near.Wall();
            }
        }
        cancelLeaving();
        startLeaving(Stand, Wall == null ? blockPosition() : Wall, Force);
        if (Stand == null) {
            beginDeparture(blockPosition());
        }
    }

    private void tickNod(ServerLevel World) {
        if (tickCount < NodReady || isAttending() || isPhasing()) {
            return;
        }
        Player Near = World.getNearestPlayer(this, NodRange);
        if (Near != null && !Near.isSpectator() && hasLineOfSight(Near)) {
            triggerAnim("gesture", "nod");
            NodReady = tickCount + NodShortest + getRandom().nextInt(NodSpread);
            return;
        }
        NodReady = tickCount + 40;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (DepartOnLoad) {
            DepartOnLoad = false;
            Scp343Away.depart(this);
            return;
        }
        if (isPhasing()) {
            tickPhase(World);
            return;
        }
        if (getY() < World.getMinBuildHeight() - VoidMargin) {
            Scp343Away.depart(this);
            return;
        }
        if (AttendLeft > 0) {
            AttendLeft--;
        }
        if (AttendLeft <= 0) {
            Listener = null;
        }
        if (SeekLeft > 0 && --SeekLeft <= 0) {
            Seeker = null;
        }
        if (ThronePos == null && (!hasRestriction() || getRestrictCenter().distSqr(blockPosition()) > (double) HomeDrift * HomeDrift)) {
            restrictTo(blockPosition(), HomeRadius);
        }
        tickThrone(World);
        tickSeat(World);
        tickContainment(World);
        tickNod(World);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp343Effects.clientAura(this);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (hasRestriction()) {
            Saved.putLong("Home", getRestrictCenter().asLong());
        }
        Saved.putByte("Phase", phase());
        if (isPhasing()) {
            Saved.putDouble("ToX", PhaseTo.x);
            Saved.putDouble("ToY", PhaseTo.y);
            Saved.putDouble("ToZ", PhaseTo.z);
        }
        Saved.putInt("Contained", ContainedTicks);
        Saved.putLong("SitReady", SitReady);
        Saved.putBoolean("FirstSit", FirstSit);
        if (Seeker != null) {
            Saved.putUUID("Seeker", Seeker);
            Saved.putInt("SeekLeft", SeekLeft);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        if (Saved.contains("Home")) {
            restrictTo(BlockPos.of(Saved.getLong("Home")), HomeRadius);
        }
        byte Kind = Saved.getByte("Phase");
        if (Kind == PhaseDeparting) {
            DepartOnLoad = true;
        } else if (Kind == PhaseEmerging && Saved.contains("ToX")) {
            setPos(Saved.getDouble("ToX"), Saved.getDouble("ToY"), Saved.getDouble("ToZ"));
        }
        setPhase(PhaseNone);
        noPhysics = false;
        setNoGravity(false);
        ContainedTicks = Saved.getInt("Contained");
        SitReady = Saved.getLong("SitReady");
        FirstSit = !Saved.contains("FirstSit") || Saved.getBoolean("FirstSit");
        if (Saved.hasUUID("Seeker")) {
            Seeker = Saved.getUUID("Seeker");
            SeekLeft = Saved.getInt("SeekLeft");
        }
        setPersistenceRequired();
    }

    public Direction facingDirection() {
        return Direction.fromYRot(getYRot());
    }
}
