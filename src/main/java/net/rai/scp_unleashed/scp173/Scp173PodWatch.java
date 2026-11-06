package net.rai.scp_unleashed.scp173;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class Scp173PodWatch extends Goal {
    public static final int Priority = 1;
    public static final int ScanInterval = 5;
    public static final double AlarmRange = 12.0;
    public static final double KeepNear = 3.5;
    public static final double KeepFar = 9.0;
    public static final double LoseRange = 28.0;
    public static final int SearchTicks = 200;
    public static final int HoldTicks = 10;
    public static final int AlarmGap = 50;
    public static final int AlarmSpread = 30;
    public static final int RepathTicks = 10;
    public static final int Vantages = 12;
    public static final double WatchModifier = 0.9;
    public static final float AlarmVolume = 0.6f;
    private final Scp131Pod Pod;
    @Nullable
    private Scp173Statue Target;
    private int Lost;
    private int AlarmLeft;
    private int Repath;

    public Scp173PodWatch(Scp131Pod Pod) {
        this.Pod = Pod;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public static void attach(Scp131Pod Pod) {
        for (WrappedGoal Wrapped : Pod.goalSelector.getAvailableGoals()) {
            if (Wrapped.getGoal() instanceof Scp173PodWatch) {
                return;
            }
        }
        Pod.goalSelector.addGoal(Priority, new Scp173PodWatch(Pod));
    }

    @Nullable
    private Scp173Statue noticed() {
        Level World = Pod.level();
        AABB Around = Pod.getBoundingBox().inflate(Scp173Sight.PodRange);
        List<Scp173Statue> Seen = World.getEntitiesOfClass(Scp173Statue.class, Around, Statue -> Statue.isAlive() && Scp173Sight.podSees(Pod, Statue));
        if (!Seen.isEmpty()) {
            return Seen.stream().min(Comparator.comparingDouble(Statue -> Statue.distanceToSqr(Pod))).orElse(null);
        }
        for (Scp131Pod Other : World.getEntitiesOfClass(Scp131Pod.class, Pod.getBoundingBox().inflate(AlarmRange), Other -> Other != Pod && Other.isAlive())) {
            Entity Focus = Other.getFocus();
            if (Focus instanceof Scp173Statue Statue && Statue.isAlive() && Statue.distanceToSqr(Pod) < LoseRange * LoseRange) {
                return Statue;
            }
        }
        return null;
    }

    @Override
    public boolean canUse() {
        if ((Pod.tickCount + Pod.getId()) % ScanInterval != 0 || !Pod.isAlive() || Pod.isPassenger()) {
            return false;
        }
        Target = noticed();
        return Target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return Target != null && Target.isAlive() && !Target.isRemoved() && Target.level() == Pod.level() && Pod.isAlive() && Lost < SearchTicks
                && Target.distanceToSqr(Pod) < LoseRange * LoseRange;
    }

    @Override
    public void start() {
        Lost = 0;
        AlarmLeft = 0;
        Repath = 0;
        if (Pod.session() != null) {
            Pod.session().end(false);
        }
        Pod.getNavigation().stop();
    }

    @Override
    public void stop() {
        Target = null;
        Pod.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (Target == null) {
            return;
        }
        Pod.setFocus(Target);
        Pod.alarm(HoldTicks);
        Pod.getLookControl().setLookAt(Target.getX(), Target.getEyeY(), Target.getZ(), 40.0f, 40.0f);
        boolean Sees = Scp173Sight.podSees(Pod, Target);
        Lost = Sees ? 0 : Lost + 1;
        if (--AlarmLeft <= 0) {
            AlarmLeft = AlarmGap + Pod.getRandom().nextInt(AlarmSpread);
            Pod.voice(Scp173Registry.PodAlarmLayers, AlarmVolume, 1.05f + Pod.getRandom().nextFloat() * 0.15f);
        }
        if (Sees) {
            Pod.getNavigation().stop();
            Repath = 0;
            return;
        }
        if (--Repath > 0 && !Pod.getNavigation().isDone()) {
            return;
        }
        Repath = RepathTicks;
        Vec3 Spot = vantage();
        if (Spot != null) {
            Pod.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, WatchModifier);
        } else {
            Pod.getNavigation().moveTo(Target, WatchModifier);
        }
    }

    @Nullable
    private Vec3 vantage() {
        if (Target == null || !(Pod.level() instanceof ServerLevel World)) {
            return null;
        }
        Vec3 Centre = Target.position();
        double Spread = (Pod.getId() % 7) * 0.9;
        double Start = Math.atan2(Pod.getZ() - Centre.z, Pod.getX() - Centre.x) + Spread * 0.15;
        double Radius = (KeepNear + KeepFar) * 0.5;
        List<Vec3> Found = new ArrayList<>();
        for (int Step = 0; Step < Vantages; Step++) {
            double Turn = Start + (Step % 2 == 0 ? 1 : -1) * ((Step + 1) / 2) * (Math.PI * 2.0 / Vantages);
            BlockPos Column = BlockPos.containing(Centre.x + Math.cos(Turn) * Radius, Centre.y, Centre.z + Math.sin(Turn) * Radius);
            BlockPos Stand = standable(World, Column);
            if (Stand == null) {
                continue;
            }
            Vec3 Eye = Vec3.atBottomCenterOf(Stand).add(0.0, Scp131Pod.EyeHeight, 0.0);
            for (Vec3 Point : Scp173Sight.samples(Target.getBoundingBox())) {
                if (Scp173Sight.clear(World, Eye, Point)) {
                    Found.add(Vec3.atBottomCenterOf(Stand));
                    break;
                }
            }
            if (Found.size() >= 3) {
                break;
            }
        }
        return Found.stream().min(Comparator.comparingDouble(Spot -> Spot.distanceToSqr(Pod.position()))).orElse(null);
    }

    @Nullable
    private static BlockPos standable(ServerLevel World, BlockPos Column) {
        for (int Dy = 2; Dy >= -3; Dy--) {
            BlockPos Spot = Column.above(Dy);
            BlockPos Below = Spot.below();
            if (World.getBlockState(Spot).getCollisionShape(World, Spot).isEmpty() && World.getFluidState(Spot).isEmpty()
                    && World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
                return Spot;
            }
        }
        return null;
    }
}
