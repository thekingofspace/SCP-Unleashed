package net.rai.scp_unleashed.scp075;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.Predicate;

public final class Scp075Goals {
    public static final int RepathTicks = 10;
    public static final int LostSightTicks = 40;
    public static final double HoldSlack = 2.0;
    public static final double ParentRange = 12.0;
    public static final double ParentFar = 3.5;
    public static final double ParentNear = 2.0;
    public static final int GiveUpTicks = 600;

    private Scp075Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp075Snail Holder;

        public HoldStill(Scp075Snail Holder) {
            this.Holder = Holder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
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

    public static final class SeekShell extends Goal {
        private final Scp075Snail Eater;
        private int Repath;

        public SeekShell(Scp075Snail Eater) {
            this.Eater = Eater;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Eater.noticedShell() != null && !Eater.locksMovement();
        }

        @Override
        public boolean canContinueToUse() {
            return Eater.noticedShell() != null && !Eater.locksMovement();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Eater.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ItemEntity Shell = Eater.noticedShell();
            if (Shell == null) {
                return;
            }
            Eater.getLookControl().setLookAt(Shell.getX(), Shell.getY(), Shell.getZ());
            if (Eater.mouthReach(Shell) <= Scp075Snail.EatReach && Math.abs(Shell.getY() - Eater.getY()) < 1.0) {
                Eater.beginEat(Shell);
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                if (!Eater.getNavigation().moveTo(Shell.getX(), Shell.getY(), Shell.getZ(), Scp075Snail.ChaseModifier) || Eater.getNavigation().isDone()) {
                    Eater.getMoveControl().setWantedPosition(Shell.getX(), Shell.getY(), Shell.getZ(), Scp075Snail.ChaseModifier);
                }
            }
        }
    }

    public static final class Engage extends Goal {
        private final Scp075Snail Shooter;
        private int LostSight;

        public Engage(Scp075Snail Shooter) {
            this.Shooter = Shooter;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Shooter.getTarget();
            return Target != null && Target.isAlive() && Shooter.canHunt() && Shooter.distanceTo(Target) <= Scp075Snail.HoldRange && Shooter.hasLineOfSight(Target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Shooter.getTarget();
            return Target != null && Target.isAlive() && Shooter.canHunt() && Shooter.distanceTo(Target) <= Scp075Snail.HoldRange + HoldSlack && LostSight < LostSightTicks;
        }

        @Override
        public void start() {
            LostSight = 0;
            Shooter.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Shooter.getTarget();
            if (Target == null) {
                return;
            }
            Shooter.getNavigation().stop();
            Shooter.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            boolean Seen = Shooter.hasLineOfSight(Target);
            LostSight = Seen ? 0 : LostSight + 1;
            if (Shooter.isActing()) {
                return;
            }
            Shooter.turnToward(Target.getX(), Target.getZ(), Scp075Snail.TurnSpeed);
            double Reach = Shooter.reachTo(Target);
            if (Reach <= Scp075Snail.SwipeReach) {
                if (Shooter.canSwipe() && Seen) {
                    Shooter.beginSwipe(Target);
                }
                return;
            }
            if (Seen && Shooter.canSpit() && Shooter.facingError(Target) <= Scp075Snail.AimTolerance) {
                Shooter.beginSpit(Target);
            }
        }
    }

    public static final class Approach extends Goal {
        private final Scp075Snail Crawler;
        private int Repath;

        public Approach(Scp075Snail Crawler) {
            this.Crawler = Crawler;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Crawler.getTarget();
            return Target != null && Target.isAlive() && Crawler.canHunt() && !Crawler.locksMovement();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Crawler.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity Target = Crawler.getTarget();
            if (Target != null && --Repath <= 0) {
                Repath = RepathTicks;
                Crawler.getNavigation().moveTo(Target, Scp075Snail.ChaseModifier);
            }
        }
    }

    public static final class FollowAdult extends Goal {
        private final Scp075Snail Young;
        @Nullable
        private Scp075Snail Parent;
        private int Repath;

        public FollowAdult(Scp075Snail Young) {
            this.Young = Young;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Young.isBaby() || Young.locksMovement() || Young.tickCount % 20 != 0) {
                return false;
            }
            Parent = Young.level().getEntitiesOfClass(Scp075Snail.class, Young.getBoundingBox().inflate(ParentRange), Found -> Found != Young && !Found.isBaby() && Found.isAlive())
                    .stream().min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Young))).orElse(null);
            return Parent != null && Young.distanceTo(Parent) > ParentFar;
        }

        @Override
        public boolean canContinueToUse() {
            return Parent != null && Parent.isAlive() && !Young.locksMovement() && Young.distanceTo(Parent) > ParentNear && Young.distanceTo(Parent) < ParentRange * 1.5;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Parent = null;
            Young.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (Parent != null && --Repath <= 0) {
                Repath = RepathTicks;
                Young.getNavigation().moveTo(Parent, Scp075Snail.BabyChaseModifier);
            }
        }
    }

    public static final class Defend extends HurtByTargetGoal {
        private final Scp075Snail Victim;

        public Defend(Scp075Snail Victim) {
            super(Victim, Scp075Snail.class);
            this.Victim = Victim;
        }

        @Override
        public boolean canUse() {
            return Victim.canHunt() && super.canUse();
        }
    }

    public static final class Hunt<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        private final Scp075Snail Hunter;

        public Hunt(Scp075Snail Hunter, Class<T> Type, Predicate<LivingEntity> Filter) {
            super(Hunter, Type, 10, true, false, Filter);
            this.Hunter = Hunter;
            setUnseenMemoryTicks(GiveUpTicks);
        }

        @Override
        public boolean canUse() {
            return Hunter.canHunt() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Hunter.canHunt() && super.canContinueToUse();
        }
    }
}
