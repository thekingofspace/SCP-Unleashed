package net.rai.scp_unleashed.scp538;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class Scp538Spider extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.3f;
    public static final float Height = 0.125f;
    public static final float EyeHeight = 0.08f;
    public static final double MaxHealth = 4.0;
    public static final double Speed = 0.165;
    public static final double HuntModifier = 1.0;
    public static final double CrawlSpeed = 0.025;
    public static final double RestlessCrawlSpeed = 0.035;
    public static final double FleeCrawlSpeed = 0.06;
    public static final double AttackRange = 12.0;
    public static final double ChaseRange = 18.0;
    public static final double FleeRange = 10.0;
    public static final double BiteReach = 0.45;
    public static final double CloseIn = 2.0;
    public static final float BiteDamage = 1.0f;
    public static final int BiteCooldown = 30;
    public static final int TargetInterval = 10;
    public static final int ThreatInterval = 5;
    public static final int RepathInterval = 10;
    public static final int FadeTicks = 40;
    public static final int FadeRecovery = 2;
    public static final int CalmDwellMin = 160;
    public static final int CalmDwellMax = 480;
    public static final int RestlessDwellMin = 50;
    public static final int RestlessDwellMax = 140;
    public static final int ScareTicks = 80;
    public static final int AmbientInterval = 220;
    public static final double ClaimRadius = 8.0;
    public static final double WalkDistance = 0.075;
    public static final double RunDistance = 0.1125;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.045, 0.04, WalkDistance, RunDistance, 0.6, 1);
    public static final double[] Contacts = {0.0, 0.5};
    public static final float StepVolume = 0.12f;
    public static final byte Calm = 0;
    public static final byte Restless = 1;
    public static final byte Hunting = 2;
    public static final byte Fleeing = 3;
    public static final DustParticleOptions Wisp = new DustParticleOptions(new Vector3f(0.09f, 0.07f, 0.13f), 0.6f);
    public static final double Spacing = 0.7;
    public static final int SpacingCheck = 10;
    private static final EntityDataAccessor<Byte> SurfaceData = SynchedEntityData.defineId(Scp538Spider.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> MoodData = SynchedEntityData.defineId(Scp538Spider.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FadeData = SynchedEntityData.defineId(Scp538Spider.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation SkitterLoop = RawAnimation.begin().thenLoop("skitter");
    private static final RawAnimation CurlHold = RawAnimation.begin().thenPlayAndHold("curl");
    private static final RawAnimation BiteOnce = RawAnimation.begin().thenPlay("bite");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private boolean Crawling;
    @Nullable
    private Scp538Surfaces.Node Anchor;
    @Nullable
    private List<Scp538Surfaces.Node> Route;
    private int RouteIndex;
    private Scp538Surfaces.Goal RouteGoal = Scp538Surfaces.Goal.Calm;
    private int RouteLight = Scp538Light.Calming;
    private double RouteSpeed = CrawlSpeed;
    private int Dwell;
    private int BiteLeft;
    @Nullable
    private Vec3 ScareFrom;
    private int ScareLeft;
    @Nullable
    private Vec3 Threat;
    private int Bites;
    private float RenderPartial;
    @Nullable
    private Quaternionf Turn;
    @Nullable
    private Vector3f Attach;
    private double TurnClock = Double.NaN;

    public Scp538Spider(EntityType<? extends Scp538Spider> Type, Level World) {
        super(Type, World);
        lookControl = new ShadowLook(this);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, ChaseRange)
                .add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp538Navigation(this, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SurfaceData, (byte) Direction.DOWN.get3DDataValue());
        Builder.define(MoodData, Calm);
        Builder.define(FadeData, (byte) 0);
    }

    @Override
    protected void registerGoals() {
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    public Direction surface() {
        return Direction.from3DDataValue(entityData.get(SurfaceData));
    }

    public void setSurface(Direction Side) {
        if (surface() != Side) {
            entityData.set(SurfaceData, (byte) Side.get3DDataValue());
        }
    }

    public byte mood() {
        return entityData.get(MoodData);
    }

    private void setMood(byte Mood) {
        if (mood() != Mood) {
            entityData.set(MoodData, Mood);
            Route = null;
            Dwell = 0;
        }
    }

    public int fade() {
        return entityData.get(FadeData);
    }

    public float fadeShare() {
        return Mth.clamp(fade() / (float) FadeTicks, 0.0f, 1.0f);
    }

    public boolean isCrawling() {
        return Crawling;
    }

    @Nullable
    public Scp538Surfaces.Node anchor() {
        return Anchor;
    }

    @Nullable
    public List<Scp538Surfaces.Node> route() {
        return Route;
    }

    @Nullable
    public Vec3 claim() {
        if (Route != null && !Route.isEmpty()) {
            return Route.get(Route.size() - 1).Position();
        }
        return Anchor != null ? Anchor.Position() : null;
    }

    public int bites() {
        return Bites;
    }

    @Nullable
    public Vec3 threat() {
        return Threat;
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

    @Nullable
    public Quaternionf turn() {
        return Turn;
    }

    @Nullable
    public Vector3f attach() {
        return Attach;
    }

    public double turnClock() {
        return TurnClock;
    }

    public void setTurn(Quaternionf Value, Vector3f Point, double Clock) {
        Turn = Value;
        Attach = Point;
        TurnClock = Clock;
    }

    public void scare(Vec3 From, int Ticks) {
        ScareFrom = From;
        ScareLeft = Math.max(ScareLeft, Ticks);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (level() instanceof ServerLevel World && !isRemoved()) {
            Entity Cause = Source.getEntity();
            if (Cause != null) {
                scare(Cause.position(), ScareTicks);
            }
            World.sendParticles(Wisp, getX(), getY() + 0.06, getZ(), 4, 0.12, 0.03, 0.12, 0.0);
            Scp538Swarm.play(World, position(), Scp538Registry.Pass.get(), 0.4f, 1.3f + random.nextFloat() * 0.2f);
        }
        return false;
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
    protected void doPush(Entity Other) {
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
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
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        byte Mood = mood();
        return Mood == Restless || Mood == Hunting ? Scp538Registry.Hiss.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World && getAmbientSound() != null) {
            Scp538Swarm.playLayers(World, position(), Scp538Registry.HissLayers, 0.35f, 1.0f + random.nextFloat() * 0.15f);
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
        return Scp538Registry.Fade.get();
    }

    @Override
    public void travel(Vec3 Input) {
        if (Crawling) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(Input);
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
        double Dy = getY() - yo;
        double Dz = getZ() - zo;
        boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz), isAlive() && fade() == 0, Gait);
        if (!Moving || isSilent()) {
            return;
        }
        for (double Contact : Contacts) {
            if (Math.floor(Stride.to() - Contact) > Math.floor(Stride.from() - Contact) && random.nextInt(3) == 0
                    && level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 8.0)) {
                level().playLocalSound(getX(), getY(), getZ(), Scp538Registry.Skitter.get(), getSoundSource(), StepVolume * (0.8f + random.nextFloat() * 0.2f),
                        (isRunning() ? 1.15f : 1.0f) * (0.92f + random.nextFloat() * 0.16f), false);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        int Light = Scp538Light.at(this);
        if (!fadeTick(World, Light)) {
            return;
        }
        BiteLeft = Math.max(0, BiteLeft - 1);
        if (ScareLeft > 0 && --ScareLeft == 0) {
            ScareFrom = null;
        }
        if (Crawling && !World.noCollision(this)) {
            letGo();
        }
        if ((tickCount + getId()) % ThreatInterval == 0) {
            Threat = Scp538Swarm.threat(this);
            if (Threat == null && ScareLeft > 0) {
                Threat = ScareFrom;
            }
        }
        if (Threat != null) {
            setMood(Fleeing);
            setTarget(null);
            flee(World);
            return;
        }
        if (!Scp538Light.calmed(Light, Scp538Light.artificial(this))) {
            LivingEntity Prey = prey(World);
            if (Prey != null) {
                setMood(Hunting);
                hunt(World, Prey);
                return;
            }
            setMood(Restless);
            roam(World, Scp538Light.Fading, RestlessCrawlSpeed, RestlessDwellMin, RestlessDwellMax, Light);
            return;
        }
        setTarget(null);
        setMood(Calm);
        roam(World, Scp538Light.Calming, CrawlSpeed, CalmDwellMin, CalmDwellMax, Light);
    }

    private boolean fadeTick(ServerLevel World, int Light) {
        int Fade = fade();
        int Next = Scp538Light.bearable(Light) ? Math.max(0, Fade - FadeRecovery) : Fade + 1;
        if (Next != Fade) {
            entityData.set(FadeData, (byte) Math.min(Next, FadeTicks));
        }
        if (Next > 0 && Next % 8 == 1) {
            World.sendParticles(Wisp, getX(), getY() + 0.05, getZ(), 2, 0.1, 0.02, 0.1, 0.0);
        }
        if (Next >= FadeTicks) {
            dissolve(World);
            return false;
        }
        return true;
    }

    public void dissolve(ServerLevel World) {
        World.sendParticles(Wisp, getX(), getY() + 0.06, getZ(), 10, 0.14, 0.05, 0.14, 0.0);
        World.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.06, getZ(), 3, 0.08, 0.02, 0.08, 0.005);
        Scp538Swarm.playLayers(World, position(), Scp538Registry.FadeLayers, 0.45f, 1.0f + random.nextFloat() * 0.2f);
        discard();
    }

    @Nullable
    private LivingEntity prey(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null && !(Scp538Swarm.canBite(Current) && Current.distanceToSqr(this) <= ChaseRange * ChaseRange)) {
            Current = null;
            setTarget(null);
        }
        if ((tickCount + getId()) % TargetInterval != 0) {
            return Current;
        }
        LivingEntity Found = Current != null ? Current : Scp538Swarm.nearestPrey(World, this, AttackRange);
        setTarget(Found);
        return Found;
    }

    private void hunt(ServerLevel World, LivingEntity Prey) {
        if (Crawling) {
            letGo();
        }
        getLookControl().setLookAt(Prey, 30.0f, 30.0f);
        if (BiteLeft == 0 && getBoundingBox().inflate(BiteReach).intersects(Prey.getBoundingBox())) {
            bite(World, Prey);
            return;
        }
        if (distanceToSqr(Prey) < CloseIn * CloseIn && Scp538Light.bearable(Scp538Light.at(World, Prey.blockPosition()))) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(Prey.getX(), Prey.getY(), Prey.getZ(), HuntModifier);
            return;
        }
        if ((tickCount + getId()) % RepathInterval == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(Prey, HuntModifier);
        }
    }

    public boolean bite(ServerLevel World, LivingEntity Prey) {
        if (!Scp538Swarm.canBite(Prey)) {
            return false;
        }
        BiteLeft = BiteCooldown;
        Bites++;
        triggerAnim("action", "bite");
        Scp538Swarm.playLayers(World, Prey.position(), Scp538Registry.BiteLayers, 0.7f, 1.0f + random.nextFloat() * 0.1f);
        Prey.hurt(damageSources().mobAttack(this), BiteDamage);
        boolean Infected = Scp538Infection.infect(Prey);
        if (Infected) {
            Scp538Swarm.scatter(World, Prey);
        }
        setTarget(null);
        getNavigation().stop();
        return Infected;
    }

    private void flee(ServerLevel World) {
        if (!ensureCrawling(World)) {
            return;
        }
        if (Route != null && RouteGoal == Scp538Surfaces.Goal.Flee) {
            follow(World);
            return;
        }
        if (Dwell-- > 0) {
            return;
        }
        plan(World, Scp538Surfaces.Goal.Flee, Scp538Light.Fading, FleeCrawlSpeed);
        Dwell = Route == null ? 10 : 0;
    }

    private void roam(ServerLevel World, int MinLight, double Pace, int DwellMin, int DwellMax, int Light) {
        if (!ensureCrawling(World)) {
            return;
        }
        if (Route != null) {
            follow(World);
            return;
        }
        if (!Scp538Light.bearable(Light)) {
            plan(World, Scp538Surfaces.Goal.Calm, Scp538Light.Fading, FleeCrawlSpeed);
            return;
        }
        if (Dwell-- > 0 && !((tickCount + getId()) % SpacingCheck == 0 && crowded(World))) {
            return;
        }
        plan(World, Scp538Surfaces.Goal.Calm, MinLight, Pace);
        Dwell = DwellMin + random.nextInt(Math.max(1, DwellMax - DwellMin));
    }

    public boolean plan(ServerLevel World, Scp538Surfaces.Goal Goal, int MinLight, double Pace) {
        Scp538Surfaces.Search Search = new Scp538Surfaces.Search(World, this, claims(World));
        Scp538Surfaces.Node Start = start(Search);
        if (Start == null) {
            letGo();
            return false;
        }
        List<Scp538Surfaces.Node> Found = Search.plan(Start, MinLight, Goal, Goal == Scp538Surfaces.Goal.Flee ? Threat : null);
        Anchor = Start;
        if (Found.isEmpty()) {
            Route = position().distanceToSqr(Start.Position()) > 1.0e-4 ? new ArrayList<>(List.of(Start)) : null;
        } else {
            Route = new ArrayList<>(Found);
            if (position().distanceToSqr(Start.Position()) > 1.0e-4) {
                Route.add(0, Start);
            }
        }
        RouteIndex = 0;
        RouteGoal = Goal;
        RouteLight = MinLight;
        RouteSpeed = Pace;
        return !Found.isEmpty();
    }

    @Nullable
    private Scp538Surfaces.Node start(Scp538Surfaces.Search Search) {
        BlockPos Cell = Scp538Light.cellOf(this);
        if (Anchor != null && Anchor.Position().distanceToSqr(position()) < 1.0e-4) {
            Scp538Surfaces.Node Known = Search.node(Anchor.Cell(), Anchor.Side());
            if (Known != null) {
                return Known;
            }
        }
        Scp538Surfaces.Node Found = Search.node(Cell, surface());
        if (Found == null) {
            Found = Search.node(Cell, Direction.DOWN);
        }
        if (Found == null) {
            for (Direction Side : Direction.values()) {
                Found = Search.node(Cell, Side);
                if (Found != null) {
                    break;
                }
            }
        }
        return Found;
    }

    private boolean crowded(ServerLevel World) {
        return !World.getEntitiesOfClass(Scp538Spider.class, getBoundingBox().inflate(Spacing), Each -> Each != this && Each.isAlive() && Each.distanceToSqr(this) < Spacing * Spacing).isEmpty();
    }

    private List<Vec3> claims(ServerLevel World) {
        List<Vec3> Found = new ArrayList<>();
        for (Scp538Spider Other : World.getEntitiesOfClass(Scp538Spider.class, getBoundingBox().inflate(ClaimRadius), Each -> Each != this && Each.isAlive())) {
            Found.add(Other.position());
            Vec3 Claim = Other.claim();
            if (Claim != null) {
                Found.add(Claim);
            }
        }
        return Found;
    }

    private void follow(ServerLevel World) {
        if (Route == null || RouteIndex >= Route.size()) {
            Route = null;
            return;
        }
        Scp538Surfaces.Node Next = Route.get(RouteIndex);
        if (Scp538Light.at(World, Next.Cell()) < RouteLight) {
            Route = null;
            Dwell = 0;
            return;
        }
        Vec3 Here = position();
        Vec3 Delta = Next.Position().subtract(Here);
        double Length = Delta.length();
        setSurface(Next.Side());
        face(Delta, Next.Side());
        if (Length <= RouteSpeed) {
            setPos(Next.Position());
            Anchor = Next;
            RouteIndex++;
            if (RouteIndex >= Route.size()) {
                Route = null;
            }
            return;
        }
        Vec3 Step = Delta.scale(RouteSpeed / Length);
        if (!World.noCollision(this, Scp538Surfaces.box(this, Here.add(Step)))) {
            Route = null;
            Dwell = 0;
            return;
        }
        setPos(Here.add(Step));
    }

    private void face(Vec3 Delta, Direction Side) {
        double Flat = Delta.x * Delta.x + Delta.z * Delta.z;
        if (Flat > 1.0e-6) {
            float Yaw = (float) (Mth.atan2(Delta.z, Delta.x) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Yaw);
            setYBodyRot(Yaw);
            setYHeadRot(Yaw);
        }
        if (Side.getAxis().isHorizontal() && Math.abs(Delta.y) > 1.0e-4 && Flat < Delta.y * Delta.y) {
            if (Flat <= 1.0e-6) {
                float Yaw = (float) (Mth.atan2(Side.getStepZ(), Side.getStepX()) * Mth.RAD_TO_DEG) - 90.0f;
                setYRot(Yaw);
                setYBodyRot(Yaw);
                setYHeadRot(Yaw);
            }
            setXRot(Delta.y > 0.0 ? -80.0f : 80.0f);
        } else {
            setXRot(0.0f);
        }
    }

    private boolean ensureCrawling(ServerLevel World) {
        if (Crawling) {
            return true;
        }
        if (!onGround() && !isInLiquid()) {
            return false;
        }
        return attach(World);
    }

    public boolean attach(ServerLevel World) {
        Scp538Surfaces.Search Search = new Scp538Surfaces.Search(World, this, List.of());
        Scp538Surfaces.Node Found = start(Search);
        if (Found == null) {
            return false;
        }
        Crawling = true;
        setNoGravity(true);
        noPhysics = true;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setSurface(Found.Side());
        Anchor = null;
        Route = new ArrayList<>(List.of(Found));
        RouteIndex = 0;
        RouteSpeed = CrawlSpeed;
        RouteLight = Scp538Light.Fading;
        RouteGoal = Scp538Surfaces.Goal.Calm;
        Dwell = 0;
        return true;
    }

    public void letGo() {
        Crawling = false;
        setNoGravity(false);
        noPhysics = false;
        Route = null;
        Anchor = null;
        setXRot(0.0f);
        setSurface(Direction.DOWN);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Crawling", Crawling);
        Tag.putByte("Surface", entityData.get(SurfaceData));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.getBoolean("Crawling")) {
            Crawling = true;
            setNoGravity(true);
            noPhysics = true;
        }
        if (Tag.contains("Surface")) {
            entityData.set(SurfaceData, (byte) Mth.clamp(Tag.getByte("Surface"), 0, 5));
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Wisp, getX(), getY() + 0.06, getZ(), 10, 0.14, 0.05, 0.14, 0.0);
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp538GaitController(this, "body", 3, Event -> {
            if (fade() > 0) {
                return Event.setAndContinue(CurlHold);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? SkitterLoop : CrawlLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 1, Event -> PlayState.STOP).triggerableAnim("bite", BiteOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    private static final class ShadowLook extends LookControl {
        private final Scp538Spider Owner;

        private ShadowLook(Scp538Spider Owner) {
            super(Owner);
            this.Owner = Owner;
        }

        @Override
        protected boolean resetXRotOnTick() {
            return !Owner.Crawling;
        }

        @Override
        public void tick() {
            if (Owner.Crawling) {
                return;
            }
            super.tick();
        }
    }
}
