package net.rai.scp_unleashed.grenade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.space.SpaceGravity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class GrenadeEntity extends Projectile {
    public static final byte PhaseArmed = 0;
    public static final byte PhaseBurning = 1;
    public static final byte PhaseVenting = 2;
    public static final byte PhaseSpent = 3;
    public static final double CentreLift = 0.125;
    public static final double HandDrop = 0.1;
    public static final double HandReach = 0.3;
    public static final float LobPitch = -4.0F;
    public static final double RenderRange = 64.0;
    public static final int FuseSyncTicks = 10;
    public static final float RestEase = 0.35F;
    private static final EntityDataAccessor<Byte> DataKind = SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DataPhase = SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DataFuse = SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DataPhaseTicks = SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.INT);
    private static Consumer<GrenadeEntity> ClientHook = Grenade -> {
    };

    private int Fuse = GrenadeTuning.FuseTicks;
    private int PhaseTicks;
    private boolean Resting;
    private boolean Landed;
    private int LastClink = -100;
    private int BumpTicks;
    private float Spin;
    private float SpinOld;
    private float Heading;
    private float Roll = 1.0F;
    @Nullable
    private Object ClientData;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> Type, Level World) {
        super(Type, World);
    }

    public static GrenadeEntity thrown(ServerLevel World, LivingEntity Thrower, GrenadeKind Kind, float Charge) {
        GrenadeEntity Grenade = new GrenadeEntity(GrenadeRegistry.Grenade.get(), World);
        Grenade.setKind(Kind);
        Grenade.setOwner(Thrower);
        Vec3 Eye = Thrower.getEyePosition();
        Vec3 Start = Eye.add(Thrower.getLookAngle().scale(HandReach)).subtract(0.0, HandDrop + CentreLift, 0.0);
        Grenade.setPos(Start.x, Start.y, Start.z);
        if (!World.noCollision(Grenade, Grenade.getBoundingBox())) {
            Grenade.setPos(Eye.x, Eye.y - CentreLift, Eye.z);
        }
        float Speed = Mth.lerp(Charge, GrenadeTuning.MinSpeed, GrenadeTuning.MaxSpeed);
        Grenade.shootFromRotation(Thrower, Thrower.getXRot(), Thrower.getYRot(), LobPitch, Speed, GrenadeTuning.ThrowSpread);
        Grenade.Heading = Thrower.getYRot() * Mth.DEG_TO_RAD;
        World.addFreshEntity(Grenade);
        return Grenade;
    }

    public static GrenadeEntity dispensed(Level World, double X, double Y, double Z, GrenadeKind Kind) {
        GrenadeEntity Grenade = new GrenadeEntity(GrenadeRegistry.Grenade.get(), World);
        Grenade.setKind(Kind);
        Grenade.setPos(X, Y - CentreLift, Z);
        return Grenade;
    }

    public static void setClientHook(Consumer<GrenadeEntity> Hook) {
        ClientHook = Hook;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataKind, (byte) 0);
        Builder.define(DataPhase, PhaseArmed);
        Builder.define(DataFuse, GrenadeTuning.FuseTicks);
        Builder.define(DataPhaseTicks, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (DataFuse.equals(Accessor)) {
            Fuse = entityData.get(DataFuse);
        } else if (DataPhaseTicks.equals(Accessor)) {
            PhaseTicks = entityData.get(DataPhaseTicks);
        }
    }

    public GrenadeKind getKind() {
        return GrenadeKind.byIndex(entityData.get(DataKind));
    }

    public void setKind(GrenadeKind Kind) {
        entityData.set(DataKind, (byte) Kind.ordinal());
    }

    public byte getPhase() {
        return entityData.get(DataPhase);
    }

    public boolean isSpent() {
        return getPhase() == PhaseSpent;
    }

    public int getFuse() {
        return Fuse;
    }

    public int getPhaseTicks() {
        return PhaseTicks;
    }

    public boolean isResting() {
        return Resting;
    }

    public boolean hasLanded() {
        return Landed;
    }

    public float getSpin(float PartialTick) {
        return Mth.lerp(PartialTick, SpinOld, Spin);
    }

    public float getHeading() {
        return Heading;
    }

    @Nullable
    public Object clientData() {
        return ClientData;
    }

    public void setClientData(@Nullable Object Data) {
        ClientData = Data;
    }

    public Vec3 centre() {
        return position().add(0.0, CentreLift, 0.0);
    }

    public void enterPhase(byte Phase, int Ticks) {
        PhaseTicks = Ticks;
        entityData.set(DataPhaseTicks, Ticks);
        entityData.set(DataPhase, Phase);
    }

    public boolean airless() {
        return SpaceAir.isAirless(level());
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }
        byte Phase = getPhase();
        if (Phase != PhaseSpent) {
            moveStep();
        }
        if (level() instanceof ServerLevel World) {
            serverTick(World, Phase);
        } else {
            clientTick(Phase);
            ClientHook.accept(this);
        }
    }

    private void clientTick(byte Phase) {
        if (Phase == PhaseArmed && Fuse > 0) {
            Fuse--;
        } else if (Phase != PhaseArmed && PhaseTicks > 0) {
            PhaseTicks--;
        }
        SpinOld = Spin;
        Vec3 Motion = getDeltaMovement();
        double Flat = Motion.horizontalDistance();
        if (Flat > 1.0E-3) {
            float Turn = Mth.wrapDegrees(((float) Mth.atan2(Motion.x, Motion.z) - Heading) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            if (Math.abs(Turn) > Mth.HALF_PI) {
                Roll = -Roll;
                Turn = Mth.wrapDegrees((Turn + Mth.PI) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            }
            Heading += Turn;
        }
        if (!Resting) {
            double Speed = Landed ? Flat : Motion.length();
            Spin += Roll * (float) Math.min(GrenadeTuning.MaxSpinPerTick, Speed * GrenadeTuning.SpinPerBlock);
        } else {
            float Rest = Mth.HALF_PI + Mth.PI * Math.round((Spin - Mth.HALF_PI) / Mth.PI);
            Spin += (Rest - Spin) * RestEase;
        }
        if (Math.abs(Spin) > Mth.TWO_PI * 64.0F) {
            float Wrap = Math.copySign(Mth.TWO_PI * 64.0F, Spin);
            Spin -= Wrap;
            SpinOld -= Wrap;
        }
    }

    private void serverTick(ServerLevel World, byte Phase) {
        if (Phase == PhaseArmed) {
            Fuse--;
            if (Fuse % FuseSyncTicks == 0) {
                entityData.set(DataFuse, Fuse);
            }
            if (Fuse <= 0) {
                detonate(World);
            }
            return;
        }
        PhaseTicks--;
        switch (Phase) {
            case PhaseBurning -> GrenadeFire.burnTick(World, this);
            case PhaseVenting -> GrenadeGas.ventTick(World, this);
            case PhaseSpent -> GrenadeBlast.spentTick(World, this);
            default -> {
            }
        }
        if (PhaseTicks <= 0 && !isRemoved()) {
            discard();
        }
    }

    public void detonate(ServerLevel World) {
        detonate(World, airless());
    }

    public void detonate(ServerLevel World, boolean Airless) {
        Fuse = 0;
        switch (getKind()) {
            case Frag -> GrenadeBlast.detonate(World, this);
            case Incendiary -> GrenadeFire.detonate(World, this, Airless);
            case Flash -> GrenadeFlash.detonate(World, this, Airless);
            case Gas -> GrenadeGas.detonate(World, this, Airless);
        }
    }

    public static double gravityFor(boolean Airless) {
        return Airless ? GrenadeTuning.Gravity * SpaceGravity.GravityScale : GrenadeTuning.Gravity;
    }

    private double gravityNow() {
        return gravityFor(airless());
    }

    private void moveStep() {
        Vec3 Motion = getDeltaMovement();
        if (Resting) {
            if (Motion.lengthSqr() > 1.0E-8 || level().noCollision(this, getBoundingBox().move(0.0, -0.04, 0.0))) {
                Resting = false;
            } else {
                return;
            }
        }
        Motion = Motion.add(0.0, -gravityNow(), 0.0);
        if (level() instanceof ServerLevel World) {
            Motion = bump(World, Motion);
            Motion = shatter(World, Motion);
        }
        setDeltaMovement(Motion);
        Vec3 Start = position();
        move(MoverType.SELF, Motion);
        if (isRemoved()) {
            return;
        }
        Vec3 Moved = position().subtract(Start);
        Vec3 After = getDeltaMovement();
        double X = Motion.x;
        double Y = Motion.y;
        double Z = Motion.z;
        double Impact = 0.0;
        boolean HitX = horizontalCollision && Math.abs(Moved.x - Motion.x) > 1.0E-5;
        boolean HitZ = horizontalCollision && Math.abs(Moved.z - Motion.z) > 1.0E-5;
        if (HitX) {
            Impact = Math.max(Impact, Math.abs(X));
            X = -X * GrenadeTuning.WallBounce;
            Z *= GrenadeTuning.WallScrape;
        }
        if (HitZ) {
            Impact = Math.max(Impact, Math.abs(Z));
            Z = -Z * GrenadeTuning.WallBounce;
            X *= GrenadeTuning.WallScrape;
        }
        if (verticalCollision) {
            Impact = Math.max(Impact, Math.abs(Y));
            if (Y < 0.0) {
                double Rebound = After.y > 1.0E-3 ? After.y * 0.8 : -Y * GrenadeTuning.FloorBounce;
                if (Rebound < GrenadeTuning.BounceFloor) {
                    Y = 0.0;
                    double Keep = rollKeep();
                    X *= Keep;
                    Z *= Keep;
                } else {
                    Y = Rebound;
                    X *= GrenadeTuning.BounceSkid;
                    Z *= GrenadeTuning.BounceSkid;
                }
                Landed = true;
            } else {
                Y = -Y * GrenadeTuning.CeilingBounce;
            }
        }
        double Drag = isInWater() ? GrenadeTuning.WaterDrag : GrenadeTuning.AirDrag;
        X *= Drag;
        Y *= Drag;
        Z *= Drag;
        if (onGround() && Y == 0.0 && X * X + Z * Z < GrenadeTuning.SettleSpeed * GrenadeTuning.SettleSpeed) {
            X = 0.0;
            Z = 0.0;
            Resting = true;
        }
        setDeltaMovement(X, Y, Z);
        if (Impact > GrenadeTuning.ClinkSpeed && level() instanceof ServerLevel World) {
            hasImpulse = true;
            if (tickCount - LastClink >= GrenadeTuning.ClinkGap) {
                LastClink = tickCount;
                GrenadeSounds.clink(World, centre(), Impact);
            }
        }
    }

    private double rollKeep() {
        BlockPos Below = getBlockPosBelowThatAffectsMyMovement();
        float Slip = level().getBlockState(Below).getFriction(level(), Below, this);
        return Math.min(GrenadeTuning.MaxRollKeep, Slip * GrenadeTuning.RollGrip);
    }

    private Vec3 bump(ServerLevel World, Vec3 Motion) {
        if (BumpTicks > 0) {
            BumpTicks--;
            return Motion;
        }
        if (Motion.lengthSqr() < 1.0E-3) {
            return Motion;
        }
        Vec3 From = centre();
        Vec3 To = From.add(Motion);
        EntityHitResult Hit = ProjectileUtil.getEntityHitResult(World, this, From, To, getBoundingBox().expandTowards(Motion).inflate(1.0), this::canHitEntity);
        if (Hit == null) {
            return Motion;
        }
        BumpTicks = GrenadeTuning.BumpCooldown;
        hasImpulse = true;
        GrenadeSounds.thud(World, Hit.getLocation(), Motion.length());
        return new Vec3(-Motion.x * GrenadeTuning.EntityBounce, Math.min(0.0, Motion.y) * GrenadeTuning.EntityBounce, -Motion.z * GrenadeTuning.EntityBounce);
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return super.canHitEntity(Target) && !(Target instanceof GrenadeEntity) && !Target.isSpectator();
    }

    private Vec3 shatter(ServerLevel World, Vec3 Motion) {
        if (Motion.length() < GrenadeTuning.GlassBreakSpeed) {
            return Motion;
        }
        Vec3 From = centre();
        BlockHitResult Hit = World.clip(new ClipContext(From, From.add(Motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (Hit.getType() != HitResult.Type.BLOCK) {
            return Motion;
        }
        BlockPos Pos = Hit.getBlockPos();
        BlockState State = World.getBlockState(Pos);
        if (!GunGlass.shatters(State) || !mayShatter(World, Pos, false)) {
            return Motion;
        }
        World.destroyBlock(Pos, false, this);
        return Motion.scale(GunGlass.carryOf(State));
    }

    public boolean mayShatter(ServerLevel World, BlockPos Pos, boolean Blast) {
        if (!GunGlass.shatters(World.getBlockState(Pos))) {
            return false;
        }
        Entity Owner = getOwner();
        if (Blast && !EventHooks.canEntityGrief(World, this)) {
            return false;
        }
        if (Owner instanceof ServerPlayer User) {
            return GunGlass.permitted(World, User, Pos);
        }
        return Blast || EventHooks.canEntityGrief(World, this);
    }

    @Override
    protected double getDefaultGravity() {
        return GrenadeTuning.Gravity;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderRange * RenderRange;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Kind", (byte) getKind().ordinal());
        Tag.putInt("Fuse", Fuse);
        Tag.putByte("Phase", getPhase());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putBoolean("Resting", Resting);
        Tag.putBoolean("Landed", Landed);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setKind(GrenadeKind.byIndex(Tag.getByte("Kind")));
        Fuse = Tag.contains("Fuse") ? Tag.getInt("Fuse") : GrenadeTuning.FuseTicks;
        entityData.set(DataFuse, Fuse);
        PhaseTicks = Tag.getInt("PhaseTicks");
        entityData.set(DataPhaseTicks, PhaseTicks);
        entityData.set(DataPhase, Tag.getByte("Phase"));
        Resting = Tag.getBoolean("Resting");
        Landed = Tag.getBoolean("Landed");
    }
}
