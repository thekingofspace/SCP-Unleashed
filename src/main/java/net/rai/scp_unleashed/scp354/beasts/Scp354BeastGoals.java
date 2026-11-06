package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp354BeastGoals {
    private Scp354BeastGoals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp354Beast Holder;

        public HoldStill(Scp354Beast Holder) {
            this.Holder = Holder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Holder.locksMovement();
        }

        @Override
        public void start() {
            Holder.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Holder.getNavigation().stop();
        }
    }

    public static final class Chase extends MeleeAttackGoal {
        private final Scp354Beast Hunter;

        public Chase(Scp354Beast Hunter, double Speed) {
            super(Hunter, Speed, true);
            this.Hunter = Hunter;
        }

        @Override
        public boolean canUse() {
            return !Hunter.locksMovement() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Hunter.locksMovement() && super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
        }
    }

    public static final class Hunt extends TargetGoal {
        private final Scp354Beast Hunter;
        private final TargetingConditions Sight;
        private int Rescan;

        public Hunt(Scp354Beast Hunter) {
            super(Hunter, true, false);
            this.Hunter = Hunter;
            this.Sight = TargetingConditions.forCombat().range(Hunter.acquireRange()).selector(Found -> Scp354Beast.isQuarry(Hunter, Found));
            setFlags(EnumSet.of(Goal.Flag.TARGET));
            setUnseenMemoryTicks(Scp354Beast.GiveUpTicks);
        }

        @Override
        public boolean canUse() {
            if (Hunter.isEmerging() && Hunter.getTarget() != null) {
                return false;
            }
            if (Hunter.getRandom().nextInt(reducedTickDelay(Scp354Beast.ScanTicks)) != 0) {
                return false;
            }
            targetMob = closest();
            return targetMob != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Hunter.getTarget();
            if (Current != null && !Scp354Beast.isQuarry(Hunter, Current) && Current.isAlive() && Current != Hunter.getLastHurtByMob()) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void start() {
            Hunter.setTarget(targetMob);
            Rescan = Scp354Beast.ScanTicks;
            super.start();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (--Rescan > 0) {
                return;
            }
            Rescan = Scp354Beast.ScanTicks;
            LivingEntity Current = Hunter.getTarget();
            LivingEntity Closer = closest();
            if (Closer == null || Closer == Current) {
                return;
            }
            if (Current == null || !Current.isAlive() || !Scp354Beast.isQuarry(Hunter, Current)
                    || Hunter.distanceToSqr(Closer) < Hunter.distanceToSqr(Current) * Scp354Beast.SwitchShare) {
                Hunter.setTarget(Closer);
                targetMob = Closer;
            }
        }

        @Nullable
        public LivingEntity closest() {
            double Range = Hunter.acquireRange();
            AABB Area = Hunter.getBoundingBox().inflate(Range, Scp354Beast.AcquireHeight, Range);
            return Hunter.level().getNearestEntity(Hunter.level().getEntitiesOfClass(LivingEntity.class, Area, Found -> Scp354Beast.isQuarry(Hunter, Found)), Sight,
                    Hunter, Hunter.getX(), Hunter.getEyeY(), Hunter.getZ());
        }
    }
}
