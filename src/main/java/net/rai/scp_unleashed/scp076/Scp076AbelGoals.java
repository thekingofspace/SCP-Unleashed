package net.rai.scp_unleashed.scp076;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class Scp076AbelGoals {
    public static final int RepathEvery = 6;
    public static final int ReturnRepath = 20;

    private Scp076AbelGoals() {
    }

    public static class Hunt extends Goal {
        private final Scp076Abel Owner;
        private int Repath;

        public Hunt(Scp076Abel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Owner.busy() && Scp076Abel.isPrey(Owner, Owner.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Prey = Owner.getTarget();
            if (Prey != null) {
                Owner.hunt(this, Prey);
            }
        }

        public void chase(LivingEntity Prey, double Speed) {
            if (--Repath <= 0 || Owner.getNavigation().isDone()) {
                Repath = RepathEvery + Owner.getRandom().nextInt(3);
                Owner.getNavigation().moveTo(Prey, Speed);
            }
        }
    }

    public static class ReturnHome extends Goal {
        private final Scp076Abel Owner;
        private int Repath;

        public ReturnHome(Scp076Abel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Owner.returning() && Owner.getTarget() == null && Owner.homePoint() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            Vec3 Home = Owner.homePoint();
            if (Home == null) {
                return;
            }
            if (--Repath <= 0 || Owner.getNavigation().isDone()) {
                Repath = ReturnRepath;
                Owner.getNavigation().moveTo(Home.x, Home.y, Home.z, Scp076Abel.ReturnSpeed);
            }
        }
    }

    public static class Prowl extends WaterAvoidingRandomStrollGoal {
        private final Scp076Abel Owner;

        public Prowl(Scp076Abel Owner) {
            super(Owner, Scp076Abel.ProwlSpeed);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.busy() && !Owner.returning() && Owner.getTarget() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.busy() && !Owner.returning() && Owner.getTarget() == null && super.canContinueToUse();
        }
    }
}
