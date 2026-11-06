package net.rai.scp_unleashed.scp682;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp682Goals {
    public static final int HuntScan = 10;
    public static final double SmellRange = 12.0;
    public static final int RepathTicks = 10;
    public static final int StuckTicks = 20;
    public static final double StuckDistance = 0.4;
    public static final int RoamMin = 120;
    public static final int RoamSpread = 160;
    public static final int RoamPush = 100;
    public static final double RoamReachMin = 8.0;
    public static final double RoamReachMax = 20.0;
    public static final int EscapeReach = 8;
    public static final int EscapeRetry = 40;
    public static final int EscapePush = 160;

    private Scp682Goals() {
    }

    public static final class Hunt extends TargetGoal {
        private final Scp682 Owner;
        @Nullable
        private LivingEntity Found;
        private int Wait;

        public Hunt(Scp682 Owner) {
            super(Owner, false, false);
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!Owner.canAct() || Owner.isCalm() || Owner.getTarget() != null) {
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
                    Body -> Scp682Rivals.isPrey(Owner, Body))) {
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
            return Owner.canAct() && !Owner.isCalm() && super.canContinueToUse();
        }
    }

    public static final class Retaliate extends HurtByTargetGoal {
        private final Scp682 Owner;

        public Retaliate(Scp682 Owner) {
            super(Owner);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return Owner.canAct() && !Owner.isCalm() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.canAct() && !Owner.isCalm() && super.canContinueToUse();
        }
    }

    public static final class Maul extends Goal {
        private final Scp682 Owner;
        private int Repath;
        private int Stuck;
        private boolean Pushing;
        private Vec3 Last = Vec3.ZERO;

        public Maul(Scp682 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Owner.canAct() && !Owner.isCalm() && Target != null && Target.isAlive();
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
            Stuck = 0;
            Pushing = false;
            Last = Owner.position();
            if (Owner.level() instanceof ServerLevel World && Owner.roarReady() && Owner.getRandom().nextInt(3) == 0) {
                Owner.roar(World);
            }
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
            Pushing = false;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null || !(Owner.level() instanceof ServerLevel World)) {
                return;
            }
            Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Gap = Math.sqrt(Owner.getBoundingBox().distanceToSqr(Target.getBoundingBox().getCenter()));
            if (Gap <= Owner.reach() - Owner.getBbWidth() * 0.5) {
                Owner.getNavigation().stop();
                turnToward(Target.position());
                if (Owner.attackReady()) {
                    Owner.strike(Target);
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                Path Route = Owner.getNavigation().createPath(Target, 0);
                Pushing = Route == null || !Route.canReach();
                if (!Pushing) {
                    Owner.getNavigation().moveTo(Route, Scp682.HuntModifier);
                }
            }
            if (Owner.tickCount % StuckTicks == 0) {
                Stuck = Owner.position().distanceTo(Last) < StuckDistance ? Stuck + 1 : 0;
                Last = Owner.position();
                if (Stuck > 0) {
                    Pushing = true;
                }
            }
            if (Pushing) {
                Owner.getNavigation().stop();
                Owner.getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), Scp682.HuntModifier);
            }
            if (Pushing || Owner.horizontalCollision) {
                Owner.smasher().tear(World, Owner, Target.position().subtract(Owner.position()));
            }
        }

        private void turnToward(Vec3 Where) {
            double Dx = Where.x - Owner.getX();
            double Dz = Where.z - Owner.getZ();
            if (Dx * Dx + Dz * Dz < 1.0e-4) {
                return;
            }
            float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            Owner.setYRot(Mth.approachDegrees(Owner.getYRot(), Wanted, 12.0f));
            Owner.setYBodyRot(Owner.getYRot());
        }
    }

    public static final class Escape extends Goal {
        private final Scp682 Owner;
        private int Wait;
        private int Push;
        @Nullable
        private BlockPos Dry;

        public Escape(Scp682 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Owner.canAct() || Owner.isCalm() || !Owner.isSoaked() || Owner.getTarget() != null) {
                return false;
            }
            if (--Wait > 0) {
                return false;
            }
            Wait = EscapeRetry;
            Dry = dryLand();
            return Dry != null;
        }

        @Nullable
        private BlockPos dryLand() {
            BlockPos Centre = Owner.blockPosition();
            BlockPos Best = null;
            double BestDistance = Double.MAX_VALUE;
            for (BlockPos Each : BlockPos.betweenClosed(Centre.offset(-EscapeReach, -2, -EscapeReach), Centre.offset(EscapeReach, 3, EscapeReach))) {
                if (!Owner.level().getFluidState(Each).isEmpty() || !Owner.level().getBlockState(Each).getCollisionShape(Owner.level(), Each).isEmpty()) {
                    continue;
                }
                BlockPos Below = Each.below();
                if (!Owner.level().getBlockState(Below).isFaceSturdy(Owner.level(), Below, Direction.UP) || !Owner.level().getFluidState(Below).isEmpty()) {
                    continue;
                }
                double Distance = Each.distSqr(Centre);
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Each.immutable();
                }
            }
            return Best;
        }

        @Override
        public void start() {
            Push = EscapePush;
            if (Dry != null) {
                Owner.getNavigation().moveTo(Dry.getX() + 0.5, Dry.getY(), Dry.getZ() + 0.5, Scp682.HuntModifier);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Push > 0 && Dry != null && Owner.canAct() && Owner.isSoaked() && Owner.getTarget() == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Push--;
            if (Dry == null) {
                return;
            }
            if (Owner.getNavigation().isDone()) {
                Owner.getMoveControl().setWantedPosition(Dry.getX() + 0.5, Dry.getY(), Dry.getZ() + 0.5, Scp682.HuntModifier);
                if (Owner.horizontalCollision) {
                    Owner.getJumpControl().jump();
                }
            }
        }

        @Override
        public void stop() {
            Dry = null;
            Owner.getNavigation().stop();
        }
    }

    public static final class Roam extends Goal {
        private final Scp682 Owner;
        private int Wait;
        private int Push;
        private Vec3 Destination = Vec3.ZERO;

        public Roam(Scp682 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Owner.canAct() || Owner.isCalm() || Owner.getTarget() != null) {
                return false;
            }
            if (--Wait > 0) {
                return false;
            }
            Wait = RoamMin + Owner.getRandom().nextInt(RoamSpread);
            float Angle = Owner.getRandom().nextFloat() * Mth.TWO_PI;
            double Reach = RoamReachMin + Owner.getRandom().nextDouble() * (RoamReachMax - RoamReachMin);
            Destination = Owner.position().add(Mth.cos(Angle) * Reach, 0.0, Mth.sin(Angle) * Reach);
            return true;
        }

        @Override
        public void start() {
            Push = 0;
            Path Route = Owner.getNavigation().createPath(Destination.x, Destination.y, Destination.z, 1);
            if (Route != null && Route.canReach()) {
                Owner.getNavigation().moveTo(Route, Scp682.RoamModifier);
            } else {
                Push = RoamPush;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.canAct() && !Owner.isCalm() && Owner.getTarget() == null && (Push > 0 || !Owner.getNavigation().isDone());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Push <= 0) {
                return;
            }
            Push--;
            Owner.getMoveControl().setWantedPosition(Destination.x, Destination.y, Destination.z, Scp682.RoamModifier);
            if (Owner.horizontalCollision && Owner.level() instanceof ServerLevel World) {
                Owner.smasher().tear(World, Owner, Destination.subtract(Owner.position()));
            }
            if (Owner.position().distanceToSqr(Destination) < 4.0) {
                Push = 0;
            }
        }

        @Override
        public void stop() {
            Push = 0;
            Owner.getNavigation().stop();
        }
    }

    public static final class Gaze extends LookAtPlayerGoal {
        private final Scp682 Owner;

        public Gaze(Scp682 Owner) {
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
