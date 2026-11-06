package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class Scp354Tentacle extends Scp354Beast {
    public static final float Width = 1.3f;
    public static final float Height = 6.0f;
    public static final float EyeHeight = 5.2f;
    public static final double Health = 200.0;
    public static final double Armour = 8.0;
    public static final double Toughness = 3.0;
    public static final double FollowRange = 32.0;
    public static final double AcquireRange = 20.0;
    public static final double AcquireHeight = 8.0;
    public static final double Reach = 5.0;
    public static final double ReachLow = 1.5;
    public static final double ReachHigh = 6.5;
    public static final double SweepDamage = 9.0;
    public static final float SweepKnock = 1.8f;
    public static final double SweepLift = 0.35;
    public static final double SweepArc = 0.26;
    public static final double SlamDamage = 18.0;
    public static final double SlamSplash = 2.2;
    public static final double SlamSplashDamage = 9.0;
    public static final float SlamKnock = 1.0f;
    public static final double SlamLift = 0.5;
    public static final int SweepTicks = 24;
    public static final int SweepHitTick = 12;
    public static final int SweepCooldown = 50;
    public static final int SlamTicks = 30;
    public static final int SlamHitTick = 17;
    public static final int SlamCooldown = 80;
    public static final int SinkTicks = 40;
    public static final int RiseTicks = 36;
    public static final int EmergeTicks = 50;
    public static final int RerootDelay = 30;
    public static final int RerootCooldown = 160;
    public static final double RerootStep = 6.0;
    public static final double RerootChase = 24.0;
    public static final int IdleRerootMin = 1200;
    public static final int IdleRerootMax = 2400;
    public static final double IdleStepMin = 3.0;
    public static final double IdleStepMax = 6.0;
    public static final double AnchorDepth = 0.35;
    public static final int AmbientInterval = 200;
    public static final byte ActionSweep = 2;
    public static final byte ActionSlam = 3;
    public static final byte ActionSink = 4;
    public static final byte ActionRise = 5;
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation SweepPlay = RawAnimation.begin().thenPlayAndHold("sweep");
    private static final RawAnimation SlamPlay = RawAnimation.begin().thenPlayAndHold("slam");
    private static final RawAnimation SinkPlay = RawAnimation.begin().thenPlayAndHold("sink");
    private static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    private static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    @Nullable
    private LivingEntity StrikeTarget;
    @Nullable
    private Vec3 Destination;
    private int SweepReady;
    private int SlamReady;
    private int RerootReady;
    private int Away;
    private int IdleReroot = IdleRerootMin;

    public Scp354Tentacle(EntityType<? extends Scp354Tentacle> Type, Level World) {
        super(Type, World);
        xpReward = 35;
        IdleReroot = IdleRerootMin + random.nextInt(IdleRerootMax - IdleRerootMin);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.ATTACK_DAMAGE, SlamDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp354Prey.HoldStill(this));
        targetSelector.addGoal(1, new Scp354Prey.Retaliate(this));
        targetSelector.addGoal(2, new Scp354Prey.Hunt(this, AcquireRange, AcquireHeight));
    }

    @Override
    public int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionEmerge -> "emerge";
            case ActionSweep -> "sweep";
            case ActionSlam -> "slam";
            case ActionSink -> "sink";
            case ActionRise -> "rise";
            default -> null;
        };
    }

    @Override
    @Nullable
    protected Scp354Gait.Shape gaitShape() {
        return null;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> deathLayers() {
        return Scp354Registry.TentacleDeathLayers;
    }

    @Override
    public boolean locksMovement() {
        return true;
    }

    @Override
    public boolean locksYaw() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionSink || Current == ActionRise;
    }

    public boolean isBurrowed() {
        byte Current = action();
        return Current == ActionSink || Current == ActionRise;
    }

    public boolean canSweep() {
        return SweepReady <= 0 && !isActing();
    }

    public boolean canSlam() {
        return SlamReady <= 0 && !isActing();
    }

    public boolean inReach(LivingEntity Target) {
        double Rise = Target.getY() - getY();
        return reachTo(Target) <= Reach && Rise > -ReachLow && Rise < ReachHigh;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        anchor();
        SweepReady = Math.max(0, SweepReady - 1);
        SlamReady = Math.max(0, SlamReady - 1);
        RerootReady = Math.max(0, RerootReady - 1);
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive() && !isActing()) {
            turnToward(Target, 8.0f);
            if (inReach(Target) && hasLineOfSight(Target)) {
                Away = 0;
                strike(Target);
            } else if (distanceTo(Target) < RerootChase) {
                Away++;
                if (Away > RerootDelay && RerootReady <= 0) {
                    Away = 0;
                    reroot(Target.position(), RerootStep);
                }
            }
        } else if (Target == null && !isActing() && --IdleReroot <= 0) {
            IdleReroot = IdleRerootMin + random.nextInt(IdleRerootMax - IdleRerootMin);
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Step = IdleStepMin + random.nextDouble() * (IdleStepMax - IdleStepMin);
            reroot(position().add(Math.cos(Angle) * Step, 0.0, Math.sin(Angle) * Step), Step);
        }
    }

    private void strike(LivingEntity Target) {
        int Crowd = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Reach, 2.0, Reach), Found -> hittable(Found) && inReach(Found)).size();
        if (canSlam() && (Crowd <= 1 || random.nextInt(3) == 0)) {
            StrikeTarget = Target;
            SlamReady = SlamCooldown + random.nextInt(20);
            SweepReady = Math.max(SweepReady, 16);
            setAction(ActionSlam, SlamTicks);
        } else if (canSweep()) {
            StrikeTarget = Target;
            SweepReady = SweepCooldown + random.nextInt(20);
            setAction(ActionSweep, SweepTicks);
        }
    }

    private void reroot(Vec3 Goal, double Step) {
        Vec3 Found = findRoot(Goal, Step);
        RerootReady = RerootCooldown;
        if (Found == null) {
            return;
        }
        Destination = Found;
        setAction(ActionSink, SinkTicks);
        if (level() instanceof ServerLevel World) {
            Scp354Sounds.play(World, position(), Scp354Registry.BurrowLayers, 1.4f, 0.8f + random.nextFloat() * 0.1f);
        }
    }

    @Nullable
    private Vec3 findRoot(Vec3 Goal, double Step) {
        Vec3 Flat = new Vec3(Goal.x - getX(), 0.0, Goal.z - getZ());
        double Length = Flat.length();
        if (Length < 1.0e-3) {
            Flat = new Vec3(1.0, 0.0, 0.0);
            Length = 1.0;
        }
        double Base = Math.atan2(Flat.z, Flat.x);
        double Far = Math.min(Step, Length);
        for (int Attempt = 0; Attempt < 18; Attempt++) {
            double Angle = Base + (random.nextDouble() - 0.5) * (Attempt < 9 ? 0.6 : 1.6);
            double Distance = Math.max(2.0, Far - (Attempt % 3) * 1.0);
            int X = Mth.floor(getX() + Math.cos(Angle) * Distance);
            int Z = Mth.floor(getZ() + Math.sin(Angle) * Distance);
            for (int Dy = 4; Dy >= -4; Dy--) {
                BlockPos Cell = new BlockPos(X, getBlockY() + Dy, Z);
                Vec3 Spot = rootAt(Cell);
                if (Spot != null) {
                    return Spot;
                }
            }
        }
        return null;
    }

    @Nullable
    private Vec3 rootAt(BlockPos Cell) {
        Level World = level();
        if (!World.isLoaded(Cell)) {
            return null;
        }
        if (Scp354Fluid.isRed(World.getFluidState(Cell)) && World.getFluidState(Cell.above()).isEmpty()) {
            Vec3 Spot = new Vec3(Cell.getX() + 0.5, Cell.getY() + Scp354Fluid.SurfaceHeight - AnchorDepth, Cell.getZ() + 0.5);
            return World.noCollision(this, getDimensions(getPose()).makeBoundingBox(Spot)) ? Spot : null;
        }
        BlockState Below = World.getBlockState(Cell.below());
        if (!World.getBlockState(Cell).isAir() || !Below.isFaceSturdy(World, Cell.below(), Direction.UP) || !Below.getFluidState().isEmpty()) {
            return null;
        }
        Vec3 Spot = new Vec3(Cell.getX() + 0.5, Cell.getY(), Cell.getZ() + 0.5);
        return World.noCollision(this, getDimensions(getPose()).makeBoundingBox(Spot)) ? Spot : null;
    }

    private void anchor() {
        BlockPos Feet = blockPosition();
        boolean Wet = Scp354Fluid.isRed(level().getFluidState(Feet)) || Scp354Fluid.isRed(level().getFluidState(Feet.below()))
                && getY() - Feet.getY() < 0.2;
        if (!Wet) {
            setNoGravity(false);
            return;
        }
        BlockPos Cursor = Scp354Fluid.isRed(level().getFluidState(Feet)) ? Feet : Feet.below();
        while (Scp354Fluid.isRed(level().getFluidState(Cursor.above())) && Cursor.getY() < level().getMaxBuildHeight()) {
            Cursor = Cursor.above();
        }
        double Y = Cursor.getY() + Scp354Fluid.SurfaceHeight - AnchorDepth;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        if (Math.abs(getY() - Y) > 1.0e-3) {
            setPos(getX(), Y, getZ());
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (Kind == ActionEmerge) {
            if (Elapsed == 20 && level() instanceof ServerLevel World) {
                Scp354Sounds.play(World, position(), Scp354Registry.TentacleIdleLayers, 2.0f, 0.7f);
            }
            return;
        }
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionSweep) {
            if (Target != null && Target.isAlive() && Elapsed < SweepHitTick - 4) {
                turnToward(Target, 12.0f);
            }
            if (Elapsed == SweepHitTick - 5) {
                Scp354Sounds.play(this, Scp354Registry.SweepLayers.subList(1, 2), 1.5f, 0.7f + random.nextFloat() * 0.1f);
            }
            if (Elapsed == SweepHitTick) {
                sweepHit();
            }
        } else if (Kind == ActionSlam) {
            if (Target != null && Target.isAlive() && Elapsed < SlamHitTick - 3) {
                turnToward(Target, 15.0f);
            }
            if (Elapsed == SlamHitTick - 6) {
                Scp354Sounds.play(this, Scp354Registry.SlamLayers.subList(0, 1), 1.5f, 0.75f + random.nextFloat() * 0.1f);
            }
            if (Elapsed == SlamHitTick) {
                slamHit(Target);
            }
        } else if (Kind == ActionSink && level() instanceof ServerLevel World && Elapsed % 5 == 0) {
            dust(World, position(), 6);
        } else if (Kind == ActionRise && level() instanceof ServerLevel World && Elapsed % 5 == 0 && Elapsed < 20) {
            dust(World, position(), 6);
        }
    }

    @Override
    protected void actionEnded(byte Kind) {
        if (Kind == ActionSink && Destination != null) {
            Vec3 Spot = Destination;
            Destination = null;
            moveTo(Spot.x, Spot.y, Spot.z, getYRot(), 0.0f);
            setAction(ActionRise, RiseTicks);
            if (level() instanceof ServerLevel World) {
                Scp354Sounds.play(World, Spot, Scp354Registry.BurrowLayers, 1.4f, 0.75f + random.nextFloat() * 0.1f);
                dust(World, Spot, 16);
            }
        }
    }

    private void dust(ServerLevel World, Vec3 At, int Count) {
        BlockPos Under = BlockPos.containing(At.x, At.y - 0.2, At.z);
        if (Scp354Fluid.isRed(World.getFluidState(BlockPos.containing(At)))) {
            World.sendParticles(Scp354Registry.Splash.get(), At.x, At.y + 0.4, At.z, Count, 0.4, 0.05, 0.4, 0.15);
            return;
        }
        BlockState Ground = World.getBlockState(Under);
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), At.x, At.y + 0.1, At.z, Count, 0.5, 0.1, 0.5, 0.1);
        }
    }

    private boolean hittable(LivingEntity Found) {
        return Found != this && Found.isAlive() && !Scp354Prey.isKin(Found) && !(Found instanceof Player User && (User.isCreative() || User.isSpectator()));
    }

    private void sweepHit() {
        Scp354Sounds.play(this, Scp354Registry.SweepLayers.subList(0, 1), 1.6f, 0.75f + random.nextFloat() * 0.1f);
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        for (LivingEntity Found : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Reach, 2.0, Reach), this::hittable)) {
            if (!inReach(Found)) {
                continue;
            }
            Vec3 Toward = new Vec3(Found.getX() - getX(), 0.0, Found.getZ() - getZ());
            if (Toward.lengthSqr() > 1.0e-4 && Facing.dot(Toward.normalize()) < SweepArc) {
                continue;
            }
            if (Found.hurt(damageSources().mobAttack(this), (float) SweepDamage)) {
                Found.knockback(SweepKnock, -Toward.x, -Toward.z);
                Found.setDeltaMovement(Found.getDeltaMovement().add(0.0, SweepLift, 0.0));
                Found.hurtMarked = true;
                setLastHurtMob(Found);
            }
        }
    }

    private void slamHit(@Nullable LivingEntity Target) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Impact = Target != null && Target.isAlive() && inReach(Target) ? Target.position() : position().add(Facing.scale(Reach * 0.75));
        if (level() instanceof ServerLevel World) {
            Scp354Sounds.play(World, Impact, Scp354Registry.SlamLayers.subList(1, 2), 1.8f, 0.7f + random.nextFloat() * 0.1f);
            dust(World, Impact, 24);
        }
        AABB Area = new AABB(Impact, Impact).inflate(SlamSplash, 1.5, SlamSplash);
        for (LivingEntity Found : level().getEntitiesOfClass(LivingEntity.class, Area, this::hittable)) {
            boolean Direct = Found == Target;
            double Dx = Found.getX() - Impact.x;
            double Dz = Found.getZ() - Impact.z;
            if (!Direct && Math.sqrt(Dx * Dx + Dz * Dz) > SlamSplash) {
                continue;
            }
            if (Found.hurt(damageSources().mobAttack(this), (float) (Direct ? SlamDamage : SlamSplashDamage))) {
                double Ax = Found.getX() - getX();
                double Az = Found.getZ() - getZ();
                Found.knockback(SlamKnock, -Ax, -Az);
                Found.setDeltaMovement(Found.getDeltaMovement().add(0.0, SlamLift, 0.0));
                Found.hurtMarked = true;
                setLastHurtMob(Found);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (isBurrowed() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid(FluidType Type) {
        return false;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean isPickable() {
        return !isBurrowed() && super.isPickable();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isActing()) {
            return;
        }
        Scp354Sounds.play(this, Scp354Registry.TentacleIdleLayers, 1.3f, 0.8f + random.nextFloat() * 0.15f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp354Registry.TentacleIdle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp354Registry.TentacleHurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        Scp354Sounds.play(this, Scp354Registry.TentacleHurtLayers, 1.3f, 0.8f + random.nextFloat() * 0.15f);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp354StageController<>(this, "body", Scp354Tentacle::shownClip, Scp354Tentacle::actionElapsed, Event -> {
            byte Current = shownAction();
            Event.getController().transitionLength(Current == ActionEmerge || Current == ActionRise ? 0 : Current == ActionNone ? 6 : 3);
            if (isDeadOrDying()) {
                Event.getController().transitionLength(4);
                return Event.setAndContinue(CollapsePlay);
            }
            return switch (Current) {
                case ActionEmerge -> Event.setAndContinue(EmergePlay);
                case ActionSweep -> Event.setAndContinue(SweepPlay);
                case ActionSlam -> Event.setAndContinue(SlamPlay);
                case ActionSink -> Event.setAndContinue(SinkPlay);
                case ActionRise -> Event.setAndContinue(RisePlay);
                default -> Event.setAndContinue(IdleLoop);
            };
        }));
    }
}
