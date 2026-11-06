package net.rai.scp_unleashed.scp280;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class Scp280Navigation extends GroundPathNavigation {
    public static final PathType LitPath = PathType.DANGER_OTHER;

    public Scp280Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        this.nodeEvaluator = new LightShyEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, MaxVisited);
    }

    public static class LightShyEvaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathTypeOfMob(PathfindingContext Context, int X, int Y, int Z, Mob Owner) {
            PathType Found = super.getPathTypeOfMob(Context, X, Y, Z, Owner);
            if (Owner instanceof Scp280Shade && standing(Found) && Scp280Light.isBright(Owner.level(), new BlockPos(X, Y, Z))) {
                return LitPath;
            }
            return Found;
        }

        private static boolean standing(PathType Found) {
            return Found == PathType.WALKABLE || Found == PathType.WALKABLE_DOOR || Found == PathType.DOOR_OPEN || Found == PathType.RAIL;
        }
    }
}
