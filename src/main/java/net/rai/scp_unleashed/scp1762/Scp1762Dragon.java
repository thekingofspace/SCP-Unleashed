package net.rai.scp_unleashed.scp1762;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scp1762Dragon extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final byte Fly = 0;
    public static final byte Glide = 1;
    public static final byte Walk = 2;
    public static final byte Idle = 3;
    public static final byte Look = 4;
    public static final byte Sit = 5;
    public static final byte Run = 6;
    public static final byte Swoop = 7;
    public static final byte Climb = 8;
    public static final byte NoAction = 0;
    public static final byte Land = 1;
    public static final byte Takeoff = 2;
    public static final int Soar = 1;
    public static final int Chase = 2;
    public static final int Perch = 3;
    public static final int Wander = 4;
    public static final int Visit = 5;
    public static final int Rest = 6;
    public static final int Startled = 7;
    public static final int Home = 8;
    public static final int Dive = 9;
    public static final double FlySpeed = 0.15;
    public static final double FlyAccel = 0.02;
    public static final double HomeSpeed = 0.4;
    public static final double CrawlSpeed = 0.034;
    public static final double StrollSpeed = 0.02;
    public static final double FleeSpeed = 0.07;
    public static final double HomeCrawl = 0.06;
    public static final double Drag = 0.94;
    public static final double ClimbSpeed = 0.12;
    public static final float ClimbPitch = -80.0f;
    public static final double SnapDepth = 0.6;
    public static final double OrbitMin = 3.0;
    public static final double OrbitMax = 8.0;
    public static final double OrbitLow = 1.5;
    public static final double OrbitHigh = 5.0;
    public static final double OrbitLead = 0.45;
    public static final double OrbitBob = 0.25;
    public static final double SwoopHeight = 0.9;
    public static final double VisitGap = 1.4;
    public static final double VisitRange = 12.0;
    public static final double LandHeight = 0.45;
    public static final int LandTicks = 12;
    public static final int LandTouch = 7;
    public static final int LandLead = 2;
    public static final int TakeoffTicks = 8;
    public static final int TakeoffPush = 2;
    public static final int TakeoffLead = 2;
    public static final double TakeoffImpulse = 0.22;
    public static final int CheckInterval = 20;
    public static final int HomeTimeout = 260;
    public static final int FlapMin = 30;
    public static final int FlapMax = 70;
    private static final EntityDataAccessor<Integer> TintData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ScriptData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ScriptStartData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ScriptYawData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ScriptSideData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<BlockPos> BoxData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp1762Dragon.class, EntityDataSerializers.INT);
    private static final Map<String, RawAnimation> Loops = new HashMap<>();
    private static final Map<String, RawAnimation> Holds = new HashMap<>();

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    public final Scp1762Kind Kind;
    private int Session;
    private int Plan = Rest;
    private int PlanTicks = 10;
    private boolean Airborne;
    private boolean Settled;
    private Vec3 Target = Vec3.ZERO;
    private double OrbitAngle;
    private double OrbitRadius;
    private double OrbitRadiusGoal;
    private double OrbitHeight;
    private double OrbitHeightGoal;
    private double OrbitSpeed = FlySpeed;
    private double OrbitSign = 1.0;
    private int OrbitShift;
    private int Partner = -1;
    private int Watched = -1;
    private int PoofIn = -1;
    private int HomeAge;
    private double HomeAngle;
    private double HomeRadius;
    private double HomeHeight;
    private Vec3 HopPoint = Vec3.ZERO;
    private float HopYaw;
    private float HopSide;
    private double LandFrom;
    private int GlideHold;
    private int SoundClock = FlapMin;
    private float Roll;
    private float RollOld;
    private int TickOrigin;
    private float RenderPartial;
    private int SeenStart = Integer.MIN_VALUE;
    private boolean HomeQueued;

    public Scp1762Dragon(EntityType<? extends Scp1762Dragon> Type, Level World, Scp1762Kind Kind) {
        super(Type, World);
        this.Kind = Kind;
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FLYING_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(TintData, Scp1762Palette.White);
        Builder.define(ModeData, Idle);
        Builder.define(ScriptData, Scp1762Script.None);
        Builder.define(ScriptStartData, 0);
        Builder.define(ScriptYawData, 0.0f);
        Builder.define(ScriptSideData, 0.0f);
        Builder.define(BoxData, BlockPos.ZERO);
        Builder.define(ActionData, NoAction);
        Builder.define(ActionStartData, 0);
    }

    @Override
    protected void registerGoals() {
    }

    public void bind(BlockPos Box, int Code, int Tint) {
        entityData.set(BoxData, Box.immutable());
        Session = Code;
        entityData.set(TintData, Tint);
    }

    @Nullable
    public BlockPos boxPos() {
        BlockPos Found = entityData.get(BoxData);
        return Found.equals(BlockPos.ZERO) ? null : Found;
    }

    public int session() {
        return Session;
    }

    public int tint() {
        return entityData.get(TintData);
    }

    public void setTint(int Value) {
        entityData.set(TintData, Value & 0xFFFFFF);
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    public byte script() {
        return entityData.get(ScriptData);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public int plan() {
        return Plan;
    }

    public boolean airborne() {
        return Airborne;
    }

    public boolean scripted() {
        return script() != Scp1762Script.None;
    }

    public boolean climbing() {
        return mode() == Climb;
    }

    private void setMode(byte Value) {
        if (entityData.get(ModeData) != Value) {
            entityData.set(ModeData, Value);
        }
    }

    public Vec3 centre() {
        BlockPos Box = boxPos();
        return Box != null ? Vec3.atBottomCenterOf(Box) : position();
    }

    @Nullable
    private Scp1762BoxBlockEntity box() {
        BlockPos Box = boxPos();
        if (Box == null || !level().isLoaded(Box)) {
            return null;
        }
        return level().getBlockEntity(Box) instanceof Scp1762BoxBlockEntity Found ? Found : null;
    }

    public int serverTick() {
        return tickCount + TickOrigin;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public static Vec3 heading(float Yaw) {
        double Radians = Math.toRadians(Yaw);
        return new Vec3(-Math.sin(Radians), 0.0, Math.cos(Radians));
    }

    public Vec3 scriptPosition(double Seconds) {
        byte Current = script();
        Vec3 Ahead = heading(entityData.get(ScriptYawData));
        Vec3 Across = new Vec3(-Ahead.z, 0.0, Ahead.x);
        Vec3 Sample = Scp1762Script.sample(Kind, Current, Seconds);
        double Base = Current == Scp1762Script.Return && Kind == Scp1762Kind.Drake ? Scp1762Script.Outside : 0.0;
        return centre().add(Across.scale(entityData.get(ScriptSideData))).add(Ahead.scale(Sample.x - Base)).add(0.0, Sample.y, 0.0);
    }

    public double scriptSeconds(float Partial) {
        int Lead = script() == Scp1762Script.Return ? Scp1762Script.ReturnLead : 0;
        return Math.max(0.0, serverTick() - entityData.get(ScriptStartData) - Lead + Partial) / 20.0;
    }

    public double actionSeconds(float Partial) {
        int Lead = action() == Land ? LandLead : TakeoffLead;
        return Math.max(0.0, serverTick() - entityData.get(ActionStartData) - Lead + Partial) / 20.0;
    }

    public double clipTicks() {
        float Partial = level().isClientSide ? RenderPartial - 1.0f : 0.0f;
        return (scripted() ? scriptSeconds(Partial) : actionSeconds(Partial)) * 20.0;
    }

    public void beginScript(byte Script, float Yaw, float Side) {
        entityData.set(ScriptData, Script);
        entityData.set(ScriptYawData, Yaw);
        entityData.set(ScriptSideData, Side);
        entityData.set(ScriptStartData, serverTick());
        entityData.set(ActionData, NoAction);
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        Vec3 Start = scriptPosition(0.0);
        moveTo(Start.x, Start.y, Start.z, Yaw, 0.0f);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
        setMode(Kind.Flies ? Fly : Run);
    }

    public void emerge(RandomSource Random, Direction Facing) {
        float Yaw;
        float Side;
        if (Kind == Scp1762Kind.Drake) {
            Direction Out = Random.nextBoolean() ? Facing : Facing.getOpposite();
            Yaw = Out.toYRot();
            Side = (Random.nextFloat() - 0.5f) * 0.5f;
        } else {
            Yaw = Random.nextFloat() * 360.0f;
            Side = (Random.nextFloat() - 0.5f) * 0.3f;
        }
        beginScript(Scp1762Script.Emerge, Yaw, Side);
    }

    private void scriptTick() {
        Vec3 Here = scriptPosition(scriptSeconds(0.0f));
        float Yaw = entityData.get(ScriptYawData);
        setPos(Here.x, Here.y, Here.z);
        setYRot(Yaw);
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
        setXRot(0.0f);
        xRotO = 0.0f;
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            return;
        }
        if (serverTick() - entityData.get(ScriptStartData) >= Scp1762Script.ticks(Kind, script())) {
            finishScript();
        }
    }

    private void finishScript() {
        byte Done = script();
        entityData.set(ScriptData, Scp1762Script.None);
        if (Done == Scp1762Script.Return) {
            Scp1762BoxBlockEntity Box = box();
            if (Box != null && level() instanceof ServerLevel World) {
                Box.arrived(World, this);
            }
            discard();
            return;
        }
        noPhysics = false;
        Vec3 Ahead = heading(entityData.get(ScriptYawData));
        if (HomeQueued) {
            HomeQueued = false;
            Airborne = Kind.Flies;
            freeSpace();
            comeHome();
            return;
        }
        if (Kind.Flies) {
            Airborne = true;
            setDeltaMovement(Ahead.scale(0.032).add(0.0, 0.07, 0.0));
            beginOrbit(getRandom());
        } else {
            Airborne = false;
            freeSpace();
            Plan = Wander;
            Target = groundSpot(getRandom());
            PlanTicks = 40 + getRandom().nextInt(40);
            setMode(Run);
        }
        setNoGravity(Airborne);
    }

    private void freeSpace() {
        for (int Step = 0; Step < 8 && !level().noCollision(this); Step++) {
            setPos(getX(), getY() + 0.15, getZ());
        }
    }

    public void comeHome() {
        if (script() == Scp1762Script.Emerge) {
            HomeQueued = true;
            return;
        }
        Plan = Home;
        HomeAge = 0;
        Settled = false;
        entityData.set(ActionData, NoAction);
        Vec3 Offset = position().subtract(centre());
        if (Kind == Scp1762Kind.Drake) {
            BlockPos Pos = boxPos();
            BlockState State = level().getBlockState(Pos != null ? Pos : blockPosition());
            Direction Facing = State.getBlock() instanceof Scp1762BoxBlock ? State.getValue(Scp1762BoxBlock.Facing) : Direction.NORTH;
            Vec3 Normal = Vec3.atLowerCornerOf(Facing.getNormal());
            Direction Side = Offset.dot(Normal) >= 0.0 ? Facing : Facing.getOpposite();
            Vec3 Out = Vec3.atLowerCornerOf(Side.getNormal());
            Vec3 Along = new Vec3(-Out.z, 0.0, Out.x);
            double Lateral = Mth.clamp(Offset.dot(Along), -0.25, 0.25);
            HopYaw = Side.getOpposite().toYRot();
            Vec3 Inward = heading(HopYaw);
            Vec3 Across = new Vec3(-Inward.z, 0.0, Inward.x);
            HopSide = (float) (Lateral * Along.dot(Across));
            HopPoint = centre().add(Out.scale(Scp1762Script.Outside)).add(Across.scale(HopSide));
            Airborne = false;
            noPhysics = false;
            setNoGravity(false);
        } else {
            Airborne = true;
            noPhysics = true;
            setNoGravity(true);
            HomeAngle = Math.atan2(Offset.z, Offset.x);
            HomeRadius = Math.max(0.6, Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z));
            HomeHeight = getY() - centre().y;
            layers(Scp1762Registry.FlapLayers, 0.4f, 1.2f);
        }
    }

    public boolean homing() {
        return Plan == Home || script() == Scp1762Script.Return;
    }

    public void poof(int Delay) {
        PoofIn = Math.max(0, Delay);
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float Yaw, float Pitch, int Steps) {
        if (scripted()) {
            return;
        }
        super.lerpTo(X, Y, Z, Yaw, Pitch, Steps);
    }

    @Override
    public void tick() {
        super.tick();
        if (scripted()) {
            scriptTick();
            if (level().isClientSide || isRemoved()) {
                return;
            }
        }
        if (level().isClientSide) {
            RollOld = Roll;
            float Turn = Mth.wrapDegrees(getYRot() - yRotO);
            float Want = flyingPose() ? Mth.clamp(-Turn * 3.0f, -32.0f, 32.0f) : 0.0f;
            Roll += (Want - Roll) * 0.2f;
            return;
        }
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (PoofIn >= 0) {
            if (PoofIn-- == 0) {
                vanishInSmoke(World);
                return;
            }
        }
        if ((tickCount + getId()) % CheckInterval == 0 || tickCount == 2) {
            Scp1762BoxBlockEntity Box = box();
            BlockPos Pos = boxPos();
            if (Pos == null || Box == null && level().isLoaded(Pos) || Box != null && !Box.owns(getUUID(), Session)) {
                World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), getX(), getY() + 0.1, getZ(), 3, 0.1, 0.1, 0.1, 0.03);
                discard();
                return;
            }
        }
        if (!scripted()) {
            think(World);
        }
        sounds();
    }

    private boolean flyingPose() {
        byte Current = mode();
        return Current == Fly || Current == Glide || Current == Swoop;
    }

    private void think(ServerLevel World) {
        RandomSource Random = getRandom();
        if (action() != NoAction) {
            actionTick();
            return;
        }
        if (Plan == Home) {
            if (Kind == Scp1762Kind.Drake) {
                hopHome();
            } else {
                homeTick();
            }
            return;
        }
        if (--PlanTicks <= 0) {
            choose(World, Random);
        }
        switch (Plan) {
            case Soar -> orbit();
            case Dive -> {
                Target = centre().add(0.0, SwoopHeight, 0.0);
                if (horizontalTo(Target) < 1.0 || PlanTicks < 5) {
                    beginOrbit(Random);
                }
            }
            case Chase -> {
                Entity Lead = level().getEntity(Partner);
                if (Lead instanceof Scp1762Dragon Other && Other.isAlive() && Other.airborne() && !Other.scripted()) {
                    Vec3 Motion = Other.getDeltaMovement();
                    Vec3 Behind = Motion.lengthSqr() > 1.0e-4 ? Motion.normalize().scale(-1.2) : Vec3.ZERO;
                    Target = Other.position().add(Behind).add(0.0, 0.2, 0.0);
                } else {
                    beginOrbit(Random);
                }
            }
            case Visit -> {
                Entity Seen = level().getEntity(Watched);
                if (Seen instanceof Player Viewer && Viewer.isAlive() && !Viewer.isSpectator()) {
                    if (!Settled) {
                        Vec3 Away = position().subtract(Viewer.position()).multiply(1.0, 0.0, 1.0);
                        Vec3 Spot = Viewer.position().add(Away.lengthSqr() > 1.0e-4 ? Away.normalize().scale(VisitGap) : new Vec3(VisitGap, 0.0, 0.0));
                        Target = leash(new Vec3(Spot.x, Viewer.getY(), Spot.z));
                    }
                } else {
                    PlanTicks = 0;
                }
            }
            default -> {
            }
        }
        steer();
    }

    private double horizontalTo(Vec3 Point) {
        double Dx = Point.x - getX();
        double Dz = Point.z - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private void startAction(byte Doing) {
        entityData.set(ActionData, Doing);
        entityData.set(ActionStartData, serverTick());
    }

    private void actionTick() {
        int Age = serverTick() - entityData.get(ActionStartData);
        if (action() == Land) {
            int Step = Mth.clamp(Age - LandLead, 0, LandTouch);
            double Want = LandFrom - LandHeight * Step / LandTouch;
            setDeltaMovement(Vec3.ZERO);
            move(MoverType.SELF, new Vec3(0.0, Want - getY(), 0.0));
            setMode(Idle);
            if (Age >= LandTicks + LandLead) {
                entityData.set(ActionData, NoAction);
                Airborne = false;
                Settled = true;
                setNoGravity(false);
            }
            return;
        }
        if (action() == Takeoff) {
            if (Age == TakeoffLead + TakeoffPush) {
                Airborne = true;
                setNoGravity(true);
                setDeltaMovement(getDeltaMovement().add(0.0, TakeoffImpulse, 0.0));
                layers(Scp1762Registry.FlapLayers, 0.4f, 1.1f + getRandom().nextFloat() * 0.2f);
            }
            if (Age >= TakeoffTicks + TakeoffLead) {
                entityData.set(ActionData, NoAction);
            }
            setMode(Fly);
        }
    }

    private void land() {
        if (Kind == Scp1762Kind.Wyrm) {
            Airborne = false;
            Settled = true;
            setNoGravity(false);
            return;
        }
        LandFrom = getY();
        Settled = true;
        setDeltaMovement(Vec3.ZERO);
        startAction(Land);
    }

    private void takeOff() {
        if (!Kind.Flies) {
            return;
        }
        Settled = false;
        if (Kind == Scp1762Kind.Wyrm || !onGround()) {
            Airborne = true;
            setNoGravity(true);
            setDeltaMovement(getDeltaMovement().add(0.0, 0.2, 0.0));
            return;
        }
        startAction(Takeoff);
    }

    private void beginOrbit(RandomSource Random) {
        Plan = Soar;
        Vec3 Offset = position().subtract(centre());
        OrbitAngle = Math.atan2(Offset.z, Offset.x);
        OrbitRadius = Math.max(0.5, Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z));
        OrbitHeight = Mth.clamp(Offset.y, 0.5, OrbitHigh);
        OrbitRadiusGoal = OrbitMin + Random.nextDouble() * (OrbitMax - OrbitMin);
        OrbitHeightGoal = OrbitLow + Random.nextDouble() * (OrbitHigh - OrbitLow);
        OrbitSpeed = FlySpeed * (0.75 + Random.nextDouble() * 0.35);
        OrbitSign = Math.floorMod(getId() + Session, 2) == 0 ? 1.0 : -1.0;
        OrbitShift = 120 + Random.nextInt(120);
        PlanTicks = 240 + Random.nextInt(260);
        if (!Airborne) {
            takeOff();
        }
    }

    private void orbit() {
        RandomSource Random = getRandom();
        if (--OrbitShift <= 0) {
            OrbitRadiusGoal = OrbitMin + Random.nextDouble() * (OrbitMax - OrbitMin);
            OrbitHeightGoal = OrbitLow + Random.nextDouble() * (OrbitHigh - OrbitLow);
            OrbitShift = 120 + Random.nextInt(120);
        }
        OrbitRadius += (OrbitRadiusGoal - OrbitRadius) * 0.015;
        OrbitHeight += (OrbitHeightGoal - OrbitHeight) * 0.015;
        OrbitAngle += OrbitSign * OrbitSpeed / Math.max(1.0, OrbitRadius);
        double Ahead = OrbitAngle + OrbitSign * OrbitLead;
        Vec3 Centre = centre();
        double Bob = OrbitBob * Math.sin(tickCount * 0.07 + getId());
        Target = new Vec3(Centre.x + Math.cos(Ahead) * OrbitRadius, Centre.y + OrbitHeight + Bob, Centre.z + Math.sin(Ahead) * OrbitRadius);
    }

    private void choose(ServerLevel World, RandomSource Random) {
        int Roll0 = Random.nextInt(100);
        Settled = false;
        Player Viewer = viewer(World);
        if (!Kind.Flies) {
            if (Viewer != null && Roll0 < 10) {
                Plan = Visit;
                Watched = Viewer.getId();
                PlanTicks = 80 + Random.nextInt(60);
                return;
            }
            if (Roll0 < 35) {
                Vec3 Spot = perchSpot(World, Random, 6.0);
                if (Spot != null) {
                    Plan = Perch;
                    Target = Spot;
                    PlanTicks = 120 + Random.nextInt(100);
                    return;
                }
            }
            if (Roll0 < 55) {
                Plan = Rest;
                PlanTicks = 40 + Random.nextInt(60);
                return;
            }
            Plan = Wander;
            Target = groundSpot(Random);
            PlanTicks = 60 + Random.nextInt(60);
            return;
        }
        if (Kind == Scp1762Kind.Wyrm && Roll0 < 22) {
            Plan = Wander;
            Airborne = false;
            setNoGravity(false);
            Target = groundSpot(Random);
            PlanTicks = 80 + Random.nextInt(80);
            return;
        }
        if (Viewer != null && Roll0 < 27 && Kind != Scp1762Kind.Wyrm) {
            Plan = Visit;
            Watched = Viewer.getId();
            PlanTicks = 140 + Random.nextInt(80);
            if (!Airborne) {
                takeOff();
            }
            return;
        }
        if (Kind != Scp1762Kind.Wyrm && Roll0 < 35) {
            Vec3 Spot = perchSpot(World, Random, Scp1762BoxBlockEntity.Radius - 1.5);
            if (Spot != null) {
                Plan = Perch;
                Target = Spot;
                PlanTicks = 160 + Random.nextInt(120);
                if (!Airborne) {
                    takeOff();
                }
                return;
            }
        }
        if (Roll0 < 45 && Airborne) {
            Scp1762Dragon Lead = partner(World);
            if (Lead != null) {
                Plan = Chase;
                Partner = Lead.getId();
                PlanTicks = 60 + Random.nextInt(60);
                return;
            }
        }
        if (Roll0 < 52 && Airborne) {
            Plan = Dive;
            PlanTicks = 80;
            return;
        }
        beginOrbit(Random);
    }

    @Nullable
    private Player viewer(ServerLevel World) {
        Vec3 Centre = centre();
        Player Best = null;
        double Closest = VisitRange * VisitRange;
        for (Player Each : World.players()) {
            if (Each.isSpectator() || !Each.isAlive()) {
                continue;
            }
            double Distance = Each.distanceToSqr(Centre);
            if (Distance < Closest) {
                Closest = Distance;
                Best = Each;
            }
        }
        return Best;
    }

    @Nullable
    private Scp1762Dragon partner(ServerLevel World) {
        List<Scp1762Dragon> Near = World.getEntitiesOfClass(Scp1762Dragon.class, getBoundingBox().inflate(8.0),
                Other -> Other != this && Other.isAlive() && Other.airborne() && Other.Session == Session && !Other.homing() && !Other.scripted());
        return Near.isEmpty() ? null : Near.get(getRandom().nextInt(Near.size()));
    }

    private Vec3 groundSpot(RandomSource Random) {
        Vec3 Centre = centre();
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = 1.2 + Random.nextDouble() * (Scp1762BoxBlockEntity.Radius - 2.2);
        return new Vec3(Centre.x + Math.cos(Angle) * Reach, getY(), Centre.z + Math.sin(Angle) * Reach);
    }

    @Nullable
    private Vec3 perchSpot(ServerLevel World, RandomSource Random, double Reach) {
        BlockPos Pos = boxPos();
        BlockPos Base = Pos != null ? Pos : blockPosition();
        for (int Attempt = 0; Attempt < 10; Attempt++) {
            int X = Base.getX() + Mth.floor((Random.nextDouble() * 2.0 - 1.0) * Reach);
            int Z = Base.getZ() + Mth.floor((Random.nextDouble() * 2.0 - 1.0) * Reach);
            if (Math.abs(X - Base.getX()) <= 1 && Math.abs(Z - Base.getZ()) <= 1) {
                continue;
            }
            for (int Y = Base.getY() + (int) Scp1762BoxBlockEntity.Ceiling - 1; Y >= Base.getY() - 1; Y--) {
                BlockPos Cell = new BlockPos(X, Y, Z);
                BlockState Below = World.getBlockState(Cell.below());
                if (World.getBlockState(Cell).getCollisionShape(World, Cell).isEmpty() && World.getBlockState(Cell.above()).getCollisionShape(World, Cell.above()).isEmpty()
                        && !Below.getCollisionShape(World, Cell.below()).isEmpty()) {
                    if (Y > Base.getY()) {
                        double Top = Cell.getY() - 1 + Below.getCollisionShape(World, Cell.below()).max(Direction.Axis.Y);
                        return new Vec3(X + 0.5, Top, Z + 0.5);
                    }
                    break;
                }
            }
        }
        return null;
    }

    private Vec3 leash(Vec3 Point) {
        Vec3 Centre = centre();
        double Dx = Point.x - Centre.x;
        double Dz = Point.z - Centre.z;
        double Reach = Math.sqrt(Dx * Dx + Dz * Dz);
        double Limit = Scp1762BoxBlockEntity.Radius - 0.6;
        if (Reach > Limit) {
            Dx *= Limit / Reach;
            Dz *= Limit / Reach;
        }
        double Y = Mth.clamp(Point.y, Centre.y - Scp1762BoxBlockEntity.Floor + 0.2, Centre.y + Scp1762BoxBlockEntity.Ceiling - 0.4);
        return new Vec3(Centre.x + Dx, Y, Centre.z + Dz);
    }

    private void steer() {
        Vec3 Motion = getDeltaMovement();
        if (Airborne) {
            fly(Motion);
            return;
        }
        boolean Resting = Plan == Rest || (Plan == Perch || Plan == Visit) && Settled;
        if (Resting) {
            setXRot(Mth.approachDegrees(getXRot(), 0.0f, 12.0f));
            setDeltaMovement(Motion.multiply(0.5, 1.0, 0.5));
            if (Plan == Visit) {
                Entity Seen = level().getEntity(Watched);
                if (Seen != null) {
                    face((float) (Mth.atan2(Seen.getZ() - getZ(), Seen.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f, 20.0f);
                }
                setMode(Look);
            } else {
                setMode(Kind == Scp1762Kind.Drake && Plan == Perch ? Sit : Idle);
                if (getRandom().nextInt(60) == 0) {
                    face(getYRot() + (getRandom().nextFloat() - 0.5f) * 90.0f, 30.0f);
                }
            }
            return;
        }
        double Speed = Plan == Startled ? FleeSpeed : Plan == Wander && Kind == Scp1762Kind.Drake ? CrawlSpeed : StrollSpeed;
        if (Plan == Perch || Plan == Visit) {
            Speed = Kind == Scp1762Kind.Drake ? CrawlSpeed : StrollSpeed * 1.5;
        }
        walkTo(leash(Target), Speed, Plan == Perch || Plan == Visit);
    }

    private void walkTo(Vec3 Goal, double Speed, boolean Settles) {
        Vec3 Motion = getDeltaMovement();
        Vec3 Want = Goal.subtract(position());
        double Flat = Math.sqrt(Want.x * Want.x + Want.z * Want.z);
        if (Flat < 0.25) {
            if (Plan == Wander) {
                PlanTicks = Math.min(PlanTicks, 10);
            }
            if (Settles) {
                Settled = true;
            }
            setDeltaMovement(Motion.multiply(0.4, 1.0, 0.4));
            setXRot(Mth.approachDegrees(getXRot(), 0.0f, 20.0f));
            setMode(Idle);
            return;
        }
        double Vx = Want.x / Flat * Speed;
        double Vz = Want.z / Flat * Speed;
        double Vy = Motion.y;
        boolean Room = getY() + 0.6 < centre().y + Scp1762BoxBlockEntity.Ceiling;
        if (horizontalCollision && Kind == Scp1762Kind.Drake && Room) {
            Vy = ClimbSpeed;
            setMode(Climb);
            setXRot(Mth.approachDegrees(getXRot(), ClimbPitch, 20.0f));
        } else {
            if (horizontalCollision && onGround()) {
                if (Kind.Flies && Kind != Scp1762Kind.Wyrm && Plan != Wander) {
                    takeOff();
                } else if (Kind != Scp1762Kind.Drake) {
                    Vy = 0.36;
                } else {
                    PlanTicks = 0;
                }
            }
            setMode(Kind == Scp1762Kind.Drake && Speed > 0.025 ? Run : Walk);
            setXRot(Mth.approachDegrees(getXRot(), 0.0f, 20.0f));
        }
        setDeltaMovement(Vx, Vy, Vz);
        face((float) (Mth.atan2(Vz, Vx) * Mth.RAD_TO_DEG) - 90.0f, 18.0f);
    }

    private void fly(Vec3 Motion) {
        Vec3 Goal = leash(Target);
        if ((Plan == Perch || Plan == Visit) && Kind != Scp1762Kind.Wyrm) {
            Goal = Goal.add(0.0, LandHeight, 0.0);
            if (horizontalTo(Goal) < 0.2 && Math.abs(getY() - Goal.y) < 0.15) {
                setPos(Goal.x, Goal.y, Goal.z);
                land();
                return;
            }
        } else if ((Plan == Perch || Plan == Visit) && horizontalTo(Goal) < 0.4) {
            land();
            return;
        }
        Vec3 Want = Goal.subtract(position());
        double Distance = Want.length();
        double Speed = Plan == Startled ? FlySpeed * 1.4 : Plan == Soar ? OrbitSpeed * 1.25 : FlySpeed;
        Vec3 Desired = Distance > 1.0e-4 ? Want.scale(Math.min(Speed, Distance * 0.25 + 0.01) / Distance) : Vec3.ZERO;
        Vec3 Steer = Desired.subtract(Motion);
        double Length = Steer.length();
        if (Length > FlyAccel) {
            Steer = Steer.scale(FlyAccel / Length);
        }
        Motion = Motion.add(Steer);
        if (horizontalCollision) {
            Motion = Motion.add(0.0, 0.06, 0.0);
        }
        setDeltaMovement(Motion);
        double Flat = Math.sqrt(Motion.x * Motion.x + Motion.z * Motion.z);
        if (Flat > 0.01) {
            face((float) (Mth.atan2(Motion.z, Motion.x) * Mth.RAD_TO_DEG) - 90.0f, 14.0f);
        }
        setXRot(Mth.clamp((float) (-Mth.atan2(Motion.y, Math.max(0.02, Flat)) * Mth.RAD_TO_DEG) * 0.6f, -30.0f, 30.0f));
        if (Kind == Scp1762Kind.Wyrm) {
            setMode(Fly);
            return;
        }
        if (Plan == Dive) {
            setMode(Swoop);
            return;
        }
        boolean Level = Motion.y < 0.006 && Flat > 0.09;
        GlideHold = Level ? Math.min(GlideHold + 1, 40) : Math.max(GlideHold - 3, 0);
        setMode(GlideHold > 12 ? Glide : Fly);
    }

    private void face(float Yaw, float Rate) {
        float Turned = Mth.approachDegrees(getYRot(), Yaw, Rate);
        setYRot(Turned);
        yBodyRot = Turned;
        yHeadRot = Turned;
    }

    private void homeTick() {
        HomeAge++;
        HomeAngle += 0.2;
        HomeRadius = Math.max(0.0, HomeRadius * 0.93 - 0.03);
        HomeHeight += (Scp1762Script.DiveHeight - HomeHeight) * 0.12;
        Vec3 Centre = centre();
        Vec3 Next = new Vec3(Centre.x + Math.cos(HomeAngle) * HomeRadius, Centre.y + HomeHeight, Centre.z + Math.sin(HomeAngle) * HomeRadius);
        Vec3 Step = Next.subtract(position());
        if (Step.length() > HomeSpeed) {
            Step = Step.normalize().scale(HomeSpeed);
        }
        setDeltaMovement(Step);
        if (Step.horizontalDistanceSqr() > 1.0e-5) {
            face((float) (Mth.atan2(Step.z, Step.x) * Mth.RAD_TO_DEG) - 90.0f, 30.0f);
        }
        setXRot(10.0f);
        setMode(Fly);
        Vec3 Entry = Centre.add(0.0, Scp1762Script.DiveHeight, 0.0);
        if (HomeRadius < 0.12 && position().distanceTo(Entry) < 0.15 || HomeAge > HomeTimeout) {
            beginScript(Scp1762Script.Return, getYRot(), 0.0f);
        }
    }

    private void hopHome() {
        HomeAge++;
        Vec3 Level0 = new Vec3(HopPoint.x, getY(), HopPoint.z);
        double Gap = horizontalTo(HopPoint);
        if (HomeAge > HomeTimeout) {
            moveTo(HopPoint.x, HopPoint.y, HopPoint.z, HopYaw, 0.0f);
            beginScript(Scp1762Script.Return, HopYaw, HopSide);
            return;
        }
        if (Gap < 0.04 && onGround() && Math.abs(Mth.wrapDegrees(getYRot() - HopYaw)) < 6.0f) {
            setPos(HopPoint.x, HopPoint.y, HopPoint.z);
            beginScript(Scp1762Script.Return, HopYaw, HopSide);
            return;
        }
        if (Gap < 0.6) {
            Vec3 Step = Level0.subtract(position());
            double Pace = Math.min(HomeCrawl * 0.5, Gap);
            Vec3 Move = Gap > 1.0e-4 ? Step.scale(Pace / Gap) : Vec3.ZERO;
            setDeltaMovement(Move.x, getDeltaMovement().y, Move.z);
            face(HopYaw, 15.0f);
            setMode(Gap > 0.05 ? Walk : Idle);
            return;
        }
        walkTo(Level0, HomeCrawl, false);
    }

    private void vanishInSmoke(ServerLevel World) {
        Scp1762BoxBlockEntity.smoke(World, position().add(0.0, getBbHeight() * 0.5, 0.0), 0.3);
        World.playSound(null, getX(), getY(), getZ(), Scp1762Registry.Puff.get(), SoundSource.NEUTRAL, 0.6f, 1.0f + getRandom().nextFloat() * 0.3f);
        Scp1762BoxBlockEntity Box = box();
        if (Box != null) {
            Box.forget(getUUID());
        }
        discard();
    }

    @Override
    public void travel(Vec3 Input) {
        if (!isControlledByLocalInstance() || scripted() || action() == Land) {
            calculateEntityAnimation(false);
            return;
        }
        Vec3 Motion = getDeltaMovement();
        if (Plan == Home && Kind != Scp1762Kind.Drake) {
            noPhysics = true;
            move(MoverType.SELF, Motion);
        } else if (Airborne) {
            setNoGravity(true);
            move(MoverType.SELF, Motion);
            setDeltaMovement(getDeltaMovement().scale(Drag));
        } else {
            setNoGravity(false);
            boolean Grounded = onGround();
            double Vy = Motion.y - getGravity();
            if (isInWater()) {
                Vy = Math.max(Vy, 0.04);
            }
            move(MoverType.SELF, new Vec3(Motion.x, Vy, Motion.z));
            if (Grounded && !onGround() && Vy <= 0.0 && !climbing()) {
                snap();
            }
            Vec3 After = getDeltaMovement();
            setDeltaMovement(After.x * 0.7, onGround() ? 0.0 : Math.max(-0.6, After.y * 0.98), After.z * 0.7);
        }
        if (!level().isClientSide && boxPos() != null && Plan != Home) {
            confine();
        }
        calculateEntityAnimation(false);
    }

    private void snap() {
        double Gap = groundGap();
        if (Gap > 0.0 && Gap <= SnapDepth) {
            move(MoverType.SELF, new Vec3(0.0, -Gap - 0.01, 0.0));
        }
    }

    public double groundGap() {
        AABB Feet = getBoundingBox();
        for (double Depth = 0.0; Depth <= 2.0; Depth += 0.025) {
            if (!level().noCollision(this, Feet.move(0.0, -Depth - 0.01, 0.0))) {
                return Depth;
            }
        }
        return 2.0;
    }

    private void confine() {
        Vec3 Centre = centre();
        double Dx = getX() - Centre.x;
        double Dz = getZ() - Centre.z;
        double Reach = Math.sqrt(Dx * Dx + Dz * Dz);
        double Limit = Scp1762BoxBlockEntity.Radius;
        double Y = Mth.clamp(getY(), Centre.y - Scp1762BoxBlockEntity.Floor, Centre.y + Scp1762BoxBlockEntity.Ceiling);
        if (Reach > Limit || Y != getY()) {
            double Scale = Reach > Limit ? (Limit - 0.05) / Reach : 1.0;
            setPos(Centre.x + Dx * Scale, Y, Centre.z + Dz * Scale);
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Reach > Limit ? -Motion.x * 0.3 : Motion.x, Y != getY() ? 0.0 : Motion.y, Reach > Limit ? -Motion.z * 0.3 : Motion.z);
            if (Plan == Wander || Plan == Startled) {
                PlanTicks = 0;
            }
        }
    }

    private void startle(@Nullable Vec3 From) {
        if (!(level() instanceof ServerLevel World) || homing() || PoofIn >= 0 || scripted() || action() != NoAction) {
            return;
        }
        Vec3 Source = From != null ? From : position().add(getRandom().nextGaussian(), 0.0, getRandom().nextGaussian());
        Vec3 Away = position().subtract(Source).multiply(1.0, 0.0, 1.0);
        Away = Away.lengthSqr() > 1.0e-4 ? Away.normalize() : new Vec3(1.0, 0.0, 0.0);
        Plan = Startled;
        PlanTicks = 30;
        Settled = false;
        Target = position().add(Away.scale(4.0)).add(0.0, Kind.Flies ? 1.6 : 0.0, 0.0);
        if (Kind.Flies && !Airborne) {
            takeOff();
        }
        setDeltaMovement(getDeltaMovement().add(Away.scale(Kind.Flies ? 0.12 : 0.08)).add(0.0, Kind.Flies && Airborne ? 0.1 : 0.0, 0.0));
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), getX(), getY() + getBbHeight() * 0.5, getZ(), 3, 0.1, 0.1, 0.1, 0.04);
        layers(Scp1762Registry.StartleLayers, 0.6f, 1.0f + getRandom().nextFloat() * 0.3f);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypes.GENERIC_KILL) || Source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(Source, Amount);
        }
        if (!level().isClientSide && (Source.getEntity() != null || Source.getDirectEntity() != null || Source.getSourcePosition() != null)) {
            Entity Attacker = Source.getEntity() != null ? Source.getEntity() : Source.getDirectEntity();
            startle(Attacker != null ? Attacker.position() : Source.getSourcePosition());
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypes.GENERIC_KILL) && !Source.is(DamageTypes.FELL_OUT_OF_WORLD) || super.isInvulnerableTo(Source);
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Held.getItem() instanceof DyeItem Dye) {
            int Colour = Dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
            if (tint() == Colour) {
                return InteractionResult.PASS;
            }
            if (!level().isClientSide) {
                setTint(Colour);
                Held.consume(1, User);
                playSound(Scp1762Registry.Rustle.get(), 0.6f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(User, Hand);
    }

    private void sounds() {
        if (--SoundClock > 0) {
            return;
        }
        RandomSource Random = getRandom();
        SoundClock = FlapMin + Random.nextInt(FlapMax - FlapMin + 1);
        byte Current = mode();
        if (Current == Fly && Kind != Scp1762Kind.Wyrm) {
            layers(Scp1762Registry.FlapLayers, 0.25f, 1.1f + Random.nextFloat() * 0.3f);
        } else if (Kind == Scp1762Kind.Wyrm && (Current == Walk || Current == Fly)) {
            playSound(Scp1762Registry.Slither.get(), 0.3f, 1.1f + Random.nextFloat() * 0.3f);
        } else if (Current == Run || Current == Walk || Current == Climb) {
            playSound(Scp1762Registry.Scurry.get(), 0.3f, 1.2f + Random.nextFloat() * 0.3f);
        }
    }

    private void layers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public void playAmbientSound() {
        if (getRandom().nextInt(3) == 0) {
            layers(Scp1762Registry.RoarLayers, 0.35f, 1.0f + getRandom().nextFloat() * 0.2f);
        } else {
            playSound(Scp1762Registry.Chirp.get(), 0.4f, 1.0f + getRandom().nextFloat() * 0.3f);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp1762Registry.Puff.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Dy, boolean Grounded, BlockState State, BlockPos Pos) {
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
    protected void doPush(Entity Other) {
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
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
        return super.getBoundingBoxForCulling().inflate(0.6);
    }

    public float roll(float Partial) {
        return Mth.lerp(Partial, RollOld, Roll);
    }

    public double animSpeed() {
        if (holdingClip()) {
            return 1.0;
        }
        byte Current = mode();
        double Dx = getX() - xo;
        double Dy = getY() - yo;
        double Dz = getZ() - zo;
        if (Current == Fly) {
            return Mth.clamp(0.9 + Dy * 6.0, 0.8, 1.4);
        }
        if (Current != Walk && Current != Run && Current != Climb) {
            return 1.0;
        }
        double Speed = Current == Climb ? Math.abs(Dy) : Math.sqrt(Dx * Dx + Dz * Dz);
        double Clip = Current == Walk ? Kind.WalkClip : Kind.RunClip;
        return Mth.clamp(Speed / Clip, 0.6, 2.6);
    }

    public boolean holdingClip() {
        return scripted() || action() != NoAction;
    }

    public String clip() {
        byte Script = script();
        if (Script == Scp1762Script.Emerge) {
            return "emerge";
        }
        if (Script == Scp1762Script.Return) {
            return "return";
        }
        byte Doing = action();
        if (Doing == Land) {
            return "land";
        }
        if (Doing == Takeoff) {
            return "takeoff";
        }
        return switch (mode()) {
            case Fly -> Kind == Scp1762Kind.Drake ? "run" : "fly";
            case Glide -> Kind == Scp1762Kind.Drake ? "run" : Kind == Scp1762Kind.Wyrm ? "fly" : "glide";
            case Swoop -> Kind == Scp1762Kind.Wyvern || Kind == Scp1762Kind.Dragon ? "swoop" : "fly";
            case Walk -> Kind.walkClip();
            case Run, Climb -> Kind == Scp1762Kind.Drake ? "run" : Kind.walkClip();
            case Look -> "look_up";
            case Sit -> Kind == Scp1762Kind.Drake ? "sit" : "idle";
            default -> "idle";
        };
    }

    public boolean restartClip() {
        int Start = holdingClip() ? scripted() ? entityData.get(ScriptStartData) : entityData.get(ActionStartData) : Integer.MIN_VALUE;
        if (Start != SeenStart) {
            SeenStart = Start;
            return Start != Integer.MIN_VALUE;
        }
        return false;
    }

    public int transition() {
        if (script() == Scp1762Script.Emerge) {
            return 0;
        }
        if (script() == Scp1762Script.Return) {
            return Scp1762Script.ReturnLead;
        }
        if (action() == Land) {
            return LandLead;
        }
        if (action() == Takeoff) {
            return TakeoffLead;
        }
        return 5;
    }

    public static RawAnimation loop(String Name) {
        synchronized (Loops) {
            return Loops.computeIfAbsent(Name, Key -> RawAnimation.begin().thenLoop(Key));
        }
    }

    public static RawAnimation hold(String Name) {
        synchronized (Holds) {
            return Holds.computeIfAbsent(Name, Key -> RawAnimation.begin().thenPlayAndHold(Key));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp1762Controller(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount + TickOrigin);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        BlockPos Box = boxPos();
        if (Box != null) {
            Saved.put("Box", NbtUtils.writeBlockPos(Box));
        }
        Saved.putInt("Session", Session);
        Saved.putInt("Tint", tint());
        Saved.putBoolean("Airborne", Airborne);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        NbtUtils.readBlockPos(Saved, "Box").ifPresent(Found -> entityData.set(BoxData, Found));
        Session = Saved.getInt("Session");
        if (Saved.contains("Tint")) {
            setTint(Saved.getInt("Tint"));
        }
        Airborne = Saved.getBoolean("Airborne") && Kind.Flies;
        entityData.set(ScriptData, Scp1762Script.None);
        entityData.set(ActionData, NoAction);
        noPhysics = false;
        Plan = Rest;
        PlanTicks = 10;
        setNoGravity(Airborne);
    }
}
