package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class Scp457Navigation extends GroundPathNavigation {
    public static final PathType RainType = PathType.DANGER_OTHER;

    public Scp457Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new Evaluator();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    public static class Evaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathTypeOfMob(PathfindingContext Context, int X, int Y, int Z, Mob Owner) {
            PathType Found = super.getPathTypeOfMob(Context, X, Y, Z, Owner);
            if (Owner.getPathfindingMalus(Found) < 0.0f) {
                return Found;
            }
            BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
            for (int Dy = -1; Dy <= 1; Dy++) {
                if (Scp457Water.wet(Context.level().getFluidState(Cursor.set(X, Y + Dy, Z)))) {
                    return PathType.WATER;
                }
            }
            if (Scp457Water.rainCell(Owner.level(), Cursor.set(X, Y, Z))) {
                return RainType;
            }
            return Found;
        }
    }
}
