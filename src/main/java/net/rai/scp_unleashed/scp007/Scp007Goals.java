package net.rai.scp_unleashed.scp007;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import java.util.EnumSet;

public final class Scp007Goals {
    public static final int GazeChance = 240;

    private Scp007Goals() {
    }

    public static class Gaze extends Goal {
        private final Scp007Entity Owner;

        public Gaze(Scp007Entity Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean calm() {
            return !Owner.isInWater() && !Owner.isPassenger() && !Owner.isLeashed() && !Owner.threatened();
        }

        @Override
        public boolean canUse() {
            if (Owner.isSeated()) {
                return true;
            }
            if (Owner.isRising() || !Owner.onGround() || !Owner.readyToGaze() || Owner.getRandom().nextInt(GazeChance) != 0) {
                return false;
            }
            return calm();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.isSeated() && calm();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
            if (!Owner.isSeated()) {
                Owner.sitDown(Owner.gazeLength());
            }
        }

        @Override
        public void stop() {
            Owner.standUp();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Owner.getNavigation().stop();
            Owner.setYHeadRot(Owner.yBodyRot);
            Owner.setXRot(0.0f);
        }
    }

    public static class Wander extends WaterAvoidingRandomStrollGoal {
        private final Scp007Entity Owner;

        public Wander(Scp007Entity Owner, double Speed, int Interval) {
            super(Owner, Speed);
            this.Owner = Owner;
            setInterval(Interval);
        }

        @Override
        public boolean canUse() {
            return !Owner.isSettled() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.isSettled() && super.canContinueToUse();
        }
    }
}
