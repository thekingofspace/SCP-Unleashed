package net.rai.scp_unleashed.scp017;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class Scp017Goals {
    public static final int DarkMargin = 2;
    public static final int FleeRadius = 14;
    public static final int FleeRise = 4;
    public static final int FleeSamples = 20;
    public static final int FleeAttempts = 3;
    public static final int PathChecks = 5;
    public static final int RepathTicks = 8;
    public static final int StalkRepathTicks = 30;
    public static final int StalkSamples = 14;
    public static final double StalkNear = 3.5;
    public static final double StalkFar = 10.0;
    public static final double WatchRange = 9.0;
    public static final int SeekLimit = 400;
    public static final int StallTicks = 100;
    public static final int SearchRetry = 200;
    public static final double SeatReach = 0.45;
    public static final int ProwlChance = 40;
    public static final int ProwlSamples = 8;

    private Scp017Goals() {
    }

    public static boolean darkEnough(Level World, BlockPos Pos) {
        return Scp017Light.at(World, Pos) <= Scp017Light.Threshold - DarkMargin;
    }

    @Nullable
    private static Path reachable(Scp017Shadow Shadow, BlockPos Target) {
        Path Route = Shadow.getNavigation().createPath(Target, 0);
        return Route != null && Route.canReach() ? Route : null;
    }

    public static final class HoldStill extends Goal {
        private final Scp017Shadow Holder;

        public HoldStill(Scp017Shadow Holder) {
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

    public static final class Flee extends Goal {
        private final Scp017Shadow Runner;
        private int Attempts;

        public Flee(Scp017Shadow Runner) {
            this.Runner = Runner;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Runner.isFleeing() && !Runner.locksMovement();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Attempts = 0;
            run();
        }

        @Override
        public void stop() {
            Runner.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (!Runner.getNavigation().isDone()) {
                return;
            }
            if (Runner.lightNow() > Scp017Light.Threshold) {
                run();
            }
        }

        private void run() {
            if (Attempts++ >= FleeAttempts) {
                Runner.fleeFailed();
                return;
            }
            Optional<Path> Escape = darkRoute(Runner);
            if (Escape.isEmpty()) {
                Runner.fleeFailed();
                return;
            }
            Runner.getNavigation().moveTo(Escape.get(), Scp017Shadow.FleeSpeed);
        }

        private static Optional<Path> darkRoute(Scp017Shadow Runner) {
            Level World = Runner.level();
            BlockPos Centre = Runner.blockPosition();
            List<BlockPos> Found = new ArrayList<>();
            for (int Ring = 2; Ring <= FleeRadius; Ring += 2) {
                for (int Sample = 0; Sample < FleeSamples; Sample++) {
                    double Angle = (Sample + Runner.getRandom().nextDouble() * 0.5) / FleeSamples * Mth.TWO_PI;
                    int X = Centre.getX() + Mth.floor(Math.cos(Angle) * Ring);
                    int Z = Centre.getZ() + Mth.floor(Math.sin(Angle) * Ring);
                    for (int Dy = -FleeRise; Dy <= FleeRise; Dy++) {
                        BlockPos Cell = new BlockPos(X, Centre.getY() + Dy, Z);
                        if (Scp017Corners.standable(World, Cell) && darkEnough(World, Cell)) {
                            Found.add(Cell);
                            break;
                        }
                    }
                }
                if (Found.size() >= PathChecks * 2) {
                    break;
                }
            }
            Found.sort(Comparator.comparingDouble(Cell -> Cell.distSqr(Centre)));
            int Checked = 0;
            for (BlockPos Cell : Found) {
                if (Checked++ >= PathChecks) {
                    break;
                }
                Path Route = reachable(Runner, Cell);
                if (Route != null) {
                    return Optional.of(Route);
                }
            }
            return Optional.empty();
        }
    }

    public static final class Hunt extends Goal {
        private final Scp017Shadow Hunter;
        private int Repath;
        private boolean Stalking;
        @Nullable
        private Vec3 LastSeen;

        public Hunt(Scp017Shadow Hunter) {
            this.Hunter = Hunter;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Hunter.isHunting() && !Hunter.locksMovement() && Hunter.getTarget() != null && Scp017Shadow.huntable(Hunter.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
            Stalking = false;
            LastSeen = null;
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
            LivingEntity Victim = Hunter.getTarget();
            if (Victim == null) {
                return;
            }
            Hunter.getLookControl().setLookAt(Victim, 30.0f, 30.0f);
            double Distance = Hunter.distanceTo(Victim);
            boolean VictimLit = Scp017Light.inLight(Victim);
            if (!VictimLit && !Hunter.shuns(Victim) && Distance <= Scp017Shadow.ReachStart && Hunter.readyToReach() && Hunter.hasLineOfSight(Victim)) {
                Hunter.beginReach(Victim);
            }
            if (--Repath > 0) {
                return;
            }
            if (!VictimLit && !Hunter.shuns(Victim)) {
                Path Route = Hunter.getNavigation().createPath(Victim, 0);
                if (Route != null && Route.canReach()) {
                    Stalking = false;
                    Hunter.getNavigation().moveTo(Route, Scp017Shadow.ChaseSpeed);
                    Repath = RepathTicks;
                    return;
                }
            }
            stalk(Victim, Distance);
        }

        private void stalk(LivingEntity Victim, double Distance) {
            Repath = StalkRepathTicks;
            if (Stalking && Distance <= WatchRange && Hunter.getNavigation().isDone()) {
                return;
            }
            Stalking = true;
            Vec3 Best = null;
            double BestDistance = Double.MAX_VALUE;
            for (int Sample = 0; Sample < StalkSamples; Sample++) {
                Vec3 Option = DefaultRandomPos.getPosTowards(Hunter, 10, 5, Victim.position(), Math.PI / 2.0);
                if (Option == null) {
                    continue;
                }
                BlockPos Cell = BlockPos.containing(Option);
                double Away = Option.distanceTo(Victim.position());
                if (Away < StalkNear || Away > StalkFar || !darkEnough(Hunter.level(), Cell)) {
                    continue;
                }
                if (Away < BestDistance) {
                    BestDistance = Away;
                    Best = Option;
                }
            }
            if (Best != null && (LastSeen == null || Best.distanceToSqr(LastSeen) > 1.0)) {
                LastSeen = Best;
                Hunter.getNavigation().moveTo(Best.x, Best.y, Best.z, Scp017Shadow.StalkSpeed);
            }
        }
    }

    public static final class SeekCorner extends Goal {
        private final Scp017Shadow Seeker;
        @Nullable
        private Scp017Corners.Corner Destination;
        private int Ticks;
        private int Stall;
        private double Closest;

        public SeekCorner(Scp017Shadow Seeker) {
            this.Seeker = Seeker;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Seeker.mode() == Scp017Shadow.ModeDormant && !Seeker.locksMovement() && Seeker.searchCooldown() <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Seeker.mode() == Scp017Shadow.ModeDormant && !Seeker.locksMovement() && Destination != null;
        }

        @Override
        public void start() {
            Seeker.setSearchCooldown(SearchRetry);
            Ticks = 0;
            Stall = 0;
            Closest = Double.MAX_VALUE;
            Destination = Scp017Corners.find(Seeker).orElse(null);
            if (Destination == null) {
                Seeker.settleAt(null);
                return;
            }
            walk();
        }

        @Override
        public void stop() {
            Destination = null;
            if (!Seeker.isSettled()) {
                Seeker.getNavigation().stop();
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Destination == null) {
                return;
            }
            Ticks++;
            Vec3 Seat = Destination.seat();
            double Dx = Seeker.getX() - Seat.x;
            double Dz = Seeker.getZ() - Seat.z;
            double Horizontal = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Horizontal <= SeatReach + 0.35 && Math.abs(Seeker.getY() - Seat.y) < 0.6) {
                if (Horizontal <= SeatReach || Seeker.getNavigation().isDone()) {
                    Scp017Corners.Corner Arrived = Destination;
                    Destination = null;
                    if (Scp017Corners.stillValid(Seeker.level(), Arrived)) {
                        Seeker.settleAt(Arrived);
                    } else {
                        Seeker.settleAt(null);
                    }
                    return;
                }
                Seeker.getNavigation().stop();
                Seeker.getMoveControl().setWantedPosition(Seat.x, Seat.y, Seat.z, Scp017Shadow.CreepSpeed);
                return;
            }
            if (Horizontal < Closest - 0.1) {
                Closest = Horizontal;
                Stall = 0;
            } else if (++Stall >= StallTicks || Ticks > SeekLimit) {
                Destination = null;
                Seeker.settleAt(null);
                return;
            }
            if (Ticks % 20 == 1 || Seeker.getNavigation().isDone()) {
                walk();
            }
        }

        private void walk() {
            if (Destination == null) {
                return;
            }
            BlockPos Cell = Destination.Pos();
            Seeker.getNavigation().moveTo(Cell.getX() + 0.5, Cell.getY(), Cell.getZ() + 0.5, Scp017Shadow.CreepSpeed);
            Seeker.getLookControl().setLookAt(Cell.getX() + 0.5, Cell.getY() + 1.0, Cell.getZ() + 0.5);
        }
    }

    public static final class Prowl extends Goal {
        private final Scp017Shadow Prowler;

        public Prowl(Scp017Shadow Prowler) {
            this.Prowler = Prowler;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Prowler.isHunting() || Prowler.locksMovement() || Prowler.getTarget() != null || Prowler.getRandom().nextInt(ProwlChance) != 0) {
                return false;
            }
            Vec3 Spot = darkSpot();
            if (Spot == null) {
                return false;
            }
            Prowler.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Scp017Shadow.ProwlSpeed);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Prowler.isHunting() && !Prowler.locksMovement() && Prowler.getTarget() == null && !Prowler.getNavigation().isDone();
        }

        @Override
        public void stop() {
            Prowler.getNavigation().stop();
        }

        @Nullable
        private Vec3 darkSpot() {
            for (int Sample = 0; Sample < ProwlSamples; Sample++) {
                Vec3 Option = DefaultRandomPos.getPos(Prowler, 12, 5);
                if (Option != null && darkEnough(Prowler.level(), BlockPos.containing(Option))) {
                    return Option;
                }
            }
            return null;
        }
    }
}
