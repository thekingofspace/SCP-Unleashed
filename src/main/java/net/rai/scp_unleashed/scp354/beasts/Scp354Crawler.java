package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp354.Scp354Swim;
import org.jetbrains.annotations.Nullable;

public class Scp354Crawler extends Scp354Beast {
    public static final float Width = 2.0f;
    public static final float Height = 1.1f;
    public static final float EyeHeight = 0.7f;
    public static final float RenderScale = 2.5f;
    public static final double SwimSpeed = 0.14;
    public static final double Draft = 0.75;
    public static final double HaulReach = 2.0;
    public static final int HaulTicks = 12;
    public static final double Health = 170.0;
    public static final double Armour = 14.0;
    public static final double Toughness = 2.0;
    public static final double KnockbackResistance = 0.6;
    public static final double Speed = 0.36;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.5;
    public static final double FollowRange = 40.0;
    public static final float BiteDamage = 11.0f;
    public static final double BiteReach = 1.6;
    public static final int BiteTicks = 12;
    public static final int BiteHitTick = 6;
    public static final int BiteCooldown = 16;
    public static final int SlowTicks = 60;
    public static final int LungeTicks = 18;
    public static final int LungeLaunchTick = 7;
    public static final int LungeHitFrom = 9;
    public static final int LungeHitUntil = 14;
    public static final int LungeCooldown = 70;
    public static final double LungeMin = 4.0;
    public static final double LungeMax = 10.0;
    public static final double LungeSpeed = 1.25;
    public static final double LungeLift = 0.32;
    public static final double RunEnter = 0.2;
    public static final double RunExit = 0.15;
    public static final double IdleSeconds = 3.0;
    public static final int StepGap = 2;
    public static final byte ActionBite = 10;
    public static final byte ActionLunge = 11;
    public static final int TrailPoints = 48;
    public static final double TrailSpacing = 0.18;
    private static final EntityDataAccessor<Boolean> ClimbingData = SynchedEntityData.defineId(Scp354Crawler.class, EntityDataSerializers.BOOLEAN);
    private static final Scp354Stride[] Strides = {Scp354Gaits.CrawlerWalk, Scp354Gaits.CrawlerRun};
    private final double[] TrailX = new double[TrailPoints];
    private final double[] TrailY = new double[TrailPoints];
    private final double[] TrailZ = new double[TrailPoints];
    private int TrailCount;
    private int TrailHead;
    private int StepQuiet;
    @Nullable
    private LivingEntity StrikeTarget;
    private int BiteReady;
    private int LungeReady = 30;
    private boolean LungeLanded;

