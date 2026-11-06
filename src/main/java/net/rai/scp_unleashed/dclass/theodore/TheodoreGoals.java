package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class TheodoreGoals {
    public static final double RunSpeed = 1.6;
    public static final double GatherSpeed = 1.3;
    public static final double HuddleSpeed = 0.9;
    public static final double HuddleRange = 7.0;
    public static final double EatReach = 1.6;
    public static final int ScanInterval = 10;

    private TheodoreGoals() {
    }

    public static final class Hold extends Goal {
        private final Theodore Owner;

        public Hold(Theodore Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Owner.isBusy();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Owner.getNavigation().stop();
            Vec3 Point = Owner.mergePoint();
            if (Point != null) {
                Owner.getLookControl().setLookAt(Point.x, Owner.getEyeY() - 0.4, Point.z);
            }
        }
    }

    public static final class Gather extends Goal {
        private final Theodore Owner;
        private int Repath;

        public Gather(Theodore Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Owner.mergePoint() != null;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            Vec3 Point = Owner.mergePoint();
            if (Point == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Point.x, Owner.getEyeY(), Point.z);
            if (Owner.position().distanceToSqr(Point) <= Theodore.MergeReach * Theodore.MergeReach * 0.5) {
                Owner.getNavigation().stop();
                return;
            }
            if (--Repath <= 0) {
                Repath = 10;
                Owner.getNavigation().moveTo(Point.x, Point.y, Point.z, GatherSpeed);
            }
        }
    }

    public static final class SeekArsenic extends Goal {
        private final Theodore Owner;
        @Nullable
        private ItemEntity Target;
        private int Cooldown;

        public SeekArsenic(Theodore Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Owner.canEatArsenic() || !(Owner.level() instanceof ServerLevel)) {
                return false;
            }
            if (--Cooldown > 0) {
                return false;
            }
            Cooldown = ScanInterval;
            Target = nearestBait();
            return Target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && Target.isAlive() && !Target.getItem().isEmpty() && Owner.canEatArsenic()
                    && Owner.distanceToSqr(Target) < (Theodore.SeekRange + 4.0) * (Theodore.SeekRange + 4.0);
        }

        @Override
        public void start() {
            Owner.markSeeking(true);
        }

        @Override
        public void stop() {
            Target = null;
            Owner.getNavigation().stop();
            Owner.markSeeking(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ItemEntity Bait = Target;
            if (Bait == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Bait, 30.0f, 30.0f);
            if (Owner.distanceToSqr(Bait) <= EatReach * EatReach) {
                Bait.getItem().shrink(1);
                if (Bait.getItem().isEmpty()) {
                    Bait.discard();
                }
                Target = null;
                Owner.beginEating();
                return;
            }
            if (Owner.tickCount % 5 == 0) {
                Owner.getNavigation().moveTo(Bait, RunSpeed);
            }
        }

        @Nullable
        private ItemEntity nearestBait() {
            AABB Area = Owner.getBoundingBox().inflate(Theodore.SeekRange);
            List<ItemEntity> Found = Owner.level().getEntitiesOfClass(ItemEntity.class, Area, Drop -> Drop.isAlive() && Theodore.isArsenic(Drop.getItem()));
            ItemEntity Best = null;
            double BestDistance = Double.MAX_VALUE;
            for (ItemEntity Drop : Found) {
                double Distance = Owner.distanceToSqr(Drop);
                if (Distance < BestDistance && isNearestTo(Drop, Distance)) {
                    Best = Drop;
                    BestDistance = Distance;
                }
            }
            return Best;
        }

        private boolean isNearestTo(ItemEntity Drop, double Mine) {
            AABB Area = Drop.getBoundingBox().inflate(Theodore.SeekRange);
            for (Theodore Other : Owner.level().getEntitiesOfClass(Theodore.class, Area, Theodore::canEatArsenic)) {
                if (Other != Owner && Other.distanceToSqr(Drop) < Mine) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class Huddle extends Goal {
        private final Theodore Owner;
        @Nullable
        private Theodore Eater;

        public Huddle(Theodore Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Owner.cluster() == null || Owner.isEater() || Owner.getRandom().nextInt(20) != 0 || !(Owner.level() instanceof ServerLevel World)) {
                return false;
            }
            Eater = World.getEntity(Owner.cluster()) instanceof Theodore Found && Found.isAlive() ? Found : null;
            return Eater != null && Owner.distanceToSqr(Eater) > HuddleRange * HuddleRange;
        }

        @Override
        public boolean canContinueToUse() {
            return Eater != null && Eater.isAlive() && !Owner.getNavigation().isDone() && Owner.mergePoint() == null;
        }

        @Override
        public void start() {
            if (Eater != null) {
                Owner.getNavigation().moveTo(Eater, HuddleSpeed);
            }
        }

        @Override
        public void stop() {
            Eater = null;
        }
    }
}
