package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp610Goals {
    public static final int ScanTicks = 10;
    public static final int GiveUpTicks = 400;
    public static final double SwitchShare = 0.64;
    public static final double HomeLeash = 24.0;

    private Scp610Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp610Creature Holder;

        public HoldStill(Scp610Creature Holder) {
            this.Holder = Holder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Holder.locksMovement();
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

    public static final class Chase extends Goal {
        private final Scp610Creature Hunter;
        private final double Speed;
        private int Repath;

        public Chase(Scp610Creature Hunter, double Speed) {
            this.Hunter = Hunter;
            this.Speed = Speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Hunter.getTarget();
            return Target != null && Target.isAlive() && !Hunter.locksMovement() && Hunter.form().mobile() && Hunter.keepsMelee();
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
            Hunter.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Hunter.getTarget();
            if (Target == null) {
                return;
            }
            Hunter.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Reach = Hunter.reachTo(Target);
            if (Reach <= Hunter.attackReach() && Hunter.getSensing().hasLineOfSight(Target)) {
                Hunter.getNavigation().stop();
                Hunter.faceToward(Target, 30.0f);
                Hunter.beginAttack(Target);
                return;
            }
            if (--Repath <= 0) {
                Repath = 6 + Hunter.getRandom().nextInt(6);
                Hunter.getNavigation().moveTo(Target, Speed);
            }
        }
    }

    public static final class ReturnHome extends Goal {
        private final Scp610Creature Walker;
        private final double Speed;

        public ReturnHome(Scp610Creature Walker, double Speed) {
            this.Walker = Walker;
            this.Speed = Speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos Home = Walker.home();
            return Home != null && Walker.getTarget() == null && !Walker.locksMovement() && Walker.form().mobile()
                    && Walker.blockPosition().distSqr(Home) > HomeLeash * HomeLeash && Walker.getRandom().nextInt(40) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos Home = Walker.home();
            return Home != null && Walker.getTarget() == null && !Walker.getNavigation().isDone() && Walker.blockPosition().distSqr(Home) > 64.0;
        }

        @Override
        public void start() {
            BlockPos Home = Walker.home();
            if (Home != null) {
                Vec3 Spot = Vec3.atBottomCenterOf(Home);
                Walker.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Speed);
            }
        }
    }

    public static final class Avenge extends TargetGoal {
        private final Scp610Creature Hunter;
        private int Seen;

        public Avenge(Scp610Creature Hunter) {
            super(Hunter, true);
            this.Hunter = Hunter;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity Attacker = Hunter.getLastHurtByMob();
            return Attacker != null && Hunter.getLastHurtByMobTimestamp() != Seen && !Scp610Creature.isFlesh(Attacker) && Attacker.isAlive();
        }

        @Override
        public void start() {
            Hunter.setTarget(Hunter.getLastHurtByMob());
            Seen = Hunter.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    public static final class Hunt extends TargetGoal {
        private final Scp610Creature Hunter;
        private final TargetingConditions Sight;
        @Nullable
        private LivingEntity Found;
        private int Rescan;

        public Hunt(Scp610Creature Hunter) {
            super(Hunter, true, false);
            this.Hunter = Hunter;
            this.Sight = TargetingConditions.forCombat().range(Scp610Creature.AcquireRange).selector(Candidate -> Scp610Creature.isQuarry(Hunter, Candidate));
            setFlags(EnumSet.of(Goal.Flag.TARGET));
            setUnseenMemoryTicks(GiveUpTicks);
        }

        @Override
        public boolean canUse() {
            if (Hunter.isEmerging() || Hunter.getRandom().nextInt(reducedTickDelay(ScanTicks)) != 0) {
                return false;
            }
            Found = closest();
            return Found != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Hunter.getTarget();
            if (Current != null && !Scp610Creature.isQuarry(Hunter, Current) && Current != Hunter.getLastHurtByMob()) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void start() {
            Hunter.setTarget(Found);
            targetMob = Found;
            Rescan = ScanTicks;
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
            Rescan = ScanTicks;
            LivingEntity Current = Hunter.getTarget();
            LivingEntity Closer = closest();
            if (Closer == null || Closer == Current) {
                return;
            }
            if (Current == null || !Current.isAlive() || Hunter.distanceToSqr(Closer) < Hunter.distanceToSqr(Current) * SwitchShare) {
                Hunter.setTarget(Closer);
                targetMob = Closer;
            }
        }

        @Nullable
        public LivingEntity closest() {
            double Range = Scp610Creature.AcquireRange;
            AABB Area = Hunter.getBoundingBox().inflate(Range, 8.0, Range);
            return Hunter.level().getNearestEntity(Hunter.level().getEntitiesOfClass(LivingEntity.class, Area, Candidate -> Scp610Creature.isQuarry(Hunter, Candidate)),
                    Sight, Hunter, Hunter.getX(), Hunter.getEyeY(), Hunter.getZ());
        }
    }
}
