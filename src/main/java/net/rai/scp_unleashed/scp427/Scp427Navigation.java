package net.rai.scp_unleashed.scp427;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class Scp427Navigation extends GroundPathNavigation {
    public static final int CrawlClearance = 2;

    public Scp427Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new LowEvaluator();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    public static class LowEvaluator extends WalkNodeEvaluator {
        @Override
        public void prepare(PathNavigationRegion Region, Mob Owner) {
            super.prepare(Region, Owner);
            entityHeight = Math.min(entityHeight, CrawlClearance);
        }
    }
}
