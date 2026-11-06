package net.rai.scp_unleashed.scp337;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import java.util.EnumSet;

public final class Scp337Goals {
    public static final int RepathTicks = 12;
    public static final float TurnSpeed = 3.0f;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.7;
    public static final int WanderInterval = 220;

    private Scp337Goals() {
    }

    public static class Creep extends Goal {
        private final Scp337Mass Mass;
        private int Repath;

        public Creep(Scp337Mass Mass) {
            this.Mass = Mass;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Mass.getTarget();
            return Target != null && Target.isAlive() && Mass.sated() <= 0;
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
            Mass.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Mass.getTarget();
            if (Target == null) {
                return;
            }
            double Reach = Scp337Quarry.reach(Mass, Target);
            boolean Seen = Mass.hasLineOfSight(Target);
            if (Reach <= Scp337Mass.LashRange) {
                turnToward(Target);
            }
            if (Reach <= Scp337Mass.HoldRange && Seen) {
                Mass.getNavigation().stop();
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                Mass.getNavigation().moveTo(Target, ChaseModifier);
            }
        }

        private void turnToward(LivingEntity Target) {
            float Wanted = (float) (Mth.atan2(Target.getZ() - Mass.getZ(), Target.getX() - Mass.getX()) * Mth.RAD_TO_DEG) - 90.0f;
            float Yaw = Mth.approachDegrees(Mass.getYRot(), Wanted, TurnSpeed);
            Mass.setYRot(Yaw);
            Mass.yBodyRot = Yaw;
            Mass.yHeadRot = Yaw;
        }
    }

    public static class Wander extends WaterAvoidingRandomStrollGoal {
        private final Scp337Mass Mass;

        public Wander(Scp337Mass Mass) {
            super(Mass, WanderModifier);
            this.Mass = Mass;
            setInterval(WanderInterval);
        }

        @Override
        public boolean canUse() {
            return Mass.getTarget() == null && super.canUse();
        }
    }
}
