package net.rai.scp_unleashed.scp280;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class Scp280Goals {
    public static final int FleeRadius = 12;
    public static final int FleeRise = 3;
    public static final int FleeSamples = 24;
    public static final int PathChecks = 10;
    public static final int RepathTicks = 8;
    public static final int ProwlRepathTicks = 60;
    public static final int ProwlSamples = 10;
    public static final int ProwlRadius = 10;
    public static final int DarkMargin = 3;
    public static final int Headroom = 3;

    private Scp280Goals() {
    }

    public static boolean darkEnough(Level World, BlockPos Pos) {
        return Scp280Light.standing(World, Pos) <= Scp280Light.BrightLevel - DarkMargin;
    }

    public static boolean standable(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.getBlockState(Pos.below()).getCollisionShape(World, Pos.below()).isEmpty()) {
            return false;
        }
        for (int Rise = 0; Rise < Headroom; Rise++) {
            BlockPos Cell = Pos.above(Rise);
            if (!World.getBlockState(Cell).getCollisionShape(World, Cell).isEmpty() || !World.getFluidState(Cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Path reachable(Scp280Shade Shade, BlockPos Target) {
        Path Route = Shade.getNavigation().createPath(Target, 1);
        return Route != null && Route.canReach() ? Route : null;
    }

    public static final class HoldStill extends Goal {
        private final Scp280Shade Holder;

        public HoldStill(Scp280Shade Holder) {
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
        private final Scp280Shade Runner;
        private int Cooldown;

        public Flee(Scp280Shade Runner) {
            this.Runner = Runner;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Runner.wantsShelter() && !Runner.locksMovement();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Cooldown = 0;
            run();
        }

        @Override
        public void stop() {
            Runner.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (Cooldown > 0) {
                Cooldown--;
                return;
            }
            if (Runner.getNavigation().isDone()) {
                run();
            }
        }

        private void run() {
            if (!Runner.onGround()) {
                Cooldown = 5;
                return;
            }
            Path Escape = darkRoute(Runner);
            if (Escape == null) {
                Runner.shelterFailed();
                Cooldown = 20;
                return;
            }
            Runner.shelterFound();
            Runner.getNavigation().moveTo(Escape, Runner.isPinned() ? Scp280Shade.CreepSpeed : Scp280Shade.FleeSpeed);
            Cooldown = 10;
        }

        @Nullable
        private static Path darkRoute(Scp280Shade Runner) {
            Level World = Runner.level();
            BlockPos Centre = Runner.blockPosition();
            int Here = Scp280Light.standing(World, Centre);
            Vec3 Away = Runner.shineSource();
            List<BlockPos> Found = new ArrayList<>();
            for (int Ring = 2; Ring <= FleeRadius; Ring += 2) {
                for (int Sample = 0; Sample < FleeSamples; Sample++) {
                    double Angle = Runner.getRandom().nextDouble() * Math.PI * 2.0;
                    int X = Mth.floor(Centre.getX() + 0.5 + Math.cos(Angle) * Ring);
                    int Z = Mth.floor(Centre.getZ() + 0.5 + Math.sin(Angle) * Ring);
                    for (int Step = 0; Step <= FleeRise * 2; Step++) {
                        int Dy = (Step + 1) / 2 * (Step % 2 == 0 ? -1 : 1);
                        BlockPos Candidate = new BlockPos(X, Centre.getY() + Dy, Z);
                        if (!standable(World, Candidate)) {
                            continue;
                        }
                        int Light = Scp280Light.standing(World, Candidate);
                        if (Light < Scp280Light.BrightLevel && Light < Here) {
                            Found.add(Candidate);
                        }
                    }
                }
                if (Found.size() >= PathChecks) {
                    break;
                }
            }
            Found.sort(Comparator.comparingDouble(Candidate -> score(World, Centre, Candidate, Away)));
            int Checks = 0;
            for (BlockPos Candidate : Found) {
                if (Checks++ >= PathChecks) {
                    break;
                }
                Path Route = reachable(Runner, Candidate);
                if (Route != null) {
                    return Route;
                }
            }
            return null;
        }

        private static double score(Level World, BlockPos Centre, BlockPos Candidate, @Nullable Vec3 Away) {
            double Score = Math.sqrt(Centre.distSqr(Candidate)) + Scp280Light.standing(World, Candidate) * 0.3;
            if (Away != null) {
                Vec3 From = Vec3.atBottomCenterOf(Centre).subtract(Away);
                Vec3 To = Vec3.atBottomCenterOf(Candidate).subtract(Vec3.atBottomCenterOf(Centre));
                if (From.lengthSqr() > 1.0e-4 && To.lengthSqr() > 1.0e-4) {
                    Score -= From.normalize().dot(To.normalize()) * 2.0;
                }
            }
            return Score;
        }
    }

    public static final class Hunt extends Goal {
        private final Scp280Shade Hunter;
        private int Repath;

        public Hunt(Scp280Shade Hunter) {
            this.Hunter = Hunter;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Hunter.getTarget();
            return Target != null && Scp280Shade.huntable(Hunter, Target) && !Hunter.locksMovement() && !Hunter.wantsShelter();
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
            double Distance = Hunter.distanceTo(Target);
            if (Distance <= Scp280Shade.StrikeReach && Hunter.readyToStrike() && Scp280Light.strikeable(Target) && !Scp280Light.isBright(Hunter) && Hunter.hasLineOfSight(Target)) {
                Hunter.getNavigation().stop();
                Hunter.beginWindup(Target);
                return;
            }
            if (--Repath > 0) {
                return;
            }
            Repath = RepathTicks + Hunter.getRandom().nextInt(4);
            if (Distance < 1.2) {
                Hunter.getNavigation().stop();
                return;
            }
            Path Route = Hunter.getNavigation().createPath(Target, 0);
            if (Route != null) {
                Hunter.getNavigation().moveTo(Route, Scp280Light.strikeable(Target) ? Scp280Shade.ChaseSpeed : Scp280Shade.StalkSpeed);
            } else {
                Hunter.getNavigation().stop();
            }
        }
    }

    public static final class Prowl extends Goal {
        private final Scp280Shade Walker;
        private int Repath;

        public Prowl(Scp280Shade Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Walker.getTarget() == null && !Walker.locksMovement() && !Walker.wantsShelter() && !Walker.isPinned();
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
        public void tick() {
            if (--Repath > 0 && !Walker.getNavigation().isDone()) {
                return;
            }
            Repath = ProwlRepathTicks + Walker.getRandom().nextInt(40);
            Vec3 Lure = Walker.lure();
            Level World = Walker.level();
            BlockPos Centre = Walker.blockPosition();
            for (int Sample = 0; Sample < ProwlSamples; Sample++) {
                double Angle = Walker.getRandom().nextDouble() * Math.PI * 2.0;
                double Radius = 3.0 + Walker.getRandom().nextDouble() * ProwlRadius;
                double X = Centre.getX() + 0.5 + Math.cos(Angle) * Radius;
                double Z = Centre.getZ() + 0.5 + Math.sin(Angle) * Radius;
                if (Lure != null) {
                    X = Mth.lerp(0.5, X, Lure.x);
                    Z = Mth.lerp(0.5, Z, Lure.z);
                }
                BlockPos Candidate = BlockPos.containing(X, Centre.getY(), Z);
                if (!standable(World, Candidate) || !darkEnough(World, Candidate)) {
                    continue;
                }
                Path Route = Walker.getNavigation().createPath(Candidate, 1);
                if (Route != null) {
                    Walker.getNavigation().moveTo(Route, Scp280Shade.ProwlSpeed);
                    return;
                }
            }
        }
    }
}
