package net.rai.scp_unleashed.scp469;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Scp469Navigation extends GroundPathNavigation {
    public static final float Waypoint = Scp469Angel.FoldWidth / 2.0f;

    public Scp469Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    public static final class Squeeze extends WalkNodeEvaluator {
        @Override
        public void prepare(PathNavigationRegion Region, Mob Owner) {
            super.prepare(Region, Owner);
            entityWidth = 1;
            entityHeight = 2;
            entityDepth = 1;
        }
    }

    public static final class Narrow extends Path {
        public Narrow(List<Node> Nodes, BlockPos Target, boolean Reached) {
            super(Nodes, Target, Reached);
        }

        public static Narrow of(Path Found) {
            if (Found instanceof Narrow Already) {
                return Already;
            }
            List<Node> Nodes = new ArrayList<>();
            for (int Index = 0; Index < Found.getNodeCount(); Index++) {
                Nodes.add(Found.getNode(Index));
            }
            Narrow Made = new Narrow(Nodes, Found.getTarget(), Found.canReach());
            Made.setNextNodeIndex(Found.getNextNodeIndex());
            return Made;
        }

        @Override
        public Vec3 getEntityPosAtNode(Entity Owner, int Index) {
            Node Step = getNode(Index);
            return new Vec3(Step.x + 0.5, Step.y, Step.z + 0.5);
        }
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new Squeeze();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    @Nullable
    @Override
    protected Path createPath(Set<BlockPos> Targets, int RegionOffset, boolean OffsetUpward, int Accuracy, float FollowRange) {
        Path Found = super.createPath(Targets, RegionOffset, OffsetUpward, Accuracy, FollowRange);
        return Found == null ? null : Narrow.of(Found);
    }

    @Override
    public boolean moveTo(@Nullable Path Route, double Speed) {
        return super.moveTo(Route == null ? null : Narrow.of(Route), Speed);
    }

    @Override
    protected void followThePath() {
        Vec3 Here = getTempMobPos();
        maxDistanceToWaypoint = Waypoint;
        Vec3i Next = path.getNextNodePos();
        double Dx = Math.abs(mob.getX() - (Next.getX() + 0.5));
        double Dy = Math.abs(mob.getY() - Next.getY());
        double Dz = Math.abs(mob.getZ() - (Next.getZ() + 0.5));
        boolean Reached = Dx <= maxDistanceToWaypoint && Dz <= maxDistanceToWaypoint && Dy < 1.0;
        if (Reached || canCutCorner(path.getNextNode().type) && aheadIsBetter(Here)) {
            path.advance();
        }
        doStuckDetection(Here);
    }

    private boolean aheadIsBetter(Vec3 Here) {
        if (path.getNextNodeIndex() + 1 >= path.getNodeCount()) {
            return false;
        }
        Vec3 Next = Vec3.atBottomCenterOf(path.getNextNodePos());
        if (!Here.closerThan(Next, 2.0)) {
            return false;
        }
        Vec3 After = Vec3.atBottomCenterOf(path.getNodePos(path.getNextNodeIndex() + 1));
        Vec3 ToNext = Next.subtract(Here);
        Vec3 ToAfter = After.subtract(Here);
        double NearNext = ToNext.lengthSqr();
        double NearAfter = ToAfter.lengthSqr();
        if (!(NearAfter < NearNext) && !(NearNext < 0.5)) {
            return false;
        }
        return ToAfter.normalize().dot(ToNext.normalize()) < 0.0;
    }
}
