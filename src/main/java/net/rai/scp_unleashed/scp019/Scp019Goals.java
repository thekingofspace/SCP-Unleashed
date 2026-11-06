package net.rai.scp_unleashed.scp019;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp019Goals {
    private Scp019Goals() {
    }

    public static double flatDistance(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static float yawTowards(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static final class HoldStill extends Goal {
        private final Scp019Creature Holder;

        public HoldStill(Scp019Creature Holder) {
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

    public static final class Wander extends Goal {
        private final Scp019Creature Walker;
        private final double Speed;
        private final int Range;
        @Nullable
        private Vec3 Destination;

        public Wander(Scp019Creature Walker, double Speed, int Range) {
            this.Walker = Walker;
            this.Speed = Speed;
            this.Range = Range;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Walker.isOwned() || Walker.locksMovement() || Walker.getRandom().nextInt(80) != 0) {
                return false;
            }
            Destination = DefaultRandomPos.getPos(Walker, Range, 3);
            return Destination != null && flatDistance(Destination, Walker.anchor()) <= Range;
        }

        @Override
        public boolean canContinueToUse() {
            return !Walker.getNavigation().isDone() && !Walker.locksMovement();
        }

        @Override
        public void start() {
            if (Destination != null) {
                Walker.getNavigation().moveTo(Destination.x, Destination.y, Destination.z, Speed);
            }
        }
    }

    public static final class DroneReport extends Goal {
        private final Scp019Drone Drone;
        private int Repath;

        public DroneReport(Scp019Drone Drone) {
            this.Drone = Drone;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Drone.isReporting() && !Drone.locksMovement();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Drone.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp019PotBlockEntity Home = Drone.home();
            Vec3 Centre = Home != null ? Home.centre() : Drone.anchor();
            if (Drone.tickCount % 10 == 0) {
                Drone.refreshQuarry();
            }
            if (flatDistance(Drone.position(), Centre) <= Scp019Drone.ReportReach && Math.abs(Drone.getY() - Centre.y) < 3.0) {
                Vec3 Seen = Drone.quarrySeen();
                LivingEntity Quarry = Drone.quarry();
                if (Home != null && Seen != null && Drone.isValidTarget(Quarry)) {
                    Home.report(Quarry, Seen);
                }
                Drone.delivered();
                Drone.getNavigation().stop();
                return;
            }
            if (--Repath <= 0) {
                Repath = 10;
                Drone.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, 1, Scp019Drone.RunSpeed);
            }
        }
    }

    public static final class DroneReturn extends Goal {
        public static final int GiveUpTicks = 400;
        public static final int SettleTicks = 30;
        public static final double ArriveGap = 0.08;
        public static final double ApproachGap = 1.4;
        public static final float TurnStep = 30.0f;
        private final Scp019Drone Drone;
        @Nullable
        private Vec3 Spot;
        private int Repath;
        private int Stuck;
        private int Settle;

        public DroneReturn(Scp019Drone Drone) {
            this.Drone = Drone;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Drone.isReturning() && !Drone.isReporting() && !Drone.locksMovement() && Drone.home() != null;
        }

        @Override
        public void start() {
            Spot = null;
            Repath = 0;
            Stuck = 0;
            Settle = 0;
        }

        @Override
        public void stop() {
            Drone.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private double speed() {
            return Drone.isHiding() ? Scp019Drone.RunSpeed : Scp019Drone.SearchSpeed;
        }

        @Override
        public void tick() {
            Scp019PotBlockEntity Home = Drone.home();
            if (Home == null) {
                return;
            }
            Vec3 Centre = Home.centre();
            if (++Stuck > (Drone.isHiding() ? Scp019Drone.HideGiveUpTicks : GiveUpTicks)) {
                Drone.puffAway();
                return;
            }
            if (Spot != null && flatDistance(Drone.position(), Spot) < ApproachGap && Math.abs(Drone.getY() - Spot.y) < 0.6) {
                settle(Centre);
                return;
            }
            Settle = 0;
            if (Spot == null || --Repath <= 0) {
                Repath = 30;
                Spot = entrySpot(Home);
                if (Spot != null) {
                    Drone.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, 0, speed());
                } else {
                    Drone.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, 1, speed());
                }
            }
            if (Spot == null && flatDistance(Drone.position(), Centre) < 2.5 && Drone.getNavigation().isDone()) {
                turnAndEnter(Centre);
            }
        }

        private void settle(Vec3 Centre) {
            Drone.getNavigation().stop();
            double Gap = flatDistance(Drone.position(), Spot);
            if (Gap > ArriveGap && ++Settle < SettleTicks) {
                Drone.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Drone.isHiding() ? 0.9 : 0.6);
                return;
            }
            Drone.getMoveControl().setWantedPosition(Drone.getX(), Drone.getY(), Drone.getZ(), 0.0);
            turnAndEnter(Centre);
        }

        private void turnAndEnter(Vec3 Centre) {
            float Wanted = yawTowards(Drone.position(), Centre);
            float Turned = Mth.approachDegrees(Drone.getYRot(), Wanted, TurnStep);
            Drone.setYRot(Turned);
            Drone.setYBodyRot(Turned);
            Drone.setYHeadRot(Turned);
            if (Mth.degreesDifferenceAbs(Turned, Wanted) < 1.0f) {
                Drone.beginEntering(Wanted);
            }
        }

        @Nullable
        private Vec3 entrySpot(Scp019PotBlockEntity Home) {
            Vec3 Centre = Home.centre();
            Vec3 Best = null;
            double BestDistance = Double.MAX_VALUE;
            Direction Lid = Home.lidSide();
            for (int[] Side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (Side[0] == Lid.getStepX() && Side[1] == Lid.getStepZ()) {
                    continue;
                }
                Vec3 Option = new Vec3(Centre.x + Side[0], Centre.y, Centre.z + Side[1]);
                AABB Box = Drone.getType().getDimensions().makeBoundingBox(Option.x, Option.y, Option.z);
                if (!Drone.level().noCollision(Drone, Box) || Drone.level().noCollision(Drone, Box.move(0.0, -0.25, 0.0))) {
                    continue;
                }
                double Distance = Option.distanceToSqr(Drone.position());
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Option;
                }
            }
            return Best;
        }
    }

    public static final class DroneSearch extends Goal {
        public static final int LookTicks = 40;
        public static final double MarkRange = 6.0;
        public static final int TravelSlack = 100;
        public static final int GiveUpMisses = 4;
        private final Scp019Drone Drone;
        @Nullable
        private BlockPos Waypoint;
        private int Looking;
        private int Travel;
        private int Retry;
        private int Misses;
        private LongArrayList Candidates = new LongArrayList();
        private int Cursor;
        private float LookYaw;

        public DroneSearch(Scp019Drone Drone) {
            this.Drone = Drone;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Drone.isReturning() && !Drone.isReporting() && !Drone.locksMovement() && Drone.home() != null;
        }

        @Override
        public void start() {
            Waypoint = null;
            Looking = 0;
            Retry = 0;
        }

        @Override
        public void stop() {
            Scp019PotBlockEntity Home = Drone.home();
            if (Home != null && Home.map() != null) {
                Home.map().release(Drone.getId());
            }
            Waypoint = null;
            Looking = 0;
            Drone.setWaypointCentre(null);
            Drone.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp019PotBlockEntity Home = Drone.home();
            if (Home == null || Home.map() == null) {
                return;
            }
            Scp019SearchMap Survey = Home.map();
            if (Looking > 0) {
                look(Home, Survey);
                return;
            }
            if (Waypoint == null) {
                if (Retry > 0) {
                    Retry--;
                    return;
                }
                pick(Home, Survey);
                return;
            }
            Travel--;
            if (Drone.tickCount % 5 == 0) {
                Survey.markAround(Drone.blockPosition(), 1);
            }
            Vec3 Destination = Vec3.atBottomCenterOf(Waypoint);
            if (flatDistance(Drone.position(), Destination) < 1.2 && Math.abs(Drone.getY() - Destination.y) < 2.0 || Drone.getNavigation().isDone()) {
                arrive(Survey);
                return;
            }
            if (Travel <= 0 || !Survey.unseen(Waypoint.asLong()) && flatDistance(Drone.position(), Destination) < 4.0) {
                if (Travel <= 0) {
                    Survey.abandon(Waypoint, Drone.getId());
                } else {
                    arrive(Survey);
                    return;
                }
                Waypoint = null;
            }
        }

        private void pick(Scp019PotBlockEntity Home, Scp019SearchMap Survey) {
            if (!Home.takePath()) {
                Retry = 1;
                return;
            }
            RandomSource Random = Drone.getRandom();
            Scp019SearchMap.Patch Option = Survey.claim(Drone.getId(), Drone.position(), Home.claimedSpots(Drone), Drone.level().getGameTime(), Random);
            if (Option == null) {
                Misses++;
                Retry = 20;
                if (Survey.finished() || Misses > GiveUpMisses && Survey.flooded()) {
                    Drone.setReturning(true);
                }
                return;
            }
            BlockPos Cell = Survey.waypoint(Option, Random);
            if (Cell == null) {
                Retry = 1;
                return;
            }
            Path Route = Drone.getNavigation().createPath(Cell, 0);
            if (Route == null || !Route.canReach() || leaves(Route, Home.centre())) {
                Survey.abandon(Cell, Drone.getId());
                Retry = 1;
                return;
            }
            Misses = 0;
            Waypoint = Cell;
            Drone.setWaypointCentre(Option.centre());
            Drone.getNavigation().moveTo(Route, Scp019Drone.SearchSpeed);
            Travel = (int) (Drone.position().distanceTo(Vec3.atBottomCenterOf(Cell)) * 14.0) + TravelSlack;
        }

        private static boolean leaves(Path Route, Vec3 Centre) {
            double Limit = (Scp019PotBlockEntity.SearchRadius + 0.5) * (Scp019PotBlockEntity.SearchRadius + 0.5);
            for (int Index = 0; Index < Route.getNodeCount(); Index++) {
                Node Step = Route.getNode(Index);
                double Dx = Step.x + 0.5 - Centre.x;
                double Dz = Step.z + 0.5 - Centre.z;
                if (Dx * Dx + Dz * Dz > Limit) {
                    return true;
                }
            }
            return false;
        }

        private void arrive(Scp019SearchMap Survey) {
            Drone.getNavigation().stop();
            BlockPos Feet = Drone.blockPosition();
            Survey.markAround(Feet, 2);
            if (Waypoint != null) {
                Survey.mark(Waypoint.asLong());
            }
            Candidates = Survey.candidates(Feet, MarkRange);
            Cursor = 0;
            Looking = LookTicks;
            LookYaw = Drone.getYRot();
        }

        private void look(Scp019PotBlockEntity Home, Scp019SearchMap Survey) {
            Looking--;
            if (Looking % 10 == 0) {
                LookYaw += (Drone.getRandom().nextFloat() - 0.5f) * 220.0f;
                double Radians = LookYaw * Mth.DEG_TO_RAD;
                Drone.getLookControl().setLookAt(Drone.getX() - Math.sin(Radians) * 4.0, Drone.getEyeY() - 0.4, Drone.getZ() + Math.cos(Radians) * 4.0, 25.0f, 30.0f);
            }
            Vec3 Eye = Drone.getEyePosition();
            while (Cursor < Candidates.size() && Home.takeRay()) {
                long Key = Candidates.getLong(Cursor++);
                if (Survey.unseen(Key) && visible(Eye, Key)) {
                    Survey.mark(Key);
                }
            }
            if (Looking <= 0) {
                Waypoint = null;
                Drone.setWaypointCentre(null);
                Survey.release(Drone.getId());
                Drone.spendShare();
            }
        }

        private boolean visible(Vec3 Eye, long Key) {
            int X = BlockPos.getX(Key);
            int Y = BlockPos.getY(Key);
            int Z = BlockPos.getZ(Key);
            Vec3 Target = new Vec3(X + 0.5, Y + 0.4, Z + 0.5);
            BlockHitResult Hit = Drone.level().clip(new ClipContext(Eye, Target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Drone));
            if (Hit.getType() == HitResult.Type.MISS) {
                return true;
            }
            BlockPos Struck = Hit.getBlockPos();
            return Struck.getX() == X && Struck.getZ() == Z && Math.abs(Struck.getY() - Y) <= 1;
        }
    }

    public static final class BruteMelee extends MeleeAttackGoal {
        private final Scp019Brute Brute;

        public BruteMelee(Scp019Brute Brute, double Speed) {
            super(Brute, Speed, false);
            this.Brute = Brute;
        }

        @Override
        public boolean canUse() {
            return !Brute.locksMovement() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Brute.action() == Scp019Creature.ActionSlam || !Brute.locksMovement() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            Brute.setGuarding(false);
        }

        @Override
        public void tick() {
            if (Brute.action() == Scp019Creature.ActionSlam) {
                Brute.getNavigation().stop();
                LivingEntity Target = Brute.getTarget();
                if (Target != null) {
                    Brute.getLookControl().setLookAt(Target, 30.0f, 30.0f);
                }
                return;
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (canPerformAttack(Target) && Brute.action() == Scp019Creature.ActionNone) {
                resetAttackCooldown();
                Brute.beginSlam(Target);
            }
        }
    }

    public static final class BruteHunt extends Goal {
        private final Scp019Brute Brute;
        private boolean Arrived;
        private int Searching;
        private int Repath;

        public BruteHunt(Scp019Brute Brute) {
            this.Brute = Brute;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Brute.getTarget() == null && !Brute.locksMovement() && Brute.hasOrder();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Arrived = false;
            Searching = 0;
            Repath = 0;
            Brute.setGuarding(false);
        }

        @Override
        public void stop() {
            Brute.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 Spot = Brute.orderSpot();
            if (Spot == null) {
                return;
            }
            if (Brute.tickCount % 5 == 0) {
                LivingEntity Target = Brute.orderTarget();
                if (Target != null && Brute.isValidTarget(Target) && Brute.inLeash(Target.position()) && Brute.getSensing().hasLineOfSight(Target)) {
                    Brute.setTarget(Target);
                    return;
                }
            }
            if (!Arrived) {
                if (--Repath <= 0) {
                    Repath = 20;
                    Brute.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, 1, Scp019Brute.ChaseSpeed);
                }
                if (flatDistance(Brute.position(), Spot) < 2.0 || Brute.getNavigation().isDone() && Repath < 15) {
                    Arrived = true;
                    Searching = Scp019Brute.SearchTicks;
                }
                return;
            }
            Searching--;
            if (Searching % 30 == 0) {
                RandomSource Random = Brute.getRandom();
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Reach = 1.5 + Random.nextDouble() * 2.5;
                Brute.getNavigation().moveTo(Spot.x + Math.cos(Angle) * Reach, Spot.y, Spot.z + Math.sin(Angle) * Reach, 1, Scp019Brute.GuardSpeed);
            }
            if (Searching <= 0) {
                Brute.clearOrder();
            }
        }
    }

    public static final class BruteReturn extends Goal {
        private final Scp019Brute Brute;
        private int Repath;

        public BruteReturn(Scp019Brute Brute) {
            this.Brute = Brute;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean idle() {
            return Brute.getTarget() == null && !Brute.locksMovement() && !Brute.hasOrder();
        }

        @Override
        public boolean canUse() {
            return idle() && Brute.distanceToPost() > Scp019Brute.PostReach * 2.0;
        }

        @Override
        public boolean canContinueToUse() {
            return idle() && Brute.distanceToPost() > Scp019Brute.PostReach;
        }

        @Override
        public void start() {
            Repath = 0;
            Brute.setGuarding(false);
        }

        @Override
        public void stop() {
            Brute.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (--Repath <= 0) {
                Repath = 20;
                Vec3 Post = Brute.post();
                Brute.getNavigation().moveTo(Post.x, Post.y, Post.z, 1, Scp019Brute.ReturnSpeed);
            }
        }
    }

    public static final class BruteGuard extends Goal {
        private final Scp019Brute Brute;
        private int Shuffle;

        public BruteGuard(Scp019Brute Brute) {
            this.Brute = Brute;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean idle() {
            return Brute.getTarget() == null && !Brute.locksMovement() && !Brute.hasOrder();
        }

        @Override
        public boolean canUse() {
            return idle() && Brute.distanceToPost() <= Scp019Brute.PostReach * 2.0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Shuffle = 100 + Brute.getRandom().nextInt(200);
        }

        @Override
        public void stop() {
            Brute.setGuarding(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            boolean Still = Brute.getNavigation().isDone();
            Brute.setGuarding(Still);
            if (--Shuffle <= 0) {
                RandomSource Random = Brute.getRandom();
                Shuffle = 160 + Random.nextInt(240);
                Vec3 Post = Brute.post();
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Reach = Random.nextDouble() * 1.4;
                Brute.getNavigation().moveTo(Post.x + Math.cos(Angle) * Reach, Post.y, Post.z + Math.sin(Angle) * Reach, 0, Scp019Brute.GuardSpeed);
            } else if (Still && Brute.distanceToPost() > Scp019Brute.PostReach) {
                Vec3 Post = Brute.post();
                Brute.getNavigation().moveTo(Post.x, Post.y, Post.z, 0, Scp019Brute.GuardSpeed);
            }
        }
    }

    public static final class BruteDefend extends HurtByTargetGoal {
        private final Scp019Brute Brute;

        public BruteDefend(Scp019Brute Brute) {
            super(Brute);
            this.Brute = Brute;
        }

        @Override
        public boolean canUse() {
            if (Brute.locksMovement() || !super.canUse()) {
                return false;
            }
            LivingEntity Attacker = Brute.getLastHurtByMob();
            return Attacker != null && Brute.isValidTarget(Attacker) && Brute.inLeash(Attacker.position());
        }
    }
}
