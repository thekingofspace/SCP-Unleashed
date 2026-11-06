package net.rai.scp_unleashed.furnishing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public class FurnishingSeat extends Entity {
    public FurnishingSeat(EntityType<? extends FurnishingSeat> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
    }

    public FurnishingSeat(Level World, Vec3 Spot, float Yaw) {
        this(FurnishingRegistry.Seat.get(), World);
        setPos(Spot.x, Spot.y, Spot.z);
        setYRot(Yaw);
        this.yRotO = Yaw;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof SeatHost)) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity Passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity Passenger, EntityDimensions Dimensions, float PartialTick) {
        return Vec3.ZERO;
    }

    @Override
    public void onPassengerTurned(Entity Passenger) {
        Passenger.setYBodyRot(getYRot());
        float Difference = Mth.wrapDegrees(Passenger.getYRot() - getYRot());
        float Clamped = Mth.clamp(Difference, -105.0f, 105.0f);
        Passenger.yRotO += Clamped - Difference;
        Passenger.setYRot(Passenger.getYRot() + Clamped - Difference);
        Passenger.setYHeadRot(Passenger.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity Passenger) {
        BlockPos Pos = blockPosition();
        Direction Front = Direction.fromYRot(getYRot());
        for (Direction Side : new Direction[]{Front, Front.getClockWise(), Front.getCounterClockWise(), Front.getOpposite()}) {
            Vec3 Spot = DismountHelper.findSafeDismountLocation(Passenger.getType(), level(), Pos.relative(Side), true);
            if (Spot != null) {
                return Spot;
            }
        }
        Vec3 Above = DismountHelper.findSafeDismountLocation(Passenger.getType(), level(), Pos.above(), true);
        return Above != null ? Above : super.getDismountLocationForPassenger(Passenger);
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
