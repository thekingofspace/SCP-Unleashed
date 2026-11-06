package net.rai.scp_unleashed.scp035;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageGeometry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class Scp035Sight {
    public static final double SmallWindowY = 9.0;
    public static final double TallLowerWindowY = 10.5;
    public static final double TallUpperWindowY = 22.5;
    public static final double WindowZ = 1.0;
    public static final double FrontMargin = 0.05;

    private Scp035Sight() {
    }

    public static boolean hidesFace(LivingEntity Target) {
        return Target.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN) || AntiMemetic.isProtected(Target);
    }

    public static boolean wearsMask(LivingEntity Target) {
        return Scp035MaskItem.is(Target.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static boolean isExempt(Player User) {
        return !User.isAlive() || User.isCreative() || User.isSpectator();
    }

    public static boolean isDrawable(Player User) {
        return Targeting.isTargetable(User) && !isExempt(User) && !hidesFace(User) && !wearsMask(User) && !User.isSleeping() && !Scp268.isNeglected(User);
    }

    public static boolean isPerson(LivingEntity Target) {
        return Target.getType().is(Scp035Registry.PersonLike);
    }

    public static boolean isDrawable(Mob Person) {
        return Person.isAlive() && Targeting.isTargetable(Person) && isPerson(Person) && !Person.isNoAi() && Person.getControllingPassenger() == null && !hidesFace(Person) && !wearsMask(Person);
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To, Predicate<BlockPos> Ignored) {
        Boolean Blocked = BlockGetter.traverseBlocks(From, To, World, (Getter, Pos) -> {
            if (Ignored.test(Pos)) {
                return null;
            }
            BlockState State = Getter.getBlockState(Pos);
            VoxelShape Shape = State.getVisualShape(Getter, Pos, CollisionContext.empty());
            if (Shape.isEmpty()) {
                return null;
            }
            return Shape.clip(From, To, Pos) != null ? Boolean.TRUE : null;
        }, Getter -> Boolean.FALSE);
        return !Blocked;
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To) {
        return clear(World, From, To, Pos -> false);
    }

    @Nullable
    public static Vec3 lockerWindow(Level World, Vec3 Eye, StorageContainerBlockEntity Locker, double Range) {
        Direction Facing = Locker.getFacing();
        Vec3 Normal = new Vec3(Facing.getStepX(), 0.0, Facing.getStepZ());
        BlockPos Pos = Locker.getStoragePos();
        boolean Tall = Locker.isTall();
        BlockPos Upper = Pos.above();
        Predicate<BlockPos> Ignored = Cell -> Cell.equals(Pos) || Tall && Cell.equals(Upper);
        double Reach = Range * Range;
        double Best = Double.MAX_VALUE;
        Vec3 Found = null;
        double[] Heights = Tall ? new double[]{TallLowerWindowY, TallUpperWindowY} : new double[]{SmallWindowY};
        for (double Height : Heights) {
            Vec3 Window = StorageGeometry.point(Pos, Facing, StorageGeometry.CentreX, Height, WindowZ);
            double Distance = Window.distanceToSqr(Eye);
            if (Distance > Reach || Distance >= Best || Eye.subtract(Window).dot(Normal) <= FrontMargin) {
                continue;
            }
            if (clear(World, Eye, Window, Ignored)) {
                Best = Distance;
                Found = Window;
            }
        }
        return Found;
    }

    public static Vec3 lockerFront(StorageContainerBlockEntity Locker) {
        return StorageGeometry.point(Locker.getStoragePos(), Locker.getFacing(), StorageGeometry.CentreX, 0.0, -8.0);
    }
}
