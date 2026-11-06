package net.rai.scp_unleashed.raid.chaos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.mtf.MtfLandingZone;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileLevels;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.underground.RaidBreach;
import net.rai.scp_unleashed.raid.underground.UndergroundRules;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ChaosInsurgent extends HostileRaider {
    public static final double[] Health = {20.0, 24.0, 28.0, 32.0, 36.0, 40.0};
    public static final double Speed = 0.3;
    public static final GunKind[][] Guns = {
            {GunKind.Pistol},
            {GunKind.Pistol, GunKind.Shotgun},
            {GunKind.AssaultRifle, GunKind.Shotgun},
            {GunKind.AssaultRifle, GunKind.Shotgun, GunKind.AssaultRifle},
            {GunKind.SniperRifle, GunKind.AssaultRifle, GunKind.Shotgun, GunKind.AssaultRifle},
            {GunKind.SniperRifle, GunKind.AssaultRifle, GunKind.MachineGun, GunKind.CombatShotgun}
    };
    public static final double SlideStart = 0.12;
    public static final double SlideMax = 0.45;
    public static final double SlideAccel = 0.03;
    public static final double SlideBrake = 2.5;
    public static final double SlideFloor = 0.14;
    public static final double ClimbSpeed = 0.22;
    public static final double HandReach = 2.25;
    public static final int RopeHoldTicks = 4;
    public static final int WaitLimit = 1200;
    public static final DustParticleOptions Smoke = dust(0.32f, 0.34f, 0.28f, 1.4f);
    public static final byte StateRappel = 6;
    public static final byte StateClimb = 7;
    public static final double AxisSnap = 0.12;

    @Nullable
    private UUID RideId;
    private int RopeSide;
    private double RopeTop;
    private double RopeSpeed;
    private int RopeHold;
    private boolean Thrower;
    private int ThrowReady;
    @Nullable
    private Vec3 ShaftAxis;
    private double ShaftFloor;
    private double ShaftTop;
    @Nullable
    private BlockPos ShaftLip;

    public ChaosInsurgent(EntityType<? extends ChaosInsurgent> Type, Level World) {
        super(Type, World);
    }

    @Override
    public HostileFaction faction() {
        return ChaosFaction.Instance;
    }

    @Override
    protected void addSeekGoals() {
        goalSelector.addGoal(2, new ChaosSecureGoal(this));
        goalSelector.addGoal(4, new ChaosBreachGoal(this));
    }

    public static GunKind gunFor(int Level, int Index) {
        GunKind[] Choices = Guns[HostileLevels.index(Level)];
        return Choices[Math.floorMod(Index, Choices.length)];
    }

    @Override
    protected void dress(int Level, int Index, RandomSource Random) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.ChaosInsurgencyHelmet.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.ChaosInsurgencyVest.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.ChaosInsurgencyPants.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.ChaosInsurgencyBoots.get()));
        setItemSlot(EquipmentSlot.MAINHAND, GunItem.loaded(gunFor(Level, Index)));
        base(Attributes.MOVEMENT_SPEED, Speed);
    }

    @Override
    protected double healthFor(int Level) {
        return HostileLevels.pick(Health, Level);
    }

    @Override
    protected DustParticleOptions mote() {
        return Smoke;
    }

    public boolean roping() {
        byte Current = state();
        return Current == StateRoping || Current == StateBoarding;
    }

    public boolean rappelling() {
        byte Current = state();
        return Current == StateRappel || Current == StateClimb;
    }

    public boolean onRope() {
        return roping() || rappelling();
    }

    public void beginRappel(Vec3 Axis, double Floor, double Top) {
        ShaftAxis = Axis;
        ShaftFloor = Floor;
        ShaftTop = Top;
        ShaftLip = null;
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        setState(StateRappel);
        playSound(MtfRegistry.RopeLand.get(), 0.6f, 1.3f);
    }

    public void beginClimb(Vec3 Axis, double Top, BlockPos Lip) {
        ShaftAxis = Axis;
        ShaftFloor = Axis.y;
        ShaftTop = Top;
        ShaftLip = Lip;
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        setState(StateClimb);
        playSound(MtfRegistry.RopeLand.get(), 0.6f, 1.2f);
    }

    public int ropeSide() {
        return RopeSide;
    }

    public double ropeTop() {
        return RopeTop;
    }

    public boolean thrower() {
        return Thrower;
    }

    public void setThrower(boolean Value) {
        Thrower = Value;
    }

    public boolean throwReady() {
        return Thrower && tickCount >= ThrowReady;
    }

    public void throwRest(int Ticks) {
        ThrowReady = tickCount + Ticks;
    }

    @Nullable
    public ChaosHelicopter helicopter() {
        if (RideId == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(RideId) instanceof ChaosHelicopter Found && Found.isAlive() ? Found : null;
    }

    public void beginRope(ChaosHelicopter Ride, int Side, double Top) {
        RideId = Ride.getUUID();
        RopeSide = Side;
        RopeTop = Top;
        RopeSpeed = SlideStart;
        RopeHold = RopeHoldTicks;
        setNoGravity(true);
        setState(StateRoping);
    }

    public void beginBoarding(ChaosHelicopter Ride, int Side) {
        RideId = Ride.getUUID();
        RopeSide = Side;
        Vec3 Stand = MtfLandingZone.standPoint(Ride.hover(), Ride.hoverYaw(), Side);
        RopeTop = Ride.hover().y + MtfLandingZone.AnchorUp - HandReach;
        float Yaw = MtfLandingZone.inwardYaw(Ride.hoverYaw(), Side);
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        moveTo(Stand.x, getY(), Stand.z, Yaw, 0.0f);
        setYHeadRot(Yaw);
        yBodyRot = Yaw;
        RopeHold = RopeHoldTicks + 2;
        setNoGravity(true);
        setState(StateBoarding);
        playSound(MtfRegistry.RopeLand.get(), 0.7f, 1.2f);
    }

    public void beginWaiting() {
        if (state() == StateWaiting) {
            return;
        }
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        setState(StateWaiting);
    }

    @Override
    protected void stateTick() {
        switch (state()) {
            case StateRoping -> slide();
            case StateBoarding -> climb();
            case StateWaiting -> await();
            case StateRappel -> rappel();
            case StateClimb -> climbOut();
            default -> {
            }
        }
    }

    private void await() {
        getNavigation().stop();
        ChaosHelicopter Ride = ChaosRaidStyle.helicopterFor(this);
        if (Ride != null) {
            getLookControl().setLookAt(Ride.getX(), Ride.getY(), Ride.getZ());
        }
        if (stateTicks() > WaitLimit) {
            HostileRaidStyle.departed(this);
        }
    }

    private void slide() {
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (RopeHold > 0) {
            RopeHold--;
            return;
        }
        if (helicopter() == null) {
            release();
            return;
        }
        double Ground = MtfLandingZone.surface((ServerLevel) level(), getX(), getZ());
        double Gap = getY() - Ground;
        RopeSpeed = Gap < SlideBrake ? Math.max(SlideFloor, RopeSpeed - 0.08) : Math.min(SlideMax, RopeSpeed + SlideAccel);
        double Next = getY() - RopeSpeed;
        if (tickCount % 6 == 0) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), MtfRegistry.RopeSlide.get(), SoundSource.NEUTRAL, 0.45f, 0.9f + getRandom().nextFloat() * 0.2f);
        }
        if (Next <= Ground) {
            setPos(getX(), Ground, getZ());
            release();
            delayReaction();
            level().playSound(null, getX(), getY(), getZ(), MtfRegistry.RopeLand.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
            level().playSound(null, getX(), getY(), getZ(), MtfRegistry.RopeLandGear.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
            return;
        }
        setPos(getX(), Next, getZ());
    }

    private void climb() {
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (RopeHold > 0) {
            RopeHold--;
            return;
        }
        if (helicopter() == null) {
            release();
            beginWaiting();
            return;
        }
        double Next = getY() + ClimbSpeed;
        if (tickCount % 8 == 0) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), MtfRegistry.RopeSlide.get(), SoundSource.NEUTRAL, 0.35f, 1.15f);
        }
        if (Next >= RopeTop) {
            HostileRaidStyle.departed(this);
            return;
        }
        setPos(getX(), Next, getZ());
    }

    private void release() {
        setNoGravity(false);
        setState(StateActive);
    }

    private boolean blocked(double X, double Y, double Z) {
        BlockPos Cell = BlockPos.containing(X, Y, Z);
        return !level().getBlockState(Cell).getCollisionShape(level(), Cell).isEmpty();
    }

    private boolean snap() {
        if (ShaftAxis == null) {
            return true;
        }
        double Gap = Math.hypot(getX() - ShaftAxis.x, getZ() - ShaftAxis.z);
        if (Gap <= AxisSnap) {
            return true;
        }
        setPos(Mth.lerp(UndergroundRules.AxisPull, getX(), ShaftAxis.x), getY(), Mth.lerp(UndergroundRules.AxisPull, getZ(), ShaftAxis.z));
        return false;
    }

    private void rappel() {
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (ShaftAxis == null) {
            release();
            return;
        }
        if (!snap()) {
            return;
        }
        double Next = getY() - UndergroundRules.RappelSpeed;
        if (tickCount % 6 == 0) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), MtfRegistry.RopeSlide.get(), SoundSource.NEUTRAL, 0.45f, 0.9f + getRandom().nextFloat() * 0.2f);
        }
        if (Next <= ShaftFloor) {
            setPos(ShaftAxis.x, ShaftFloor, ShaftAxis.z);
            release();
            delayReaction();
            level().playSound(null, getX(), getY(), getZ(), MtfRegistry.RopeLand.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
            level().playSound(null, getX(), getY(), getZ(), MtfRegistry.RopeLandGear.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
            return;
        }
        if (Next < ShaftTop + 0.5 && blocked(getX(), Next, getZ())) {
            release();
            return;
        }
        setPos(getX(), Next, getZ());
    }

    private void climbOut() {
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (ShaftAxis == null) {
            release();
            return;
        }
        if (!snap()) {
            return;
        }
        double Next = getY() + UndergroundRules.ClimbSpeed;
        if (tickCount % 8 == 0) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), MtfRegistry.RopeSlide.get(), SoundSource.NEUTRAL, 0.35f, 1.15f);
        }
        if (Next >= ShaftTop + 1.0) {
            if (ShaftLip != null) {
                setPos(ShaftLip.getX() + 0.5, ShaftLip.getY(), ShaftLip.getZ() + 0.5);
            } else {
                setPos(getX(), ShaftTop + 1.0, getZ());
            }
            release();
            return;
        }
        if (Next + getBbHeight() < ShaftTop + 0.5 && blocked(getX(), Next + getBbHeight(), getZ())) {
            release();
            return;
        }
        setPos(getX(), Next, getZ());
    }

    @Override
    public Vec3 exitPoint(Raid Current) {
        if (level() instanceof ServerLevel World) {
            byte Mode = RaidBreach.mode(Current);
            if (Mode == RaidBreach.ModeShaft && RaidBreach.open(Current) && RaidBreach.inside(World, Current, this)) {
                return RaidBreach.axis(RaidBreach.data(Current));
            }
            BlockPos Door = Mode == RaidBreach.ModeEntrance ? RaidBreach.entrance(RaidBreach.data(Current)) : null;
            if (Door != null && distanceToSqr(Vec3.atBottomCenterOf(Door)) > UndergroundRules.EntranceReach * UndergroundRules.EntranceReach
                    && RaidBreach.inside(World, Current, this)) {
                return Vec3.atBottomCenterOf(Door);
            }
        }
        Vec3 Stand = ChaosRaidStyle.standPoint(Current, slot() % 2);
        return Stand != null ? Stand : super.exitPoint(Current);
    }

    @Override
    public double exitReach() {
        return 1.2;
    }

    @Override
    public boolean useArsenal(LivingEntity Target, double Distance, boolean Visible) {
        return ChaosArsenal.use(this, Target, Distance, Visible);
    }

    public float ropeSway(float Partial) {
        return Mth.sin((tickCount + Partial) * 0.35f) * 0.05f;
    }
}
