package net.rai.scp_unleashed.scp073;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp073Goals {
    public static final int WanderReach = 10;
    public static final int WanderVertical = 7;
    public static final int WanderTries = 4;
    public static final int ShoreReach = 14;
    public static final int ShoreVertical = 4;
    public static final int ShoreRecheck = 20;
    public static final int DryTicks = 10;

    private Scp073Goals() {
    }

    public static class Wander extends RandomStrollGoal {
        private final Scp073Cain Cain;

        public Wander(Scp073Cain Owner, double Speed, int Interval) {
            super(Owner, Speed, Interval);
            Cain = Owner;
        }

        @Override
        public boolean canUse() {
            return !Cain.isInWater() && !Cain.isAttending() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Cain.isInWater() && !Cain.isAttending() && super.canContinueToUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            boolean Desert = Scp073Cain.isDesert(Cain.level(), Cain.blockPosition());
            Vec3 Fallback = null;
            for (int Attempt = 0; Attempt < WanderTries; Attempt++) {
                Vec3 Found = LandRandomPos.getPos(Cain, WanderReach, WanderVertical);
                if (Found == null) {
                    continue;
                }
                BlockPos Pos = BlockPos.containing(Found);
                if (!Desert || Scp073Cain.isDesert(Cain.level(), Pos)) {
                    return Found;
                }
                if (Fallback == null) {
                    Fallback = Found;
                }
            }
            return Desert ? null : Fallback;
        }
    }

    public static class Attend extends Goal {
        private final Scp073Cain Cain;

        public Attend(Scp073Cain Owner) {
            Cain = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Cain.isInWater() && Cain.isAttending();
        }

        @Override
        public void start() {
            Cain.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player Listener = Cain.listener();
            if (Listener != null) {
                Cain.getLookControl().setLookAt(Listener, 30.0f, 30.0f);
            }
        }
    }

    public static class LeaveWater extends Goal {
        private final Scp073Cain Cain;
        @Nullable
        private BlockPos Shore;
        private int Recheck;
        private int Dry;

        public LeaveWater(Scp073Cain Owner) {
            Cain = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Cain.isInWater() && Cain.level() instanceof ServerLevel;
        }

        @Override
        public boolean canContinueToUse() {
            return Dry < DryTicks;
        }

        @Override
        public void start() {
            Dry = 0;
            Recheck = 0;
            Cain.setPathfindingMalus(PathType.WATER, 0.0f);
        }

        @Override
        public void stop() {
            Cain.setPathfindingMalus(PathType.WATER, -1.0f);
            Cain.getNavigation().stop();
            Shore = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Dry = Cain.isInWater() ? 0 : Dry + 1;
            if (--Recheck <= 0 || Shore == null || Cain.getNavigation().isDone()) {
                Recheck = ShoreRecheck;
                Shore = findShore();
                if (Shore != null) {
                    Cain.getNavigation().moveTo(Shore.getX() + 0.5, Shore.getY(), Shore.getZ() + 0.5, Scp073Cain.EscapeSpeed);
                }
            }
            if (Shore != null && Cain.getNavigation().isDone()) {
                Cain.getMoveControl().setWantedPosition(Shore.getX() + 0.5, Shore.getY(), Shore.getZ() + 0.5, Scp073Cain.EscapeSpeed);
            }
            if (Shore != null && Cain.isInWater() && Shore.getY() >= Cain.getY() - 0.2 && Cain.horizontalCollision) {
                Cain.getJumpControl().jump();
            }
        }

        @Nullable
        private BlockPos findShore() {
            if (!(Cain.level() instanceof ServerLevel World)) {
                return null;
            }
            BlockPos Centre = Cain.blockPosition();
            for (int Radius = 1; Radius <= ShoreReach; Radius++) {
                BlockPos Best = null;
                double BestScore = Double.MAX_VALUE;
                for (int Dx = -Radius; Dx <= Radius; Dx++) {
                    for (int Dz = -Radius; Dz <= Radius; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                            continue;
                        }
                        for (int Dy = ShoreVertical; Dy >= -ShoreVertical; Dy--) {
                            BlockPos Candidate = Centre.offset(Dx, Dy, Dz);
                            if (!LeadPlanner.isDryStandable(World, Candidate) || !World.getFluidState(Candidate.below()).isEmpty()) {
                                continue;
                            }
                            double Score = Candidate.distSqr(Centre) - (World.getBlockState(Candidate.below()).is(BlockTags.SAND) ? 4.0 : 0.0);
                            if (Score < BestScore) {
                                BestScore = Score;
                                Best = Candidate;
                            }
                            break;
                        }
                    }
                }
                if (Best != null) {
                    return Best;
                }
            }
            return null;
        }
    }
}
