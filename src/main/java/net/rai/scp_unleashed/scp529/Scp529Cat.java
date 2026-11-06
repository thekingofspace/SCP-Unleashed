package net.rai.scp_unleashed.scp529;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp529Cat extends Cat implements GeoEntity, IEntityWithComplexSpawn, ActionClock.Owner {
    public static final String NameKey = "entity.scp_unleashed.scp529_cat.name";
    public static final float Width = 0.6f;
    public static final float Height = 0.7f;
    public static final double MaxHealth = 10.0;
    public static final double Speed = 0.2;
    public static final double AttackDamage = 3.0;
    public static final double FollowRange = 24.0;
    public static final double SeekModifier = 1.0;
    public static final double FretModifier = 1.0;
    public static final double WalkDistance = 0.4516;
    public static final double RunDistance = 1.1;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.006, 0.07, 0.062, WalkDistance, RunDistance, 0.25, 2);
    public static final byte ActionNone = 0;
    public static final byte ActionSitDown = 1;
    public static final byte ActionStandUp = 2;
    public static final byte ActionLieDown = 3;
    public static final byte ActionGetUp = 4;
    public static final byte ActionStretch = 5;
    public static final byte ActionGroom = 6;
    public static final byte ActionKnead = 7;
    public static final byte ActionSwat = 8;
    public static final byte ActionHiss = 9;
    public static final byte ActionYowl = 10;
    public static final byte ActionBury = 11;
    public static final int SitDownTicks = 12;
    public static final int StandUpTicks = 12;
    public static final int LieDownTicks = 18;
    public static final int GetUpTicks = 16;
    public static final int StretchTicks = 44;
    public static final int GroomTicks = 52;
    public static final int KneadTicks = 32;
    public static final int SwatTicks = 12;
    public static final int HissTicks = 28;
    public static final int YowlTicks = 30;
    public static final int BuryTicks = 36;
    public static final byte RestNone = 0;
    public static final byte RestLoaf = 1;
    public static final byte RestSleep = 2;
    public static final int SitSettleTicks = 600;
    public static final int SitDozeTicks = 1200;
    public static final int NapChance = 2400;
    public static final int NapMin = 400;
    public static final int NapMax = 1400;
    public static final int NapDoze = 300;
    public static final int WakeGrace = 100;
    public static final int IdleActionMin = 300;
    public static final int IdleActionMax = 900;
    public static final float StretchAfterWake = 0.7f;
    public static final int BuryDelayMin = 600;
    public static final int BuryDelayMax = 1800;
    public static final int CravingTicks = 6000;
    public static final int DistressTicks = 9600;
    public static final int YowlMin = 120;
    public static final int YowlMax = 220;
    public static final int StrokeCooldown = 10;
    public static final double StrokeReach = 3.0;
    public static final double BehindDot = -0.3;
    public static final float VoidPurrChance = 0.3f;
    public static final float ScratchDamage = 1.0f;
    public static final float CheeseHeal = 2.0f;
    public static final int SleepPurrInterval = 60;
    public static final int StrokeIgnored = 0;
    public static final int StrokePurr = 1;
    public static final int FondFish = 2;
    public static final int FondCheese = 1;
    public static final int FondStroke = 1;
    public static final int FondHurt = -6;
    public static final int FondMax = 10;
    public static final int FondMin = -10;
    public static final int FondLike = 3;
    public static final int VisitMin = 1200;
    public static final int VisitMax = 2400;
    public static final double VisitRange = 24.0;
    public static final double LikedNear = 6.0;
    public static final int SettleChance = 2400;
    public static final int SettleMin = 300;
    public static final int SettleMax = 1200;
    public static final int StrokeSwat = 2;
    public static final int StrokeYowl = 3;
    private static final Set<Byte> Holding = Set.of(ActionStretch, ActionGroom, ActionKnead, ActionBury, ActionHiss);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp529Cat.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp529Cat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> RestData = SynchedEntityData.defineId(Scp529Cat.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DistressData = SynchedEntityData.defineId(Scp529Cat.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SneakLoop = RawAnimation.begin().thenLoop("sneak");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SitLoop = RawAnimation.begin().thenLoop("sit");
    private static final RawAnimation LoafLoop = RawAnimation.begin().thenLoop("loaf");
    private static final RawAnimation SleepLoop = RawAnimation.begin().thenLoop("sleep");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    private final StrideGait Stride = new StrideGait();
    @Nullable
    private AnimationController<Scp529Cat> BodyController;
    private int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    private int SatTicks;
    private final Map<UUID, Integer> Fondness = new HashMap<>();
    private int VisitLeft = VisitMin;
    private boolean Napping;
    private int NapTicks;
    private int RestTicks;
    private int IdleLeft = IdleActionMin;
    private boolean StretchPending;
    private int BuryLeft;
    private boolean Craving;
    private int CravingLeft;
    private int DistressLeft;
    private int YowlLeft;
    private int StrokeLeft;
    private int HurtAt = -100000;
    private int CheeseFed;
    @Nullable
    private UUID Sought;
    private boolean Loading;

    public Scp529Cat(EntityType<? extends Scp529Cat> Type, Level World) {
        super(Type, World);
        if (!World.isClientSide) {
            setCustomName(Component.translatable(NameKey));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(RestData, RestNone);
        Builder.define(DistressData, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new Scp529Goals.Fret(this));
        goalSelector.addGoal(5, new Scp529Goals.BedNap(this));
        goalSelector.addGoal(6, new Scp529Goals.Seek(this));
        goalSelector.addGoal(7, new Scp529Goals.Perch(this));
        goalSelector.addGoal(10, new Scp529Goals.Nap(this));
        goalSelector.addGoal(10, new Scp529Goals.Settle(this));
    }

    @Override
    protected void reassessTameGoals() {
    }

    @Override
    public void tame(Player User) {
    }

    @Override
    public void setTame(boolean Tame, boolean ApplyEffects) {
        super.setTame(false, ApplyEffects);
    }

    @Override
    public void setOrderedToSit(boolean OrderedToSit) {
        super.setOrderedToSit(false);
    }

    public int fondness(UUID Who) {
        return Fondness.getOrDefault(Who, 0);
    }

    public boolean likes(UUID Who) {
        return fondness(Who) >= FondLike;
    }

    public void fond(UUID Who, int Change) {
        int Value = Mth.clamp(fondness(Who) + Change, FondMin, FondMax);
        if (Value == 0) {
            Fondness.remove(Who);
        } else {
            Fondness.put(Who, Value);
        }
    }

    @Nullable
    public Player favourite(double Range) {
        Player Best = null;
        int BestFondness = FondLike - 1;
        for (Player User : level().players()) {
            if (User.isSpectator() || !User.isAlive() || User.distanceTo(this) > Range) {
                continue;
            }
            int Value = fondness(User.getUUID());
            if (Value > BestFondness) {
                Best = User;
                BestFondness = Value;
            }
        }
        return Best;
    }

    public boolean visitDue() {
        return VisitLeft <= 0;
    }

    public void visited() {
        VisitLeft = VisitMin + random.nextInt(VisitMax - VisitMin + 1);
    }

    public void hurryVisit(int Ticks) {
        VisitLeft = Math.min(VisitLeft, Ticks);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        if (getCustomName() == null) {
            setCustomName(Component.translatable(NameKey));
        }
        return Result;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public byte rest() {
        return entityData.get(RestData);
    }

    public boolean isResting() {
        return rest() != RestNone;
    }

    public boolean isDistressed() {
        return entityData.get(DistressData);
    }

    public boolean isCraving() {
        return Craving;
    }

    public int cheeseFed() {
        return CheeseFed;
    }

    public int cravingLeft() {
        return CravingLeft;
    }

    public int distressLeft() {
        return DistressLeft;
    }

    public int buryLeft() {
        return BuryLeft;
    }

    public boolean isNapping() {
        return Napping;
    }

    public int napTicks() {
        return NapTicks;
    }

    public int satTicks() {
        return SatTicks;
    }

    public void hurryCraving(int Ticks) {
        CravingLeft = Math.min(CravingLeft, Ticks);
    }

    public void hurryDistress(int Ticks) {
        DistressLeft = Math.min(DistressLeft, Ticks);
    }

    public void hurryBury(int Ticks) {
        BuryLeft = Ticks;
    }

    public void hurrySitting(int Ticks) {
        SatTicks = Math.max(SatTicks, Ticks);
    }

    public void hurryIdle(int Ticks) {
        IdleLeft = Ticks;
    }

    @Nullable
    public UUID sought() {
        return Sought;
    }

    public void seek(@Nullable UUID Who) {
        Sought = Who;
    }

    public boolean recentlyHurt() {
        return tickCount - HurtAt < WakeGrace;
    }

    public boolean canAct() {
        return isAlive() && onGround() && !isInWater() && !isInSittingPose() && !isResting() && !isActing() && !isPassenger() && !isLying();
    }

    public boolean idleFree() {
        return canAct() && getNavigation().isDone() && getTarget() == null && !isCrouching() && !isDistressed();
    }

    @Nullable
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(clockNow());
        }
        return clipOf(action());
    }

    @Nullable
    public static String clipOf(byte Kind) {
        return switch (Kind) {
            case ActionSitDown -> "sit_down";
            case ActionStandUp -> "stand_up";
            case ActionLieDown -> "lie_down";
            case ActionGetUp -> "get_up";
            case ActionStretch -> "stretch";
            case ActionGroom -> "groom";
            case ActionKnead -> "knead";
            case ActionSwat -> "swat";
            case ActionHiss -> "hiss";
            case ActionYowl -> "yowl";
            case ActionBury -> "bury";
            default -> null;
        };
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private double clockNow() {
        return tickCount + RenderPartial;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        Clock.begin(Current, clipOf(Current), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && (Key.equals(ActionData) || Key.equals(ActionStartData))) {
            syncClock();
            if (Key.equals(ActionStartData) && BodyController != null && isActing()) {
                BodyController.forceAnimationReset();
            }
        }
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Holding.contains(Kind)) {
            getNavigation().stop();
        }
        updateControlFlags();
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
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

    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        if (Holding.contains(action())) {
            goalSelector.setControlFlag(Goal.Flag.MOVE, false);
            goalSelector.setControlFlag(Goal.Flag.JUMP, false);
        }
    }

    @Override
    public void setInSittingPose(boolean Value) {
        boolean Was = isInSittingPose();
        super.setInSittingPose(Value);
        if (level().isClientSide || Loading || Was == Value || !isAlive()) {
            return;
        }
        if (Value) {
            setAction(ActionSitDown, SitDownTicks);
        } else if (isResting()) {
            wake();
        } else if (action() != ActionGetUp) {
            setAction(ActionStandUp, StandUpTicks);
        }
    }

    public void setNapping(boolean Value) {
        Napping = Value;
        NapTicks = 0;
    }

    public void napTick() {
        NapTicks++;
    }

    private byte wantedRest() {
        if (isLying()) {
            return RestSleep;
        }
        if (!onGround() || isInWater() || isDistressed() || recentlyHurt() || isPassenger()) {
            return RestNone;
        }
        if (Napping) {
            return NapTicks > NapDoze ? RestSleep : RestLoaf;
        }
        if (isInSittingPose() && SatTicks > SitSettleTicks) {
            return SatTicks > SitSettleTicks + SitDozeTicks && level().isNight() ? RestSleep : RestLoaf;
        }
        return RestNone;
    }

    public void settle(byte Rest) {
        byte Old = rest();
        if (Old == Rest) {
            return;
        }
        entityData.set(RestData, Rest);
        RestTicks = 0;
        if (Old == RestNone) {
            getNavigation().stop();
            setAction(ActionLieDown, LieDownTicks);
        } else if (Rest == RestNone) {
            setAction(ActionGetUp, GetUpTicks);
            StretchPending = random.nextFloat() < StretchAfterWake;
        }
    }

    public void wake() {
        Napping = false;
        settle(RestNone);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        if (ActionTicks > 0) {
            ActionTicks--;
            if (ActionTicks <= 0) {
                finishAction();
            }
        }
        serverTick(World);
    }

    private void finishAction() {
        byte Done = action();
        entityData.set(ActionData, ActionNone);
        updateControlFlags();
        if (Done == ActionGetUp && StretchPending) {
            StretchPending = false;
            if (canAct()) {
                setAction(ActionStretch, StretchTicks);
            }
        }
    }

    private void serverTick(ServerLevel World) {
        SatTicks = isInSittingPose() ? SatTicks + 1 : 0;
        VisitLeft = Math.max(0, VisitLeft - 1);
        RestTicks++;
        StrokeLeft = Math.max(0, StrokeLeft - 1);
        byte Wanted = wantedRest();
        if (Wanted != rest()) {
            settle(Wanted);
        }
        if (rest() == RestSleep && !isLying() && RestTicks % SleepPurrInterval == 0) {
            Scp529Effects.voice(this, Scp529Registry.Purr.get(), 0.35f, 0.95f);
        }
        if (BuryLeft > 0) {
            BuryLeft--;
            if (BuryLeft == 0) {
                if (idleFree()) {
                    bury(World);
                } else {
                    BuryLeft = 40;
                }
            }
        }
        if (IdleLeft > 0) {
            IdleLeft--;
        } else if (idleFree()) {
            byte Pick = random.nextBoolean() ? ActionGroom : ActionStretch;
            setAction(Pick, Pick == ActionGroom ? GroomTicks : StretchTicks);
            IdleLeft = IdleActionMin + random.nextInt(IdleActionMax - IdleActionMin + 1);
        }
        cravingTick(World);
    }

    private void cravingTick(ServerLevel World) {
        if (!Craving) {
            return;
        }
        if (isDistressed()) {
            DistressLeft--;
            if (DistressLeft <= 0) {
                calm();
                Craving = false;
                return;
            }
            YowlLeft--;
            if (YowlLeft <= 0) {
                yowl(World);
            }
            return;
        }
        CravingLeft--;
        if (CravingLeft <= 0) {
            distress(World);
        }
    }

    public void distress(ServerLevel World) {
        Craving = true;
        CravingLeft = 0;
        DistressLeft = DistressTicks;
        entityData.set(DistressData, true);
        Napping = false;
        if (isInSittingPose()) {
            setInSittingPose(false);
        }
        yowl(World);
    }

    public void calm() {
        entityData.set(DistressData, false);
        DistressLeft = 0;
    }

    public void yowl(ServerLevel World) {
        YowlLeft = YowlMin + random.nextInt(YowlMax - YowlMin + 1);
        Scp529Effects.voice(this, Scp529Registry.Yowl.get(), 1.0f, 0.95f + random.nextFloat() * 0.1f);
        if (canAct()) {
            setAction(ActionYowl, YowlTicks);
        }
    }

    public void bury(ServerLevel World) {
        setAction(ActionBury, BuryTicks);
        Scp529Effects.scratch(World, this);
    }

    public void scheduleBury() {
        BuryLeft = BuryDelayMin + random.nextInt(BuryDelayMax - BuryDelayMin + 1);
    }

    private void clientTick() {
        Clock.settle();
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && !isInSittingPose() && !isResting() && (onGround() || isInWater()), Gait);
    }

    public void face(Vec3 Target) {
        double Dx = Target.x - getX();
        double Dz = Target.z - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    public boolean behind(Vec3 From) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        Vec3 Toward = new Vec3(From.x - getX(), 0.0, From.z - getZ());
        if (Toward.lengthSqr() < 1.0e-6) {
            return false;
        }
        return Facing.dot(Toward.normalize()) < BehindDot;
    }

    public int stroke(ServerLevel World, Player User, float Roll) {
        if (StrokeLeft > 0 || !isAlive()) {
            return StrokeIgnored;
        }
        StrokeLeft = StrokeCooldown;
        if (isDistressed()) {
            yowl(World);
            return StrokeYowl;
        }
        if (behind(User.position()) && Roll >= VoidPurrChance) {
            face(User.position());
            if (canAct()) {
                setAction(ActionSwat, SwatTicks);
            }
            Scp529Effects.voice(this, Scp529Registry.Swat.get(), 0.9f, 1.0f);
            if (User.distanceTo(this) <= StrokeReach) {
                User.hurt(damageSources().mobAttack(this), ScratchDamage);
            }
            return StrokeSwat;
        }
        Scp529Effects.voice(this, Scp529Registry.Purr.get(), 0.8f, 1.0f);
        Scp529Effects.hearts(World, this, 2);
        fond(User.getUUID(), FondStroke);
        if (canAct()) {
            setAction(ActionKnead, KneadTicks);
        }
        return StrokePurr;
    }

    public boolean feedCheese(ServerLevel World, Player User, ItemStack Stack) {
        if (!isAlive()) {
            return false;
        }
        Stack.consume(1, User);
        heal(CheeseHeal);
        Scp529Effects.voice(this, Scp529Registry.Eat.get(), 1.0f, 1.0f);
        Scp529Effects.hearts(World, this, 3);
        fond(User.getUUID(), FondCheese);
        CheeseFed++;
        Craving = true;
        CravingLeft = CravingTicks;
        calm();
        scheduleBury();
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Held.isEmpty() && Hand == InteractionHand.MAIN_HAND && User.isSecondaryUseActive()) {
            if (level() instanceof ServerLevel World) {
                stroke(World, User, random.nextFloat());
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (Scp529Cheese.is(Held)) {
            if (level() instanceof ServerLevel World) {
                feedCheese(World, User, Held);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isFood(Held)) {
            if (level() instanceof ServerLevel World) {
                feedFish(World, User, Held);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (Held.getItem() instanceof DyeItem) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(User, Hand);
    }

    public boolean feedFish(ServerLevel World, Player User, ItemStack Stack) {
        if (!isAlive() || !isFood(Stack)) {
            return false;
        }
        FoodProperties Food = Stack.getFoodProperties(this);
        Stack.consume(1, User);
        heal(Food != null ? (float) Food.nutrition() : 1.0f);
        Scp529Effects.voice(this, Scp529Registry.Eat.get(), 1.0f, 1.0f);
        Scp529Effects.hearts(World, this, 3);
        fond(User.getUUID(), FondFish);
        scheduleBury();
        return true;
    }

    @Override
    public void hiss() {
        super.hiss();
        if (!level().isClientSide && canAct()) {
            setAction(ActionHiss, HissTicks);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit && !isInSittingPose() && !isResting() && !isActing()) {
            setAction(ActionSwat, SwatTicks);
        }
        return Hit;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Taken = super.hurt(Source, Amount);
        if (Taken && !level().isClientSide) {
            HurtAt = tickCount;
            Napping = false;
            if (Source.getEntity() instanceof Player User) {
                fond(User.getUUID(), FondHurt);
            }
        }
        return Taken;
    }

    @Override
    public void playSound(SoundEvent Sound, float Volume, float Pitch) {
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Scp529Registry.voice(Sound);
        if (Layers == null) {
            super.playSound(Sound, Volume, Pitch);
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            super.playSound(Layer.get(), Volume, Pitch);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (isDistressed()) {
            return Scp529Registry.Yowl.get();
        }
        if (rest() == RestSleep) {
            return null;
        }
        if (favourite(LikedNear) != null) {
            return random.nextInt(3) == 0 ? Scp529Registry.Trill.get() : Scp529Registry.Meow.get();
        }
        return super.getAmbientSound();
    }

    @Override
    public boolean canMate(Animal Other) {
        return false;
    }

    @Nullable
    @Override
    public Cat getBreedOffspring(ServerLevel World, AgeableMob Partner) {
        return null;
    }

    @Override
    public boolean isBaby() {
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
    protected void playStepSound(BlockPos Pos, BlockState State) {
        if (!isCrouching()) {
            super.playStepSound(Pos, State);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putBoolean("Craving", Craving);
        Saved.putInt("CravingLeft", CravingLeft);
        Saved.putBoolean("Distressed", isDistressed());
        Saved.putInt("DistressLeft", DistressLeft);
        Saved.putInt("CheeseFed", CheeseFed);
        Saved.putInt("BuryLeft", BuryLeft);
        if (Sought != null) {
            Saved.putUUID("Sought", Sought);
        }
        ListTag Fond = new ListTag();
        for (Map.Entry<UUID, Integer> Entry : Fondness.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Who", Entry.getKey());
            Row.putInt("Fondness", Entry.getValue());
            Fond.add(Row);
        }
        Saved.put("Fondness", Fond);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        Loading = true;
        try {
            super.readAdditionalSaveData(Saved);
            super.setOrderedToSit(false);
            setInSittingPose(false);
        } finally {
            Loading = false;
        }
        Craving = Saved.getBoolean("Craving");
        CravingLeft = Saved.getInt("CravingLeft");
        entityData.set(DistressData, Saved.getBoolean("Distressed"));
        DistressLeft = Saved.getInt("DistressLeft");
        CheeseFed = Saved.getInt("CheeseFed");
        BuryLeft = Saved.getInt("BuryLeft");
        Sought = Saved.hasUUID("Sought") ? Saved.getUUID("Sought") : null;
        Fondness.clear();
        for (Tag Each : Saved.getList("Fondness", Tag.TAG_COMPOUND)) {
            if (Each instanceof CompoundTag Entry && Entry.hasUUID("Who")) {
                Fondness.put(Entry.getUUID("Who"), Mth.clamp(Entry.getInt("Fondness"), FondMin, FondMax));
            }
        }
        setOwnerUUID(null);
        super.setTame(false, false);
        entityData.set(ActionData, ActionNone);
        entityData.set(RestData, RestNone);
        ActionTicks = 0;
        setPersistenceRequired();
        updateControlFlags();
    }

    public RawAnimation baseAnimation() {
        if (isDeadOrDying()) {
            return IdleLoop;
        }
        byte Rest = rest();
        if (Rest == RestSleep || isLying()) {
            return SleepLoop;
        }
        if (Rest == RestLoaf) {
            return LoafLoop;
        }
        if (isInSittingPose()) {
            return SitLoop;
        }
        if (isStriding()) {
            if (isRunning()) {
                return RunLoop;
            }
            return isCrouching() ? SneakLoop : WalkLoop;
        }
        return IdleLoop;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp529Cat> Body = new Scp529GaitController(this, "body", Scp529GaitController.LoopBlend, Event -> {
            String Shown = actionClip();
            if (Shown != null && Shown.equals("swat") && isRunning()) {
                Shown = null;
            }
            RawAnimation Base = baseAnimation();
            Event.getController().transitionLength(Scp529GaitController.blendFor(Shown, Base == SleepLoop || Base == LoafLoop));
            if (Shown != null && !isDeadOrDying()) {
                return Event.setAndContinue(Scp529GaitController.play(Shown));
            }
            return Event.setAndContinue(Base);
        });
        BodyController = Body;
        Controllers.add(Body);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
