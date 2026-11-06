package net.rai.scp_unleashed.scp492;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp492Goals {
    public static final int FleeReach = 10;
    public static final int FleeVertical = 4;
    public static final int WaterReach = 10;
    public static final int Replan = 20;
    public static final double CuriousRange = 24.0;
    public static final double CuriousStart = 5.5;
    public static final double CuriousStop = 3.5;
    public static final int CuriousTicks = 300;

    private Scp492Goals() {
    }

    public static class Flee extends Goal {
        private final Scp492Figure Figure;
        private int Wait;

        public Flee(Scp492Figure Figure) {
            this.Figure = Figure;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        private boolean frightened() {
            return Figure.isAlive() && !Figure.isTorn() && (Figure.isOnFire() || Figure.fearSource() != null);
        }

        @Override
        public boolean canUse() {
            return frightened();
        }

        @Override
        public boolean canContinueToUse() {
            return frightened();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Figure.setInSittingPose(false);
            plan();
        }

        @Override
        public void tick() {
            Wait--;
            if (Wait <= 0 || Figure.getNavigation().isDone()) {
                plan();
            }
        }

        @Override
        public void stop() {
            Figure.getNavigation().stop();
            if (Figure.isOrderedToSit()) {
                Figure.setInSittingPose(true);
            }
        }

        private void plan() {
            Wait = Replan;
            if (Figure.isOnFire()) {
                BlockPos Water = water();
                if (Water != null) {
                    Figure.getNavigation().moveTo(Water.getX() + 0.5, Water.getY(), Water.getZ() + 0.5, Scp492Figure.FleeModifier);
                    return;
                }
            }
            Vec3 From = Figure.fearSource() != null ? Figure.fearSource() : Figure.position();
            Vec3 Away = DefaultRandomPos.getPosAway(Figure, FleeReach, FleeVertical, From);
            if (Away == null) {
                Figure.getNavigation().stop();
                return;
            }
            Figure.getNavigation().moveTo(Away.x, Away.y, Away.z, Scp492Figure.FleeModifier);
        }

        @Nullable
        private BlockPos water() {
            if (Figure.isInWater()) {
                return null;
            }
            return BlockPos.findClosestMatch(Figure.blockPosition(), WaterReach, 3, Pos -> Figure.level().getFluidState(Pos).is(FluidTags.WATER)).orElse(null);
        }
    }

    public static class Curious extends Goal {
        private final Scp492Figure Figure;
        @Nullable
        private Player Target;
        private int Ticks;

        public Curious(Scp492Figure Figure) {
            this.Figure = Figure;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player pick() {
            Player Best = null;
            double BestDistance = CuriousRange * CuriousRange;
            for (Player User : Figure.level().players()) {
                if (User.isSpectator() || !User.isAlive()) {
                    continue;
                }
                double Distance = User.distanceToSqr(Figure);
                if (User.getUUID().equals(Figure.sought())) {
                    Distance *= 0.25;
                }
                if (Distance < BestDistance) {
                    Best = User;
                    BestDistance = Distance;
                }
            }
            return Best;
        }

        @Override
        public boolean canUse() {
            if (Figure.isTame() || !Figure.free() || Figure.isAfraid()) {
                return false;
            }
            Player Found = pick();
            if (Found == null || Found.distanceTo(Figure) <= CuriousStart || Scp492Fear.frightening(Found)) {
                return false;
            }
            Target = Found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && Target.isAlive() && !Figure.isTame() && Figure.free() && !Figure.isAfraid() && Ticks < CuriousTicks
                    && Target.distanceTo(Figure) > CuriousStop && Target.distanceTo(Figure) < CuriousRange * 1.5;
        }

        @Override
        public void start() {
            Ticks = 0;
            if (Target != null) {
                Figure.getNavigation().moveTo(Target, Scp492Figure.ApproachModifier);
            }
        }

        @Override
        public void tick() {
            Ticks++;
            if (Target == null) {
                return;
            }
            Figure.getLookControl().setLookAt(Target, 20.0f, 20.0f);
            if (Ticks % 10 == 0) {
                Figure.getNavigation().moveTo(Target, Scp492Figure.ApproachModifier);
            }
        }

        @Override
        public void stop() {
            Target = null;
            Figure.getNavigation().stop();
        }
    }
}
