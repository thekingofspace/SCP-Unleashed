package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.PaceMeter;
import net.rai.scp_unleashed.motion.Settle;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class Scp354Wingbeast extends Scp354Beast {
    public static final float Width = 1.6f;
    public static final float Height = 1.2f;
    public static final float EyeHeight = 0.9f;
    public static final float RenderScale = 2.5f;
    public static final double Health = 150.0;
    public static final double Armour = 4.0;
    public static final double Toughness = 1.0;
    public static final double KnockbackResistance = 0.4;
    public static final double Speed = 0.3;
    public static final double FlySpeed = 0.45;
    public static final double SwoopSpeed = 0.85;
    public static final double Agility = 0.12;
    public static final double DiveAgility = 0.25;
    public static final double CircleRadius = 7.0;
    public static final double CircleHeight = 7.0;
    public static final double HomeHeight = 9.0;
    public static final int CircleMin = 40;
    public static final int CircleMax = 90;
    public static final double SwoopRange = 18.0;
    public static final float RakeDamage = 9.0f;
    public static final double RakeReach = 1.4;
    public static final int RakeTicks = 14;
    public static final int RakeHitTick = 7;
    public static final int SwoopTicks = 60;
    public static final int CarryTicks = 50;
    public static final double CarryClimb = 0.26;
    public static final int CarryCooldown = 240;
    public static final float MaxCarryHeight = 2.05f;
    public static final float MaxCarryWidth = 1.0f;
    public static final float FlapSeconds = 0.7f;
    public static final byte ActionSwoop = 10;
    public static final byte ActionRake = 11;
    public static final byte ActionCarry = 12;
    private static final Scp354Stride[] Strides = {Scp354Gaits.WingbeastAir, Scp354Gaits.WingbeastAir};
    @Nullable
    private LivingEntity StrikeTarget;
    @Nullable
    private BlockPos Home;
    private int CircleLeft = CircleMin;
    private int CarryReady = 60;
    private double Orbit;
    private float Bank;
    private float BankO;
    private float FlapClock;
    private String Flight = "idle";
    private final PaceMeter FlightPace = new PaceMeter();
    private final PaceMeter FlightClimb = new PaceMeter();
    private final Settle<String> FlightSettle = new Settle<>();

    public Scp354Wingbeast(EntityType<? extends Scp354Wingbeast> Type, Level World) {
        super(Type, World);
        xpReward = 45;
        setNoGravity(true);
        Orbit = random.nextDouble() * Math.PI * 2.0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FLYING_SPEED, FlySpeed)
                .add(Attributes.ATTACK_DAMAGE, RakeDamage).add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        registerBaseGoals();
        goalSelector.addGoal(3, new Soar(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        FlyingPathNavigation Flying = new FlyingPathNavigation(this, World);
        Flying.setCanOpenDoors(false);
        return Flying;
    }

    @Override
    protected Scp354Stride[] strides() {
        return Strides;
    }

    @Override
    protected double runEnter() {
        return 10.0;
    }

    @Override
    protected double runExit() {
        return 9.0;
    }

    @Override
    protected double idleSeconds() {
        return FlapSeconds;
    }

    @Override
    public float renderScale() {
        return RenderScale;
    }

    @Override
    public Scp354Voice voice() {
        return Scp354BeastRegistry.WingbeastVoice;
    }

    @Override
    public double acquireRange() {
        return 40.0;
    }

    @Override
    protected boolean slowBlend(String Clip) {
        return super.slowBlend(Clip) || "swoop".equals(Clip) || "rake".equals(Clip) || "carry".equals(Clip);
    }

    @Override
    public boolean swimsOut() {
        return false;
    }

    @Override
    protected boolean treads() {
        return false;
    }

    @Nullable
    @Override
    protected String clipFor(byte Action) {
        return switch (Action) {
            case ActionSwoop -> "swoop";
            case ActionRake -> "rake";
            default -> null;
        };
    }

    @Nullable
    @Override
    protected String loopClip() {
        return isCarrying() ? "carry" : Flight;
    }

    @Override
    public boolean locksMovement() {
        return action() == ActionEmerge;
    }

    public boolean isCarrying() {
        return action() == ActionCarry || isVehicle();
    }

    public float bank(float Partial) {
        return Mth.lerp(Partial, BankO, Bank);
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setHome(BlockPos Spot) {
        Home = Spot.immutable();
    }

    @Override
    public void beginEmerge() {
        super.beginEmerge();
        Home = blockPosition();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Fall, boolean Ground, BlockState State, BlockPos Pos) {
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity Passenger, EntityDimensions Dimensions, float Partial) {
        return new Vec3(0.0, -Passenger.getBbHeight() - 0.05, 0.0);
    }

    public static boolean grabbable(LivingEntity Found) {
        return Found.getBbHeight() <= MaxCarryHeight && Found.getBbWidth() <= MaxCarryWidth && !Found.isPassenger() && !Found.isVehicle() && Targeting.isTargetable(Found);
    }

    public boolean canSwoop() {
        return canAct() && !isVehicle();
    }

    public void beginSwoop(LivingEntity Target) {
        StrikeTarget = Target;
        setAction(ActionSwoop, SwoopTicks);
        playLayers(Scp354BeastRegistry.WingbeastSwoop, 2.0f, 1.0f);
    }

    public void beginRake(LivingEntity Target) {
        StrikeTarget = Target;
        setAction(ActionRake, RakeTicks);
    }

    public boolean beginCarry(LivingEntity Target) {
        if (!grabbable(Target) || !Target.startRiding(this, true)) {
            return false;
        }
        StrikeTarget = Target;
        CarryReady = CarryCooldown;
        setAction(ActionCarry, CarryTicks);
        playLayers(Scp354BeastRegistry.WingbeastGrab, 1.4f, 0.9f);
        strike(Target, RakeDamage * 0.5f, 0.0, 0.0);
        return true;
    }

    public void drop() {
        if (isVehicle()) {
            ejectPassengers();
        }
    }

    @Override
    protected void finishAction(byte Kind) {
        super.finishAction(Kind);
        if (Kind == ActionCarry) {
            drop();
        }
        if (Kind == ActionSwoop || Kind == ActionRake || Kind == ActionCarry) {
            CircleLeft = CircleMin + random.nextInt(CircleMax - CircleMin);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!isDeadOrDying() && !isNoGravity()) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            flightTick();
            return;
        }
        if (Home == null) {
            Home = blockPosition();
        }
        CarryReady = Math.max(0, CarryReady - 1);
        if (action() != ActionCarry && isVehicle()) {
            drop();
        }
    }

    private void flightTick() {
        BankO = Bank;
        float Turn = Mth.wrapDegrees(yBodyRot - yBodyRotO);
        Bank += (Mth.clamp(Turn * 4.0f, -40.0f, 40.0f) - Bank) * 0.2f;
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Pace = FlightPace.push(Math.sqrt(Dx * Dx + Dz * Dz));
        double Climb = FlightClimb.push(getY() - yo);
        String Want;
        if (onGround() || Pace < 0.08 && Math.abs(Climb) < 0.06) {
            Want = "idle";
        } else if (Math.abs(Bank) > 14.0f) {
            Want = "bank";
        } else if (Climb > 0.03 || Pace < 0.28) {
            Want = "fly";
        } else {
            Want = "glide";
        }
        if (FlightSettle.ready(Flight, Want, Want.equals("glide") ? 9 : 4)) {
            Flight = Want;
            FlightSettle.clear();
        }
        String Loop = loopClip();
        boolean Beating = !isActing() && ("fly".equals(Loop) || "idle".equals(Loop) || "carry".equals(Loop));
        if (Beating && isAlive() && !isSilent()) {
            FlapClock += 1.0f / (FlapSeconds * 20.0f * ("carry".equals(Loop) ? 0.72f : 1.0f));
            if (FlapClock >= 1.0f) {
                FlapClock -= 1.0f;
                if (level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 40.0)) {
                    for (DeferredHolder<SoundEvent, SoundEvent> Layer : voice().Step()) {
                        level().playLocalSound(getX(), getY(), getZ(), Layer.get(), getSoundSource(), voice().StepVolume(), voice().Pitch() * (0.92f + random.nextFloat() * 0.12f), false);
                    }
                }
            }
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionSwoop) {
            if (Target == null || !Target.isAlive()) {
                endAction();
                return;
            }
            steer(Target.position().add(0.0, Target.getBbHeight() * 0.6, 0.0), SwoopSpeed, DiveAgility);
            if (distanceTo(Target) <= RakeReach + (getBbWidth() + Target.getBbWidth()) * 0.5) {
                if (CarryReady <= 0 && grabbable(Target) && random.nextInt(5) < 2 && beginCarry(Target)) {
                    return;
                }
                beginRake(Target);
            }
        } else if (Kind == ActionRake) {
            setDeltaMovement(getDeltaMovement().scale(0.7).add(0.0, 0.03, 0.0));
            if (Target != null && Target.isAlive()) {
                faceToward(Target, 30.0f);
                if (Elapsed == RakeHitTick && distanceTo(Target) <= RakeReach + (getBbWidth() + Target.getBbWidth()) * 0.5 + 0.6) {
                    if (strike(Target, RakeDamage, 0.7, 0.2)) {
                        playLayers(Scp354BeastRegistry.WingbeastRake, 1.3f, 1.0f);
                    }
                }
            }
        } else if (Kind == ActionCarry) {
            Vec3 Forward = Vec3.directionFromRotation(0.0f, getYRot()).scale(0.12);
            setDeltaMovement(new Vec3(Forward.x, CarryClimb, Forward.z));
            if (!isVehicle() && Elapsed > 2) {
                endAction();
                CircleLeft = CircleMin;
            }
        }
    }

    public void steer(Vec3 Point, double Pace, double Turn) {
        Vec3 Toward = Point.subtract(position());
        double Length = Toward.length();
        if (Length < 1.0e-3) {
            return;
        }
        Vec3 Want = Toward.scale(Math.min(Pace, Length * 0.5) / Length);
        Vec3 Velocity = getDeltaMovement();
        setDeltaMovement(Velocity.add(Want.subtract(Velocity).scale(Turn)));
        Vec3 Heading = getDeltaMovement();
        if (Heading.horizontalDistanceSqr() > 1.0e-4) {
            float Yaw = (float) (Mth.atan2(Heading.z, Heading.x) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Yaw, 10.0f)));
            yBodyRot = getYRot();
        }
    }

    private Vec3 circlePoint(Vec3 Centre, double Radius, double Height) {
        Orbit += 0.045;
        double Floor = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(Centre.x), Mth.floor(Centre.z));
        double Y = Math.max(Centre.y + Height, Floor + Height * 0.6);
        return new Vec3(Centre.x + Math.cos(Orbit) * Radius, Y, Centre.z + Math.sin(Orbit) * Radius);
    }

    private void soarTick() {
        if (isActing() || isDeadOrDying()) {
            return;
        }
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive()) {
            steer(circlePoint(Target.position(), CircleRadius, CircleHeight), FlySpeed, Agility);
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (--CircleLeft <= 0 && canSwoop() && hasLineOfSight(Target) && horizontalDistanceTo(Target) < SwoopRange) {
                beginSwoop(Target);
            }
            return;
        }
        Vec3 Centre = Home != null ? Vec3.atBottomCenterOf(Home) : position();
        steer(circlePoint(Centre, CircleRadius + 2.0, HomeHeight), FlySpeed * 0.7, Agility * 0.7);
    }

    private double horizontalDistanceTo(Entity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    @Override
    public void die(DamageSource Source) {
        drop();
        super.die(Source);
        setNoGravity(false);
    }

    @Override
    protected void tickDeath() {
        if (isNoGravity()) {
            setNoGravity(false);
        }
        super.tickDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        setNoGravity(true);
    }

    public static final class Soar extends Goal {
        private final Scp354Wingbeast Flyer;

        public Soar(Scp354Wingbeast Flyer) {
            this.Flyer = Flyer;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Flyer.locksMovement() && Flyer.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Flyer.soarTick();
        }
    }
}
