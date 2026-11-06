package net.rai.scp_unleashed.scp469;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlRefusing;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class Scp469Angel extends Monster implements GeoEntity, IEntityWithComplexSpawn, ControlRefusing {
    public static final float SpreadWidth = 2.2f;
    public static final float SpreadHeight = 2.2f;
    public static final float FoldWidth = 0.9f;
    public static final float FoldHeight = 1.9f;
    public static final float EyeHeight = 1.4f;
    public static final float FoldEyeHeight = 1.5f;
    public static final EntityDimensions Spread = EntityDimensions.scalable(SpreadWidth, SpreadHeight).withEyeHeight(EyeHeight);
    public static final EntityDimensions Folded = EntityDimensions.scalable(FoldWidth, FoldHeight).withEyeHeight(FoldEyeHeight);
    public static final double MaxHealth = 180.0;
    public static final double Armour = 6.0;
    public static final double Toughness = 2.0;
    public static final double Speed = 0.3;
    public static final double RoamModifier = 0.48;
    public static final double HuntModifier = 1.0;
    public static final double LungeModifier = 1.22;
    public static final double FoldSlow = -0.3;
    public static final double FollowRange = 64.0;
    public static final double Steadiness = 0.6;
    public static final double Stride = 1.0;
    public static final float SweepDamageAmount = 7.0f;
    public static final double SweepShove = 1.6;
    public static final double SweepLift = 0.35;
    public static final double SweepReach = 2.7;
    public static final double FoldReach = 1.9;
    public static final double SweepArc = -0.2;
    public static final int SweepTicks = 18;
    public static final int SweepHitTick = 9;
    public static final int SweepCooldown = 26;
    public static final double EmbraceReach = 2.3;
    public static final double HoldDistance = 0.85;
    public static final int EmbraceTicks = 80;
    public static final int EmbraceInterval = 10;
    public static final float EmbraceDamageAmount = 1.5f;
    public static final int EmbraceCooldown = 240;
    public static final float EmbraceChance = 0.45f;
    public static final float RescueDamage = 10.0f;
    public static final int StaggerTicks = 30;
    public static final double LungeRange = 8.0;
    public static final int LungeTicks = 40;
    public static final int LungeCooldown = 100;
    public static final int LoseSightTicks = 100;
    public static final int SearchTicks = 100;
    public static final double ArriveDistance = 2.0;
    public static final int BurstTicks = 12;
    public static final int ReformTicks = 40;
    public static final float GlanceChance = 0.4f;
    public static final float ProjectileShare = 0.5f;
    public static final int MinFoldTicks = 30;
    public static final int SettleTicks = 15;
    public static final int ForcedFoldTicks = 40;
    public static final int LookAhead = 4;
    public static final double LookReach = 5.0;
    public static final double FootClear = 0.51;
    public static final int RegenDelay = 400;
    public static final int RegenInterval = 40;
    public static final float RegenAmount = 1.0f;
    public static final int AmbientInterval = 260;
    public static final int Experience = 30;
    public static final byte ActionNone = 0;
    public static final byte ActionSweep = 1;
    public static final byte ActionEmbrace = 2;
    public static final byte ActionReform = 3;
    public static final ResourceLocation FoldSlowId = Scp469Registry.location("scp469_fold_slow");
    public static final Scp469Gait.Shape AngelGait = new Scp469Gait.Shape(
            new Scp469Gait.Profile(16.0, 0.0446, 0.4, 2.2, new double[]{0.0, 0.167, 0.333, 0.5, 0.667, 0.833}, new float[]{1.0f, 0.9f, 1.0f, 0.9f, 1.0f, 0.9f}),
            new Scp469Gait.Profile(8.0, 0.1823, 0.6, 1.8, new double[]{0.0, 0.167, 0.333, 0.5, 0.667, 0.833}, new float[]{1.0f, 0.9f, 1.0f, 0.9f, 1.0f, 0.9f}),
            new Scp469Gait.Profile(10.0, 0.0625, 0.3, 2.4, new double[]{0.0, 0.167, 0.333, 0.5, 0.667, 0.833}, new float[]{0.8f, 0.7f, 0.8f, 0.7f, 0.8f, 0.7f}),
            0.12, 0.09, 1, () -> Scp469Registry.Step.get(), () -> Scp469Registry.StepScrape.get(), 0.32f, 0.22f, 1.0f, 24.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp469Angel.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp469Angel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FoldedData = SynchedEntityData.defineId(Scp469Angel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp469Angel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LungingData = SynchedEntityData.defineId(Scp469Angel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EmbracedData = SynchedEntityData.defineId(Scp469Angel.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation AlertLoop = RawAnimation.begin().thenLoop("alert");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SqueezeLoop = RawAnimation.begin().thenLoop("squeeze");
    private static final RawAnimation SweepPlay = RawAnimation.begin().thenPlayAndHold("sweep");
    private static final RawAnimation EmbracePlay = RawAnimation.begin().thenPlay("embrace").thenLoop("embrace_hold");
    private static final RawAnimation BurstPlay = RawAnimation.begin().thenPlayAndHold("burst");
    private static final RawAnimation ReformPlay = RawAnimation.begin().thenPlayAndHold("reform");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionTicks;
    private int SweepCooldownLeft;
    private int EmbraceCooldownLeft;
    private int LungeLeft;
    private int LungeCooldownLeft;
    private int FoldTicks;
    private int ForcedFold;
    private int StaggerLeft;
    private int LastHurtTick = -100000;
    @Nullable
    private Vec3 LastSeen;
    private int UnseenTicks;
    private int SearchLeft = -1;
    @Nullable
    private UUID Remembered;
    @Nullable
    private BlockPos Footing;
    @Nullable
    private LivingEntity Victim;
    private int EmbraceLeft;
    private float RescueTaken;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp469Gait Gait;
    private int FeatherClock;
    private int ActionEnded = -1000;

    public Scp469Angel(EntityType<? extends Scp469Angel> Type, Level World) {
        super(Type, World);
        xpReward = Experience;
        jumpControl = new Grounded(this);
        if (getNavigation() instanceof Scp469Navigation Ground) {
            Ground.setCanPassDoors(true);
            Ground.setCanOpenDoors(true);
        }
    }

    private static final class Grounded extends JumpControl {
        private Grounded(Scp469Angel Owner) {
            super(Owner);
        }

        @Override
        public void jump() {
        }

        @Override
        public void tick() {
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.ATTACK_DAMAGE, SweepDamageAmount)
                .add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.STEP_HEIGHT, Stride)
                .add(Attributes.JUMP_STRENGTH, 0.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 1024.0);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp469Navigation(this, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(FoldedData, false);
        Builder.define(HuntingData, false);
        Builder.define(LungingData, false);
        Builder.define(EmbracedData, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp469Goals.Hold(this));
        goalSelector.addGoal(2, new Scp469Goals.Batter(this));
        goalSelector.addGoal(3, new Scp469Goals.Hunt(this));
        goalSelector.addGoal(6, new Scp469Goals.Roam(this));
        targetSelector.addGoal(1, new Scp469Goals.Avenge(this));
        targetSelector.addGoal(2, new Scp469Goals.Seek(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp469.refuses");
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

    public boolean isFolded() {
        return entityData.get(FoldedData);
    }

    public boolean isHunting() {
        return entityData.get(HuntingData);
    }

    public boolean isLunging() {
        return entityData.get(LungingData);
    }

    public boolean isEmbracing() {
        return action() == ActionEmbrace;
    }

    public boolean isReforming() {
        return action() == ActionReform;
    }

    public int embracedId() {
        return entityData.get(EmbracedData);
    }

    @Nullable
    public LivingEntity victim() {
        return Victim;
    }

    @Nullable
    public Vec3 lastSeen() {
        return LastSeen;
    }

    @Nullable
    public UUID remembered() {
        return Remembered;
    }

    @Nullable
    public BlockPos footing() {
        return Footing;
    }

    public boolean busy() {
        return action() != ActionNone || StaggerLeft > 0;
    }

    public void remember(@Nullable Vec3 Seen, @Nullable UUID Who) {
        LastSeen = Seen;
        Remembered = Who;
        UnseenTicks = 0;
        SearchLeft = -1;
    }

    public void forget() {
        LastSeen = null;
        Remembered = null;
        SearchLeft = -1;
        UnseenTicks = 0;
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionSweep -> "sweep";
            case ActionEmbrace -> "embrace";
            case ActionReform -> "reform";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
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

    public float renderPartial() {
        return RenderPartial;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp469Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Nullable
    public Scp469Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp469Gait(AngelGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp469Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp469Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Stance) {
        return isFolded() ? Folded : Spread;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (FoldedData.equals(Accessor)) {
            refreshDimensions();
        }
    }

    public AABB spreadBox(Vec3 Spot) {
        AABB Box = Spread.makeBoundingBox(Spot);
        return new AABB(Box.minX + 0.02, Box.minY + FootClear, Box.minZ + 0.02, Box.maxX - 0.02, Box.maxY, Box.maxZ - 0.02);
    }

    public boolean spreadFits(Vec3 Spot) {
        return level().noCollision(this, spreadBox(Spot));
    }

    public void setFolded(boolean Fold) {
        if (Fold == isFolded()) {
            return;
        }
        if (!Fold && !spreadFits(position())) {
            return;
        }
        entityData.set(FoldedData, Fold);
        refreshDimensions();
        FoldTicks = 0;
        AttributeInstance Pace = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Pace != null) {
            Pace.removeModifier(FoldSlowId);
            if (Fold) {
                Pace.addTransientModifier(new AttributeModifier(FoldSlowId, FoldSlow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (level() instanceof ServerLevel World) {
            Scp469Effects.fold(World, this, Fold);
        }
    }

    private boolean wantsFold() {
        if (action() != ActionNone || tickCount - ActionEnded < SettleTicks) {
            return isFolded();
        }
        if (ForcedFold > 0) {
            return true;
        }
        if (!spreadFits(position())) {
            return true;
        }
        Path Route = getNavigation().getPath();
        if (Route == null || Route.isDone()) {
            return false;
        }
        int Last = Math.min(Route.getNodeCount(), Route.getNextNodeIndex() + LookAhead);
        for (int Index = Math.max(0, Route.getNextNodeIndex() - 1); Index < Last; Index++) {
            Node Step = Route.getNode(Index);
            Vec3 Spot = new Vec3(Step.x + 0.5, Step.y, Step.z + 0.5);
            if (Spot.distanceToSqr(position()) > LookReach * LookReach) {
                break;
            }
            if (!spreadFits(Spot)) {
                return true;
            }
        }
        return false;
    }

    public String foldState() {
        Path Route = getNavigation().getPath();
        StringBuilder Blocked = new StringBuilder();
        for (net.minecraft.world.phys.shapes.VoxelShape Shape : level().getBlockCollisions(this, spreadBox(position()))) {
            Blocked.append(Shape.bounds()).append(' ');
        }
        return "forced " + ForcedFold + ", fits " + spreadFits(position()) + ", horizontal " + horizontalCollision + ", route " + (Route == null ? "none" : Route.getNextNodeIndex() + "/" + Route.getNodeCount())
                + ", entities " + level().getEntityCollisions(this, spreadBox(position())).size() + ", blocks " + Blocked;
    }

    private void tickFold() {
        if (ForcedFold > 0) {
            ForcedFold--;
        }
        if (!isFolded() && horizontalCollision && getNavigation().isInProgress() && !busy() && tickCount - ActionEnded >= SettleTicks) {
            ForcedFold = ForcedFoldTicks;
        }
        boolean Want = wantsFold();
        if (isFolded()) {
            FoldTicks++;
            if (!Want && FoldTicks >= MinFoldTicks) {
                setFolded(false);
            }
        } else if (Want) {
            setFolded(true);
        }
    }

    public void beginReform() {
        setAction(ActionReform, ReformTicks);
        setFolded(false);
    }

    public void leaveStasis() {
        releaseVictim(false);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        StaggerLeft = 0;
        refreshDimensions();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp469Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            Scp469Effects.clientTick(this);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (onGround()) {
            Footing = blockPosition();
        }
        if (tickCount % 20 == 0 && getNavigation() instanceof Scp469Navigation Ground) {
            Ground.setCanOpenDoors(EventHooks.canEntityGrief(World, this));
        }
        SweepCooldownLeft = Math.max(0, SweepCooldownLeft - 1);
        EmbraceCooldownLeft = Math.max(0, EmbraceCooldownLeft - 1);
        LungeCooldownLeft = Math.max(0, LungeCooldownLeft - 1);
        StaggerLeft = Math.max(0, StaggerLeft - 1);
        tickMemory(World);
        tickAction(World);
        tickLunge(World);
        tickFold();
        boolean Hunting = getTarget() != null || LastSeen != null;
        if (Hunting != isHunting()) {
            entityData.set(HuntingData, Hunting);
        }
        tickMending();
    }

    private void tickMemory(ServerLevel World) {
        LivingEntity Target = getTarget();
        if (Target != null && !Scp469Prey.prey(this, Target)) {
            setTarget(null);
            Target = null;
        }
        if (Target != null) {
            if (getSensing().hasLineOfSight(Target)) {
                LastSeen = Target.position();
                Remembered = Target.getUUID();
                UnseenTicks = 0;
                SearchLeft = -1;
            } else {
                UnseenTicks++;
                if (LastSeen == null) {
                    LastSeen = Target.position();
                }
            }
        } else if (Remembered != null && LastSeen != null && tickCount % 20 == 0) {
            Entity Found = World.getEntity(Remembered);
            if (Found instanceof LivingEntity Living && Scp469Prey.prey(this, Living) && distanceToSqr(Living) < FollowRange * FollowRange && hasLineOfSight(Living)) {
                setTarget(Living);
            }
        }
        if (LastSeen != null && (Target == null || UnseenTicks > LoseSightTicks)) {
            if (SearchLeft < 0 && position().distanceToSqr(LastSeen) < ArriveDistance * ArriveDistance * 4.0) {
                SearchLeft = SearchTicks;
            }
            if (SearchLeft > 0) {
                SearchLeft--;
                if (SearchLeft == 0) {
                    forget();
                    setTarget(null);
                }
            }
        }
    }

    private void tickLunge(ServerLevel World) {
        if (LungeLeft > 0) {
            LungeLeft--;
            if (LungeLeft == 0) {
                entityData.set(LungingData, false);
                LungeCooldownLeft = LungeCooldown;
            }
        }
    }

    public boolean canLunge() {
        return LungeLeft <= 0 && LungeCooldownLeft <= 0 && !busy();
    }

    public void beginLunge() {
        if (!(level() instanceof ServerLevel World) || !canLunge()) {
            return;
        }
        LungeLeft = LungeTicks;
        entityData.set(LungingData, true);
        Scp469Effects.layers(World, this, Scp469Registry.LungeLayers, 1.2f, 1.0f);
    }

    public double huntSpeed() {
        return LungeLeft > 0 ? LungeModifier : HuntModifier;
    }

    private void tickAction(ServerLevel World) {
        byte Current = action();
        if (Current == ActionNone) {
            return;
        }
        int Elapsed = (int) actionElapsed();
        if (Current == ActionSweep && Elapsed == SweepHitTick) {
            landSweep(World);
        }
        if (Current == ActionEmbrace) {
            tickEmbrace(World);
            return;
        }
        if (Current == ActionReform) {
            getNavigation().stop();
            Scp469Effects.reforming(World, this, Elapsed);
        }
        if (ActionTicks > 0) {
            ActionTicks--;
            if (ActionTicks == 0) {
                entityData.set(ActionData, ActionNone);
                ActionEnded = tickCount;
            }
        }
    }

    public double reach() {
        return isFolded() ? FoldReach : SweepReach;
    }

    public double flatDistance(Entity Other) {
        double Dx = Other.getX() - getX();
        double Dz = Other.getZ() - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public boolean canStrike(LivingEntity Target) {
        return flatDistance(Target) - Target.getBbWidth() * 0.5 <= reach() && Math.abs(Target.getY() - getY()) < 2.5;
    }

    public boolean canEmbrace(LivingEntity Target) {
        return EmbraceCooldownLeft <= 0 && !isFolded() && !Scp469Embrace.isHeld(Target) && !Target.isPassenger() && flatDistance(Target) <= EmbraceReach
                && Math.abs(Target.getY() - getY()) < 1.5 && Scp469Prey.prey(this, Target);
    }

    public void restrain(int Ticks) {
        SweepCooldownLeft = Math.max(SweepCooldownLeft, Ticks);
        LungeCooldownLeft = Math.max(LungeCooldownLeft, Ticks);
    }

    public boolean attackReady() {
        return SweepCooldownLeft <= 0 && !busy();
    }

    public void strike(LivingEntity Target) {
        if (!attackReady()) {
            return;
        }
        getLookControl().setLookAt(Target, 30.0f, 30.0f);
        faceToward(Target);
        if (canEmbrace(Target) && getRandom().nextFloat() < EmbraceChance) {
            beginEmbrace(Target);
            return;
        }
        beginSweep();
    }

    public void faceToward(Entity Target) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
    }

    public void beginSweep() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        setAction(ActionSweep, SweepTicks);
        SweepCooldownLeft = SweepCooldown + SweepTicks;
        Scp469Effects.layers(World, this, Scp469Registry.FlapLayers, 1.0f, 0.9f);
    }

    private void landSweep(ServerLevel World) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        Scp469Effects.layers(World, this, Scp469Registry.SweepLayers, 1.2f, 0.95f);
        Scp469Effects.sweep(World, this, Facing);
        double Reach = reach();
        for (LivingEntity Struck : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Reach, 1.0, Reach), Other -> Other != this && Scp469Prey.prey(this, Other))) {
            Vec3 Offset = new Vec3(Struck.getX() - getX(), 0.0, Struck.getZ() - getZ());
            double Distance = Offset.length();
            if (Distance - Struck.getBbWidth() * 0.5 > Reach || Distance > 1.0e-3 && Offset.normalize().dot(Facing) < SweepArc) {
                continue;
            }
            hitWithSweep(World, Struck, Offset);
        }
    }

    public boolean hitWithSweep(ServerLevel World, LivingEntity Struck, Vec3 Offset) {
        boolean Hit = Struck.hurt(damageSources().source(Scp469Registry.SweepDamage, this), SweepDamageAmount);
        if (Hit) {
            Vec3 Away = Offset.lengthSqr() > 1.0e-4 ? Offset.normalize() : Vec3.directionFromRotation(0.0f, yBodyRot);
            double Resist = 1.0 - Mth.clamp(Struck.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 Shove = new Vec3(Away.x * SweepShove * Resist, SweepLift * Resist, Away.z * SweepShove * Resist);
            Struck.setDeltaMovement(Struck.getDeltaMovement().multiply(0.5, 0.0, 0.5).add(Shove));
            Struck.hasImpulse = true;
            Struck.hurtMarked = true;
            setLastHurtMob(Struck);
        }
        return Hit;
    }

    public void beginEmbrace(LivingEntity Target) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        setAction(ActionEmbrace, EmbraceTicks);
        Victim = Target;
        EmbraceLeft = EmbraceTicks;
        RescueTaken = 0.0f;
        entityData.set(EmbracedData, Target.getId());
        Scp469Embrace.seize(this, Target);
        Scp469Effects.layers(World, this, Scp469Registry.EmbraceLayers, 1.1f, 0.9f);
        Scp469Effects.burstAround(World, Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0), 26, 0.45);
    }

    public Vec3 holdPoint() {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        return position().add(Facing.x * HoldDistance, 0.0, Facing.z * HoldDistance);
    }

    private void tickEmbrace(ServerLevel World) {
        LivingEntity Held = Victim;
        getNavigation().stop();
        if (Held == null || Held.isRemoved() || Held.isDeadOrDying() || Held.level() != World || Held.distanceToSqr(this) > 36.0 || !Scp469Embrace.heldBy(Held, this)) {
            releaseVictim(false);
            return;
        }
        faceToward(Held);
        yBodyRot = getYRot();
        Scp469Embrace.tether(Held, holdPoint());
        int Elapsed = EmbraceTicks - EmbraceLeft;
        if (Elapsed > 0 && Elapsed % EmbraceInterval == 0) {
            Held.hurt(damageSources().source(Scp469Registry.EmbraceDamage, this), EmbraceDamageAmount);
            Scp469Effects.burstAround(World, Held.position().add(0.0, Held.getBbHeight() * 0.5, 0.0), 5, 0.3);
            if (Elapsed % (EmbraceInterval * 3) == 0) {
                Scp469Effects.layers(World, this, Scp469Registry.RustleLayers, 0.8f, 0.85f);
            }
        }
        EmbraceLeft--;
        if (EmbraceLeft <= 0) {
            releaseVictim(true);
        }
    }

    public void releaseVictim(boolean Push) {
        LivingEntity Held = Victim;
        Victim = null;
        if (Held != null) {
            Scp469Embrace.release(Held);
            if (Push && Held.isAlive()) {
                Vec3 Away = Held.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                Vec3 Shove = Away.lengthSqr() > 1.0e-4 ? Away.normalize().scale(0.6) : Vec3.ZERO;
                Held.setDeltaMovement(Shove.x, 0.2, Shove.z);
                Held.hasImpulse = true;
                Held.hurtMarked = true;
            }
            if (level() instanceof ServerLevel World) {
                Scp469Effects.burstAround(World, Held.position().add(0.0, Held.getBbHeight() * 0.5, 0.0), 14, 0.4);
            }
        }
        if (entityData.get(EmbracedData) != -1) {
            entityData.set(EmbracedData, -1);
        }
        if (action() == ActionEmbrace) {
            entityData.set(ActionData, ActionNone);
            ActionTicks = 0;
            ActionEnded = tickCount;
            EmbraceCooldownLeft = EmbraceCooldown;
        }
    }

    public void breakFree(boolean Shoved) {
        if (!isEmbracing()) {
            return;
        }
        releaseVictim(true);
        StaggerLeft = StaggerTicks;
        SweepCooldownLeft = Math.max(SweepCooldownLeft, StaggerTicks);
        if (level() instanceof ServerLevel World) {
            Scp469Effects.layers(World, this, Scp469Registry.HurtLayers, 1.0f, 1.1f);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (isReforming() && !Source.is(DamageTypes.GENERIC_KILL) && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Entity Attacker = Source.getEntity();
        if (level() instanceof ServerLevel World && isAlive() && Source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (getRandom().nextFloat() < GlanceChance) {
                Scp469Effects.glance(World, this, Source.getSourcePosition());
                return false;
            }
            Amount *= ProjectileShare;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World) {
            LastHurtTick = tickCount;
            Scp469Effects.hurtFeathers(World, this);
            if (isEmbracing() && Victim != null) {
                if (Attacker == Victim || Source.getDirectEntity() == Victim) {
                    breakFree(true);
                } else {
                    RescueTaken += Amount;
                    if (RescueTaken >= RescueDamage) {
                        breakFree(true);
                    }
                }
            }
            if (isAlive() && Attacker instanceof LivingEntity Living && Living != getTarget() && Scp469Prey.prey(this, Living)
                    && (getTarget() == null || distanceToSqr(Living) < distanceToSqr(getTarget()))) {
                setTarget(Living);
            }
        }
        return Hurt;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        double Before = getDeltaMovement().y;
        super.knockback(Strength * 0.5, X, Z);
        Vec3 After = getDeltaMovement();
        if (After.y > Math.max(0.0, Before)) {
            setDeltaMovement(After.x, Math.max(0.0, Before), After.z);
        }
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void jumpFromGround() {
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> Kind) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
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
    public boolean canChangeDimensions(Level From, Level To) {
        return !isEmbracing() && super.canChangeDimensions(From, To);
    }

    private void tickMending() {
        if (getHealth() < getMaxHealth() && tickCount - LastHurtTick > RegenDelay && tickCount % RegenInterval == 0) {
            heal(RegenAmount);
        }
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && !isRemoved() && !dead) {
            releaseVictim(false);
            if (!Source.is(DamageTypes.GENERIC_KILL)) {
                Scp469Reform.get(World).schedule(World, this);
            }
            Scp469Effects.burst(World, this);
        }
        super.die(Source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            if (deathTime % 3 == 0 && deathTime < BurstTicks) {
                Scp469Effects.burstAround(World, position().add(0.0, 1.2, 0.0), 10, 0.8);
            }
            if (deathTime >= BurstTicks && !isRemoved()) {
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (!level().isClientSide) {
            releaseVictim(false);
        }
        super.remove(Reason);
    }

    @Override
    protected boolean shouldDropLoot() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isHunting() ? Scp469Registry.Rustle.get() : Scp469Registry.Shimmer.get();
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World) {
            Scp469Effects.layers(World, this, isHunting() ? Scp469Registry.RustleLayers : Scp469Registry.ShimmerLayers, isHunting() ? 0.9f : 0.7f, 1.0f);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp469Registry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp469Effects.layers(World, this, Scp469Registry.HurtLayers, 0.9f, 1.0f);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0f;
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (level() instanceof ServerLevel World) {
            Scp469Effects.layers(World, this, Layers, Volume, Pitch);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp469GaitController(this, "body", 0, Event -> {
            Event.getController().transitionLength(Scp469GaitController.blendFor(this));
            if (isDeadOrDying()) {
                return Event.setAndContinue(BurstPlay);
            }
            byte Current = action();
            if (Current == ActionReform) {
                return Event.setAndContinue(ReformPlay);
            }
            if (Current == ActionEmbrace) {
                return Event.setAndContinue(EmbracePlay);
            }
            if (Current == ActionSweep) {
                return Event.setAndContinue(SweepPlay);
            }
            if (isFolded()) {
                return Event.setAndContinue(SqueezeLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            return Event.setAndContinue(isHunting() ? AlertLoop : IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putBoolean("Folded", isFolded());
        if (LastSeen != null) {
            Saved.putDouble("SeenX", LastSeen.x);
            Saved.putDouble("SeenY", LastSeen.y);
            Saved.putDouble("SeenZ", LastSeen.z);
        }
        if (Remembered != null) {
            Saved.putUUID("Remembered", Remembered);
        }
        if (Footing != null) {
            Saved.putLong("Footing", Footing.asLong());
        }
        Saved.putInt("SweepCooldown", SweepCooldownLeft);
        Saved.putInt("EmbraceCooldown", EmbraceCooldownLeft);
        Saved.putInt("Search", SearchLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        Victim = null;
        entityData.set(EmbracedData, -1);
        LastSeen = Saved.contains("SeenX") ? new Vec3(Saved.getDouble("SeenX"), Saved.getDouble("SeenY"), Saved.getDouble("SeenZ")) : null;
        Remembered = Saved.hasUUID("Remembered") ? Saved.getUUID("Remembered") : null;
        Footing = Saved.contains("Footing") ? BlockPos.of(Saved.getLong("Footing")) : null;
        SweepCooldownLeft = Saved.getInt("SweepCooldown");
        EmbraceCooldownLeft = Saved.getInt("EmbraceCooldown");
        SearchLeft = Saved.contains("Search") ? Saved.getInt("Search") : -1;
        boolean Fold = Saved.getBoolean("Folded");
        entityData.set(FoldedData, Fold);
        AttributeInstance Pace = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Pace != null) {
            Pace.removeModifier(FoldSlowId);
            if (Fold) {
                Pace.addTransientModifier(new AttributeModifier(FoldSlowId, FoldSlow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        refreshDimensions();
        entityData.set(HuntingData, LastSeen != null);
        setPersistenceRequired();
    }

    public static Holder<net.minecraft.world.damagesource.DamageType> sweepType(Level World) {
        return World.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(Scp469Registry.SweepDamage);
    }

    public int featherClock() {
        return FeatherClock++;
    }

    public boolean inFluid() {
        return isInWater() || isInLava() || isEyeInFluid(FluidTags.WATER);
    }
}
