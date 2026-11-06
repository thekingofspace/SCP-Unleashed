package net.rai.scp_unleashed.scp538;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class Scp538Navigation extends GroundPathNavigation {
    public Scp538Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        this.nodeEvaluator = new LitNodes();
        this.nodeEvaluator.setCanPassDoors(false);
        this.nodeEvaluator.setCanOpenDoors(false);
        return new PathFinder(this.nodeEvaluator, MaxVisited);
    }

    public static class LitNodes extends WalkNodeEvaluator {
        @Override
        public PathType getPathTypeOfMob(PathfindingContext Context, int X, int Y, int Z, Mob Walker) {
            if (!Scp538Light.bearable(Scp538Light.at(Walker.level(), new BlockPos(X, Y, Z)))) {
                return PathType.BLOCKED;
            }
            return super.getPathTypeOfMob(Context, X, Y, Z, Walker);
        }
    }
}
