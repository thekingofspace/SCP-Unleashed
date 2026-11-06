package net.rai.scp_unleashed.props;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PropSeatEntity extends Entity {
    public static final double SeatHeight = 9.0 / 16.0;
    public static final float LookRange = 100.0f;
    public static final double BackShift = 0.1;

    public PropSeatEntity(EntityType<? extends PropSeatEntity> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public static boolean occupied(Level World, BlockPos Pos) {
        return World.getEntitiesOfClass(PropSeatEntity.class, new AABB(Pos)).stream().anyMatch(Entity::isVehicle);
    }

    public static boolean sit(Level World, BlockPos Pos, BlockState State, Player User) {
        if (occupied(World, Pos)) {
            return false;
        }
        release(World, Pos);
        PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), World);
        Direction Facing = State.getValue(HorizontalDirectionalBlock.FACING);
        Seat.moveTo(Pos.getX() + 0.5 - Facing.getStepX() * BackShift, Pos.getY(), Pos.getZ() + 0.5 - Facing.getStepZ() * BackShift, Facing.toYRot(), 0.0f);
        World.addFreshEntity(Seat);
        if (User.startRiding(Seat)) {
            return true;
        }
        Seat.discard();
        return false;
    }

    public static void release(Level World, BlockPos Pos) {
        for (PropSeatEntity Seat : World.getEntitiesOfClass(PropSeatEntity.class, new AABB(Pos))) {
            Seat.ejectPassengers();
            Seat.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        BlockState Under = level().getBlockState(blockPosition());
        if (!isVehicle() || !(Under.getBlock() instanceof SeatBlock Host) || !Host.seats(Under)) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity Passenger, EntityDimensions Dimensions, float PartialTick) {
        BlockState Under = level().getBlockState(blockPosition());
        return new Vec3(0.0, Under.getBlock() instanceof SeatBlock Host ? Host.seatHeight(Under) : SeatHeight, 0.0);
    }

    @Override
    protected void positionRider(Entity Passenger, Entity.MoveFunction Callback) {
        super.positionRider(Passenger, Callback);
        clampRotation(Passenger);
    }

    @Override
    public void onPassengerTurned(Entity Passenger) {
        clampRotation(Passenger);
    }

    private void clampRotation(Entity Passenger) {
        Passenger.setYBodyRot(getYRot());
        float Offset = Mth.wrapDegrees(Passenger.getYRot() - getYRot());
        float Clamped = Mth.clamp(Offset, -LookRange, LookRange);
        Passenger.yRotO += Clamped - Offset;
        Passenger.setYRot(Passenger.getYRot() + Clamped - Offset);
        Passenger.setYHeadRot(Passenger.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity Passenger) {
        BlockPos Pos = blockPosition();
        Direction Front = Direction.fromYRot(getYRot());
        for (Direction Side : List.of(Front, Front.getClockWise(), Front.getCounterClockWise(), Front.getOpposite())) {
            for (int Lift : new int[]{0, 1, -1}) {
                Vec3 Spot = DismountHelper.findSafeDismountLocation(Passenger.getType(), level(), Pos.relative(Side).above(Lift), true);
                if (Spot != null) {
                    return Spot;
                }
            }
        }
        return super.getDismountLocationForPassenger(Passenger);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
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
}
