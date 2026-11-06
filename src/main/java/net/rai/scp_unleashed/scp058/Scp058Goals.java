package net.rai.scp_unleashed.scp058;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp058Goals {
    public static final double LeapRise = 4.0;
    public static final int LeapChance = 4;

    private Scp058Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp058Heart Holder;

        public HoldStill(Scp058Heart Holder) {
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

    public static final class Leap extends Goal {
        private final Scp058Heart Jumper;

        public Leap(Scp058Heart Jumper) {
            this.Jumper = Jumper;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Jumper.getTarget();
            if (Target == null || !Target.isAlive() || !Jumper.canLeap() || Math.abs(Target.getY() - Jumper.getY()) > LeapRise) {
                return false;
            }
            double Distance = Jumper.distanceTo(Target);
            return Distance >= Scp058Heart.LeapMin && Distance <= Scp058Heart.LeapMax && Jumper.hasLineOfSight(Target)
                    && Jumper.getRandom().nextInt(reducedTickDelay(LeapChance)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Jumper.action() == Scp058Heart.ActionLeap;
        }

        @Override
        public void start() {
            LivingEntity Target = Jumper.getTarget();
            if (Target != null) {
                Jumper.beginLeap(Target);
            }
        }
    }

    public static final class Strike extends MeleeAttackGoal {
        private final Scp058Heart Striker;

        public Strike(Scp058Heart Striker, double Speed) {
            super(Striker, Speed, true);
            this.Striker = Striker;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (Striker.isActing() || !Striker.hasLineOfSight(Target)) {
                return;
            }
            double Reach = Striker.reachTo(Target);
            if (Reach <= Scp058Heart.StingReach && Striker.canSting()) {
                resetAttackCooldown();
                Striker.beginSting(Target);
            } else if (Reach <= Scp058Heart.LashReach && Striker.canLash()) {
                resetAttackCooldown();
                Striker.beginLash(Target);
            }
        }
    }

    public static final class Prowl extends RandomStrollGoal {
        public static final int Radius = 10;
        public static final int WaterRadius = 15;
        public static final int Rise = 7;
        public static final int Tries = 3;
        private final Scp058Heart Prowler;

        public Prowl(Scp058Heart Prowler, double Speed) {
            super(Prowler, Speed);
            this.Prowler = Prowler;
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            int Reach = Prowler.isInWaterOrBubble() ? WaterRadius : Radius;
            for (int Attempt = 0; Attempt < Tries; Attempt++) {
                Vec3 Spot = LandRandomPos.getPos(Prowler, Reach, Rise, this::inSight);
                if (Spot == null) {
                    continue;
                }
                Path Route = Prowler.getNavigation().createPath(BlockPos.containing(Spot), 1);
                if (Route != null && Route.canReach()) {
                    return Spot;
                }
            }
            return null;
        }

        private double inSight(BlockPos Pos) {
            Vec3 Eye = Prowler.getEyePosition();
            Vec3 Aim = Vec3.atCenterOf(Pos);
            return Prowler.level().clip(new ClipContext(Eye, Aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Prowler)).getType() == HitResult.Type.MISS ? 1.0 : 0.0;
        }
    }

    public static final class Hunt extends TargetGoal {
        private final Scp058Heart Hunter;
        private final TargetingConditions Sight;
        private int Rescan;

        public Hunt(Scp058Heart Hunter) {
            super(Hunter, true, false);
            this.Hunter = Hunter;
            this.Sight = TargetingConditions.forCombat().range(Scp058Heart.AcquireRange).selector(Scp058Heart::isPrey);
            setFlags(EnumSet.of(Goal.Flag.TARGET));
            setUnseenMemoryTicks(Scp058Heart.GiveUpTicks);
        }

        @Override
        public boolean canUse() {
            if (Hunter.getRandom().nextInt(reducedTickDelay(Scp058Heart.ScanTicks)) != 0) {
                return false;
            }
            targetMob = closest();
            return targetMob != null;
        }

        @Override
        public void start() {
            Hunter.setTarget(targetMob);
            Rescan = Scp058Heart.ScanTicks;
            super.start();
        }

        @Override
        public void tick() {
            if (--Rescan > 0) {
                return;
            }
            Rescan = Scp058Heart.ScanTicks;
            LivingEntity Current = Hunter.getTarget();
            LivingEntity Closer = closest();
            if (Closer == null || Closer == Current) {
                return;
            }
            if (Current == null || !Current.isAlive() || Hunter.distanceToSqr(Closer) < Hunter.distanceToSqr(Current) * Scp058Heart.SwitchShare) {
                Hunter.setTarget(Closer);
                targetMob = Closer;
            }
        }

        @Nullable
        private LivingEntity closest() {
            AABB Area = Hunter.getBoundingBox().inflate(Scp058Heart.AcquireRange, Scp058Heart.AcquireHeight, Scp058Heart.AcquireRange);
            return Hunter.level().getNearestEntity(Hunter.level().getEntitiesOfClass(LivingEntity.class, Area, Scp058Heart::isPrey), Sight, Hunter,
                    Hunter.getX(), Hunter.getEyeY(), Hunter.getZ());
        }
    }
}
