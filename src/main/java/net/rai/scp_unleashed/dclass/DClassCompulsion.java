package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp012.Scp012CompositionItem;
import net.rai.scp_unleashed.scp012.Scp012Containment;
import net.rai.scp_unleashed.scp012.Scp012Exposure;
import net.rai.scp_unleashed.scp012.Scp012Sources;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageGeometry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public final class DClassCompulsion extends Goal {
    public static final int CheckInterval = 10;
    public static final double Stand = 2.0;

    private final DClass Owner;
    @Nullable
    private Vec3 Window;
    private int Lost;

    public DClassCompulsion(DClass Owner) {
        this.Owner = Owner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (Owner.tickCount % CheckInterval != Math.floorMod(Owner.getId(), CheckInterval) || Owner.isRestingPose()) {
            return false;
        }
        Window = find();
        return Window != null;
    }

    @Override
    public void start() {
        Lost = 0;
        Owner.wake();
    }

    @Override
    public boolean canContinueToUse() {
        return Window != null && Owner.isAlive() && Lost < 40;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (Window == null) {
            return;
        }
        if (Owner.tickCount % CheckInterval == 0) {
            Vec3 Seen = find();
            if (Seen == null) {
                Lost += CheckInterval;
            } else {
                Lost = 0;
                Window = Seen;
            }
        }
        Owner.getLookControl().setLookAt(Window.x, Window.y, Window.z, 40.0f, Owner.getMaxHeadXRot());
        if (Owner.distanceToSqr(Window) > Stand * Stand) {
            if (Owner.tickCount % 10 == 0) {
                Owner.getNavigation().moveTo(Window.x, Window.y - 1.0, Window.z, 0.55);
            }
        } else {
            Owner.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        Window = null;
        Owner.getNavigation().stop();
    }

    @Nullable
    private Vec3 find() {
        if (!(Owner.level() instanceof ServerLevel World) || Senses.isBlindfolded(Owner)) {
            return null;
        }
        Vec3 Eye = Owner.getEyePosition();
        Vec3 Look = Owner.getViewVector(1.0f);
        double Reach = (double) Scp012Exposure.WindowRange * Scp012Exposure.WindowRange;
        Vec3 Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : Scp012Sources.near(World, Eye, Scp012Exposure.WindowRange)) {
            StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Pos);
            if (Locker == null || !Scp012Containment.isExposed(Locker) || !Scp012CompositionItem.is(Locker.peekContents())) {
                continue;
            }
            Direction Facing = Locker.getFacing();
            Vec3 Normal = new Vec3(Facing.getStepX(), 0.0, Facing.getStepZ());
            BlockPos Base = Locker.getStoragePos();
            boolean Tall = Locker.isTall();
            BlockPos Upper = Base.above();
            Predicate<BlockPos> Ignored = Cell -> Cell.equals(Base) || Tall && Cell.equals(Upper);
            double[] Heights = Tall ? new double[]{Scp012Exposure.TallLowerWindowY, Scp012Exposure.TallUpperWindowY} : new double[]{Scp012Exposure.SmallWindowY};
            for (double Height : Heights) {
                Vec3 Spot = StorageGeometry.point(Base, Facing, StorageGeometry.CentreX, Height, Scp012Exposure.WindowZ);
                Vec3 Towards = Spot.subtract(Eye);
                double Distance = Towards.lengthSqr();
                if (Distance > Reach || Distance >= BestDistance || Eye.subtract(Spot).dot(Normal) <= 0.05) {
                    continue;
                }
                if (Towards.normalize().dot(Look) < Scp012Exposure.ViewCone) {
                    continue;
                }
                if (Scp012Exposure.clearSight(World, Eye, Spot, Ignored)) {
                    Best = Spot;
                    BestDistance = Distance;
                }
            }
        }
        return Best;
    }
}
