package net.rai.scp_unleashed.scp003;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp003Goals {
    private Scp003Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp003Creature Holder;

        public HoldStill(Scp003Creature Holder) {
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

    public static final class FleePlayers extends Goal {
        private final PathfinderMob Runner;
        private final double Range;
        private final double WalkSpeed;
        private final double SprintSpeed;
        @Nullable
        private Player Threat;
        @Nullable
        private Path Escape;

        public FleePlayers(PathfinderMob Runner, double Range, double WalkSpeed, double SprintSpeed) {
            this.Runner = Runner;
            this.Range = Range;
            this.WalkSpeed = WalkSpeed;
            this.SprintSpeed = SprintSpeed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Runner instanceof Scp003Creature Creature && Creature.locksMovement()) {
                return false;
            }
            Threat = Runner.level().getNearestPlayer(Runner.getX(), Runner.getY(), Runner.getZ(), Range, Found -> !Found.isSpectator() && Found.isAlive());
            if (Threat == null) {
                return false;
            }
            Vec3 Away = DefaultRandomPos.getPosAway(Runner, 16, 7, Threat.position());
            if (Away == null || Threat.distanceToSqr(Away) < Threat.distanceToSqr(Runner)) {
                return false;
            }
            Escape = Runner.getNavigation().createPath(Away.x, Away.y, Away.z, 0);
            return Escape != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !Runner.getNavigation().isDone() && Threat != null && Threat.isAlive();
        }

        @Override
        public void start() {
            Runner.getNavigation().moveTo(Escape, WalkSpeed);
        }

        @Override
        public void stop() {
            Threat = null;
            Escape = null;
        }

        @Override
        public void tick() {
            PathNavigation Navigation = Runner.getNavigation();
            if (Threat != null) {
                Navigation.setSpeedModifier(Runner.distanceToSqr(Threat) < 25.0 ? SprintSpeed : WalkSpeed);
            }
        }
    }
}
