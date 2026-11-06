package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DaeviteGoals {
    public static final int Repath = 8;
    public static final double PatrolSpeed = 0.95;
    public static final double HomeSpeed = 0.85;
    public static final double PostSpeed = 0.6;
    public static final int PostChance = 140;
    public static final int PostRadius = 7;

    private DaeviteGoals() {
    }

    public static class Fight extends Goal {
        private final DaeviteMob Owner;
        private int Wait;

        public Fight(DaeviteMob Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Prey = Owner.getTarget();
            return Prey != null && Owner.isPrey(Prey) && !Owner.doomed();
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
            Wait = 0;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity Prey = Owner.getTarget();
            if (Prey != null) {
                Owner.fight(this, Prey);
            }
        }

        public void chase(LivingEntity Prey, double Speed) {
            if (--Wait <= 0 || Owner.getNavigation().isDone()) {
                Wait = Repath + Owner.getRandom().nextInt(4);
                Owner.getNavigation().moveTo(Prey, Speed);
            }
        }

        public void steer(Vec3 Spot, double Speed) {
            if (--Wait <= 0 || Owner.getNavigation().isDone()) {
                Wait = Repath + Owner.getRandom().nextInt(4);
                Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Speed);
            }
        }
    }

    public static class Patrol extends Goal {
        private final DaeviteMob Owner;
        private int Wait;

        public Patrol(DaeviteMob Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Owner.getTarget() == null && Owner.patrolTarget() != null && !Owner.doomed();
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
        public void tick() {
            Vec3 Target = Owner.patrolTarget();
            if (Target == null) {
                return;
            }
            if (Owner.position().distanceToSqr(Target) < 9.0) {
                Owner.clearPatrol();
                return;
            }
            if (--Wait <= 0) {
                Wait = 20;
                Owner.getNavigation().moveTo(Target.x, Target.y, Target.z, PatrolSpeed);
            }
        }
    }

    public static class ReturnHome extends Goal {
        private final DaeviteMob Owner;
        private int Wait;

        public ReturnHome(DaeviteMob Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos Home = Owner.home();
            return Owner.getTarget() == null && Owner.patrolTarget() == null && Home != null && !Owner.doomed()
                    && Owner.blockPosition().distSqr(Home) > DaeviteMob.HomeLeash * DaeviteMob.HomeLeash;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos Home = Owner.home();
            return Owner.getTarget() == null && Owner.patrolTarget() == null && Home != null && !Owner.doomed() && Owner.blockPosition().distSqr(Home) > 25.0;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos Home = Owner.home();
            if (Home != null && --Wait <= 0) {
                Wait = 20;
                Owner.getNavigation().moveTo(Home.getX() + 0.5, Home.getY(), Home.getZ() + 0.5, HomeSpeed);
            }
        }
    }

    public static class Post extends Goal {
        private final DaeviteMob Owner;
        private Vec3 Spot = Vec3.ZERO;

        public Post(DaeviteMob Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Owner.getTarget() != null || Owner.patrolTarget() != null || Owner.doomed() || Owner.getRandom().nextInt(PostChance) != 0) {
                return false;
            }
            BlockPos Centre = Owner.home() != null ? Owner.home() : Owner.blockPosition();
            int Dx = Owner.getRandom().nextInt(PostRadius * 2 + 1) - PostRadius;
            int Dz = Owner.getRandom().nextInt(PostRadius * 2 + 1) - PostRadius;
            Spot = new Vec3(Centre.getX() + Dx + 0.5, Centre.getY(), Centre.getZ() + Dz + 0.5);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.getTarget() == null && !Owner.getNavigation().isDone() && !Owner.doomed();
        }

        @Override
        public void start() {
            Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, PostSpeed);
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }
    }

    public static class Channel extends Goal {
        private final DaeviteOverseer Owner;
        private int Wait;

        public Channel(DaeviteOverseer Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.channelPoint() != null && !Owner.doomed();
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
        public void stop() {
            Owner.getNavigation().stop();
            Owner.stopChannel();
        }

        @Override
        public void tick() {
            Vec3 Point = Owner.channelPoint();
            Vec3 Altar = Owner.channelAltar();
            if (Point == null) {
                return;
            }
            double Gap = Owner.position().distanceToSqr(Point.x, Owner.getY(), Point.z);
            if (Gap > 1.6) {
                Owner.leaveChannelPose();
                if (--Wait <= 0) {
                    Wait = 10;
                    Owner.getNavigation().moveTo(Point.x, Point.y, Point.z, 1.1);
                }
            } else {
                Owner.getNavigation().stop();
                Owner.holdChannel();
            }
            if (Altar != null) {
                Owner.getLookControl().setLookAt(Altar.x, Altar.y + 1.0, Altar.z, 30.0f, 30.0f);
            }
        }
    }
}
