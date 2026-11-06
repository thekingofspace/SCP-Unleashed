package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;

public class Scp149DryNodes extends FlyNodeEvaluator {
    public static final int Clearance = 2;

    @Override
    public PathType getPathType(PathfindingContext Context, int X, int Y, int Z) {
        PathType Found = super.getPathType(Context, X, Y, Z);
        if (Found == PathType.WATER || Context.level().getFluidState(new BlockPos(X, Y, Z)).is(FluidTags.WATER)) {
            return PathType.WATER;
        }
        if (Found == PathType.OPEN || Found == PathType.WALKABLE) {
            for (int Depth = 1; Depth <= Clearance; Depth++) {
                if (Context.level().getFluidState(new BlockPos(X, Y - Depth, Z)).is(FluidTags.WATER)) {
                    return PathType.WATER_BORDER;
                }
            }
        }
        return Found;
    }
}
