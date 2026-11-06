package net.rai.scp_unleashed.scp3199;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp3199Goals {
    public static final int HuntScan = 10;
    public static final double SmellRange = 10.0;
    public static final int RepathTicks = 8;
    public static final int RetryOnDone = 3;
    public static final int RoamMin = 100;
    public static final int RoamSpread = 140;
    public static final double RoamReachMin = 3.0;
    public static final double RoamReachMax = 8.0;
    public static final float TurnSpeed = 18.0f;

    private Scp3199Goals() {
    }

    public static boolean isPrey(Scp3199 Owner, LivingEntity Body) {
        if (Body == Owner || Body instanceof Scp3199 || !Body.isAlive() || Body.isRemoved() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && !Scp268.isNeglected(User);
        }
        if (ControlBodies.isPersonLike(Body)) {
            return !ControlSessions.isBoundToSession(Body) && !(Body instanceof DClassLike Person && (Person.isSpecimen() || Person.isPuppet()));
        }
        return Body instanceof AbstractVillager || Body instanceof Animal;
    }

    public static final class Hunt extends TargetGoal {
        private final Scp3199 Owner;
        @Nullable
        private LivingEntity Found;
        private int Wait;

        public Hunt(Scp3199 Owner) {
            super(Owner, false, false);
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!Owner.canAct() || Owner.getTarget() != null) {
                return false;
            }
            if (--Wait > 0) {
                return false;
            }
            Wait = HuntScan;
            Found = nearest();
            return Found != null;
        }

        @Nullable
        private LivingEntity nearest() {
            double Range = getFollowDistance();
            LivingEntity Best = null;
            double BestDistance = Range * Range;
            for (LivingEntity Body : Owner.level().getEntitiesOfClass(LivingEntity.class, Owner.getBoundingBox().inflate(Range, Range * 0.5, Range),
                    Body -> isPrey(Owner, Body))) {
                double Distance = Body.distanceToSqr(Owner);
                if (Distance >= BestDistance) {
                    continue;
                }
                if (Distance > SmellRange * SmellRange && !Owner.getSensing().hasLineOfSight(Body)) {
                    continue;
                }
                BestDistance = Distance;
                Best = Body;
            }
            return Best;
        }

        @Override
        public void start() {
            Owner.setTarget(Found);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Owner.getTarget();
            return Owner.canAct() && Target != null && isPrey(Owner, Target) && super.canContinueToUse();
        }
    }

    public static final class Retaliate extends HurtByTargetGoal {
        private final Scp3199 Owner;

        public Retaliate(Scp3199 Owner) {
            super(Owner, Scp3199.class);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            LivingEntity Attacker = Owner.getLastHurtByMob();
            return Owner.canAct() && Attacker != null && !(Attacker instanceof Player User && (User.isCreative() || User.isSpectator())) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.canAct() && super.canContinueToUse();
        }
    }

    public static final class Maul extends Goal {
        private final Scp3199 Owner;
        private int Repath;

        public Maul(Scp3199 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Owner.canAct() && Target != null && Target.isAlive() && Owner.canAttack(Target);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Repath = 0;
            Owner.setAggressive(true);
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
            Owner.setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Gap = Math.sqrt(Owner.getBoundingBox().distanceToSqr(Target.getBoundingBox().getCenter()));
            if (Gap <= Owner.reach() - Owner.getBbWidth() * 0.5 && Owner.getSensing().hasLineOfSight(Target)) {
                Owner.getNavigation().stop();
                turnToward(Target.position());
                if (Owner.attackReady()) {
                    Owner.strike(Target);
                }
                Repath = 0;
                return;
            }
            if (Owner.getNavigation().isDone()) {
                Repath = Math.min(Repath, RetryOnDone);
            }
            if (--Repath <= 0) {
                Repath = RepathTicks + Owner.getRandom().nextInt(4);
                Path Route = Owner.getNavigation().createPath(Target, 0);
                if (Route != null) {
                    Owner.getNavigation().moveTo(Route, Scp3199.HuntModifier);
                } else {
                    Owner.getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), Scp3199.HuntModifier);
                }
            }
        }

        private void turnToward(Vec3 Where) {
            double Dx = Where.x - Owner.getX();
            double Dz = Where.z - Owner.getZ();
            if (Dx * Dx + Dz * Dz < 1.0e-4) {
                return;
            }
            float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            Owner.setYRot(Mth.approachDegrees(Owner.getYRot(), Wanted, TurnSpeed));
            Owner.setYBodyRot(Owner.getYRot());
        }
    }

    public static final class Roam extends Goal {
        private final Scp3199 Owner;
        private int Wait;
        private Vec3 Destination = Vec3.ZERO;

        public Roam(Scp3199 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Owner.canAct() || Owner.getTarget() != null) {
                return false;
            }
            if (--Wait > 0) {
                return false;
            }
            Wait = RoamMin + Owner.getRandom().nextInt(RoamSpread);
            BlockPos Home = Owner.nesting().anchor();
            Vec3 Centre = Home == null ? Owner.position() : Vec3.atBottomCenterOf(Home);
            float Angle = Owner.getRandom().nextFloat() * Mth.TWO_PI;
            double Reach = RoamReachMin + Owner.getRandom().nextDouble() * (RoamReachMax - RoamReachMin);
            Destination = Centre.add(Mth.cos(Angle) * Reach, 0.0, Mth.sin(Angle) * Reach);
            return true;
        }

        @Override
        public void start() {
            Path Route = Owner.getNavigation().createPath(Destination.x, Destination.y, Destination.z, 1);
            if (Route != null) {
                Owner.getNavigation().moveTo(Route, Scp3199.RoamModifier);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.canAct() && Owner.getTarget() == null && !Owner.getNavigation().isDone();
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }
    }

    public static final class Gaze extends LookAtPlayerGoal {
        private final Scp3199 Owner;

        public Gaze(Scp3199 Owner) {
            super(Owner, Player.class, 16.0f);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return Owner.canAct() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.canAct() && super.canContinueToUse();
        }
    }
}
