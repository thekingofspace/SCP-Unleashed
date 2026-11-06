package net.rai.scp_unleashed.scp343;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp343Goals {
    public static final double ArriveDistance = 0.45;
    public static final double CloseEnough = 1.4;
    public static final int RepathEvery = 20;
    public static final int SitTimeout = 400;
    public static final double SitReach = 1.6;

    private Scp343Goals() {
    }

    private static boolean busy(Scp343God God) {
        return God.isPhasing() || God.isLeaving() || God.isPassenger();
    }

    public static class Wander extends RandomStrollGoal {
        private final Scp343God God;

        public Wander(Scp343God Owner, double Speed, int Interval) {
            super(Owner, Speed, Interval);
            God = Owner;
        }

        @Override
        public boolean canUse() {
            return !busy(God) && !God.isAttending() && God.seeker() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !busy(God) && !God.isAttending() && God.seeker() == null && super.canContinueToUse();
        }
    }

    public static class Homeward extends MoveTowardsRestrictionGoal {
        private final Scp343God God;

        public Homeward(Scp343God Owner, double Speed) {
            super(Owner, Speed);
            God = Owner;
        }

        @Override
        public boolean canUse() {
            return !busy(God) && !God.isAttending() && God.seeker() == null && super.canUse();
        }
    }

    public static class Attend extends Goal {
        private final Scp343God God;

        public Attend(Scp343God Owner) {
            God = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !God.isPhasing() && God.isAttending();
        }

        @Override
        public void start() {
            God.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player Listener = God.listener();
            if (Listener != null) {
                God.getLookControl().setLookAt(Listener, 30.0f, 30.0f);
            }
        }
    }

    public static class Approach extends Goal {
        private final Scp343God God;
        private int Repath;

        public Approach(Scp343God Owner) {
            God = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !busy(God) && !God.isAttending() && God.seeker() != null;
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
            God.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player Target = God.seeker();
            if (Target == null) {
                return;
            }
            God.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Target.distanceTo(God) <= Scp343God.GreetDistance) {
                if (Target instanceof ServerPlayer Reader) {
                    God.greet(Reader);
                } else {
                    God.seek(null);
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathEvery;
                God.getNavigation().moveTo(Target, Scp343God.StrollSpeed);
            }
        }
    }

    public static class Sit extends Goal {
        private final Scp343God God;
        @Nullable
        private BlockPos Throne;
        private int Ticks;
        private int Repath;

        public Sit(Scp343God Owner) {
            God = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(God.level() instanceof ServerLevel World) || God.thronePos() == null) {
                return false;
            }
            return God.wantsToSit(World.getGameTime()) && !Scp343Thrones.occupied(World, God.thronePos());
        }

        @Override
        public boolean canContinueToUse() {
            return Throne != null && Ticks < SitTimeout && !busy(God) && !God.isAttending() && God.level() instanceof ServerLevel World
                    && Scp343Thrones.formed(World, Throne) && !Scp343Thrones.occupied(World, Throne);
        }

        @Override
        public void start() {
            Throne = God.thronePos();
            Ticks = 0;
            Repath = 0;
        }

        @Override
        public void stop() {
            if (!God.isSeated() && God.level() instanceof ServerLevel World) {
                God.sitDelayed(World.getGameTime());
            }
            Throne = null;
            God.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Throne == null || !(God.level() instanceof ServerLevel World)) {
                return;
            }
            Ticks++;
            Vec3 Seat = Scp343Thrones.seat(World, Throne);
            BlockPos Front = Scp343Thrones.front(World, Throne);
            double Distance = Math.sqrt(Math.pow(God.getX() - Seat.x, 2) + Math.pow(God.getZ() - Seat.z, 2));
            if (Distance <= SitReach && Math.abs(God.getY() - Throne.getY()) < 1.5) {
                if (God.sitOn(World, Throne)) {
                    Throne = null;
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathEvery;
                God.getNavigation().moveTo(God.getNavigation().createPath(Front, 0), Scp343God.StrollSpeed);
            }
        }
    }

    public static class StepAway extends Goal {
        private final Scp343God God;
        private int Repath;

        public StepAway(Scp343God Owner) {
            God = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return God.isLeaving() && !God.isPhasing();
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
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            God.countLeave();
            BlockPos Stand = God.leaveStand();
            BlockPos Wall = God.leaveWall();
            if (Stand == null || Wall == null) {
                God.beginDeparture(God.blockPosition());
                return;
            }
            God.getLookControl().setLookAt(Wall.getX() + 0.5, Wall.getY() + 1.0, Wall.getZ() + 0.5);
            double Distance = Math.sqrt(Math.pow(God.getX() - (Stand.getX() + 0.5), 2) + Math.pow(God.getZ() - (Stand.getZ() + 0.5), 2));
            boolean Close = Distance <= ArriveDistance || Distance <= CloseEnough && God.getNavigation().isDone();
            if (Close || God.leaveTicks() > Scp343God.LeaveTimeout) {
                BlockPos Target = Wall;
                if (!Close && God.level() instanceof ServerLevel World) {
                    BlockPos Beside = Scp343Walls.wallBeside(World, God.blockPosition(), God.getRandom());
                    Target = Beside != null ? Beside : God.blockPosition();
                }
                God.beginDeparture(Target);
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathEvery;
                God.getNavigation().moveTo(God.getNavigation().createPath(Stand, 0), Scp343God.LeaveSpeed);
            }
        }
    }
}
