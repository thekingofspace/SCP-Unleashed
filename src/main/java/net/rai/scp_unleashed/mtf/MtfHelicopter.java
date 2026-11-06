package net.rai.scp_unleashed.mtf;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MtfHelicopter extends Entity implements GeoEntity {
    public static final byte PhaseApproach = 0;
    public static final byte PhaseHover = 1;
    public static final byte PhaseDeploy = 2;
    public static final byte PhasePickup = 3;
    public static final byte PhaseDepart = 4;
    public static final double MaxSpeed = 1.1;
    public static final double Accel = 0.035;
    public static final double MinSpeed = 0.08;
    public static final double SlowRadius = 40.0;
    public static final double ClimbRate = 0.35;
    public static final float TurnRate = 3.5f;
    public static final double RopeRate = 0.6;
    public static final double RetractRate = 0.9;
    public static final double NextRiderGap = 3.5;
    public static final double BoardReach = 1.6;
    public static final int SettleTicks = 10;
    public static final int RopeGap = 10;
    public static final int LingerTicks = 30;
    public static final int PickupPatience = 900;
    public static final int ApproachLimit = 2400;
    public static final int DepartLimit = 600;
    public static final double ExitClimb = 10.0;
    public static final double TerrainClearance = 10.0;
    public static final int ThumpEvery = 4;
    public static final float ThumpVolume = 0.7f;
    public static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(MtfHelicopter.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> LeftRopeData = SynchedEntityData.defineId(MtfHelicopter.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RightRopeData = SynchedEntityData.defineId(MtfHelicopter.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HoverYawData = SynchedEntityData.defineId(MtfHelicopter.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID CallerId;
    private MtfTier Tier = MtfTier.Faint;
    private boolean Pickup;
    private Vec3 Hover = Vec3.ZERO;
    private Vec3 Origin = Vec3.ZERO;
    private double Cruise;
    private double Speed;
    protected int PhaseTicks;
    private int Queue;
    private int NextRole;
    private int Linger;
    private final double[] RopeTarget = new double[2];
    private final int[] RopeReady = new int[2];
    private final List<List<UUID>> Riders = List.of(new ArrayList<>(), new ArrayList<>());
    private int LerpSteps;
    private double LerpX;
    private double LerpY;
    private double LerpZ;
    private double LerpYaw;
    private final float[] ShownRope = new float[2];
    private final float[] ShownRopeO = new float[2];
    private float Tilt;
    private float TiltO;
    private boolean Heard;

    public MtfHelicopter(EntityType<? extends MtfHelicopter> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Nullable
    public static MtfHelicopter dispatch(ServerLevel World, MtfMission Mission, boolean Pickup) {
        MtfHelicopter Made = MtfRegistry.HelicopterType.get().create(World);
        if (Made == null) {
            return null;
        }
        MtfLandingZone.Zone Zone = Mission.zone();
        Vec3 Entry = MtfLandingZone.entry(World, Zone, World.getRandom());
        Made.setup(Mission.caller(), Mission.tier(), Pickup, Zone, Entry);
        if (!World.addFreshEntity(Made)) {
            return null;
        }
        Mission.setHelicopter(Made.getUUID());
        return Made;
    }

    public void setup(UUID Caller, MtfTier Kind, boolean ForPickup, MtfLandingZone.Zone Zone, Vec3 Entry) {
        CallerId = Caller;
        Tier = Kind;
        Pickup = ForPickup;
        Hover = Zone.hover();
        Origin = Entry;
        Cruise = Math.max(Entry.y, Hover.y + MtfLandingZone.CruiseLift);
        Queue = ForPickup ? 0 : Kind.size();
        NextRole = 0;
        entityData.set(HoverYawData, Zone.Yaw());
        float Heading = heading(Entry, Hover, Zone.Yaw());
        moveTo(Entry.x, Entry.y, Entry.z, Heading, 0.0f);
        setPhase(PhaseApproach);
    }

    public void becomePickup(MtfLandingZone.Zone Zone) {
        Pickup = true;
        Queue = 0;
        Riders.get(0).clear();
        Riders.get(1).clear();
        if (Zone.hover().distanceToSqr(Hover) > 4.0) {
            Hover = Zone.hover();
            entityData.set(HoverYawData, Zone.Yaw());
            setPhase(PhaseApproach);
            return;
        }
        if (phase() == PhaseDeploy) {
            setPhase(PhasePickup);
        }
    }

    private static float heading(Vec3 From, Vec3 To, float Fallback) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        if (Dx * Dx + Dz * Dz < 1.0) {
            return Fallback;
        }
        return (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(PhaseData, PhaseApproach);
        Builder.define(LeftRopeData, 0.0f);
        Builder.define(RightRopeData, 0.0f);
        Builder.define(HoverYawData, 0.0f);
    }

    public byte phase() {
        return entityData.get(PhaseData);
    }

    private void setPhase(byte Value) {
        entityData.set(PhaseData, Value);
        PhaseTicks = 0;
    }

    public boolean isPickup() {
        return Pickup;
    }

    public boolean isDeparting() {
        return phase() == PhaseDepart;
    }

    public boolean ropesDown() {
        byte Current = phase();
        return (Current == PhaseDeploy || Current == PhasePickup) && rope(0) > 0.5f && rope(1) > 0.5f;
    }

    public Vec3 hover() {
        return Hover;
    }

    public float hoverYaw() {
        return entityData.get(HoverYawData);
    }

    @Nullable
    public UUID callerId() {
        return CallerId;
    }

    public MtfTier tier() {
        return Tier;
    }

    public int queued() {
        return Queue;
    }

    public float rope(int Side) {
        return entityData.get(Side == 0 ? LeftRopeData : RightRopeData);
    }

    private void setRope(int Side, double Value) {
        entityData.set(Side == 0 ? LeftRopeData : RightRopeData, (float) Math.max(0.0, Value));
    }

    public float shownRope(int Side, float Partial) {
        return Mth.lerp(Partial, ShownRopeO[Side], ShownRope[Side]);
    }

    public float tilt(float Partial) {
        return Mth.lerp(Partial, TiltO, Tilt);
    }

    public boolean heard() {
        return Heard;
    }

    public void markHeard() {
        Heard = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        PhaseTicks++;
        serverTick((ServerLevel) level());
    }

    protected void serverTick(ServerLevel World) {
        MtfMission Mission = CallerId == null ? null : MtfMissions.get(CallerId);
        boolean Ours = Mission != null && getUUID().equals(Mission.helicopter());
        if (!Ours && phase() != PhaseDepart) {
            depart();
        }
        switch (phase()) {
            case PhaseApproach -> approach(World);
            case PhaseHover -> settle(World, Mission);
            case PhaseDeploy -> deploy(World, Mission);
            case PhasePickup -> pickup(World, Mission);
            default -> leave(World);
        }
    }

    private void clientTick() {
        if (tickCount % ThumpEvery == 0) {
            level().playLocalSound(getX(), getY() + 4.0, getZ(), MtfRegistry.Thump.get(), SoundSource.NEUTRAL, ThumpVolume, 1.0f, false);
        }
        if (LerpSteps > 0) {
            lerpPositionAndRotationStep(LerpSteps, LerpX, LerpY, LerpZ, LerpYaw, getXRot());
            LerpSteps--;
        }
        for (int Side = 0; Side < 2; Side++) {
            ShownRopeO[Side] = ShownRope[Side];
            float Want = rope(Side);
            float Step = Want > ShownRope[Side] ? (float) RopeRate : (float) RetractRate;
            ShownRope[Side] = ShownRope[Side] + Mth.clamp(Want - ShownRope[Side], -Step, Step);
        }
        TiltO = Tilt;
        double Moved = Math.sqrt(Mth.square(getX() - xo) + Mth.square(getZ() - zo));
        float Want = (float) Mth.clamp(Moved * 0.16, 0.0, 0.16);
        Tilt += (Want - Tilt) * 0.08f;
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float Yaw, float Pitch, int Steps) {
        LerpX = X;
        LerpY = Y;
        LerpZ = Z;
        LerpYaw = Yaw;
        LerpSteps = 6;
    }

    @Override
    public double lerpTargetX() {
        return LerpSteps > 0 ? LerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return LerpSteps > 0 ? LerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return LerpSteps > 0 ? LerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return LerpSteps > 0 ? (float) LerpYaw : getYRot();
    }

    private void turnToward(float Wanted) {
        float Delta = Mth.wrapDegrees(Wanted - getYRot());
        setYRot(getYRot() + Mth.clamp(Delta, -TurnRate, TurnRate));
    }

    private double terrainAhead(ServerLevel World, Vec3 Direction) {
        double Highest = World.getMinBuildHeight();
        for (double Ahead = 0.0; Ahead <= 24.0; Ahead += 8.0) {
            double X = getX() + Direction.x * Ahead;
            double Z = getZ() + Direction.z * Ahead;
            if (World.hasChunkAt(BlockPos.containing(X, getY(), Z))) {
                Highest = Math.max(Highest, MtfLandingZone.surface(World, X, Z));
            }
        }
        return Highest;
    }

    protected void approach(ServerLevel World) {
        double Dx = Hover.x - getX();
        double Dz = Hover.z - getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Wanted = Flat > SlowRadius ? MaxSpeed : Math.max(MinSpeed, MaxSpeed * Flat / SlowRadius);
        Speed += Mth.clamp(Wanted - Speed, -Accel * 1.5, Accel);
        double Step = Math.min(Speed, Flat);
        Vec3 Direction = Flat > 1.0E-4 ? new Vec3(Dx / Flat, 0.0, Dz / Flat) : Vec3.ZERO;
        double Height = Flat > SlowRadius ? Cruise : Mth.lerp(Flat / SlowRadius, Hover.y, Cruise);
        if (Flat > 12.0) {
            Height = Math.max(Height, terrainAhead(World, Direction) + TerrainClearance);
        }
        double Rise = Mth.clamp(Height - getY(), -ClimbRate, ClimbRate);
        setPos(getX() + Direction.x * Step, getY() + Rise, getZ() + Direction.z * Step);
        turnToward(Flat > 14.0 ? heading(position(), Hover, hoverYaw()) : hoverYaw());
        boolean Arrived = Flat < 0.06 && Math.abs(getY() - Hover.y) < 0.06 && Math.abs(Mth.wrapDegrees(getYRot() - hoverYaw())) < 1.0f;
        if (Arrived) {
            setPos(Hover.x, Hover.y, Hover.z);
            setYRot(hoverYaw());
            Speed = 0.0;
            setPhase(PhaseHover);
        } else if (PhaseTicks > ApproachLimit) {
            depart();
        }
    }

    protected void settle(ServerLevel World, @Nullable MtfMission Mission) {
        setPos(Hover.x, Hover.y, Hover.z);
        if (PhaseTicks == SettleTicks) {
            for (int Side = 0; Side < 2; Side++) {
                Vec3 Anchor = MtfLandingZone.anchor(Hover, hoverYaw(), Side);
                RopeTarget[Side] = Math.min(MtfLandingZone.MaxRope, Anchor.y - MtfLandingZone.surface(World, Anchor.x, Anchor.z));
            }
            playLayers(MtfRegistry.RopeDropLayers, 1.4f, 0.9f);
            if (Mission != null && !Pickup) {
                MtfMissions.deploying(this, Mission);
            }
        }
        if (PhaseTicks <= SettleTicks) {
            return;
        }
        boolean Done = true;
        for (int Side = 0; Side < 2; Side++) {
            double Next = Math.min(RopeTarget[Side], rope(Side) + RopeRate);
            setRope(Side, Next);
            Done &= Next >= RopeTarget[Side] - 1.0E-3;
        }
        if (Done) {
            setPhase(Pickup ? PhasePickup : PhaseDeploy);
            if (Pickup && Mission != null) {
                MtfMissions.pickupReady(this, Mission);
            }
        }
    }

    private List<MtfOperative> riders(ServerLevel World, int Side, byte State) {
        List<MtfOperative> Found = new ArrayList<>();
        List<UUID> Listed = Riders.get(Side);
        Listed.removeIf(Id -> !(World.getEntity(Id) instanceof MtfOperative Rider) || !Rider.isAlive() || Rider.state() != State);
        for (UUID Id : Listed) {
            if (World.getEntity(Id) instanceof MtfOperative Rider) {
                Found.add(Rider);
            }
        }
        return Found;
    }

    private void deploy(ServerLevel World, @Nullable MtfMission Mission) {
        setPos(Hover.x, Hover.y, Hover.z);
        if (Mission == null) {
            Queue = 0;
        }
        boolean Busy = false;
        for (int Side = 0; Side < 2; Side++) {
            List<MtfOperative> Sliding = riders(World, Side, MtfOperative.StateRoping);
            Busy |= !Sliding.isEmpty();
            MtfOperative Last = Sliding.isEmpty() ? null : Sliding.get(Sliding.size() - 1);
            boolean Free = Last == null || Last.ropeTop() - Last.getY() > NextRiderGap;
            if (Queue > 0 && Free && PhaseTicks >= RopeReady[Side] && Mission != null) {
                MtfOperative Rider = spawnRider(World, Mission, Side);
                if (Rider != null) {
                    Riders.get(Side).add(Rider.getUUID());
                    Busy = true;
                }
                RopeReady[Side] = PhaseTicks + RopeGap;
            }
        }
        if (Queue > 0 || Busy) {
            Linger = 0;
            return;
        }
        if (++Linger >= LingerTicks) {
            if (Mission != null) {
                MtfMissions.deployed(this, Mission);
            }
            depart();
        }
    }

    @Nullable
    private MtfOperative spawnRider(ServerLevel World, MtfMission Mission, int Side) {
        MtfOperative Rider = MtfRegistry.OperativeType.get().create(World);
        if (Rider == null) {
            Queue = 0;
            return null;
        }
        int Index = NextRole++;
        Queue--;
        MtfRole Role = Tier.roles().get(Math.min(Index, Tier.size() - 1));
        Rider.configure(Mission.caller(), Tier, Role, Index, World.getRandom());
        Vec3 Stand = MtfLandingZone.standPoint(Hover, hoverYaw(), Side);
        double Top = Stand.y - MtfOperative.HandReach;
        float Yaw = MtfLandingZone.inwardYaw(hoverYaw(), Side);
        Rider.moveTo(Stand.x, Top, Stand.z, Yaw, 0.0f);
        Rider.setYHeadRot(Yaw);
        Rider.setYBodyRot(Yaw);
        Rider.beginRope(getUUID(), Side, Top);
        if (!World.addFreshEntity(Rider)) {
            return null;
        }
        Mission.enlist(Rider.getUUID());
        return Rider;
    }

    private void pickup(ServerLevel World, @Nullable MtfMission Mission) {
        setPos(Hover.x, Hover.y, Hover.z);
        boolean Busy = false;
        for (int Side = 0; Side < 2; Side++) {
            List<MtfOperative> Climbing = riders(World, Side, MtfOperative.StateBoarding);
            Busy |= !Climbing.isEmpty();
            MtfOperative Last = Climbing.isEmpty() ? null : Climbing.get(Climbing.size() - 1);
            boolean Free = Last == null || Last.getY() - groundBelow(World, Side) > NextRiderGap;
            if (Mission == null || !Free || PhaseTicks < RopeReady[Side]) {
                continue;
            }
            MtfOperative Next = waiting(World, Mission, Side);
            if (Next != null) {
                Next.beginBoarding(this, Side);
                Riders.get(Side).add(Next.getUUID());
                RopeReady[Side] = PhaseTicks + RopeGap;
                Busy = true;
            }
        }
        boolean Waiting = Mission != null && Mission.alive() > 0;
        if (Busy || Waiting && PhaseTicks < PickupPatience) {
            Linger = 0;
            return;
        }
        if (++Linger >= LingerTicks) {
            depart();
        }
    }

    private double groundBelow(ServerLevel World, int Side) {
        Vec3 Anchor = MtfLandingZone.anchor(Hover, hoverYaw(), Side);
        return MtfLandingZone.surface(World, Anchor.x, Anchor.z);
    }

    @Nullable
    private MtfOperative waiting(ServerLevel World, MtfMission Mission, int Side) {
        Vec3 Stand = MtfLandingZone.standPoint(Hover, hoverYaw(), Side);
        MtfOperative Best = null;
        double BestGap = BoardReach * BoardReach;
        for (UUID Id : Mission.operatives()) {
            if (!(World.getEntity(Id) instanceof MtfOperative Candidate) || !Candidate.isAlive() || Candidate.state() != MtfOperative.StateRegroup) {
                continue;
            }
            double Gap = Mth.square(Candidate.getX() - Stand.x) + Mth.square(Candidate.getZ() - Stand.z);
            if (Gap < BestGap && Math.abs(Candidate.getY() - groundBelow(World, Side)) < 2.5) {
                BestGap = Gap;
                Best = Candidate;
            }
        }
        return Best;
    }

    protected void depart() {
        if (phase() != PhaseDepart) {
            setPhase(PhaseDepart);
            Speed = 0.0;
            if (CallerId != null) {
                MtfMissions.departing(this);
            }
        }
    }

    protected void leave(ServerLevel World) {
        boolean Reeling = false;
        for (int Side = 0; Side < 2; Side++) {
            if (rope(Side) > 0.0f) {
                setRope(Side, rope(Side) - RetractRate);
                Reeling = true;
            }
        }
        if (Reeling) {
            return;
        }
        Vec3 Away = new Vec3(Origin.x - Hover.x, 0.0, Origin.z - Hover.z);
        double Reach = Away.length();
        boolean Overhead = Reach < 2.0;
        Vec3 Direction = Overhead ? Vec3.ZERO : Away.scale(1.0 / Reach);
        boolean Climbing = getY() < Hover.y + ExitClimb && !Overhead;
        Speed += Mth.clamp((Climbing ? 0.15 : MaxSpeed) - Speed, -Accel, Accel);
        double Height = Math.max(Cruise, terrainAhead(World, Direction) + TerrainClearance);
        double Rise = Overhead ? ClimbRate : Mth.clamp(Height - getY(), -ClimbRate, ClimbRate);
        Vec3 Next = new Vec3(getX() + Direction.x * Speed, getY() + Rise, getZ() + Direction.z * Speed);
        if (!Overhead) {
            turnToward(heading(Hover, Origin, getYRot()));
        }
        double Travelled = Math.sqrt(Mth.square(Next.x - Hover.x) + Mth.square(Next.z - Hover.z));
        boolean Gone = Overhead ? Next.y > Cruise + 24.0 : Travelled >= Reach - 1.0;
        if (Gone || PhaseTicks > DepartLimit || !World.isPositionEntityTicking(BlockPos.containing(Next))) {
            discard();
            return;
        }
        setPos(Next.x, Next.y, Next.z);
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = 320.0 * getViewScale();
        return Distance < Range * Range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double Drop = Math.max(rope(0), rope(1)) + 1.0;
        return getBoundingBox().inflate(8.0, 1.5, 9.0).expandTowards(0.0, -Drop, 0.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "frame", 0, Event -> Event.setAndContinue(Idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