    public Scp354Crawler(EntityType<? extends Scp354Crawler> Type, Level World) {
        super(Type, World);
        xpReward = 45;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, BiteDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ClimbingData, false);
    }

    @Override
    protected void registerGoals() {
        registerBaseGoals();
        goalSelector.addGoal(3, new Scp354BeastGoals.Chase(this, ChaseModifier));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp354Swim.DryClimberNavigation(this, World);
    }

    @Override
    protected Scp354Stride[] strides() {
        return Strides;
    }

    @Override
    protected double runEnter() {
        return RunEnter;
    }

    @Override
    protected double runExit() {
        return RunExit;
    }

    @Override
    protected double idleSeconds() {
        return IdleSeconds;
    }

    @Override
    public float renderScale() {
        return RenderScale;
    }

    @Override
    public double swimSpeed() {
        return SwimSpeed;
    }

    @Override
    public double draft() {
        return Draft;
    }

    @Override
    public double haulReach() {
        return HaulReach;
    }

    @Override
    public int haulTicks() {
        return HaulTicks;
    }

    @Override
    public Scp354Voice voice() {
        return Scp354BeastRegistry.CrawlerVoice;
    }

    @Override
    protected boolean countsClimb() {
        return true;
    }

    @Nullable
    @Override
    protected String clipFor(byte Action) {
        return switch (Action) {
            case ActionBite -> "bite";
            case ActionLunge -> "lunge";
            default -> null;
        };
    }

    @Nullable
    @Override
    protected String loopClip() {
        return isClimbing() && !onGround() ? "climb" : null;
    }

    @Override
    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionBite || Current == ActionLunge;
    }

    public boolean isClimbing() {
        return entityData.get(ClimbingData);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState State, Vec3 Multiplier) {
        if (!State.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(State, Multiplier);
        }
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    public boolean canBite() {
        return BiteReady <= 0 && canAct();
    }

    public boolean canLunge() {
        return LungeReady <= 0 && canAct() && onGround();
    }

    @Override
    public void engage(LivingEntity Target) {
        if (!canAct() || !hasLineOfSight(Target)) {
            return;
        }
        double Reach = reachTo(Target);
        if (canBite() && Reach <= BiteReach && Math.abs(Target.getY() - getY()) < 2.5) {
            beginBite(Target);
        } else if (canLunge() && Reach >= LungeMin && Reach <= LungeMax && Math.abs(Target.getY() - getY()) < 3.0 && random.nextInt(4) == 0
                && !Scp354Swim.crossesPool(level(), position(), Target.position())) {
            beginLunge(Target);
        }
    }

    public void beginBite(LivingEntity Target) {
        StrikeTarget = Target;
        BiteReady = BiteTicks + BiteCooldown;
        setAction(ActionBite, BiteTicks);
    }

    public void beginLunge(LivingEntity Target) {
        StrikeTarget = Target;
        LungeReady = LungeTicks + LungeCooldown;
        LungeLanded = false;
        faceToward(Target, 180.0f);
        setAction(ActionLunge, LungeTicks);
        playLayers(voice().Idle(), 1.4f, 1.2f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            recordTrail();
            return;
        }
        BiteReady = Math.max(0, BiteReady - 1);
        LungeReady = Math.max(0, LungeReady - 1);
        boolean Climbing = horizontalCollision && !locksMovement();
        if (entityData.get(ClimbingData) != Climbing) {
            entityData.set(ClimbingData, Climbing);
        }
        if (Climbing && tickCount % 14 == 0) {
            playLayers(Scp354BeastRegistry.CrawlerScrape, 0.8f, 1.0f);
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Target != null && Target.isAlive()) {
            getLookControl().setLookAt(Target, 40.0f, 40.0f);
            if (Kind == ActionBite || Elapsed < LungeLaunchTick) {
                faceToward(Target, 30.0f);
            }
        }
        if (Kind == ActionBite) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == BiteHitTick) {
                biteHit(Target, BiteReach + 0.4);
            }
            return;
        }
        if (Elapsed < LungeLaunchTick) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        } else if (Elapsed == LungeLaunchTick) {
            launch(Target);
        } else if (Elapsed >= LungeHitFrom && Elapsed <= LungeHitUntil && !LungeLanded && Target != null && reachTo(Target) <= BiteReach + 0.6) {
            LungeLanded = true;
            biteHit(Target, BiteReach + 0.8);
        }
    }

    private void launch(@Nullable LivingEntity Target) {
        Vec3 Toward = Target != null ? new Vec3(Target.getX() - getX(), 0.0, Target.getZ() - getZ()) : Vec3.directionFromRotation(0.0f, getYRot());
        double Distance = Toward.length();
        if (Distance < 1.0e-3) {
            return;
        }
        double Push = Math.min(LungeSpeed, 0.35 + Distance * 0.11);
        Vec3 Velocity = Toward.scale(Push / Distance);
        setDeltaMovement(Velocity.x, LungeLift, Velocity.z);
        hasImpulse = true;
        playLayers(Scp354BeastRegistry.CrawlerLunge, 1.4f, 1.0f);
    }

    private void biteHit(@Nullable LivingEntity Target, double Reach) {
        if (Target == null || !Target.isAlive() || reachTo(Target) > Reach || !hasLineOfSight(Target)) {
            return;
        }
        if (strike(Target, BiteDamage, 0.4, 0.1)) {
            playLayers(Scp354BeastRegistry.CrawlerBite, 1.2f, 1.0f);
            Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SlowTicks, 1), this);
        }
    }

    @Override
    protected void footfall(int Limb, boolean Running) {
        if (StepQuiet > tickCount) {
            return;
        }
        StepQuiet = tickCount + StepGap;
        super.footfall(Limb, Running);
    }

    private void recordTrail() {
        if (TrailCount == 0) {
            Vec3 Back = Vec3.directionFromRotation(0.0f, yBodyRot).scale(-1.0);
            for (int Index = TrailPoints - 1; Index >= 0; Index--) {
                pushTrail(getX() + Back.x * TrailSpacing * Index, getY(), getZ() + Back.z * TrailSpacing * Index);
            }
            return;
        }
        int Last = (TrailHead - 1 + TrailPoints) % TrailPoints;
        double Dx = getX() - TrailX[Last];
        double Dy = getY() - TrailY[Last];
        double Dz = getZ() - TrailZ[Last];
        if (Dx * Dx + Dy * Dy + Dz * Dz >= TrailSpacing * TrailSpacing) {
            pushTrail(getX(), getY(), getZ());
        }
    }

    private void pushTrail(double X, double Y, double Z) {
        TrailX[TrailHead] = X;
        TrailY[TrailHead] = Y;
        TrailZ[TrailHead] = Z;
        TrailHead = (TrailHead + 1) % TrailPoints;
        TrailCount = Math.min(TrailPoints, TrailCount + 1);
    }

    public Vec3 trailPoint(Vec3 Head, double Behind) {
        double Left = Behind;
        double Px = Head.x;
        double Py = Head.y;
        double Pz = Head.z;
        for (int Step = 1; Step <= TrailCount; Step++) {
            int Slot = (TrailHead - Step + TrailPoints * 2) % TrailPoints;
            double Dx = TrailX[Slot] - Px;
            double Dy = TrailY[Slot] - Py;
            double Dz = TrailZ[Slot] - Pz;
            double Length = Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
            if (Length >= Left && Length > 1.0e-6) {
                double Share = Left / Length;
                return new Vec3(Px + Dx * Share, Py + Dy * Share, Pz + Dz * Share);
            }
            Left -= Length;
            Px = TrailX[Slot];
            Py = TrailY[Slot];
            Pz = TrailZ[Slot];
        }
        Vec3 Back = Vec3.directionFromRotation(0.0f, yBodyRot).scale(-Left);
        return new Vec3(Px + Back.x, Py, Pz + Back.z);
    }

    public static float headingYaw(Vec3 Forward) {
        return (float) (Mth.atan2(Forward.z, Forward.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static float headingPitch(Vec3 Forward) {
        double Flat = Math.sqrt(Forward.x * Forward.x + Forward.z * Forward.z);
        return (float) (Mth.atan2(Forward.y, Flat) * Mth.RAD_TO_DEG);
    }
}
