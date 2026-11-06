package net.rai.scp_unleashed.scp106;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public class Scp106Restraint extends Entity {
    public static final int StrugglePresses = 8;
    public static final int StruggleDecay = 30;
    public static final float YawLimit = 70.0f;

    private int Presses;
    private boolean LastShift;
    private int SincePress;

    public Scp106Restraint(EntityType<? extends Scp106Restraint> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
    }

    public Scp106Restraint(Level World, Vec3 Spot, float Yaw) {
        this(Scp106Registry.Restraint.get(), World);
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

    public int presses() {
        return Presses;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof Scp106FemurBreakerBlock)) {
            ejectPassengers();
            discard();
            return;
        }
        if (++SincePress > StruggleDecay && Presses > 0) {
            Presses--;
            SincePress = 0;
        }
        for (Entity Passenger : getPassengers()) {
            if (!(Passenger instanceof Player User)) {
                continue;
            }
            boolean Shift = User.isShiftKeyDown();
            if (Shift && !LastShift) {
                Presses++;
                SincePress = 0;
                level().playSound(null, blockPosition(), Scp106Registry.Buckle.get(), SoundSource.BLOCKS, 0.4f, 1.4f + random.nextFloat() * 0.3f);
                if (Presses >= StrugglePresses) {
                    LastShift = false;
                    Presses = 0;
                    User.setShiftKeyDown(false);
                    User.stopRiding();
                    User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.struggled_free").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.struggling", Presses, StrugglePresses).withStyle(ChatFormatting.DARK_GRAY), true);
            }
            LastShift = Shift;
            User.setShiftKeyDown(false);
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
        float Clamped = Mth.clamp(Difference, -YawLimit, YawLimit);
        Passenger.yRotO += Clamped - Difference;
        Passenger.setYRot(Passenger.getYRot() + Clamped - Difference);
        Passenger.setYHeadRot(Passenger.getYRot());
    }

    @Override
    protected void positionRider(Entity Passenger, Entity.MoveFunction Mover) {
        super.positionRider(Passenger, Mover);
        if (Passenger instanceof LivingEntity Living && !(Passenger instanceof Player)) {
            Living.setYBodyRot(getYRot());
            Living.setYHeadRot(getYRot());
            Living.setYRot(getYRot());
        }
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
        Vec3 Above = DismountHelper.findSafeDismountLocation(Passenger.getType(), level(), Pos.above(2), true);
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
