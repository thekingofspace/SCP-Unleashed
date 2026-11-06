package net.rai.scp_unleashed.scp163;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class Scp163Navigation extends GroundPathNavigation {
    public Scp163Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new Walker();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    public static class Walker extends WalkNodeEvaluator {
        @Override
        public PathType getPathType(PathfindingContext Context, int X, int Y, int Z) {
            BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, Y, Z);
            if (Scp163Footing.isInstrument(Context.getBlockState(Cursor)) || Scp163Footing.isInstrument(Context.getBlockState(Cursor.move(Direction.DOWN)))) {
                return PathType.BLOCKED;
            }
            return super.getPathType(Context, X, Y, Z);
        }
    }
}
