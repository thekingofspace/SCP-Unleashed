package net.rai.scp_unleashed.scp093;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public final class Scp093TorsoGoals {
    public static final int RepathTicks = 12;
    public static final int RepathSpread = 6;
    public static final double DirectReach = 12.0;
    public static final double ArriveRange = 3.5;
    public static final double SearchRadius = 10.0;
    public static final int SearchTicks = 80;

    private Scp093TorsoGoals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp093Torso Holder;

        public HoldStill(Scp093Torso Holder) {
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
            Holder.setDeltaMovement(Holder.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    public static final class Engage extends Goal {
        private final Scp093Torso Brute;
        private int Repath;
        private int Searching;
        @Nullable
        private Vec3 Search;

        public Engage(Scp093Torso Brute) {
            this.Brute = Brute;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Brute.getTarget();
            return Target != null && Target.isAlive() && !Brute.locksMovement();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
            Searching = 0;
            Search = null;
        }

        @Override
        public void stop() {
            Brute.getNavigation().stop();
            Search = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Brute.getTarget();
            if (Target == null) {
                return;
            }
            boolean Sees = Brute.getSensing().hasLineOfSight(Target);
            boolean Knows = Brute.senses(Target);
            if (Knows) {
                Brute.getLookControl().setLookAt(Target, Scp093Torso.HeadTurn, Brute.getMaxHeadXRot());
            }
            double Reach = Brute.reachTo(Target);
            boolean Even = Math.abs(Target.getY() - Brute.getY()) < Scp093Torso.EvenHeight;
            if (Sees && Even && Reach <= Scp093Torso.SwipeReach) {
                Brute.getNavigation().stop();
                Search = null;
                Searching = 0;
                Repath = 0;
                Brute.turnToward(Target.getX(), Target.getZ());
                if (Brute.facingError(Target) <= Scp093Torso.AimTolerance) {
                    if (Brute.canSlam() && Brute.getRandom().nextFloat() < Scp093Torso.SlamChance) {
                        Brute.beginSlam(Target);
                    } else if (Brute.canSwipe()) {
                        Brute.beginSwipe(Target);
                    }
                }
                return;
            }
            if (--Repath > 0) {
                return;
            }
            Repath = RepathTicks + Brute.getRandom().nextInt(RepathSpread);
            if (Knows) {
                Search = null;
                Searching = 0;
                boolean Pathed = Brute.getNavigation().moveTo(Target, Scp093Torso.ChaseModifier);
                if ((!Pathed || Brute.getNavigation().isDone()) && Reach < DirectReach) {
                    Brute.getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), Scp093Torso.ChaseModifier);
                }
                return;
            }
            Vec3 Last = Brute.lastSeen();
            if (Last == null) {
                return;
            }
            if (Search == null && Brute.position().distanceToSqr(Last) > ArriveRange * ArriveRange) {
                route(Last);
                return;
            }
            Searching += Repath;
            if (Search == null || Searching > SearchTicks || Brute.position().distanceToSqr(Search) < ArriveRange * ArriveRange) {
                double Angle = Brute.getRandom().nextDouble() * Math.PI * 2.0;
                double Spread = SearchRadius * (0.4 + 0.6 * Brute.getRandom().nextDouble());
                Search = new Vec3(Last.x + Math.cos(Angle) * Spread, Last.y, Last.z + Math.sin(Angle) * Spread);
                Searching = 0;
            }
            route(Search);
        }

        private void route(Vec3 Spot) {
            if (Brute.getNavigation() instanceof Scp093TorsoNavigation Route) {
                Route.headFor(Spot.x, Spot.y, Spot.z, Scp093Torso.ChaseModifier);
            } else {
                Brute.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Scp093Torso.ChaseModifier);
            }
        }
    }

    public static final class Hunt<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        private final Scp093Torso Hunter;
        private final Predicate<LivingEntity> Filter;

        public Hunt(Scp093Torso Hunter, Class<T> Type, Predicate<LivingEntity> Filter) {
            super(Hunter, Type, 10, false, false, Filter);
            this.Hunter = Hunter;
            this.Filter = Filter;
        }

        @Override
        protected void findTarget() {
            boolean Inside = Scp093Wastes.inside(Hunter);
            TargetingConditions Fresh = TargetingConditions.forCombat().range(getFollowDistance()).selector(Filter);
            targetConditions = Inside ? Fresh.ignoreLineOfSight() : Fresh;
            super.findTarget();
            if (target == null && !Inside) {
                targetConditions = TargetingConditions.forCombat().range(Scp093Torso.SenseRange).selector(Filter).ignoreLineOfSight();
                super.findTarget();
            }
        }

        @Override
        public boolean canUse() {
            return !Hunter.isRising() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = mob.getTarget() != null ? mob.getTarget() : targetMob;
            if (!Hunter.keeps(Current)) {
                return false;
            }
            mob.setTarget(Current);
            return true;
        }
    }

    public static final class Retaliate extends HurtByTargetGoal {
        private final Scp093Torso Hunter;

        public Retaliate(Scp093Torso Hunter) {
            super(Hunter, Scp093Torso.class);
            this.Hunter = Hunter;
        }

        @Override
        public boolean canUse() {
            return !Hunter.isRising() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = mob.getTarget() != null ? mob.getTarget() : targetMob;
            if (!Hunter.keeps(Current)) {
                return false;
            }
            mob.setTarget(Current);
            return true;
        }
    }
}
