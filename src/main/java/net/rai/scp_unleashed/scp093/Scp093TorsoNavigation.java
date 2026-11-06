package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.Nullable;

public class Scp093TorsoNavigation extends GroundPathNavigation {
    public static final int NodeBudget = 400;
    public static final double ArrivalShare = 0.5;
    @Nullable
    private BlockPos Heading;

    public Scp093TorsoNavigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisitedNodes) {
        nodeEvaluator = new Scp093TorsoNodes();
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanWalkOverFences(true);
        return new PathFinder(nodeEvaluator, Math.min(MaxVisitedNodes, NodeBudget));
    }

    @Nullable
    @Override
    public Path createPath(BlockPos Pos, int Accuracy) {
        Heading = Pos;
        return super.createPath(Pos, Accuracy);
    }

    @Nullable
    @Override
    public Path createPath(Entity Target, int Accuracy) {
        Heading = Target.blockPosition();
        return super.createPath(Target, Accuracy);
    }

    @Override
    public boolean moveTo(Entity Target, double Speed) {
        Path Found = createPath(Target, 0);
        if (Found != null && moveTo(Found, Speed)) {
            return true;
        }
        Heading = Target.blockPosition();
        speedModifier = Speed;
        return true;
    }

    public boolean headFor(double X, double Y, double Z, double Speed) {
        BlockPos Spot = BlockPos.containing(X, Y, Z);
        Path Found = createPath(Spot, 1);
        if (Found != null && moveTo(Found, Speed)) {
            return true;
        }
        Heading = Spot;
        speedModifier = Speed;
        return true;
    }

    @Override
    public void stop() {
        super.stop();
        Heading = null;
    }

    @Nullable
    public BlockPos heading() {
        return Heading;
    }

    public boolean idle() {
        return isDone() && Heading == null;
    }

    @Override
    public void tick() {
        if (!isDone()) {
            super.tick();
            return;
        }
        if (Heading == null) {
            return;
        }
        double Reach = Math.max(mob.getBbWidth() * ArrivalShare, 1.0);
        boolean Over = mob.getY() > Heading.getY() && BlockPos.containing(Heading.getX(), mob.getY(), Heading.getZ()).closerToCenterThan(mob.position(), Reach);
        if (Heading.closerToCenterThan(mob.position(), Reach) || Over) {
            Heading = null;
            return;
        }
        mob.getMoveControl().setWantedPosition(Heading.getX() + 0.5, Heading.getY(), Heading.getZ() + 0.5, speedModifier);
    }
}
