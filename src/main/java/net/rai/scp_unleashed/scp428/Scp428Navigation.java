package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp428Navigation extends GroundPathNavigation {
    public static final int Footprint = 1;
    public static final int Headroom = 2;
    public static final double HoverReach = 0.42;
    public static final double HoverRise = 1.6;
    public static final double PanelTouch = 1.0e-4;
    public static final double SkipNear = 0.5;

    public Scp428Navigation(Mob Walker, Level World) {
        super(Walker, World);
        setCanOpenDoors(true);
        setCanPassDoors(true);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new Nodes();
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    public double speed() {
        return speedModifier;
    }

    @Nullable
    public Scp428Doorwork.Doorway blockingDoor(BlockPos Destination) {
        if (!(nodeEvaluator instanceof Nodes Walker)) {
            return null;
        }
        Path Kept = path;
        path = null;
        Path Through;
        Walker.Through = true;
        try {
            Through = createPath(Destination, 1);
        } finally {
            Walker.Through = false;
            path = Kept;
        }
        if (Through == null || !Through.canReach()) {
            return null;
        }
        for (int Index = 0; Index < Through.getNodeCount(); Index++) {
            Node Step = Through.getNode(Index);
            for (int Lift = 0; Lift <= 1; Lift++) {
                BlockPos Pos = new BlockPos(Step.x, Step.y + Lift, Step.z);
                BlockState State = level.getBlockState(Pos);
                if (Scp428Doorwork.isDoorway(State) && !Scp428Doorwork.isOpen(State)) {
                    return Scp428Doorwork.doorwayAt(level, Pos);
                }
            }
        }
        return null;
    }

    @Override
    public void stop() {
        super.stop();
        if (mob.getMoveControl() instanceof Scp428Hover Hover) {
            Hover.halt();
        }
    }

    @Override
    protected boolean canUpdatePath() {
        return mob instanceof Scp428Core || super.canUpdatePath();
    }

    @Override
    public void tick() {
        if (!(mob instanceof Scp428Core Core)) {
            super.tick();
            return;
        }
        tick++;
        if (hasDelayedRecomputation) {
            recomputePath();
        }
        if (isDone()) {
            return;
        }
        Node Next = path.getNextNode();
        double Dx = Math.abs(mob.getX() - (Next.x + 0.5));
        double Dz = Math.abs(mob.getZ() - (Next.z + 0.5));
        double Dy = Math.abs(mob.getY() - Core.clearance() - Next.y);
        maxDistanceToWaypoint = (float) HoverReach;
        if (Dx <= HoverReach && Dz <= HoverReach && Dy < HoverRise) {
            path.advance();
        }
        skipPassed();
        doStuckDetection(mob.position());
        if (!isDone()) {
            Node Wanted = path.getNextNode();
            BlockPos Cell = Wanted.asBlockPos();
            boolean AlongX = Math.abs(Wanted.x + 0.5 - mob.getX()) >= Math.abs(Wanted.z + 0.5 - mob.getZ());
            double Across = gapCentre(Cell, AlongX);
            mob.getMoveControl().setWantedPosition(AlongX ? Wanted.x + 0.5 : Cell.getX() + Across, Wanted.y, AlongX ? Cell.getZ() + Across : Wanted.z + 0.5, speedModifier);
        }
    }

    private void skipPassed() {
        while (!path.isDone() && path.getNextNodeIndex() + 1 < path.getNodeCount()) {
            Node Next = path.getNextNode();
            Node After = path.getNode(path.getNextNodeIndex() + 1);
            if (After.y != Next.y) {
                return;
            }
            double Ax = Next.x + 0.5 - mob.getX();
            double Az = Next.z + 0.5 - mob.getZ();
            double Bx = After.x + 0.5 - mob.getX();
            double Bz = After.z + 0.5 - mob.getZ();
            double Near = Ax * Ax + Az * Az;
            double Far = Bx * Bx + Bz * Bz;
            if (Ax * Bx + Az * Bz >= 0.0 || Far >= Near && Near >= SkipNear) {
                return;
            }
            path.advance();
        }
    }

    public double gapCentre(BlockPos Cell, boolean AlongX) {
        double Low = 0.0;
        double High = 1.0;
        for (int Lift = 0; Lift <= 1; Lift++) {
            BlockPos Pos = Cell.above(Lift);
            BlockState State = level.getBlockState(Pos);
            if (!Scp428Doorwork.isDoorway(State) || !Scp428Doorwork.isOpen(State)) {
                continue;
            }
            VoxelShape Shape = State.getCollisionShape(level, Pos);
            for (AABB Part : Shape.toAabbs()) {
                double Min = AlongX ? Part.minZ : Part.minX;
                double Max = AlongX ? Part.maxZ : Part.maxX;
                if (Min <= PanelTouch && Max < 1.0 - PanelTouch) {
                    Low = Math.max(Low, Max);
                } else if (Max >= 1.0 - PanelTouch && Min > PanelTouch) {
                    High = Math.min(High, Min);
                }
            }
        }
        return High > Low ? (Low + High) * 0.5 : 0.5;
    }

    public static class Nodes extends WalkNodeEvaluator {
        public boolean Through;

        @Override
        public void prepare(PathNavigationRegion Region, Mob Walker) {
            super.prepare(Region, Walker);
            entityWidth = Footprint;
            entityDepth = Footprint;
            entityHeight = Math.min(entityHeight, Headroom);
        }

        @Override
        public PathType getPathType(PathfindingContext Context, int X, int Y, int Z) {
            PathType Found = super.getPathType(Context, X, Y, Z);
            if (Through) {
                BlockState State = Context.getBlockState(new BlockPos(X, Y, Z));
                if (Scp428Doorwork.isDoorway(State) && !Scp428Doorwork.isOpen(State)) {
                    return PathType.DOOR_OPEN;
                }
            }
            if (Found == PathType.FENCE && canOpenDoors()) {
                BlockState State = Context.getBlockState(new BlockPos(X, Y, Z));
                if (State.getBlock() instanceof FenceGateBlock && !State.getValue(FenceGateBlock.OPEN)) {
                    return PathType.DOOR_WOOD_CLOSED;
                }
            }
            return Found;
        }
    }
}
