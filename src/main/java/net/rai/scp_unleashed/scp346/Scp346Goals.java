package net.rai.scp_unleashed.scp346;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

public final class Scp346Goals {
    public static final double FleeDistance = 10.0;
    public static final int ArrivalLimit = 1200;
    public static final double FollowStart = 9.0;
    public static final double FollowStop = 3.0;
    public static final double TeleportDistance = 24.0;
    public static final double OwnerRoostRange = 10.0;
    public static final double RoostReach = 1.8;
    public static final int WanderLimit = 700;
    public static final int WaypointSpread = 7;
    public static final int WaypointTries = 16;
    public static final int StuckTicks = 40;
    public static final int PerchRadius = 6;
    public static final int PerchRise = 8;
    public static final double WaddleHome = 6.0;
    public static final int WaddleChance = 70;
    public static final int RoostCheck = 10;

    private Scp346Goals() {
    }

    private static double horizontal(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private static boolean night(Level World) {
        return World.isNight();
    }

    public static class Escape extends Goal {
        private final Scp346Pterosaur Bird;
        private Vec3 Target = Vec3.ZERO;

        public Escape(Scp346Pterosaur Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Bird.panic() > 0 && !Bird.isOrderedToSit() && !Bird.isPassenger();
        }

        @Override
        public boolean canContinueToUse() {
            return Bird.panic() > 0 && !Bird.isOrderedToSit();
        }

        @Override
        public void start() {
            Bird.wake();
            Bird.requestFlight();
            Target = fleeTarget();
        }

        @Override
        public void tick() {
            if (horizontal(Bird.position(), Target) < 1.5) {
                Target = fleeTarget();
            }
            Bird.steer(Target, 1.4);
        }

        private Vec3 fleeTarget() {
            RandomSource Random = Bird.getRandom();
            Vec3 Away = Bird.position().subtract(Bird.panicFrom());
            Away = new Vec3(Away.x, 0.0, Away.z);
            if (Away.lengthSqr() < 0.01) {
                double Angle = Random.nextDouble() * Mth.TWO_PI;
                Away = new Vec3(Math.cos(Angle), 0.0, Math.sin(Angle));
            }
            Away = Away.normalize();
            double Lift = 3.0 + Random.nextDouble() * 2.0;
            double Ceiling = Bird.level().getMaxBuildHeight() - 2.0;
            return new Vec3(Bird.getX() + Away.x * FleeDistance, Math.min(Ceiling, Bird.getY() + Lift), Bird.getZ() + Away.z * FleeDistance);
        }
    }

    public static class Arrive extends Goal {
        private final Scp346Pterosaur Bird;

        public Arrive(Scp346Pterosaur Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Bird.arrival() != null && !Bird.isOrderedToSit() && Bird.panic() <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Bird.arrival() != null && Bird.panic() <= 0 && !Bird.isOrderedToSit();
        }

        @Override
        public void tick() {
            Vec3 Target = Bird.arrival();
            if (Target == null) {
                return;
            }
            double Across = horizontal(Bird.position(), Target);
            double Above = Bird.getY() - Target.y;
            if (Bird.arrivalTicks() > ArrivalLimit) {
                if (Bird.groundBelow(8.0)) {
                    Bird.land();
                }
                Bird.setArrival(null);
                return;
            }
            if (!Bird.isFlying()) {
                if (Across < 2.0 && Math.abs(Above) < 1.5) {
                    Bird.setArrival(null);
                } else if (!Bird.takingOff()) {
                    Bird.requestFlight();
                }
                return;
            }
            if (Across < 0.9 && Above < 2.5) {
                Bird.land();
                Bird.setArrival(null);
                return;
            }
            Bird.steer(Target.add(0.0, Across > 4.0 ? 2.0 : 0.5, 0.0), Across > 6.0 ? 1.2 : 0.8);
        }
    }

    public static class FollowOwner extends Goal {
        private final Scp346Pterosaur Bird;
        @Nullable
        private LivingEntity Owner;

        public FollowOwner(Scp346Pterosaur Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Found = Bird.getOwner();
            if (!Bird.isTame() || Found == null || Found.isSpectator() || Bird.unableToMoveToOwner() || Bird.panic() > 0) {
                return false;
            }
            if (Bird.distanceToSqr(Found) < FollowStart * FollowStart) {
                return false;
            }
            Owner = Found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Owner != null && Owner.isAlive() && !Bird.unableToMoveToOwner() && Bird.distanceToSqr(Owner) > FollowStop * FollowStop;
        }

        @Override
        public void start() {
            Bird.wake();
            Bird.requestFlight();
        }

        @Override
        public void stop() {
            if (Owner != null && Bird.isFlying() && Owner.onGround() && Bird.groundBelow(4.0)) {
                Bird.land();
            }
            Owner = null;
        }

        @Override
        public void tick() {
            if (Owner == null) {
                return;
            }
            Bird.getLookControl().setLookAt(Owner, 10.0f, Bird.getMaxHeadXRot());
            if (Bird.distanceToSqr(Owner) > TeleportDistance * TeleportDistance && Owner.onGround()) {
                Bird.tryToTeleportToOwner();
                return;
            }
            if (!Bird.isFlying()) {
                Bird.requestFlight();
                return;
            }
            Bird.steer(Owner.position().add(0.0, 1.8, 0.0), 1.3);
        }
    }

    public static class Roost extends Goal {
        private final Scp346Pterosaur Bird;
        private Vec3 Spot = Vec3.ZERO;

        public Roost(Scp346Pterosaur Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean allowed() {
            if (!night(Bird.level()) || Bird.panic() > 0 || Bird.isPassenger()) {
                return false;
            }
            if (Bird.isTame()) {
                LivingEntity Owner = Bird.getOwner();
                return Bird.isOrderedToSit() || Owner != null && Bird.distanceToSqr(Owner) < OwnerRoostRange * OwnerRoostRange;
            }
            return Bird.arrival() == null;
        }

        @Override
        public boolean canUse() {
            if ((Bird.tickCount + Bird.getId()) % RoostCheck != 0 || !allowed() || Bird.isActing()) {
                return false;
            }
            if (Bird.isTame()) {
                return Bird.onGround() && !Bird.isFlying();
            }
            Scp346Pterosaur Leader = Scp346Flock.leader(Bird);
            if (Leader == Bird) {
                return Bird.onGround() && !Bird.isFlying();
            }
            return Leader.isRoosting() || Leader.onGround() && !Leader.isFlying();
        }

        @Override
        public boolean canContinueToUse() {
            return allowed();
        }

        @Override
        public void start() {
            Spot = target();
        }

        @Override
        public void stop() {
            Bird.wake();
        }

        private Vec3 target() {
            if (Bird.isTame()) {
                return Bird.position();
            }
            return Scp346Flock.roostSpot(Bird, Scp346Flock.leader(Bird));
        }

        @Override
        public void tick() {
            if (Bird.isRoosting()) {
                return;
            }
            if (Bird.tickCount % 20 == 0) {
                Spot = target();
            }
            double Across = horizontal(Bird.position(), Spot);
            if (Across > RoostReach) {
                if (Across > 4.0 || Bird.isFlying()) {
                    if (!Bird.isFlying()) {
                        Bird.requestFlight();
                        return;
                    }
                    if (Across < 1.0 && Bird.getY() - Spot.y < 2.5) {
                        Bird.land();
                        return;
                    }
                    Bird.steer(Spot.add(0.0, 0.8, 0.0), 0.9);
                } else if (Bird.getNavigation().isDone()) {
                    Bird.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, 1.0);
                }
                return;
            }
            if (Bird.isFlying()) {
                Bird.land();
                return;
            }
            if (Bird.onGround() && !Bird.isActing()) {
                Bird.setRoosting(true);
            }
        }
    }

    public static class Tempt extends TemptGoal {
        private final Scp346Pterosaur Bird;

        public Tempt(Scp346Pterosaur Bird) {
            super(Bird, 1.0, Scp346Pterosaur::isFish, false);
            this.Bird = Bird;
        }

        @Override
        public boolean canUse() {
            return !Bird.isFlying() && !Bird.isRoosting() && !Bird.isActing() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Bird.isFlying() && !Bird.isRoosting() && super.canContinueToUse();
        }
    }

    public static class Wander extends Goal {
        private final Scp346Pterosaur Bird;
        @Nullable
        private Vec3 Target;
        private boolean Landing;
        private boolean Done;
        private int Waypoints;
        private int Ticks;
        private int Stalled;
        private double Best = Double.MAX_VALUE;

        public Wander(Scp346Pterosaur Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Bird.isOrderedToSit() || Bird.isRoosting() || Bird.panic() > 0 || Bird.arrival() != null || Bird.isPassenger()) {
                return false;
            }
            if (Bird.isFlying()) {
                return true;
            }
            if (Bird.isTame() || night(Bird.level())) {
                return false;
            }
            return Bird.onGround() && Bird.restless() && !Bird.isActing();
        }

        @Override
        public boolean canContinueToUse() {
            return !Done && Bird.panic() <= 0 && !Bird.isOrderedToSit() && (Bird.isFlying() || Bird.takingOff());
        }

        @Override
        public void start() {
            Done = false;
            Landing = false;
            Target = null;
            Ticks = 0;
            if (Bird.isFlying()) {
                Waypoints = night(Bird.level()) || Bird.isTame() ? 0 : 1 + Bird.getRandom().nextInt(2);
            } else {
                Waypoints = 2 + Bird.getRandom().nextInt(3);
                Bird.requestFlight();
            }
        }

        @Override
        public void tick() {
            Ticks++;
            if (!Bird.isFlying()) {
                return;
            }
            if (Ticks > WanderLimit && Bird.groundBelow(8.0)) {
                Bird.land();
                Done = true;
                return;
            }
            if (Target != null && !Landing) {
                double Left = Bird.position().distanceTo(Target);
                if (Left < Best - 0.4) {
                    Best = Left;
                    Stalled = 0;
                } else if (++Stalled > StuckTicks) {
                    Target = null;
                }
            }
            if (Target == null || !Landing && Bird.position().distanceTo(Target) < 1.5) {
                choose();
            }
            if (Target == null) {
                return;
            }
            if (Landing && horizontal(Bird.position(), Target) < 0.7 && Bird.getY() - Target.y < 1.6) {
                Bird.land();
                Done = true;
                return;
            }
            Bird.steer(Landing ? Target.add(0.0, 0.6, 0.0) : Target, Landing ? 0.8 : 1.0);
        }

        private void choose() {
            Level World = Bird.level();
            RandomSource Random = Bird.getRandom();
            Vec3 Centre = Scp346Flock.centre(Bird);
            Stalled = 0;
            Best = Double.MAX_VALUE;
            while (Waypoints > 0) {
                Waypoints--;
                Optional<Vec3> Next = waypoint(World, Centre, Random, Bird);
                if (Next.isPresent()) {
                    Target = Next.get();
                    return;
                }
            }
            Optional<Vec3> Perch = Scp346Flock.perch(World, Centre, PerchRadius, PerchRise, Random);
            if (Perch.isEmpty()) {
                Perch = Scp346Flock.perch(World, Bird.position(), PerchRadius, PerchRise * 2, Random);
            }
            if (Perch.isPresent()) {
                Target = Perch.get();
                Landing = true;
                return;
            }
            if (Bird.groundBelow(6.0)) {
                Bird.land();
                Done = true;
            }
        }
    }

