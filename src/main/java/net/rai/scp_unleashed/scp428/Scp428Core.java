package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.npc.doors.NpcDoors;
import net.rai.scp_unleashed.scp268.Scp268;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp428.network.Scp428HoldPayload;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp428Core extends Monster implements GeoEntity {
    public static final int ReachTicks = 50;
    public static final int HoverTicks = 14;
    public static final double GrabReach = 0.9;
    public static final double GrabRise = 1.4;
    public static final double GripSlip = 3.0;
    public static final int RegrabCooldown = 100;
    public static final int MassRegrabTicks = 40;
    public static final int HolderDelay = 60;
    public static final int HolderRest = 20;
    public static final double DragLead = 0.07;
    public static final double HolderSlip = 2.6;
    public static final double SlotRadius = 0.8;
    public static final double ClutchRange = 12.0;
    public static final int AutoShortest = 7;
    public static final int AutoSpread = 6;
    public static final double EscapePush = 0.55;
    public static final double[] SlotAngles = {180.0, 100.0, 260.0, 35.0};
    public static final double HuddleGap = 0.6;
    public static final double BraceGap = 0.3;
    public static final double HuddleSpacing = 0.72;
    public static final double HuddleOuter = 0.95;
    public static final double GuardRange = 6.0;
    public static final double GuardStep = 1.2;
    public static final double GuardKeep = 1.6;
    public static final double GuardSpread = 0.8;
    public static final int GuardTicks = 5;
    public static final int CrowdedGuards = 5;
    public static final int BraceTicks = 30;
    public static final int TouchMemory = 20;
    public static final int AnchorFresh = 3;
    public static final int AngerTicks = 400;
    public static final double AngerRange = 48.0;
    public static final double AngerClutch = 20.0;
    public static final double AngerPace = 1.4;
    public static final double AngerJoin = 2.2;
    public static final int MaintainTicks = 10;
    public static final int MissingGrace = 60;
    public static final int RetargetTicks = 10;
    public static final int LinkTicks = 10;
    public static final double LoseShare = 1.6;
    public static final double HearShare = 0.75;
    public static final double AlertShare = 1.5;
    public static final float GrowRate = 0.05f;
    public static final float SqueezeRate = 0.125f;
    public static final int SqueezeHold = 30;
    public static final int FleeTicks = 80;
    public static final float FireShare = 1.5f;
    public static final int DeathTicks = 40;
    public static final int RecentTicks = 40;
    public static final int SquelchInterval = 16;
    public static final int PulseInterval = 140;
    public static final int CreakInterval = 12;
    public static final double MoveThreshold = 0.004;
    public static final double CrawlReference = 0.03;
    public static final double SpawnSpread = 2.5;
    public static final int Xp = 12;
    public static final double BobLow = 0.05;
    public static final double BobHigh = 0.05;
    public static final double BobRipple = 0.03;
    public static final double RenderSize = 2.0;
    public static final byte StateRoaming = 0;
    public static final byte StateHunting = 1;
    public static final byte StateGripping = 2;
    public static final byte StateFleeing = 3;
    public static final String RecordsKey = "Records";
    private static final EntityDataAccessor<Integer> CountData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GirthData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SqueezeData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<CompoundTag> GripData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> LinksData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.COMPOUND_TAG);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DriftLoop = RawAnimation.begin().thenLoop("drift");
    private static final RawAnimation GripLoop = RawAnimation.begin().thenLoop("grip");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final Vector3f FleshDust = new Vector3f(0.42f, 0.05f, 0.08f);
    private static final EntityDataAccessor<CompoundTag> TouchData = SynchedEntityData.defineId(Scp428Core.class, EntityDataSerializers.COMPOUND_TAG);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Scp428Record> Records = new ArrayList<>();
    private final List<PendingAbsorb> Pending = new ArrayList<>();
    private final Map<UUID, Long> Cooldowns = new HashMap<>();
    private final Map<UUID, Long> MassRegrab = new HashMap<>();
    private final Map<UUID, Scp428Hold> Holds = new LinkedHashMap<>();
    private final Map<UUID, Long> Recent = new HashMap<>();
    private final Scp428Doorwork.Agent Agent = new Scp428Doorwork.Agent();
    private boolean Seeded;
    private boolean Folding;
    @Nullable
    private UUID GripVictim;
    private long GripStart;
    @Nullable
    private UUID Alerted;
    private long AlertedAt;
    @Nullable
    private UUID FleeFrom;
    private int FleeLeft;
    private int SqueezeLeft;
    private int SquelchReady = SquelchInterval;
    private int PulseReady = PulseInterval;
    private int Absorbed;
    private float GirthO;
    private float SqueezeO;
    private double ClientSpeed;
    private int[] ClientLinks = new int[0];
    private final List<Retract> Retracts = new ArrayList<>();
    private final Map<Integer, Noted> LastAnchors = new HashMap<>();
    private final Map<UUID, Vec3> GuardSpots = new HashMap<>();
    @Nullable
    private UUID Intruder;
    private int BraceLeft;
    @Nullable
    private UUID BraceFrom;
    @Nullable
    private UUID AngerFrom;
    private int AngerLeft;

    public Scp428Core(EntityType<? extends Scp428Core> Type, Level World) {
        super(Type, World);
        xpReward = Xp;
        setPersistenceRequired();
        setNoGravity(true);
        moveControl = new Scp428Hover(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Scp428Size.maxHealth(1)).add(Attributes.MOVEMENT_SPEED, Scp428Size.speed(1))
                .add(Attributes.ARMOR, Scp428Size.armour(1)).add(Attributes.KNOCKBACK_RESISTANCE, Scp428Size.knockback(1))
                .add(Attributes.FOLLOW_RANGE, Scp428Size.sense(Scp428Size.Capacity) * LoseShare).add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(CountData, 0);
        Builder.define(GirthData, 0.0f);
        Builder.define(SqueezeData, 0.0f);
        Builder.define(StateData, StateRoaming);
        Builder.define(GripData, new CompoundTag());
        Builder.define(LinksData, new CompoundTag());
        Builder.define(TouchData, new CompoundTag());
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp428Navigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp428Goals.Hold(this));
        goalSelector.addGoal(2, new Scp428Goals.Flee(this));
        goalSelector.addGoal(3, new Scp428Goals.Hunt(this));
        goalSelector.addGoal(5, new Scp428Goals.Roam(this));
    }

    public int count() {
        return entityData.get(CountData);
    }

    public float girth() {
        return entityData.get(GirthData);
    }

    public float squeeze() {
        return entityData.get(SqueezeData);
    }

    public float girth(float Partial) {
        return Mth.lerp(Partial, GirthO, girth());
    }

    public float squeeze(float Partial) {
        return Mth.lerp(Partial, SqueezeO, squeeze());
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public boolean isGripping() {
        return GripVictim != null;
    }

    @Nullable
    public UUID gripVictim() {
        return GripVictim;
    }

    public int absorbed() {
        return Absorbed;
    }

    public Scp428Doorwork.Agent agent() {
        return Agent;
    }

    public List<Scp428Record> records() {
        return List.copyOf(Records);
    }

    public double clientSpeed() {
        return ClientSpeed;
    }

    public List<Retract> retracts() {
        return Retracts;
    }

    public void noteAnchor(int Id, Vec3 Anchor) {
        LastAnchors.put(Id, new Noted(Anchor, level().getGameTime()));
    }

    public float renderWidth(float Partial) {
        return Scp428Size.width(girth(Partial), squeeze(Partial));
    }

    public float renderTall(float Partial) {
        return Scp428Size.tall(girth(Partial), squeeze(Partial));
    }

    public boolean isResting() {
        return isAlive() && getTarget() == null && GripVictim == null && Holds.isEmpty() && AngerLeft <= 0;
    }

    @Nullable
    public Scp428Record recordOf(@Nullable UUID RecordId) {
        if (RecordId == null) {
            return null;
        }
        for (Scp428Record Entry : Records) {
            if (Entry.id().equals(RecordId)) {
                return Entry;
            }
        }
        return null;
    }

    public void anger(LivingEntity Shooter) {
        AngerFrom = Shooter.getUUID();
        AngerLeft = AngerTicks;
        BraceLeft = 0;
        GuardSpots.clear();
        if (GripVictim == null && Scp428Quarry.isQuarry(Shooter) && level() instanceof ServerLevel World) {
            setTarget(Shooter);
        }
    }

    public double clutchRange() {
        return angry() ? AngerClutch : ClutchRange;
    }

    public double pace() {
        return angry() ? AngerPace : 1.0;
    }

    public double joinReach() {
        return angry() ? AngerJoin : Scp428Instance.JoinReach;
    }

    public boolean angry() {
        return AngerLeft > 0 && AngerFrom != null;
    }

    @Nullable
    public LivingEntity angerTarget() {
        if (!angry() || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(AngerFrom) instanceof LivingEntity Found && Found.isAlive() && Targeting.isTargetable(Found) ? Found : null;
    }

    private void angerTick(ServerLevel World) {
        if (AngerLeft <= 0) {
            return;
        }
        LivingEntity Shooter = AngerFrom == null ? null : World.getEntity(AngerFrom) instanceof LivingEntity Found ? Found : null;
        if (--AngerLeft <= 0 || Shooter == null || !Shooter.isAlive() || Shooter.isRemoved() || Shooter.level() != World || Shooter.distanceTo(this) > AngerRange
                || !Scp428Quarry.isQuarry(Shooter)) {
            LivingEntity Current = getTarget();
            if (Current != null && AngerFrom != null && Current.getUUID().equals(AngerFrom) && heldTarget(World) == null) {
                setTarget(null);
            }
            AngerFrom = null;
            AngerLeft = 0;
        }
    }

    @Nullable
    public Scp428Instance cardHolder(ServerLevel World, BlockPos Scanner) {
        Scp428Instance Best = null;
        Vec3 Near = Vec3.atCenterOf(Scanner);
        for (Scp428Record Entry : Records) {
            if (NpcDoors.pickCard(World, Scanner, Entry.carried()).isEmpty() || Entry.body() == null || !(World.getEntity(Entry.body()) instanceof Scp428Instance Body)
                    || !Body.isAlive() || Body.isHolding()) {
                continue;
            }
            if (Best == null || Body.distanceToSqr(Near) < Best.distanceToSqr(Near)) {
                Best = Body;
            }
        }
        return Best;
    }

    public boolean bracing() {
        return BraceLeft > 0;
    }

    public boolean guards(Scp428Instance Body) {
        return BraceLeft <= 0 && GuardSpots.containsKey(Body.getUUID());
    }

    @Nullable
    public LivingEntity intruder() {
        if (Intruder == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(Intruder) instanceof LivingEntity Found && Found.isAlive() ? Found : null;
    }

    public double huddleRadius() {
        return radius() + (BraceLeft > 0 ? BraceGap : HuddleGap);
    }

    public static int ringCapacity(double Radius) {
        return Math.max(1, (int) Math.floor(Math.PI * 2.0 * Radius / HuddleSpacing));
    }

    public static double[] ringPlace(int Slot, int Count, double Inner) {
        int Before = 0;
        int Ring = 0;
        double Radius = Inner;
        int Index = Mth.clamp(Slot, 0, Math.max(0, Count - 1));
        while (true) {
            int Capacity = ringCapacity(Radius);
            int Left = Count - Before;
            if (Index < Before + Capacity || Left <= Capacity) {
                int Here = Math.max(1, Math.min(Left, Capacity));
                double Shift = Ring % 2 == 1 ? 0.5 : 0.0;
                return new double[]{Radius, (Index - Before + Shift) * Math.PI * 2.0 / Here};
            }
            Before += Capacity;
            Radius += HuddleOuter;
            Ring++;
        }
    }

    public double huddleBase() {
        return (getUUID().getLeastSignificantBits() & 0xFFFFL) / 65536.0 * Math.PI * 2.0;
    }

    public double floorLevel() {
        return moveControl instanceof Scp428Hover Hover && !Double.isNaN(Hover.lastFloor()) ? Hover.lastFloor() : getY() - clearance();
    }

    @Nullable
    public static Vec3 standable(ServerLevel World, Entity Body, double X, double Z, double Base) {
        double Top = Scp428Hover.scan(World, X, Z, Base + 1.2, Base - 2.0);
        if (Double.isNaN(Top)) {
            return null;
        }
        Vec3 Spot = new Vec3(X, Top, Z);
        return World.noCollision(Body, Body.getType().getDimensions().makeBoundingBox(Spot).deflate(0.05)) ? Spot : null;
    }

    @Nullable
    public Vec3 ringPoint(ServerLevel World, Scp428Instance Body) {
        int Slot = Body.recordId() == null ? -1 : slotOf(Body.recordId());
        if (Slot < 0) {
            return null;
        }
        double[] Place = ringPlace(Slot, Math.max(Records.size(), Slot + 1), huddleRadius());
        double Angle = huddleBase() + Place[1];
        return standable(World, Body, getX() + Math.cos(Angle) * Place[0], getZ() + Math.sin(Angle) * Place[0], floorLevel());
    }

    @Nullable
    public Vec3 huddleSpot(ServerLevel World, Scp428Instance Body) {
        Vec3 Guard = BraceLeft <= 0 ? GuardSpots.get(Body.getUUID()) : null;
        return Guard != null ? Guard : ringPoint(World, Body);
    }

    private boolean intrudes(LivingEntity Candidate) {
        if (Candidate == this || Candidate instanceof Scp428Core || Candidate instanceof Scp428Instance || !Candidate.isAlive() || Scp268.isNeglected(Candidate) || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        return Candidate instanceof Player User ? !User.isSpectator() : ControlBodies.isPersonLike(Candidate);
    }

    private void guardTick(ServerLevel World) {
        GuardSpots.clear();
        Intruder = null;
        if (!isResting() || BraceLeft > 0) {
            return;
        }
        LivingEntity Found = null;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(GuardRange, 3.0, GuardRange), this::intrudes)) {
            if (Candidate.distanceTo(this) <= GuardRange && (Found == null || Candidate.distanceToSqr(this) < Found.distanceToSqr(this))) {
                Found = Candidate;
            }
        }
        if (Found == null) {
            return;
        }
        List<Scp428Instance> Live = new ArrayList<>(bodies(World).stream().filter(Body -> !Body.isHolding()).toList());
        if (Live.isEmpty()) {
            return;
        }
        LivingEntity Watched = Found;
        Live.sort(Comparator.comparingDouble(Body -> Body.distanceToSqr(Watched)));
        int Count = Live.size() >= CrowdedGuards ? 3 : Math.min(2, Live.size());
        List<Scp428Instance> Picked = new ArrayList<>(Live.subList(0, Count));
        Vec3 Toward = new Vec3(Found.getX() - getX(), 0.0, Found.getZ() - getZ());
        double Distance = Toward.length();
        Vec3 Heading = Distance < 1.0e-4 ? new Vec3(1.0, 0.0, 0.0) : Toward.scale(1.0 / Distance);
        Vec3 Across = new Vec3(-Heading.z, 0.0, Heading.x);
        double Ring = huddleRadius();
        double Out = Mth.clamp(Distance - GuardKeep, Ring, Ring + GuardStep);
        double[] Offsets = Count == 1 ? new double[]{0.0} : Count == 2 ? new double[]{-GuardSpread * 0.5, GuardSpread * 0.5} : new double[]{-GuardSpread, 0.0, GuardSpread};
        Picked.sort(Comparator.comparingDouble(Body -> Body.position().subtract(position()).dot(Across)));
        double Base = floorLevel();
        for (int Index = 0; Index < Picked.size(); Index++) {
            Scp428Instance Body = Picked.get(Index);
            Vec3 Line = position().add(Heading.scale(Out)).add(Across.scale(Offsets[Index]));
            Vec3 Spot = standable(World, Body, Line.x, Line.z, Base);
            if (Spot == null) {
                Vec3 Close = position().add(Heading.scale(Ring)).add(Across.scale(Offsets[Index]));
                Spot = standable(World, Body, Close.x, Close.z, Base);
            }
            if (Spot != null) {
                GuardSpots.put(Body.getUUID(), Spot);
            }
        }
        Intruder = Found.getUUID();
    }

    public CompoundTag gripInfo() {
        return entityData.get(GripData);
    }

    public int fleeLeft() {
        return FleeLeft;
    }

    @Nullable
    public UUID fleeFrom() {
        return FleeFrom;
    }

    public double radius() {
        return getBbWidth() * 0.5;
    }

    public double clearance() {
        return Scp428Size.clearance(girth(), squeeze());
    }

    public double bob(float Partial) {
        double Time = tickCount + Partial;
        double Phase = getId() * 0.618;
        return (BobLow + BobHigh * Scp428Size.share(girth())) * Math.sin(Time * 0.07 + Phase) + BobRipple * Math.sin(Time * 0.131 + Phase * 2.0);
    }

    public CompoundTag touchInfo() {
        return entityData.get(TouchData);
    }

    public void touched(BlockPos Pos) {
        CompoundTag Info = new CompoundTag();
        Info.putLong("P", Pos.asLong());
        Info.putLong("T", level().getGameTime());
        entityData.set(TouchData, Info);
    }

    @Override
    public void travel(Vec3 Input) {
        if (isDeadOrDying()) {
            super.travel(Input);
            return;
        }
        if (isControlledByLocalInstance()) {
            move(MoverType.SELF, getDeltaMovement());
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Size = Math.max(RenderSize, getBoundingBox().getSize()) * 64.0 * getViewScale();
        return Distance < Size * Size;
    }

    public int slotOf(UUID RecordId) {
        for (int Index = 0; Index < Records.size(); Index++) {
            if (Records.get(Index).id().equals(RecordId)) {
                return Index;
            }
        }
        return -1;
    }

    public boolean claims(UUID RecordId, UUID Body) {
        for (Scp428Record Entry : Records) {
            if (Entry.id().equals(RecordId)) {
                return Body.equals(Entry.body());
            }
        }
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Current) {
        float Girth = girth();
        float Squeeze = squeeze();
        float Tall = Scp428Size.tall(Girth, Squeeze);
        return EntityDimensions.scalable(Scp428Size.width(Girth, Squeeze), Tall).withEyeHeight(Tall * Scp428Size.EyeShare);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (GirthData.equals(Key) || SqueezeData.equals(Key)) {
            refreshDimensions();
        }
        if (LinksData.equals(Key) && level().isClientSide) {
            int[] Next = entityData.get(LinksData).getIntArray("I");
            for (int Old : ClientLinks) {
                boolean Kept = false;
                for (int Id : Next) {
                    Kept |= Id == Old;
                }
                Noted Last = LastAnchors.remove(Old);
                if (!Kept && Last != null && level().getGameTime() - Last.At() <= AnchorFresh) {
                    Retracts.add(new Retract(Last.Point(), level().getGameTime()));
                }
            }
            ClientLinks = Next;
        }
    }

    @Override
    public void tick() {
        GirthO = girth();
        SqueezeO = squeeze();
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            ClientSpeed += (Math.sqrt(Dx * Dx + Dz * Dz) - ClientSpeed) * 0.35;
            long Now = level().getGameTime();
            Retracts.removeIf(Entry -> Now - Entry.Start() > Retract.Ticks + 2);
            return;
        }
        if (isAlive() && level() instanceof ServerLevel World) {
            serverTick(World);
        }
    }

    private void serverTick(ServerLevel World) {
        if (!Seeded) {
            Seeded = true;
            Records.add(Scp428Record.of(Scp428Looks.stray(random)));
            syncCount(false);
        }
        processPending(World);
        if (tickCount % MaintainTicks == 0) {
            maintain(World, false);
        }
        growTick();
        squeezeTick(World);
        Agent.tick(this, World);
        if (FleeLeft > 0) {
            FleeLeft--;
        }
        if (BraceLeft > 0 && --BraceLeft <= 0) {
            if (BraceFrom != null && World.getEntity(BraceFrom) instanceof LivingEntity Attacker && Scp428Quarry.isQuarry(Attacker)) {
                alert(Attacker);
            }
            BraceFrom = null;
            if (GripVictim == null) {
                retarget(World);
            }
        }
        angerTick(World);
        if (GripVictim != null) {
            tickGrip(World);
        } else if (tickCount % RetargetTicks == 0 && BraceLeft <= 0) {
            retarget(World);
        }
        tickHolds(World);
        if (tickCount % GuardTicks == 0 || BraceLeft > 0) {
            guardTick(World);
        }
        if (tickCount % LinkTicks == 0) {
            syncLinks(World);
        }
        if (Recent.size() > 8) {
            long Now = World.getGameTime();
            Recent.values().removeIf(At -> Now - At > RecentTicks);
            Cooldowns.values().removeIf(Until -> Until <= Now);
            MassRegrab.values().removeIf(Until -> Until <= Now);
        }
        murmur();
        updateState();
    }

    private void growTick() {
        float Target = count();
        float Current = girth();
        if (Math.abs(Target - Current) < 1.0e-3f) {
            return;
        }
        float Next = Current < Target ? Math.min(Target, Current + GrowRate) : Math.max(Target, Current - GrowRate);
        entityData.set(GirthData, Next);
    }

    private void squeezeTick(ServerLevel World) {
        float Girth = girth();
        float FullWidth = Scp428Size.diameter(Girth);
        float FullHeight = Scp428Size.height(Girth);
        double FullClear = Scp428Size.clearance(Girth, 0.0f);
        double Floor = moveControl instanceof Scp428Hover Hover && !Double.isNaN(Hover.lastFloor()) ? Hover.lastFloor() : getY() - clearance();
        boolean Needed = false;
        boolean Bulky = FullWidth > Scp428Size.NarrowWidth + 0.05f || FullHeight + FullClear > Scp428Size.NarrowHeight;
        AABB Here = EntityDimensions.scalable(FullWidth, FullHeight).makeBoundingBox(new Vec3(getX(), Floor + FullClear, getZ()));
        if (Bulky) {
            if (!World.noBlockCollision(this, Here.deflate(0.02))) {
                Needed = true;
            } else {
                Path Route = getNavigation().getPath();
                if (Route != null && !Route.isDone()) {
                    int From = Route.getNextNodeIndex();
                    for (int Index = From; Index < Math.min(Route.getNodeCount(), From + 2); Index++) {
                        Node Step = Route.getNode(Index);
                        AABB Ahead = EntityDimensions.scalable(FullWidth, FullHeight).makeBoundingBox(Vec3.atBottomCenterOf(Step.asBlockPos()).add(0.0, FullClear, 0.0));
                        if (!World.noBlockCollision(this, Ahead.deflate(0.02))) {
                            Needed = true;
                            break;
                        }
                    }
                }
            }
        }
        float Want;
        if (Needed) {
            SqueezeLeft = SqueezeHold;
            Want = 1.0f;
        } else if (SqueezeLeft > 0) {
            SqueezeLeft--;
            Want = 1.0f;
        } else {
            Want = 0.0f;
        }
        float Current = squeeze();
        if (Math.abs(Want - Current) > 1.0e-3f) {
            entityData.set(SqueezeData, Current < Want ? Math.min(Want, Current + SqueezeRate) : Math.max(Want, Current - SqueezeRate));
        }
    }

    private void retarget(ServerLevel World) {
        LivingEntity Held = heldTarget(World);
        if (Held != null) {
            if (Held != getTarget()) {
                setTarget(Held);
            }
            return;
        }
        LivingEntity Shooter = angerTarget();
        if (Shooter != null && Scp428Quarry.isQuarry(Shooter) && !cooling(Shooter, World.getGameTime())) {
            if (Shooter != getTarget()) {
                setTarget(Shooter);
            }
            return;
        }
        double Sense = Scp428Size.sense(count());
        long Now = World.getGameTime();
        LivingEntity Current = getTarget();
        if (Current != null && (!Scp428Quarry.isQuarry(Current) || Current.level() != World || Current.distanceTo(this) > Sense * LoseShare || cooling(Current, Now))) {
            Current = null;
        }
        if (Current == null) {
            Current = Scp428Quarry.pick(World, this, Sense, Sense * HearShare);
            if (Current != null && cooling(Current, Now)) {
                Current = null;
            }
        }
        if (Current == null && Alerted != null && Now - AlertedAt < 200 && World.getEntity(Alerted) instanceof LivingEntity Reported
                && Scp428Quarry.isQuarry(Reported) && Reported.distanceTo(this) <= Sense * AlertShare && !cooling(Reported, Now)) {
            Current = Reported;
        }
        if (Current != getTarget()) {
            setTarget(Current);
        }
    }

    @Nullable
    private LivingEntity heldTarget(ServerLevel World) {
        LivingEntity Best = null;
        for (Scp428Hold Hold : Holds.values()) {
            if (World.getEntity(Hold.victim()) instanceof LivingEntity Victim && Victim.isAlive() && (Best == null || Victim.distanceToSqr(this) < Best.distanceToSqr(this))) {
                Best = Victim;
            }
        }
        return Best;
    }

    public boolean cooling(LivingEntity Candidate) {
        return cooling(Candidate, level().getGameTime());
    }

    private boolean cooling(LivingEntity Candidate, long Now) {
        Long Until = Cooldowns.get(Candidate.getUUID());
        return Until != null && Until > Now;
    }

    public void alert(LivingEntity Found) {
        Alerted = Found.getUUID();
        AlertedAt = level().getGameTime();
    }

    public boolean inGrabRange(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Dy = Target.getY() - getY();
        return Flat <= radius() + Target.getBbWidth() * 0.5 + GrabReach && Dy > -GrabRise && Dy < GrabRise + getBbHeight();
    }

    public boolean startGrip(LivingEntity Victim) {
        if (!(level() instanceof ServerLevel World) || GripVictim != null || !Scp428Quarry.isQuarry(Victim) || cooling(Victim, World.getGameTime())) {
            return false;
        }
        Long Driven = MassRegrab.get(Victim.getUUID());
        UUID Owner = Scp428Holds.coreOf(Victim.getUUID());
        if (Driven != null && Driven > World.getGameTime() || Owner != null && !Owner.equals(getUUID())) {
            return false;
        }
        holdFor(World, Victim);
        GripVictim = Victim.getUUID();
        GripStart = World.getGameTime();
        Scp428Grip.lock(Victim);
        getNavigation().stop();
        CompoundTag Info = new CompoundTag();
        Info.putInt("V", Victim.getId());
        Info.putLong("S", GripStart);
        Info.putInt("L", ReachTicks);
        entityData.set(GripData, Info);
        playLayersAt(Victim.position(), Scp428Registry.GrabLayers, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        playLayers(Scp428Registry.ReachLayers, 0.8f, 0.9f + random.nextFloat() * 0.1f);
        updateState();
        return true;
    }

    @Nullable
    public LivingEntity gripped() {
        if (GripVictim == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(GripVictim) instanceof LivingEntity Found ? Found : null;
    }

    private void tickGrip(ServerLevel World) {
        LivingEntity Victim = gripped();
        if (Victim == null || !Victim.isAlive() || !Scp428Quarry.isQuarry(Victim) || Victim.level() != World) {
            releaseGrip(false);
            return;
        }
        Vec3 Toward = new Vec3(Victim.getX() - getX(), 0.0, Victim.getZ() - getZ());
        double Gap = Toward.length();
        if (Gap > radius() + Victim.getBbWidth() * 0.5 + GripSlip) {
            releaseGrip(false);
            return;
        }
        Vec3 Heading = Gap < 1.0e-4 ? new Vec3(0.0, 0.0, 1.0) : Toward.scale(1.0 / Gap);
        Vec3 HoldPoint = position().add(Heading.scale(radius() + Victim.getBbWidth() * 0.5 + 0.05));
        Scp428Grip.lock(Victim);
        Scp428Grip.hold(Victim, HoldPoint, position());
        getNavigation().stop();
        float Yaw = (float) (Mth.atan2(Heading.z, Heading.x) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), Yaw, 10.0f));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        long Elapsed = World.getGameTime() - GripStart;
        if (Elapsed > 0 && Elapsed % CreakInterval == 0 && Elapsed < ReachTicks) {
            playLayers(Scp428Registry.ReachLayers, 0.45f, 0.85f + Elapsed / (float) ReachTicks * 0.3f);
        }
        if (Elapsed >= ReachTicks - HoverTicks && Elapsed < ReachTicks) {
            Vec3 Head = Scp428Quarry.head(Victim);
            World.sendParticles(new DustParticleOptions(FleshDust, 0.7f), Head.x, Head.y, Head.z, 2, 0.2, 0.15, 0.2, 0.0);
        }
        if (Elapsed >= ReachTicks) {
            connect(World, Victim);
        }
    }

    private void connect(ServerLevel World, LivingEntity Victim) {
        Scp428Hold Hold = Holds.get(Victim.getUUID());
        if (Hold != null) {
            endHold(World, Hold, false, false);
        }
        releaseGrip(false);
        playLayersAt(Scp428Quarry.head(Victim), Scp428Registry.LashLayers, 1.0f, 0.9f + random.nextFloat() * 0.1f);
        strike(World, Victim);
    }

    public void strike(ServerLevel World, LivingEntity Victim) {
        Cooldowns.put(Victim.getUUID(), World.getGameTime() + RegrabCooldown);
        Victim.invulnerableTime = 0;
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp428Registry.AbsorbDamage), this);
        Victim.hurt(Source, Float.MAX_VALUE);
    }

    public void releaseGrip(boolean Driven) {
        UUID Victim = GripVictim;
        if (Victim != null && Driven) {
            MassRegrab.put(Victim, level().getGameTime() + MassRegrabTicks);
        }
        GripVictim = null;
        entityData.set(GripData, new CompoundTag());
        if (Victim != null && !Holds.containsKey(Victim) && level() instanceof ServerLevel World && World.getEntity(Victim) instanceof LivingEntity Found) {
            Scp428Grip.unlock(Found);
        }
        updateState();
    }

    public void releaseAll() {
        if (level() instanceof ServerLevel World) {
            for (Scp428Hold Hold : List.copyOf(Holds.values())) {
                endHold(World, Hold, true, false);
            }
        }
        if (GripVictim != null) {
            releaseGrip(false);
        }
    }

    public int holdCount(UUID Victim) {
        Scp428Hold Hold = Holds.get(Victim);
        return Hold == null ? 0 : holderCount(Hold);
    }

    public int holdPresses(UUID Victim) {
        Scp428Hold Hold = Holds.get(Victim);
        return Hold == null ? 0 : Hold.presses();
    }

    public boolean isHolding(UUID Victim) {
        return Holds.containsKey(Victim);
    }

    public int holdTotal() {
        return Holds.size();
    }

    private int holderCount(Scp428Hold Hold) {
        return Hold.holders().size() + (Hold.victim().equals(GripVictim) ? 1 : 0);
    }

    private Scp428Hold holdFor(ServerLevel World, LivingEntity Victim) {
        Scp428Hold Hold = Holds.get(Victim.getUUID());
        if (Hold != null) {
            return Hold;
        }
        Hold = new Scp428Hold(Victim.getUUID(), World.getGameTime());
        Holds.put(Victim.getUUID(), Hold);
        Scp428Holds.register(Victim.getUUID(), getUUID());
        Scp428Grip.lock(Victim);
        return Hold;
    }

    public boolean canClutch(Scp428Instance Body, LivingEntity Victim) {
        if (!Scp428Quarry.isQuarry(Victim) || cooling(Victim, level().getGameTime()) || !Body.canGrab()) {
            return false;
        }
        UUID Owner = Scp428Holds.coreOf(Victim.getUUID());
        if (Owner != null && !Owner.equals(getUUID())) {
            return false;
        }
        Scp428Hold Hold = Holds.get(Victim.getUUID());
        return Hold == null || Hold.holders().size() < Scp428Hold.MaxHolders;
    }

    @Nullable
    public LivingEntity clutchTarget(Scp428Instance Body) {
        if (!(level() instanceof ServerLevel World)) {
            return null;
        }
        LivingEntity Best = null;
        for (Scp428Hold Hold : Holds.values()) {
            if (World.getEntity(Hold.victim()) instanceof LivingEntity Victim && Body.distanceTo(Victim) <= clutchRange() && canClutch(Body, Victim)
                    && (Best == null || Body.distanceToSqr(Victim) < Body.distanceToSqr(Best))) {
                Best = Victim;
            }
        }
        if (Best != null) {
            return Best;
        }
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive() && Body.distanceTo(Target) <= clutchRange() && canClutch(Body, Target)) {
            return Target;
        }
        return null;
    }

    public boolean join(Scp428Instance Body, LivingEntity Victim) {
        if (!(level() instanceof ServerLevel World) || !canClutch(Body, Victim)) {
            return false;
        }
        Scp428Hold Hold = holdFor(World, Victim);
        Hold.add(Body.getUUID());
        Body.grab(Victim);
        playLayersAt(Victim.position(), Scp428Registry.HoldLayers, 0.9f, 0.9f + random.nextFloat() * 0.2f);
        sync(Hold, Victim, holderCount(Hold));
        return true;
    }

    public void letGo(Scp428Instance Body, int Delay) {
        for (Scp428Hold Hold : Holds.values()) {
            if (Hold.remove(Body.getUUID())) {
                break;
            }
        }
        Body.release(Delay);
    }

    @Nullable
    public Vec3 slotPoint(Scp428Instance Body) {
        if (!(level() instanceof ServerLevel World)) {
            return null;
        }
        for (Scp428Hold Hold : Holds.values()) {
            int Slot = Hold.slotOf(Body.getUUID());
            if (Slot < 0 || !(World.getEntity(Hold.victim()) instanceof LivingEntity Victim)) {
                continue;
            }
            Vec3 Toward = new Vec3(getX() - Victim.getX(), 0.0, getZ() - Victim.getZ());
            Toward = Toward.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 0.0, 1.0) : Toward.normalize();
            double Angle = Math.toRadians(SlotAngles[Slot % SlotAngles.length]);
            Vec3 Turned = new Vec3(Toward.x * Math.cos(Angle) - Toward.z * Math.sin(Angle), 0.0, Toward.x * Math.sin(Angle) + Toward.z * Math.cos(Angle));
            return Victim.position().add(Turned.scale(SlotRadius + Victim.getBbWidth() * 0.5));
        }
        return null;
    }

    private void tickHolds(ServerLevel World) {
        if (Holds.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        for (Scp428Hold Hold : List.copyOf(Holds.values())) {
            if (!(World.getEntity(Hold.victim()) instanceof LivingEntity Victim) || !Victim.isAlive() || !Scp428Quarry.isQuarry(Victim) || Victim.level() != World
                    || Victim.distanceTo(this) > Scp428Instance.SnapRange * 1.5) {
                endHold(World, Hold, true, false);
                continue;
            }
            Hold.holders().removeIf(Id -> !(World.getEntity(Id) instanceof Scp428Instance Body) || !Body.isAlive() || Body.isRemoved() || !Body.isHolding(Hold.victim())
                    || Body.distanceTo(Victim) > HolderSlip);
            int Count = holderCount(Hold);
            if (Count <= 0) {
                endHold(World, Hold, true, false);
                continue;
            }
            Scp428Grip.lock(Victim);
            if (!Hold.victim().equals(GripVictim)) {
                drag(Victim);
            }
            if (!(Victim instanceof Player) && Hold.autoTick(AutoShortest + random.nextInt(AutoSpread))) {
                pressOnce(Hold, Victim, Now, Count);
            }
            if (Hold.escaped(Count)) {
                escape(World, Hold, Victim);
                continue;
            }
            sync(Hold, Victim, Count);
        }
    }

    private void drag(LivingEntity Victim) {
        Vec3 Toward = new Vec3(getX() - Victim.getX(), 0.0, getZ() - Victim.getZ());
        double Gap = Toward.length();
        Vec3 Point = Victim.position();
        if (Gap > radius() + Victim.getBbWidth() * 0.5 + GrabReach * 0.8) {
            Point = Point.add(Toward.scale(DragLead / Gap));
        }
        Scp428Grip.hold(Victim, Point, position());
    }

    public boolean struggle(UUID VictimId) {
        Scp428Hold Hold = Holds.get(VictimId);
        if (Hold == null || !(level() instanceof ServerLevel World) || !(World.getEntity(VictimId) instanceof LivingEntity Victim)) {
            return false;
        }
        int Count = holderCount(Hold);
        if (!pressOnce(Hold, Victim, World.getGameTime(), Count)) {
            return false;
        }
        if (Hold.escaped(Count)) {
            escape(World, Hold, Victim);
        } else {
            sync(Hold, Victim, Count);
        }
        return true;
    }

    private boolean pressOnce(Scp428Hold Hold, LivingEntity Victim, long Now, int Count) {
        if (!Hold.press(Now, Count)) {
            return false;
        }
        playLayersAt(Victim.position().add(0.0, Victim.getBbHeight() * 0.6, 0.0), Scp428Registry.StruggleLayers, 0.7f, 0.9f + random.nextFloat() * 0.3f);
        return true;
    }

    private void escape(ServerLevel World, Scp428Hold Hold, LivingEntity Victim) {
        playLayersAt(Victim.position().add(0.0, Victim.getBbHeight() * 0.6, 0.0), Scp428Registry.BreakFreeLayers, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        endHold(World, Hold, true, true);
        Vec3 Away = new Vec3(Victim.getX() - getX(), 0.0, Victim.getZ() - getZ());
        if (Away.lengthSqr() > 1.0e-4) {
            Away = Away.normalize().scale(EscapePush);
            Victim.push(Away.x, 0.25, Away.z);
            Victim.hurtMarked = true;
        }
    }

    private void endHold(ServerLevel World, Scp428Hold Hold, boolean Immune, boolean Escaped) {
        Holds.remove(Hold.victim());
        Scp428Holds.unregister(Hold.victim(), getUUID());
        for (UUID Id : Hold.holders()) {
            if (World.getEntity(Id) instanceof Scp428Instance Body) {
                Body.release(Escaped ? HolderDelay : HolderRest);
            }
        }
        Hold.holders().clear();
        if (Hold.victim().equals(GripVictim)) {
            releaseGrip(false);
        }
        if (Immune) {
            Cooldowns.put(Hold.victim(), World.getGameTime() + RegrabCooldown);
        }
        if (World.getEntity(Hold.victim()) instanceof LivingEntity Victim) {
            Scp428Grip.unlock(Victim);
            if (Immune && getTarget() == Victim) {
                setTarget(null);
            }
            if (Victim instanceof ServerPlayer Held && Held.connection != null) {
                PacketDistributor.sendToPlayer(Held, Scp428HoldPayload.Released);
            }
        }
    }

    private void sync(Scp428Hold Hold, LivingEntity Victim, int Count) {
        if (Victim instanceof ServerPlayer Held && Held.connection != null && Hold.changed(Count)) {
            PacketDistributor.sendToPlayer(Held, new Scp428HoldPayload(Count, Hold.presses(), Scp428Hold.required(Count)));
        }
    }

    public void absorbed(LivingEntity Victim) {
        long Now = level().getGameTime();
        Long Before = Recent.get(Victim.getUUID());
        if (Before != null && Now - Before < RecentTicks) {
            return;
        }
        Recent.put(Victim.getUUID(), Now);
        Absorbed++;
        Scp428Hold Hold = Holds.get(Victim.getUUID());
        if (Hold != null && level() instanceof ServerLevel World) {
            endHold(World, Hold, false, false);
        }
        if (GripVictim != null && GripVictim.equals(Victim.getUUID())) {
            releaseGrip(false);
        }
        if (getTarget() == Victim) {
            setTarget(null);
        }
        Pending.add(new PendingAbsorb(Scp428Looks.of(Victim, random), Victim.position(), Victim.getYRot(), Victim instanceof Player ? null : Victim.getUUID(), Scp428Carry.capture(Victim)));
    }

    public void adopt(Scp428Looks.Look Taken, Vec3 Spot, float Yaw) {
        adopt(Taken, Spot, Yaw, Scp428Carry.Haul.none(false));
    }

    public void adopt(Scp428Looks.Look Taken, Vec3 Spot, float Yaw, Scp428Carry.Haul Carried) {
        Pending.add(new PendingAbsorb(Taken, Spot, Yaw, null, Carried));
    }

    private void processPending(ServerLevel World) {
        if (Pending.isEmpty()) {
            return;
        }
        List<PendingAbsorb> Batch = new ArrayList<>(Pending);
        Pending.clear();
        for (PendingAbsorb Entry : Batch) {
            if (Entry.Corpse() != null && World.getEntity(Entry.Corpse()) instanceof LivingEntity Corpse && !(Corpse instanceof Player) && Corpse.isDeadOrDying()
                    && Records.size() < Scp428Size.Capacity) {
                Corpse.discard();
            }
            if (Records.size() >= Scp428Size.Capacity) {
                World.sendParticles(ParticleTypes.POOF, Entry.Spot().x, Entry.Spot().y + 0.8, Entry.Spot().z, 10, 0.3, 0.5, 0.3, 0.02);
                if (Entry.Carried().Owned()) {
                    Scp428Carry.spill(World, Entry.Spot(), Entry.Carried().Stacks());
                }
                continue;
            }
            Scp428Record Made = Scp428Record.of(Entry.Look(), Entry.Carried());
            Records.add(Made);
            spawnBody(World, Made, Entry.Spot(), Entry.Yaw(), true);
            playLayersAt(Entry.Spot(), Scp428Registry.AbsorbLayers, 1.0f, 0.9f + random.nextFloat() * 0.1f);
            World.sendParticles(new DustParticleOptions(FleshDust, 1.0f), Entry.Spot().x, Entry.Spot().y + 1.0, Entry.Spot().z, 14, 0.3, 0.6, 0.3, 0.0);
            syncCount(true);
        }
        syncLinks(World);
    }

    public void lose(UUID RecordId, Vec3 At) {
        if (Folding || !(level() instanceof ServerLevel World)) {
            return;
        }
        Iterator<Scp428Record> Each = Records.iterator();
        while (Each.hasNext()) {
            Scp428Record Entry = Each.next();
            if (Entry.id().equals(RecordId)) {
                Each.remove();
                if (Scp428Carry.dropsOnDeath(Entry, Scp428Carry.keepInventory(World))) {
                    Scp428Carry.spill(World, At, Entry.release());
                }
                syncCount(false);
                if (isAlive()) {
                    playLayers(Scp428Registry.SnapLayers, 1.0f, 0.9f + random.nextFloat() * 0.15f);
                    playLayers(Scp428Registry.HurtLayers, 0.7f, 0.8f);
                }
                syncLinks(World);
                return;
            }
        }
    }

    private void syncCount(boolean Grew) {
        int Count = Records.size();
        entityData.set(CountData, Count);
        AttributeInstance Health = getAttribute(Attributes.MAX_HEALTH);
        if (Health != null) {
            Health.setBaseValue(Scp428Size.maxHealth(Count));
        }
        AttributeInstance Speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.setBaseValue(Scp428Size.speed(Count));
        }
        AttributeInstance Armour = getAttribute(Attributes.ARMOR);
        if (Armour != null) {
            Armour.setBaseValue(Scp428Size.armour(Count));
        }
        AttributeInstance Knockback = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (Knockback != null) {
            Knockback.setBaseValue(Scp428Size.knockback(Count));
        }
        if (Grew) {
            setHealth((float) Math.min(getMaxHealth(), getHealth() + Scp428Size.HealthPerInstance));
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    public void maintain(ServerLevel World, boolean Now) {
        for (Scp428Record Entry : Records) {
            UUID Body = Entry.body();
            Entity Found = Body == null ? null : World.getEntity(Body);
            if (Found instanceof Scp428Instance Live && !Live.isRemoved()) {
                if (Live.isAlive()) {
                    Entry.found();
                    Entry.setHealth(Live.getHealth());
                }
                continue;
            }
            if (Body != null && !Now) {
                Entry.addMissing(MaintainTicks);
                if (Entry.missing() < MissingGrace) {
                    continue;
                }
            }
            spawnBody(World, Entry, nearby(World), random.nextFloat() * 360.0f, false);
        }
    }

    private Vec3 nearby(ServerLevel World) {
        for (int Attempt = 0; Attempt < 8; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = radius() + 0.8 + random.nextDouble() * SpawnSpread;
            Vec3 Spot = position().add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
            AABB Box = Scp428Registry.Instance.get().getDimensions().makeBoundingBox(Spot);
            if (World.noCollision(Box) && !World.getBlockState(BlockPos.containing(Spot).below()).isAir()) {
                return Spot;
            }
        }
        return position();
    }

    @Nullable
    private Scp428Instance spawnBody(ServerLevel World, Scp428Record Entry, Vec3 Spot, float Yaw, boolean Fresh) {
        Scp428Instance Body = Scp428Registry.Instance.get().create(World);
        if (Body == null) {
            return null;
        }
        Vec3 Place = Spot;
        if (!World.noCollision(Body.getType().getDimensions().makeBoundingBox(Place))) {
            Place = nearby(World);
        }
        Body.bind(this, Entry);
        Body.arm(Entry);
        Body.moveTo(Place.x, Place.y, Place.z, Yaw, 0.0f);
        Body.setYHeadRot(Yaw);
        Body.setYBodyRot(Yaw);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Place)), MobSpawnType.MOB_SUMMONED, null);
        if (Entry.health() > 0.0f) {
            Body.setHealth(Math.min(Body.getMaxHealth(), Entry.health()));
        }
        if (Fresh) {
            Body.rise();
        }
        if (!World.addFreshEntity(Body)) {
            return null;
        }
        Entry.setBody(Body.getUUID());
        Entry.setHealth(Body.getHealth());
        if (!Fresh) {
            World.sendParticles(new DustParticleOptions(FleshDust, 0.8f), Place.x, Place.y + 1.0, Place.z, 8, 0.25, 0.5, 0.25, 0.0);
        }
        return Body;
    }

    public void unfurl(ServerLevel World) {
        for (Scp428Record Entry : Records) {
            Entry.setBody(null);
        }
        maintain(World, true);
        syncLinks(World);
        playLayers(Scp428Registry.AbsorbLayers, 0.8f, 0.7f);
    }

    public List<Scp428Instance> bodies(ServerLevel World) {
        List<Scp428Instance> Found = new ArrayList<>();
        for (Scp428Record Entry : Records) {
            if (Entry.body() != null && World.getEntity(Entry.body()) instanceof Scp428Instance Live && Live.isAlive()) {
                Found.add(Live);
            }
        }
        return Found;
    }

    private void syncLinks(ServerLevel World) {
        List<Scp428Instance> Found = bodies(World);
        int[] Ids = new int[Found.size()];
        for (int Index = 0; Index < Ids.length; Index++) {
            Ids[Index] = Found.get(Index).getId();
        }
        CompoundTag Current = entityData.get(LinksData);
        int[] Old = Current.getIntArray("I");
        if (Arrays.equals(Old, Ids)) {
            return;
        }
        CompoundTag Next = new CompoundTag();
        Next.putIntArray("I", Ids);
        entityData.set(LinksData, Next);
    }

    private void foldBodies(boolean Effects) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Folding = true;
        try {
            for (Scp428Instance Body : bodies(World)) {
                if (Effects) {
                    World.sendParticles(new DustParticleOptions(FleshDust, 1.0f), Body.getX(), Body.getY() + 1.0, Body.getZ(), 10, 0.25, 0.5, 0.25, 0.0);
                }
                Body.sever();
                Body.discard();
            }
        } finally {
            Folding = false;
        }
    }

    private void murmur() {
        boolean Moving = (getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo) > MoveThreshold * MoveThreshold;
        if (Moving) {
            if (--SquelchReady <= 0) {
                float Size = Math.min(1.0f, 0.4f + girth() / Scp428Size.Capacity);
                playLayers(Scp428Registry.SquelchLayers, 0.35f + Size * 0.4f, 1.15f - Size * 0.35f + random.nextFloat() * 0.1f);
                SquelchReady = SquelchInterval + random.nextInt(6);
            }
        } else {
            SquelchReady = Math.min(SquelchReady, 4);
        }
        if (--PulseReady <= 0) {
            playLayers(Scp428Registry.PulseLayers, 0.4f, 0.9f + random.nextFloat() * 0.15f);
            PulseReady = PulseInterval + random.nextInt(80);
        }
    }

    private void updateState() {
        byte Next;
        if (GripVictim != null) {
            Next = StateGripping;
        } else if (FleeLeft > 0) {
            Next = StateFleeing;
        } else if (getTarget() != null) {
            Next = StateHunting;
        } else {
            Next = StateRoaming;
        }
        if (entityData.get(StateData) != Next) {
            entityData.set(StateData, Next);
        }
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + getBbHeight() * 0.5, getZ(), Layer.get(), getSoundSource(), Volume, Pitch);
        }
    }

    public void playLayersAt(Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, Where.x, Where.y, Where.z, Layer.get(), getSoundSource(), Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        Entity Attacker = Source.getEntity();
        if (!level().isClientSide && Attacker != null && Scp428Holds.isHeld(Attacker)) {
            return false;
        }
        float Scaled = Source.is(DamageTypeTags.IS_FIRE) ? Amount * FireShare : Amount;
        boolean Resting = !level().isClientSide && isResting() && BraceLeft <= 0;
        boolean Hurt = super.hurt(Source, Scaled);
        boolean Shot = Source.getDirectEntity() instanceof Projectile && Attacker instanceof Player Shooter && !Shooter.isCreative() && !Shooter.isSpectator();
        if (Hurt && !level().isClientSide && Shot && isAlive() && count() > 0) {
            anger((LivingEntity) Attacker);
        } else if (Hurt && !level().isClientSide) {
            if (Resting && isAlive() && count() > 0 && Attacker instanceof LivingEntity) {
                BraceLeft = BraceTicks;
                BraceFrom = Attacker.getUUID();
                GuardSpots.clear();
            }
        }
        if (Hurt && !level().isClientSide) {
            if (GripVictim != null && isAlive() && Attacker != null && !GripVictim.equals(Attacker.getUUID())) {
                releaseGrip(true);
                playLayers(Scp428Registry.SnapLayers, 0.9f, 1.1f);
            }
            if (count() <= 0 && isAlive() && Attacker instanceof LivingEntity) {
                FleeLeft = FleeTicks;
                FleeFrom = Attacker.getUUID();
            }
            if (isAlive()) {
                playLayers(Scp428Registry.HurtLayers, 0.8f, 0.85f + random.nextFloat() * 0.2f);
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Before = dead;
        super.die(Source);
        if (!Before && dead && level() instanceof ServerLevel World) {
            setNoGravity(false);
            releaseAll();
            boolean Keep = Scp428Carry.keepInventory(World);
            for (Scp428Record Entry : Records) {
                if (Scp428Carry.dropsOnDeath(Entry, Keep)) {
                    Vec3 At = Entry.body() != null && World.getEntity(Entry.body()) instanceof Scp428Instance Body ? Body.position() : position();
                    Scp428Carry.spill(World, At, Entry.release());
                }
            }
            Folding = true;
            try {
                for (Scp428Instance Body : bodies(World)) {
                    Body.sever();
                    Body.hurt(World.damageSources().genericKill(), Float.MAX_VALUE);
                }
            } finally {
                Folding = false;
            }
            playLayers(Scp428Registry.DeathLayers, 1.0f, 0.8f);
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (!level().isClientSide) {
            releaseAll();
            if (Reason == RemovalReason.CHANGED_DIMENSION || Reason == RemovalReason.DISCARDED) {
                foldBodies(Reason == RemovalReason.CHANGED_DIMENSION);
            }
        }
        super.remove(Reason);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide() && !isRemoved()) {
            if (level() instanceof ServerLevel World) {
                World.sendParticles(new DustParticleOptions(FleshDust, 1.2f), getX(), getY() + getBbHeight() * 0.4, getZ(), 24, getBbWidth() * 0.3, 0.2, getBbWidth() * 0.3, 0.0);
            }
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void jumpFromGround() {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setPersistenceRequired();
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp428Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp428Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
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
    public AABB getBoundingBoxForCulling() {
        AABB Own = super.getBoundingBoxForCulling().inflate(0.5);
        if (!level().isClientSide) {
            return Own;
        }
        CompoundTag Grip = gripInfo();
        if (!Grip.isEmpty() && level().getEntity(Grip.getInt("V")) instanceof LivingEntity Victim) {
            Own = Own.minmax(Victim.getBoundingBox());
        }
        CompoundTag Touch = touchInfo();
        if (!Touch.isEmpty() && level().getGameTime() - Touch.getLong("T") <= TouchMemory) {
            Own = Own.minmax(new AABB(BlockPos.of(Touch.getLong("P"))));
        }
        for (Retract Entry : Retracts) {
            Own = Own.minmax(new AABB(Entry.From(), Entry.From()));
        }
        return Own.inflate(1.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (level() instanceof ServerLevel World) {
            for (Scp428Record Entry : Records) {
                if (Entry.body() != null && World.getEntity(Entry.body()) instanceof Scp428Instance Live && Live.isAlive()) {
                    Entry.setHealth(Live.getHealth());
                }
            }
        }
        ListTag Listed = new ListTag();
        for (Scp428Record Entry : Records) {
            Listed.add(Entry.save(registryAccess()));
        }
        Saved.put(RecordsKey, Listed);
        Saved.putBoolean("Seeded", Seeded);
        Saved.putInt("Absorbed", Absorbed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Records.clear();
        ListTag Listed = Saved.getList(RecordsKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Listed.size() && Records.size() < Scp428Size.Capacity; Index++) {
            Records.add(Scp428Record.load(Listed.getCompound(Index), registryAccess()));
        }
        Seeded = Saved.getBoolean("Seeded") || !Records.isEmpty();
        Absorbed = Saved.getInt("Absorbed");
        entityData.set(CountData, Records.size());
        entityData.set(GirthData, (float) Records.size());
        float Health = getHealth();
        syncCount(false);
        setHealth(Math.min(Health, getMaxHealth()));
        GripVictim = null;
        Holds.clear();
        Pending.clear();
        AngerFrom = null;
        AngerLeft = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 5, Event -> {
            if (isDeadOrDying()) {
                Event.getController().setAnimationSpeed(1.0);
                return Event.setAndContinue(DeathPlay);
            }
            if (state() == StateGripping) {
                Event.getController().setAnimationSpeed(1.0);
                return Event.setAndContinue(GripLoop);
            }
            if (ClientSpeed > MoveThreshold) {
                Event.getController().setAnimationSpeed(Mth.clamp(ClientSpeed / CrawlReference, 0.5, 2.0));
                return Event.setAndContinue(DriftLoop);
            }
            Event.getController().setAnimationSpeed(1.0);
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public record PendingAbsorb(Scp428Looks.Look Look, Vec3 Spot, float Yaw, @Nullable UUID Corpse, Scp428Carry.Haul Carried) {
    }

    public record Noted(Vec3 Point, long At) {
    }

    public record Retract(Vec3 From, long Start) {
        public static final int Ticks = 8;
    }
}
