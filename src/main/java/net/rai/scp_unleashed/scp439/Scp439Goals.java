package net.rai.scp_unleashed.scp439;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp439Goals {
    private Scp439Goals() {
    }

    public static final class Pacer {
        private long Next = Long.MIN_VALUE;

        public boolean ready(Mob Owner, int Every) {
            long Now = Owner.level().getGameTime();
            if (Now < Next) {
                return false;
            }
            Next = Now + Every;
            return true;
        }
    }

    public static final class QueenHunt extends Goal {
        public static final int Rethink = 10;
        private final Scp439Queen Queen;
        private int Delay;

        public QueenHunt(Scp439Queen Queen) {
            this.Queen = Queen;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Queen.isActing() || Queen.resting() || Queen.stillTravelling()) {
                return false;
            }
            if (--Delay > 0) {
                return Queen.prey() != null;
            }
            Delay = Rethink;
            LivingEntity Found = Queen.choosePrey();
            Queen.setPrey(Found);
            return Found != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !Queen.resting() && Queen.prey() != null;
        }

        @Override
        public void stop() {
            Queen.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Queen.prey();
            if (Target == null || Queen.isActing()) {
                return;
            }
            Queen.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Queen.inBiteReach(Target) && Queen.canSting()) {
                Queen.getNavigation().stop();
                Queen.beginBite(Target);
                return;
            }
            if (Queen.tickCount % Rethink == 0 || Queen.getNavigation().isDone()) {
                LivingEntity Closer = Queen.choosePrey();
                if (Closer != null && Closer != Target) {
                    Queen.setPrey(Closer);
                    Target = Closer;
                }
                Queen.getNavigation().moveTo(Target, Queen.distanceTo(Target) < 6.0f ? 1.2 : 1.0);
            }
        }
    }

    public static final class QueenTravel extends Goal {
        public static final double Step = 14.0;
        public static final int StuckTicks = 400;
        private final Scp439Queen Queen;
        private double Best;
        private int Stuck;

        public QueenTravel(Scp439Queen Queen) {
            this.Queen = Queen;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Queen.stillTravelling() && !Queen.isActing() && Queen.waypoint() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Best = Queen.travelled();
            Stuck = 0;
        }

        @Override
        public void tick() {
            BlockPos Goal0 = Queen.waypoint();
            if (Goal0 == null) {
                return;
            }
            double Now = Queen.travelled();
            if (Now > Best + 1.0) {
                Best = Now;
                Stuck = 0;
            } else if (++Stuck > StuckTicks) {
                Queen.repickWaypoint();
                Stuck = 0;
            }
            if (Queen.getNavigation().isDone() || Queen.tickCount % 60 == 0) {
                Vec3 Toward = Vec3.atBottomCenterOf(Goal0).subtract(Queen.position());
                Vec3 Flat = new Vec3(Toward.x, 0.0, Toward.z);
                if (Flat.lengthSqr() < 1.0) {
                    Queen.repickWaypoint();
                    return;
                }
                Vec3 Aim = Queen.position().add(Flat.normalize().scale(Math.min(Step, Flat.length())));
                Vec3 Spot = DefaultRandomPos.getPosTowards(Queen, 10, 6, Aim, Math.PI / 3.0);
                Vec3 Target = Spot != null ? Spot : Aim;
                Queen.getNavigation().moveTo(Target.x, Target.y, Target.z, 1.0);
            }
        }
    }

    public static final class Bite extends Goal {
        private final Scp439Creature Biter;
        private final double Speed;
        private int Repath;

        public Bite(Scp439Creature Biter, double Speed) {
            this.Biter = Biter;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Biter.getTarget();
            return Target != null && Target.isAlive() && !Biter.isEmerging();
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
            Biter.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Biter.getTarget();
            if (Target == null || Biter.isActing()) {
                return;
            }
            Biter.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            boolean Close = Biter instanceof Scp439Worker Worker ? Worker.inBiteReach(Target) : Biter instanceof Scp439Brute Brute && Brute.inBiteReach(Target);
            if (Close) {
                Biter.getNavigation().stop();
                if (Biter instanceof Scp439Worker Worker) {
                    Worker.beginBite(Target);
                } else if (Biter instanceof Scp439Brute Brute) {
                    Brute.beginBite(Target);
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = 6 + Biter.getRandom().nextInt(4);
                Biter.getNavigation().moveTo(Target, Speed);
            }
        }
    }

    public static final class Deliver extends Goal {
        public static final int DetourPatience = 80;
        public static final int DetourShunned = 600;
        private final Scp439Worker Worker;
        @Nullable
        private ItemEntity Detour;
        private int Detoured;

        public Deliver(Scp439Worker Worker) {
            this.Worker = Worker;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Worker.isCarrying() && Worker.getTarget() == null && Worker.home() != null && !Worker.isActing();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            Detour = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean detour() {
            if (Detour != null && (!Detour.isAlive() || !Worker.canTake(Detour.getItem()) || ++Detoured > DetourPatience)) {
                if (Detour.isAlive() && Detoured > DetourPatience) {
                    Worker.shun(Detour, DetourShunned);
                }
                Detour = null;
            }
            if (Detour == null && Worker.tickCount % 5 == 0) {
                Detour = Worker.detourLoot();
                Detoured = 0;
            }
            if (Detour == null) {
                return false;
            }
            if (Worker.distanceToSqr(Detour) <= Scp439Worker.PickReach) {
                Worker.getNavigation().stop();
                Worker.pickUp(Detour);
                Detour = null;
                return true;
            }
            Worker.approach(Detour, 1.15, Worker.tickCount % 10 == 0);
            return true;
        }

        @Override
        public void tick() {
            if (detour()) {
                return;
            }
            Vec3 Centre = Worker.homeCentre();
            double Flat = Math.sqrt(Mth.square(Centre.x - Worker.getX()) + Mth.square(Centre.z - Worker.getZ()));
            if (Flat <= Scp439Worker.DeliverReach + 0.6 && Math.abs(Centre.y - Worker.getY()) < 2.0) {
                Worker.getNavigation().stop();
                if (!Worker.deliver() && Worker.hive() == null) {
                    Worker.setCarried(ItemStack.EMPTY);
                }
                return;
            }
            if (Worker.getNavigation().isDone() || Worker.tickCount % 40 == 0) {
                Worker.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, 1.1);
            }
        }
    }

    public static final class Collect extends Goal {
        public static final int Patience = 300;
        public static final int Shunned = 600;
        private final Scp439Worker Worker;
        private final Pacer Pace = new Pacer();
        @Nullable
        private ItemEntity Wanted;
        private int Chased;

        public Collect(Scp439Worker Worker) {
            this.Worker = Worker;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Worker.isCarrying() || Worker.getTarget() != null || Worker.hiveId() == null || Worker.isActing() || !Pace.ready(Worker, 5)) {
                return false;
            }
            Wanted = Worker.nearestLoot(false);
            return Wanted != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Wanted != null && Wanted.isAlive() && !Worker.isCarrying() && Worker.getTarget() == null && Chased < Patience;
        }

        @Override
        public void start() {
            Chased = 0;
        }

        @Override
        public void stop() {
            if (Wanted != null && Chased >= Patience) {
                Worker.shun(Wanted, Shunned);
            }
            Wanted = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Wanted == null) {
                return;
            }
            Chased++;
            if (Worker.distanceToSqr(Wanted) <= Scp439Worker.PickReach) {
                Worker.getNavigation().stop();
                Worker.pickUp(Wanted);
                Wanted = null;
                return;
            }
            Worker.approach(Wanted, 1.15, Worker.tickCount % 20 == 0);
        }
    }

    public static final class Homeward extends Goal {
        private final Scp439Creature Member;
        private final double Limit;
        private final double Speed;

        public Homeward(Scp439Creature Member, double Limit, double Speed) {
            this.Member = Member;
            this.Limit = Limit;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Member.home() != null && Member.getTarget() == null && !Member.isActing() && Member.fromHome() > Limit;
        }

        @Override
        public boolean canContinueToUse() {
            return Member.getTarget() == null && Member.fromHome() > Limit * 0.5 + 1.0 && !Member.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 Centre = Member.homeCentre();
            Member.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, Speed);
        }
    }

    public static final class Wander extends Goal {
        private final Scp439Creature Member;
        private final int Radius;
        private final double Speed;

        public Wander(Scp439Creature Member, int Radius, double Speed) {
            this.Member = Member;
            this.Radius = Radius;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Member.isActing() || Member.getTarget() != null || !Member.restless() && Member.getRandom().nextInt(80) != 0) {
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !Member.getNavigation().isDone() && Member.getTarget() == null;
        }

        @Override
        public void start() {
            Vec3 Centre = Member.home() != null ? Member.homeCentre() : Member.position();
            for (int Attempt = 0; Attempt < 6; Attempt++) {
                double Angle = Member.getRandom().nextDouble() * Mth.TWO_PI;
                double Reach = 2.0 + Member.getRandom().nextDouble() * Radius;
                BlockPos Spot = BlockPos.containing(Centre.x + Math.cos(Angle) * Reach, Centre.y, Centre.z + Math.sin(Angle) * Reach);
                if (Member.getNavigation().moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Speed)) {
                    return;
                }
            }
        }
    }

    public static final class Forage extends TargetGoal {
        private final Scp439Worker Worker;
        private final Pacer Pace = new Pacer();

        public Forage(Scp439Worker Worker) {
            super(Worker, false, false);
            this.Worker = Worker;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Worker.isCarrying() || Worker.hiveId() == null || Worker.getTarget() != null || !Pace.ready(Worker, 20) || Worker.nearestLoot(true) != null) {
                return false;
            }
            LivingEntity Best = null;
            double BestDistance = Double.MAX_VALUE;
            AABB Area = Worker.getBoundingBox().inflate(Scp439Worker.PreySight, 6.0, Scp439Worker.PreySight);
            for (LivingEntity Found : Worker.level().getEntitiesOfClass(LivingEntity.class, Area, Candidate -> Scp439Worker.isPrey(Candidate) && Worker.nearHome(Candidate.position()))) {
                double Distance = Worker.distanceToSqr(Found);
                if (Distance < BestDistance) {
                    Best = Found;
                    BestDistance = Distance;
                }
            }
            if (Best == null) {
                return false;
            }
            targetMob = Best;
            return true;
        }

        @Override
        public void start() {
            Worker.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Worker.getTarget();
            return Target != null && Target.isAlive() && Worker.nearHome(Target.position()) && super.canContinueToUse();
        }
    }

    public static final class Guard extends TargetGoal {
        private final Scp439Brute Brute;
        private final Pacer Pace = new Pacer();

        public Guard(Scp439Brute Brute) {
            super(Brute, false, false);
            this.Brute = Brute;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Brute.getTarget() != null || !Pace.ready(Brute, 10)) {
                return false;
            }
            Vec3 Centre = Brute.home() != null ? Brute.homeCentre() : Brute.position();
            AABB Area = new AABB(Centre, Centre).inflate(Scp439Brute.GuardRadius, 6.0, Scp439Brute.GuardRadius);
            LivingEntity Best = null;
            double BestDistance = Double.MAX_VALUE;
            for (LivingEntity Found : Brute.level().getEntitiesOfClass(LivingEntity.class, Area, Brute::guards)) {
                double Distance = Brute.distanceToSqr(Found);
                if (Distance < BestDistance) {
                    Best = Found;
                    BestDistance = Distance;
                }
            }
            if (Best == null) {
                return false;
            }
            targetMob = Best;
            return true;
        }

        @Override
        public void start() {
            Brute.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Brute.getTarget();
            return Target != null && Target.isAlive() && Scp439Brute.isIntruder(Target) && !Brute.outOfLeash(Target);
        }
    }
}
