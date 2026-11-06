package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp2845 extends PathfinderMob implements GeoEntity {
    public static final float Width = 1.4f;
    public static final float Height = 3.0f;
    public static final float EyeHeight = 2.6f;
    public static final double MaxHealth = 1000.0;
    public static final double Speed = 0.28;
    public static final double AttackDamage = 14.0;
    public static final double FollowRange = 48.0;
    public static final double StepHeight = 1.5;
    public static final double RoamModifier = 0.5;
    public static final double HuntModifier = 1.35;
    public static final double ReturnModifier = 1.0;
    public static final double CalmModifier = 0.4;
    public static final double WalkDistance = 1.65;
    public static final double GallopDistance = 2.75;
    public static final double StrideDistance = 1.5125;
    public static final double[] WalkContacts = {0.0, 0.25, 0.5, 0.75};
    public static final double[] GallopContacts = {0.0, 0.08, 0.48, 0.56};
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.22, 0.17, WalkDistance, GallopDistance, 0.6, 1);
    public static final StrideGait.Shape Plough = new StrideGait.Shape(0.004, 9.0, 8.0, StrideDistance, StrideDistance, 0.6, 1);
    public static final double SightRange = 40.0;
    public static final double MobRange = 20.0;
    public static final double HomeRange = 24.0;
    public static final int RetargetTicks = 10;
    public static final int BowTicks = 240;
    public static final int RecallTimeout = 400;
    public static final double ArriveGap = 0.8;
    public static final double SettleGap = 3.5;
    public static final double RecallFar = 40.0;
    public static final double RecallRange = 160.0;
    public static final int RecallReach = 6;
    public static final double StandOff = 2.4;
    public static final long UpkeepTicks = 72000L;
    public static final int CircleCheckTicks = 40;
    public static final int BreakoutTicks = 500;
    public static final double BreakoutSpeed = 0.13;
    public static final double BreakoutClear = Scp2845Circle.Reach + 5.0;
    public static final int ArriveTicks = 70;
    public static final int AmbientInterval = 420;
    public static final float StepVolume = 0.5f;
    public static final double CullSpread = 2.5;
    public static final double CullRise = 2.2;
    public static final byte Free = 0;
    public static final byte Recalled = 1;
    public static final byte Bowing = 2;
    public static final byte Contained = 3;
    public static final byte Breaking = 4;
    public static final byte Arriving = 5;
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp2845.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation GallopLoop = RawAnimation.begin().thenLoop("gallop");
    private static final RawAnimation StrideLoop = RawAnimation.begin().thenLoop("stride");
    private static final RawAnimation BowHold = RawAnimation.begin().thenPlayAndHold("bow");
    private static final RawAnimation CastOnce = RawAnimation.begin().thenPlay("cast");
    private static final RawAnimation StompOnce = RawAnimation.begin().thenPlay("stomp");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    final Scp2845Powers Powers = new Scp2845Powers(this);
    @Nullable
    private BlockPos Home;
    @Nullable
    private BlockPos CircleStone;
    @Nullable
    private BlockPos BowStone;
    private long LastOffering;
    private int BowLeft;
    private int RecallLeft;
    private boolean ReturnContained;
    @Nullable
    private BlockPos BreakFrom;
    private double BreakX;
    private double BreakZ;
    private int BreakLeft;
    private int ArriveLeft;
    @Nullable
    private UUID Prey;
    private int RetargetWait;
    private int RepathWait;
    private float RenderPartial;

    public Scp2845(EntityType<? extends Scp2845> Type, Level World) {
        super(Type, World);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, StepHeight)
                .add(Attributes.SAFE_FALL_DISTANCE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, Free);
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
        record(World.getLevel());
        return Result;
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    void setMode(byte Value) {
        if (mode() != Value) {
            entityData.set(ModeData, Value);
        }
    }

    public boolean isBowing() {
        return mode() == Bowing;
    }

    public boolean isContained() {
        return mode() == Contained;
    }

    public boolean isBreakingOut() {
        return mode() == Breaking;
    }

    public boolean isRecalled() {
        return mode() == Recalled;
    }

    public boolean isFree() {
        byte Value = mode();
        return Value == Free || Value == Arriving;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    @Nullable
    public BlockPos circleStone() {
        return CircleStone;
    }

    @Nullable
    public BlockPos bowStone() {
        return BowStone;
    }

    public long lastOffering() {
        return LastOffering;
    }

    public int bowLeft() {
        return BowLeft;
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

    public void record(ServerLevel World) {
        Scp2845Records.State Phase = switch (mode()) {
            case Contained -> Scp2845Records.State.Contained;
            default -> Scp2845Records.State.Free;
        };
        Scp2845Records.get(World.getServer()).set(getUUID(), Phase, World, Phase == Scp2845Records.State.Contained ? CircleStone : Home);
    }

    public void arrive(ServerLevel World, BlockPos Stone) {
        Home = Stone.immutable();
        ArriveLeft = ArriveTicks;
        setMode(Arriving);
        record(World);
        Scp2845StoneBlock.seal(World, Stone, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
        }
    }

    private void clientTick() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && !isBowing(), isBreakingOut() ? Plough : Gait);
        if (Moving && !isSilent()) {
            int Steps = contactsCrossed(Stride.from(), Stride.to(), isRunning() ? GallopContacts : WalkContacts);
            for (int Index = 0; Index < Steps; Index++) {
                float Volume = StepVolume * (isRunning() ? 1.5f : isBreakingOut() ? 1.4f : 1.0f);
                level().playLocalSound(getX(), getY(), getZ(), Scp2845Registry.Hoof.get(), SoundSource.HOSTILE, Volume, 0.85f + random.nextFloat() * 0.15f, false);
                level().playLocalSound(getX(), getY(), getZ(), Scp2845Registry.HoofStone.get(), SoundSource.HOSTILE, Volume * 0.6f, 0.8f + random.nextFloat() * 0.15f, false);
            }
        }
        if (isBreakingOut() && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 2.0, getY() + 2.4 + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 2.0,
                    0.0, 0.02, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level() instanceof ServerLevel World) {
            serverStep(World);
        }
    }

    public void serverStep(ServerLevel World) {
        Powers.cool();
        switch (mode()) {
            case Arriving -> arrivingStep(World);
            case Recalled -> recallStep(World);
            case Bowing -> bowStep(World);
            case Contained -> containedStep(World);
            case Breaking -> breakingStep(World);
            default -> freeStep(World);
        }
    }

    private void arrivingStep(ServerLevel World) {
        getNavigation().stop();
        if (ArriveLeft == ArriveTicks) {
            call(World, 1.6f);
            triggerAnim("action", "cast");
        }
        ArriveLeft--;
        if (ArriveLeft <= 0) {
            setMode(Free);
        }
    }

    private boolean validPrey(LivingEntity Candidate) {
        if (Candidate == this || !Candidate.isAlive() || Candidate instanceof Scp2845 || Candidate instanceof Chicken || Candidate instanceof ArmorStand || Candidate.isInvulnerable()) {
            return false;
        }
        if (!Targeting.isTargetable(Candidate) || Candidate.getType().is(Tags.EntityTypes.BOSSES) || Candidate.getType().is(Scp2845Registry.Ignored)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return true;
    }

    @Nullable
    public LivingEntity prey() {
        if (Prey == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        Entity Found = World.getEntity(Prey);
        return Found instanceof LivingEntity Living && validPrey(Living) && distanceTo(Living) <= SightRange + 8.0 ? Living : null;
    }

    public void setPrey(@Nullable LivingEntity Target) {
        Prey = Target == null ? null : Target.getUUID();
        setTarget(Target);
    }

    private void retarget(ServerLevel World) {
        LivingEntity Current = prey();
        AABB Sight = getBoundingBox().inflate(SightRange, 16.0, SightRange);
        List<Player> Players = World.getEntitiesOfClass(Player.class, Sight, Candidate -> validPrey(Candidate) && distanceTo(Candidate) <= SightRange
                && (hasLineOfSight(Candidate) || distanceTo(Candidate) < 16.0));
        LivingEntity Best = Players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (Best == null) {
            AABB Near = getBoundingBox().inflate(MobRange, 8.0, MobRange);
            Best = World.getEntitiesOfClass(LivingEntity.class, Near, Candidate -> !(Candidate instanceof Player) && validPrey(Candidate) && hasLineOfSight(Candidate))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        if (Current instanceof Player && Current.isAlive() && (Best == null || distanceTo(Current) < distanceTo(Best) + 6.0)) {
            return;
        }
        setPrey(Best);
    }

    private void freeStep(ServerLevel World) {
        if (tickCount % CircleCheckTicks == 0) {
            keepHome(World);
        }
        if (RetargetWait-- <= 0) {
            RetargetWait = RetargetTicks;
            retarget(World);
        }
        LivingEntity Target = prey();
        if (Powers.busy()) {
            Powers.tick(World, Target);
            return;
        }
        if (Target == null) {
            setTarget(null);
            roam(World);
            return;
        }
        getLookControl().setLookAt(Target, 40.0f, 40.0f);
        if (Powers.choose(World, Target)) {
            return;
        }
        double Gap = distanceTo(Target);
        if (Gap > 3.0 && (RepathWait-- <= 0 || getNavigation().isDone())) {
            RepathWait = 6;
            getNavigation().moveTo(Target, Gap > 8.0 ? HuntModifier : RoamModifier * 1.4);
        } else if (Gap <= 3.0) {
            getNavigation().stop();
        }
    }

    private void keepHome(ServerLevel World) {
        if (Home == null || !World.isLoaded(Home)) {
            return;
        }
        if (!Scp2845Circle.isStone(World, Home)) {
            Home = null;
            record(World);
            return;
        }
        Scp2845StoneBlock.seal(World, Home, true);
    }

    private void roam(ServerLevel World) {
        if (!getNavigation().isDone()) {
            return;
        }
        BlockPos Anchor = Home != null ? Home : blockPosition();
        if (Home != null && distanceToSqr(Vec3.atBottomCenterOf(Home)) > HomeRange * HomeRange) {
            getNavigation().moveTo(Home.getX() + 0.5, Home.getY(), Home.getZ() + 0.5, RoamModifier);
            return;
        }
        if (random.nextInt(120) == 0) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = 4.0 + random.nextDouble() * 10.0;
            getNavigation().moveTo(Anchor.getX() + 0.5 + Math.cos(Angle) * Reach, Anchor.getY(), Anchor.getZ() + 0.5 + Math.sin(Angle) * Reach, RoamModifier);
        }
    }

    public void call(ServerLevel World, float Volume) {
        Scp2845Sounds.play(World, getEyePosition(), Scp2845Registry.CallLayers, Volume, 0.9f + random.nextFloat() * 0.1f);
    }

    public void chime(ServerLevel World, float Volume) {
        Scp2845Sounds.play(World, getEyePosition().add(0.0, 0.6, 0.0), Scp2845Registry.ChimeLayers, Volume, 0.8f + random.nextFloat() * 0.3f);
    }

    public Vec3 standSpot(BlockPos Stone) {
        Vec3 Centre = Vec3.atBottomCenterOf(Stone);
        Vec3 Away = position().subtract(Centre).multiply(1.0, 0.0, 1.0);
        if (Away.lengthSqr() < 1.0e-3) {
            Away = new Vec3(0.0, 0.0, 1.0);
        }
        return Centre.add(Away.normalize().scale(StandOff));
    }

    public void recall(ServerLevel World, BlockPos Stone) {
        if (isBowing() || isRecalled()) {
            return;
        }
        ReturnContained = isContained();
        Powers.cancel();
        setPrey(null);
        BowStone = Stone.immutable();
        if (!ReturnContained) {
            Home = BowStone;
            CircleStone = null;
            record(World);
            Scp2845StoneBlock.seal(World, BowStone, true);
        }
        RecallLeft = RecallTimeout;
        setMode(Recalled);
        chime(World, 1.2f);
        if (distanceToSqr(Vec3.atBottomCenterOf(BowStone)) > RecallFar * RecallFar) {
            Powers.warpTo(World, standSpot(BowStone));
        }
    }

    private void recallStep(ServerLevel World) {
        if (BowStone == null || World.isLoaded(BowStone) && !Scp2845Circle.isStone(World, BowStone)) {
            endBow(World);
            return;
        }
        Vec3 Spot = standSpot(BowStone);
        Vec3 Centre = Vec3.atCenterOf(BowStone);
        getLookControl().setLookAt(Centre.x, Centre.y, Centre.z, 30.0f, 30.0f);
        double Gap = position().multiply(1.0, 0.0, 1.0).distanceTo(Spot.multiply(1.0, 0.0, 1.0));
        RecallLeft--;
        if (Gap < ArriveGap || RecallLeft <= 0) {
            if (Gap >= SettleGap) {
                Powers.warpTo(World, Spot);
            } else if (Gap >= ArriveGap) {
                teleportTo(Spot.x, Math.max(getY(), Spot.y), Spot.z);
            }
            startBow(World);
            return;
        }
        if (Gap < SettleGap) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, ReturnModifier * 0.6);
            return;
        }
        if (getNavigation().isDone() || tickCount % 20 == 0) {
            getNavigation().moveTo(Spot.x, Spot.y, Spot.z, ReturnModifier);
        }
    }

    public void startBow(ServerLevel World) {
        if (BowStone == null) {
            return;
        }
        getNavigation().stop();
        Vec3 Centre = Vec3.atBottomCenterOf(BowStone);
        float Yaw = (float) (Mth.atan2(Centre.z - getZ(), Centre.x - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        BowLeft = BowTicks;
        setMode(Bowing);
        Scp2845Sounds.play(World, getEyePosition(), Scp2845Registry.Bow, 1.0f, 0.9f);
        chime(World, 0.9f);
    }

    private void bowStep(ServerLevel World) {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (BowStone != null && BowLeft % 40 == 0) {
            World.sendParticles(ParticleTypes.END_ROD, BowStone.getX() + 0.5, BowStone.getY() + 1.1, BowStone.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
        }
        BowLeft--;
        if (BowLeft <= 0) {
            endBow(World);
        }
    }

    private void endBow(ServerLevel World) {
        BowLeft = 0;
        if (ReturnContained && CircleStone != null) {
            setMode(Contained);
        } else {
            setMode(Free);
            Powers.defy();
            call(World, 1.2f);
        }
        ReturnContained = false;
        record(World);
    }

    public void feed(ServerLevel World, long Now) {
        LastOffering = Now;
        chime(World, 0.7f);
    }

    public static void offering(ServerLevel World, Vec3 At, @Nullable Player Giver) {
        long Now = World.getGameTime();
        AABB Reach = new AABB(At, At).inflate(Scp2845Circle.Reach + 4, 12.0, Scp2845Circle.Reach + 4);
        for (Scp2845 Deer : World.getEntitiesOfClass(Scp2845.class, Reach, Scp2845::isAlive)) {
            if ((Deer.isContained() || Deer.isBowing() && Deer.ReturnContained) && Deer.CircleStone != null && Scp2845Circle.inside(Deer.CircleStone, At)) {
                Deer.feed(World, Now);
            }
        }
        List<BlockPos> Stones = Scp2845Circle.stonesNear(World, BlockPos.containing(At), RecallReach);
        Stones.sort(Comparator.comparingDouble(Pos -> Pos.distToCenterSqr(At)));
        for (BlockPos Stone : Stones) {
            Scp2845 Bound = summoned(World, Stone);
            if (Bound != null) {
                Bound.recall(World, Stone);
            }
        }
    }

    @Nullable
    public static Scp2845 summoned(ServerLevel World, BlockPos Stone) {
        List<? extends Scp2845> Everyone = World.getEntities(Scp2845Registry.DeerType.get(), Scp2845::isAlive);
        Scp2845 Best = null;
        double BestDistance = Double.MAX_VALUE;
        Vec3 Centre = Vec3.atCenterOf(Stone);
        for (Scp2845 Deer : Everyone) {
            if (Deer.isBowing() || Deer.isRecalled()) {
                continue;
            }
            if (Deer.isContained() && Stone.equals(Deer.CircleStone) || Deer.isFree() && Stone.equals(Deer.Home)) {
                return Deer;
            }
            if (!Deer.isFree() && !Deer.isBreakingOut()) {
                continue;
            }
            double Distance = Deer.distanceToSqr(Centre);
            if (Distance < RecallRange * RecallRange && Distance < BestDistance) {
                Best = Deer;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public void releasedFromStasis(ServerLevel World) {
        Powers.cancel();
        setPrey(null);
        BowLeft = 0;
        ReturnContained = false;
        BowStone = null;
        ghost(false);
        Optional<BlockPos> Circle = Scp2845Circle.validAround(World, position());
        if (Circle.isPresent()) {
            contain(World, Circle.get());
            return;
        }
        Home = null;
        CircleStone = null;
        setMode(Free);
        record(World);
        call(World, 1.4f);
    }

    public void contain(ServerLevel World, BlockPos Stone) {
        CircleStone = Stone.immutable();
        Home = null;
        LastOffering = World.getGameTime();
        setMode(Contained);
        record(World);
        Scp2845StoneBlock.seal(World, CircleStone, false);
        chime(World, 1.0f);
    }

    private void containedStep(ServerLevel World) {
        setTarget(null);
        if (CircleStone == null) {
            breakout(World);
            return;
        }
        long Now = World.getGameTime();
        if (tickCount % CircleCheckTicks == 0) {
            if (Now - LastOffering > UpkeepTicks) {
                breakout(World);
                return;
            }
            if (Scp2845Circle.check(World, CircleStone).broken()) {
                breakout(World);
                return;
            }
        }
        if (!Scp2845Circle.inside(CircleStone, position())) {
            if (getNavigation().isDone() || tickCount % 20 == 0) {
                getNavigation().moveTo(CircleStone.getX() + 0.5, CircleStone.getY(), CircleStone.getZ() + 0.5, CalmModifier * 1.5);
            }
            return;
        }
        if (getNavigation().isDone() && random.nextInt(90) == 0) {
            int Room = Scp2845Circle.Reach - 5;
            double X = CircleStone.getX() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * Room;
            double Z = CircleStone.getZ() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * Room;
            getNavigation().moveTo(X, CircleStone.getY(), Z, CalmModifier);
        }
    }

    public void breakout(ServerLevel World) {
        BlockPos From = CircleStone != null ? CircleStone : blockPosition();
        Vec3 Centre = Vec3.atBottomCenterOf(From);
        Vec3 Way = position().subtract(Centre).multiply(1.0, 0.0, 1.0);
        if (Way.lengthSqr() < 1.0) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            Way = new Vec3(Math.cos(Angle), 0.0, Math.sin(Angle));
        }
        Way = Way.normalize();
        BreakFrom = From.immutable();
        BreakX = Way.x;
        BreakZ = Way.z;
        BreakLeft = BreakoutTicks;
        boolean StoneKept = CircleStone != null && Scp2845Circle.isStone(World, CircleStone);
        Home = StoneKept ? CircleStone : null;
        CircleStone = null;
        getNavigation().stop();
        setMode(Breaking);
        Scp2845Records.get(World.getServer()).set(getUUID(), Scp2845Records.State.Free, World, Home);
        if (Home != null) {
            Scp2845StoneBlock.seal(World, Home, true);
        }
        call(World, 2.0f);
        Scp2845Sounds.play(World, position(), Scp2845Registry.Rend, 1.4f, 0.8f);
    }

    public boolean griefs(ServerLevel World) {
        return EventHooks.canEntityGrief(World, this);
    }

    private void breakingStep(ServerLevel World) {
        if (BreakFrom == null) {
            setMode(Free);
            return;
        }
        Vec3 Way = new Vec3(BreakX, 0.0, BreakZ);
        float Yaw = (float) (Mth.atan2(BreakZ, BreakX) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        getNavigation().stop();
        boolean Grief = griefs(World);
        if (Grief) {
            ghost(false);
            clearPath(World, Way);
            move(MoverType.SELF, Way.scale(BreakoutSpeed));
        } else {
            ghost(true);
            setDeltaMovement(Vec3.ZERO);
            setPos(position().add(Way.scale(BreakoutSpeed)));
        }
        BreakLeft--;
        double Travelled = position().multiply(1.0, 0.0, 1.0).distanceTo(Vec3.atBottomCenterOf(BreakFrom).multiply(1.0, 0.0, 1.0));
        if (Travelled >= BreakoutClear || BreakLeft <= 0) {
            ghost(false);
            surface(World);
            BreakFrom = null;
            setMode(Free);
            record(World);
            Powers.defy();
        }
    }

    private void surface(ServerLevel World) {
        for (int Lift = 0; Lift <= 8 && !World.noCollision(this); Lift++) {
            setPos(getX(), Math.floor(getY()) + 1.0, getZ());
        }
    }

    private void ghost(boolean Through) {
        noPhysics = Through;
        setNoGravity(Through);
    }

    public int clearPath(ServerLevel World, Vec3 Way) {
        AABB Ahead = getBoundingBox().move(Way.scale(0.8)).inflate(0.35, 0.0, 0.35).expandTowards(0.0, 0.6, 0.0);
        int Cleared = 0;
        BlockPos Feet = blockPosition();
        for (BlockPos Pos : BlockPos.betweenClosed(BlockPos.containing(Ahead.minX, Ahead.minY + 0.05, Ahead.minZ), BlockPos.containing(Ahead.maxX, Ahead.maxY, Ahead.maxZ))) {
            if (Pos.getY() < Feet.getY()) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || State.getDestroySpeed(World, Pos) < 0.0f || State.is(Scp2845Registry.Stone.get())) {
                continue;
            }
            if (State.getCollisionShape(World, Pos).isEmpty() && State.getFluidState().isEmpty() && !State.canBeReplaced()) {
                continue;
            }
            if (World.destroyBlock(Pos, true, this)) {
                Cleared++;
            }
        }
        if (Cleared > 0 && tickCount % 4 == 0) {
            Scp2845Sounds.play(World, position(), Scp2845Registry.Rend, 0.9f, 0.7f + random.nextFloat() * 0.2f);
        }
        return Cleared;
    }

    public void dissolve() {
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 60, 0.8, 1.2, 0.8, 0.05);
            World.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.5, getZ(), 40, 0.8, 1.2, 0.8, 0.02);
            chime(World, 1.5f);
            forgetSelf(World);
        }
        remove(RemovalReason.KILLED);
    }

    private void forgetSelf(ServerLevel World) {
        Scp2845Records.get(World.getServer()).forget(getUUID());
        if (Home != null && World.isLoaded(Home)) {
            Scp2845StoneBlock.seal(World, Home, false);
        }
    }

    @Override
    public void kill() {
        dissolve();
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (level() instanceof ServerLevel World && !isRemoved()) {
            if (Reason == RemovalReason.CHANGED_DIMENSION && isBowing() && BowStone != null) {
                Scp2845Records.get(World.getServer()).set(getUUID(), Scp2845Records.State.Stasis, World, BowStone);
                Scp2845StoneBlock.seal(World, BowStone, false);
            } else if (Reason == RemovalReason.DISCARDED || Reason == RemovalReason.KILLED) {
                forgetSelf(World);
            }
        }
        super.remove(Reason);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide) {
            return false;
        }
        if (Source.is(DamageTypes.FELL_OUT_OF_WORLD) || Source.is(DamageTypes.OUTSIDE_BORDER)) {
            rescue();
            return false;
        }
        if (Source.is(DamageTypes.GENERIC_KILL) && Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            dissolve();
            return false;
        }
        shrug(Source);
        return false;
    }

    private void rescue() {
        BlockPos Anchor = Home != null ? Home : CircleStone != null ? CircleStone : level().getSharedSpawnPos();
        int Y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Anchor.getX(), Anchor.getZ());
        teleportTo(Anchor.getX() + 0.5, Y + 1.0, Anchor.getZ() + 2.5);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0f;
    }

    private void shrug(DamageSource Source) {
        if (!(level() instanceof ServerLevel World) || invulnerableTime > 0) {
            return;
        }
        invulnerableTime = 10;
        World.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.6, getZ(), 12, 0.6, 0.8, 0.6, 0.4);
        Scp2845Sounds.play(World, getEyePosition(), Scp2845Registry.Shrug, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        if (isFree() && Source.getEntity() instanceof LivingEntity Attacker && validPrey(Attacker)) {
            setPrey(Attacker);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypes.GENERIC_KILL) && !Source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
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
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean AllowPassengers) {
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
    public boolean doHurtTarget(Entity Target) {
        return Powers.gore(Target);
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) {
            dissolve();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CullSpread, 0.0, CullSpread).expandTowards(0.0, CullRise, 0.0);
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World && !isBowing()) {
            if (isContained()) {
                chime(World, 0.4f);
            } else {
                call(World, 0.9f);
            }
        }
    }

    void playAction(String Name) {
        triggerAnim("action", Name);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Mode", mode());
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
        if (CircleStone != null) {
            Tag.put("Circle", NbtUtils.writeBlockPos(CircleStone));
        }
        if (BowStone != null) {
            Tag.put("BowStone", NbtUtils.writeBlockPos(BowStone));
        }
        if (BreakFrom != null) {
            Tag.put("BreakFrom", NbtUtils.writeBlockPos(BreakFrom));
        }
        Tag.putLong("LastOffering", LastOffering);
        Tag.putInt("BowLeft", BowLeft);
        Tag.putInt("RecallLeft", RecallLeft);
        Tag.putBoolean("ReturnContained", ReturnContained);
        Tag.putDouble("BreakX", BreakX);
        Tag.putDouble("BreakZ", BreakZ);
        Tag.putInt("BreakLeft", BreakLeft);
        Tag.putInt("ArriveLeft", ArriveLeft);
        Tag.put("Powers", Powers.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setMode(Tag.getByte("Mode"));
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        CircleStone = NbtUtils.readBlockPos(Tag, "Circle").orElse(null);
        BowStone = NbtUtils.readBlockPos(Tag, "BowStone").orElse(null);
        BreakFrom = NbtUtils.readBlockPos(Tag, "BreakFrom").orElse(null);
        LastOffering = Tag.getLong("LastOffering");
        BowLeft = Tag.getInt("BowLeft");
        RecallLeft = Tag.getInt("RecallLeft");
        ReturnContained = Tag.getBoolean("ReturnContained");
        BreakX = Tag.getDouble("BreakX");
        BreakZ = Tag.getDouble("BreakZ");
        BreakLeft = Tag.getInt("BreakLeft");
        ArriveLeft = Tag.getInt("ArriveLeft");
        Powers.load(Tag.getCompound("Powers"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp2845GaitController(this, "body", 5, Event -> {
            if (isBowing()) {
                return Event.setAndContinue(BowHold);
            }
            if (isStriding()) {
                if (isBreakingOut()) {
                    return Event.setAndContinue(StrideLoop);
                }
                return Event.setAndContinue(isRunning() ? GallopLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 3, Event -> PlayState.STOP).triggerableAnim("cast", CastOnce)
                .triggerableAnim("stomp", StompOnce).triggerableAnim("attack", AttackOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
