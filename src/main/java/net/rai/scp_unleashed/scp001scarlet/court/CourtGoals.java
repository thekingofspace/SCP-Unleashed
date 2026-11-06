package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class CourtGoals {
    private CourtGoals() {
    }

    public static class Melee extends Goal {
        protected final CourtMinion Owner;
        private final double Speed;
        private final int Cooldown;
        private final int Length;
        private int Wait;
        private int Repath;

        public Melee(CourtMinion Owner, double Speed, int Cooldown, int Length) {
            this.Owner = Owner;
            this.Speed = Speed;
            this.Cooldown = Cooldown;
            this.Length = Length;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        protected boolean allowed() {
            return true;
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Target != null && CourtFx.validTarget(Target) && allowed() && !Owner.rising();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
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
            LivingEntity Target = Owner.getTarget();
            if (Target == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Wait > 0) {
                Wait--;
            }
            boolean Close = Owner.distanceToSqr(Target) <= Owner.reachSqr(Target);
            if (Owner.busy()) {
                Owner.getNavigation().stop();
                return;
            }
            if (Close) {
                Owner.getNavigation().stop();
                if (Wait <= 0) {
                    Owner.startAction(CourtMinion.Attack, Length);
                    Wait = Cooldown;
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = 8 + Owner.getRandom().nextInt(6);
                Owner.getNavigation().moveTo(Target, Speed);
            }
        }
    }

    public static class KeepDistance extends Goal {
        private final CourtMinion Owner;
        private final double Near;
        private final double Far;
        private final double Speed;
        private int Repath;

        public KeepDistance(CourtMinion Owner, double Near, double Far, double Speed) {
            this.Owner = Owner;
            this.Near = Near;
            this.Far = Far;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Target != null && CourtFx.validTarget(Target) && !Owner.rising();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null || Owner.busy()) {
                return;
            }
            Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (--Repath > 0) {
                return;
            }
            Repath = 10;
            double Distance = Owner.distanceTo(Target);
            if (Distance > Far) {
                Owner.getNavigation().moveTo(Target, Speed);
            } else if (Distance < Near) {
                Vec3 Away = CourtFx.flat(Owner.position().subtract(Target.position()));
                Vec3 Goal = Owner.position().add(Away.scale(Near - Distance + 3.0));
                Owner.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, Speed * 1.15);
            } else {
                Owner.getNavigation().stop();
            }
        }
    }
}