    public static Optional<Vec3> waypoint(Level World, Vec3 Centre, RandomSource Random, Scp346Pterosaur Bird) {
        Vec3 Eye = Bird.position().add(0.0, Scp346Pterosaur.Height * 0.5, 0.0);
        for (int Try = 0; Try < WaypointTries; Try++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Reach = 2.0 + Random.nextDouble() * (WaypointSpread - 2.0);
            double X = Centre.x + Math.cos(Angle) * Reach;
            double Z = Centre.z + Math.sin(Angle) * Reach;
            BlockPos Column = BlockPos.containing(X, Bird.getY(), Z);
            if (!World.isLoaded(Column)) {
                continue;
            }
            double Y;
            if (World.canSeeSky(Column.above(2))) {
                double Ground = World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Column).getY();
                Y = Math.max(Ground + 3.0 + Random.nextDouble() * 3.0, Centre.y + 2.0);
            } else {
                Y = Bird.getY() + Random.nextDouble() * 3.0 - 1.0;
            }
            Y = Mth.clamp(Y, Bird.getY() - 8.0, Bird.getY() + 8.0);
            Vec3 Spot = new Vec3(X, Y, Z);
            if (!Scp346Flock.clear(World, Spot)) {
                continue;
            }
            Vec3 Middle = Spot.add(0.0, Scp346Pterosaur.Height * 0.5, 0.0);
            if (World.clip(new ClipContext(Eye, Middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Bird)).getType() != HitResult.Type.MISS) {
                continue;
            }
            return Optional.of(Spot);
        }
        return Optional.empty();
    }

    public static class Waddle extends Goal {
        private final Scp346Pterosaur Bird;
        @Nullable
        private Vec3 Target;

        public Waddle(Scp346Pterosaur Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Bird.isFlying() || !Bird.onGround() || Bird.isActing() || Bird.isRoosting() || Bird.isInSittingPose() || Bird.isOrderedToSit() || Bird.panic() > 0) {
                return false;
            }
            if (Bird.getRandom().nextInt(WaddleChance) != 0 || Scp346Flock.perchedOnLedge(Bird)) {
                return false;
            }
            Vec3 Centre = Scp346Flock.centre(Bird);
            Target = Bird.position().distanceTo(Centre) > WaddleHome ? DefaultRandomPos.getPosTowards(Bird, 4, 2, Centre, Math.PI / 2.0) : LandRandomPos.getPos(Bird, 3, 1);
            return Target != null;
        }

        @Override
        public void start() {
            if (Target != null) {
                Bird.getNavigation().moveTo(Target.x, Target.y, Target.z, 1.0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !Bird.getNavigation().isDone() && !Bird.isFlying() && !Bird.isActing() && Bird.panic() <= 0;
        }

        @Override
        public void stop() {
            Target = null;
            if (!Bird.isFlying()) {
                Bird.getNavigation().stop();
            }
        }
    }
}
