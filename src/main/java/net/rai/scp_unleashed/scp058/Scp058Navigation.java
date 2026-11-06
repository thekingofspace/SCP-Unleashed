package net.rai.scp_unleashed.scp058;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

public class Scp058Navigation extends GroundPathNavigation {
    public static final int StallTicks = 40;
    public static final int RestTicks = 60;
    public static final double CloseStep = 0.05;
    public static final double RiseStep = 0.05;

    @Nullable
    private BlockPos Chase;
    private double BestFlat;
    private double BestY;
    private int Stall;
    private int Rest;

    public Scp058Navigation(Mob Owner, Level World) {
        super(Owner, World);
    }

    @Override
    public boolean moveTo(@Nullable Path Route, double Pace) {
        Chase = null;
        return super.moveTo(Route, Pace);
    }

    @Override
    public boolean moveTo(Entity Quarry, double Pace) {
        Path Route = createPath(Quarry, 0);
        boolean Moving = Route != null && super.moveTo(Route, Pace);
        if ((Route != null && Route.canReach()) || Rest > 0) {
            Chase = null;
            return Moving;
        }
        BlockPos Next = Quarry.blockPosition();
        if (Chase == null) {
            Stall = 0;
            BestY = mob.getY();
            BestFlat = flat(Next);
        } else if (!Chase.equals(Next)) {
            BestFlat = flat(Next);
        }
        Chase = Next;
        speedModifier = Pace;
        return true;
    }

    @Override
    public void stop() {
        super.stop();
        Chase = null;
    }

    @Override
    public void tick() {
        if (Rest > 0) {
            Rest--;
        }
        if (!isDone()) {
            super.tick();
            return;
        }
        if (Chase == null) {
            return;
        }
        if (arrived(Chase)) {
            Chase = null;
            return;
        }
        double Flat = flat(Chase);
        boolean Closer = Flat < BestFlat - CloseStep;
        boolean Higher = mob.getY() > BestY + RiseStep;
        if (Closer) {
            BestFlat = Flat;
        }
        if (Higher) {
            BestY = mob.getY();
        }
        Stall = Closer || Higher ? 0 : Stall + 1;
        if (Stall >= StallTicks) {
            giveUp();
            return;
        }
        mob.getMoveControl().setWantedPosition(Chase.getX() + 0.5, Chase.getY(), Chase.getZ() + 0.5, speedModifier);
    }

    private double flat(BlockPos Goal) {
        double DeltaX = Goal.getX() + 0.5 - mob.getX();
        double DeltaZ = Goal.getZ() + 0.5 - mob.getZ();
        return Math.sqrt(DeltaX * DeltaX + DeltaZ * DeltaZ);
    }

    private boolean arrived(BlockPos Goal) {
        double Reach = Math.max(mob.getBbWidth(), 1.0);
        if (Goal.closerToCenterThan(mob.position(), Reach)) {
            return true;
        }
        return mob.getY() > Goal.getY() && BlockPos.containing(Goal.getX(), mob.getY(), Goal.getZ()).closerToCenterThan(mob.position(), Reach);
    }

    public void giveUp() {
        stop();
        Stall = 0;
        Rest = RestTicks;
    }

    public boolean resting() {
        return Rest > 0;
    }
}
